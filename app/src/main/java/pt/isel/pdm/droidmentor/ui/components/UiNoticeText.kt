package pt.isel.pdm.droidmentor.ui.components // Translates typed notices only in the Android UI layer.

import pt.isel.pdm.droidmentor.R // Supplies safe resource-backed feedback text.

/** Selects a safe message resource without including raw exception details or user input. */
fun UiNotice.messageResource(): Int = when (this) { // Handles every existing feedback type explicitly.
    UiNotice.KEY_SAVED -> R.string.notice_key_saved // Confirms preferences persistence.
    UiNotice.KEY_CLEARED -> R.string.notice_key_cleared // Confirms selective key removal.
    UiNotice.EMPTY_KEY -> R.string.notice_empty_key // Requests nonblank input.
    UiNotice.INVALID_INPUT -> R.string.notice_invalid_input // Reports invalid local input.
    UiNotice.NOT_FOUND -> R.string.notice_not_found // Reports a missing conversation.
    UiNotice.BUSY -> R.string.notice_busy // Explains an operation already running.
    UiNotice.UNRESOLVED_MESSAGE -> R.string.notice_unresolved // Requests settling or retrying the current turn.
    UiNotice.MISSING_KEY -> R.string.notice_missing_key // Requests configuration without revealing a value.
    UiNotice.SETTINGS_ERROR -> R.string.notice_settings_error // Reports an unreadable preference store.
    UiNotice.OFFLINE -> R.string.notice_offline // Explains the unavailable connection.
    UiNotice.INVALID_HISTORY -> R.string.notice_invalid_history // Reports an invalid saved conversation.
    UiNotice.INVALID_RETRY -> R.string.notice_invalid_retry // Reports an invalid saved retry target.
    UiNotice.STORAGE_ERROR -> R.string.notice_storage_error // Reports a local persistence problem.
} // Ends typed feedback translation.
