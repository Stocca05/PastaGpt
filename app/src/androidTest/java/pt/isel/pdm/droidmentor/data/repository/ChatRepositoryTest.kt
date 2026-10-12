package pt.isel.pdm.droidmentor.data.repository // Verifies the repository against real Room and simulated Ktor requests.

import androidx.room.Room // Builds the production database schema in memory.
import androidx.datastore.preferences.core.PreferenceDataStoreFactory // Creates a dedicated test-only Preferences DataStore for real settings integration.
import androidx.test.ext.junit.runners.AndroidJUnit4 // Runs real SQLite integration tests on Android.
import androidx.test.platform.app.InstrumentationRegistry // Provides the target application context.
import io.ktor.client.HttpClient // Holds test-owned clients for cleanup.
import io.ktor.client.engine.config // Creates engine factories owned by each configured client.
import io.ktor.client.engine.mock.MockEngine // Keeps every API request inside the test process.
import io.ktor.client.engine.mock.respond // Supplies successful or failed simulated responses.
import io.ktor.client.network.sockets.SocketTimeoutException // Simulates a transport timeout for persisted error verification.
import io.ktor.client.plugins.logging.Logger // Supplies a silent final log destination.
import io.ktor.client.request.HttpRequestData // Inspects full request history and credential headers in memory.
import io.ktor.http.ContentType // Marks simulated responses as JSON.
import io.ktor.http.HttpHeaders // Names the response content type header.
import io.ktor.http.HttpStatusCode // Supplies simulated HTTP statuses.
import io.ktor.http.content.TextContent // Reads the actual JSON serialized by the data source.
import io.ktor.http.headersOf // Builds JSON response headers.
import java.io.IOException // Simulates preferences or network IO failures.
import java.io.File // Identifies only the isolated temporary preference file owned by one test.
import java.util.UUID // Generates fictitious credentials and local test IDs only at runtime.
import java.util.concurrent.atomic.AtomicInteger // Counts operations safely across coroutine threads.
import java.util.concurrent.atomic.AtomicLong // Supplies deterministic timestamps across concurrent chats.
import kotlinx.coroutines.CompletableDeferred // Coordinates request barriers without arbitrary sleeps.
import kotlinx.coroutines.CoroutineScope // Gives the isolated DataStore an explicitly owned coroutine scope.
import kotlinx.coroutines.Dispatchers // Runs the preference file operations on IO.
import kotlinx.coroutines.SupervisorJob // Owns only the isolated test preference store's lifetime.
import kotlinx.coroutines.async // Sends or observes data in child coroutines.
import kotlinx.coroutines.awaitCancellation // Keeps a simulated operation pending until the caller cancels it.
import kotlinx.coroutines.cancelAndJoin // Waits for cancellation cleanup to persist the message failure.
import kotlinx.coroutines.channels.Channel // Collects Room snapshots while the HTTP operation is paused.
import kotlinx.coroutines.flow.Flow // Provides the fake settings read contract.
import kotlinx.coroutines.flow.first // Reads one persisted snapshot or preference value.
import kotlinx.coroutines.flow.flow // Supplies deterministic settings reads and injected failures.
import kotlinx.coroutines.launch // Observes Room in a child coroutine.
import kotlinx.coroutines.runBlocking // Bridges JUnit to suspend methods only in test code.
import kotlinx.coroutines.withTimeout // Bounds every asynchronous test wait.
import kotlinx.serialization.json.Json // Inspects the outgoing JSON independently of DTO decoding.
import kotlinx.serialization.json.jsonArray // Reads every conversation turn sent to the engine.
import kotlinx.serialization.json.jsonObject // Reads the stateless request fields.
import kotlinx.serialization.json.jsonPrimitive // Extracts harmless test turn text.
import org.junit.After // Closes the test's clients and database even after a failure.
import org.junit.Assert.assertEquals // Compares safe fixtures, states and result categories.
import org.junit.Assert.assertFalse // Checks a failed exchange did not create a model row.
import org.junit.Assert.assertNull // Checks retry clears the saved failure label.
import org.junit.Assert.assertTrue // Checks credential privacy without displaying generated values.
import org.junit.Before // Creates isolated production-schema storage for every case.
import org.junit.Test // Marks repository verification cases.
import org.junit.runner.RunWith // Selects the Android test runner.
import pt.isel.pdm.droidmentor.data.local.DroidMentorDatabase // Uses the real application database.
import pt.isel.pdm.droidmentor.data.local.entity.MessageEntity // Seeds deliberately invalid persisted histories for rejection tests.
import pt.isel.pdm.droidmentor.data.remote.GeminiRemoteDataSource // Exercises the actual request builder and typed error boundary.
import pt.isel.pdm.droidmentor.data.remote.HttpClientFactory // Uses the actual client plugins around MockEngine.
import pt.isel.pdm.droidmentor.domain.Message // Reads only domain messages from repository flows.
import pt.isel.pdm.droidmentor.domain.MessageErrorCategory // Checks failure labels persisted in Room.
import pt.isel.pdm.droidmentor.domain.MessageStatus // Checks pending, completed and failed states.
import pt.isel.pdm.droidmentor.domain.Role // Checks user and model row ordering.

