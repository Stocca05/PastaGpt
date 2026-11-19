package pt.isel.pdm.droidmentor.data.remote // Keeps Gemini requests in the network data layer.

import io.ktor.client.HttpClient // Uses the Ktor client supplied by the manual service locator.
import io.ktor.client.request.header // Adds the credential only to this request's header.
import io.ktor.client.request.post // Sends a stateless generateContent POST request.
import io.ktor.client.request.setBody // Passes the request DTO to the configured JSON converter.
import java.io.IOException // Stops an offline operation inside the existing typed network error boundary.
import kotlinx.coroutines.currentCoroutineContext // Checks the caller's cancellation before validating input.
import kotlinx.coroutines.ensureActive // Preserves cancellation even when no HTTP request is needed.
import pt.isel.pdm.droidmentor.data.remote.dto.Content // Wraps the system instruction separately from the history.
import pt.isel.pdm.droidmentor.data.remote.dto.GenerateContentRequest // Defines the stateless JSON request fields.
import pt.isel.pdm.droidmentor.data.remote.dto.InlineData // Attaches ephemeral in-memory Base64 image data (V9).
import pt.isel.pdm.droidmentor.data.remote.dto.Part // Holds text and optional inline_data parts.
import pt.isel.pdm.droidmentor.domain.ConversationTurn // Accepts validated domain turns instead of persistence entities.
import pt.isel.pdm.droidmentor.domain.MentorPersona // Supplies the app's central mentor instructions by default.
import pt.isel.pdm.droidmentor.domain.Role // Checks the request's user/model author sequence.

/**
 * Sends one stateless text-generation request and returns only its safe typed outcome.
 *
 * @param client Shared configured Ktor client; its owner manages its lifetime.
 * @param isConnected Mandatory current connectivity check, supplied by the connectivity component.
 * @param resultMapper Converts transport and response failures without exposing raw exceptions.
 */
