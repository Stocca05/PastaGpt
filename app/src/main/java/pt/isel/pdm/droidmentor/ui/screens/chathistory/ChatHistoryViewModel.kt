package pt.isel.pdm.droidmentor.ui.screens.chathistory // Owns saved conversation state and creation commands.

import androidx.lifecycle.ViewModel // Gives history its navigation-entry lifetime.
import androidx.lifecycle.viewModelScope // Cancels work when the destination is removed.
import kotlinx.coroutines.CancellationException // Preserves owner cancellation.
import kotlinx.coroutines.CoroutineStart // Ensures entry fields are assigned before the undo timer starts.
import kotlinx.coroutines.Job // Tracks the pending 5-second undo timer so it can be cancelled or flushed.
import kotlinx.coroutines.NonCancellable // Finishes a confirmed deletion if the screen closes before 5 seconds.
import kotlinx.coroutines.channels.Channel // Buffers one-shot navigation and feedback events.
import kotlinx.coroutines.delay // Waits for the 5-second undo window before deleting in Room.
import kotlinx.coroutines.flow.MutableStateFlow // Holds an immutable Room-derived screen snapshot.
import kotlinx.coroutines.flow.asStateFlow // Exposes read-only history state.
import kotlinx.coroutines.flow.receiveAsFlow // Exposes buffered events without replay.
import kotlinx.coroutines.flow.update // Preserves independent state fields during updates.
import kotlinx.coroutines.launch // Collects Room and performs local suspend commands.
import kotlinx.coroutines.withContext // Runs confirmed cleanup safely across coroutine cancellation.
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
    val pendingDeleteId: String? = null, // Hides a confirmed chat during the 5-second undo window.
    val loading: Boolean = true, // Waits for the first Room snapshot.
    val creating: Boolean = false, // Prevents duplicate chat creation from repeated taps.
    val storageError: Boolean = false, // Distinguishes an unreadable database from empty history.
) { // Derives the visible conversation list without mutating Room snapshots.
    /** Saved conversations excluding the chat currently waiting in the undo window. */
    val visibleChats: List<ChatSummary> // Keeps the list stable while allowing instant undo.
        get() = if (pendingDeleteId == null) chats else chats.filterNot { it.chat.id == pendingDeleteId } // Filters only the pending deletion.
} // Ends history fields.

