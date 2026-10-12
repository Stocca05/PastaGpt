package pt.isel.pdm.droidmentor.data.local.dao // Groups the tests of the app's DAO operations.

import android.database.sqlite.SQLiteConstraintException // Represents a rejected insert in SQLite.
import androidx.room.Room // Builds the database used by each test.
import androidx.test.ext.junit.runners.AndroidJUnit4 // Runs the tests on an Android device.
import androidx.test.platform.app.InstrumentationRegistry // Provides the installed app context.
import kotlinx.coroutines.CompletableDeferred // Signals that a Flow has emitted its initial value.
import kotlinx.coroutines.async // Runs concurrent inserts or a Flow collector in a child coroutine.
import kotlinx.coroutines.awaitAll // Waits for all concurrent insert results.
import kotlinx.coroutines.flow.distinctUntilChanged // Ignores repeated snapshots with the same values.
import kotlinx.coroutines.flow.first // Reads one matching result from a Flow.
import kotlinx.coroutines.flow.onEach // Signals when the first observed snapshot arrives.
import kotlinx.coroutines.flow.take // Limits a collector to the snapshots needed by the test.
import kotlinx.coroutines.flow.toList // Collects those snapshots for assertions.
import kotlinx.coroutines.runBlocking // Lets test methods call suspend methods; never used in app code.
import kotlinx.coroutines.withTimeout // Prevents a broken observer from leaving a test waiting forever.
import org.junit.After // Marks database cleanup after each test.
import org.junit.Assert.assertEquals // Checks stored values against the expected results.
import org.junit.Assert.assertFalse // Checks that an invalid operation was rejected.
import org.junit.Assert.assertNull // Checks missing rows and empty previews.
import org.junit.Assert.assertTrue // Checks successful writes and rejected constraints.
import org.junit.Before // Marks setup before each test.
import org.junit.Test // Marks each test case.
import org.junit.runner.RunWith // Chooses the Android test runner.
import pt.isel.pdm.droidmentor.data.local.entity.ChatEntity // Supplies chat rows for the tests.
import pt.isel.pdm.droidmentor.data.local.DroidMentorDatabase // Uses the real app schema for these DAO tests.
import pt.isel.pdm.droidmentor.data.local.entity.MessageEntity // Supplies message rows for the tests.
import pt.isel.pdm.droidmentor.domain.MessageErrorCategory // Supplies saved failure labels.
import pt.isel.pdm.droidmentor.domain.MessageStatus // Supplies the pending, completed and failed states.
import pt.isel.pdm.droidmentor.domain.Role // Supplies the user and model roles.

/** Checks production DAO queries and transactions against real SQLite. */
@RunWith(AndroidJUnit4::class) // Uses the Android JUnit runner.
class DaoStorageTest { // Groups the DAO behaviour checks.
    private lateinit var database: DroidMentorDatabase // Holds an in-memory instance of the real app database.
    private lateinit var chats: ChatDao // Holds the generated production chat DAO.
    private lateinit var messages: MessageDao // Holds the generated production message DAO.

    /** Creates the in-memory database and gets the real app DAOs. */
    @Before // Runs setup before each test.
    fun createDatabase() { // Prepares a clean database with no saved user data.
        val context = InstrumentationRegistry.getInstrumentation().targetContext // Uses the installed app context.
        database = Room.inMemoryDatabaseBuilder(context, DroidMentorDatabase::class.java).build() // Uses the actual app schema without a disk file.
        chats = database.chatDao() // Gets the generated chat operations.
        messages = database.messageDao() // Gets the generated message operations.
    } // Ends test setup.

    /** Closes the temporary database after each case. */
    @After // Runs even when an assertion fails.
    fun closeDatabase() { // Releases the database resources.
        database.close() // Discards all rows from this case.
    } // Ends test cleanup.

    /** Checks empty previews, recency order and stable ordering for equal timestamps. */
    @Test // Checks the chat-list query and its nullable relation.
    fun emptyChatsHaveNullPreviewsAndStableOrder() = runBlocking { // Allows calls to suspend DAO methods.
        chats.insert(chat("chat-b", 2000L)) // Inserts a recent chat.
        chats.insert(chat("chat-a", 2000L)) // Inserts a chat with the same timestamp but an earlier ID.
        chats.insert(chat("older-chat", 1000L)) // Inserts an older conversation.
        val result = withTimeout(5000L) { chats.observeChats().first() } // Reads the first consistent snapshot.
        assertEquals(listOf("chat-a", "chat-b", "older-chat"), result.map { it.chat.id }) // Checks both sort keys.
        assertTrue(result.all { it.lastMessageId == null && it.lastMessage == null }) // Checks that empty chats keep a null preview.
        assertNull(chats.getById("missing-chat")) // Checks the nullable single-chat lookup too.
    } // Ends the empty-chat test.