class GeminiRemoteDataSource( // Uses explicit constructor dependencies without a DI framework.
    private val client: HttpClient, // Reuses the client without changing its defaults or storing a key on it.
    private val isConnected: suspend () -> Boolean, // Allows a fresh Android connection check off the main dispatcher rather than assuming the device is online.
    private val resultMapper: GeminiResultMapper = GeminiResultMapper(), // Uses the already tested typed response boundary.
    private val retryPolicy: RetryPolicy = RetryPolicy(), // Applies bounded exponential backoff + Retry-After on HTTP 429 and 503.
    private val encodeImageBase64: suspend (String) -> Pair<String, String>? = { null }, // Encodes relative image paths in memory only at request build time.
) { // Begins the request implementation.

    /** Contains the single editable model name used to build the endpoint. */
    companion object { // Keeps the chosen model in one place without an extra configuration abstraction.
        /** Stable model documented by Google; access for the user's BYOK project is checked during live testing. */
        const val MODEL_NAME = "gemini-3.5-flash-lite" // Builds every request for the agreed M2 model.
    } // Ends the model configuration.

    /**
     * Sends every supplied turn and the separate mentor instruction on each call.
     *
     * The repository supplies the complete history prepared by HistoryPayloadBuilder. This method
     * also rejects invalid wire roles or blank text before HTTP. It never stores the key, changes
     * local messages, owns a server session, retries automatically or exposes a result to the UI.
     *
     * @param history All completed exchanges followed by the current user turn, already ordered.
     * @param systemInstruction Mentor instructions; defaults to the app's central persona.
     * @param apiKey Credential read from Preferences DataStore for this call only.
     * @return Usable mentor text or a typed failure; caller cancellation remains cancellation.
     */
    suspend fun generateContent( // Exposes stateless generation with bounded transient 429/503 retry support.
        history: List<ConversationTurn>, // Receives the entire caller-supplied conversation without a history limit.
        systemInstruction: String = MentorPersona.SYSTEM_INSTRUCTION, // Resends the central persona unless the caller explicitly supplies instructions.
        apiKey: String, // Keeps the credential only in this operation's memory and HTTP header.
    ): ApiResult<String> { // Returns no raw HTTP response or exception to its repository caller.
        currentCoroutineContext().ensureActive() // Stops cancelled work before input checks or network activity.
        val key = apiKey.trim() // Removes surrounding whitespace in memory without modifying persistent preferences.
        if (key.isEmpty() || key.any { it <= ' ' || it == '\u007f' }) { // Rejects missing keys and unsafe whitespace or control characters in a header value.
            return ApiResult.InvalidApiKey // Blocks an unusable credential before invoking the network engine.
        } // Ends credential input validation.
        if (systemInstruction.isBlank() || !validHistory(history)) { // Requires a usable persona and a complete alternating conversation.
            return ApiResult.BadRequest // Rejects invalid local request input without an HTTP call.
        } // Ends the local request guard.
        val wireContents = buildWireContents(history) ?: return ApiResult.BadRequest // Encodes any attached image paths cleanly in memory right before sending.

        val maxAttempts = retryPolicy.maxAttempts.coerceAtLeast(1) // Guarantees at least one HTTP attempt even if a custom policy passes zero.
        for (attempt in 0 until maxAttempts) { // Runs initial call plus up to two retries on transient 429/503 responses.
            currentCoroutineContext().ensureActive() // Stops before starting another attempt if the caller was cancelled.
            val outcome = resultMapper.executeDetailed { // Rechecks connectivity on EVERY attempt inside the safe error boundary.
                if (!isConnected()) { // Rechecks current network availability before each initial or retried POST.
                    throw IOException("No validated network connection") // Stops before POST; the mapper returns NetworkUnavailable safely.
                } // Ends mandatory connectivity gate.
                client.post("v1beta/models/$MODEL_NAME:generateContent") { // Uses the factory's Gemini HTTPS base URL and the single configured model.
                    header("x-goog-api-key", key) // Sends the key in this request only, never in its URL or body.
                    setBody(GenerateContentRequest( // Lets ContentNegotiation serialize a fresh stateless DTO for every attempt.
                        systemInstruction = Content(parts = listOf(Part(text = systemInstruction))), // Keeps the persona in its own system_instruction object.
                        contents = wireContents, // Sends complete multimodal turns with Base64 generated solely in memory.
                    )) // Finishes the full request body without credentials or a session identifier.
                } // Ends one HTTP operation; the mapper classifies the response.
            } // Ends one typed attempt.
            if (!outcome.retryableStatus || attempt == maxAttempts - 1) { // Stops immediately on success, permanent error, offline, or exhausted attempts.
                return outcome.result // Returns the final typed outcome to the repository.
            } // Ends retry termination check.
            retryPolicy.waitBeforeRetry(attempt, outcome.retryAfterSeconds) // Waits with Retry-After or exponential backoff + jitter before the next attempt.
        } // Ends bounded retry loop.
        return ApiResult.Unknown // Fallback unreachable because maxAttempts >= 1 always returns in the loop.
    } // Ends stateless text generation.

    private suspend fun buildWireContents(history: List<ConversationTurn>): List<Content>? { // Produces wire DTOs with ephemeral inline_data in memory only.
        return history.map { turn -> // Converts each domain turn cleanly in sequence order.
            val parts = mutableListOf<Part>() // Collects text and/or inline_data parts for this turn.
            if (turn.text.isNotBlank()) { // Omits blank caption text on image-only user messages.
                parts.add(Part(text = turn.text)) // Adds exact preserved text part.
            } // Ends text part check.
            val path = turn.imagePath // Reads optional sandbox-safe relative image path.
            if (!path.isNullOrBlank()) { // Produces Base64 purely in memory at request time (V9).
                val encoded = encodeImageBase64(path) // Returns (mimeType, base64) from private storage or null if removed.
                if (encoded != null) { // Attaches inline_data only when the local image bytes are available.
                    parts.add(Part(inlineData = InlineData(encoded.first, encoded.second))) // Appends ephemeral Base64 payload.
                } // Ends encoded bytes check.
            } // Ends image part check.
            if (parts.isEmpty()) return null // Rejects a turn whose sole image disappeared and has no fallback text.
            Content(role = if (turn.role == Role.USER) "user" else "model", parts = parts) // Wraps parts under explicit Gemini role.
        } // Ends turn mapping.
    } // Ends ephemeral multimodal content conversion.

    private fun validHistory(history: List<ConversationTurn>): Boolean { // Checks the wire-level rules without duplicating saved-message state validation.
        if (history.isEmpty() || history.size % 2 == 0) { // Requires 2N + 1 turns so the request starts and ends with user.
            return false // Blocks missing questions and histories ending with a model reply.
        } // Ends the turn-count guard.
        for ((index, turn) in history.withIndex()) { // Checks every supplied turn rather than filtering invalid entries.
            val expectedRole = if (index % 2 == 0) Role.USER else Role.MODEL // Requires strict alternation from the first user turn.
            val hasImage = turn.role == Role.USER && !turn.imagePath.isNullOrBlank() // Allows image-only or text+image user turns.
            if (turn.role != expectedRole || (turn.text.isBlank() && !hasImage)) { // Rejects invalid roles and completely empty turns.
                return false // Keeps an invalid conversation out of the remote request.
            } // Ends validation of this turn.
        } // Ends the supplied history loop.
        return true // Accepts the entire history only after every wire-level rule passed.
    } // Ends the additional request history guard.
} // Ends the Gemini data source.
