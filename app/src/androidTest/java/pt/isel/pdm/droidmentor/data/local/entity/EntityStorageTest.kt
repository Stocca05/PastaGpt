package pt.isel.pdm.droidmentor.data.local.entity // Groups the tests of entity storage.

import android.database.sqlite.SQLiteConstraintException // Represents a database constraint violation.
import androidx.room.Room // Builds the temporary Room database.
import androidx.test.ext.junit.runners.AndroidJUnit4 // Runs these tests on an Android device.
import androidx.test.platform.app.InstrumentationRegistry // Provides the installed app's test context.
import kotlinx.coroutines.runBlocking // Lets a test call suspend DAO methods; used only in test code.
import org.junit.After // Marks cleanup that runs after each test.
import org.junit.Assert.assertEquals // Compares expected and stored values.
import org.junit.Before // Marks setup that runs before each test.
import org.junit.Test // Marks each test case.
import org.junit.runner.RunWith // Selects the Android test runner.
import pt.isel.pdm.droidmentor.domain.MessageErrorCategory // Supplies the error labels to save and read.
import pt.isel.pdm.droidmentor.domain.MessageStatus // Supplies pending, completed and failed states.
import pt.isel.pdm.droidmentor.domain.Role // Supplies the user and model roles.

/** Checks the real SQLite behaviour of the M2 entities and enum converters. */
@RunWith(AndroidJUnit4::class) // Runs the class using the Android JUnit runner.
class EntityStorageTest { // Keeps the entity storage cases together.
    private lateinit var database: EntityTestDatabase // Holds the fresh database created for each test.
    private lateinit var dao: EntityTestDao // Holds its generated test operations.

    /** Creates a separate in-memory database before each test. */
    @Before // Runs this method before the test body.
    fun createDatabase() { // Prepares the test database and its DAO.
        val context = InstrumentationRegistry.getInstrumentation().targetContext // Uses the installed test app context.
        database = Room.inMemoryDatabaseBuilder(context, EntityTestDatabase::class.java).build() // Creates a disposable database.
        dao = database.testDao() // Gets the generated operations for this database.
    } // Ends the setup method.

    /** Closes the temporary database after each test. */
    @After // Runs this cleanup even when a test fails.
    fun closeDatabase() { // Releases the database resources.
        database.close() // Discards the in-memory rows when this database closes.
    } // Ends the cleanup method.

    /** Checks that completed user/model rows retain their fields and correct sequence order. */
    @Test // Checks enum conversion while reading actual stored rows.
    fun completedExchangeKeepsItsValuesAndOrder() = runBlocking { // Allows calls to the suspend test DAO.
        dao.insertChat(chat("chat-1")) // Creates the parent before inserting messages.
        val user = message("user-1", "chat-1", 0L, status = MessageStatus.SENT) // Defines the completed user turn.
        val model = message("model-1", "chat-1", 1L, Role.MODEL, MessageStatus.SENT) // Defines the completed model reply.
        dao.insertMessage(model) // Inserts the later row first to avoid relying on insertion order.
        dao.insertMessage(user) // Inserts the earlier row second.
        assertEquals(listOf(user, model), dao.messages("chat-1")) // Checks ordering, values and both role conversions.
    } // Ends the completed-exchange test.

    /** Checks a sequence larger than an Int and a null error category through SQLite. */
    @Test // Checks that the chosen column types preserve these values.
    fun pendingMessageKeepsLongSequenceAndNullError() = runBlocking { // Calls the suspend database operations.
        dao.insertChat(chat("chat-1")) // Creates the message's parent chat.
        val pending = message("pending-1", "chat-1", Int.MAX_VALUE.toLong() + 1L) // Uses a sequence that needs Long storage.
        dao.insertMessage(pending) // Stores a pending user message with no error.
        assertEquals(listOf(pending), dao.messages("chat-1")) // Checks that neither the sequence nor null was changed.
    } // Ends the pending-message test.

    /** Checks that every saved error label can be read back as its typed category. */
    @Test // Checks the converter through actual failed-message rows.
    fun failedMessagesKeepTheirErrorCategories() = runBlocking { // Calls the suspend test DAO.
        for (category in MessageErrorCategory.entries) { // Checks every current error category.
            val chatId = "chat-${category.name}" // Gives each failed message its own chat.
            dao.insertChat(chat(chatId)) // Creates that failed message's parent row.
            val failed = message("failed-${category.name}", chatId, 0L, status = MessageStatus.FAILED, error = category) // Defines its error state.
            dao.insertMessage(failed) // Saves the failed message and its category.
            assertEquals(listOf(failed), dao.messages(chatId)) // Checks that the stored error is restored exactly.
        } // Ends the loop over categories.
    } // Ends the failed-message test.

