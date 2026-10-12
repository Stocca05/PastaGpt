package pt.isel.pdm.droidmentor.data.local.relation // Groups the results that combine related Room rows.

import androidx.room.Embedded // Includes the fields of a chat in this query result.
import androidx.room.Relation // Loads the message linked by the query's last-message ID.
import pt.isel.pdm.droidmentor.data.local.entity.ChatEntity // Supplies the chat row.
import pt.isel.pdm.droidmentor.data.local.entity.MessageEntity // Supplies the optional last message row.

/**
 * A chat and its latest message for the conversation list.
 *
 * @property chat The stored chat fields.
 * @property lastMessageId ID selected by the DAO query; this is not a new column in the chats table.
 * @property lastMessage The highest-sequence message, or null for an empty chat.
 */
data class ChatWithLastMessage( // Groups the parent chat and the message used for its preview.
    @Embedded // Reads the chat fields from the query's chat columns.
    val chat: ChatEntity, // Holds the parent chat row.
    val lastMessageId: String?, // Holds the extra ID calculated by the SQL query.
    @Relation(parentColumn = "lastMessageId", entityColumn = "id") // Loads only the selected message by its ID.
    val lastMessage: MessageEntity?, // Keeps an empty chat's preview as null.
) // Ends the combined query result.
