package pt.isel.pdm.droidmentor.data.connectivity // Tests actual Android service access and callback lifetime.

import android.content.Context // Provides the target application's service context.
import android.net.ConnectivityManager // Reads independent platform state for the snapshot check.
import android.net.NetworkCapabilities // Names the internet and validation flags used by Android.
import android.os.Build // Restricts network toggling to an emulator rather than a user's physical phone.
import android.os.ParcelFileDescriptor // Closes test shell streams together with their owned descriptors.
import androidx.test.ext.junit.runners.AndroidJUnit4 // Runs these tests through Android instrumentation.
import androidx.test.platform.app.InstrumentationRegistry // Provides the application context and shell automation.
import io.ktor.client.engine.config // Creates a client-owned in-memory HTTP engine.
import io.ktor.client.engine.mock.MockEngine // Ensures the network gate test never contacts Gemini.
import io.ktor.client.engine.mock.respond // Builds the simulated response after connectivity is restored.
import io.ktor.http.ContentType // Marks the simulated successful response as JSON.
import io.ktor.http.HttpHeaders // Names the simulated response Content-Type header.
import io.ktor.http.headersOf // Creates that response header.
import java.util.UUID // Generates a fictitious credential for the in-memory HTTP test.
import kotlinx.coroutines.CompletableDeferred // Passes the main-dispatcher check result back to the test thread.
import kotlinx.coroutines.Dispatchers // Starts a caller on the main dispatcher to check the suspend API.
import kotlinx.coroutines.cancelAndJoin // Waits for callback cleanup before a test finishes.
import kotlinx.coroutines.channels.Channel // Records real status changes while Wi-Fi and data are toggled.
import kotlinx.coroutines.flow.first // Cancels each one-value subscription automatically.
import kotlinx.coroutines.launch // Collects real Android callback events in a child coroutine.
import kotlinx.coroutines.runBlocking // Bridges JUnit instrumentation to suspend operations only in test code.
import kotlinx.coroutines.withTimeout // Gives every callback wait a clear bounded failure time.
import org.junit.Assert.assertEquals // Compares safe status values and request counts.
import org.junit.Assert.assertFalse // Checks offline preflight reports no validated network.
import org.junit.Assert.assertTrue // Checks callback operation and recovery without printing credentials.
import org.junit.Assume.assumeTrue // Prevents the network toggle test from changing a physical phone's connection.
import org.junit.Test // Marks instrumented connectivity verification cases.
import org.junit.runner.RunWith // Selects the Android test runner.
import pt.isel.pdm.droidmentor.data.remote.ApiResult // Checks the safe network outcomes used by the source.
import pt.isel.pdm.droidmentor.data.remote.GeminiRemoteDataSource // Uses the actual source's mandatory connectivity gate.
import pt.isel.pdm.droidmentor.data.remote.HttpClientFactory // Supplies the real client configuration around MockEngine.
import pt.isel.pdm.droidmentor.domain.ConnectivityStatus // Identifies actual observed connected/disconnected states.
import pt.isel.pdm.droidmentor.domain.ConversationTurn // Supplies a valid test question for the simulated call.
import pt.isel.pdm.droidmentor.domain.Role // Identifies the user turn in that question.

/** Verifies fresh checks, callback cleanup and offline/recovery events on Android. */
@RunWith(AndroidJUnit4::class) // Runs against the connected device's real network service.
class ConnectivityObserverTest { // Uses no real credential and sends no external HTTP request.
    private val instrumentation = InstrumentationRegistry.getInstrumentation() // Accesses instrumentation-owned test facilities.
    private val context = instrumentation.targetContext.applicationContext // Uses the application context rather than retaining an Activity.