    /** Checks that deleting one chat removes its messages and preserves another chat's rows. */
    @Test // Exercises the actual foreign-key cascade.
    fun deletingChatDeletesOnlyItsMessages() = runBlocking { // Allows suspend test queries.
        dao.insertChat(chat("removed-chat")) // Creates the chat that will be deleted.
        dao.insertChat(chat("kept-chat")) // Creates a separate chat that must remain.
        dao.insertMessage(message("removed-message", "removed-chat", 0L)) // Adds a child to the first chat.
        val kept = message("kept-message", "kept-chat", 0L) // Defines the child that must survive.
        dao.insertMessage(kept) // Saves the second chat's child.
        dao.deleteChat("removed-chat") // Deletes only the first parent.
        assertEquals(emptyList<MessageEntity>(), dao.messages("removed-chat")) // Checks that its child was removed automatically.
        assertEquals(listOf(kept), dao.messages("kept-chat")) // Checks that the other chat's child still exists.
    } // Ends the cascade test.

    /** Checks that two different message IDs cannot use the same position in one chat. */
    @Test(expected = SQLiteConstraintException::class) // Expects SQLite to reject the duplicate position.
    fun duplicateSequenceInSameChatIsRejected() = runBlocking { // Allows suspend inserts in this test.
        dao.insertChat(chat("chat-1")) // Creates the common parent chat.
        dao.insertMessage(message("message-1", "chat-1", 0L)) // Occupies the first position.
        dao.insertMessage(message("message-2", "chat-1", 0L)) // Tries to occupy the same position with a new ID.
    } // Ends the uniqueness test.

    /** Checks that the sequence is unique per chat rather than across the whole database. */
    @Test // Checks the scope of the composite unique index.
    fun differentChatsCanUseTheSameSequence() = runBlocking { // Allows suspend database operations.
        dao.insertChat(chat("chat-1")) // Creates the first chat.
        dao.insertChat(chat("chat-2")) // Creates the second chat.
        val first = message("message-1", "chat-1", 0L) // Uses sequence zero in the first chat.
        val second = message("message-2", "chat-2", 0L) // Uses the same sequence in a different chat.
        dao.insertMessage(first) // Saves the first message.
        dao.insertMessage(second) // Saves the second message without a uniqueness conflict.
        assertEquals(listOf(first), dao.messages("chat-1")) // Checks the first chat's row.
        assertEquals(listOf(second), dao.messages("chat-2")) // Checks the second chat's row.
    } // Ends the per-chat sequence test.

    /** Checks that a message cannot reference a missing parent chat. */
    @Test(expected = SQLiteConstraintException::class) // Expects the foreign key to reject the row.
    fun messageWithoutParentChatIsRejected() = runBlocking { // Allows the suspend insert to run.
        dao.insertMessage(message("orphan-message", "missing-chat", 0L)) // Attempts an insert without creating the parent.
    } // Ends the missing-parent test.

    private fun chat(id: String): ChatEntity { // Creates a small parent row for a test.
        return ChatEntity(id = id, title = "Test chat", createdAt = 1000L, updatedAt = 1000L) // Uses fixed times so assertions are repeatable.
    } // Ends the chat helper.

    private fun message( // Creates a small message row with values chosen by the test.
        id: String, // Receives the local message ID.
        chatId: String, // Receives the parent chat ID.
        seq: Long, // Receives the position inside that chat.
        role: Role = Role.USER, // Uses the user role unless the test needs a model reply.
        status: MessageStatus = MessageStatus.SENDING, // Uses a pending message by default.
        error: MessageErrorCategory? = null, // Records no error unless one is provided.
    ): MessageEntity { // Returns the row used in the test.
        return MessageEntity( // Builds the real production entity.
            id = id, // Copies the message ID.
            chatId = chatId, // Copies its parent ID.
            role = role, // Copies its author role.
            text = "Test message", // Uses fixed text with no credentials.
            seq = seq, // Copies the per-chat position.
            status = status, // Copies the state needed by this case.
            createdAt = 1000L, // Uses a fixed creation time.
            errorCategory = error, // Copies the optional error category.
        ) // Ends the entity constructor.
    } // Ends the message helper.
} // Ends the test class.