/** Checks offline-first commands, one request per chat, same-row retry and persisted network outcomes. */
@RunWith(AndroidJUnit4::class) // Executes production Room transactions on the connected device.
class ChatRepositoryTest { // Uses no real API key and no external HTTP endpoint.
    private lateinit var database: DroidMentorDatabase // Holds only this test's in-memory history.
    private lateinit var settings: TestSettings // Supplies controlled key reads without a credential file.
    private val clients = mutableListOf<HttpClient>() // Tracks client ownership for cleanup.
    private val requests = AtomicInteger() // Counts only requests that reach MockEngine.
    private val checks = AtomicInteger() // Counts explicit connectivity checks.
    private val clock = AtomicLong(1_000) // Supplies deterministic saved timestamps.
    private var online = true // Controls the injected connection prerequisite.
    private var status = 200 // Controls the next simulated HTTP response status.
    private var body = """{"candidates":[{"content":{"parts":[{"text":"Reply"}]},"finishReason":"STOP"}]}""" // Supplies a harmless usable model reply by default.
    private var transportFailure: Exception? = null // Injects transport failure only in selected tests.
    private var onRequest: suspend (HttpRequestData) -> Unit = {} // Allows deterministic request barriers or inspections.
    private var onCheck: suspend (Int) -> Unit = {} // Allows connectivity loss between the initial and final checks.

    /** Builds isolated real storage and a fake preference reader before each case. */
    @Before // Prevents one test's messages or settings from affecting another.
    fun prepare() { // Creates no production database file.
        val context = InstrumentationRegistry.getInstrumentation().targetContext // Uses the installed test application's context.
        database = Room.inMemoryDatabaseBuilder(context, DroidMentorDatabase::class.java).build() // Keeps the production schema and transaction behaviour.
        settings = TestSettings() // Creates a fresh runtime-generated fictitious key.
    } // Ends setup.

    /** Releases clients and discards the test-only database. */
    @After // Runs even when an asynchronous assertion fails.
    fun close() { // Leaves the app's real history and key preferences untouched.
        clients.forEach { it.close() } // Closes every engine owned by this test.
        database.close() // Discards only this case's in-memory rows.
    } // Ends cleanup.

    /** Checks create, observe, rename and cascade delete work offline without a key. */
    @Test // Verifies local chat management independently of send prerequisites.
    fun localChatCommandsRemainAvailableOffline() = runBlocking { // Calls coroutine-backed Room operations from the test worker.
        online = false // Makes API sending unavailable.
        settings.key = null // Leaves key preferences unconfigured.
        val repository = repository() // Creates the production repository with controlled dependencies.
        val id = createChat(repository) // Persists an empty local conversation.
        val initial = repository.observeChats().first() as ChatReadResult.Success // Reads the Room-backed domain summary list.
        assertEquals(id, initial.value.single().chat.id) // Checks the created row is observable.
        assertNull(initial.value.single().lastMessage) // Checks an empty chat has no fabricated preview.
        assertEquals(ChatCommandResult.Stored, repository.renameChat(id, "Renamed")) // Changes the saved title without networking.
        assertEquals("Renamed", (repository.observeChat(id).first() as ChatReadResult.Success).value?.title) // Checks the title comes back through Room observation.
        database.messageDao().insert(row("saved-user", id, Role.USER, MessageStatus.SENT)) // Adds a saved child row for cascade verification.
        assertEquals(ChatCommandResult.Stored, repository.deleteChat(id)) // Deletes the local chat and its child.
        assertNull((repository.observeChat(id).first() as ChatReadResult.Success).value) // Checks deletion is observed as a missing chat.
        assertTrue(messages(repository, id).isEmpty()) // Checks cascade removed the stored history.
        assertEquals(0, requests.get()) // Checks local commands never reach HTTP.
    } // Ends offline local command verification.

    /** Checks the pending row is visible before HTTP and success is observed only as a complete Room exchange. */
    @Test // Verifies optimistic insertion and atomic final state through repository flows.
    fun pendingAndCompletedExchangeAreObservedFromRoom() = runBlocking { // Coordinates the network response with the Room collector.
        val repository = repository() // Uses actual Room, source and result mapper.
        val id = createChat(repository) // Creates the parent conversation.
        val arrived = CompletableDeferred<Unit>() // Signals that the pending exchange reached the simulated engine.
        val release = CompletableDeferred<Unit>() // Holds the reply until the pending Room snapshot is observed.
        onRequest = { // Inspects local persistence while HTTP is still waiting.
            val saved = database.messageDao().getMessages(id) // Reads the pending row directly for the integration assertion.
            assertEquals(MessageStatus.SENDING, saved.single().status) // Checks insertion happened before the remote operation.
            arrived.complete(Unit) // Lets the test inspect the same pending state through the public flow.
            withTimeout(5_000) { release.await() } // Keeps the server response paused without sleeping.
        } // Ends the request barrier.
        val snapshots = Channel<List<Message>>(Channel.UNLIMITED) // Queues safe domain snapshots from Room.
        val observer = launch { repository.observeMessages(id).collect { if (it is ChatReadResult.Success) snapshots.send(it.value) } } // Observes only repository-backed stored values.
        val send = async { repository.sendMessage(id, "Question") } // Starts the optimistic send.
        try { // Ensures the barrier and observer are released even on failure.
            withTimeout(5_000) { arrived.await() } // Waits until HTTP confirms the pending insert exists.
            val pending = withTimeout(5_000) { var value = snapshots.receive(); while (value.size != 1) value = snapshots.receive(); value } // Waits for the pending Room snapshot.
            assertEquals(MessageStatus.SENDING, pending.single().status) // Checks what the future UI will actually observe.
            release.complete(Unit) // Allows the server to return its usable reply.
            assertEquals(ChatCommandResult.Stored, send.await()) // Receives a persistence acknowledgement, never model text.
            val completed = withTimeout(5_000) { var value = snapshots.receive(); while (value.size != 2) value = snapshots.receive(); value } // Waits for the final stored exchange.
            assertEquals(listOf(Role.USER, Role.MODEL), completed.map { it.role }) // Checks both saved participants are present.
            assertTrue(completed.all { it.status == MessageStatus.SENT && it.errorCategory == null }) // Checks success was committed without partial message states.
        } finally { // Prevents a failed test from leaving network or flow work pending.
            release.complete(Unit) // Unblocks the simulated response if an earlier assertion failed.
            send.cancelAndJoin() // Stops any still-running attempt and its cleanup.
            observer.cancelAndJoin() // Releases the Room flow collector.
            snapshots.close() // Releases queued observations.
        } // Ends the controlled send lifecycle.
    } // Ends optimistic and atomic observation verification.

