package pt.isel.pdm.droidmentor.data.repository // Keeps local repository acknowledgements separate from remote API outcomes.

/** A Room-backed read or a storage failure without raw exceptions. */
sealed interface ChatReadResult<out T> { // Lets the UI distinguish saved data from an unreadable database.
    /**
     * Data read from Room.
     * @property value The current persisted domain snapshot.
     */
    data class Success<T>(val value: T) : ChatReadResult<T> { // Contains only data obtained from the local database.
        /** Hides message content when a read result is converted to diagnostic text. */
        override fun toString(): String = "ChatReadResult.Success(value=<hidden>)" // Avoids accidental disclosure of saved conversation text.
    } // Ends the Room read result.

    /** The database could not be read; this is not an empty history. */
    data object StorageError : ChatReadResult<Nothing> // Exposes no SQLite exception or raw data.
} // Ends the safe read outcomes.

/** Local command acknowledgement; network answers and failures are observed from Room instead. */
sealed interface ChatCommandResult { // Contains no ApiResult, model answer or raw network error.
    /**
     * A new chat was persisted and can be opened.
     * @property chatId The generated local chat ID used for navigation.
     */
    data class Created(val chatId: String) : ChatCommandResult // Returns the ID only after the chat insert completed.

    /** The requested local change or settled send outcome was saved successfully. */
    data object Stored : ChatCommandResult // Does not indicate whether the network attempt succeeded; read its saved message state.

    /** A supplied ID, title or message text is unusable. */
    data object InvalidInput : ChatCommandResult // Stops before changing history.

    /** The requested chat or retry row no longer exists. */
    data object NotFound : ChatCommandResult // Reports missing local data without a network request.

    /** Another operation currently owns this chat. */
    data object Busy : ChatCommandResult // Rejects a concurrent send instead of queuing a new user turn.

    /** A saved SENDING or FAILED row blocks a new user turn. */
    data object UnresolvedMessage : ChatCommandResult // Requires the current attempt to settle or the failed row to be retried.

    /** No configured key is available before the optimistic write. */
    data object MissingApiKey : ChatCommandResult // Leaves the draft and saved history unchanged.

    /** Key preferences could not be read before the optimistic write. */
    data object SettingsError : ChatCommandResult // Distinguishes unavailable storage from a missing key.

    /** Connectivity is unavailable before the optimistic write. */
    data object Offline : ChatCommandResult // Leaves the draft available without appending a failed placeholder.

    /** The saved history cannot form a complete stateless request. */
    data object InvalidHistory : ChatCommandResult // Never repairs or filters corrupted prior turns silently.

    /** The selected row is not the chat's final unresolved failed user turn. */
    data object InvalidRetry : ChatCommandResult // Refuses replaying model or completed messages.

    /** A local write could not complete; no raw persistence exception is returned. */
    data object StorageError : ChatCommandResult // Gives the UI a local failure acknowledgement without a network payload.
} // Ends local command outcomes.
