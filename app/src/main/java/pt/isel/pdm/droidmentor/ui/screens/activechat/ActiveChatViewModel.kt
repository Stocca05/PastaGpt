package pt.isel.pdm.droidmentor.ui.screens.activechat // Owns a selected conversation's local state and explicit commands.

import androidx.lifecycle.ViewModel // Scopes this conversation to its navigation entry.
import androidx.lifecycle.viewModelScope // Cancels collectors and commands when the conversation is left.
import kotlinx.coroutines.CancellationException // Preserves repository cancellation and its local cleanup.
import kotlinx.coroutines.channels.Channel // Buffers one-shot local feedback.
import kotlinx.coroutines.flow.Flow // Accepts the container's connectivity contract without Android services.
import kotlinx.coroutines.flow.MutableStateFlow // Holds the latest immutable conversation snapshot.
import kotlinx.coroutines.flow.asStateFlow // Exposes state without exposing its mutation methods.
import kotlinx.coroutines.flow.combine // Waits for both Room queries before publishing the initial conversation.
import kotlinx.coroutines.flow.receiveAsFlow // Exposes one-shot notices without replay.
import kotlinx.coroutines.flow.update // Keeps independent collector updates safe.
import kotlinx.coroutines.launch // Collects data and performs suspend commands outside Composables.
import pt.isel.pdm.droidmentor.data.repository.ChatCommandResult // Reads only local command acknowledgements.
import pt.isel.pdm.droidmentor.data.repository.ChatReadResult // Handles typed local storage read failures.
import pt.isel.pdm.droidmentor.data.repository.ChatRepository // Supplies offline-first observation, send and retry operations.
import pt.isel.pdm.droidmentor.data.repository.SettingsReadResult // Reads key presence without placing the key in screen state.
import pt.isel.pdm.droidmentor.data.repository.SettingsRepository // Supplies the configured-key status stream.
import pt.isel.pdm.droidmentor.di.dependencyViewModelFactory // Builds the manual factory with the selected local ID.
import pt.isel.pdm.droidmentor.domain.Chat // Holds Room-derived conversation metadata.
import pt.isel.pdm.droidmentor.domain.ConnectivityStatus // Describes validated connectivity without Android imports in the domain.
import pt.isel.pdm.droidmentor.domain.Message // Holds Room-derived message content and error categories.
import pt.isel.pdm.droidmentor.domain.MessageStatus // Identifies unresolved pending and failed turns.
import pt.isel.pdm.droidmentor.domain.Role // Restricts retry to the final failed user turn.
import pt.isel.pdm.droidmentor.ui.components.UiEvent // Publishes safe local notices rather than network results.
import pt.isel.pdm.droidmentor.ui.components.UiNotice // Identifies safe local command failures.
import pt.isel.pdm.droidmentor.ui.components.toUiNotice // Maps typed acknowledgements into UI feedback.