    /** Checks that a preview uses sequence order rather than message timestamps. */
    @Test // Checks the latest-message subquery and the Room relation.
    fun chatPreviewUsesHighestSequence() = runBlocking { // Calls the suspend DAO operations.
        chats.insert(chat("chat-1")) // Creates the parent conversation.
        messages.insert(message("first", "chat-1", createdAt = 3000L)) // Saves a first row with a later timestamp.
        val latest = messages.insert(message("last", "chat-1", role = Role.MODEL, status = MessageStatus.SENT, createdAt = 2000L)) // Saves the higher sequence.
        val result = withTimeout(5000L) { chats.observeChats().first() } // Reads the parent and selected message together.
        assertEquals("last", result.single().lastMessageId) // Checks the extra ID selected by SQL.
        assertEquals(latest, result.single().lastMessage) // Checks that the actual last row is loaded.
        assertEquals(3000L, result.single().chat.updatedAt) // Checks that an older timestamp did not move recency backwards.
    } // Ends the preview-order test.

    /** Checks that an observed chat list emits a title update. */
    @Test // Checks the production chat Flow instead of only a one-shot query.
    fun chatFlowEmitsTitleChanges() = runBlocking { // Provides a scope for the observer and write.
        chats.insert(chat("chat-1")) // Creates the initial chat.
        val ready = CompletableDeferred<Unit>() // Will signal that the old title has been observed.
        val observer = async { // Collects before the title update is made.
            withTimeout(5000L) { // Limits the wait for the two distinct snapshots.
                chats.observeChats().distinctUntilChanged().onEach { ready.complete(Unit) }.take(2).toList() // Saves old and new results.
            } // Ends the observer timeout.
        } // Ends the observer coroutine setup.
        withTimeout(5000L) { ready.await() } // Waits until the initial snapshot has actually arrived.
        assertEquals(1, chats.updateTitle("chat-1", "Updated title", 2000L)) // Changes one stored chat.
        val snapshots = observer.await() // Waits for the new Flow value.
        assertEquals("Test chat", snapshots[0].single().chat.title) // Checks the initial state.
        assertEquals("Updated title", snapshots[1].single().chat.title) // Checks the update was emitted.
        assertEquals(0, chats.updateTitle("missing-chat", "Unused", 3000L)) // Checks an absent ID changes no rows.
    } // Ends the chat Flow test.

    /** Checks that a message Flow emits a newly inserted row. */
    @Test // Checks that Room is a working observation source for chat content.
    fun messageFlowEmitsInsertedRows() = runBlocking { // Provides the observer's coroutine scope.
        chats.insert(chat("chat-1")) // Creates the empty parent chat.
        val ready = CompletableDeferred<Unit>() // Signals that the initial empty history was read.
        val observer = async { // Starts collecting before the insert.
            withTimeout(5000L) { // Prevents a missing emission from hanging the test.
                messages.observeMessages("chat-1").distinctUntilChanged().onEach { ready.complete(Unit) }.take(2).toList() // Collects two values.
            } // Ends the observer timeout.
        } // Ends the observer setup.
        withTimeout(5000L) { ready.await() } // Waits for the empty-history snapshot.
        val stored = messages.insert(message("user-1", "chat-1")) // Saves the first real message.
        val snapshots = observer.await() // Waits for the insert to be observed.
        assertEquals(emptyList<MessageEntity>(), snapshots[0]) // Checks the initial empty history.
        assertEquals(listOf(stored), snapshots[1]) // Checks the emitted message has its allocated sequence.
    } // Ends the message Flow test.

