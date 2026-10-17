package pt.isel.pdm.droidmentor.data.remote // Tests the data-layer error boundary without contacting Gemini.

import io.ktor.client.engine.config // Builds client-owned simulated engines.
import io.ktor.client.engine.mock.MockEngine // Simulates HTTP responses and transport failures.
import io.ktor.client.engine.mock.respond // Builds a simulated response from text or a byte channel.
import io.ktor.client.network.sockets.ConnectTimeoutException // Simulates the Ktor connection timeout type.
import io.ktor.client.network.sockets.SocketTimeoutException // Simulates the Ktor socket timeout type.
import io.ktor.client.plugins.HttpRequestTimeoutException // Simulates Ktor's overall request timeout type.
import io.ktor.client.plugins.logging.Logger // Supplies a silent log destination for these tests.
import io.ktor.client.plugins.timeout // Overrides the request timeout in the simulated timer test.
import io.ktor.client.request.get // Runs a harmless simulated HTTP operation.
import io.ktor.http.ContentType // Provides JSON and plain-text response media types.
import io.ktor.http.Headers // Builds response metadata without real credentials.
import io.ktor.http.HttpHeaders // Names Content-Type and Retry-After headers.
import io.ktor.http.HttpStatusCode // Supplies HTTP statuses to MockEngine.
import io.ktor.http.headersOf // Builds simple response headers for conversion tests.
import io.ktor.utils.io.ByteChannel // Simulates a response stream that fails while being read.
import java.io.IOException // Simulates generic transport IO failures.
import java.time.Instant // Supplies a fixed time for Retry-After date tests.
import java.util.UUID // Generates fictitious sensitive values only in test memory.
import kotlinx.coroutines.CancellationException // Checks cancellation remains normal coroutine control flow.
import kotlinx.coroutines.async // Starts a separately cancellable test caller.
import kotlinx.coroutines.cancel // Cancels only that caller in the cancellation-related IO test.
import kotlinx.coroutines.currentCoroutineContext // Identifies the caller to cancel.
import kotlinx.coroutines.delay // Simulates slow work for the actual Ktor timeout timer.
import kotlinx.coroutines.test.StandardTestDispatcher // Makes the simulated engine use the controlled test scheduler.
import kotlinx.coroutines.test.runTest // Runs suspending operations and virtual-time checks.
import org.junit.Assert.assertEquals // Compares safe text fixtures, numeric metadata and result categories.
import org.junit.Assert.assertNull // Checks cancellation does not produce a network result.
import org.junit.Assert.assertTrue // Checks sensitive values without displaying them on failure.
import org.junit.Test // Marks each verification case as a JUnit test.
import pt.isel.pdm.droidmentor.domain.MessageErrorCategory // Checks the labels the repository will later persist.

/** Verifies HTTP, transport and response outcomes through the configured Ktor client and MockEngine. */
class GeminiResultMapperTest { // Tests one network operation at a time without a real key or endpoint.
    private val now = Instant.parse("2026-10-10T12:00:00.250Z") // Fixes a fractional instant to test date rounding deterministically.
    private val validBody = """{"candidates":[{"content":{"role":"model","parts":[{"text":"Answer"}]},"finishReason":"STOP"}]}""" // Uses a literal API-shaped response independent of the DTO encoder.
    private val silentLogger = object : Logger { // Prevents these tests from printing any response or exception data.
        /** Ignores messages; the tests use clients with logging disabled. */
        override fun log(message: String) = Unit // Implements the logger contract without a console or file write.
    } // Ends the silent test logger.

    /** Checks answer parts are joined unchanged, thoughts are excluded and alternatives are never merged. */
    @Test // Exercises successful JSON decoding through ContentNegotiation.
    fun successfulResponseUsesOnlyFirstCandidatesAnswerParts() = runTest { // Supplies ordered parts and two generated alternatives.
        val body = """{"candidates":[{"content":{"role":"model","parts":[{"text":"Thought","thought":true},{"text":"  Flow café 🍝"},{"text":null},{"text":"\nNext line. "}],"extra":1},"finishReason":"STOP"},{"content":{"parts":[{"text":"Other alternative"}]},"finishReason":"STOP"}],"extra":true}""" // Includes unsupported metadata and harmless Unicode text.
        assertEquals(ApiResult.Success("  Flow café 🍝\nNext line. "), result(body)) // Checks no trimming, invented separator, thought text or alternative is added.
    } // Ends the multipart success test.

