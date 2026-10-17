package pt.isel.pdm.droidmentor.data.connectivity // Keeps callback ordering logic beside the Android connectivity wrapper.

import pt.isel.pdm.droidmentor.domain.ConnectivityStatus // Emits only the app's safe connected or disconnected state.

/** Tracks default-network events without Android objects so handover races can be tested locally. */
internal class DefaultNetworkTracker(private val emit: (ConnectivityStatus) -> Unit) { // Receives the observer's non-blocking status publisher.
    private val lock = Any() // Protects initial snapshots and callback events that can arrive on different threads.
    private var networkId: Long? = null // Identifies only the current default network for this subscription.
    private var receivedCallback = false // Prevents an older initial snapshot from overwriting a newer callback.

    /** Supplies the initial state only while no network callback has been handled. */
    fun seed(id: Long?, status: ConnectivityStatus) { // Accepts one freshly read default-network snapshot.
        synchronized(lock) { // Keeps the state update and publication together to avoid reordering them.
            if (!receivedCallback) { // Lets callback information take priority over a snapshot read during registration.
                networkId = id // Records the network associated with the supplied initial status.
                emit(status) // Publishes the initial validated or disconnected state.
            } // Ends the initial snapshot guard.
        } // Ends the protected snapshot publication.
    } // Ends initial state handling.

    /** Starts tracking a new default network while waiting for its capability callback. */
    fun available(id: Long) { // Handles the identity supplied by onAvailable.
        synchronized(lock) { // Serialises this event with initial seeding and other callbacks.
            receivedCallback = true // Marks callback data as newer than the initial snapshot.
            networkId = id // Replaces the previous default network during a handover.
            emit(ConnectivityStatus.DISCONNECTED) // Never assumes availability alone means validated internet access.
        } // Ends the protected available event.
    } // Ends new-network handling.

    /** Updates status only for the currently tracked default network. */
    fun capabilities(id: Long, hasInternet: Boolean, isValidated: Boolean) { // Accepts the flags supplied directly by onCapabilitiesChanged.
        synchronized(lock) { // Keeps the identity check and emission atomic with a handover.
            receivedCallback = true // Prevents an older snapshot from replacing this event.
            if (networkId == id) { // Ignores late capability events from a previous default network.
                emit(if (hasInternet && isValidated) ConnectivityStatus.CONNECTED else ConnectivityStatus.DISCONNECTED) // Requires both flags before reporting online.
            } // Ends the current-network identity check.
        } // Ends protected capability handling.
    } // Ends network validation handling.

    /** Reports disconnection only when the lost network is still the tracked default. */
    fun lost(id: Long) { // Handles a default network losing its role or connection.
        synchronized(lock) { // Serialises loss with handover events and initial seeding.
            receivedCallback = true // Stops a snapshot read before this event from restoring a lost connection.
            if (networkId == id) { // Ignores a delayed loss event from an older default network.
                networkId = null // Clears the lost default network identity.
                emit(ConnectivityStatus.DISCONNECTED) // Reports no validated connection for the subscription.
            } // Ends current-network loss checking.
        } // Ends protected loss handling.
    } // Ends lost-network handling.
} // Ends default-network callback ordering.