    /** Checks that changing only a message error updates the related chat-list preview. */
    @Test // Checks invalidation of the message relation when the chat row stays the same.
    fun chatPreviewFlowEmitsMessageErrorChanges() = runBlocking { // Provides a scope for observation and the status write.
        chats.insert(chat("chat-1")) // Creates the parent row with a fixed timestamp.
        val pending = messages.insert(message("user-1", "chat-1")) // Saves the row used by the chat preview.
        val ready = CompletableDeferred<Unit>() // Signals that its pending preview was observed.
        val observer = async { // Collects the preview before changing only the child row.
            withTimeout(5000L) { // Limits the wait for both distinct previews.
                chats.observeChats().distinctUntilChanged().onEach { ready.complete(Unit) }.take(2).toList() // Collects initial and failed states.
            } // Ends the observer timeout.
        } // Ends the collector setup.
        withTimeout(5000L) { ready.await() } // Waits for the initial parent-and-message snapshot.
        assertTrue(messages.updateStatus(pending.id, MessageStatus.FAILED, MessageErrorCategory.TIMEOUT)) // Changes only the message state.
        val snapshots = observer.await() // Waits for the related preview to be emitted again.
        assertEquals(MessageStatus.SENDING, snapshots[0].single().lastMessage?.status) // Checks the initial child state.
        assertEquals(MessageStatus.FAILED, snapshots[1].single().lastMessage?.status) // Checks the changed child state is visible.
        assertEquals(MessageErrorCategory.TIMEOUT, snapshots[1].single().lastMessage?.errorCategory) // Checks the typed error is visible too.
        assertEquals(snapshots[0].single().chat, snapshots[1].single().chat) // Confirms the parent row was not needed to trigger the update.
    } // Ends the relation-invalidation test.

    /** Checks that simultaneous inserts receive distinct consecutive positions. */
    @Test // Exercises the whole allocation-and-insert transaction under concurrency.
    fun concurrentInsertsAllocateConsecutiveSequences() = runBlocking { // Provides a structured scope for all inserts.
        chats.insert(chat("chat-1")) // Creates the common parent chat.
        val writes = (0 until 20).map { index -> // Starts twenty writes with the same initial sequence and timestamp.
            async { messages.insert(message("message-$index", "chat-1")) } // Lets the DAO allocate each real position.
        } // Ends the list of concurrent insert jobs.
        val stored = writes.awaitAll() // Waits for every transaction to finish.
        assertEquals((0L until 20L).toList(), stored.map { it.seq }.sorted()) // Checks that every position was allocated once.
        val history = messages.getMessages("chat-1") // Reads the final stored order.
        assertEquals((0L until 20L).toList(), history.map { it.seq }) // Checks the read query follows the same order.
        assertEquals(20, history.map { it.id }.toSet().size) // Checks no row was replaced or lost.
    } // Ends the concurrent-insert test.

    /** Checks that allocating a position in one chat does not advance another chat. */
    @Test // Checks per-chat sequence allocation.
    fun sequenceAllocationIsSeparateForEachChat() = runBlocking { // Calls the suspend insertion method.
        chats.insert(chat("chat-1")) // Creates the first parent chat.
        chats.insert(chat("chat-2")) // Creates the second parent chat.
        assertEquals(0L, messages.insert(message("first", "chat-1")).seq) // Allocates the first chat's initial position.
        assertEquals(1L, messages.insert(message("second", "chat-1")).seq) // Allocates its next position.
        assertEquals(0L, messages.insert(message("other", "chat-2")).seq) // Starts the second chat independently.
    } // Ends the independent-allocation test.

    /** Checks that failure and retry reuse the same row and clear the saved error. */
    @Test // Checks the DAO's guarded status update.
    fun retryKeepsTheSameMessageAndClearsItsError() = runBlocking { // Calls suspend state changes.
        chats.insert(chat("chat-1")) // Creates the conversation.
        val pending = messages.insert(message("user-1", "chat-1")) // Saves the original pending row.
        assertTrue(messages.updateStatus(pending.id, MessageStatus.FAILED, MessageErrorCategory.TIMEOUT)) // Saves a typed failure.
        val failed = messages.getMessages("chat-1").single() // Reads the failure from Room.
        assertEquals(MessageStatus.FAILED, failed.status) // Checks the failed state was stored.
        assertEquals(MessageErrorCategory.TIMEOUT, failed.errorCategory) // Checks the error was stored too.
        assertTrue(messages.updateStatus(pending.id, MessageStatus.SENDING)) // Prepares the same row for retry.
        assertEquals(listOf(pending), messages.getMessages("chat-1")) // Checks the ID and position are unchanged and the error is null.
    } // Ends the retry-state test.

