package pt.isel.pdm.droidmentor.domain // Tests history rules without Android, persistence or network dependencies.

import org.junit.Assert.assertEquals // Compares the supplied harmless texts, roles and validation outcomes.
import org.junit.Assert.assertTrue // Checks that invalid histories produce no usable payload.
import org.junit.Test // Marks methods as JUnit test cases.

/** Checks complete stateless histories and rejects inconsistent local messages before request conversion. */
class HistoryPayloadBuilderTest { // Groups the domain rules required for M2-08.

    /** Checks no request can be prepared without the current user message. */
    @Test // Covers a chat whose message history is empty.
    fun emptyHistoryIsRejected() { // Supplies no persisted messages.
        assertInvalid(emptyList(), HistoryValidationError.EMPTY_HISTORY) // Checks an empty list is not a ready conversation.
    } // Ends the empty history test.

    /** Checks the first pending user message is already a complete one-turn request history. */
    @Test // Covers the first send in a new conversation.
    fun firstPendingUserProducesOneTurn() { // Supplies only the pending user row.
        val message = row(0, Role.USER, MessageStatus.SENDING, " First question. ") // Includes whitespace that must be retained.
        assertEquals(listOf(ConversationTurn(Role.USER, message.text)), readyTurns(listOf(message))) // Checks the role and entire text survive conversion.
    } // Ends the first-message test.

    /** Checks N completed exchanges always produce 2N + 1 turns with the next user message. */
    @Test // Covers conversations of several lengths rather than one fixed example.
    fun everyCompletedExchangeIsIncluded() { // Exercises empty prior history through twelve completed exchanges.
        for (exchanges in 0..12) { // Varies the number of completed user/model pairs.
            val messages = history(exchanges) // Creates completed pairs followed by one pending user row.
            val turns = readyTurns(messages) // Requires the builder to accept the valid history.
            assertEquals(2 * exchanges + 1, turns.size) // Checks the current user is added after every completed pair.
            assertEquals(messages.map { it.text }, turns.map { it.text }) // Checks no older turn was removed.
            assertEquals(messages.map { it.role }, turns.map { it.role }) // Checks role order is retained.
        } // Ends the conversation length loop.
    } // Ends the full-history count test.

    /** Checks sequence order is restored even when the supplied rows arrive in a different order. */
    @Test // Covers a caller whose input list is not already sorted.
    fun sequenceDeterminesTurnOrder() { // Provides a deliberately shuffled two-exchange conversation.
        val messages = history(2) // Creates the expected sequence order.
        val shuffled = listOf(messages[4], messages[1], messages[3], messages[0], messages[2]) // Places the pending message first in the input.
        assertEquals(messages.map { it.text }, readyTurns(shuffled).map { it.text }) // Checks the output uses seq rather than input position.
    } // Ends the sequence sorting test.

    /** Checks timestamps never determine conversational order. */
    @Test // Covers equal and backwards-running creation timestamps.
    fun timestampsDoNotReorderTurns() { // Changes timestamps while retaining the established sequence.
        val messages = history(2).mapIndexed { index, message -> message.copy(createdAt = if (index < 2) 100 else 10L - index) } // Uses tied and decreasing dates.
        assertEquals(messages.map { it.text }, readyTurns(messages.reversed()).map { it.text }) // Checks sequence still wins over timestamps.
    } // Ends the timestamp ordering test.

    /** Checks valid Long sequences need not be contiguous and do not overflow during validation. */
    @Test // Covers valid ordering close to Long's upper limit.
    fun largeAndNonContiguousSequencesAreAccepted() { // Supplies three distinct non-negative positions.
        val messages = history(1).mapIndexed { index, message -> message.copy(seq = listOf(3_000_000_000L, Long.MAX_VALUE - 10, Long.MAX_VALUE)[index]) } // Includes a large gap and the largest Long value.
        assertEquals(messages.map { it.text }, readyTurns(messages.reversed()).map { it.text }) // Checks ordering without adding to sequence values.
    } // Ends the large sequence test.

