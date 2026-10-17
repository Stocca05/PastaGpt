package pt.isel.pdm.droidmentor.data.local.dao // Groups the app's database operations.

import androidx.room.Dao // Marks this class as a Room DAO.
import androidx.room.Insert // Generates the internal row insert.
import androidx.room.Query // Generates methods from SQL queries.
import androidx.room.Transaction // Makes several database operations commit or roll back together.
import androidx.room.TypeConverters // Applies enum conversion to DAO parameters.
import kotlinx.coroutines.flow.Flow // Emits updated message lists from Room.
import pt.isel.pdm.droidmentor.data.local.converter.MessageConverters // Stores enum parameters as text.
import pt.isel.pdm.droidmentor.data.local.entity.MessageEntity // Represents a stored message row.
import pt.isel.pdm.droidmentor.domain.MessageErrorCategory // Represents the saved failure type.
import pt.isel.pdm.droidmentor.domain.MessageStatus // Represents the exchange state.
import pt.isel.pdm.droidmentor.domain.Role // Identifies the user and model rows.

/** Reads messages and performs atomic local writes for their exchanges. */
@Dao // Lets Room generate query methods and transaction wrappers.
@TypeConverters(MessageConverters::class) // Uses the same enum names for columns and query parameters.
abstract class MessageDao { // Allows both generated queries and small transaction methods.
    /** Observes one chat's messages in deterministic sequence order. */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY seq ASC") // Filters the chat and sorts by position.
    abstract fun observeMessages(chatId: String): Flow<List<MessageEntity>> // Emits the stored history as it changes.

    /** Reads the full stored history used to build a stateless request. */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY seq ASC") // Reads every row in this chat.
    abstract suspend fun getMessages(chatId: String): List<MessageEntity> // Returns a snapshot for the repository.

    /** Reads one message, or returns null when its row is missing. */
    @Query("SELECT * FROM messages WHERE id = :messageId") // Searches by local message ID.
    abstract suspend fun getById(messageId: String): MessageEntity? // Returns the row needed by a state change.

    /**
     * Allocates and inserts the next position atomically, also updating chat recency.
     * The supplied sequence is replaced; the returned row contains its saved sequence.
     */
    @Transaction // Stops concurrent inserts from choosing the same position.
    open suspend fun insert(message: MessageEntity): MessageEntity { // Saves a message with a freshly allocated sequence.
        val saved = message.copy(seq = nextSeq(message.chatId)) // Computes the position inside this transaction.
        insertRow(saved) // Inserts the copied row, rejecting duplicate IDs or an absent parent.
        touchChat(saved.chatId, saved.createdAt) // Updates recency in the same transaction.
        return saved // Returns the actual row with its allocated position.
    } // Ends the atomic insert.

    /**
     * Records a user-message failure or prepares that same row for retry.
     * Completing an exchange is reserved for [completeExchange]. Invalid state requests return false.
     */
    @Transaction // Keeps the current-state check and the update together.
    open suspend fun updateStatus( // Changes a pending or failed user row without inserting a duplicate.
        messageId: String, // Selects the local message to change.
        status: MessageStatus, // Chooses SENDING for retry or FAILED for an unsuccessful attempt.
        errorCategory: MessageErrorCategory? = null, // Supplies a failure label, or null to clear it on retry.
    ): Boolean { // Reports whether the requested update could be applied.
        if (status == MessageStatus.SENT) return false // A usable reply must be saved before marking the exchange completed.
        if (status == MessageStatus.FAILED && errorCategory == null) return false // A failure needs a saved reason.
        if (status == MessageStatus.SENDING && errorCategory != null) return false // A retry clears the previous error.
        val current = getById(messageId) ?: return false // Stops when the message no longer exists.
        if (current.role != Role.USER || current.status == MessageStatus.SENT) return false // Keeps completed and model rows unchanged.
        return updateStatusRow(messageId, current.status, status, errorCategory) == 1 // Checks that exactly this row was updated.
    } // Ends the state update.

    /**
     * Saves one usable model reply and marks its pending user row SENT in the same transaction.
     * Returns false for an absent, non-pending or non-final user row, or a blank reply.
     */
    @Transaction // Rolls back both writes if either one fails.
    open suspend fun completeExchange( // Completes the pending exchange without exposing a partial success.
        userMessageId: String, // Selects the user message awaiting this response.
        replyId: String, // Supplies a new local ID for the model row.
        replyText: String, // Supplies the usable response text.
        createdAt: Long, // Supplies the response time in milliseconds.
    ): Boolean { // Reports whether this pending exchange was completed.
        if (replyText.isBlank()) return false // Refuses to save an empty response as a successful exchange.
        val user = getById(userMessageId) ?: return false // Stops if the pending row was removed.
        if (user.role != Role.USER || user.status != MessageStatus.SENDING) return false // Rejects repeats or the wrong role.
        val sequence = nextSeq(user.chatId) // Finds the next position while the transaction owns the writes.
        if (user.seq != sequence - 1L) return false // Prevents a reply from being added after an unrelated later turn.
        val changed = updateStatusRow(user.id, MessageStatus.SENDING, MessageStatus.SENT, null) // Marks the user row completed locally.
        if (changed != 1) return false // Requires exactly one pending row before adding the reply.
        val reply = MessageEntity( // Creates the model row that belongs to the completed exchange.
            id = replyId, // Keeps the app-generated reply ID.
            chatId = user.chatId, // Uses the same parent chat as the user message.
            role = Role.MODEL, // Identifies the reply as model content.
            text = replyText, // Saves the response text.
            seq = sequence, // Places it directly after the user message.
            status = MessageStatus.SENT, // A stored usable reply belongs to a completed exchange.
            createdAt = createdAt, // Saves the supplied response timestamp.
        ) // Ends the model-row constructor.
        insertRow(reply) // A constraint failure here rolls back the earlier SENT update too.
        touchChat(user.chatId, createdAt) // Updates chat recency as part of the same commit.
        return true // Reports a fully saved exchange.
    } // Ends the exchange transaction.

    @Insert // Keeps raw inserts inside the DAO transaction methods.
    protected abstract suspend fun insertRow(message: MessageEntity) // Inserts the row without replacing existing history.

    @Query("SELECT COALESCE(MAX(seq), -1) + 1 FROM messages WHERE chatId = :chatId") // Starts an empty chat at zero.
    protected abstract suspend fun nextSeq(chatId: String): Long // Reads the next position only inside a write transaction.

    @Query( // Changes the state only if the row still has the expected state.
        "UPDATE messages SET status = :status, errorCategory = :errorCategory " + // Writes the new state and optional error.
            "WHERE id = :messageId AND role = 'USER' AND status = :expectedStatus" // Limits the change to this user row.
    ) // Ends the guarded state query.
    protected abstract suspend fun updateStatusRow( // Generates the internal state update.
        messageId: String, // Identifies the row to update.
        expectedStatus: MessageStatus, // Confirms the state that was read in the transaction.
        status: MessageStatus, // Supplies the new state.
        errorCategory: MessageErrorCategory?, // Supplies the new error value, including null.
    ): Int // Returns the number of matching rows.

    @Query("UPDATE chats SET updatedAt = MAX(updatedAt, :updatedAt) WHERE id = :chatId") // Keeps timestamps from moving backwards.
    protected abstract suspend fun touchChat(chatId: String, updatedAt: Long): Int // Updates the owning chat's recency.
} // Ends the message DAO.