    /** Checks full exchange completion and rejection of a repeated response. */
    @Test // Exercises the two-write success transaction.
    fun completedExchangeSavesBothRowsOnlyOnce() = runBlocking { // Calls the suspend exchange method.
        chats.insert(chat("chat-1")) // Creates the parent conversation.
        val pending = messages.insert(message("user-1", "chat-1")) // Saves the pending user turn.
        assertTrue(messages.completeExchange(pending.id, "reply-1", "Mentor reply", 2000L)) // Saves and completes the exchange.
        val history = messages.getMessages("chat-1") // Reads the committed history.
        assertEquals(listOf(Role.USER, Role.MODEL), history.map { it.role }) // Checks the expected two roles.
        assertEquals(listOf(0L, 1L), history.map { it.seq }) // Checks adjacent positions.
        assertTrue(history.all { it.status == MessageStatus.SENT && it.errorCategory == null }) // Checks both committed states.
        assertEquals(2000L, chats.getById("chat-1")?.updatedAt) // Checks recency was part of the commit.
        assertFalse(messages.completeExchange(pending.id, "reply-2", "Repeated reply", 3000L)) // Refuses to complete an already sent row.
        assertEquals(history, messages.getMessages("chat-1")) // Checks that no extra model row appeared.
    } // Ends the exchange-completion test.

    /** Checks that an insertion failure rolls back the earlier SENT change and chat timestamp. */
    @Test // Checks atomicity using a deliberate primary-key collision.
    fun failedReplyInsertRollsBackExchangeCompletion() = runBlocking { // Runs the transaction that must fail.
        chats.insert(chat("chat-1")) // Creates the conversation with a fixed timestamp.
        val pending = messages.insert(message("user-1", "chat-1")) // Saves the only message in the chat.
        expectConstraintViolation { // Expects the reply to collide with the existing user ID.
            messages.completeExchange(pending.id, pending.id, "Mentor reply", 4000L) // Fails after the transaction's SENT update.
        } // Ends the expected-failure block.
        assertEquals(listOf(pending), messages.getMessages("chat-1")) // Checks the row is still SENDING and no reply was saved.
        assertEquals(1000L, chats.getById("chat-1")?.updatedAt) // Checks the timestamp did not advance.
    } // Ends the completion-rollback test.

    /** Checks that duplicate message IDs do not replace existing history or consume a position. */
    @Test // Checks the atomic insert's conflict policy.
    fun duplicateIdDoesNotReplaceHistory() = runBlocking { // Runs the original and conflicting inserts.
        chats.insert(chat("chat-1")) // Creates the parent chat.
        val original = messages.insert(message("user-1", "chat-1")) // Saves the original row at position zero.
        expectConstraintViolation { messages.insert(message("user-1", "chat-1", createdAt = 4000L)) } // Rejects the repeated ID.
        assertEquals(listOf(original), messages.getMessages("chat-1")) // Checks the existing row was preserved.
        assertEquals(1000L, chats.getById("chat-1")?.updatedAt) // Checks failed insertion did not update recency.
        assertEquals(1L, messages.insert(message("user-2", "chat-1")).seq) // Checks the next successful insert has no gap.
    } // Ends the duplicate-ID test.

    /** Checks that state updates cannot bypass exchange completion or attach invalid errors. */
    @Test // Checks the state method's rejection paths.
    fun invalidStatusChangesLeaveRowsUnchanged() = runBlocking { // Calls the guarded status operation.
        chats.insert(chat("chat-1")) // Creates the parent conversation.
        val pending = messages.insert(message("user-1", "chat-1")) // Saves the pending user message.
        assertFalse(messages.updateStatus(pending.id, MessageStatus.SENT)) // Requires completeExchange to save a usable reply.
        assertFalse(messages.updateStatus(pending.id, MessageStatus.FAILED)) // Refuses a failure without an error label.
        assertFalse(messages.updateStatus(pending.id, MessageStatus.SENDING, MessageErrorCategory.TIMEOUT)) // Refuses an error on a retry.
        assertFalse(messages.updateStatus("missing", MessageStatus.FAILED, MessageErrorCategory.UNKNOWN)) // Rejects a missing row safely.
        assertEquals(listOf(pending), messages.getMessages("chat-1")) // Checks no invalid request changed the stored data.
        assertTrue(messages.completeExchange(pending.id, "reply-1", "Mentor reply", 2000L)) // Completes the real exchange.
        val completed = messages.getMessages("chat-1") // Saves its completed state for comparison.
        assertFalse(messages.updateStatus("reply-1", MessageStatus.FAILED, MessageErrorCategory.TIMEOUT)) // Protects the model row.
        assertFalse(messages.updateStatus(pending.id, MessageStatus.FAILED, MessageErrorCategory.TIMEOUT)) // Protects the completed user row.
        assertEquals(completed, messages.getMessages("chat-1")) // Checks the completed exchange remained unchanged.
    } // Ends the invalid-status test.