    /** Checks optional metadata and the model's output-token limit still allow usable text. */
    @Test // Covers successful status variants and supported finish metadata.
    fun usableTextAcceptsOptionalMetadataAndOutputLimit() = runTest { // Leaves the DTO's optional metadata genuinely optional.
        for (reason in listOf("STOP", "MAX_TOKENS", "", "FINISH_REASON_UNSPECIFIED")) { // Covers the supported non-blocking stopping values.
            assertEquals(ApiResult.Success("Answer"), result(validBody.replace("\"STOP\"", "\"$reason\""))) // Checks existing answer text is usable for each case.
        } // Ends the stopping metadata loop.
        assertEquals(ApiResult.Success("Answer"), result(validBody.replace(",\"finishReason\":\"STOP\"", "").replace("\"role\":\"model\",", ""))) // Checks omitted role and finish fields do not invent an error.
        assertEquals(ApiResult.Success("Answer"), result(validBody, status = 201)) // Checks a successful HTTP status still uses the response decoder.
        val feedback = validBody.dropLast(1) + ",\"promptFeedback\":{\"blockReason\":\"BLOCK_REASON_UNSPECIFIED\"}}" // Supplies the documented default non-blocking prompt value.
        assertEquals(ApiResult.Success("Answer"), result(feedback)) // Checks the default marker does not block a valid answer.
    } // Ends optional success metadata verification.

    /** Checks absent, blank, non-text and thought-only answers are typed empty responses. */
    @Test // Distinguishes empty output from malformed JSON.
    fun unusableAnswerTextIsAnEmptyResponse() = runTest { // Supplies several valid ways to have no usable answer.
        val bodies = listOf( // Lists empty transport bodies and valid empty response structures.
            "", // Represents a response with no body bytes.
            " \n\t ", // Represents a whitespace-only body.
            "{}", // Represents an envelope without candidates.
            """{"candidates":[]}""", // Represents an explicitly empty candidate list.
            """{"candidates":[{}]}""", // Represents a candidate without content.
            """{"candidates":[{"content":{}}]}""", // Represents content without parts.
            """{"candidates":[{"content":{"parts":[{}, {"text":null}]}}]}""", // Represents parts without usable text.
            """{"candidates":[{"content":{"parts":[{"text":" \n "}]}}]}""", // Represents only whitespace answer text.
            """{"candidates":[{"content":{"parts":[{"text":"Thought only","thought":true}]}}]}""", // Represents a thought without an answer.
        ) // Ends the empty response fixture list.
        for (body in bodies) { // Runs every fixture through the actual client decoder.
            assertEquals(ApiResult.EmptyResponse, result(body)) // Checks no unusable reply is returned as success.
        } // Ends the empty response loop.
        assertEquals(ApiResult.EmptyResponse, result("", status = 204)) // Checks a successful no-content status also produces no answer.
    } // Ends empty output verification.

    /** Checks HTTP statuses take priority over malformed or sensitive error bodies. */
    @Test // Covers required client errors, every 5xx code and other non-success statuses.
    fun httpFailuresAreMappedBeforeReadingTheirBodies() = runTest { // Keeps raw error text out of returned values.
        val dummy = UUID.randomUUID().toString() // Generates a fictitious sensitive server error body.
        val cases = listOf(400 to ApiResult.BadRequest, 401 to ApiResult.InvalidApiKey, 403 to ApiResult.InvalidApiKey, 302 to ApiResult.Unknown, 404 to ApiResult.Unknown, 418 to ApiResult.Unknown) // Defines the expected HTTP contract independently of response JSON.
        for ((status, expected) in cases) { // Covers each specified client or unclassified status.
            val outcome = result(dummy, status) // Supplies a non-JSON raw error body that must not be decoded.
            assertEquals(expected, outcome) // Checks the numeric status determines the safe category.
            assertTrue("HTTP results must hide raw error text", !outcome.toString().contains(dummy)) // Avoids printing the generated value on failure.
        } // Ends the explicit HTTP status table.
        for (status in 500..599) { // Covers the entire required server-error range.
            assertEquals(ApiResult.ServerError, result("<html>Unavailable</html>", status)) // Checks non-JSON bodies cannot disguise a server failure.
        } // Ends the server status loop.
    } // Ends HTTP failure mapping verification.

