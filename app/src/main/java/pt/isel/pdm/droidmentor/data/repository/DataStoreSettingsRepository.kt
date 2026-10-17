package pt.isel.pdm.droidmentor.data.repository // Groups the concrete repository implementations.

import androidx.datastore.core.DataStore // Accepts the store supplied by manual dependency injection.
import androidx.datastore.preferences.core.Preferences // Represents preference snapshots.
import androidx.datastore.preferences.core.edit // Performs atomic preference updates.
import androidx.datastore.preferences.core.stringPreferencesKey // Defines the preference name, not an actual credential.
import kotlinx.coroutines.CancellationException // Keeps coroutine cancellation separate from storage failure.
import kotlinx.coroutines.flow.Flow // Exposes the stored values without blocking the caller.
import kotlinx.coroutines.flow.catch // Converts read failures to a typed result.
import kotlinx.coroutines.flow.distinctUntilChanged // Avoids repeated emissions of an unchanged key.
import kotlinx.coroutines.flow.map // Converts preference snapshots to repository results.

/**
 * Stores the Gemini key exclusively in Preferences DataStore.
 *
 * @param dataStore The single store instance supplied by the app, or an isolated store in tests.
 */
class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository { // Receives its storage dependency by hand.
    /** Observes settings while keeping read errors explicit and credential text hidden. */
    override val readResult: Flow<SettingsReadResult> = dataStore.data // Observes snapshots from DataStore's background reader.
        .map<Preferences, SettingsReadResult> { preferences -> // Allows either a successful read or an error result in the Flow.
            val value = preferences[API_KEY] // Reads only the dedicated key preference into memory.
            val key = value?.takeIf { it.isNotBlank() } // Treats an empty stored value as unconfigured.
            SettingsReadResult.Success(key) // Returns a result whose text representation hides the key.
        } // Ends the snapshot conversion.
        .catch { error -> // Handles storage errors without logging their details.
            if (error is CancellationException) throw error // Lets cancelled coroutine work stop normally.
            emit(SettingsReadResult.StorageError) // Reports failure instead of replacing or clearing the stored file.
        } // Ends the read error handling.

    /** Observes only key changes; the separate result Flow reports any read failure. */
    override val apiKey: Flow<String?> = readResult.map { result -> // Extracts the in-memory value required by the caller.
        when (result) { // Checks the typed read outcome.
            is SettingsReadResult.Success -> result.apiKey // Supplies the successfully read value, including null when absent.
            SettingsReadResult.StorageError -> null // Supplies no credential when storage cannot be read.
        } // Ends the result check.
    }.distinctUntilChanged() // Avoids repeating an unchanged key value.

    /** Saves the trimmed nonblank value and reports failure without returning exception details. */
    override suspend fun saveApiKey(value: String): SettingsWriteResult { // Handles one requested key update.
        val key = value.trim() // Removes accidental whitespace around the input without logging it.
        if (key.isEmpty()) return SettingsWriteResult.EMPTY_KEY // Preserves the previous value when the input is blank.
        return try { // Waits for the asynchronous preference write.
            dataStore.edit { preferences -> // Updates the snapshot atomically on DataStore's IO scope.
                preferences[API_KEY] = key // Writes the credential only to Preferences DataStore.
            } // Ends the edit transaction.
            SettingsWriteResult.SUCCESS // Confirms the completed write.
        } catch (error: CancellationException) { // Recognises cancellation rather than a failed write.
            throw error // Allows the calling coroutine to stop.
        } catch (error: Exception) { // Handles storage exceptions without exposing their messages.
            SettingsWriteResult.STORAGE_ERROR // Returns a safe typed failure to the caller.
        } // Ends the write error handling.
    } // Ends the save method.

    /** Removes only the key preference and reports whether DataStore completed the edit. */
    override suspend fun clearApiKey(): SettingsWriteResult { // Handles one requested deletion.
        return try { // Waits for DataStore to complete the atomic update.
            dataStore.edit { preferences -> // Opens a mutable preference snapshot on the storage scope.
                preferences.remove(API_KEY) // Removes the key without clearing unrelated values.
            } // Ends the edit transaction.
            SettingsWriteResult.SUCCESS // Confirms the key preference was removed.
        } catch (error: CancellationException) { // Keeps cancellation separate from storage errors.
            throw error // Stops cancelled work normally.
        } catch (error: Exception) { // Handles a failed edit without logging exception details.
            SettingsWriteResult.STORAGE_ERROR // Reports the safe failure outcome.
        } // Ends the deletion error handling.
    } // Ends the clear method.

    private companion object { // Keeps the preference name local to this implementation.
        val API_KEY = stringPreferencesKey("gemini_api_key") // Identifies a preference slot; this string is not an API key value.
    } // Ends the preference-name holder.
} // Ends the DataStore repository.
