package pt.isel.pdm.droidmentor.data.repository // Defines repository operations independently of the Room implementation.

import kotlinx.coroutines.flow.Flow // Observes locally persisted snapshots as they change.
import pt.isel.pdm.droidmentor.domain.Chat // Exposes domain conversation metadata.
import pt.isel.pdm.droidmentor.domain.ChatSummary // Exposes domain history-list previews.
import pt.isel.pdm.droidmentor.domain.Message // Exposes saved domain messages and their error labels.

/** Owns chat commands while keeping Room as the only source of observed history and network outcomes. */
interface ChatRepository { // Can be supplied manually to ViewModels and replaced by a simple fake in their tests.
    /** Observes saved chat summaries in the DAO's recency order. */
    fun observeChats(): Flow<ChatReadResult<List<ChatSummary>>> // Exposes no Room entity or direct API result.

    /** Observes one saved conversation, including null when it has been deleted. */
    fun observeChat(chatId: String): Flow<ChatReadResult<Chat?>> // Lets the active screen react to title changes and deletion.

    /** Observes all saved messages in sequence order, including persisted network failure labels. */
    fun observeMessages(chatId: String): Flow<ChatReadResult<List<Message>>> // Drives chat content entirely from Room.

    /** Creates an empty local conversation, including while offline or without a configured key. */
    suspend fun createChat(title: String): ChatCommandResult // Returns the new local ID only after persistence.

    /** Renames an existing local conversation without changing its messages. */
    suspend fun renameChat(chatId: String, title: String): ChatCommandResult // Performs a local change independent of network access.

    /** Deletes a conversation and its cascaded history when no send currently owns it. */
    suspend fun deleteChat(chatId: String): ChatCommandResult // Refuses to delete this repository's in-flight exchange.

    /** Saves one pending user turn, sends the full history and persists the settled outcome. */
    suspend fun sendMessage(chatId: String, text: String): ChatCommandResult // Returns only a local acknowledgement; observeMessages carries the outcome.

    /** Reactivates the same final failed user row and resends the full history without appending a duplicate. */
    suspend fun retryMessage(chatId: String, messageId: String): ChatCommandResult // Preserves the failed row's ID, sequence, text and creation timestamp.
} // Ends the chat repository contract.