    /** Checks later sends carry every completed exchange and the current user text unchanged. */
    @Test // Exercises the repository's complete stateless payload construction.
    fun subsequentSendsIncludeTheEntireStoredHistory() = runBlocking { // Sends three user questions into one conversation.
        val repository = repository() // Uses the production builder and actual source.
        val id = createChat(repository) // Creates one saved conversation.
        val payloads = mutableListOf<List<String>>() // Captures only harmless fixture text from serialized contents.
        onRequest = { request -> payloads.add(requestTexts(request)) } // Records all actual outgoing turns.
        for (text in listOf(" First café 🍝\n", "Second", "Third")) { // Includes whitespace and Unicode in the first message.
            assertEquals(ChatCommandResult.Stored, repository.sendMessage(id, text)) // Persists every settled exchange before the next send.
        } // Ends sequential sending.
        assertEquals(listOf(1, 3, 5), payloads.map { it.size }) // Checks 2N + 1 contents for every request.
        assertEquals(listOf(" First café 🍝\n", "Reply", "Second", "Reply", "Third"), payloads.last()) // Checks no stored exchange or character was removed.
        assertEquals((0L..5L).toList(), messages(repository, id).map { it.seq }) // Checks all six saved rows have deterministic order.
    } // Ends complete history integration verification.

    /** Checks every API failure becomes a persisted user error rather than a returned network result. */
    @Test // Covers HTTP, transport, JSON, empty, blocked and unknown response outcomes.
    fun networkFailuresAreSavedAsMessageCategories() = runBlocking { // Creates a separate conversation for each failed attempt.
        val repository = repository() // Shares one repository while preserving per-chat histories.
        val cases = listOf(400 to MessageErrorCategory.BAD_REQUEST, 403 to MessageErrorCategory.INVALID_API_KEY, 429 to MessageErrorCategory.RATE_LIMITED, 503 to MessageErrorCategory.SERVER_ERROR) // Defines required HTTP failure labels.
        for ((code, category) in cases) { // Exercises each HTTP family through the full source.
            status = code // Selects the next server failure.
            verifySavedFailure(repository, createChat(repository), category) // Requires a saved failure and only a local acknowledgement.
        } // Ends HTTP category verification.
        status = 200 // Restores successful HTTP status for response-level failures.
        for ((payload, category) in listOf("{" to MessageErrorCategory.MALFORMED_RESPONSE, "{}" to MessageErrorCategory.EMPTY_RESPONSE, """{"promptFeedback":{"blockReason":"SAFETY"}}""" to MessageErrorCategory.BLOCKED_RESPONSE, """{"candidates":[{"finishReason":"OTHER"}]}""" to MessageErrorCategory.UNKNOWN)) { // Lists independent response problems.
            body = payload // Selects the response body for this attempt.
            verifySavedFailure(repository, createChat(repository), category) // Checks the persisted label reaches the public Room flow.
        } // Ends response-level category verification.
        transportFailure = IOException("Connection unavailable") // Simulates a transport problem instead of response decoding.
        verifySavedFailure(repository, createChat(repository), MessageErrorCategory.NETWORK_UNAVAILABLE) // Checks IO is saved without exception details.
        transportFailure = SocketTimeoutException("Socket expired") // Simulates a specific network timeout.
        verifySavedFailure(repository, createChat(repository), MessageErrorCategory.TIMEOUT) // Checks timeout has its own persisted category.
    } // Ends persisted network outcome verification.

    /** Checks failed turns block new messages and retry reuses the exact final saved row. */
    @Test // Covers complete prior history, error clearing and no duplicated user message.
    fun retryReusesTheSameRowAndCompleteHistory() = runBlocking { // Creates a completed pair followed by a failed second user.
        val repository = repository() // Creates the repository under test.
        val id = createChat(repository) // Creates the parent chat.
        assertEquals(ChatCommandResult.Stored, repository.sendMessage(id, "First")) // Completes the initial exchange.
        status = 503 // Makes the next attempt fail.
        assertEquals(ChatCommandResult.Stored, repository.sendMessage(id, "Retry this")) // Saves failure in the same pending row.
        val failed = messages(repository, id).last() // Reads the failed user's original identity and timestamp.
        assertEquals(ChatCommandResult.UnresolvedMessage, repository.sendMessage(id, "Do not append")) // Checks a new user cannot bypass the failure.
        status = 200 // Allows the manual retry to receive a usable reply.
        onRequest = { request -> // Verifies the pending retry before the server responds.
            val pending = database.messageDao().getById(failed.id) // Reads the existing reactivated row.
            assertEquals(MessageStatus.SENDING, pending?.status) // Checks retry marks the existing row pending.
            assertNull(pending?.errorCategory) // Checks the previous failure is cleared before HTTP.
            assertEquals(listOf("First", "Reply", "Retry this"), requestTexts(request)) // Checks the whole conversation is resent once.
        } // Ends retry inspection.
        assertEquals(ChatCommandResult.Stored, repository.retryMessage(id, failed.id)) // Settles the same failed attempt.
        val saved = messages(repository, id) // Reads the completed retry only through Room.
        assertEquals(4, saved.size) // Checks retry appended only the model reply.
        assertEquals(failed.id, saved[2].id) // Checks the same user ID was reused.
        assertEquals(failed.seq, saved[2].seq) // Checks the same user position was reused.
        assertEquals(failed.createdAt, saved[2].createdAt) // Checks retry did not replace the original message timestamp.
        assertEquals(3, requests.get()) // Checks there was one first send, one failure and one manual retry.
    } // Ends same-row retry verification.

