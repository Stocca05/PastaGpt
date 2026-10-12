package pt.isel.pdm.droidmentor.data.repository // Groups the repository contracts and storage results.

/** Describes a settings read without passing storage exceptions to the UI. */
sealed interface SettingsReadResult { // Lists the possible read outcomes.
    /**
     * A successful read; a null value means the key has not been configured.
     * @property apiKey The value read into memory, or null when absent.
     */
    class Success(val apiKey: String?) : SettingsReadResult { // Holds the key only in memory after reading DataStore.
        /** Hides the key if a caller accidentally converts this result to text. */
        override fun toString(): String { // Avoids a generated string that could contain credentials.
            return "SettingsReadResult.Success(key=<hidden>)" // Describes the outcome without including the stored value.
        } // Ends the safe text representation.
    } // Ends the successful read result.

    /** The preferences could not be read; no key value or exception details are exposed. */
    data object StorageError : SettingsReadResult // Represents an unreadable store instead of pretending it is empty.
} // Ends the read result types.

/** Describes whether a save or clear operation completed. */
enum class SettingsWriteResult { // Lists outcomes safe to display or inspect without credentials.
    /** DataStore completed the requested update. */
    SUCCESS, // Confirms the preference update finished successfully.

    /** The supplied key contained no non-whitespace characters. */
    EMPTY_KEY, // Rejects an empty value without changing existing preferences.

    /** The preferences could not be updated. */
    STORAGE_ERROR, // Reports storage failure without returning exception messages.
} // Ends the write result types.
