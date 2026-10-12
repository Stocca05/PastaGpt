package pt.isel.pdm.droidmentor.data.repository // Coordinates local history, preferences and the Gemini data source.

import androidx.room.withTransaction // Keeps preparation checks and optimistic writes in one Room transaction.
import java.util.UUID // Generates only local conversation and message IDs.
import java.util.concurrent.ConcurrentHashMap // Shares one safe in-memory operation lock for each chat ID.
import kotlinx.coroutines.CancellationException // Preserves cancellation after settling any owned pending row.
import kotlinx.coroutines.NonCancellable // Allows required local cleanup after a send's caller is cancelled.
import kotlinx.coroutines.currentCoroutineContext // Checks cancellation before local command work.
import kotlinx.coroutines.ensureActive // Stops an already cancelled command before side effects.
import kotlinx.coroutines.flow.Flow // Exposes Room-backed observation to the UI.
import kotlinx.coroutines.flow.first // Reads the latest preference snapshot before each prerequisite phase.
import kotlinx.coroutines.flow.catch // Handles only upstream Room failures without intercepting collector errors.
import kotlinx.coroutines.flow.map // Converts each stored snapshot to domain values and safe read outcomes.
import kotlinx.coroutines.sync.Mutex // Prevents concurrent operations in the same chat without blocking different chats.
import kotlinx.coroutines.withContext // Runs cancellation cleanup in a context that can finish its Room write.
import pt.isel.pdm.droidmentor.data.local.DroidMentorDatabase // Supplies the real transaction owner and DAOs.
import pt.isel.pdm.droidmentor.data.local.entity.ChatEntity // Builds and maps persisted conversation rows.
import pt.isel.pdm.droidmentor.data.local.entity.MessageEntity // Builds and maps persisted user/model rows.
import pt.isel.pdm.droidmentor.data.remote.ApiResult // Handles network outcomes privately inside the data layer.
import pt.isel.pdm.droidmentor.data.remote.GeminiRemoteDataSource // Sends validated complete histories through Ktor.
import pt.isel.pdm.droidmentor.data.remote.toMessageErrorCategory // Reduces API failures to the safe enum saved in Room.
import pt.isel.pdm.droidmentor.domain.Chat // Returns persisted domain metadata instead of entities.
import pt.isel.pdm.droidmentor.domain.ChatSummary // Returns local chat-list previews.
import pt.isel.pdm.droidmentor.domain.HistoryPayloadBuilder // Validates saved exchanges before any API call.
import pt.isel.pdm.droidmentor.domain.HistoryPayloadResult // Distinguishes prepared histories from local validation rejection.
import pt.isel.pdm.droidmentor.domain.Message // Returns saved domain messages to observers.
import pt.isel.pdm.droidmentor.domain.MessageErrorCategory // Stores only safe failure labels in user rows.
import pt.isel.pdm.droidmentor.domain.MessageStatus // Coordinates pending, completed and failed exchanges.
import pt.isel.pdm.droidmentor.domain.Role // Distinguishes saved user questions and model replies.

/**
 * App-owned offline-first chat repository; all observed history and API outcomes come from Room.
 *
 * @param database Database owning the preparation and completion transactions.
 * @param settings Preferences repository used to reread the key for each attempt.
 * @param remote Shared stateless Gemini source with its own final connection gate.
 * @param isConnected Fresh suspending connectivity check, normally ConnectivityObserver::isConnected.
 * @param clock Current time in milliseconds; injectable for deterministic persistence tests.
 * @param newId New local ID; injectable for transaction rollback tests.
 */