    /** Checks a busy chat refuses sends, retry, rename and deletion without queuing new work. */
    @Test // Verifies the in-flight gate while the network operation is paused.
    fun concurrentOperationsOnTheSameChatAreRejected() = runBlocking { // Holds one source call open while issuing competing commands.
        val repository = repository() // Uses one app-owned repository instance.
        val id = createChat(repository) // Creates the chat whose operation will own the lock.
        val arrived = CompletableDeferred<Unit>() // Signals the active request reached MockEngine.
        val release = CompletableDeferred<Unit>() // Controls when the request can complete.
        onRequest = { arrived.complete(Unit); withTimeout(5_000) { release.await() } } // Pauses the one active attempt.
        val active = async { repository.sendMessage(id, "First") } // Starts the send that owns this chat.
        try { // Cleans up the active attempt if an assertion fails.
            withTimeout(5_000) { arrived.await() } // Waits until the pending row and request exist.
            val pending = messages(repository, id).single() // Reads the current stored user row.
            assertEquals(ChatCommandResult.Busy, repository.sendMessage(id, "Second")) // Checks no queued extra user turn is created.
            assertEquals(ChatCommandResult.Busy, repository.retryMessage(id, pending.id)) // Checks an active row cannot be retried concurrently.
            assertEquals(ChatCommandResult.Busy, repository.renameChat(id, "Changed")) // Keeps local mutations from racing the exchange.
            assertEquals(ChatCommandResult.Busy, repository.deleteChat(id)) // Protects the active history from this repository's delete command.
            assertEquals(1, messages(repository, id).size) // Checks the rejected commands changed no messages.
            assertEquals(1, requests.get()) // Checks only the first operation reached HTTP.
        } finally { // Releases the operation even when the gate assertion fails.
            release.complete(Unit) // Allows the response to finish.
            active.await() // Waits for persistence before closing the test database.
        } // Ends the in-flight gate lifecycle.
    } // Ends same-chat concurrency verification.

    /** Checks one slow conversation does not hold a global lock over a different conversation. */
    @Test // Covers concurrent sends to independent chat IDs.
    fun differentChatsCanSendIndependently() = runBlocking { // Pauses the first chat but leaves the second free to complete.
        val repository = repository() // Uses the shared application repository and client.
        val first = createChat(repository) // Creates the slow conversation.
        val second = createChat(repository) // Creates the independent conversation.
        val arrived = CompletableDeferred<Unit>() // Signals only the slow request's start.
        val release = CompletableDeferred<Unit>() // Keeps that request pending.
        onRequest = { request -> if (requestTexts(request).first() == "Slow") { arrived.complete(Unit); withTimeout(5_000) { release.await() } } } // Holds only the selected conversation.
        val active = async { repository.sendMessage(first, "Slow") } // Starts the slow chat's exchange.
        try { // Ensures it is released after the independent send.
            withTimeout(5_000) { arrived.await() } // Confirms the first chat is still in flight.
            assertEquals(ChatCommandResult.Stored, withTimeout(5_000) { repository.sendMessage(second, "Independent") }) // Checks a different chat can complete immediately.
            assertEquals(MessageStatus.SENDING, messages(repository, first).single().status) // Checks the first chat remained pending meanwhile.
            assertEquals(2, messages(repository, second).size) // Checks the second chat's exchange was saved atomically.
        } finally { // Releases the slow source call regardless of assertion results.
            release.complete(Unit) // Allows the first reply to arrive.
            active.await() // Waits for its persistence and unlock.
        } // Ends independent chat verification.
    } // Ends per-chat rather than global locking verification.

    /** Checks the persisted unresolved-row guard also blocks a second repository instance. */
    @Test // Verifies transaction preparation rather than relying only on an in-memory mutex.
    fun secondRepositoryCannotAppendToAnInFlightChat() = runBlocking { // Creates two repository objects over the same Room database.
        val first = repository() // Owns the paused send.
        val second = repository() // Uses a separate lock map but the same persistent history.
        val id = createChat(first) // Creates their shared conversation.
        val arrived = CompletableDeferred<Unit>() // Signals the first pending exchange.
        val release = CompletableDeferred<Unit>() // Keeps it unresolved in Room.
        onRequest = { arrived.complete(Unit); withTimeout(5_000) { release.await() } } // Pauses every actual request, so a second one would also expose a bug.
        val active = async { first.sendMessage(id, "First") } // Saves the first pending user row.
        try { // Cleans up the first request after checking the persisted guard.
            withTimeout(5_000) { arrived.await() } // Waits until the first row is in Room.
            assertEquals(ChatCommandResult.UnresolvedMessage, second.sendMessage(id, "Second")) // Checks the second instance cannot append another pending turn.
            assertEquals(1, requests.get()) // Checks no second HTTP call was started.
        } finally { // Releases the first request for normal cleanup.
            release.complete(Unit) // Allows it to settle.
            active.await() // Waits for its completed exchange.
        } // Ends the persisted guard verification.
    } // Ends cross-instance preparation verification.