    /** Checks long histories and long individual texts are kept in full. */
    @Test // Guards against token-saving truncation or accidental character limits.
    fun longHistoryAndTextAreNotTruncated() { // Prepares many completed exchanges and one long pending question.
        val messages = history(1_000).toMutableList() // Creates 2,001 turns, including all completed pairs.
        messages[messages.lastIndex] = messages.last().copy(text = "Flow café 🍝\n".repeat(2_000)) // Supplies a long Unicode question without changing its identity.
        val turns = readyTurns(messages.reversed()) // Also exercises sequence sorting on a long conversation.
        assertEquals(2_001, turns.size) // Checks every turn is retained.
        assertEquals(messages.map { it.text }, turns.map { it.text }) // Checks full text equality for all turns.
    } // Ends the long history test.

    /** Checks messages from another chat cannot enter the prepared history. */
    @Test // Covers both mixed chat rows and an entirely wrong chat list.
    fun foreignChatMessagesAreRejected() { // Changes only the ownership of otherwise valid messages.
        val messages = history(1).toMutableList() // Prepares one completed pair and the pending row.
        messages[1] = messages[1].copy(chatId = "other-chat") // Introduces a reply belonging to another conversation.
        assertInvalid(messages, HistoryValidationError.INVALID_IDENTITY) // Checks the foreign reply is rejected rather than filtered away.
        assertInvalid(listOf(row(0, Role.USER, MessageStatus.SENDING).copy(chatId = "other-chat")), HistoryValidationError.INVALID_IDENTITY) // Checks even one foreign pending row cannot be sent for this chat.
    } // Ends the chat ownership test.

    /** Checks blank chat or message identifiers cannot describe a valid persisted history. */
    @Test // Covers invalid local identities without changing message text or role.
    fun blankIdentitiesAreRejected() { // Supplies rows with incomplete local identifiers.
        val message = row(0, Role.USER, MessageStatus.SENDING) // Creates a valid first pending row.
        assertInvalid(listOf(message.copy(id = " \n")), HistoryValidationError.INVALID_IDENTITY) // Checks the row needs a usable local ID.
        val result = HistoryPayloadBuilder.build(" ", listOf(message.copy(chatId = " "))) // Supplies a blank target chat and matching blank ownership.
        assertEquals(HistoryPayloadResult.Invalid(HistoryValidationError.INVALID_IDENTITY), result) // Checks the requested chat itself must have a usable ID.
    } // Ends the blank identity test.

    /** Checks a duplicated local ID is not silently included twice in the payload. */
    @Test // Covers duplicated identity with otherwise distinct sequence values.
    fun duplicateMessageIdsAreRejected() { // Reuses one local ID on a different turn.
        val messages = history(1).toMutableList() // Creates a valid three-turn conversation.
        messages[1] = messages[1].copy(id = messages[0].id) // Assigns the reply the same identity as the first user row.
        assertInvalid(messages, HistoryValidationError.INVALID_IDENTITY) // Checks the builder detects the duplicate row identity.
    } // Ends the duplicate identity test.

    /** Checks negative or duplicated sequence values cannot establish a reliable turn order. */
    @Test // Covers malformed ordering independently of timestamps.
    fun invalidSequencesAreRejected() { // Changes only the saved positions of valid rows.
        assertInvalid(listOf(row(-1, Role.USER, MessageStatus.SENDING)), HistoryValidationError.INVALID_SEQUENCE) // Rejects a negative position.
        val messages = history(1).toMutableList() // Creates a valid completed exchange and current row.
        messages[1] = messages[1].copy(seq = messages[0].seq) // Assigns two turns the same position.
        assertInvalid(messages, HistoryValidationError.INVALID_SEQUENCE) // Checks ambiguous ordering is rejected.
    } // Ends the invalid sequence test.

