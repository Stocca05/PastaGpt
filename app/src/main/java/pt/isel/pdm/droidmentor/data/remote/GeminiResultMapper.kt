package pt.isel.pdm.droidmentor.data.remote // Keeps HTTP and Gemini response handling in the data layer.

import io.ktor.client.call.NoTransformationFoundException // Identifies responses that the configured client cannot convert.
import io.ktor.client.call.body // Decodes a successful response using Ktor's kotlinx.serialization converter.
import io.ktor.client.network.sockets.ConnectTimeoutException // Identifies connection time limits before generic IO handling.
import io.ktor.client.network.sockets.SocketTimeoutException // Identifies socket time limits before generic IO handling.
import io.ktor.client.plugins.HttpRequestTimeoutException // Identifies Ktor's overall request timeout.
import io.ktor.client.plugins.ResponseException // Recovers HTTP status information if a client validates error statuses.
import io.ktor.client.statement.HttpResponse // Reads status and headers without exposing them to the UI.
import io.ktor.client.statement.bodyAsText // Detects empty response bodies before JSON conversion.
import io.ktor.http.HttpHeaders // Names the standard Retry-After header.
import io.ktor.serialization.ContentConvertException // Identifies failures reported by the JSON converter.
import java.io.IOException // Identifies transport failures after checking specific network timeouts.
import java.time.Duration // Converts an HTTP-date into a remaining delay.
import java.time.Instant // Supplies an injectable clock for deterministic date tests.
import java.time.ZonedDateTime // Parses dates that include the HTTP time zone.
import java.time.format.DateTimeFormatter // Provides the standard HTTP-date formatter.
import java.time.format.DateTimeParseException // Rejects invalid date headers without exposing their content.
import kotlinx.coroutines.CancellationException // Preserves cancellation as normal coroutine control flow.
import kotlinx.coroutines.currentCoroutineContext // Checks whether the caller is still active after a transport failure.
import kotlinx.coroutines.ensureActive // Prevents cancellation-related IO from being reported as a normal failure.
import kotlinx.serialization.SerializationException // Identifies malformed supported response fields.
import pt.isel.pdm.droidmentor.data.remote.dto.GenerateContentResponse // Reads the supported Gemini response structure.

/**
 * Converts one HTTP operation and its Gemini response into a safe typed result.
 *
 * @param clock Current instant used only to interpret an HTTP-date Retry-After value.
 */
class GeminiResultMapper(private val clock: () -> Instant = Instant::now) { // Uses manual constructor injection to make date conversion testable.

    /**
     * Executes the supplied Ktor operation and handles network and response failures.
     *
     * The data source supplies its POST operation here. This mapper has no endpoint, model,
     * credential, retry loop or UI dependency. Caller cancellation is always rethrown.
     *
     * @param request Suspending HTTP operation whose response should be decoded.
     * @return Usable text or a failure without raw bodies, headers or exceptions.
     */
    suspend fun execute(request: suspend () -> HttpResponse): ApiResult<String> { // Keeps the request and all decoding inside the same failure boundary.
        return try { // Handles both sending and reading the response.
            currentCoroutineContext().ensureActive() // Stops before invoking the operation if the caller is already cancelled.
            val response = try { // Also supports a client that throws for HTTP error statuses.
                request() // Calls only the operation supplied by the data source or a test.
            } catch (error: ResponseException) { // Recovers the response without printing the exception's body or URL.
                error.response // Lets the same HTTP status mapper handle validated and unvalidated statuses.
            } // Ends recovery of HTTP status exceptions.
            mapResponse(response) // Reads a successful response or maps its HTTP failure status.
        } catch (error: CancellationException) { // Distinguishes cancelled work from a failed request.
            throw error // Preserves structured coroutine cancellation.
        } catch (error: Exception) { // Contains ordinary transport, conversion and unexpected failures in the data layer.
            currentCoroutineContext().ensureActive() // Preserves cancellation even if the engine reported it as an IOException.
            when (error) { // Checks specific failures before the generic IO fallback.
                is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> ApiResult.Timeout // Maps all Ktor network timeout types.
                is SerializationException, is ContentConvertException, is NoTransformationFoundException -> ApiResult.MalformedResponse // Maps JSON and unsupported response conversion failures.
                is IOException -> ApiResult.NetworkUnavailable // Maps the remaining connection and transport IO failures.
                else -> ApiResult.Unknown // Hides details of an ordinary failure outside the known categories.
            } // Ends exception classification.
        } // Ends the complete network and decoding boundary.
    } // Ends the safe operation wrapper.