    /** Checks missing key, unreadable preferences and offline preflight do not create pending rows. */
    @Test // Keeps initial prerequisite failures distinct and the draft available to the caller.
    fun initialPrerequisiteFailuresLeaveHistoryUnchanged() = runBlocking { // Reuses one empty saved chat across blocked commands.
        val repository = repository() // Uses the controlled settings and connectivity dependencies.
        val id = createChat(repository) // Creates local history without invoking those prerequisites.
        settings.key = null // Simulates unconfigured preferences.
        assertEquals(ChatCommandResult.MissingApiKey, repository.sendMessage(id, "Question")) // Blocks before optimistic insertion.
        settings.failReads = true // Simulates unavailable key storage.
        assertEquals(ChatCommandResult.SettingsError, repository.sendMessage(id, "Question")) // Reports unreadable storage separately from a missing value.
        settings.failReads = false // Restores readable preferences.
        settings.key = UUID.randomUUID().toString() // Supplies a new fictitious value only in memory.
        online = false // Removes the initial connection prerequisite.
        assertEquals(ChatCommandResult.Offline, repository.sendMessage(id, "Question")) // Blocks before saving a failed placeholder.
        assertTrue(messages(repository, id).isEmpty()) // Checks no blocked preflight changed saved messages.
        assertEquals(0, requests.get()) // Checks no blocked preflight called the source.
    } // Ends initial prerequisite verification.

    /** Checks connectivity loss after the optimistic write becomes a persisted failure without HTTP. */
    @Test // Exercises the mandatory second prerequisite check in the repository.
    fun connectionLossAfterInsertionIsPersisted() = runBlocking { // Leaves the first check online and the second offline.
        val repository = repository() // Uses the same counted checker as the data source.
        val id = createChat(repository) // Creates a sendable empty chat.
        onCheck = { number -> if (number == 2) online = false } // Loses connectivity only after preparation has saved the user.
        assertEquals(ChatCommandResult.Stored, repository.sendMessage(id, "Question")) // Acknowledges the saved failure rather than returning a network result.
        assertEquals(MessageErrorCategory.NETWORK_UNAVAILABLE, messages(repository, id).single().errorCategory) // Checks the failure is observed from Room.
        assertEquals(0, requests.get()) // Checks the final repository gate stopped before HTTP.
    } // Ends post-insert connection loss verification.

    /** Checks a key cleared after insertion is recorded locally and starts no request. */
    @Test // Exercises a preferences change between initial preflight and the actual call.
    fun clearedKeyAfterInsertionIsPersisted() = runBlocking { // Clears only the fake preference value on its second read.
        val repository = repository() // Creates the production repository.
        val id = createChat(repository) // Creates a valid local chat.
        settings.onRead = { number -> if (number == 2) settings.key = null } // Removes the key after the pending row exists.
        assertEquals(ChatCommandResult.Stored, repository.sendMessage(id, "Question")) // Confirms the failed outcome was persisted.
        assertEquals(MessageErrorCategory.INVALID_API_KEY, messages(repository, id).single().errorCategory) // Checks the saved prerequisite failure label.
        assertEquals(0, requests.get()) // Checks the missing current key never reaches HTTP.
    } // Ends key-clear race verification.

    /** Checks the latest preference key is read again before the source call. */
    @Test // Covers changing the key after the optimistic write.
    fun changedKeyIsRereadBeforeHttp() = runBlocking { // Supplies a replacement only on the second preference read.
        val repository = repository() // Uses the controlled fake preference reader.
        val id = createChat(repository) // Creates a local conversation.
        val replacement = UUID.randomUUID().toString() // Generates a fictitious replacement credential.
        settings.onRead = { number -> if (number == 2) settings.key = replacement } // Updates preferences after preparation.
        onRequest = { request -> assertTrue("HTTP must use the current preference value", request.headers["x-goog-api-key"] == replacement) } // Uses a boolean assertion without echoing the value.
        assertEquals(ChatCommandResult.Stored, repository.sendMessage(id, "Question")) // Completes the exchange with the reread value.
        assertTrue(messages(repository, id).none { it.text.contains(replacement) }) // Checks the key does not enter persisted message text.
    } // Ends key replacement verification.

    /** Checks unreadable preferences after insertion also become a saved failure rather than a stuck pending row. */
    @Test // Covers the second preferences phase failing after the optimistic write.
    fun settingsFailureAfterInsertionIsPersisted() = runBlocking { // Makes only the second key read fail.
        val repository = repository() // Creates the production repository.
        val id = createChat(repository) // Creates a valid local chat.
        settings.onRead = { number -> if (number == 2) settings.failReads = true } // Fails preference access only after preparation succeeded.
        assertEquals(ChatCommandResult.Stored, repository.sendMessage(id, "Question")) // Acknowledges that the settled failure was saved.
        val saved = messages(repository, id).single() // Reads the stored outcome instead of a settings exception.
        assertEquals(MessageStatus.FAILED, saved.status) // Checks the row does not remain pending.
        assertEquals(MessageErrorCategory.UNKNOWN, saved.errorCategory) // Checks only a safe label was stored for the unavailable dependency.
        assertEquals(0, requests.get()) // Checks no HTTP call used stale credential data.
    } // Ends post-insert settings failure verification.

    /** Checks a non-final failed row cannot be retried by discarding a later saved turn. */
    @Test // Covers selecting a failed message from malformed older history.
    fun nonFinalFailedRowCannotBeRetried() = runBlocking { // Creates an intentionally invalid legacy sequence only in test memory.
        val repository = repository() // Creates the production repository.
        val id = createChat(repository) // Creates the parent chat.
        database.messageDao().insert(row("failed", id, Role.USER, MessageStatus.FAILED).copy(errorCategory = MessageErrorCategory.TIMEOUT)) // Inserts a failed first user.
        database.messageDao().insert(row("later", id, Role.MODEL, MessageStatus.SENT)) // Inserts a later row that must not be silently discarded.
        val before = messages(repository, id) // Records all saved rows before attempting recovery.
        assertEquals(ChatCommandResult.InvalidRetry, repository.retryMessage(id, "failed")) // Refuses to replay a failed row that is not final.
        assertEquals(before, messages(repository, id)) // Checks the malformed history remains unchanged for explicit handling.
        assertEquals(0, requests.get()) // Checks no invalid retry reaches Gemini.
    } // Ends non-final retry verification.

