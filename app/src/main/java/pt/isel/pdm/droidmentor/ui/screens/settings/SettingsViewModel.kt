package pt.isel.pdm.droidmentor.ui.screens.settings // Owns settings state and commands outside Compose.

import androidx.lifecycle.ViewModel // Gives settings its screen-owned lifetime.
import androidx.lifecycle.viewModelScope // Cancels collectors and commands when the destination is removed.
import kotlinx.coroutines.CancellationException // Preserves structured coroutine cancellation.
import kotlinx.coroutines.channels.Channel // Buffers one-shot notices without replaying old snackbars.
import kotlinx.coroutines.flow.MutableStateFlow // Holds the latest immutable settings snapshot.
import kotlinx.coroutines.flow.asStateFlow // Exposes read-only state to the screen.
import kotlinx.coroutines.flow.receiveAsFlow // Exposes the event channel for lifecycle-aware collection.
import kotlinx.coroutines.flow.update // Updates state safely when reads and commands complete.
import kotlinx.coroutines.launch // Starts asynchronous repository work in the ViewModel scope.
import pt.isel.pdm.droidmentor.data.repository.SettingsReadResult // Distinguishes unconfigured preferences from read failures.
import pt.isel.pdm.droidmentor.data.repository.SettingsRepository // Injects the settings contract by hand.
import pt.isel.pdm.droidmentor.data.repository.SettingsWriteResult // Handles save and clear outcomes without raw exceptions.
import pt.isel.pdm.droidmentor.di.dependencyViewModelFactory // Uses the existing manual AndroidX factory helper.
import pt.isel.pdm.droidmentor.ui.components.UiEvent // Publishes safe one-shot feedback.
import pt.isel.pdm.droidmentor.ui.components.UiNotice // Identifies resource-backed notices.

/** Immutable settings snapshot; the stored key is represented only by its configured status. */
data class SettingsUiState( // Keeps the new key draft only in memory until the user saves it.
    val draft: String = "", // Holds newly entered input without restoring it through saved state.
    val configured: Boolean = false, // Indicates presence without exposing the stored key.
    val loading: Boolean = true, // Waits for the first typed DataStore snapshot.
    val saving: Boolean = false, // Prevents duplicate writes and draft changes during a write.
    val storageError: Boolean = false, // Distinguishes unreadable preferences from an unconfigured key.
) { // Adds a safe diagnostic representation for input that may contain a credential.
    /** Hides the draft if state is accidentally converted to text. */
    override fun toString(): String = "SettingsUiState(input=<hidden>)" // Never includes a typed key in logs or assertions.
} // Ends the immutable settings state.

/** Observes key presence and performs explicit save/clear operations through the injected repository. */
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() { // Receives no Context, DataStore instance or networking dependency.
    private val mutableState = MutableStateFlow(SettingsUiState()) // Starts with an empty in-memory input field.
    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED) // Holds notices until an active screen collects them.

    /** Read-only current settings state. */
    val state = mutableState.asStateFlow() // Prevents Compose from mutating the backing flow directly.

    /** One-shot save, clear and validation feedback. */
    val events = eventChannel.receiveAsFlow() // Avoids replaying old feedback after recomposition.

    init { // Starts observing storage without performing a save or clear.
        viewModelScope.launch { // Uses repository flows instead of reading preference files on the main thread.
            repository.readResult.collect { result -> // Reads only the configured flag into the UI state.
                mutableState.update { current -> // Preserves any unsaved input while preferences emit.
                    current.copy( // Updates only preference status, keeping the draft unchanged.
                        configured = result is SettingsReadResult.Success && !result.apiKey.isNullOrBlank(), // Stores only the presence flag.
                        loading = false, // Finishes the initial read.
                        storageError = result is SettingsReadResult.StorageError, // Distinguishes a failure from missing configuration.
                    ) // Ends the safe preference snapshot.
                } // Ends the settings snapshot update.
            } // Ends typed preference collection.
        } // Ends the observation job.
    } // Ends initial settings observation.

    /** Updates the input only while no preference write owns it. */
    fun updateDraft(value: String) { // Receives text changes from the password field.
        if (!state.value.saving) mutableState.update { it.copy(draft = value) } // Prevents a late write result from clearing newer input.
    } // Ends draft editing.

    /** Saves the current draft through Preferences DataStore and clears it only on success. */
    fun save() { // Captures one input value for this command.
        write(clear = false) // Runs the shared save/clear command guard.
    } // Ends the save event.

    /** Removes the configured key through the same repository. */
    fun clear() { // Leaves unrelated preference entries to the repository's selective clear operation.
        write(clear = true) // Runs the shared save/clear command guard.
    } // Ends the clear event.

    private fun write(clear: Boolean) { // Keeps duplicate-write prevention and safe failure handling in one small helper.
        if (state.value.saving || state.value.loading) return // Rejects repeated writes or writes before the initial preferences read.
        val input = state.value.draft // Captures the in-memory draft without logging or persisting another copy.
        mutableState.update { it.copy(saving = true) } // Disables conflicting input events before launching work.
        viewModelScope.launch { // Calls only suspend repository operations; DataStore handles its own IO.
            try { // Converts unexpected local failures into safe feedback while preserving cancellation.
                val result = if (clear) repository.clearApiKey() else repository.saveApiKey(input) // Selects the explicitly requested storage operation.
                val notice = when (result) { // Handles the typed result without exposing the supplied key.
                    SettingsWriteResult.SUCCESS -> { // Clears input only after a successful preference write.
                        mutableState.update { it.copy(draft = "") } // Removes the completed draft from memory.
                        if (clear) UiNotice.KEY_CLEARED else UiNotice.KEY_SAVED // Distinguishes the two successful commands.
                    } // Ends success handling.
                    SettingsWriteResult.EMPTY_KEY -> UiNotice.EMPTY_KEY // Retains the input and requests a nonblank value.
                    SettingsWriteResult.STORAGE_ERROR -> UiNotice.STORAGE_ERROR // Retains the draft after an unsuccessful save.
                } // Ends the write-result mapping.
                eventChannel.send(UiEvent.Notice(notice)) // Publishes feedback containing no credential or exception text.
            } catch (cancelled: CancellationException) { // Keeps cancellation available to the screen owner's coroutine scope.
                throw cancelled // Never disguises owner cancellation as a storage failure.
            } catch (_: Exception) { // Protects the screen from an unexpected local repository failure.
                eventChannel.send(UiEvent.Notice(UiNotice.STORAGE_ERROR)) // Reports only a safe storage label.
            } finally { // Re-enables input regardless of the command outcome.
                mutableState.update { it.copy(saving = false) } // Leaves the draft intact unless success already cleared it.
            } // Ends write cleanup.
        } // Ends the settings command job.
    } // Ends guarded preference writing.

    /** Manual production factory using the app's settings repository contract. */
    companion object { // Keeps construction beside the ViewModel that needs it.
        /** Creates a settings ViewModel without a dependency injection framework. */
        val Factory = dependencyViewModelFactory { SettingsViewModel(it.settingsRepository) } // Resolves the existing app container through AndroidX extras.
    } // Ends manual settings construction.
} // Ends the settings ViewModel.