    private suspend fun mapResponse(response: HttpResponse): ApiResult<String> { // Uses status first so malformed error bodies cannot hide an HTTP failure.
        val status = response.status.value // Reads only the numeric status needed for classification.
        if (status !in 200..299) { // Treats only successful HTTP statuses as candidate text responses.
            return when (status) { // Maps the status families required by the assignment.
                400 -> ApiResult.BadRequest // Reports an invalid request without reading its raw error message.
                401, 403 -> ApiResult.InvalidApiKey // Reports key rejection or insufficient access.
                429 -> ApiResult.RateLimited(retryAfter(response.headers[HttpHeaders.RetryAfter])) // Preserves safe numeric server guidance without performing a retry.
                in 500..599 -> ApiResult.ServerError // Keeps every server-side HTTP failure in the same category.
                else -> ApiResult.Unknown // Covers redirects and other unclassified non-success statuses.
            } // Ends HTTP status classification.
        } // Ends the HTTP failure branch.

        if (response.bodyAsText().isBlank()) { // Checks the saved Ktor body for an absent or whitespace-only response.
            return ApiResult.EmptyResponse // Avoids calling an empty body malformed JSON.
        } // Ends the empty body check.
        val decoded = response.body<GenerateContentResponse>() // Uses the existing ContentNegotiation JSON configuration to decode the saved body.
        val promptBlock = decoded.promptFeedback?.blockReason // Reads only the prompt-level blocking signal.
        if (!promptBlock.isNullOrBlank() && promptBlock != "BLOCK_REASON_UNSPECIFIED") { // Recognises known and future explicit prompt block reasons.
            return ApiResult.BlockedResponse // Never accepts text from a response whose prompt was blocked.
        } // Ends the prompt block check.
        val candidate = decoded.candidates.firstOrNull() ?: return ApiResult.EmptyResponse // Selects one alternative rather than merging unrelated generated candidates.
        when (candidate.finishReason) { // Classifies the selected candidate before using any of its text.
            "SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "IMAGE_SAFETY", "IMAGE_PROHIBITED_CONTENT", "IMAGE_RECITATION", "ESCALATION", "PUP_LIMITED_DISABLED" -> return ApiResult.BlockedResponse // Discards text from documented content and policy blocks.
            "MALFORMED_RESPONSE" -> return ApiResult.MalformedResponse // Preserves the server's explicit malformed-response outcome.
            null, "", "FINISH_REASON_UNSPECIFIED", "STOP", "MAX_TOKENS" -> Unit // Allows usable text when stopping metadata is absent or reports normal completion or the output limit.
            else -> return ApiResult.Unknown // Does not treat an unfamiliar or unsupported stopping reason as a successful answer.
        } // Ends stopping-reason classification.

        val content = candidate.content ?: return ApiResult.EmptyResponse // Requires generated content before extracting a reply.
        if (content.role != null && content.role != "model") { // Accepts an omitted role but rejects a supplied author other than the model.
            return ApiResult.MalformedResponse // Prevents a user-labelled response from being persisted as a mentor reply.
        } // Ends the generated author check.
        val text = content.parts.filter { it.thought != true }.mapNotNull { it.text }.joinToString("") // Joins ordered answer text parts without thought parts, trimming or invented separators.
        if (text.isBlank()) { // Checks the selected candidate actually contains usable answer text.
            return ApiResult.EmptyResponse // Rejects empty, non-text, thought-only and whitespace-only replies.
        } // Ends answer text validation.
        return ApiResult.Success(text) // Returns only the selected usable reply, without response metadata or exceptions.
    } // Ends HTTP and response interpretation.

    private fun retryAfter(header: String?): Long? { // Converts optional server guidance into safe non-negative seconds.
        val value = header?.trim()?.takeIf { it.isNotEmpty() } ?: return null // Treats absent and blank headers as missing guidance.
        if (value.all { it in '0'..'9' }) { // Accepts the unsigned decimal delay-seconds format.
            return value.toLongOrNull() // Rejects numeric overflow rather than wrapping or throwing.
        } // Ends decimal delay parsing.
        return try { // Attempts the standard HTTP-date form when the header is not a decimal delay.
            val target = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() // Parses the date and its server time zone.
            val remaining = Duration.between(clock(), target) // Measures delay from the time the response is being handled.
            if (remaining.isNegative || remaining.isZero) { // Handles a retry date that has already passed.
                0L // Records no remaining server delay.
            } else { // Handles a retry date still in the future.
                remaining.seconds + if (remaining.nano > 0) 1 else 0 // Rounds up fractional seconds to avoid retrying earlier than requested.
            } // Ends remaining delay conversion.
        } catch (error: DateTimeParseException) { // Rejects unsupported or malformed date text without including it in diagnostics.
            null // Keeps the rate-limit outcome even when its optional delay cannot be read.
        } // Ends HTTP-date parsing.
    } // Ends safe Retry-After conversion.
} // Ends the network result mapper.