class RoomChatRepository( // Receives every dependency manually without a framework or background application scope.
    private val database: DroidMentorDatabase, // Keeps transactions short and never holds one across HTTP.
    private val settings: SettingsRepository, // Reads the credential without storing it in the repository.
    private val remote: GeminiRemoteDataSource, // Supplies only typed data-layer outcomes.
    private val isConnected: suspend () -> Boolean, // Rechecks validated connectivity before preparing and sending.
    private val clock: () -> Long = System::currentTimeMillis, // Supplies local timestamps without network metadata.
    private val newId: () -> String = { UUID.randomUUID().toString() }, // Creates local IDs unrelated to server response IDs.
) : ChatRepository { // Implements the Room-only observation contract.
    private val chats = database.chatDao() // Reuses the production chat DAO.
    private val messages = database.messageDao() // Reuses the production transactional message DAO.
    private val locks = ConcurrentHashMap<String, Mutex>() // Keeps per-chat operation locks for this app-owned repository's lifetime.

    /** Observes only persisted chat summaries and typed local read failures. */
    override fun observeChats(): Flow<ChatReadResult<List<ChatSummary>>> { // Keeps the local DAO as the source of every emitted value.
        return readFlow(chats.observeChats().map { rows -> rows.map { ChatSummary(chat(it.chat), it.lastMessage?.let { row -> message(row) }) } }) // Maps every saved summary before handling upstream storage failures.
    } // Ends the chat summaries stream.

    /** Observes one saved chat through the same Room query and returns null after deletion. */
    override fun observeChat(chatId: String): Flow<ChatReadResult<Chat?>> { // Reacts to persisted title, recency and deletion changes.
        return readFlow(chats.observeChats().map { rows -> rows.firstOrNull { it.chat.id == chatId }?.chat?.let { chat(it) } }) // Selects only the requested saved conversation.
    } // Ends the single-chat stream.

    /** Observes every saved message and its persisted error label in sequence order. */
    override fun observeMessages(chatId: String): Flow<ChatReadResult<List<Message>>> { // Never observes an API result directly.
        return readFlow(messages.observeMessages(chatId).map { rows -> rows.map { message(it) } }) // Converts every stored row without dropping errors or text.
    } // Ends the Room-backed message stream.

    /** Creates local history without requiring connectivity or configured preferences. */
    override suspend fun createChat(title: String): ChatCommandResult { // Supports offline local chat creation.
        currentCoroutineContext().ensureActive() // Stops a cancelled command before side effects.
        if (title.isBlank()) return ChatCommandResult.InvalidInput // Requires a usable display title.
        return try { // Converts ordinary insert failures to local acknowledgements.
            val id = newId() // Generates a local conversation identity.
            if (id.isBlank()) return ChatCommandResult.InvalidInput // Refuses an unusable injected identity.
            val timestamp = clock() // Uses the same initial creation and update time.
            chats.insert(ChatEntity(id, title.trim(), timestamp, timestamp)) // Persists the empty conversation through Room.
            ChatCommandResult.Created(id) // Returns navigation metadata only after insertion succeeded.
        } catch (error: CancellationException) { // Preserves command cancellation.
            throw error // Does not replace cancellation with a storage message.
        } catch (error: Exception) { // Contains constraint or local storage failure.
            ChatCommandResult.StorageError // Returns no raw exception or partial success.
        } // Ends safe local creation.
    } // Ends chat creation.

    /** Changes only the saved title when no operation currently owns this chat. */
    override suspend fun renameChat(chatId: String, title: String): ChatCommandResult { // Requires no network or key.
        currentCoroutineContext().ensureActive() // Preserves cancellation before acquiring the operation gate.
        if (chatId.isBlank() || title.isBlank()) return ChatCommandResult.InvalidInput // Rejects incomplete local input.
        val lock = lock(chatId) // Uses the same operation gate as sends and retries.
        if (!lock.tryLock()) return ChatCommandResult.Busy // Refuses to race an active exchange.
        return try { // Contains local title persistence failures.
            if (chats.updateTitle(chatId, title.trim(), clock()) == 1) ChatCommandResult.Stored else ChatCommandResult.NotFound // Reports only the actual affected row.
        } catch (error: CancellationException) { // Preserves caller cancellation.
            throw error // Leaves coroutine control flow intact.
        } catch (error: Exception) { // Contains ordinary database errors.
            ChatCommandResult.StorageError // Gives a local storage outcome without details.
        } finally { // Runs for success, failure and cancellation.
            lock.unlock() // Releases this chat without touching other chats' locks.
        } // Ends guarded renaming.
    } // Ends local title editing.

    /** Deletes one local conversation and its cascaded history while protecting active exchanges. */
    override suspend fun deleteChat(chatId: String): ChatCommandResult { // Requires no key or connectivity.
        currentCoroutineContext().ensureActive() // Stops cancelled local work before deletion.
        if (chatId.isBlank()) return ChatCommandResult.InvalidInput // Requires a local target identity.
        val lock = lock(chatId) // Shares the active exchange's operation gate.
        if (!lock.tryLock()) return ChatCommandResult.Busy // Refuses deletion while this repository is sending.
        return try { // Contains ordinary deletion failures.
            if (chats.deleteById(chatId) == 1) ChatCommandResult.Stored else ChatCommandResult.NotFound // Lets Room's foreign key cascade remove messages.
        } catch (error: CancellationException) { // Preserves normal cancellation.
            throw error // Does not turn cancelled work into a local error.
        } catch (error: Exception) { // Contains database exceptions.
            ChatCommandResult.StorageError // Returns a safe local storage acknowledgement.
        } finally { // Runs even when the query fails.
            lock.unlock() // Releases only this chat's operation gate.
        } // Ends protected deletion.
    } // Ends local chat removal.

    /** Saves the pending user, sends complete history and persists its success or error before returning. */
    override suspend fun sendMessage(chatId: String, text: String): ChatCommandResult { // Supplies new text to the shared send/retry lifecycle.
        return attempt(chatId, text, null) // Never returns a network answer to the caller.
    } // Ends new-message sending.

    /** Retries the existing final failed user with the same identity, position, text and creation time. */
    override suspend fun retryMessage(chatId: String, messageId: String): ChatCommandResult { // Supplies an existing row rather than appending a new one.
        return attempt(chatId, null, messageId) // Uses the same prerequisites and persistence lifecycle as a new send.
    } // Ends manual retry.

    private suspend fun attempt(chatId: String, text: String?, retryId: String?): ChatCommandResult { // Centralises the shared lifecycle without starting a background coroutine.
        currentCoroutineContext().ensureActive() // Preserves cancellation before validation or locking.
        if (chatId.isBlank() || (retryId == null && text.isNullOrBlank()) || retryId?.isBlank() == true) return ChatCommandResult.InvalidInput // Requires usable input for either a new send or a selected retry.
        val lock = lock(chatId) // Gives only this conversation an active operation owner.
        if (!lock.tryLock()) return ChatCommandResult.Busy // Rejects a second request instead of queueing another user turn.
        var pendingId: String? = null // Tracks only the row owned by this attempt for cancellation or storage-failure cleanup.
        try { // Keeps prerequisites, optimistic preparation, network and persistence under the same chat gate.
            if (chats.getById(chatId) == null) return ChatCommandResult.NotFound // Stops before reading a key for a missing conversation.
            val initial = readSettings() // Reads the current DataStore-backed preference result without caching it in the repository.
            if (initial is SettingsReadResult.StorageError) return ChatCommandResult.SettingsError // Leaves history unchanged if preferences cannot be read.
            val initialKey = (initial as SettingsReadResult.Success).apiKey?.trim() // Keeps the first credential snapshot only in attempt memory.
            if (initialKey.isNullOrBlank()) return ChatCommandResult.MissingApiKey // Blocks before inserting a pending row when no key is configured.
            if (text?.contains(initialKey) == true) return ChatCommandResult.InvalidInput // Prevents the configured credential from being saved as incoming user text.
            if (!isConnected()) return ChatCommandResult.Offline // Leaves the draft and local history unchanged when initially offline.

            val rejected = database.withTransaction { // Makes unresolved-row checks and optimistic insertion/reactivation indivisible.
                if (chats.getById(chatId) == null) return@withTransaction ChatCommandResult.NotFound // Rechecks existence inside the preparation transaction.
                val saved = messages.getMessages(chatId) // Reads every prior row in sequence order while preparation owns the transaction.
                if (saved.any { it.text.contains(initialKey) }) return@withTransaction ChatCommandResult.InvalidHistory // Refuses to transmit configured credential text from legacy history.
                val candidate: MessageEntity // Describes the one pending user that this attempt will own.
                if (retryId == null) { // Prepares a new user row only when no unresolved turn exists.
                    if (saved.any { it.status != MessageStatus.SENT }) return@withTransaction ChatCommandResult.UnresolvedMessage // Blocks both SENDING and FAILED prior rows.
                    if (saved.lastOrNull()?.seq == Long.MAX_VALUE) return@withTransaction ChatCommandResult.InvalidHistory // Rejects sequence overflow before the DAO allocates the next position.
                    val sequence = saved.lastOrNull()?.seq?.plus(1) ?: 0L // Predicts only the position needed to validate prospective full history.
                    candidate = MessageEntity(newId(), chatId, Role.USER, text.orEmpty(), sequence, MessageStatus.SENDING, clock()) // Keeps the supplied message text unchanged.
                } else { // Prepares only the selected final failed user row for retry.
                    val existing = messages.getById(retryId) ?: return@withTransaction ChatCommandResult.NotFound // Requires an existing local retry target.
                    if (existing.chatId != chatId || existing.role != Role.USER || existing.status != MessageStatus.FAILED || saved.lastOrNull()?.id != retryId) return@withTransaction ChatCommandResult.InvalidRetry // Refuses foreign, model, completed or non-final rows.
                    candidate = existing.copy(status = MessageStatus.SENDING, errorCategory = null) // Preserves ID, sequence, text and timestamp while clearing the old failure.
                } // Ends new-versus-retry row selection.
                val prospective = if (retryId == null) saved + candidate else saved.map { if (it.id == retryId) candidate else it } // Includes every prior row and exactly one current user.
                if (HistoryPayloadBuilder.build(chatId, prospective.map { message(it) }) is HistoryPayloadResult.Invalid) return@withTransaction ChatCommandResult.InvalidHistory // Rejects invalid prior states or alternation before changing storage.
                pendingId = candidate.id // Records ownership before the suspend write so cancellation after commit can still settle the row.
                if (retryId == null) { // Performs the optimistic new-user insertion.
                    messages.insert(candidate) // Lets the DAO allocate the sequence and update recency atomically.
                } else if (!messages.updateStatus(candidate.id, MessageStatus.SENDING)) { // Reactivates the same failed row in this transaction.
                    return@withTransaction ChatCommandResult.StorageError // Reports a failed guarded state change without sending.
                } // Ends optimistic row persistence.
                null // Signals preparation succeeded without returning a network result or row to the UI.
            } // Commits preparation before any HTTP operation begins.
            if (rejected != null) return rejected // Returns only a typed local reason when preparation was blocked.
            val userId = pendingId ?: return ChatCommandResult.StorageError // Requires the saved row identity before proceeding.

            val current = readSettings() // Rereads the key after optimistic insertion instead of trusting the initial snapshot.
            if (current is SettingsReadResult.StorageError) return saveFailure(userId, MessageErrorCategory.UNKNOWN) // Persists a safe failure if preferences became unreadable.
            val key = (current as SettingsReadResult.Success).apiKey?.trim() // Holds only this call's current credential in memory.
            if (key.isNullOrBlank()) return saveFailure(userId, MessageErrorCategory.INVALID_API_KEY) // Persists a removed-key failure without starting HTTP.
            val history = HistoryPayloadBuilder.build(chatId, messages.getMessages(chatId).map { message(it) }) // Rebuilds the full actual saved history after preparation.
            if (history !is HistoryPayloadResult.Success || history.turns.any { it.text.contains(key) }) return saveFailure(userId, MessageErrorCategory.BAD_REQUEST) // Refuses invalid or sensitive payload text before the source call.
            if (!isConnected()) return saveFailure(userId, MessageErrorCategory.NETWORK_UNAVAILABLE) // Rechecks connectivity immediately before invoking the source's own final gate.
            val outcome = remote.generateContent(history.turns, apiKey = key) // Uses the central persona and complete stateless request inside the typed source.
            val result = when (outcome) { // Consumes remote values privately and exposes their settled states only through Room.
                is ApiResult.Success -> { // Persists only a usable, non-sensitive answer.
                    if (outcome.value.contains(key)) { // Handles an improperly echoed credential even in a successful server response.
                        saveFailure(userId, MessageErrorCategory.BLOCKED_RESPONSE) // Saves a safe failure instead of sensitive model text.
                    } else if (messages.completeExchange(userId, newId(), outcome.value, clock())) { // Commits the model reply and user SENT state together.
                        ChatCommandResult.Stored // Acknowledges only completed local persistence.
                    } else { // Handles a deleted or externally changed pending row.
                        if (chats.getById(chatId) == null) ChatCommandResult.NotFound else ChatCommandResult.StorageError // Does not claim completion that Room rejected.
                    } // Ends successful reply persistence.
                } // Ends handling of decoded model text.
                else -> saveFailure(userId, outcome.toMessageErrorCategory() ?: MessageErrorCategory.UNKNOWN) // Saves only the failure label, never HTTP bodies or exception objects.
            } // Ends settled outcome persistence.
            if (result == ChatCommandResult.Stored) pendingId = null // Prevents cleanup from touching an already settled exchange.
            return result // Returns only a local acknowledgement after the persistence decision.
        } catch (error: CancellationException) { // Keeps user or lifecycle cancellation as coroutine cancellation.
            throw error // The finally block still settles any owned SENDING row before propagating cancellation.
        } catch (error: Exception) { // Contains ordinary local storage or dependency failures without revealing their details.
            return ChatCommandResult.StorageError // Never exposes a raw exception or network answer to the caller.
        } finally { // Runs after a normal return, failed write or cancellation.
            withContext(NonCancellable) { // Allows cleanup to finish even when the send's caller is cancelled.
                try { // Attempts only local cleanup, never another API request.
                    val row = pendingId?.let { messages.getById(it) } // Looks up only the identity owned by this attempt.
                    if (row?.chatId == chatId && row.role == Role.USER && row.status == MessageStatus.SENDING) { // Leaves completed, deleted and foreign rows untouched.
                        messages.updateStatus(row.id, MessageStatus.FAILED, MessageErrorCategory.UNKNOWN) // Makes an interrupted attempt retryable without saving cancellation or exception details.
                    } // Ends owned pending-row cleanup.
                } catch (error: Exception) { // Handles unavailable local storage without replacing the original cancellation or storage result.
                    Unit // Leaves the explicit local failure acknowledgement intact when persistence is unavailable.
                } // Ends best-effort local cleanup.
            } // Ends the non-cancellable cleanup section.
            lock.unlock() // Releases the per-chat operation gate for a later manual retry.
        } // Ends the complete attempt lifecycle.
    } // Ends optimistic send and manual retry coordination.

    private fun <T> readFlow(source: Flow<T>): Flow<ChatReadResult<T>> { // Applies the same safe read wrapper to each already mapped Room query.
        return source.map<T, ChatReadResult<T>> { ChatReadResult.Success(it) }.catch { error -> // Catches query and mapping failures upstream, while preserving collector exception transparency.
            if (error is CancellationException) throw error // Keeps stopping observation as normal coroutine cancellation.
            emit(ChatReadResult.StorageError) // Reports unreadable storage without pretending it is empty or hiding a collector's own bug.
        } // Ends the upstream-only read failure handler.
    } // Ends the common Room read outcome wrapper.

    private fun lock(chatId: String) = locks.computeIfAbsent(chatId) { Mutex() } // Reuses one lock identity for every operation in this conversation.

    private suspend fun readSettings(): SettingsReadResult { // Distinguishes preferences failure from an absent key without exposing exceptions.
        return try { // Reads a fresh DataStore-backed snapshot for this prerequisite phase.
            settings.readResult.first() // Does not keep a credential field or use the nullable stream's ambiguous storage-error fallback.
        } catch (error: CancellationException) { // Preserves cancellation during the settings read.
            throw error // Lets the attempt's cleanup handle any already saved pending row.
        } catch (error: Exception) { // Contains ordinary preferences failures, including a stream ending before its first value.
            SettingsReadResult.StorageError // Returns only a safe storage classification.
        } // Ends controlled preferences reading.
    } // Ends the current key snapshot helper.

    private suspend fun saveFailure(messageId: String, category: MessageErrorCategory): ChatCommandResult { // Persists a category before acknowledging a settled attempt.
        return if (messages.updateStatus(messageId, MessageStatus.FAILED, category)) ChatCommandResult.Stored else ChatCommandResult.StorageError // Reports only whether the local failure state was saved.
    } // Ends typed failure persistence.

    private fun chat(row: ChatEntity) = Chat(row.id, row.title, row.createdAt, row.updatedAt) // Maps only saved conversation metadata to the domain.

    private fun message(row: MessageEntity) = Message(row.id, row.chatId, row.role, row.text, row.seq, row.status, row.createdAt, row.errorCategory) // Preserves every saved message field, including the failure label.
} // Ends the Room-only observed chat repository.
