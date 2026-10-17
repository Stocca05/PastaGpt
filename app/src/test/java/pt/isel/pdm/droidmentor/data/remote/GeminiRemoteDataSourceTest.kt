package pt.isel.pdm.droidmentor.data.remote // Tests the stateless Gemini request through the configured client.

import io.ktor.client.engine.config // Creates an engine factory owned by the test client.
import io.ktor.client.engine.mock.MockEngine // Simulates requests entirely in memory.
import io.ktor.client.engine.mock.respond // Builds the simulated Gemini response.
import io.ktor.client.network.sockets.SocketTimeoutException // Simulates a real Ktor socket timeout type.
import io.ktor.client.plugins.logging.Logger // Captures only the final safe debug log output.
import io.ktor.client.request.get // Checks the shared client remains usable after a data-source call.
import io.ktor.client.statement.bodyAsText // Reads a harmless follow-up response in the client ownership test.
import io.ktor.http.ContentType // Provides the JSON media type.
import io.ktor.http.HttpHeaders // Names standard HTTP response headers.
import io.ktor.http.HttpMethod // Checks that the source uses POST.
import io.ktor.http.HttpStatusCode // Supplies response statuses for simulated failures.
import io.ktor.http.content.TextContent // Reads the JSON produced by the actual request serializer.
import io.ktor.http.headersOf // Builds JSON response headers.
import java.io.IOException // Simulates transport IO failure without a real server.
import java.util.UUID // Generates fictitious credentials only at test runtime.
import kotlinx.coroutines.CancellationException // Checks cancelled requests remain cancellation.
import kotlinx.coroutines.async // Starts a test caller that can be cancelled independently.
import kotlinx.coroutines.cancel // Cancels that caller before invoking the data source.
import kotlinx.coroutines.currentCoroutineContext // Identifies the caller to cancel.
import kotlinx.coroutines.test.runTest // Runs suspending requests from JUnit tests.
import kotlinx.serialization.json.Json // Inspects the request body independently of DTO decoding.
import kotlinx.serialization.json.jsonArray // Reads the complete contents list and text parts.
import kotlinx.serialization.json.jsonObject // Reads exact JSON request field names.
import kotlinx.serialization.json.jsonPrimitive // Reads role and text strings from the serialized payload.
import org.junit.Assert.assertEquals // Compares harmless request metadata, text fixtures and typed results.
import org.junit.Assert.assertNull // Checks cancelled work cannot return a request outcome.
import org.junit.Assert.assertTrue // Checks credentials with boolean assertions that never print their values.
import org.junit.Test // Marks each method as a JUnit verification case.
import pt.isel.pdm.droidmentor.domain.ConversationTurn // Supplies complete domain histories to the source.
import pt.isel.pdm.droidmentor.domain.MentorPersona // Checks the actual app persona is sent on every default call.
import pt.isel.pdm.droidmentor.domain.Role // Supplies the local user/model author enum.

