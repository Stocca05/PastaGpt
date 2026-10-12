package pt.isel.pdm.droidmentor.ui.screens.activechat // Maps persisted failure labels only in the UI layer.

import pt.isel.pdm.droidmentor.R // Supplies safe error text rather than raw server responses.
import pt.isel.pdm.droidmentor.domain.MessageErrorCategory // Uses the category saved with a failed Room message.

/** Selects readable feedback from the persisted category without exposing provider or exception details. */
fun MessageErrorCategory.messageResource(): Int = when (this) { // Handles all ten saved failure categories.
    MessageErrorCategory.INVALID_API_KEY -> R.string.message_invalid_key // Explains rejected key or model access.
    MessageErrorCategory.BAD_REQUEST -> R.string.message_bad_request // Explains a rejected request.
    MessageErrorCategory.RATE_LIMITED -> R.string.message_rate_limited // Explains quota or rate limits.
    MessageErrorCategory.SERVER_ERROR -> R.string.message_server_error // Explains a provider-side failure.
    MessageErrorCategory.NETWORK_UNAVAILABLE -> R.string.message_network_error // Explains a connection or transport failure.
    MessageErrorCategory.TIMEOUT -> R.string.message_timeout // Explains an expired request.
    MessageErrorCategory.MALFORMED_RESPONSE -> R.string.message_malformed // Explains an unreadable reply.
    MessageErrorCategory.EMPTY_RESPONSE -> R.string.message_empty // Explains a reply without usable text.
    MessageErrorCategory.BLOCKED_RESPONSE -> R.string.message_blocked // Explains content blocked by the provider.
    MessageErrorCategory.UNKNOWN -> R.string.message_unknown // Explains an otherwise unclassified failure.
} // Ends saved error translation.
