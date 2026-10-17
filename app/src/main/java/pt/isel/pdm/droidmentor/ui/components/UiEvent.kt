package pt.isel.pdm.droidmentor.ui.components // Keeps screen events independent of Android resources.

import pt.isel.pdm.droidmentor.data.repository.ChatCommandResult // Maps local acknowledgements into safe screen feedback.

/** One-shot UI actions, containing neither API answers nor credential values. */
sealed interface UiEvent { // Separates navigation and feedback from continuously observed screen state.
    /** Opens a conversation only after the repository has persisted it. */
    data class OpenChat(val chatId: String) : UiEvent // Carries only the local Room identifier.

    /** Requests a resource-backed snackbar with a safe typed message. */
    data class Notice(val notice: UiNotice) : UiEvent // Carries no exception text or user input.
} // Ends the screen event types.

/** Safe feedback labels translated into resources by the screen layer. */
enum class UiNotice { // Keeps ViewModels free of Context and resource lookups.
    KEY_SAVED, // Confirms a completed DataStore save.
    KEY_CLEARED, // Confirms a completed DataStore clear.
    EMPTY_KEY, // Requests a nonblank key before saving.
    INVALID_INPUT, // Reports an unusable title, ID or text.
    NOT_FOUND, // Reports a missing local conversation.
    BUSY, // Reports an operation already in progress.
    UNRESOLVED_MESSAGE, // Requires the unresolved message to settle or be retried.
    MISSING_KEY, // Requires key configuration before a send.
    SETTINGS_ERROR, // Reports an unreadable settings store.
    OFFLINE, // Explains why a new interaction cannot start.
    INVALID_HISTORY, // Reports a conversation that cannot form a valid stateless request.
    INVALID_RETRY, // Reports an invalid retry target.
    STORAGE_ERROR, // Reports a local persistence failure without raw exception details.
} // Ends typed feedback labels.

/** Maps only command failures; successful sends are displayed through Room observation. */
fun ChatCommandResult.toUiNotice(): UiNotice? = when (this) { // Handles every existing repository acknowledgement explicitly.
    is ChatCommandResult.Created, ChatCommandResult.Stored -> null // Successful local commands need no network-result feedback.
    ChatCommandResult.InvalidInput -> UiNotice.INVALID_INPUT // Reports invalid local input.
    ChatCommandResult.NotFound -> UiNotice.NOT_FOUND // Reports missing local data.
    ChatCommandResult.Busy -> UiNotice.BUSY // Reports the chat's operation gate.
    ChatCommandResult.UnresolvedMessage -> UiNotice.UNRESOLVED_MESSAGE // Explains the blocked new turn.
    ChatCommandResult.MissingApiKey -> UiNotice.MISSING_KEY // Requests key configuration.
    ChatCommandResult.SettingsError -> UiNotice.SETTINGS_ERROR // Reports a settings read failure.
    ChatCommandResult.Offline -> UiNotice.OFFLINE // Reports the repository's fresh connectivity check.
    ChatCommandResult.InvalidHistory -> UiNotice.INVALID_HISTORY // Reports an invalid persisted history.
    ChatCommandResult.InvalidRetry -> UiNotice.INVALID_RETRY // Reports an invalid failed-message selection.
    ChatCommandResult.StorageError -> UiNotice.STORAGE_ERROR // Reports a local write failure.
} // Ends local feedback mapping.