    /** Checks numeric Retry-After values are preserved without performing a retry. */
    @Test // Covers zero, surrounding whitespace and the largest representable delay.
    fun unsignedRetryDelaysArePreserved() = runTest { // Supplies rate-limit responses with decimal metadata.
        val cases = listOf("0" to 0L, " 15 " to 15L, Long.MAX_VALUE.toString() to Long.MAX_VALUE) // Defines valid non-negative delay values.
        for ((header, seconds) in cases) { // Tests each valid delay format through HTTP headers.
            assertEquals(ApiResult.RateLimited(seconds), result("error", 429, jsonHeaders(header))) // Checks the exact delay reaches the typed result.
        } // Ends the numeric delay loop.
    } // Ends numeric Retry-After verification.

    /** Checks absent, invalid and overflowing retry metadata cannot replace the rate-limit outcome. */
    @Test // Covers malformed optional guidance separately from the HTTP 429 failure.
    fun invalidRetryMetadataIsIgnoredSafely() = runTest { // Reads headers that are not usable delay or date values.
        val values = listOf(null, "", " ", "-1", "+1", "1.5", "9223372036854775808", "not a date") // Includes invalid syntax and a value beyond Long.
        for (header in values) { // Supplies each value in its own simulated response.
            assertEquals(ApiResult.RateLimited(null), result("error", 429, jsonHeaders(header))) // Checks invalid metadata becomes unknown rather than throwing.
        } // Ends invalid delay verification.
    } // Ends optional metadata failure verification.

    /** Checks an HTTP-date is converted from receipt time with rounding up and no negative delay. */
    @Test // Uses a fixed injected clock to avoid wall-clock timing assumptions.
    fun retryDatesUseTheCurrentClockAndRoundUp() = runTest { // Supplies standard GMT HTTP dates.
        assertEquals(ApiResult.RateLimited(120), result("error", 429, jsonHeaders("Sat, 10 Oct 2026 12:02:00 GMT"))) // Checks 119.75 remaining seconds rounds up to 120.
        assertEquals(ApiResult.RateLimited(0), result("error", 429, jsonHeaders("Sat, 10 Oct 2026 12:00:00 GMT"))) // Checks an already passed date has no remaining delay.
        assertEquals(ApiResult.RateLimited(0), result("error", 429, jsonHeaders("Fri, 09 Oct 2026 12:00:00 GMT"))) // Checks older dates never produce a negative delay.
    } // Ends HTTP-date conversion verification.

    /** Checks the mapper performs exactly one operation even when the server suggests a long wait. */
    @Test // Guards against adding automatic retry or backoff to M2.
    fun rateLimitDoesNotRetryTheOperation() = runTest { // Supplies an operation counter and a rate-limit response.
        var calls = 0 // Counts requests without recording their headers or body.
        val engine = MockEngine.config { // Creates an owned simulated engine.
            addHandler { // Runs once for each HTTP attempt.
                calls += 1 // Records the attempt count.
                respond("error", HttpStatusCode.TooManyRequests, jsonHeaders("120")) // Suggests a wait without making the test wait.
            } // Ends the response handler.
        } // Ends the engine configuration.
        HttpClientFactory.create(false, engine, silentLogger).use { client -> // Uses the production network configuration without logging.
            assertEquals(ApiResult.RateLimited(120), mapper().execute { client.get("v1beta/test") }) // Checks metadata is returned immediately as an outcome.
        } // Closes the simulated client.
        assertEquals(1, calls) // Checks no second request or retry loop was started.
    } // Ends the single-attempt verification.

