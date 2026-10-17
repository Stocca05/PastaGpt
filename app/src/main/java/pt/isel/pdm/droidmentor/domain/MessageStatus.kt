package pt.isel.pdm.droidmentor.domain // Groups the domain models.

/** Describes the outcome of the exchange associated with a persisted message. */
enum class MessageStatus { // Lists the possible values for MessageStatus.
    /** The user message is awaiting a usable mentor reply. */
    SENDING, // The user message is waiting for the mentor reply.

    /** The exchange completed and both the user message and mentor reply are persisted. */
    SENT, // The message belongs to a completed exchange.

    /** The attempt ended without a usable reply; its error category is persisted. */
    FAILED, // The attempt finished without a usable reply.
} // Ends this block.
