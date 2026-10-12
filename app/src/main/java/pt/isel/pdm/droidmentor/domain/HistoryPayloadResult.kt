package pt.isel.pdm.droidmentor.domain // Keeps validation outcomes independent of Android and the data layer.

/** A complete prepared history or a safe reason why the supplied messages cannot be sent. */
sealed interface HistoryPayloadResult { // Lets callers handle success and rejection explicitly without exceptions.

    /**
     * All ordered domain turns for the next request, without local IDs or message states.
     *
     * @property turns Every completed exchange followed by the current pending user turn.
     */
    data class Success(val turns: List<ConversationTurn>) : HistoryPayloadResult // Exposes the complete prepared list only after validation succeeds.

    /**
     * A rejected history with no partial payload or raw message details.
     *
     * @property reason Safe category explaining which conversation rule was violated.
     */
    data class Invalid(val reason: HistoryValidationError) : HistoryPayloadResult // Gives callers a typed rejection instead of a thrown exception.
} // Ends the history result types.

/** Safe validation labels; they contain no message text, credential or exception details. */
enum class HistoryValidationError { // Lists the local history problems that prevent preparing a request.
    /** No persisted current user message was supplied. */
    EMPTY_HISTORY, // A request needs at least one user turn.

    /** A chat or message ID is blank, a row belongs to another chat, or a message ID is duplicated. */
    INVALID_IDENTITY, // The supplied rows do not describe distinct messages in the requested chat.

    /** A message position is negative or shared by another message. */
    INVALID_SEQUENCE, // The supplied positions cannot establish reliable conversational order.

    /** Roles do not start with user and then alternate between user and model. */
    INVALID_ROLE_ORDER, // The supplied authors violate the request's turn order.

    /** A user or model turn contains only whitespace or no text. */
    BLANK_TEXT, // A turn has no usable text and must not be silently removed.

    /** A failed user row has not yet been reactivated for retry. */
    UNRESOLVED_FAILURE, // The failed turn blocks preparation until the same row becomes pending again.

    /** Completed turns are not SENT or a successful or pending row retains an error category. */
    INVALID_MESSAGE_STATE, // The saved states do not form completed exchanges followed by one current attempt.

    /** The final turn is not a pending user message. */
    MISSING_PENDING_USER, // A request cannot end with a model reply or a user row already marked SENT.
} // Ends the safe validation labels.
