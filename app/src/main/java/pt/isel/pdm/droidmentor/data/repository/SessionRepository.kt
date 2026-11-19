package pt.isel.pdm.droidmentor.data.repository // Keeps session-restoration preferences separate from API-key settings.

import kotlinx.coroutines.flow.Flow // Exposes persisted active conversation changes.

/** Stores the currently open conversation ID separately from credentials for M3-06 session restoration. */
interface SessionRepository { // Replaceable contract for manual DI and navigation/startup tests.
    /** Emits the saved active conversation ID, or null when no session is active. */
    val activeChatId: Flow<String?> // Never stores or reads API keys.

    /** Reads the saved active conversation ID once, returning null if absent or unreadable. */
    suspend fun readActiveChatId(): String? // Used at launch before selecting the initial destination.

    /** Persists [chatId] as the active session when entering Active Chat. */
    suspend fun setActiveChatId(chatId: String) // Ignores blank IDs safely.

    /** Clears the active session only on explicit user exit or missing conversation cleanup. */
    suspend fun clearActiveChatId() // Never called merely because the OS stopped the process.
} // Ends session repository contract.