    /** Checks explicit prompt blocks and candidate policy blocks discard even non-empty returned text. */
    @Test // Covers block priority and the documented block finish reasons.
    fun blockedResponsesNeverProduceAnAnswer() = runTest { // Uses otherwise usable answer text to make the block check meaningful.
        for (reason in listOf("SAFETY", "BLOCKLIST", "FUTURE_BLOCK")) { // Covers known and future explicit prompt blocks.
            val body = validBody.dropLast(1) + ",\"promptFeedback\":{\"blockReason\":\"$reason\"}}" // Adds a block signal alongside a candidate containing text.
            assertEquals(ApiResult.BlockedResponse, result(body)) // Checks prompt feedback takes priority over generated text.
        } // Ends the prompt block loop.
        val reasons = listOf("SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "IMAGE_SAFETY", "IMAGE_PROHIBITED_CONTENT", "IMAGE_RECITATION", "ESCALATION", "PUP_LIMITED_DISABLED") // Lists documented policy-related finish reasons.
        for (reason in reasons) { // Covers each supported candidate block signal.
            assertEquals(ApiResult.BlockedResponse, result(validBody.replace("\"STOP\"", "\"$reason\""))) // Checks no blocked candidate is converted into success.
        } // Ends the candidate block loop.
        assertEquals(ApiResult.BlockedResponse, result("""{"promptFeedback":{"blockReason":"SAFETY"}}""")) // Checks a prompt block needs no returned candidate to be recognised.
    } // Ends response blocking verification.

    /** Checks unsupported stopping reasons stay unknown and explicit malformed output stays malformed. */
    @Test // Prevents success when the server reports an unrecognised failure reason.
    fun unsupportedFinishReasonsAreTypedFailures() = runTest { // Supplies usable text together with failure metadata.
        for (reason in listOf("OTHER", "LANGUAGE", "FUTURE_REASON", "MALFORMED_FUNCTION_CALL")) { // Covers unknown and unsupported non-answer outcomes.
            assertEquals(ApiResult.Unknown, result(validBody.replace("\"STOP\"", "\"$reason\""))) // Checks a non-empty body alone does not imply success.
        } // Ends unsupported reason verification.
        assertEquals(ApiResult.MalformedResponse, result(validBody.replace("\"STOP\"", "\"MALFORMED_RESPONSE\""))) // Checks the server's explicit malformed-response signal.
    } // Ends stopping-reason failure verification.

    /** Checks malformed JSON, wrong supported field types and invalid generated authors never escape. */
    @Test // Distinguishes unreadable responses from valid empty responses.
    fun malformedResponsesAreTyped() = runTest { // Supplies malformed transport text or API structure.
        val bodies = listOf("{", "not JSON", "[]", """{"candidates":{}}""", """{"candidates":null}""", """{"candidates":[{"content":{"parts":[{"text":{}}]}}]}""", validBody.replace("\"model\"", "\"user\"")) // Covers syntax, top-level shape, field types and incorrect author.
        for (body in bodies) { // Exercises each malformed case through ContentNegotiation.
            assertEquals(ApiResult.MalformedResponse, result(body)) // Checks conversion failures are contained and categorised.
        } // Ends malformed response verification.
    } // Ends the malformed JSON boundary test.

    /** Checks absent or unsupported response media types are typed conversion failures. */
    @Test // Covers Ktor's no-transformation failure rather than just JSON syntax errors.
    fun unsupportedContentTypesAreMalformedResponses() = runTest { // Supplies valid JSON with unusable response metadata.
        assertEquals(ApiResult.MalformedResponse, result(validBody, headers = Headers.Empty)) // Checks an absent converter media type cannot leak a Ktor exception.
        assertEquals(ApiResult.MalformedResponse, result(validBody, headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()))) // Checks a non-JSON media type produces a typed failure.
    } // Ends response media type verification.

    /** Checks specific network timeouts are classified before their generic IO parents. */
    @Test // Covers request, connection, socket, ordinary IO and unexpected failures.
    fun transportExceptionsAreTypedWithoutTheirDetails() = runTest { // Injects transport exceptions into the operation boundary.
        val cases = listOf(HttpRequestTimeoutException("https://example.org/test", 100L) to ApiResult.Timeout, ConnectTimeoutException("Connect expired", null) to ApiResult.Timeout, SocketTimeoutException("Socket expired") to ApiResult.Timeout, IOException("Connection unavailable") to ApiResult.NetworkUnavailable, IllegalStateException("Unexpected failure") to ApiResult.Unknown) // Defines independent exception-to-result expectations.
        for ((failure, expected) in cases) { // Covers every required transport family and the fallback.
            val outcome = mapper().execute { throw failure } // Simulates a failure before any response is available.
            assertEquals(expected, outcome) // Checks the result contains a safe category instead of the exception.
        } // Ends the transport classification table.
    } // Ends transport exception verification.

