package pt.isel.pdm.droidmentor.data.repository // Implements active-session persistence with Preferences DataStore.

import androidx.datastore.core.DataStore // Reads and writes preference files asynchronously.
import androidx.datastore.preferences.core.Preferences // Holds preference snapshots.
import androidx.datastore.preferences.core.edit // Performs atomic DataStore updates.
import androidx.datastore.preferences.core.stringPreferencesKey // Names the activeChatId preference key.
import java.io.IOException // Handles unreadable preference files gracefully without crashing.
import kotlinx.coroutines.CancellationException // Preserves structured coroutine cancellation.
import kotlinx.coroutines.flow.Flow // Streams activeChatId updates.
import kotlinx.coroutines.flow.catch // Converts storage IO errors into a safe null fallback.
import kotlinx.coroutines.flow.first // Reads the current active session at startup.
import kotlinx.coroutines.flow.map // Extracts and trims the stored conversation ID.

/** Preferences DataStore implementation of [SessionRepository] for M3-06 session restoration. */
class DataStoreSessionRepository( // Uses a dedicated session DataStore separate from API-key storage.
    private val store: DataStore<Preferences>, // Injected manually by DroidMentorApplication or tests.
) : SessionRepository { // Implements session persistence without blocking the main thread.
    /** Emits the saved active chat ID, or null when absent or unreadable. */
    override val activeChatId: Flow<String?> = store.data // Observes the dedicated session store.
        .catch { error -> // Keeps startup safe if the preference file is unreadable.
            if (error is CancellationException) throw error // Preserves cancellation.
            emit(androidx.datastore.preferences.core.emptyPreferences()) // Falls back to no active session (Title screen).
        } // Ends safe read handler.
        .map { prefs -> prefs[ACTIVE_CHAT_ID]?.trim()?.takeIf { it.isNotEmpty() } } // Normalizes blank values to null.

    /** Reads the saved active chat ID once for conditional startup routing. */
    override suspend fun readActiveChatId(): String? { // Safe to call from MainActivity startup.
        return try { // Protects launch against unexpected DataStore IO errors.
            activeChatId.first() // Reads the current snapshot.
        } catch (error: CancellationException) { // Preserves cancellation.
            throw error // Never swallows coroutine cancellation.
        } catch (_: Exception) { // Falls back cleanly to Title if storage fails.
            null // Avoids crashing on startup.
        } // Ends safe startup read.
    } // Ends one-shot session read.

    /** Saves [chatId] when entering an active conversation. */
    override suspend fun setActiveChatId(chatId: String) { // Never clears on process death.
        val normalized = chatId.trim() // Normalizes surrounding whitespace.
        if (normalized.isEmpty()) return // Refuses storing an unusable blank ID.
        try { // Guards against storage write IO errors.
            store.edit { prefs -> prefs[ACTIVE_CHAT_ID] = normalized } // Persists the active chat ID atomically.
        } catch (error: CancellationException) { // Preserves cancellation.
            throw error // Lets coroutine cancellation propagate.
        } catch (_: IOException) { // Keeps UI navigation smooth if preference storage fails.
            Unit // Startup simply falls back to Title if saving failed.
        } // Ends safe session write.
    } // Ends active chat save.

    /** Removes the saved active chat ID only on explicit exit from Active Chat. */
    override suspend fun clearActiveChatId() { // Called on TopAppBar Back, system Back or missing-chat cleanup.
        try { // Guards against storage write IO errors.
            store.edit { prefs -> prefs.remove(ACTIVE_CHAT_ID) } // Clears only the active-session key.
        } catch (error: CancellationException) { // Preserves cancellation.
            throw error // Lets coroutine cancellation propagate.
        } catch (_: IOException) { // Avoids crashing navigation on storage failure.
            Unit // Leaves navigation return functional.
        } // Ends safe session clear.
    } // Ends active chat clear.

    private companion object { // Keeps the preference key private to this repository.
        val ACTIVE_CHAT_ID = stringPreferencesKey("active_chat_id") // Stores only the local Room chat ID.
    } // Ends companion keys.
} // Ends DataStore session repository.
