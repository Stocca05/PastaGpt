package pt.isel.pdm.droidmentor.ui.navigation // Groups the destinations shared by the app's navigation graph.

import android.net.Uri // Encodes a chat ID as one safe route segment without changing the stored ID.

/** Defines the five M2 destinations; only the local chat ID is passed to the active conversation. */
object DroidMentorRoutes { // Keeps route names and argument names in one place.
    /** Main entry destination while active-session restoration remains outside M2. */
    const val TITLE = "Title" // Names the root of the M2 back stack.

    /** Destination for the locally saved conversation list. */
    const val CHAT_HISTORY = "ChatHistory" // Names the screen that opens new or existing chats later.

    /** Required argument containing the local Room conversation ID. */
    const val CHAT_ID = "chatId" // Passes neither message content nor a Gemini credential through navigation.

    /** Active conversation route pattern registered by the graph. */
    const val ACTIVE_CHAT = "ActiveChat/{chatId}" // Requires a specific local chat ID instead of a global current-chat field.

    /** Destination for application and author information. */
    const val ABOUT = "About" // Names the app information destination.

    /** Destination for the key configuration screen. */
    const val SETTINGS = "Settings" // Names the settings destination without embedding preference values.

    /** Builds an active-chat route while preserving reserved characters in the local ID. */
    fun activeChat(chatId: String): String { // Accepts an opaque local ID supplied by the history screen.
        require(chatId.isNotBlank()) { "A chat route requires a nonblank local ID" } // Rejects programmer mistakes before building an unusable route.
        return "ActiveChat/${Uri.encode(chatId)}" // Keeps slashes, percent signs and query characters inside one encoded segment.
    } // Ends active-chat route construction.
} // Ends the shared navigation destinations.