    /** Checks Ktor's real request-timeout timer is also contained by the mapper. */
    @Test // Uses controlled virtual time rather than waiting for a real network timeout.
    fun ktorRequestTimerProducesTypedTimeout() = runTest { // Runs both the engine delay and timeout on the test scheduler.
        val engine = MockEngine.config { // Creates an owned delayed simulated engine.
            dispatcher = StandardTestDispatcher(testScheduler) // Makes the engine's coroutines use this test's virtual clock.
            addHandler { // Simulates a server slower than the request's configured limit.
                delay(1_000) // Keeps the response pending beyond the request timeout.
                respond(validBody, headers = jsonHeaders()) // Would return a usable answer if the timer did not stop it.
            } // Ends the delayed response handler.
        } // Ends the timed engine configuration.
        HttpClientFactory.create(false, engine, silentLogger).use { client -> // Installs the real HttpTimeout plugin.
            val outcome = mapper().execute { client.get("v1beta/test") { timeout { requestTimeoutMillis = 100 } } } // Makes the real timeout fire before the delayed response.
            assertEquals(ApiResult.Timeout, outcome) // Checks the thrown Ktor timeout became a safe result.
        } // Closes the client and simulated engine.
    } // Ends the real timeout timer verification.

    /** Checks IO failure while receiving the body is contained just like failure while sending. */
    @Test // Exercises a response channel that cannot finish being read.
    fun bodyReadFailureIsANetworkFailure() = runTest { // Creates a failed simulated response stream.
        val channel = ByteChannel() // Creates an in-memory channel instead of a network socket.
        channel.cancel(IOException("Body unavailable")) // Makes reading the response body fail.
        val engine = MockEngine.config { addHandler { respond(channel, headers = jsonHeaders()) } } // Returns the failed channel with a successful HTTP status.
        HttpClientFactory.create(false, engine, silentLogger).use { client -> // Uses normal Ktor body reading and conversion.
            assertEquals(ApiResult.NetworkUnavailable, mapper().execute { client.get("v1beta/test") }) // Checks body IO is caught inside the same boundary.
        } // Closes the client.
    } // Ends response stream failure verification.

    /** Checks a client that validates HTTP statuses still produces the same typed HTTP result. */
    @Test // Exercises an actual Ktor ResponseException rather than a status-only response.
    fun validatedHttpFailuresStillUseStatusMapping() = runTest { // Temporarily enables status validation only on a test client copy.
        val engine = MockEngine.config { addHandler { respond("error", HttpStatusCode.Forbidden, jsonHeaders()) } } // Supplies the HTTP failure that Ktor will throw.
        HttpClientFactory.create(false, engine, silentLogger).use { client -> // Creates the ordinary configured client.
            client.config { expectSuccess = true }.use { validatingClient -> // Makes this test operation throw for HTTP 403.
                assertEquals(ApiResult.InvalidApiKey, mapper().execute { validatingClient.get("v1beta/test") }) // Checks the wrapper recovers the response and preserves its status category.
            } // Closes the test-only client copy.
        } // Closes the original client.
    } // Ends validated HTTP status verification.

    /** Checks explicit cancellation is rethrown rather than converted to an ordinary network error. */
    @Test // Verifies structured cancellation across the operation boundary.
    fun cancellationIsPreserved() = runTest { // Supplies a cancelled operation without any sensitive text.
        var cancelled = false // Records whether cancellation escaped as expected.
        try { // Attempts a request whose caller has cancelled it.
            mapper().execute { throw CancellationException("Cancelled") } // Simulates normal cancellation control flow.
        } catch (error: CancellationException) { // Catches the expected cancellation only in the test.
            cancelled = true // Records that no ApiResult was substituted.
        } // Ends the expected cancellation check.
        assertTrue("Caller cancellation must propagate", cancelled) // Checks cancellation was not swallowed.
    } // Ends explicit cancellation verification.

    /** Checks cancellation still wins when an engine reports the cancelled operation as IO failure. */
    @Test // Verifies the caller-state check in the generic exception branch.
    fun cancelledIoDoesNotReturnANetworkResult() = runTest { // Cancels only a child caller, leaving the test able to inspect the result.
        var outcome: ApiResult<String>? = null // Tracks whether the mapper incorrectly returns after cancellation.
        val caller = async { // Starts a caller whose job can be cancelled independently.
            outcome = mapper().execute { // Assigns a result only if the mapper improperly continues.
                currentCoroutineContext().cancel() // Cancels this request's caller before the simulated engine error.
                throw IOException("Cancelled transport") // Simulates how a cancelled engine might report its interrupted IO.
            } // Ends the cancelled request operation.
        } // Ends the separate caller body.
        try { // Waits for cancellation from the child.
            caller.await() // Ensures its operation has completed before checking the result variable.
        } catch (error: CancellationException) { // Accepts the expected child cancellation without cancelling the parent test.
            Unit // Leaves the outcome unset for the assertion.
        } // Ends the child cancellation wait.
        assertNull("Cancelled IO must not be converted to an ApiResult", outcome) // Checks the mapper did not return a misleading network error.
    } // Ends cancellation-related IO verification.

