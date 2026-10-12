package pt.isel.pdm.droidmentor.data.connectivity // Contains the Android implementation of the app's network observation.

import android.content.Context // Supplies the application-owned connectivity service.
import android.net.ConnectivityManager // Reads the default network and registers its callbacks.
import android.net.Network // Identifies the network supplied by each Android callback.
import android.net.NetworkCapabilities // Reads INTERNET and VALIDATED flags from Android.
import kotlinx.coroutines.CancellationException // Preserves cancellation if callback registration is interrupted.
import kotlinx.coroutines.Dispatchers // Runs synchronous Android service access away from the UI thread.
import kotlinx.coroutines.channels.awaitClose // Releases the network callback when collection is cancelled.
import kotlinx.coroutines.flow.Flow // Exposes connection changes as a cold coroutine stream.
import kotlinx.coroutines.flow.callbackFlow // Bridges Android callbacks into a safe channel-backed Flow.
import kotlinx.coroutines.flow.conflate // Keeps the latest connection status for slow collectors.
import kotlinx.coroutines.flow.distinctUntilChanged // Avoids repeated equivalent status updates.
import kotlinx.coroutines.flow.flowOn // Moves registration, snapshots and cleanup to the IO dispatcher.
import kotlinx.coroutines.withContext // Makes the immediate preflight connection check safe to call from the main dispatcher.
import pt.isel.pdm.droidmentor.domain.ConnectivityStatus // Returns only the app's framework-independent connection state.

/**
 * Observes the default network used by ordinary Ktor requests and provides a fresh preflight check.
 *
 * @param context Context used only to obtain the application connectivity service.
 */
class ConnectivityObserver(context: Context) { // Can be held by the manual service locator without retaining an Activity.
    private val manager: ConnectivityManager? = context.applicationContext.getSystemService(ConnectivityManager::class.java) // Stores only the system service, allowing a missing service to fail closed.

    /** Registers one default-network callback per collector and removes it when that collector stops. */
    val status: Flow<ConnectivityStatus> = callbackFlow { // Starts observation only while the caller collects the stream.
        val service = manager // Reads the application-owned service for this collection.
        if (service == null) { // Handles a device or test context without the connectivity service.
            trySend(ConnectivityStatus.DISCONNECTED) // Reports no usable network without throwing to the UI.
            close() // Finishes the stream because no callback can be registered.
            return@callbackFlow // Stops before creating a subscription.
        } // Ends the missing-service check.
        val tracker = DefaultNetworkTracker { trySend(it) } // Publishes ordered callback states without suspending the Android callback thread.
        val callback = object : ConnectivityManager.NetworkCallback() { // Listens only to changes in the app's default network.
            /** Waits for capability data instead of treating availability as internet validation. */
            override fun onAvailable(network: Network) { // Receives the new default network identity.
                tracker.available(network.networkHandle) // Starts tracking it without querying synchronous APIs inside the callback.
            } // Ends availability handling.

            /** Uses capability data delivered by Android to update the current default network. */
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { // Receives the actual event flags without a second potentially stale query.
                tracker.capabilities(network.networkHandle, capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET), capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) // Requires validated internet and ignores older networks.
            } // Ends capability change handling.

            /** Does not let a late loss from an old network disconnect a newer default network. */
            override fun onLost(network: Network) { // Receives the identity of the default network that was lost.
                tracker.lost(network.networkHandle) // Updates status only if that identity still matches the current network.
            } // Ends loss handling.
        } // Ends the Android callback bridge.
        try { // Registers before taking the initial snapshot so transitions are not missed between the two steps.
            service.registerDefaultNetworkCallback(callback) // Tracks the route used by normal Ktor requests instead of every secondary network.
        } catch (error: CancellationException) { // Keeps coroutine cancellation as normal control flow.
            throw error // Lets cancellation stop collection normally.
        } catch (error: Exception) { // Handles unavailable permissions or callback registration failures without logging system details.
            trySend(ConnectivityStatus.DISCONNECTED) // Fails closed when reliable observation cannot be started.
            close() // Finishes the failed subscription without leaking an exception to collectors.
            return@callbackFlow // Skips cleanup because no callback was successfully registered.
        } // Ends safe callback registration.
        val initial = snapshot() // Reads the current default network after registration on the IO dispatcher.
        tracker.seed(initial.first, initial.second) // Uses the snapshot only if no newer callback was already handled.
        awaitClose { // Runs even when a collector takes one value and immediately cancels.
            try { // Releases the one callback owned by this collection.
                service.unregisterNetworkCallback(callback) // Prevents accumulating callbacks across screens or repeated subscriptions.
            } catch (error: Exception) { // Handles a callback already removed by the system without exposing system exception text.
                Unit // Keeps cancellation cleanup from producing a UI error.
            } // Ends callback cleanup.
        } // Ends the subscription lifetime.
    }.distinctUntilChanged().conflate().flowOn(Dispatchers.IO) // Emits only changes, keeps the latest value and performs service access outside the main thread.

    /** Checks the current default network directly, without requiring a status collector or trusting a cached value. */
    suspend fun isConnected(): Boolean = withContext(Dispatchers.IO) { // Allows repository and data-source checks from any caller dispatcher.
        snapshot().second == ConnectivityStatus.CONNECTED // Requires both capability flags in a fresh system snapshot.
    } // Ends the immediate send prerequisite check.

    private fun snapshot(): Pair<Long?, ConnectivityStatus> { // Returns only the current network handle and a safe status.
        val service = manager ?: return null to ConnectivityStatus.DISCONNECTED // Fails closed if the system service is unavailable.
        return try { // Contains ordinary Android service failures in the connectivity component.
            val network = service.activeNetwork ?: return null to ConnectivityStatus.DISCONNECTED // Uses only the default network that ordinary HTTP would use.
            val capabilities = service.getNetworkCapabilities(network) // Reads the flags for that specific default network.
            val validated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) // Requires declared internet and successful Android validation.
            if (service.activeNetwork != network) { // Detects a handover that happened while the capabilities were being read.
                null to ConnectivityStatus.DISCONNECTED // Avoids returning a positive check for a network that is no longer default.
            } else { // Handles a snapshot whose network identity is still current.
                network.networkHandle to if (validated) ConnectivityStatus.CONNECTED else ConnectivityStatus.DISCONNECTED // Keeps unvalidated or missing capabilities offline.
            } // Ends snapshot identity checking.
        } catch (error: Exception) { // Handles system-service or permission failures without persisting their details.
            null to ConnectivityStatus.DISCONNECTED // Never assumes online when the current network cannot be checked.
        } // Ends safe system snapshot reading.
    } // Ends the fresh default-network snapshot helper.
} // Ends Android connectivity observation.