/** Immutable selected-conversation state; all saved messages and API error categories come from Room. */
data class ActiveChatUiState( // Keeps the unsent in-memory draft separate from saved history.
    val chat: Chat? = null, // Holds the selected conversation or null after local deletion.
    val messages: List<Message> = emptyList(), // Holds the complete ordered Room message list.
    val draft: String = "", // Holds unsent text in memory only.
    val loading: Boolean = true, // Waits for both initial Room queries.
    val working: Boolean = false, // Tracks this ViewModel's explicit send or retry command.
    val storageError: Boolean = false, // Distinguishes unreadable history from an empty conversation.
    val connected: Boolean = false, // Holds the validated connectivity status for offline UI.
    val hasKey: Boolean = false, // Indicates key presence without storing its value in UI state.
    val settingsError: Boolean = false, // Reports unreadable key preferences separately from missing configuration.
) { // Defines simple derived guards used by the message screen.
    /** Whether any saved turn still owns an unresolved network attempt. */
    val pending: Boolean get() = messages.any { it.status == MessageStatus.SENDING } // Also reflects sends started by another observer of the same repository.

    /** Whether a failed saved turn must be retried before a new user turn. */
    val failed: Boolean get() = messages.any { it.status == MessageStatus.FAILED } // Keeps full-history validation consistent with the repository.

    /** Whether the current draft may start a new explicit send. */
    val canSend: Boolean // Derives availability from the complete local snapshot.
        get() = !loading && !storageError && chat != null // Requires readable saved conversation metadata.
            && connected && hasKey && !settingsError // Blocks new interactions while offline or unconfigured.
            && !working && !pending && !failed // Rejects concurrent and unresolved turns.
            && draft.isNotBlank() // Requires actual user input.

    /** Whether the final saved failed user row can be retried now. */
    val canRetry: Boolean // Shares the same guard between the visible action and its event.
        get() = !loading && !storageError && chat != null // Requires readable saved history.
            && connected && hasKey && !settingsError // Requires usable prerequisites before calling the repository.
            && !working && !pending // Prevents a second operation from owning the same chat.
            && messages.lastOrNull()?.role == Role.USER // Selects only the final user turn.
            && messages.lastOrNull()?.status == MessageStatus.FAILED // Selects only an unresolved failed row.

    /** Hides drafts and message content in accidental diagnostic output. */
    override fun toString(): String = "ActiveChatUiState(content=<hidden>)" // Avoids printing an unsent draft that could contain sensitive input.
} // Ends the selected conversation state.