    /** Checks every failure has the persisted domain label and success has none. */
    @Test // Covers conversion to Room-compatible categories without performing any database write.
    fun resultsMapToSafeMessageErrorCategories() { // Defines the complete result-to-label contract.
        val cases = listOf(ApiResult.InvalidApiKey to MessageErrorCategory.INVALID_API_KEY, ApiResult.BadRequest to MessageErrorCategory.BAD_REQUEST, ApiResult.RateLimited(10) to MessageErrorCategory.RATE_LIMITED, ApiResult.ServerError to MessageErrorCategory.SERVER_ERROR, ApiResult.NetworkUnavailable to MessageErrorCategory.NETWORK_UNAVAILABLE, ApiResult.Timeout to MessageErrorCategory.TIMEOUT, ApiResult.MalformedResponse to MessageErrorCategory.MALFORMED_RESPONSE, ApiResult.EmptyResponse to MessageErrorCategory.EMPTY_RESPONSE, ApiResult.BlockedResponse to MessageErrorCategory.BLOCKED_RESPONSE, ApiResult.Unknown to MessageErrorCategory.UNKNOWN) // Lists safe category expectations explicitly.
        for ((result, category) in cases) { // Checks every failure case supported by the sealed result.
            assertEquals(category, result.toMessageErrorCategory()) // Checks only the persisted enum label is produced.
        } // Ends failure-to-label verification.
        assertNull(ApiResult.Success("Answer").toMessageErrorCategory()) // Checks success clears the error category.
    } // Ends persisted label verification.

    /** Checks successful values remain usable but cannot leak through automatic diagnostic text. */
    @Test // Uses only an in-memory fictitious sensitive string.
    fun resultDiagnosticsHideSensitiveValues() = runTest { // Simulates server text containing a value that must not be logged.
        val dummy = UUID.randomUUID().toString() // Generates a test-only value rather than a real credential.
        val success = result(validBody.replace("Answer", dummy)) // Decodes the generated text without printing it.
        assertTrue("Success must retain its decoded value", success is ApiResult.Success && success.value == dummy) // Uses a boolean assertion to avoid exposing the value.
        assertTrue("Success diagnostics must hide the value", !success.toString().contains(dummy)) // Checks the safe result representation.
        val failure = mapper().execute { throw IOException(dummy) } // Simulates an exception whose message contains sensitive text.
        assertEquals(ApiResult.NetworkUnavailable, failure) // Checks the exception is reduced to a safe category.
        assertTrue("Failure diagnostics must hide exception text", !failure.toString().contains(dummy)) // Checks no raw exception text is retained.
    } // Ends result privacy verification.

    private fun mapper() = GeminiResultMapper { now } // Uses the same deterministic clock for every date fixture.

    private fun jsonHeaders(retryAfter: String? = null): Headers = Headers.build { // Marks response JSON and optionally supplies server delay metadata.
        append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) // Selects the client's installed JSON converter.
        if (retryAfter != null) { // Leaves a genuinely missing delay header absent.
            append(HttpHeaders.RetryAfter, retryAfter) // Supplies only the test metadata for this response.
        } // Ends optional header insertion.
    } // Ends simulated JSON header construction.

    private suspend fun result(body: String, status: Int = 200, headers: Headers = jsonHeaders()): ApiResult<String> { // Runs fixtures through the real client and complete mapper boundary.
        val engine = MockEngine.config { addHandler { respond(body, HttpStatusCode.fromValue(status), headers) } } // Creates one owned engine with the selected response.
        return HttpClientFactory.create(false, engine, silentLogger).use { client -> // Ensures the test's client and engine are closed.
            mapper().execute { client.get("v1beta/test") } // Sends no key and contacts no real server.
        } // Ends the simulated client lifetime.
    } // Ends the typed fixture result helper.
} // Ends the complete network result mapper tests.
