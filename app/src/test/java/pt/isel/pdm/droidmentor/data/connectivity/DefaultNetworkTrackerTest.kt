package pt.isel.pdm.droidmentor.data.connectivity // Tests network event ordering without Android objects or permissions.

import org.junit.Assert.assertEquals // Compares only safe enum status sequences.
import org.junit.Test // Marks the network event cases as JUnit tests.
import pt.isel.pdm.droidmentor.domain.ConnectivityStatus // Supplies the app's connected and disconnected values.

/** Checks validation, handovers and initial snapshot races using harmless local network identifiers. */
class DefaultNetworkTrackerTest { // Tests the state rules separately from Android callback registration.

    /** Checks an initial snapshot can describe either a validated network or no connection. */
    @Test // Covers both initial states without any callbacks.
    fun initialSnapshotIsPublished() { // Records the initial states supplied by the observer.
        val states = mutableListOf<ConnectivityStatus>() // Captures safe status values only.
        val tracker = DefaultNetworkTracker { states.add(it) } // Uses an in-memory publisher for the test.
        tracker.seed(1, ConnectivityStatus.CONNECTED) // Supplies a validated current default network.
        tracker.seed(null, ConnectivityStatus.DISCONNECTED) // Supplies a later snapshot before any callback was handled.
        assertEquals(listOf(ConnectivityStatus.CONNECTED, ConnectivityStatus.DISCONNECTED), states) // Checks both snapshots are represented accurately.
    } // Ends initial state verification.

    /** Checks network availability alone never reports validated internet access. */
    @Test // Covers the interval before Android supplies capabilities.
    fun availableWaitsForValidatedCapabilities() { // Supplies only the network identity event first.
        val states = mutableListOf<ConnectivityStatus>() // Captures each published event state.
        val tracker = DefaultNetworkTracker { states.add(it) } // Creates a tracker with no previous network.
        tracker.available(1) // Reports a new default network whose capabilities are not yet known.
        tracker.capabilities(1, true, true) // Reports both INTERNET and VALIDATED flags for that same network.
        assertEquals(listOf(ConnectivityStatus.DISCONNECTED, ConnectivityStatus.CONNECTED), states) // Checks validation is required before online status.
    } // Ends availability versus validation verification.

    /** Checks all four capability combinations and rejects captive or non-internet networks. */
    @Test // Covers the complete two-flag truth table.
    fun bothInternetAndValidationAreRequired() { // Varies the declared internet and validation flags independently.
        val cases = listOf(Triple(false, false, ConnectivityStatus.DISCONNECTED), Triple(true, false, ConnectivityStatus.DISCONNECTED), Triple(false, true, ConnectivityStatus.DISCONNECTED), Triple(true, true, ConnectivityStatus.CONNECTED)) // Defines the platform capability contract explicitly.
        for ((internet, validated, expected) in cases) { // Covers every combination with a fresh tracker.
            val states = mutableListOf<ConnectivityStatus>() // Captures output for this single capability case.
            val tracker = DefaultNetworkTracker { states.add(it) } // Creates an independent current-network state.
            tracker.available(1) // Identifies the current network before supplying capabilities.
            tracker.capabilities(1, internet, validated) // Supplies this case's actual flags.
            assertEquals(expected, states.last()) // Checks only the fully validated internet case becomes online.
        } // Ends capability combination verification.
    } // Ends the validation truth table test.

    /** Checks loss and later restoration of validation update the same default network. */
    @Test // Covers a network that remains available while its internet access changes.
    fun capabilityChangesUpdateCurrentNetwork() { // Simulates validation changes without replacing the network identity.
        val states = mutableListOf<ConnectivityStatus>() // Records the observed state changes.
        val tracker = DefaultNetworkTracker { states.add(it) } // Creates an independent subscription state.
        tracker.seed(1, ConnectivityStatus.CONNECTED) // Starts with an already validated default network.
        tracker.capabilities(1, true, false) // Simulates loss of Android validation.
        tracker.capabilities(1, true, true) // Simulates a successfully validated connection again.
        assertEquals(listOf(ConnectivityStatus.CONNECTED, ConnectivityStatus.DISCONNECTED, ConnectivityStatus.CONNECTED), states) // Checks the observer can support offline and recovery feedback.
    } // Ends capability change verification.

    /** Checks losing the current network clears its identity and ignores later stale capabilities. */
    @Test // Covers disconnection followed by a late event for the lost network.
    fun lostCurrentNetworkCannotBeRevivedByStaleCapabilities() { // Starts with one connected default network.
        val states = mutableListOf<ConnectivityStatus>() // Records only status changes, not network metadata.
        val tracker = DefaultNetworkTracker { states.add(it) } // Creates the tracker under test.
        tracker.seed(1, ConnectivityStatus.CONNECTED) // Supplies the initial current network.
        tracker.lost(1) // Removes that default network.
        tracker.capabilities(1, true, true) // Supplies a stale validation event after loss.
        assertEquals(listOf(ConnectivityStatus.CONNECTED, ConnectivityStatus.DISCONNECTED), states) // Checks stale data cannot restore the disconnected network.
    } // Ends current network loss verification.

