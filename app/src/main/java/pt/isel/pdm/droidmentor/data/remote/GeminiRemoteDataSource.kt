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
import pt.isel.pdm.droidmentor.data.remote.dto.Part // Holds the system instruction as a text part.
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
    suspend fun generateContent( // Exposes the single text-generation operation required by M2.
        history: List<ConversationTurn>, // Receives the entire caller-supplied conversation without a history limit.
        systemInstruction: String = MentorPersona.SYSTEM_INSTRUCTION, // Resends the central persona unless the caller explicitly supplies instructions.
        apiKey: String, // Keeps the credential only in this operation's memory and HTTP header.
    ): ApiResult<String> { // Returns no raw HTTP response or exception to its repository caller.
        currentCoroutineContext().ensureActive() // Stops cancelled work before input checks or network activity.
        val key = apiKey.trim() // Removes surrounding whitespace in memory without modifying persistent preferences.
        if (key.isEmpty() || key.any { it <= ' ' || it == '\u007f' }) { // Rejects missing keys and unsafe whitespace or control characters in a header value.
            return ApiResult.InvalidApiKey // Blocks an unusable credential before invoking the network engine.
        } // Ends credential input validation.
        if (systemInstruction.isBlank() || !validHistory(history)) { // Requires a usable persona and a complete alternating text conversation.
            return ApiResult.BadRequest // Rejects invalid local request input without an HTTP call.
        } // Ends the local request guard.

        return resultMapper.execute { // Keeps connectivity checks, request construction, HTTP and decoding inside the typed error boundary.
            if (!isConnected()) { // Rechecks the supplied current connection state immediately before this attempt.
                throw IOException("No validated network connection") // Stops before POST; the mapper returns NetworkUnavailable without exposing this exception.
            } // Ends the mandatory connectivity gate.
            client.post("v1beta/models/$MODEL_NAME:generateContent") { // Uses the factory's Gemini HTTPS base URL and the single configured model.
                header("x-goog-api-key", key) // Sends the key in this request only, never in its URL or body.
                setBody(GenerateContentRequest( // Lets ContentNegotiation serialize a fresh stateless DTO for every call.
                    systemInstruction = Content(parts = listOf(Part(text = systemInstruction))), // Keeps the persona in its own system_instruction object.
                    contents = history.map { it.toContent() }, // Converts all supplied turns without dropping or trimming any text.
                )) // Finishes the full request body without credentials or a session identifier.
            } // Ends the one HTTP operation; the mapper reads its result.
        } // Ends the typed request and response boundary.
    } // Ends stateless text generation.

    private fun validHistory(history: List<ConversationTurn>): Boolean { // Checks the wire-level rules without duplicating saved-message state validation.
        if (history.isEmpty() || history.size % 2 == 0) { // Requires 2N + 1 turns so the request starts and ends with user.
            return false // Blocks missing questions and histories ending with a model reply.
        } // Ends the turn-count guard.
        for ((index, turn) in history.withIndex()) { // Checks every supplied turn rather than filtering invalid entries.
            val expectedRole = if (index % 2 == 0) Role.USER else Role.MODEL // Requires strict alternation from the first user turn.
            if (turn.role != expectedRole || turn.text.isBlank()) { // Rejects repeated authors and blank historical or current text.
                return false // Keeps an invalid conversation out of the remote request.
            } // Ends validation of this turn.
        } // Ends the supplied history loop.
        return true // Accepts the entire history only after every wire-level rule passed.
    } // Ends the additional request history guard.
} // Ends the Gemini data source.
