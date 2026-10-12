package pt.isel.pdm.droidmentor.ui.screens.chathistory // Owns saved conversation state and creation commands.

import androidx.lifecycle.ViewModel // Gives history its navigation-entry lifetime.
import androidx.lifecycle.viewModelScope // Cancels work when the destination is removed.
import kotlinx.coroutines.CancellationException // Preserves owner cancellation.
import kotlinx.coroutines.channels.Channel // Buffers one-shot navigation and feedback events.
import kotlinx.coroutines.flow.MutableStateFlow // Holds an immutable Room-derived screen snapshot.
import kotlinx.coroutines.flow.asStateFlow // Exposes read-only history state.
import kotlinx.coroutines.flow.receiveAsFlow // Exposes buffered events without replay.
import kotlinx.coroutines.flow.update // Preserves independent state fields during updates.
import kotlinx.coroutines.launch // Collects Room and performs local suspend commands.
import pt.isel.pdm.droidmentor.data.repository.ChatCommandResult // Reads the saved new-chat ID from a local acknowledgement.
import pt.isel.pdm.droidmentor.data.repository.ChatReadResult // Distinguishes saved summaries from storage failures.
import pt.isel.pdm.droidmentor.data.repository.ChatRepository // Injects the already verified offline-first repository contract.
import pt.isel.pdm.droidmentor.di.dependencyViewModelFactory // Uses the manual AndroidX factory helper.
import pt.isel.pdm.droidmentor.domain.ChatSummary // Holds only Room-derived domain summaries.
import pt.isel.pdm.droidmentor.ui.components.UiEvent // Publishes local navigation or safe feedback.
import pt.isel.pdm.droidmentor.ui.components.UiNotice // Identifies a safe unexpected storage failure.
import pt.isel.pdm.droidmentor.ui.components.toUiNotice // Maps typed local command failures to screen feedback.

/** Immutable history state observed from Room rather than an API response. */
data class ChatHistoryUiState( // Keeps saved content separate from local command progress.
    val chats: List<ChatSummary> = emptyList(), // Contains the current locally saved conversation list.
    val loading: Boolean = true, // Waits for the first Room snapshot.
    val creating: Boolean = false, // Prevents duplicate chat creation from repeated taps.
    val storageError: Boolean = false, // Distinguishes an unreadable database from empty history.
) // Ends history fields.

/** Observes saved chat summaries and opens a new conversation only after its local insert completes. */
class ChatHistoryViewModel(private val repository: ChatRepository) : ViewModel() { // Performs no network call or credential operation.
    private val mutableState = MutableStateFlow(ChatHistoryUiState()) // Starts without inventing any sample conversation.
    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED) // Retains one-shot events until the screen is active.

    /** Read-only Room-derived history state. */
    val state = mutableState.asStateFlow() // Prevents the screen from bypassing repository operations.

    /** One-shot new-chat navigation and local command feedback. */
    val events = eventChannel.receiveAsFlow() // Emits each command event once.

    init { // Starts local observation independently of connectivity and key presence.
        viewModelScope.launch { // Room's Flow handles database work off the UI dispatcher.
            repository.observeChats().collect { result -> // Uses only locally persisted summaries.
                mutableState.update { current -> // Preserves an in-flight local creation flag.
                    current.copy( // Preserves command progress while updating the saved list.
                        chats = (result as? ChatReadResult.Success)?.value.orEmpty(), // Uses the repository's saved summaries only.
                        loading = false, // Finishes the initial query.
                        storageError = result is ChatReadResult.StorageError, // Distinguishes unreadable storage from empty history.
                    ) // Ends the Room-derived list update.
                } // Ends the saved list update.
            } // Ends Room summary collection.
        } // Ends the history observation job.
    } // Ends initial history observation.

    /** Creates one local conversation using the screen's translated initial title. */
    fun createChat(title: String) { // Does not require a key or network connection.
        if (state.value.creating) return // Ignores repeated taps while this ViewModel owns a creation command.
        mutableState.update { it.copy(creating = true) } // Marks the command before launching it.
        viewModelScope.launch { // Performs the suspend local insert through the repository.
            try { // Keeps navigation conditional on actual persistence.
                val result = repository.createChat(title) // Requests one saved empty conversation.
                if (result is ChatCommandResult.Created) { // Receives a local ID only after successful insertion.
                    eventChannel.send(UiEvent.OpenChat(result.chatId)) // Requests navigation without inserting a fake row into screen state.
                } else { // Handles a rejected or failed local command.
                    result.toUiNotice()?.let { eventChannel.send(UiEvent.Notice(it)) } // Reports only the typed acknowledgement.
                } // Ends creation-result handling.
            } catch (cancelled: CancellationException) { // Preserves cancellation when history is removed from the stack.
                throw cancelled // Never opens an unsaved conversation after owner cancellation.
            } catch (_: Exception) { // Handles unexpected local failures without raw exception text.
                eventChannel.send(UiEvent.Notice(UiNotice.STORAGE_ERROR)) // Keeps feedback safe and typed.
            } finally { // Releases the local creation guard.
                mutableState.update { it.copy(creating = false) } // Allows another explicit creation after this command settles.
            } // Ends creation cleanup.
        } // Ends the creation job.
    } // Ends the new-conversation event.

    /** Manual factory using the app-owned chat repository. */
    companion object { // Keeps construction with the ViewModel that needs the dependency.
        /** Creates a history ViewModel from the manual container. */
        val Factory = dependencyViewModelFactory { ChatHistoryViewModel(it.chatRepository) } // Uses no DI framework or concrete database in the screen.
    } // Ends history factory construction.
} // Ends the history ViewModel.