    /** Checks that empty replies, failed sends and non-final user rows cannot be completed. */
    @Test // Checks the exchange method's rejection paths.
    fun invalidExchangeRequestsDoNotAddReplies() = runBlocking { // Calls the completion transaction with invalid requests.
        chats.insert(chat("chat-1")) // Creates the parent chat.
        val first = messages.insert(message("user-1", "chat-1")) // Saves the first pending user row.
        assertFalse(messages.completeExchange(first.id, "empty", "   ", 2000L)) // Rejects an empty reply.
        val second = messages.insert(message("user-2", "chat-1")) // Simulates an unexpected later stored turn.
        assertFalse(messages.completeExchange(first.id, "out-of-order", "Reply", 2000L)) // Rejects completion of the earlier row.
        assertTrue(messages.updateStatus(second.id, MessageStatus.FAILED, MessageErrorCategory.SERVER_ERROR)) // Marks the final row failed.
        assertFalse(messages.completeExchange(second.id, "failed", "Reply", 2000L)) // Requires retry to make the row pending first.
        assertFalse(messages.completeExchange("missing", "missing-reply", "Reply", 2000L)) // Rejects an absent user row.
        assertEquals(2, messages.getMessages("chat-1").size) // Checks no model reply was inserted.
    } // Ends the invalid-exchange test.

    /** Checks deletion and cascade through the production DAO methods. */
    @Test // Checks the app DAO's deletion query and resulting observed history.
    fun deletingChatClearsItsObservedHistory() = runBlocking { // Calls the suspend delete method.
        chats.insert(chat("chat-1")) // Creates the parent conversation.
        messages.insert(message("user-1", "chat-1")) // Adds a child row.
        assertEquals(1, chats.deleteById("chat-1")) // Deletes the one matching parent.
        assertNull(messages.getById("user-1")) // Checks that the foreign key removed its child.
        assertTrue(withTimeout(5000L) { messages.observeMessages("chat-1").first() }.isEmpty()) // Checks the resulting history snapshot.
        assertTrue(withTimeout(5000L) { chats.observeChats().first() }.isEmpty()) // Checks the resulting chat-list snapshot.
        assertEquals(0, chats.deleteById("chat-1")) // Reports no deletion for the now-missing parent.
    } // Ends the deletion test.

    private suspend fun expectConstraintViolation(action: suspend () -> Unit) { // Checks a real failed database write.
        var rejected = false // Records whether SQLite rejected the operation.
        try { // Runs the caller's insert or transaction.
            action() // Executes the write that should violate a constraint.
        } catch (error: SQLiteConstraintException) { // Catches only the expected database failure.
            rejected = true // Records that the constraint was enforced.
        } // Ends the expected-error handling.
        assertTrue("SQLite should reject this write", rejected) // Fails if the operation unexpectedly succeeded.
    } // Ends the constraint helper.

    private fun chat(id: String, updatedAt: Long = 1000L): ChatEntity { // Creates a test chat with repeatable times.
        return ChatEntity(id = id, title = "Test chat", createdAt = 1000L, updatedAt = updatedAt) // Returns the parent row.
    } // Ends the chat helper.

    private fun message( // Creates a row whose initial sequence the DAO must replace.
        id: String, // Receives the local message ID.
        chatId: String, // Receives the owning chat ID.
        role: Role = Role.USER, // Uses a user turn unless another role is supplied.
        status: MessageStatus = MessageStatus.SENDING, // Uses a pending row by default.
        createdAt: Long = 1000L, // Uses a repeatable time unless the test needs another value.
    ): MessageEntity { // Returns the entity passed to the DAO.
        return MessageEntity( // Constructs a real message row.
            id = id, // Uses the supplied local ID.
            chatId = chatId, // Uses the supplied parent ID.
            role = role, // Uses the role needed by this case.
            text = "Test message", // Supplies fixed content without credentials.
            seq = 999L, // Deliberately supplies a position that must never be trusted by insert.
            status = status, // Uses the requested state.
            createdAt = createdAt, // Uses the requested timestamp.
        ) // Ends the message constructor.
    } // Ends the message helper.
} // Ends the DAO test class.
