package pt.isel.pdm.droidmentor.domain // Keeps the history rules in pure Kotlin without framework imports.

/** Validates complete local message histories before the data layer converts them to Gemini DTOs. */
object HistoryPayloadBuilder { // Provides a stateless builder with no shared mutable history.

    /**
     * Produces every completed exchange and the current pending user turn in sequence order.
     *
     * Invalid rows are rejected rather than filtered, repaired or truncated. This method never
     * changes the input. A retry must already have changed the same failed row to SENDING and
     * cleared its error through the repository and DAO before this method is called.
     *
     * @param chatId Local conversation whose complete persisted history was loaded.
     * @param messages All saved messages for that chat, including the current SENDING user row.
     * @return Complete role/text turns or a safe rejection reason without a partial payload.
     */
    fun build(chatId: String, messages: List<Message>): HistoryPayloadResult { // Accepts local models without knowing about Room or HTTP.
        if (chatId.isBlank()) { // Checks the requested local conversation has a usable identifier.
            return HistoryPayloadResult.Invalid(HistoryValidationError.INVALID_IDENTITY) // Rejects an unidentified chat without preparing turns.
        } // Ends the requested chat identity check.
        if (messages.isEmpty()) { // Checks a current user row was actually supplied.
            return HistoryPayloadResult.Invalid(HistoryValidationError.EMPTY_HISTORY) // Blocks requests that contain no user question.
        } // Ends the empty history check.

        val ordered = messages.sortedBy { it.seq } // Sorts a new list using saved positions rather than timestamps.
        val messageIds = mutableSetOf<String>() // Tracks local IDs only during this validation call.
        val turns = mutableListOf<ConversationTurn>() // Collects every valid turn without imposing a history limit.
        var previousSequence = -1L // Allows the first non-negative position without adding to a Long sequence.

        for ((index, message) in ordered.withIndex()) { // Checks each turn in its established conversational order.
            if (message.chatId != chatId || message.id.isBlank() || !messageIds.add(message.id)) { // Checks ownership, a usable row ID and uniqueness.
                return HistoryPayloadResult.Invalid(HistoryValidationError.INVALID_IDENTITY) // Rejects foreign or duplicated rows rather than omitting them.
            } // Ends the row identity check.
            if (message.seq <= previousSequence) { // Rejects negative first positions and equal positions after sorting.
                return HistoryPayloadResult.Invalid(HistoryValidationError.INVALID_SEQUENCE) // Blocks ambiguous or invalid ordering.
            } // Ends the sequence check.
            previousSequence = message.seq // Records the checked position without requiring contiguous numbering.

            val expectedRole = if (index % 2 == 0) Role.USER else Role.MODEL // Starts with user and alternates authors for every turn.
            if (message.role != expectedRole) { // Checks the saved author agrees with its turn position.
                return HistoryPayloadResult.Invalid(HistoryValidationError.INVALID_ROLE_ORDER) // Blocks a missing or repeated participant turn.
            } // Ends the role order check.
            if (message.text.isBlank()) { // Checks all historical and current turns have usable text.
                return HistoryPayloadResult.Invalid(HistoryValidationError.BLANK_TEXT) // Rejects blank turns without skipping them.
            } // Ends the text check.

            if (message.status == MessageStatus.FAILED) { // Checks failures before any turn can be considered complete or pending.
                val reason = if (message.role == Role.USER) HistoryValidationError.UNRESOLVED_FAILURE else HistoryValidationError.INVALID_MESSAGE_STATE // Only user rows can represent a saved failed attempt.
                return HistoryPayloadResult.Invalid(reason) // Requires a valid retry instead of adding a new turn after failure.
            } // Ends the unresolved failure check.
            if (message.errorCategory != null) { // Checks SENT and SENDING rows do not retain failed-attempt metadata.
                return HistoryPayloadResult.Invalid(HistoryValidationError.INVALID_MESSAGE_STATE) // Requires retry to clear the error before rebuilding history.
            } // Ends the saved error consistency check.

            val isLast = index == ordered.lastIndex // Identifies the only row allowed to be awaiting a reply.
            if (isLast && message.role == Role.USER) { // Checks the current question at the end of history.
                if (message.status != MessageStatus.SENDING) { // Requires the current user attempt to be pending.
                    return HistoryPayloadResult.Invalid(HistoryValidationError.MISSING_PENDING_USER) // Prevents replaying an already completed user row.
                } // Ends the current user state check.
            } else if (message.status != MessageStatus.SENT) { // Requires every older turn and every model reply to be completed.
                return HistoryPayloadResult.Invalid(HistoryValidationError.INVALID_MESSAGE_STATE) // Blocks unfinished historical exchanges or invalid model states.
            } // Ends the completed-versus-current state check.

            turns.add(ConversationTurn(message.role, message.text)) // Preserves the entire text and role without local persistence metadata.
        } // Ends validation of the ordered message rows.

        if (ordered.last().role != Role.USER) { // Checks the full conversation ends with the current user rather than a completed reply.
            return HistoryPayloadResult.Invalid(HistoryValidationError.MISSING_PENDING_USER) // Rejects a completed history with no new question.
        } // Ends the final participant check.
        return HistoryPayloadResult.Success(turns.toList()) // Exposes all prepared turns only after every rule passed.
    } // Ends complete history validation and conversion.
} // Ends the stateless domain builder.