    /** Checks a delayed loss from the previous default network cannot disconnect the replacement. */
    @Test // Covers a common Wi-Fi/mobile handover ordering.
    fun oldNetworkLossDoesNotDisconnectNewDefault() { // Replaces one validated default network with another.
        val states = mutableListOf<ConnectivityStatus>() // Records the handover's statuses.
        val tracker = DefaultNetworkTracker { states.add(it) } // Creates one subscription state.
        tracker.seed(1, ConnectivityStatus.CONNECTED) // Starts with the old default network.
        tracker.available(2) // Makes the new network the tracked default.
        tracker.capabilities(2, true, true) // Validates the new default network.
        tracker.lost(1) // Delivers a delayed loss event for the old network.
        assertEquals(listOf(ConnectivityStatus.CONNECTED, ConnectivityStatus.DISCONNECTED, ConnectivityStatus.CONNECTED), states) // Checks the replacement remains connected.
    } // Ends stale loss handover verification.

    /** Checks capability changes on an older network cannot change the current default's status. */
    @Test // Covers both stale validation gain and stale validation loss.
    fun oldNetworkCapabilitiesAreIgnored() { // Supplies old-network events after a validated handover.
        val states = mutableListOf<ConnectivityStatus>() // Captures statuses for one current default route.
        val tracker = DefaultNetworkTracker { states.add(it) } // Creates the state tracker.
        tracker.available(1) // Identifies the original default network.
        tracker.available(2) // Replaces it with a new default before validation.
        tracker.capabilities(2, true, true) // Validates only the new network.
        val before = states.toList() // Saves the output before stale events arrive.
        tracker.capabilities(1, true, false) // Supplies an old network's validation loss.
        tracker.capabilities(1, true, true) // Supplies an old network's validation gain.
        assertEquals(before, states) // Checks neither stale event affects the active default.
    } // Ends old-network capability verification.

    /** Checks a snapshot read during registration cannot overwrite a newer available event. */
    @Test // Covers an initial snapshot racing with a handover callback.
    fun availableEventWinsOverAnOlderSnapshot() { // Delivers the new network callback before initial seeding finishes.
        val states = mutableListOf<ConnectivityStatus>() // Captures the published state order.
        val tracker = DefaultNetworkTracker { states.add(it) } // Starts with no initial snapshot.
        tracker.available(2) // Receives the newer default network identity.
        tracker.seed(1, ConnectivityStatus.CONNECTED) // Attempts to publish an older default-network snapshot.
        tracker.capabilities(2, true, true) // Validates the newer default network.
        assertEquals(listOf(ConnectivityStatus.DISCONNECTED, ConnectivityStatus.CONNECTED), states) // Checks the old snapshot cannot replace the newer identity.
    } // Ends initial handover race verification.

    /** Checks an old snapshot cannot restore a connection already lost by callback. */
    @Test // Covers registration and a quick subsequent default-network loss.
    fun lossEventWinsOverAnOlderSnapshot() { // Delivers disconnection before the initial snapshot is published.
        val states = mutableListOf<ConnectivityStatus>() // Records output from the race.
        val tracker = DefaultNetworkTracker { states.add(it) } // Creates the state for this subscription.
        tracker.available(1) // Identifies the network delivered by Android.
        tracker.lost(1) // Loses that network while the snapshot is still being read.
        val before = states.toList() // Saves the output after disconnection.
        tracker.seed(1, ConnectivityStatus.CONNECTED) // Attempts to publish the stale positive snapshot.
        assertEquals(before, states) // Checks the lost connection is not revived.
    } // Ends initial loss race verification.

    /** Checks each subscription has its own network state even when identifiers are large. */
    @Test // Covers independent collectors and the full Long identifier range.
    fun subscriptionsKeepIndependentNetworkState() { // Creates two trackers with separate publishers.
        val firstStates = mutableListOf<ConnectivityStatus>() // Captures the first subscription's output.
        val secondStates = mutableListOf<ConnectivityStatus>() // Captures the second subscription's output.
        val first = DefaultNetworkTracker { firstStates.add(it) } // Owns the first subscription's state.
        val second = DefaultNetworkTracker { secondStates.add(it) } // Owns the second subscription's state.
        first.available(Long.MAX_VALUE) // Supplies a harmless large identifier to the first subscription.
        first.capabilities(Long.MAX_VALUE, true, true) // Validates that network without changing the identifier.
        second.seed(null, ConnectivityStatus.DISCONNECTED) // Starts the second subscription with no default network.
        second.lost(Long.MAX_VALUE) // Sends an irrelevant loss to the independent second tracker.
        assertEquals(ConnectivityStatus.CONNECTED, firstStates.last()) // Checks the first tracker remains connected.
        assertEquals(listOf(ConnectivityStatus.DISCONNECTED), secondStates) // Checks the second tracker did not share the first tracker’s identity.
    } // Ends subscription independence verification.
} // Ends local default-network event tests.