    /** Checks every prepared conversation starts with a user turn. */
    @Test // Covers a model message at the beginning of history.
    fun initialModelTurnIsRejected() { // Supplies a reply without its preceding user row.
        assertInvalid(listOf(row(0, Role.MODEL, MessageStatus.SENT)), HistoryValidationError.INVALID_ROLE_ORDER) // Checks a model cannot start the request.
    } // Ends the first-role test.

    /** Checks neither user/user nor model/model adjacency is allowed. */
    @Test // Covers both ways to break strict role alternation.
    fun consecutiveEqualRolesAreRejected() { // Changes one role in otherwise ordered histories.
        val users = history(1).toMutableList() // Creates user/model/user turns.
        users[1] = users[1].copy(role = Role.USER) // Replaces the model with a second consecutive user.
        assertInvalid(users, HistoryValidationError.INVALID_ROLE_ORDER) // Checks adjacent user turns are rejected.
        val models = history(2).toMutableList() // Creates user/model/user/model/user turns.
        models[2] = models[2].copy(role = Role.MODEL) // Replaces a user with a second consecutive model.
        assertInvalid(models, HistoryValidationError.INVALID_ROLE_ORDER) // Checks adjacent model turns are rejected.
    } // Ends the role alternation test.

    /** Checks completed pairs alone cannot be sent as though they contained a new pending question. */
    @Test // Covers a history whose final turn belongs to the model.
    fun completedHistoryWithoutPendingUserIsRejected() { // Removes the next pending user from a valid history.
        assertInvalid(history(2).dropLast(1), HistoryValidationError.MISSING_PENDING_USER) // Checks a request needs the current pending turn.
    } // Ends the missing current turn test.

    /** Checks a sent user row is not replayed as the current pending message. */
    @Test // Covers an incomplete exchange that is incorrectly marked completed.
    fun finalSentUserIsRejected() { // Supplies a last user message with the wrong status.
        val messages = history(1).toMutableList() // Creates a valid pending history.
        messages[messages.lastIndex] = messages.last().copy(status = MessageStatus.SENT) // Marks the final row sent without adding a reply.
        assertInvalid(messages, HistoryValidationError.MISSING_PENDING_USER) // Checks the builder requires SENDING for the last user.
    } // Ends the final user status test.

    /** Checks blank text in any saved turn is rejected instead of silently dropped. */
    @Test // Covers historical user text, model text and the current question.
    fun blankTextInAnyTurnIsRejected() { // Replaces each valid turn with whitespace-only text.
        for (index in 0..2) { // Covers every position in the shortest multi-turn request.
            val messages = history(1).toMutableList() // Restores the valid history for this position.
            messages[index] = messages[index].copy(text = " \t\n ") // Supplies text with no usable characters.
            assertInvalid(messages, HistoryValidationError.BLANK_TEXT) // Checks no blank turn enters the payload.
        } // Ends the blank text loop.
    } // Ends the blank message test.

    /** Checks only SENT/SENT/SENDING is accepted across all 27 three-turn status combinations. */
    @Test // Covers every status combination for a completed pair and the pending user.
    fun everyStatusCombinationIsChecked() { // Uses the assignment's single valid state combination as the expected contract.
        val validStatuses = listOf(MessageStatus.SENT, MessageStatus.SENT, MessageStatus.SENDING) // Specifies the one completed exchange plus current request.
        for (first in MessageStatus.entries) { // Varies the old user's exchange state.
            for (second in MessageStatus.entries) { // Varies the model reply state.
                for (third in MessageStatus.entries) { // Varies the current user's state.
                    val statuses = listOf(first, second, third) // Records this test combination without altering role order.
                    val messages = history(1).mapIndexed { index, message -> message.copy(status = statuses[index], errorCategory = if (statuses[index] == MessageStatus.FAILED) MessageErrorCategory.TIMEOUT else null) } // Supplies an error for every failed row.
                    val result = HistoryPayloadBuilder.build("chat", messages) // Validates the combination.
                    assertEquals(statuses == validStatuses, result is HistoryPayloadResult.Success) // Checks all other combinations block preparation.
                } // Ends the current-user status loop.
            } // Ends the model status loop.
        } // Ends the old-user status loop.
    } // Ends the complete status table test.