    /** Checks the repository reads a real isolated DataStore and persists only conversation text in Room. */
    @Test // Exercises all production persistence and networking collaborators together.
    fun realDataStoreSuppliesTheHeaderWithoutEnteringRoom() = runBlocking { // Creates a separate credential store without reading the app's real settings.
        val context = InstrumentationRegistry.getInstrumentation().targetContext // Gets the cache directory for this isolated test file.
        val file = File(context.cacheDir, "chat-repository-${UUID.randomUUID()}.preferences_pb") // Chooses a file unrelated to the production key store.
        val job = SupervisorJob() // Owns the store's background IO operations.
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job), produceFile = { file }) // Creates the real Preferences implementation for this one file.
        val preferences = DataStoreSettingsRepository(store) // Uses the actual settings repository without any fake reader.
        val dummy = UUID.randomUUID().toString() // Generates a fictitious credential that cannot authenticate with Gemini.
        try { // Always closes and removes only this test's preference storage.
            assertEquals(SettingsWriteResult.SUCCESS, preferences.saveApiKey(dummy)) // Stores the generated value only in Preferences DataStore.
            val repository = repository(preferences = preferences) // Connects the real settings implementation to production Room and the actual source.
            val id = createChat(repository) // Creates local history independently of the key store.
            onRequest = { request -> assertTrue("The header must use the DataStore value", request.headers["x-goog-api-key"] == dummy) } // Checks the current key with a non-printing boolean assertion.
            assertEquals(ChatCommandResult.Stored, repository.sendMessage(id, "Question")) // Runs the complete repository-to-source-to-Room cycle.
            val saved = messages(repository, id) // Reads the saved user and model rows through the repository stream.
            assertEquals(2, saved.size) // Checks both rows were persisted successfully.
            assertTrue("DataStore credentials must never become Room message text", saved.none { it.text.contains(dummy) }) // Checks the credential remained outside chat storage.
        } finally { // Runs after successful assertions or failures.
            job.cancelAndJoin() // Finishes the preference writer before deleting its file.
            if (file.exists()) assertTrue("The isolated preference file must be removable", file.delete()) // Removes only the unique test-owned file.
        } // Ends real-store cleanup.
    } // Ends the real persistence integration test.

    /** Checks a collector's own failure is not misreported as a database read failure. */
    @Test // Verifies the upstream-only Flow catch boundary.
    fun collectorFailuresKeepExceptionTransparency() = runBlocking { // Intentionally fails the consumer after a valid Room snapshot.
        val repository = repository() // Creates the production observation wrapper.
        val expected = IllegalArgumentException("Test consumer failure") // Uses a generic test exception with no data values.
        var observed: Exception? = null // Records only which exception instance reached the test collector.
        try { // Collects a valid saved-data stream whose consumer deliberately fails.
            repository.observeChats().collect { throw expected } // Simulates a consumer bug rather than an upstream database failure.
        } catch (error: Exception) { // Catches the deliberate test failure outside the repository's read boundary.
            observed = error // Allows checking identity without printing payloads or exception text.
        } // Ends intentional consumer failure observation.
        assertTrue("Collector failures must remain the original consumer exception", observed === expected) // Rejects a misleading storage result or a Flow exception-transparency violation.
    } // Ends consumer exception boundary verification.

    /** Checks an invalid already saved history is rejected before changing rows or calling Gemini. */
    @Test // Guards against filtering malformed prior turns into an apparently valid request.
    fun invalidSavedHistoryIsNotRepairedOrSent() = runBlocking { // Seeds a completed user row whose required model reply is missing.
        val repository = repository() // Creates the production repository.
        val id = createChat(repository) // Creates the parent chat.
        database.messageDao().insert(row("orphan-user", id, Role.USER, MessageStatus.SENT)) // Creates the deliberately invalid completed exchange.
        val before = messages(repository, id) // Saves the persisted state before the rejected command.
        assertEquals(ChatCommandResult.InvalidHistory, repository.sendMessage(id, "Next")) // Requires the full prospective history to be valid before insertion.
        assertEquals(before, messages(repository, id)) // Checks no repair or new pending row was made.
        assertEquals(0, requests.get()) // Checks malformed history never reaches HTTP.
    } // Ends malformed persisted history verification.

    /** Checks retry refuses missing, completed, model and foreign rows without changing them. */
    @Test // Covers local retry selection before any new request.
    fun invalidRetryTargetsAreRejected() = runBlocking { // Creates one completed exchange and an unrelated failed row.
        val repository = repository() // Uses the repository under test.
        val id = createChat(repository) // Creates the first chat.
        assertEquals(ChatCommandResult.Stored, repository.sendMessage(id, "First")) // Creates saved completed user and model rows.
        val completed = messages(repository, id) // Reads the two retry-ineligible rows.
        assertEquals(ChatCommandResult.NotFound, repository.retryMessage(id, "missing")) // Rejects a nonexistent local row.
        assertEquals(ChatCommandResult.InvalidRetry, repository.retryMessage(id, completed[0].id)) // Refuses replaying a completed user.
        assertEquals(ChatCommandResult.InvalidRetry, repository.retryMessage(id, completed[1].id)) // Refuses retrying a model row.
        val other = createChat(repository) // Creates a different local conversation.
        database.messageDao().insert(row("foreign-failed", other, Role.USER, MessageStatus.FAILED).copy(errorCategory = MessageErrorCategory.TIMEOUT)) // Creates a failed row in the other chat.
        assertEquals(ChatCommandResult.InvalidRetry, repository.retryMessage(id, "foreign-failed")) // Refuses changing a row owned by another conversation.
        assertEquals(1, requests.get()) // Checks only the original successful send reached HTTP.
    } // Ends invalid retry selection verification.

    /** Checks cancelling a running HTTP request records a retryable failure and releases the chat. */
    @Test // Guards against leaving SENDING forever after normal coroutine cancellation.
    fun cancellationDuringHttpPersistsFailureAndAllowsRetry() = runBlocking { // Keeps the source pending until the caller is cancelled.
        val repository = repository() // Creates the repository whose cleanup is being tested.
        val id = createChat(repository) // Creates the conversation.
        val arrived = CompletableDeferred<Unit>() // Signals the point at which HTTP is active.
        onRequest = { arrived.complete(Unit); awaitCancellation() } // Waits indefinitely without sleeping until the send is cancelled.
        val active = async { repository.sendMessage(id, "Question") } // Starts the pending request.
        withTimeout(5_000) { arrived.await() } // Waits for the request to reach the controlled cancellation point.
        active.cancelAndJoin() // Waits for NonCancellable Room cleanup to finish.
        val failed = messages(repository, id).single() // Reads the settled cancellation state from Room.
        assertEquals(MessageStatus.FAILED, failed.status) // Checks no permanently pending row remains.
        assertEquals(MessageErrorCategory.UNKNOWN, failed.errorCategory) // Checks cancellation details are not persisted.
        onRequest = {} // Restores the normal simulated response behaviour.
        assertEquals(ChatCommandResult.Stored, repository.retryMessage(id, failed.id)) // Checks the chat lock was released and the same row can be retried.
    } // Ends cancellation during transport verification.

    /** Checks cancellation during the post-insert settings recheck also cleans up the pending row. */
    @Test // Covers cancellation before an HTTP request exists.
    fun cancellationAfterInsertionBeforeHttpPersistsFailure() = runBlocking { // Pauses the second preferences read after the optimistic transaction.
        val repository = repository() // Creates the production repository.
        val id = createChat(repository) // Creates the parent chat.
        val inserted = CompletableDeferred<Unit>() // Signals that preparation finished and rereading the key started.
        settings.onRead = { number -> if (number == 2) { inserted.complete(Unit); awaitCancellation() } } // Holds the recheck until caller cancellation.
        val active = async { repository.sendMessage(id, "Question") } // Saves the pending user and reaches the held preferences read.
        withTimeout(5_000) { inserted.await() } // Waits for the precise post-insert cancellation window.
        active.cancelAndJoin() // Waits for cleanup even though the caller's job is cancelled.
        assertEquals(MessageStatus.FAILED, messages(repository, id).single().status) // Checks this earlier cancellation also settled the row.
        assertEquals(0, requests.get()) // Checks no HTTP operation was constructed.
    } // Ends post-insert cancellation window verification.

    /** Checks an atomic completion rollback never leaves a SENT user without a reply. */
    @Test // Forces the model reply ID to collide with its user row.
    fun failedCompletionWriteRollsBackAndSettlesTheUser() = runBlocking { // Uses deliberately colliding local IDs instead of corrupting production storage.
        val ids = ArrayDeque(listOf("chat", "user", "user")) // Supplies a chat ID, user ID and colliding reply ID.
        val repository = repository { ids.removeFirst() } // Injects deterministic local ID generation.
        val id = createChat(repository) // Saves the first generated ID as the parent chat.
        assertEquals(ChatCommandResult.StorageError, repository.sendMessage(id, "Question")) // Reports the failed local completion without returning model text.
        val saved = messages(repository, id) // Reads the post-rollback state through Room.
        assertEquals(1, saved.size) // Checks the reply was not partially inserted.
        assertEquals(MessageStatus.FAILED, saved.single().status) // Checks rollback plus cleanup did not leave a SENT orphan or SENDING forever.
        assertEquals(MessageErrorCategory.UNKNOWN, saved.single().errorCategory) // Checks only a safe cleanup label was saved.
    } // Ends failed atomic completion verification.

    /** Checks the configured credential cannot be stored as a user message or echoed model reply. */
    @Test // Uses a runtime-generated fictitious credential for persistence boundary checks.
    fun credentialTextNeverEntersRoomThroughSending() = runBlocking { // Exercises both incoming user text and a sensitive returned reply.
        val repository = repository() // Creates the production repository with a known fake preference value.
        val id = createChat(repository) // Creates an empty local conversation.
        val dummy = settings.key ?: error("Expected fictitious test credential") // Reads only the generated in-memory value.
        assertEquals(ChatCommandResult.InvalidInput, repository.sendMessage(id, "Key: $dummy")) // Rejects storing the configured value in a user turn.
        assertTrue(messages(repository, id).isEmpty()) // Checks no user credential text was inserted.
        body = """{"candidates":[{"content":{"parts":[{"text":"$dummy"}]},"finishReason":"STOP"}]}""" // Simulates a server reply that improperly echoes the credential.
        assertEquals(ChatCommandResult.Stored, repository.sendMessage(id, "Question")) // Persists a blocked outcome instead of the sensitive reply.
        val saved = messages(repository, id) // Reads only persisted domain values.
        assertEquals(MessageErrorCategory.BLOCKED_RESPONSE, saved.single().errorCategory) // Checks the sensitive reply became a safe failure label.
        assertTrue("No configured credential may enter saved message text", saved.none { it.text.contains(dummy) }) // Uses a boolean assertion that never prints the value.
        assertFalse(saved.any { it.role == Role.MODEL }) // Checks no sensitive model row was saved.
    } // Ends credential persistence boundary verification.

    /** Checks an unavailable table produces an explicit read/write storage outcome without exceptions. */
    @Test // Distinguishes unreadable persistence from an empty history.
    fun storageFailuresAreTypedInsteadOfEmptyData() = runBlocking { // Breaks only the schema of this isolated in-memory test database.
        val repository = repository() // Keeps the production repository pointing to the isolated database.
        database.openHelper.writableDatabase.execSQL("DROP TABLE chats") // Makes read and write queries fail deterministically without touching any production file.
        assertEquals(ChatCommandResult.StorageError, repository.createChat("Local chat")) // Checks local write exceptions do not escape.
        assertEquals(ChatReadResult.StorageError, repository.observeChats().first()) // Checks a read error is not silently replaced with an empty list.
    } // Ends explicit storage failure verification.

    private fun repository(preferences: SettingsRepository = settings, newId: () -> String = { UUID.randomUUID().toString() }): RoomChatRepository { // Builds production collaborators around a test-owned engine and selected preference implementation.
        val connected: suspend () -> Boolean = { onCheck(checks.incrementAndGet()); online } // Shares fresh prerequisite checks with repository and source.
        val engine = MockEngine.config { // Keeps actual request construction and decoding inside the test process.
            addHandler { request -> // Runs only after all repository and source gates passed.
                requests.incrementAndGet() // Counts actual engine attempts atomically.
                onRequest(request) // Performs this case's inspection or suspension barrier.
                transportFailure?.let { throw it } // Injects a controlled transport error when requested.
                respond(body, HttpStatusCode.fromValue(status), headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) // Supplies the selected harmless response.
            } // Ends the simulated engine operation.
        } // Ends the owned engine factory.
        val logger = object : Logger { // Keeps request and credential diagnostics out of the console.
            /** Ignores messages because the configured test client disables logging. */
            override fun log(message: String) = Unit // Writes no test payload or credential value.
        } // Ends the silent logger.
        val client = HttpClientFactory.create(false, engine, logger) // Uses the actual JSON, timeout and default URL setup.
        clients.add(client) // Records ownership so cleanup also runs after assertion failure.
        return RoomChatRepository(database, preferences, GeminiRemoteDataSource(client, connected), connected, clock::getAndIncrement, newId) // Supplies every dependency manually to the real repository.
    } // Ends production repository fixture construction.

    private suspend fun createChat(repository: ChatRepository): String { // Reads only a local persisted ID from the command result.
        return when (val result = repository.createChat("Local chat")) { // Creates a parent row without network or key prerequisites.
            is ChatCommandResult.Created -> result.chatId // Uses the app-generated ID for later commands.
            else -> error("Could not create isolated test chat") // Reports no credential or raw persistence details if setup fails.
        } // Ends local creation result handling.
    } // Ends test chat creation.

    private suspend fun messages(repository: ChatRepository, chatId: String): List<Message> { // Reads the same Room-backed stream the future UI will use.
        return when (val result = withTimeout(5_000) { repository.observeMessages(chatId).first() }) { // Bounds reading the current persisted snapshot.
            is ChatReadResult.Success -> result.value // Returns only saved domain messages for assertions.
            ChatReadResult.StorageError -> error("Could not read isolated test history") // Does not print SQLite exception details.
        } // Ends safe read result handling.
    } // Ends persisted message observation.

    private suspend fun verifySavedFailure(repository: ChatRepository, chatId: String, category: MessageErrorCategory) { // Requires failed network results to reach Room before command completion.
        assertEquals(ChatCommandResult.Stored, repository.sendMessage(chatId, "Question")) // Receives only a local persistence acknowledgement.
        val saved = messages(repository, chatId) // Reads the settled failure from Room.
        assertEquals(1, saved.size) // Checks failure created no fabricated model reply.
        assertEquals(MessageStatus.FAILED, saved.single().status) // Checks the pending user was settled.
        assertEquals(category, saved.single().errorCategory) // Checks the safe persisted failure label.
    } // Ends failed outcome assertion.

    private fun requestTexts(request: HttpRequestData) = Json.parseToJsonElement((request.body as TextContent).text).jsonObject.getValue("contents").jsonArray.map { it.jsonObject.getValue("parts").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content } // Reads only harmless question/reply fixtures from the actual request body.

    private fun row(id: String, chatId: String, role: Role, status: MessageStatus) = MessageEntity(id, chatId, role, "Saved fixture", 0, status, 1_000) // Supplies a raw legacy row only for persistence rejection cases.

    private class TestSettings : SettingsRepository { // Provides controlled reads without saving a key anywhere outside test memory.
        var key: String? = UUID.randomUUID().toString() // Generates a fresh fictitious value for each test.
        var failReads = false // Controls a storage failure separately from a missing value.
        var onRead: suspend (Int) -> Unit = {} // Coordinates a key change or cancellation at a precise prerequisite stage.
        private val reads = AtomicInteger() // Counts preference reads safely across concurrent chats.

        /** Returns a fresh controlled preferences snapshot on each subscription. */
        override val readResult: Flow<SettingsReadResult> get() = flow { // Matches the cold production settings stream contract.
            onRead(reads.incrementAndGet()) // Applies the case's controlled event before returning this snapshot.
            if (failReads) throw IOException("Settings unavailable") // Simulates generic storage failure without exposing a value.
            emit(SettingsReadResult.Success(key)) // Keeps the credential only in memory for this read.
        } // Ends controlled preference reading.

        /** Provides the nullable key stream required by the settings contract. */
        override val apiKey: Flow<String?> get() = flow { emit(key) } // Supplies only the value used by this test's fake settings.

        /** Updates only the fake's in-memory value. */
        override suspend fun saveApiKey(value: String): SettingsWriteResult { // Implements the contract without a persistent credential file.
            key = value // Replaces the test-only in-memory value.
            return SettingsWriteResult.SUCCESS // Reports the fake update completed.
        } // Ends the fake save operation.

        /** Removes only the fake's in-memory value. */
        override suspend fun clearApiKey(): SettingsWriteResult { // Implements clear without touching production preferences.
            key = null // Leaves the fake unconfigured.
            return SettingsWriteResult.SUCCESS // Reports the fake deletion completed.
        } // Ends the fake clear operation.
    } // Ends controlled settings injection.
} // Ends offline-first repository verification.
