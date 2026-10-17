package pt.isel.pdm.droidmentor.domain // Keeps the app's connection state independent of Android APIs.

/** Whether the app's default network currently offers validated internet access. */
enum class ConnectivityStatus { // Exposes a small state model for the repository and future offline banner.
    /** The default network has both INTERNET and VALIDATED capabilities. */
    CONNECTED, // Allows an attempted send while normal transport failures remain possible.

    /** No validated default internet network is currently known. */
    DISCONNECTED, // Blocks sending until the connection becomes usable.
} // Ends the connection status model.