    /** Checks no successful or pending row may retain an error category. */
    @Test // Covers every known error category at each valid turn position.
    fun staleErrorsOnSentOrSendingRowsAreRejected() { // Introduces inconsistent error metadata into otherwise valid histories.
        for (category in MessageErrorCategory.entries) { // Covers all ten persisted error labels.
            for (index in 0..2) { // Covers old user, model and pending user rows.
                val messages = history(1).toMutableList() // Restores valid status values for each case.
                messages[index] = messages[index].copy(errorCategory = category) // Leaves an error on a row whose state does not allow one.
                assertInvalid(messages, HistoryValidationError.INVALID_MESSAGE_STATE) // Checks failed-state metadata cannot leak into a ready history.
            } // Ends the row position loop.
        } // Ends the error category loop.
    } // Ends the stale error test.

    /** Checks unresolved failures stop a first request and cannot be skipped to send a new user turn. */
    @Test // Covers a failed current row and a failed row followed by another pending user.
    fun unresolvedFailureBlocksLaterTurns() { // Uses the same failed row in both histories.
        val failed = row(0, Role.USER, MessageStatus.FAILED).copy(errorCategory = MessageErrorCategory.TIMEOUT) // Creates a validly recorded failed attempt.
        assertInvalid(listOf(failed), HistoryValidationError.UNRESOLVED_FAILURE) // Checks the failed row must be reactivated before retry.
        assertInvalid(listOf(failed, row(1, Role.USER, MessageStatus.SENDING)), HistoryValidationError.UNRESOLVED_FAILURE) // Checks a new turn cannot bypass the unresolved failure.
        assertInvalid(listOf(failed.copy(errorCategory = null)), HistoryValidationError.UNRESOLVED_FAILURE) // Checks a missing error label still cannot make a failed row sendable.
    } // Ends the unresolved failure test.

    /** Checks a retry becomes valid only after the same final row is pending and its error is cleared. */
    @Test // Exercises the state transition performed by the DAO without mutating the builder input.
    fun retryUsesTheSameRowAfterItsErrorIsCleared() { // Starts with a failed last user after one completed exchange.
        val messages = history(1).toMutableList() // Creates the completed pair and pending user.
        val failed = messages.last().copy(status = MessageStatus.FAILED, errorCategory = MessageErrorCategory.NETWORK_UNAVAILABLE) // Marks only the last attempt failed.
        messages[messages.lastIndex] = failed // Keeps the failed row in its existing position.
        assertInvalid(messages, HistoryValidationError.UNRESOLVED_FAILURE) // Checks the failed history is blocked.
        messages[messages.lastIndex] = failed.copy(status = MessageStatus.SENDING) // Simulates a retry that forgot to clear the old error.
        assertInvalid(messages, HistoryValidationError.INVALID_MESSAGE_STATE) // Checks clearing the error is necessary.
        val retried = failed.copy(status = MessageStatus.SENDING, errorCategory = null) // Reactivates the same identity and sequence as the DAO does.
        messages[messages.lastIndex] = retried // Replaces the failed value without appending another message.
        assertEquals(failed.id, retried.id) // Checks the local row identity is reused.
        assertEquals(failed.seq, retried.seq) // Checks the message position is reused.
        assertEquals(3, readyTurns(messages).size) // Checks the retry keeps exactly one current user turn.
        assertEquals(MessageStatus.FAILED, failed.status) // Checks the builder did not change the original failed object.
    } // Ends the retry contract test.

