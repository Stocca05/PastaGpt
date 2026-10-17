package pt.isel.pdm.droidmentor.data.remote // Keeps the client tests beside the network package they verify.

import io.ktor.client.call.body // Decodes response bodies using the installed content converter.
import io.ktor.client.engine.config // Builds an engine factory with a simulated request handler.
import io.ktor.client.engine.mock.MockRequestHandleScope // Provides the simulated response builder scope.
import io.ktor.client.request.HttpRequestData // Represents the outgoing request inspected by the handler.
import io.ktor.client.request.HttpResponseData // Represents the simulated response returned by the handler.
import io.ktor.client.engine.mock.MockEngine // Simulates HTTP requests entirely in memory.
import io.ktor.client.engine.mock.respond // Builds a simulated HTTP response.
import io.ktor.client.engine.okhttp.OkHttpEngine // Identifies the production engine through its Ktor type.
import io.ktor.client.plugins.HttpTimeoutCapability // Reads the timeout settings received by the engine.
import io.ktor.client.plugins.logging.Logger // Captures only the factory's final log output.
import io.ktor.client.plugins.logging.Logging // Checks whether logging is installed.
import io.ktor.client.plugins.pluginOrNull // Looks up an optional client plugin.
import io.ktor.client.request.get // Sends a simulated GET request.
import io.ktor.client.request.header // Adds a fictitious credential header for the privacy tests.
import io.ktor.client.request.parameter // Adds a fictitious query value for the privacy tests.
import io.ktor.client.request.post // Sends a simulated JSON POST request.
import io.ktor.client.request.setBody // Supplies the test request object.
import io.ktor.client.statement.bodyAsText // Reads a response without assuming it contains successful JSON.
import io.ktor.http.ContentType // Provides JSON media types for simulated responses.
import io.ktor.http.HttpHeaders // Names standard headers used by the tests.
import io.ktor.http.HttpStatusCode // Provides HTTP status values.
import io.ktor.http.content.TextContent // Reads the JSON body produced by the converter.
import io.ktor.http.headersOf // Builds the response headers used by MockEngine.
import java.io.IOException // Simulates a network failure without a real connection.
import java.util.UUID // Generates fictitious sensitive values only in memory.
import kotlinx.coroutines.job // Reads the engine job to verify resource cleanup.
import kotlinx.coroutines.withTimeout // Prevents a failed lifecycle check from hanging the test.
import kotlinx.coroutines.test.runTest // Runs suspending client calls from JUnit tests.
import kotlinx.serialization.Serializable // Generates a serializer for the test-only payload.
import kotlinx.serialization.json.Json // Parses the outgoing test JSON for its field checks.
import kotlinx.serialization.json.jsonObject // Reads object fields from parsed JSON.
import org.junit.Assert.assertEquals // Compares safe values such as status codes and timeout durations.
import org.junit.Assert.assertFalse // Checks that a JSON field or plugin is absent.
import org.junit.Assert.assertNotNull // Checks that the debug logging plugin is present.
import org.junit.Assert.assertNull // Checks that the release logging plugin is absent.
import org.junit.Assert.assertTrue // Checks privacy without printing any generated sensitive value.
import org.junit.Test // Marks methods as JUnit test cases.
import pt.isel.pdm.droidmentor.BuildConfig // Checks the actual build variant used by the production factory.

/** Verifies client defaults and diagnostic privacy without calling Gemini or using a real key. */
class HttpClientFactoryTest { // Groups tests for this configuration task only.

    /** Checks the public factory uses Ktor's OkHttp engine and the generated build flag. */
    @Test // Runs once for each tested build variant.
    fun productionClientUsesOkHttpAndBuildVariantLogging() = runTest { // Creates a real engine without sending any request.
        val client = HttpClientFactory.create() // Keeps a reference so the test can also inspect engine cleanup.
        client.use { // Closes the client after inspecting its configuration.
            assertTrue(client.engine is OkHttpEngine) // Checks OkHttp is used as the Ktor engine.
            assertEquals(BuildConfig.DEBUG, client.pluginOrNull(Logging) != null) // Checks debug enables logging and release omits it.
        } // Releases the production client's resources.
        withTimeout(1_000) { client.engine.coroutineContext.job.join() } // Checks closing the client also finishes its owned engine.
    } // Ends the production configuration test.