/** Checks endpoint, headers, full-history JSON, connectivity and typed outcomes without a real key. */
class GeminiRemoteDataSourceTest { // Keeps every simulated request under the ownership of its test client.
    private val responseBody = """{"candidates":[{"content":{"role":"model","parts":[{"text":"Reply"}]},"finishReason":"STOP"}],"responseId":"ignored-server-id"}""" // Supplies a usable reply and an ID the source must never reuse.
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()) // Enables the production response converter.

    /** Checks the real request method, model endpoint, key header and separate full-history JSON. */
    @Test // Verifies the wire request rather than only a DTO constructed in isolation.
    fun postContainsAllTurnsAndSeparateInstructions() = runTest { // Supplies one completed exchange and the next question.
        val dummy = UUID.randomUUID().toString() // Generates a fictitious credential held only in memory.
        val instructions = " Teach Android clearly.\nUse Kotlin. " // Supplies custom instructions with whitespace that must survive.
        val history = listOf(ConversationTurn(Role.USER, "Explain \"Flow\".\n"), ConversationTurn(Role.MODEL, " First answer. "), ConversationTurn(Role.USER, "Keep café and 🍝.")) // Supplies all three turns in the required order.
        val engine = MockEngine.config { // Creates the in-memory request handler.
            addHandler { request -> // Inspects what the production data source would send.
                assertEquals(HttpMethod.Post, request.method) // Checks the source calls generateContent with POST.
                assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent", request.url.toString()) // Checks the verified model and factory base URL.
                assertTrue("The key must appear in exactly one credential header", request.headers.getAll("x-goog-api-key") == listOf(dummy)) // Uses a boolean comparison so failure never echoes the value.
                assertTrue("The URL must contain no credential query", request.url.parameters.isEmpty() && !request.url.toString().contains(dummy)) // Checks no key or server session is passed in the URL.
                assertEquals(ContentType.Application.Json, request.body.contentType?.withoutParameters()) // Checks the outgoing media type is JSON.
                val body = (request.body as TextContent).text // Reads the serializer's in-memory output.
                assertTrue("The body must contain no credential", !body.contains(dummy)) // Checks the key is separate from JSON.
                val root = Json.parseToJsonElement(body).jsonObject // Inspects exact wire fields without using the request DTO decoder.
                assertEquals(setOf("system_instruction", "contents"), root.keys) // Rejects session, cached-content or credential fields in the body.
                val system = root.getValue("system_instruction").jsonObject // Reads the persona object separately from contents.
                assertEquals(setOf("parts"), system.keys) // Checks the system instruction has no conversation role.
                assertEquals(instructions, system.getValue("parts").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content) // Checks the entire supplied instruction is preserved.
                val contents = root.getValue("contents").jsonArray // Reads every serialized conversation turn.
                assertEquals(listOf("user", "model", "user"), contents.map { it.jsonObject.getValue("role").jsonPrimitive.content }) // Checks explicit role conversion and turn order.
                assertEquals(history.map { it.text }, contents.map { it.jsonObject.getValue("parts").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content }) // Checks every text character is retained.
                respond(responseBody, headers = jsonHeaders) // Returns a harmless simulated mentor reply.
            } // Ends the request inspection.
        } // Ends the engine setup.
        HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Applies the real client configuration to MockEngine.
            val source = GeminiRemoteDataSource(client, { true }) // Supplies explicit online connectivity for this simulated request.
            assertEquals(ApiResult.Success("Reply"), source.generateContent(history, instructions, dummy)) // Checks the response travels through the existing result mapper.
        } // Closes the shared test client and its owned engine.
    } // Ends the complete wire contract test.

    /** Checks every call resends the persona and supplied history while credentials remain per request. */
    @Test // Reuses the same client and source for two sequential stateless calls.
    fun repeatedCallsResendPersonaAndNeverKeepCredentialsOrSessions() = runTest { // Changes both the history and the key between requests.
        val keys = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString()) // Generates two distinct fictitious credentials in memory.
        val histories = listOf(firstHistory(), listOf(ConversationTurn(Role.USER, "Question"), ConversationTurn(Role.MODEL, "Reply"), ConversationTurn(Role.USER, "Next question"))) // Supplies full histories for each call.
        var requests = 0 // Tracks which request is being inspected without storing its credential.
        val engine = MockEngine.config { // Creates an engine for both sequential requests.
            addHandler { request -> // Checks that each POST is freshly constructed.
                val index = requests++ // Selects this call's supplied values.
                assertTrue("Each request must use only its own key", request.headers.getAll("x-goog-api-key") == listOf(keys[index])) // Checks keys are not accumulated in client defaults.
                val root = Json.parseToJsonElement((request.body as TextContent).text).jsonObject // Reads this call's JSON.
                assertEquals(setOf("system_instruction", "contents"), root.keys) // Checks no response ID becomes a server session field.
                assertEquals(MentorPersona.SYSTEM_INSTRUCTION, root.getValue("system_instruction").jsonObject.getValue("parts").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content) // Checks the real central persona is resent on both calls.
                assertEquals(histories[index].map { it.text }, root.getValue("contents").jsonArray.map { it.jsonObject.getValue("parts").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content }) // Checks history is supplied in full for this call only.
                assertTrue("Server response IDs must not affect the URL", !request.url.toString().contains("ignored-server-id")) // Checks the previous response identity is never reused.
                respond(responseBody, headers = jsonHeaders) // Returns the same reply shape with an irrelevant server response ID.
            } // Ends the per-call inspection.
        } // Ends the repeated-call engine setup.
        HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Shares one configured client for both calls.
            val source = GeminiRemoteDataSource(client, { true }) // Shares one data source without retaining conversation state.
            for (index in histories.indices) { // Sends the two complete histories in sequence.
                assertEquals(ApiResult.Success("Reply"), source.generateContent(histories[index], apiKey = keys[index])) // Uses the central persona default on each call.
            } // Ends the repeated send loop.
        } // Closes the client after both requests.
        assertEquals(2, requests) // Checks there is exactly one operation per call.
    } // Ends repeated stateless call verification.

    /** Checks a large supplied history is serialized in full with no client-side turn or text limit. */
    @Test // Verifies the actual POST after the existing domain builder's long-history tests.
    fun longHistoryIsSentWithoutTruncation() = runTest { // Prepares hundreds of alternating turns.
        val history = (0..500).map { index -> ConversationTurn(if (index % 2 == 0) Role.USER else Role.MODEL, "Turn $index café 🍝\n") } // Creates 501 ordered turns ending with user.
        val engine = MockEngine.config { // Inspects the complete serialized body.
            addHandler { request -> // Reads the contents actually sent by the source.
                val contents = Json.parseToJsonElement((request.body as TextContent).text).jsonObject.getValue("contents").jsonArray // Reads all encoded turns.
                assertEquals(501, contents.size) // Checks the source did not cap the conversation length.
                assertEquals(history.map { it.text }, contents.map { it.jsonObject.getValue("parts").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content }) // Checks full ordered text for every turn.
                respond(responseBody, headers = jsonHeaders) // Returns the simulated reply.
            } // Ends long-payload inspection.
        } // Ends the large-history engine setup.
        HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Uses actual JSON encoding for the long payload.
            assertEquals(ApiResult.Success("Reply"), GeminiRemoteDataSource(client, { true }).generateContent(history, apiKey = UUID.randomUUID().toString())) // Sends only a runtime-generated fictitious key.
        } // Closes the simulated client.
    } // Ends full long-history wire verification.

    /** Checks missing keys and unsafe interior whitespace or controls stop before connectivity and HTTP. */
    @Test // Covers invalid local credential input without reading DataStore or a real key.
    fun invalidCredentialInputStartsNoRequest() = runTest { // Uses only empty input or generated fictitious key fragments.
        var calls = 0 // Counts any accidental HTTP operation.
        var checks = 0 // Counts any unnecessary connectivity check before rejecting unusable input.
        val engine = MockEngine.config { addHandler { calls += 1; respond(responseBody, headers = jsonHeaders) } } // Records requests that should never occur.
        HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Creates the client used by all guard cases.
            val source = GeminiRemoteDataSource(client, { checks += 1; true }) // Tracks the online check separately from HTTP.
            val dummy = UUID.randomUUID().toString() // Generates an in-memory value for invalid header variations.
            for (key in listOf("", " \n\t ", "$dummy x", "$dummy\r\nx", "$dummy\u0000x", "$dummy\u007fx")) { // Includes missing input and interior whitespace/control characters.
                assertEquals(ApiResult.InvalidApiKey, source.generateContent(firstHistory(), apiKey = key)) // Uses safe result assertions that never print the input.
            } // Ends invalid key verification.
        } // Closes the test client.
        assertEquals(0, calls) // Checks no invalid key reaches the engine.
        assertEquals(0, checks) // Checks input rejection occurs before an attempted send.
    } // Ends invalid credential guard verification.

    /** Checks invalid roles, missing turns, blank text and blank instructions never reach HTTP. */
    @Test // Verifies the source's wire-level guard independently of repository integration.
    fun invalidHistoryOrInstructionsStartNoRequest() = runTest { // Supplies malformed domain turn lists.
        var calls = 0 // Counts accidental requests from invalid local input.
        val engine = MockEngine.config { addHandler { calls += 1; respond(responseBody, headers = jsonHeaders) } } // Responds only if a guard incorrectly permits HTTP.
        val histories = listOf(emptyList(), listOf(ConversationTurn(Role.MODEL, "Reply")), listOf(ConversationTurn(Role.USER, "Question"), ConversationTurn(Role.MODEL, "Reply")), listOf(ConversationTurn(Role.USER, "Question"), ConversationTurn(Role.USER, "Repeated user"), ConversationTurn(Role.USER, "Next")), listOf(ConversationTurn(Role.USER, "Question"), ConversationTurn(Role.MODEL, "Reply"), ConversationTurn(Role.MODEL, "Repeated model")), listOf(ConversationTurn(Role.USER, " \n ")), listOf(ConversationTurn(Role.USER, "Question"), ConversationTurn(Role.MODEL, " "), ConversationTurn(Role.USER, "Next"))) // Covers count, start/end role, alternation and text failures.
        HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Uses a valid shared client while checking local rejection.
            val source = GeminiRemoteDataSource(client, { true }) // Makes network availability irrelevant to malformed input.
            val dummy = UUID.randomUUID().toString() // Supplies a usable fictitious header value.
            for (history in histories) { // Checks each invalid history without filtering its bad turns.
                assertEquals(ApiResult.BadRequest, source.generateContent(history, apiKey = dummy)) // Requires typed local rejection.
            } // Ends invalid history verification.
            assertEquals(ApiResult.BadRequest, source.generateContent(firstHistory(), " \n ", dummy)) // Checks unusable system instructions are also rejected.
        } // Closes the shared client.
        assertEquals(0, calls) // Checks no rejected history or persona starts HTTP.
    } // Ends invalid request guard verification.

    /** Checks connectivity is refreshed once immediately before every attempted HTTP operation. */
    @Test // Changes network state between calls using the same source.
    fun connectivityIsRecheckedAndOfflineCallsAreBlocked() = runTest { // Records gate and engine events in their actual order.
        var online = true // Starts with an available connection.
        val events = mutableListOf<String>() // Records only event names, never request details.
        val engine = MockEngine.config { // Records actual HTTP operations separately from gate checks.
            addHandler { events.add("request"); respond(responseBody, headers = jsonHeaders) } // Adds one event per request that reaches the engine.
        } // Ends event engine setup.
        HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Shares the same client across changing connection states.
            val source = GeminiRemoteDataSource(client, { events.add("check"); online }) // Reads the current value rather than caching it at construction.
            val dummy = UUID.randomUUID().toString() // Creates one fictitious credential for the sequence.
            assertEquals(ApiResult.Success("Reply"), source.generateContent(firstHistory(), apiKey = dummy)) // Sends while connectivity is available.
            online = false // Simulates loss of connectivity between calls.
            assertEquals(ApiResult.NetworkUnavailable, source.generateContent(firstHistory(), apiKey = dummy)) // Checks the offline call stops before the engine.
            online = true // Simulates reconnection without creating a new source.
            assertEquals(ApiResult.Success("Reply"), source.generateContent(firstHistory(), apiKey = dummy)) // Checks the next call reads connectivity again.
        } // Closes the client after the state changes.
        assertEquals(listOf("check", "request", "check", "check", "request"), events) // Checks each send is gated and the offline attempt has no request event.
    } // Ends current connectivity verification.

    /** Checks ordinary connectivity-provider failures are contained by the same safe error boundary. */
    @Test // Covers a provider that fails before an HTTP operation can be constructed.
    fun connectivityProviderFailureIsTyped() = runTest { // Simulates an unexpected provider error with fictitious sensitive details.
        var calls = 0 // Counts any HTTP operation after a failed prerequisite check.
        val dummy = UUID.randomUUID().toString() // Generates an in-memory value that must never appear in the result.
        val engine = MockEngine.config { addHandler { calls += 1; respond(responseBody, headers = jsonHeaders) } } // Records unexpected HTTP activity.
        HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Provides the real client configuration.
            val source = GeminiRemoteDataSource(client, { throw IllegalStateException(dummy) }) // Fails only the injected connection check.
            val outcome = source.generateContent(firstHistory(), apiKey = dummy) // Executes through the mapper's exception boundary.
            assertEquals(ApiResult.Unknown, outcome) // Checks the ordinary provider failure does not escape.
            assertTrue("Connectivity failures must hide raw details", !outcome.toString().contains(dummy)) // Uses a boolean assertion without printing the value.
        } // Closes the client.
        assertEquals(0, calls) // Checks a failed gate starts no HTTP operation.
    } // Ends provider failure verification.

    /** Checks HTTP and response failures remain typed after integration with the actual POST source. */
    @Test // Covers the result mapper through its production caller.
    fun responseFailuresRemainTypedThroughTheSource() = runTest { // Supplies representative server and response failures.
        val cases = listOf(400 to ApiResult.BadRequest, 401 to ApiResult.InvalidApiKey, 403 to ApiResult.InvalidApiKey, 429 to ApiResult.RateLimited(null), 503 to ApiResult.ServerError) // Defines the required HTTP integration outcomes.
        for ((status, expected) in cases) { // Sends one stateless POST for each failure status.
            val engine = MockEngine.config { addHandler { respond("error", HttpStatusCode.fromValue(status), jsonHeaders) } } // Supplies a raw error body without relying on its JSON.
            HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Creates a configured client for this case.
                assertEquals(expected, GeminiRemoteDataSource(client, { true }).generateContent(firstHistory(), apiKey = UUID.randomUUID().toString())) // Checks status errors stay typed at the source boundary.
            } // Closes this case's client.
        } // Ends HTTP failure verification.
        val bodies = listOf("{" to ApiResult.MalformedResponse, "{}" to ApiResult.EmptyResponse, """{"promptFeedback":{"blockReason":"SAFETY"}}""" to ApiResult.BlockedResponse) // Defines response-level integration outcomes.
        for ((body, expected) in bodies) { // Sends each response structure through the actual source and JSON converter.
            val engine = MockEngine.config { addHandler { respond(body, headers = jsonHeaders) } } // Supplies a successful HTTP status with the selected response problem.
            HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Creates this case's owned client and engine.
                assertEquals(expected, GeminiRemoteDataSource(client, { true }).generateContent(firstHistory(), apiKey = UUID.randomUUID().toString())) // Checks malformed, empty and blocked outcomes remain distinct.
            } // Closes this response case's client.
        } // Ends response structure verification.
    } // Ends typed response integration verification.

    /** Checks IO and timeout failures from the source's POST never escape as raw exceptions. */
    @Test // Exercises transport errors with the real caller and request serialization.
    fun transportFailuresRemainTypedThroughTheSource() = runTest { // Supplies simulated failures after the request reaches the engine.
        val cases = listOf(IOException("Connection unavailable") to ApiResult.NetworkUnavailable, SocketTimeoutException("Socket expired") to ApiResult.Timeout) // Defines the transport families needed for source integration.
        for ((failure, expected) in cases) { // Runs one source call for each injected transport problem.
            val engine = MockEngine.config { addHandler { throw failure } } // Fails inside the actual POST path.
            HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Uses the production timeout and conversion plugins.
                assertEquals(expected, GeminiRemoteDataSource(client, { true }).generateContent(firstHistory(), apiKey = UUID.randomUUID().toString())) // Checks the source returns the mapper's safe outcome.
            } // Closes the failing client.
        } // Ends the source transport error loop.
    } // Ends transport integration verification.

    /** Checks cancellation from a running POST is preserved across the source and mapper. */
    @Test // Covers the cancelled engine path with a harmless exception message.
    fun cancelledPostRemainsCancellation() = runTest { // Makes only the simulated request operation cancel.
        val engine = MockEngine.config { addHandler { throw CancellationException("Cancelled") } } // Stops the engine operation by normal coroutine cancellation.
        HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Provides the configured client for the cancellation path.
            var cancelled = false // Records whether cancellation was propagated.
            try { // Attempts a valid request whose engine cancels it.
                GeminiRemoteDataSource(client, { true }).generateContent(firstHistory(), apiKey = UUID.randomUUID().toString()) // Calls the normal source operation.
            } catch (error: CancellationException) { // Catches expected cancellation only in the test.
                cancelled = true // Records that no ordinary API failure replaced it.
            } // Ends cancellation observation.
            assertTrue("Cancelled POST must remain cancellation", cancelled) // Checks the source preserves the caller's control flow.
        } // Closes the cancelled test client.
    } // Ends POST cancellation verification.

    /** Checks an already cancelled caller stops even before invalid input guards return. */
    @Test // Guards the source's preflight cancellation check.
    fun cancelledCallerStartsNoRequestOrInputResult() = runTest { // Uses a separately cancelled child so the parent can inspect the outcome.
        var calls = 0 // Counts any accidental HTTP operation from the cancelled caller.
        var outcome: ApiResult<String>? = null // Tracks any improperly returned local validation result.
        val engine = MockEngine.config { addHandler { calls += 1; respond(responseBody, headers = jsonHeaders) } } // Records operations that should never occur.
        HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Keeps the client alive until cancellation is checked.
            val caller = async { // Starts an independently cancellable request caller.
                currentCoroutineContext().cancel() // Cancels before invoking the source.
                outcome = GeminiRemoteDataSource(client, { true }).generateContent(emptyList(), apiKey = "") // Supplies invalid input that must not hide cancellation.
            } // Ends the child caller body.
            try { // Waits for the expected cancellation.
                caller.await() // Ensures the cancelled source call has finished.
            } catch (error: CancellationException) { // Accepts normal child cancellation without cancelling the parent test.
                Unit // Leaves the result variable unset.
            } // Ends the cancellation wait.
        } // Closes the shared client.
        assertNull(outcome) // Checks cancellation did not become a local input result.
        assertEquals(0, calls) // Checks the cancelled caller started no HTTP request.
    } // Ends preflight cancellation verification.

    /** Checks debug diagnostics hide the header, URL, body and exception values during actual source calls. */
    @Test // Combines production request construction with the already configured safe logger.
    fun debugRequestsNeverDiscloseCredentials() = runTest { // Uses one fictitious value for successful and failed calls.
        val dummy = UUID.randomUUID().toString() // Creates only an in-memory test credential.
        val output = TestLogger() // Captures final log output without printing it.
        var calls = 0 // Selects success, HTTP failure and transport failure responses.
        val engine = MockEngine.config { // Records no raw request details outside the in-memory handler.
            addHandler { request -> // Checks the credential is not embedded in the URL or body.
                assertTrue("Credential must remain outside URL and body", !request.url.toString().contains(dummy) && !(request.body as TextContent).text.contains(dummy)) // Reports only a generic message on disclosure.
                when (calls++) { // Varies the simulated outcome of each source call.
                    0 -> respond(responseBody, headers = jsonHeaders) // Returns a successful harmless reply.
                    1 -> respond(dummy, HttpStatusCode.Unauthorized, jsonHeaders) // Simulates a raw sensitive error body.
                    else -> throw IOException(dummy) // Simulates sensitive transport exception text.
                } // Ends response selection.
            } // Ends the privacy handler.
        } // Ends the debug engine setup.
        HttpClientFactory.create(true, engine, output).use { client -> // Enables the production safe logging plugin for these source calls.
            val source = GeminiRemoteDataSource(client, { true }) // Uses the actual request and failure boundary.
            assertEquals(ApiResult.Success("Reply"), source.generateContent(firstHistory(), apiKey = dummy)) // Exercises successful request and response logging.
            assertEquals(ApiResult.InvalidApiKey, source.generateContent(firstHistory(), apiKey = dummy)) // Exercises HTTP error logging without exposing its body.
            assertEquals(ApiResult.NetworkUnavailable, source.generateContent(firstHistory(), apiKey = dummy)) // Exercises the exception logging path.
        } // Closes the debug client.
        assertTrue("Debug requests must produce activity messages", output.messages.isNotEmpty()) // Checks the logging path actually ran.
        assertTrue("Debug output must hide the credential", output.messages.none { it.contains(dummy) }) // Avoids printing the fictitious value on failure.
        assertTrue("Debug output must be fixed safe text", output.messages.all { it == "DroidMentor HTTP activity (details omitted)" }) // Checks URLs, bodies, headers and exception details were discarded.
    } // Ends source diagnostic privacy verification.

    /** Checks surrounding key whitespace is removed only in memory and the shared client keeps no key default. */
    @Test // Verifies client ownership and per-request credential lifetime together.
    fun keyIsTrimmedPerCallAndSharedClientRemainsUsable() = runTest { // Reuses the client after the source operation.
        val dummy = UUID.randomUUID().toString() // Generates a harmless credential value in memory.
        var calls = 0 // Distinguishes the source's POST from the owner's subsequent GET.
        val engine = MockEngine.config { // Inspects headers for both operations.
            addHandler { request -> // Checks that source-specific headers never become shared defaults.
                if (calls++ == 0) { // Inspects the generated POST.
                    assertTrue("The source must trim only surrounding whitespace", request.headers["x-goog-api-key"] == dummy) // Uses a boolean assertion without echoing the credential.
                } else { // Inspects a separate operation issued by the client's owner.
                    assertNull(request.headers["x-goog-api-key"]) // Checks the shared client did not retain the source's key.
                } // Ends per-operation header checks.
                respond(responseBody, headers = jsonHeaders) // Keeps both operations harmless and successful.
            } // Ends shared-client inspection.
        } // Ends the ownership engine setup.
        HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Keeps ownership of the client outside the data source.
            assertEquals(ApiResult.Success("Reply"), GeminiRemoteDataSource(client, { true }).generateContent(firstHistory(), apiKey = " \n$dummy\t ")) // Checks in-memory whitespace cleanup on a generated value.
            client.get("v1beta/test").bodyAsText() // Checks the source did not close the shared client after its request.
        } // Closes the client only when its owning test finishes.
        assertEquals(2, calls) // Checks both operations reached the same owned engine.
    } // Ends key lifetime and shared client ownership verification.

    private fun firstHistory() = listOf(ConversationTurn(Role.USER, "Question")) // Provides the smallest valid full history.

    private class TestLogger : Logger { // Captures only the factory's final fixed diagnostic messages.
        val messages = mutableListOf<String>() // Keeps log observations in memory without writing a report containing payloads.

        /** Records final diagnostic text without sending it to the console. */
        override fun log(message: String) { // Implements the supplied log destination for test clients.
            messages.add(message) // Lets privacy tests inspect only the safe final output.
        } // Ends in-memory log collection.
    } // Ends the safe test log destination.
} // Ends data-source integration verification.
