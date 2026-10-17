package pt.isel.pdm.droidmentor.data.local.dao // Groups the app's database operations.

import androidx.room.Dao // Marks this interface as a Room DAO.
import androidx.room.Insert // Generates an insert that checks database constraints.
import androidx.room.Query // Generates an operation from a SQL statement.
import androidx.room.Transaction // Keeps a chat and its related message in the same read snapshot.
import kotlinx.coroutines.flow.Flow // Emits new query results when the stored rows change.
import pt.isel.pdm.droidmentor.data.local.entity.ChatEntity // Represents a stored chat.
import pt.isel.pdm.droidmentor.data.local.relation.ChatWithLastMessage // Represents a chat-list item and its preview.

/** Reads and changes local chats through Room. */
@Dao // Lets Room generate the implementation of these methods.
interface ChatDao { // Declares the operations needed for chat persistence.
    /** Observes chats by most recent update, including only each chat's latest message. */
    @Transaction // Keeps the main query and the message relation consistent.
    @Query( // Selects each chat and the ID needed to load its last message.
        "SELECT chats.*, " + // Reads the stored chat columns.
            "(SELECT id FROM messages WHERE chatId = chats.id ORDER BY seq DESC LIMIT 1) AS lastMessageId " + // Picks the latest position.
            "FROM chats ORDER BY updatedAt DESC, id ASC" // Sorts by recency, with a stable ID order for ties.
    ) // Ends the chat-list query.
    fun observeChats(): Flow<List<ChatWithLastMessage>> // Notifies the caller about chat or message changes.

    /** Reads one chat, returning null if it no longer exists. */
    @Query("SELECT * FROM chats WHERE id = :chatId") // Searches by the local chat ID.
    suspend fun getById(chatId: String): ChatEntity? // Reads the row without blocking the caller's thread.

    /** Inserts a chat without replacing another row with the same ID. */
    @Insert // Uses the default ABORT policy to preserve existing history.
    suspend fun insert(chat: ChatEntity) // Saves the parent row through Room's coroutine support.

    /** Updates a title and its timestamp, returning the number of changed rows. */
    @Query("UPDATE chats SET title = :title, updatedAt = MAX(updatedAt, :updatedAt) WHERE id = :chatId") // Changes only this chat.
    suspend fun updateTitle(chatId: String, title: String, updatedAt: Long): Int // Returns zero when the chat is missing.

    /** Deletes one chat and lets its foreign key remove the child messages. */
    @Query("DELETE FROM chats WHERE id = :chatId") // Deletes the selected parent row.
    suspend fun deleteById(chatId: String): Int // Reports whether a chat was actually deleted.
} // Ends the chat DAO.