    /** Checks the base URL, JSON content type, null omission and extra response fields together. */
    @Test // Exercises the converter through the complete request pipeline.
    fun relativeRequestUsesGeminiUrlAndConfiguredJson() = runTest { // Runs a POST against a simulated endpoint.
        val engine = mockEngine { request -> // Inspects what would be sent over the network.
            assertEquals("https://generativelanguage.googleapis.com/v1beta/test", request.url.toString()) // Checks relative endpoint resolution.
            assertEquals(ContentType.Application.Json, request.body.contentType?.withoutParameters()) // Checks the serialized body is marked as JSON.
            val fields = Json.parseToJsonElement((request.body as TextContent).text).jsonObject // Examines only the harmless test object.
            assertFalse(fields.containsKey("optional")) // Checks explicit nulls are not serialized.
            respond("""{"text":"answer","extra":"ignored"}""", headers = jsonHeaders()) // Adds a field unknown to the test serializer.
        } // Ends the simulated response handler.
        HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Applies the production configuration to MockEngine.
            val response = client.post("v1beta/test") { setBody(TestPayload("question")) }.body<TestPayload>() // Sends and decodes using ContentNegotiation.
            assertEquals("answer", response.text) // Checks the known field survives decoding.
            assertNull(response.optional) // Checks a missing optional field is accepted.
        } // Closes the simulated client.
    } // Ends the JSON and URL test.

    /** Checks the engine receives the agreed connection and socket timeout durations. */
    @Test // Verifies timeout propagation rather than waiting for a real server.
    fun requestsCarryConnectionAndSocketTimeouts() = runTest { // Executes a request through the timeout plugin.
        val engine = mockEngine { request -> // Reads the settings attached to the outgoing request.
            val timeouts = request.getCapabilityOrNull(HttpTimeoutCapability) // Gets the engine's timeout configuration.
            assertNotNull(timeouts) // Checks HttpTimeout attached its settings.
            assertEquals(10_000L, timeouts?.connectTimeoutMillis) // Checks the ten-second connection limit.
            assertEquals(60_000L, timeouts?.socketTimeoutMillis) // Checks the sixty-second inactivity limit.
            respond("ok") // Finishes the request immediately.
        } // Ends the timeout inspection.
        HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Uses the same settings as the app.
            client.get("v1beta/test").bodyAsText() // Makes the request reach MockEngine.
        } // Closes the client.
    } // Ends the timeout configuration test.

    /** Checks HTTP failures remain inspectable responses for the later typed result mapper. */
    @Test // Covers every status family required by the assignment.
    fun httpErrorsRemainResponses() = runTest { // Runs the configured client without implementing the future data source.
        val statuses = listOf(400, 401, 403, 429, 500, 503) // Lists representative client and server errors.
        for (code in statuses) { // Checks every listed status with its own simulated response.
            val engine = mockEngine { respond("error", HttpStatusCode.fromValue(code)) } // Returns the chosen HTTP failure.
            HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Configures expectSuccess as in production.
                val response = client.get("v1beta/test") // Obtains the response instead of a status exception.
                assertEquals(code, response.status.value) // Checks the failure status was preserved.
                assertEquals("error", response.bodyAsText()) // Checks the body remains available to the data layer.
            } // Closes this status test's client.
        } // Ends the status loop.
    } // Ends the HTTP error test.

    /** Checks a redirect cannot automatically forward the fictitious credential to another host. */
    @Test // Tests the redirect setting through actual request behaviour.
    fun redirectsAreNotFollowed() = runTest { // Uses an in-memory redirect response.
        var requests = 0 // Counts outgoing calls without recording their details.
        val engine = mockEngine { // Simulates the first server asking for a different address.
            requests += 1 // Records how many calls reached the engine.
            respond("redirect", HttpStatusCode.TemporaryRedirect, headersOf(HttpHeaders.Location, "https://example.org/other")) // Supplies a cross-host redirect.
        } // Ends the redirect handler.
        HttpClientFactory.create(false, engine, TestLogger()).use { client -> // Uses the production redirect policy.
            val response = client.get("v1beta/test") { header("x-goog-api-key", UUID.randomUUID().toString()) } // Adds an in-memory fictitious credential.
            assertEquals(HttpStatusCode.TemporaryRedirect, response.status) // Checks the original response reaches the caller.
            assertEquals(1, requests) // Checks no redirected request was sent.
        } // Closes the redirect test client.
    } // Ends the redirect test.

    /** Checks debug diagnostics hide query values, headers, both bodies and response metadata. */
    @Test // Uses generated fictitious sensitive content, never a Gemini credential.
    fun debugLoggingNeverIncludesRequestOrResponseDetails() = runTest { // Exercises successful request and response logging.
        val dummy = UUID.randomUUID().toString() // Creates a runtime-only value to detect accidental disclosure.
        val output = TestLogger() // Captures messages after the factory's safe logger.
        val engine = mockEngine { // Returns sensitive test content in both headers and body.
            respond(dummy, headers = headersOf("x-test-detail", dummy)) // Keeps all test details in memory.
        } // Ends the simulated response.
        HttpClientFactory.create(true, engine, output).use { client -> // Enables the debug logging path.
            assertNotNull(client.pluginOrNull(Logging)) // Checks the debug plugin is installed.
            client.post("v1beta/test") { // Sends several possible sources of sensitive diagnostic text.
                header("x-goog-api-key", dummy) // Adds the fictitious value to a credential header.
                parameter("key", dummy) // Adds it to the URL to test the INFO-level logging risk.
                setBody(TestPayload(dummy)) // Adds it to a serialized request body.
            }.bodyAsText() // Receives the simulated sensitive response body.
            assertTrue("Debug activity must be logged", output.messages.size >= 2) // Checks both request and response diagnostics ran.
            assertTrue("Logs must hide sensitive values", output.messages.none { it.contains(dummy) }) // Avoids printing the generated value on failure.
            assertTrue("Logs must contain only fixed safe text", output.messages.all { it == "DroidMentor HTTP activity (details omitted)" }) // Rejects any URL, body, header or metadata text.
        } // Closes the debug client.
    } // Ends the successful logging privacy test.

    /** Checks debug logs also hide sensitive text from an exception and its request URL. */
    @Test // Exercises the logging failure path with an in-memory IOException.
    fun debugLoggingNeverIncludesExceptionDetails() = runTest { // Simulates a failed request rather than contacting a server.
        val dummy = UUID.randomUUID().toString() // Generates a fictitious sensitive value in memory.
        val output = TestLogger() // Captures only the final safe log messages.
        val engine = mockEngine { throw IOException(dummy) } // Simulates exception text that must never enter logs.
        HttpClientFactory.create(true, engine, output).use { client -> // Enables diagnostics on the failing request.
            var failed = false // Records the expected network failure without storing its message.
            try { // Calls the configured client and catches the test-only failure.
                client.get("v1beta/test") { parameter("key", dummy) } // Also puts the fictitious value in the request URL.
            } catch (error: IOException) { // Leaves typed network error mapping to its own M2 task.
                failed = true // Records that the simulated failure reached the data-layer caller.
            } // Ends the expected failure handling.
            assertTrue("The simulated request must fail", failed) // Checks the failure path was exercised.
            assertTrue("The failure path must produce debug activity", output.messages.size >= 2) // Checks request and failure diagnostics ran.
            assertTrue("Failure logs must hide sensitive details", output.messages.none { it.contains(dummy) }) // Uses a boolean assertion without displaying the value.
            assertTrue("Failure logs must contain only fixed safe text", output.messages.all { it == "DroidMentor HTTP activity (details omitted)" }) // Checks no exception text passed through.
        } // Closes the failing client.
    } // Ends the exception logging privacy test.

    /** Checks the release configuration has no logging plugin and emits no messages. */
    @Test // Exercises a request using the release configuration.
    fun releaseLoggingIsAbsent() = runTest { // Checks release behaviour independently of the current test variant.
        val output = TestLogger() // Records any unexpected log attempt.
        val engine = mockEngine { respond("ok") } // Completes a harmless simulated request.
        HttpClientFactory.create(false, engine, output).use { client -> // Disables the plugin as in a release build.
            assertNull(client.pluginOrNull(Logging)) // Checks Logging was not installed.
            client.get("v1beta/test").bodyAsText() // Exercises the complete request and response path.
            assertTrue(output.messages.isEmpty()) // Checks the supplied output received no message.
        } // Closes the release client.
    } // Ends the release logging test.

    private fun mockEngine(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) = MockEngine.config { // Makes Ktor own the simulated engine as it owns the app engine.
        addHandler(handler) // Registers the simulated server behaviour for this test.
    } // Ends the engine factory helper.

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()) // Marks simulated JSON responses for decoding.

    @Serializable // Generates only the serializer needed to check client configuration.
    private data class TestPayload( // Keeps Gemini DTO implementation outside this task.
        val text: String, // Represents one harmless text field used by the converter test.
        val optional: String? = null // Tests omission and decoding of nullable fields.
    ) // Ends the test-only payload.

    private class TestLogger : Logger { // Captures messages after the production privacy filter.
        val messages = mutableListOf<String>() // Stores only final diagnostic output in memory.

        /** Collects the message sent to the final log destination. */
        override fun log(message: String) { // Does not print messages to the console or a file.
            messages.add(message) // Lets tests check that the output is fixed and safe.
        } // Ends the log collector.
    } // Ends the test logger.
} // Ends the factory test class.
