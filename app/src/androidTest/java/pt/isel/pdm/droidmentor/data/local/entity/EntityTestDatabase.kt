package pt.isel.pdm.droidmentor.data.local.entity // Keeps the entity test helpers together.

import androidx.room.Dao // Marks the interface containing test database operations.
import androidx.room.Database // Lists the entities used by the test database.
import androidx.room.Insert // Generates a method that inserts a row.
import androidx.room.Query // Generates a method from its SQL query.
import androidx.room.RoomDatabase // Base class needed by a Room database.
import pt.isel.pdm.droidmentor.data.local.dao.ChatDao // Adds the real chat operations to the test schema.
import pt.isel.pdm.droidmentor.data.local.dao.MessageDao // Adds the real message operations to the test schema.

/** In-memory database for entity and DAO checks, separate from the future app database. */
@Database( // Creates a schema for these tests only.
    entities = [ChatEntity::class, MessageEntity::class], // Uses the real app entities.
    version = 1, // Gives the temporary test schema a version.
    exportSchema = false, // This disposable test schema is not a shipped app schema.
) // Ends the test database settings.
abstract class EntityTestDatabase : RoomDatabase() { // Lets Room generate the database implementation.
    /** Returns the small set of operations needed by entity tests. */
    abstract fun testDao(): EntityTestDao // Provides the generated test DAO.

    /** Returns the production chat DAO for tests of its real queries. */
    abstract fun chatDao(): ChatDao // Lets Room generate and validate the app's chat DAO.

    /** Returns the production message DAO for tests of its real transactions. */
    abstract fun messageDao(): MessageDao // Lets Room generate and validate the app's message DAO.
} // Ends the test database class.

/** Minimal database operations used only to test the entity definitions. */
@Dao // Tells Room to generate these operations.
interface EntityTestDao { // Defines the test queries without implementing the app DAOs.
    /** Inserts one parent chat, rejecting duplicate IDs. */
    @Insert // Uses Room's default ABORT policy when a constraint is violated.
    suspend fun insertChat(chat: ChatEntity) // Runs the insert through Room's coroutine support.

    /** Inserts one message, checking its foreign key and unique position. */
    @Insert // Rejects a duplicate row instead of overwriting existing history.
    suspend fun insertMessage(message: MessageEntity) // Inserts the child row in its chat.

    /** Reads all messages of a chat in sequence order. */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY seq ASC") // Filters the chat and sorts its messages.
    suspend fun messages(chatId: String): List<MessageEntity> // Returns the stored rows to the test.

    /** Deletes a parent chat so tests can check the cascade. */
    @Query("DELETE FROM chats WHERE id = :chatId") // Removes the selected parent row.
    suspend fun deleteChat(chatId: String) // Lets the foreign key delete its child rows.
} // Ends the test DAO interface.