/** Observes only local history and persists send/retry outcomes through the repository. */
class ActiveChatViewModel( // Receives the selected ID and replaceable contracts by hand.
    private val chatId: String, // Identifies the local Room conversation selected by navigation.
    private val repository: ChatRepository, // Owns all local history and networking coordination.
    settingsRepository: SettingsRepository, // Supplies typed key-presence updates without exposing preferences storage.
    connectivity: Flow<ConnectivityStatus>, // Supplies the container's validated status stream.
) : ViewModel() { // Gives commands and collectors the destination's coroutine lifetime.
    private val mutableState = MutableStateFlow(ActiveChatUiState()) // Starts without inventing any API or Room content.
    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED) // Buffers only safe local feedback.

    /** Read-only selected-conversation state. */
    val state = mutableState.asStateFlow() // Exposes immutable snapshots to Compose.

    /** One-shot prerequisite and local storage feedback. */
    val events = eventChannel.receiveAsFlow() // Keeps network answers out of the event channel.

    init { // Starts independent local history, connectivity and settings observation.
        viewModelScope.launch { // Lets Room perform its own asynchronous reads.
            combine(repository.observeChat(chatId), repository.observeMessages(chatId)) { chat, messages -> chat to messages }.collect { (chat, messages) -> // Waits for both initial local snapshots.
                mutableState.update { current -> // Preserves the unsent draft and command progress.
                    current.copy( // Keeps the draft and command guard while applying saved history.
                        chat = (chat as? ChatReadResult.Success)?.value, // Reads locally saved conversation metadata.
                        messages = (messages as? ChatReadResult.Success)?.value.orEmpty(), // Reads locally saved messages and error labels.
                        loading = false, // Both initial Room queries have now emitted.
                        storageError = chat is ChatReadResult.StorageError || messages is ChatReadResult.StorageError, // Exposes either local read failure.
                    ) // Ends the Room-derived conversation update.
                } // Ends the local history update.
            } // Ends combined Room observation.
        } // Ends the history collector.
        viewModelScope.launch { // Observes the real Android provider through its Flow interface.
            connectivity.collect { status -> mutableState.update { it.copy(connected = status == ConnectivityStatus.CONNECTED) } } // Changes only the connectivity flag.
        } // Ends the network-status collector.
        viewModelScope.launch { // Observes presence without retaining a saved credential value.
            settingsRepository.readResult.collect { result -> // Reads the typed settings outcome.
                mutableState.update { current -> // Keeps all conversation content unchanged.
                    current.copy( // Stores key status rather than the credential itself.
                        hasKey = result is SettingsReadResult.Success && !result.apiKey.isNullOrBlank(), // Indicates configured presence.
                        settingsError = result is SettingsReadResult.StorageError, // Reports an unreadable preference file.
                    ) // Ends the safe settings projection.
                } // Ends the prerequisite update.
            } // Ends configured-key observation.
        } // Ends the settings collector.
    } // Ends initial local observations.

    /** Edits the in-memory draft unless a send currently owns its value. */
    fun updateDraft(value: String) { // Allows editing while offline or while a failed saved row awaits retry.
        if (!state.value.working && !state.value.pending) mutableState.update { it.copy(draft = value) } // Prevents command completion from discarding newer input.
    } // Ends draft editing.

    /** Sends one nonblank draft through the repository, leaving rejected drafts intact. */
    fun send() { // Relies on repository prerequisite checks before every network operation.
        if (!state.value.canSend) return // Prevents blank, concurrent or unresolved-turn sends.
        val text = state.value.draft // Captures the unchanged user text for this attempt.
        perform(clearDraft = true) { repository.sendMessage(chatId, text) } // Clears input only after a locally settled outcome.
    } // Ends explicit sending.

    /** Retries only the final failed user row, preserving its ID and the current unsent draft. */
    fun retry() { // Never appends a duplicate user message from the ViewModel.
        val snapshot = state.value // Captures one coherent selection and operation state.
        if (!snapshot.canRetry) return // Applies the same prerequisite and ownership guard as the screen.
        val message = snapshot.messages.lastOrNull() ?: return // Reads the final saved target without an unsafe assertion.
        perform(clearDraft = false) { repository.retryMessage(chatId, message.id) } // Sends the same saved row through the repository's guarded retry command.
    } // Ends explicit retry.

    private fun perform(clearDraft: Boolean, command: suspend () -> ChatCommandResult) { // Shares the small command guard and safe local feedback handling.
        mutableState.update { it.copy(working = true) } // Prevents another event from starting before the coroutine executes.
        viewModelScope.launch { // Performs suspend repository coordination without IO in the Composable.
            try { // Preserves cancellation and avoids exposing unexpected local exception text.
                val result = command() // Receives only a local acknowledgement, never the model answer.
                if (clearDraft && result == ChatCommandResult.Stored) mutableState.update { it.copy(draft = "") } // Clears the sent draft after success or failure was saved in Room.
                result.toUiNotice()?.let { eventChannel.send(UiEvent.Notice(it)) } // Reports rejected commands while saved API errors remain in the Room message list.
            } catch (cancelled: CancellationException) { // Lets the repository finish its cancellation cleanup when the destination is left.
                throw cancelled // Preserves the original structured cancellation.
            } catch (_: Exception) { // Handles an unexpected local failure without raw diagnostic content.
                eventChannel.send(UiEvent.Notice(UiNotice.STORAGE_ERROR)) // Reports only a typed local error.
            } finally { // Releases the ViewModel's event guard after the command settles.
                mutableState.update { it.copy(working = false) } // Room's pending rows still independently prevent another send.
            } // Ends command cleanup.
        } // Ends explicit command execution.
    } // Ends shared send/retry coordination.

    /** Manual constructor binding for a specific local conversation ID. */
    companion object { // Keeps the navigation argument outside the process-wide dependency graph.
        /** Builds a factory whose ViewModel belongs to the selected conversation's navigation entry. */
        fun factory(chatId: String) = dependencyViewModelFactory { container -> // Captures only the local ID, never a credential or history snapshot.
            ActiveChatViewModel(chatId, container.chatRepository, container.settingsRepository, container.connectivityStatus) // Supplies the existing manually wired contracts explicitly.
        } // Ends selected-conversation factory construction.
    } // Ends manual active-chat construction.
} // Ends the active conversation ViewModel.
