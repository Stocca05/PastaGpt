package pt.isel.pdm.droidmentor.domain // Keeps chat-list values independent of Room and Android.

/**
 * A saved chat and its optional last message for the history list.
 *
 * @property chat The local conversation and its title/timestamps.
 * @property lastMessage Highest-sequence saved message, or null for an empty conversation.
 */
data class ChatSummary( // Exposes domain values rather than Room's query relation.
    val chat: Chat, // Holds the persisted conversation metadata.
    val lastMessage: Message?, // Holds only the latest saved message for the preview.
) // Ends the chat-list domain model.
