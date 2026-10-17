package pt.isel.pdm.droidmentor.data.repository // Groups the app's repository contracts.

import kotlinx.coroutines.flow.Flow // Provides asynchronous observation of stored settings.

/** Reads, saves and removes the Gemini key without exposing its storage implementation. */
interface SettingsRepository { // Allows a fake repository to be supplied in ViewModel tests later.
    /** Observes the key, or null when missing or unavailable; inspect [readResult] for read errors. */
    val apiKey: Flow<String?> // Keeps the key available only as an in-memory value read from its store.

    /** Observes successful reads and distinguishes storage errors from an unconfigured key. */
    val readResult: Flow<SettingsReadResult> // Gives the UI an explicit error outcome without an exception.

    /** Saves a nonblank key through the preferences implementation. */
    suspend fun saveApiKey(value: String): SettingsWriteResult // Returns a typed outcome after the write completes.

    /** Removes the stored key while preserving any other preferences in the file. */
    suspend fun clearApiKey(): SettingsWriteResult // Returns a typed outcome after deletion completes.
} // Ends the settings repository interface.