    /** Checks the fresh suspend API can be called from the main dispatcher without needing a collector. */
    @Test // Compares the component with the platform's current validated default network.
    fun mainDispatcherCheckMatchesPlatformSnapshot() = runBlocking { // Runs the JUnit test on its instrumentation worker thread.
        val manager = context.getSystemService(ConnectivityManager::class.java) // Reads the same Android service independently of the observer.
        val network = manager.activeNetwork // Gets the current default route for this test snapshot.
        val capabilities = network?.let { manager.getNetworkCapabilities(it) } // Reads capabilities only when a default network exists.
        val expected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) // Computes the documented platform prerequisite.
        val result = CompletableDeferred<Boolean>() // Carries only the checked connection flag.
        val caller = launch(Dispatchers.Main) { // Invokes the observer from an actual main-dispatcher coroutine.
            result.complete(ConnectivityObserver(context).isConnected()) // Lets the implementation perform its synchronous reads on IO.
        } // Ends the main-dispatcher caller.
        assertEquals(expected, withTimeout(5_000) { result.await() }) // Checks current validation without depending on an active status subscription.
        caller.join() // Waits for the caller to finish before returning from the test.
    } // Ends the main-safe fresh snapshot test.

    /** Checks repeatedly cancelling subscriptions releases callbacks rather than hitting Android's callback limit. */
    @Test // Creates more successive subscriptions than the platform's simultaneous callback budget.
    fun cancelledCollectorsReleaseTheirNetworkCallbacks() = runBlocking { // Uses real registrations and automatic first-value cancellation.
        val observer = ConnectivityObserver(context) // Reuses one application-owned observer.
        assertTrue("This callback lifetime test requires validated emulator internet", observer.isConnected()) // Ensures CONNECTED can distinguish a healthy registration from a failed one.
        withTimeout(30_000) { // Bounds the entire repeated registration verification.
            repeat(110) { // Exceeds the usual 100 callback budget if any subscription leaks its registration.
                assertEquals(ConnectivityStatus.CONNECTED, observer.status.first { it == ConnectivityStatus.CONNECTED }) // Takes validated status and cancels the producer before the next subscription.
            } // Ends repeated callback registration and cancellation.
        } // Ends bounded callback lifetime verification.
    } // Ends subscription cleanup verification.

    /** Checks real offline/recovery events and the source gate while restoring the emulator's original network settings. */
    @Test // Changes emulator networking only and uses MockEngine for every API operation.
    fun emulatorOfflineAndRecoveryGateTheActualSource() = runBlocking { // Collects platform callbacks while changing test emulator connectivity.
        assumeTrue("Network toggles run only on an emulator", Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish") // Leaves physical device networking unchanged.
        val wifiWasEnabled = shell("settings get global wifi_on") != "0" // Saves the emulator's original Wi-Fi setting.
        val dataWasEnabled = shell("settings get global mobile_data") != "0" // Saves its original mobile-data setting.
        val observer = ConnectivityObserver(context) // Uses the actual connectivity implementation.
        val states = Channel<ConnectivityStatus>(Channel.UNLIMITED) // Queues safe enum changes for bounded assertions.
        val collector = launch { observer.status.collect { states.send(it) } } // Registers the real default-network callback.
        var requests = 0 // Counts only calls that pass the connectivity gate.
        val engine = MockEngine.config { // Keeps all HTTP operations inside the test process.
            addHandler { // Runs only if the actual observer permits sending.
                requests += 1 // Records the simulated HTTP attempt.
                respond("""{"candidates":[{"content":{"parts":[{"text":"Reply"}]},"finishReason":"STOP"}]}""", headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) // Returns a harmless in-memory answer.
            } // Ends the simulated response handler.
        } // Ends the mock engine setup.
        try { // Restores networking and removes the callback even if an assertion fails.
            assertTrue("This transition test requires an initially validated connection", observer.isConnected()) // Verifies the starting state before disabling the emulator network.
            awaitStatus(states, observer, ConnectivityStatus.CONNECTED) // Waits for real validated status rather than treating onAvailable as online.
            shell("svc wifi disable") // Disables only the emulator's Wi-Fi service.
            shell("svc data disable") // Disables any emulator mobile-data fallback.
            awaitStatus(states, observer, ConnectivityStatus.DISCONNECTED) // Waits until the event and fresh snapshot both report offline.
            assertFalse(observer.isConnected()) // Checks the immediate send prerequisite also sees no usable route.
            HttpClientFactory.create(false, engine, silentLogger()).use { client -> // Uses the real client around the in-memory engine.
                val source = GeminiRemoteDataSource(client, observer::isConnected) // Wires the actual suspend Android check into the source's required gate.
                val history = listOf(ConversationTurn(Role.USER, "Question")) // Supplies a valid one-turn request independent of the connectivity result.
                val dummy = UUID.randomUUID().toString() // Generates a fictitious credential held only in memory.
                assertEquals(ApiResult.NetworkUnavailable, source.generateContent(history, apiKey = dummy)) // Checks the offline call returns a typed failure.
                assertEquals(0, requests) // Checks no HTTP operation reached even the in-memory engine while offline.
                restoreNetwork(wifiWasEnabled, dataWasEnabled) // Restores the settings that supplied the initial validated connection.
                awaitStatus(states, observer, ConnectivityStatus.CONNECTED) // Waits for Android to validate the recovered default network.
                assertEquals(ApiResult.Success("Reply"), source.generateContent(history, apiKey = dummy)) // Checks the same source can send after recovery.
                assertEquals(1, requests) // Checks exactly one operation reached MockEngine after reconnection.
            } // Closes the test client and its engine.
        } finally { // Handles successful completion, timeout and assertion failures alike.
            collector.cancelAndJoin() // Unregisters the Android callback before ending the test.
            states.close() // Releases the test's queued status observations.
            restoreNetwork(wifiWasEnabled, dataWasEnabled) // Always restores both original settings without touching host networking.
            withTimeout(15_000) { observer.status.first { it == ConnectivityStatus.CONNECTED } } // Confirms a validated connection is restored for later device tests.
        } // Ends mandatory network and callback cleanup.
    } // Ends actual Android offline and recovery integration verification.

    private suspend fun awaitStatus(states: Channel<ConnectivityStatus>, observer: ConnectivityObserver, expected: ConnectivityStatus) { // Rejects stale queued events by also checking current platform state.
        withTimeout(15_000) { // Gives Android's asynchronous network changes a bounded time to complete.
            while (true) { // Waits without polling or fixed sleeps.
                val status = states.receive() // Receives the next real callback or initial snapshot state.
                if (status == expected && observer.isConnected() == (expected == ConnectivityStatus.CONNECTED)) { // Requires the event and the fresh preflight check to agree.
                    break // Finishes only after the requested current state is reached.
                } // Ends stale-event rejection.
            } // Ends status event waiting.
        } // Ends the bounded status wait.
    } // Ends the transition assertion helper.

    private fun restoreNetwork(wifi: Boolean, data: Boolean) { // Restores only the settings captured before this test.
        shell(if (wifi) "svc wifi enable" else "svc wifi disable") // Restores the original emulator Wi-Fi state.
        shell(if (data) "svc data enable" else "svc data disable") // Restores the original emulator mobile-data state.
    } // Ends original-network restoration.

    private fun shell(command: String): String { // Runs test-only local Android service commands without printing their output.
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command) // Uses test shell privileges without adding app permissions.
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText().trim() } // Reads output on the test worker and closes the stream and its descriptor through one owner.
    } // Ends emulator command handling.

    private fun silentLogger() = object : io.ktor.client.plugins.logging.Logger { // Keeps test diagnostics from printing request or credential details.
        /** Ignores messages because this test client has logging disabled. */
        override fun log(message: String) = Unit // Performs no console or file write.
    } // Ends the silent test log destination.
} // Ends actual Android connectivity verification.