    /** Checks sorting and conversion never change the supplied rows or their input order. */
    @Test // Covers both successful and rejected validation calls.
    fun inputMessagesAreNeverChanged() { // Takes snapshots before calling the builder.
        val messages = history(2).reversed().toMutableList() // Supplies a mutable list in reverse sequence order.
        val original = messages.toList() // Saves the original values and order.
        readyTurns(messages) // Prepares the ordered output.
        assertEquals(original, messages) // Checks the builder sorted a separate list.
        messages[0] = messages[0].copy(status = MessageStatus.FAILED) // Creates an invalid final message in the input's reverse order.
        val invalidOriginal = messages.toList() // Saves the rejected input values.
        assertInvalid(messages, HistoryValidationError.UNRESOLVED_FAILURE) // Checks the invalid call also returns without changing rows.
        assertEquals(invalidOriginal, messages) // Checks validation did not attempt a retry or filter rows.
    } // Ends the input immutability test.

    /** Checks preparing one chat does not retain turns or failed state for the next call. */
    @Test // Covers separate conversations using the same builder object.
    fun independentCallsDoNotShareHistory() { // Alternates successful and invalid requests between two chat IDs.
        readyTurns(history(2)) // First prepares a longer valid history.
        assertInvalid(emptyList(), HistoryValidationError.EMPTY_HISTORY) // Then checks a rejected input does not retain that output.
        val next = row(0, Role.USER, MessageStatus.SENDING, "Other question").copy(chatId = "other-chat") // Creates a pending message belonging to another chat.
        val result = HistoryPayloadBuilder.build("other-chat", listOf(next)) // Reuses the builder for the independent chat.
        assertEquals(HistoryPayloadResult.Success(listOf(ConversationTurn(Role.USER, next.text))), result) // Checks only this chat's turn is returned.
    } // Ends the independent call test.

    private fun history(exchanges: Int): List<Message> { // Builds only valid local fixtures for the contract tests.
        val messages = mutableListOf<Message>() // Collects completed pairs followed by the current user.
        for (index in 0 until exchanges) { // Adds each completed exchange in sequence order.
            messages.add(row(2L * index, Role.USER, MessageStatus.SENT, "Question $index")) // Adds the completed user's row.
            messages.add(row(2L * index + 1, Role.MODEL, MessageStatus.SENT, "Answer $index")) // Adds the model reply for that exchange.
        } // Ends the completed exchange loop.
        messages.add(row(2L * exchanges, Role.USER, MessageStatus.SENDING, "Next question")) // Adds exactly one current pending user.
        return messages // Supplies the complete fixture without IDs from any server.
    } // Ends the valid history helper.

    private fun row(seq: Long, role: Role, status: MessageStatus, text: String = "Message $seq") = Message( // Creates one harmless local message for the selected case.
        id = "message-$seq", // Assigns a unique local identity based on the fixture position.
        chatId = "chat", // Associates the default fixture with the requested chat.
        role = role, // Uses the author selected by the test.
        text = text, // Uses the full test text, including deliberate whitespace or Unicode.
        seq = seq, // Uses the position selected by the test.
        status = status, // Uses the exchange state selected by the test.
        createdAt = 100, // Supplies an unrelated fixed timestamp so ordering must use seq.
    ) // Ends the message fixture.

    private fun readyTurns(messages: List<Message>): List<ConversationTurn> { // Reads a successful result without nullable casts or assertions.
        return when (val result = HistoryPayloadBuilder.build("chat", messages)) { // Validates the test chat's complete supplied history.
            is HistoryPayloadResult.Success -> result.turns // Returns the prepared domain turns for content assertions.
            is HistoryPayloadResult.Invalid -> error("Expected valid test history: ${result.reason}") // Prints only a safe enum label if a valid fixture is rejected.
        } // Ends the exhaustive result check.
    } // Ends the successful result helper.

    private fun assertInvalid(messages: List<Message>, reason: HistoryValidationError) { // Requires rejection without producing a partial turn list.
        val result = HistoryPayloadBuilder.build("chat", messages) // Validates the selected malformed fixture.
        assertTrue("Invalid histories must have no ready payload", result is HistoryPayloadResult.Invalid) // Checks invalid input cannot be sent as a partial history.
        assertEquals(HistoryPayloadResult.Invalid(reason), result) // Checks the safe category explaining the rejection.
    } // Ends the rejection helper.
} // Ends the history builder tests.
