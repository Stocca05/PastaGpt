package pt.isel.pdm.droidmentor.data.remote // Places the client configuration in the network data layer.

import io.ktor.client.HttpClient // Provides the only HTTP client used by the app.
import io.ktor.client.engine.HttpClientEngineFactory // Lets tests supply a factory for Ktor's in-memory engine.
import io.ktor.client.engine.okhttp.OkHttp // Uses OkHttp only through the Ktor engine.
import io.ktor.client.plugins.DefaultRequest // Applies the common Gemini address to relative requests.
import io.ktor.client.plugins.HttpTimeout // Configures connection and socket time limits.
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation // Converts request and response bodies using the configured serializer.
import io.ktor.client.plugins.logging.DEFAULT // Provides Ktor's platform logger for the fixed debug message.
import io.ktor.client.plugins.logging.LogLevel // Selects logging without headers or bodies.
import io.ktor.client.plugins.logging.Logger // Defines where safe debug messages are written.
import io.ktor.client.plugins.logging.Logging // Adds diagnostics only to debug clients.
import io.ktor.http.ContentType // Provides the JSON media type.
import io.ktor.http.contentType // Sets the common request content type.
import io.ktor.serialization.kotlinx.json.json // Registers kotlinx.serialization as the JSON converter.
import kotlinx.serialization.json.Json // Configures JSON encoding and decoding.
import pt.isel.pdm.droidmentor.BuildConfig // Reads the DEBUG flag generated separately for each build variant.

/** Creates the Ktor client that the manual service locator will own and close. */
object HttpClientFactory { // Keeps the shared network configuration in one simple place.

    /** Creates an app client with the OkHttp engine and logging controlled by the build variant. */
    fun create(): HttpClient { // Exposes the production factory without accepting credentials.
        return create(BuildConfig.DEBUG, OkHttp, Logger.DEFAULT) // Lets Ktor create and own the engine using the configuration tested with MockEngine.
    } // Ends the production factory.

    /** Applies the app configuration to an injected engine factory and log destination for local tests. */
    internal fun create(isDebug: Boolean, engineFactory: HttpClientEngineFactory<*>, logOutput: Logger): HttpClient { // Allows tests to run without a server or Android logger.
        return HttpClient(engineFactory) { // Lets the client own and close the engine it creates.
            expectSuccess = false // Leaves HTTP error statuses available for the data source's typed result mapping.
            followRedirects = false // Prevents a redirect from forwarding a credential header to another address.

            install(ContentNegotiation) { // Enables automatic JSON conversion for serializable objects.
                json(Json { // Uses only kotlinx.serialization to encode and decode bodies.
                    ignoreUnknownKeys = true // Accepts extra response fields introduced by the server.
                    explicitNulls = false // Omits null fields from outgoing JSON objects.
                }) // Finishes the JSON configuration.
            } // Finishes the content converter setup.

            install(HttpTimeout) { // Applies these limits to every request unless a request overrides them.
                connectTimeoutMillis = 10_000 // Allows ten seconds to establish the connection.
                socketTimeoutMillis = 60_000 // Allows sixty seconds of inactivity while exchanging data.
            } // Finishes the timeout setup.

            install(DefaultRequest) { // Supplies defaults for later Gemini endpoint requests.
                url("https://generativelanguage.googleapis.com/") // Keeps the base address independent of any key or model name.
                contentType(ContentType.Application.Json) // Sends serialized request bodies as JSON.
            } // Finishes the common request setup.

            if (isDebug) { // Installs no logging plugin in the release variant.
                install(Logging) { // Records only the occurrence of HTTP activity in debug builds.
                    level = LogLevel.INFO // Avoids collecting request and response headers or bodies.
                    logger = object : Logger { // Replaces Ktor's diagnostic text before passing it to the log destination.
                        /** Writes a fixed message without using URLs, payloads or exception details. */
                        override fun log(message: String) { // Ignores incoming text because even an INFO URL can contain a credential.
                            logOutput.log("DroidMentor HTTP activity (details omitted)") // Emits no data taken from the request or response.
                        } // Ends the safe log method.
                    } // Ends the safe logger.
                } // Finishes the debug logging setup.
            } // Ends the build-variant check.
        } // Finishes building the configured client.
    } // Ends the testable factory.
} // Ends the client factory.