/** Observes saved chat summaries and opens a new conversation only after its local insert completes. */
class ChatHistoryViewModel( // Performs no network call or credential operation.
    private val repository: ChatRepository, // Owns Room persistence and cascade deletes.
    private val undoWindowMs: Long = DEFAULT_UNDO_WINDOW_MS, // Allows tests to keep the 5-second undo contract deterministic.
) : ViewModel() { // Keeps history lifetime scoped to this destination.
    private val mutableState = MutableStateFlow(ChatHistoryUiState()) // Starts without inventing any sample conversation.
    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED) // Retains one-shot events until the screen is active.
    private var activeDelete: PendingDelete? = null // Tracks the active undo entry without cross-job flag races.

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
                flushPendingDeleteSynchronously() // Commits any earlier pending deletion before opening a new chat.
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

    /** Renames one saved conversation manually without changing its messages. */
    fun renameChat(chatId: String, title: String) { // Performs a local rename independent of network access.
        viewModelScope.launch { // Runs the suspend repository rename off the UI thread.
            try { // Keeps unexpected local storage failures safe and typed.
                repository.renameChat(chatId, title).toUiNotice()?.let { eventChannel.send(UiEvent.Notice(it)) } // Surfaces invalid input, busy or storage notices.
            } catch (cancelled: CancellationException) { // Preserves structured coroutine cancellation.
                throw cancelled // Lets owner cancellation propagate.
            } catch (_: Exception) { // Converts unexpected failures to a typed notice.
                eventChannel.send(UiEvent.Notice(UiNotice.STORAGE_ERROR)) // Avoids leaking raw exception text.
            } // Ends safe rename handling.
        } // Ends rename coroutine.
    } // Ends manual rename command.

    /**
     * Schedules deletion of [chatId] after a 5-second undo window.
     * Any earlier pending deletion is committed immediately so no confirmed delete is lost.
     */
    fun requestDelete(chatId: String) { // Starts the undoable deletion flow after dialog confirmation.
        if (chatId.isBlank() || state.value.pendingDeleteId == chatId) return // Ignores blank IDs and duplicate requests for the same row.
        val previous = activeDelete // Captures any earlier pending deletion entry.
        if (previous != null) { // Replaces the earlier timer without racing on shared mutable flags.
            previous.commitOnCancel = false // Prevents the cancelled timer from deleting the same chat a second time.
            previous.job.cancel() // Stops the old 5-second countdown.
        } // Ends previous entry cancellation.
        mutableState.update { it.copy(pendingDeleteId = chatId) } // Hides the deleted chat immediately in the UI list.
        val entry = PendingDelete(chatId) // Creates isolated per-request undo state.
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) { // Waits 5 seconds before deleting the row in Room.
            if (previous != null) commitDelete(previous.chatId, clearPendingIfMatch = false) // Flushes the earlier confirmed deletion once.
            try { // Lets undo or replacement cancel the delay cleanly.
                delay(undoWindowMs) // Gives the user 5 seconds to tap Undo in the snackbar.
                entry.commitOnCancel = false // Marks the countdown complete so cancellation during Room work won't retry.
                commitDelete(chatId, clearPendingIfMatch = true) // Cascades the deletion in Room once the window expires.
            } catch (cancelled: CancellationException) { // Checks why this specific undo entry was cancelled.
                if (entry.commitOnCancel) { // Commits the confirmed delete if the screen closed before 5 seconds.
                    withContext(NonCancellable) { repository.deleteChat(chatId) } // Runs safely even while viewModelScope is cancelling.
                } // Leaves Room untouched when cancelled by Undo or replaced by a newer delete.
                throw cancelled // Preserves structured coroutine cancellation.
            } finally { // Clears the active pointer only if this entry is still the current one.
                if (activeDelete === entry) activeDelete = null // Releases the completed or cancelled undo entry.
            } // Ends undo-window execution.
        } // Ends pending deletion job.
        entry.job = job // Binds the job before starting so undo cannot see an uninitialized reference.
        activeDelete = entry // Records this entry so undo or flush can target it directly.
        job.start() // Starts the 5-second countdown after state is wired.
    } // Ends undoable delete scheduling.

    /** Cancels the pending deletion and restores the hidden chat immediately. */
    fun undoDelete() { // Invoked when the user taps Undo in the snackbar.
        val current = activeDelete ?: return // Does nothing when no deletion is pending.
        current.commitOnCancel = false // Tells only this timer job not to delete in Room.
        activeDelete = null // Clears the active entry reference.
        current.job.cancel() // Stops the 5-second countdown before Room is modified.
        mutableState.update { it.copy(pendingDeleteId = null) } // Makes the conversation visible again in the list.
    } // Ends delete cancellation.

    /** Commits any pending deletion immediately, e.g. when opening another conversation. */
    fun flushPendingDelete() { // Ensures a pending delete settles promptly when leaving history.
        val current = activeDelete ?: return // Skips work when nothing is waiting in the undo window.
        current.commitOnCancel = false // Avoids running deletion twice when cancelling the delay.
        activeDelete = null // Clears the active entry reference.
        current.job.cancel() // Cancels the remaining wait time.
        mutableState.update { it.copy(pendingDeleteId = null) } // Clears the temporary UI filter before deleting in Room.
        viewModelScope.launch { commitDelete(current.chatId, clearPendingIfMatch = false) } // Persists the deletion immediately.
    } // Ends immediate flush.

    private suspend fun flushPendingDeleteSynchronously() { // Used inside createChat before navigating away.
        val current = activeDelete ?: return // Skips when no deletion is pending.
        current.commitOnCancel = false // Avoids running deletion twice.
        activeDelete = null // Clears the active entry reference.
        current.job.cancel() // Cancels the remaining wait time.
        commitDelete(current.chatId, clearPendingIfMatch = true) // Commits the pending deletion before creating a new chat.
    } // Ends synchronous flush helper.

    private suspend fun commitDelete(chatId: String, clearPendingIfMatch: Boolean) { // Performs the Room delete and reports safe failures.
        val result = try { // Guards against unexpected local storage exceptions.
            repository.deleteChat(chatId) // Deletes the chat and cascades to its messages in Room.
        } catch (cancelled: CancellationException) { // Preserves cancellation if the owner is destroyed mid-query.
            throw cancelled // Lets coroutine cancellation propagate.
        } catch (_: Exception) { // Converts unexpected failures into a safe typed result.
            ChatCommandResult.StorageError // Keeps exception text out of UI feedback.
        } // Ends safe delete call.
        if (clearPendingIfMatch) { // Clears the pending marker only if it still refers to this chat.
            mutableState.update { if (it.pendingDeleteId == chatId) it.copy(pendingDeleteId = null) else it } // Releases the UI filter.
        } // Ends pending-ID cleanup.
        result.toUiNotice()?.let { eventChannel.send(UiEvent.Notice(it)) } // Notifies the user if Room rejected or failed the delete.
    } // Ends Room delete helper.

    private class PendingDelete(val chatId: String) { // Holds per-request cancellation state so consecutive deletes cannot race.
        var commitOnCancel: Boolean = true // Only true if the owner closes before Undo or replacement.
        lateinit var job: Job // Stores the timer coroutine started for this request.
    } // Ends isolated undo-entry state.

    /** Manual factory using the app-owned chat repository. */
    companion object { // Keeps construction with the ViewModel that needs the dependency.
        /** Default 5-second delay before a confirmed deletion is committed to Room. */
        const val DEFAULT_UNDO_WINDOW_MS = 5_000L // Matches the M3-01 5-second undo window specification.

        /** Creates a history ViewModel from the manual container. */
        val Factory = dependencyViewModelFactory { ChatHistoryViewModel(it.chatRepository) } // Uses no DI framework or concrete database in the screen.
    } // Ends history factory construction.
} // Ends the history ViewModel.
