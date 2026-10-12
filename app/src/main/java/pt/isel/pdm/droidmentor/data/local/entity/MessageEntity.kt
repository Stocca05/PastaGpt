package pt.isel.pdm.droidmentor.data.local.entity // Groups the classes stored as Room rows.

import androidx.room.Entity // Marks this class as a database table.
import androidx.room.ForeignKey // Connects a message row to its parent chat.
import androidx.room.Index // Defines an index used for lookup or uniqueness.
import androidx.room.PrimaryKey // Marks the ID column of a row.
import androidx.room.TypeConverters // Tells Room which enum converter methods to use.
import pt.isel.pdm.droidmentor.data.local.converter.MessageConverters // Converts our enum values to database text.
import pt.isel.pdm.droidmentor.domain.MessageErrorCategory // Represents the saved failure type.
import pt.isel.pdm.droidmentor.domain.MessageStatus // Represents the state of the message exchange.
import pt.isel.pdm.droidmentor.domain.Role // Represents the author of the message.

/**
 * One text message row belonging to a local chat.
 *
 * @property id Message ID kept when retrying an unsuccessful send.
 * @property chatId ID of the parent chat.
 * @property role Whether the user or the mentor wrote the message.
 * @property text The message text.
 * @property seq Message order inside this chat.
 * @property status Whether the exchange is waiting, completed or failed.
 * @property createdAt Creation time in milliseconds since the Unix epoch.
 * @property errorCategory Saved failure type, or null when no error is recorded.
 */
@Entity( // Defines the table and its constraints.
    tableName = "messages", // Stores these rows in the messages table.
    foreignKeys = [ // Lists the parent-row relationships.
        ForeignKey( // Connects every message to an existing chat.
            entity = ChatEntity::class, // Uses the chats entity as the parent table.
            parentColumns = ["id"], // References the ID column of the parent chat.
            childColumns = ["chatId"], // Uses chatId to find the message's parent.
            onDelete = ForeignKey.CASCADE, // Deleting a chat also deletes its message rows.
        ), // Ends this foreign-key definition.
    ], // Ends the list of parent relationships.
    indices = [ // Lists the indexes on the messages table.
        Index(value = ["chatId"]), // Makes message lookup by chat faster.
        Index(value = ["chatId", "seq"], unique = true), // Prevents duplicate positions within one chat.
    ], // Ends the list of indexes.
) // Ends the table definition.
@TypeConverters(MessageConverters::class) // Applies the enum converters to this entity's columns.
data class MessageEntity( // Defines the columns of one message row.
    @PrimaryKey // Prevents two messages from sharing the same ID.
    val id: String, // Stores the local message ID.
    val chatId: String, // Stores the local parent chat ID.
    val role: Role, // Stores the author through the role converter.
    val text: String, // Stores the message's text content.
    val seq: Long, // Stores the message position as a 64-bit integer.
    val status: MessageStatus, // Stores the exchange state through its converter.
    val createdAt: Long, // Stores when this message was created.
    val errorCategory: MessageErrorCategory? = null, // Allows a null error for waiting or completed messages.
) // Ends the list of message columns.
