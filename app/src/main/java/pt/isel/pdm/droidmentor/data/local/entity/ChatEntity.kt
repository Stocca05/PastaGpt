package pt.isel.pdm.droidmentor.data.local.entity // Groups the classes stored as Room rows.

import androidx.room.Entity // Marks a class as a database table.
import androidx.room.PrimaryKey // Marks the column that identifies each row.

/**
 * One chat row in the local database.
 *
 * @property id Chat ID created by the app.
 * @property title Title shown in the chat list.
 * @property createdAt Creation time in milliseconds since the Unix epoch.
 * @property updatedAt Last update time, using the same unit.
 */
@Entity(tableName = "chats") // Stores these rows in the chats table.
data class ChatEntity( // Defines the columns of one chat row.
    @PrimaryKey // Prevents two chats from sharing the same ID.
    val id: String, // Uses the app's local ID as the row key.
    val title: String, // Stores the title of the conversation.
    val createdAt: Long, // Stores when the chat was created.
    val updatedAt: Long, // Stores when the chat last changed.
) // Ends the list of chat columns.
