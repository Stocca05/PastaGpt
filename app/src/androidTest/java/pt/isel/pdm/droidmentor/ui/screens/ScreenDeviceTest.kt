package pt.isel.pdm.droidmentor.ui.screens // Checks real Compose interactions using isolated collaborators.

import androidx.lifecycle.ViewModel // Types test-owned models without a DI framework.
import androidx.lifecycle.ViewModelStore // Owns model lifetimes independently of the app's production graph.
import androidx.lifecycle.viewModelScope // Waits for cancelled collectors before closing test storage.
import androidx.room.Room // Creates an isolated in-memory database with the production schema.
import androidx.compose.ui.semantics.SemanticsProperties // Checks password masking without asserting printable credential text.
import androidx.compose.ui.test.SemanticsMatcher // Matches safe password semantics.
import androidx.compose.ui.test.assert // Checks field semantics rather than its raw contents.
import androidx.compose.ui.test.assertIsEnabled // Checks available user actions.
import androidx.compose.ui.test.assertIsNotEnabled // Checks blocked sends and retries.
import androidx.compose.ui.test.hasSetTextAction // Finds the single input field without querying its value.
import androidx.compose.ui.test.junit4.createComposeRule // Hosts real screen content on the emulator.
import androidx.compose.ui.test.onNodeWithText // Finds safe resource labels and harmless test messages.
import androidx.compose.ui.test.performClick // Invokes the actual screen event bindings.
import androidx.compose.ui.test.performScrollTo // Keeps form actions reachable when the keyboard reduces space.
import androidx.compose.ui.test.performTextInput // Types generated fake input or harmless questions.
import androidx.test.ext.junit.runners.AndroidJUnit4 // Runs against real Android UI and Room behaviour.
import androidx.test.platform.app.InstrumentationRegistry // Supplies target context and main-thread cleanup.
import io.ktor.client.HttpClient // Owns the test's client using the real app configuration.
import io.ktor.client.engine.config // Configures the injected engine factory using the declared Ktor API.
import io.ktor.client.engine.mock.MockEngine // Simulates Gemini without a real endpoint or credential.
import io.ktor.client.engine.mock.respond // Supplies controlled provider responses.
import io.ktor.client.plugins.logging.Logger // Discards test HTTP diagnostics.
import io.ktor.http.ContentType // Labels the simulated reply as JSON.
import io.ktor.http.HttpHeaders // Supplies the response content-type header.
import io.ktor.http.HttpStatusCode // Selects success or a provider error.
import io.ktor.http.headersOf // Constructs harmless simulated response headers.
import kotlinx.coroutines.CompletableDeferred // Holds a reply so pending UI can be inspected.
import kotlinx.coroutines.Job // Waits for model cancellation and Room cleanup.
import kotlinx.coroutines.flow.Flow // Implements the replaceable settings contract in memory.
import kotlinx.coroutines.flow.MutableStateFlow // Controls configured status and connectivity.
import kotlinx.coroutines.flow.map // Projects the fake preferences key contract without a file.
import kotlinx.coroutines.runBlocking // Bridges suspend test queries on the JUnit worker thread.
import kotlinx.coroutines.withTimeout // Bounds cleanup and local database operations.
import org.junit.After // Releases only this test's resources.
import org.junit.Assert.assertEquals // Compares safe IDs, counts and saved status enums.
import org.junit.Assert.assertTrue // Checks privacy and state without printing keys.
import org.junit.Before // Creates fresh isolated storage before each test.
import org.junit.Rule // Owns the test UI activity lifecycle.
import org.junit.Test // Marks the new device interaction cases.
import org.junit.runner.RunWith // Selects AndroidJUnit4.
import pt.isel.pdm.droidmentor.data.local.DroidMentorDatabase // Uses the exact production Room schema.
import pt.isel.pdm.droidmentor.data.remote.GeminiRemoteDataSource // Uses the real stateless source and result mapper around MockEngine.
import pt.isel.pdm.droidmentor.data.remote.HttpClientFactory // Uses the real JSON, timeout and URL configuration.
import pt.isel.pdm.droidmentor.data.repository.ChatCommandResult // Reads a saved local ID after creation.
import pt.isel.pdm.droidmentor.data.repository.RoomChatRepository // Persists actual pending, failed and completed rows.
import pt.isel.pdm.droidmentor.data.repository.SettingsReadResult // Supplies typed fake preference reads.
import pt.isel.pdm.droidmentor.data.repository.SettingsRepository // Injects preferences by their interface.
import pt.isel.pdm.droidmentor.data.repository.SettingsWriteResult // Supplies typed fake save/clear results.
import pt.isel.pdm.droidmentor.domain.ConnectivityStatus // Controls validated-status UI updates without changing the user's network settings.
import pt.isel.pdm.droidmentor.domain.MessageStatus // Checks actual persisted pending and settled rows.
import pt.isel.pdm.droidmentor.domain.Role // Checks retry produces one user row and one model row.
import pt.isel.pdm.droidmentor.ui.screens.activechat.ActiveChatScreen // Exercises the actual message layout and event handlers.
import pt.isel.pdm.droidmentor.ui.screens.activechat.ActiveChatViewModel // Uses the real Room-driven screen model with explicit test dependencies.
import pt.isel.pdm.droidmentor.ui.screens.chathistory.ChatHistoryScreen // Exercises actual local creation and saved-ID navigation bindings.
import pt.isel.pdm.droidmentor.ui.screens.chathistory.ChatHistoryViewModel // Uses the real offline history model.
import pt.isel.pdm.droidmentor.ui.screens.settings.SettingsScreen // Exercises password and command widgets.
import pt.isel.pdm.droidmentor.ui.screens.settings.SettingsViewModel // Uses the real settings event logic.
import pt.isel.pdm.droidmentor.ui.theme.DroidMentorTheme // Applies the production Material theme.
import java.util.UUID // Generates fictitious credentials only in memory.
import java.util.concurrent.atomic.AtomicInteger // Counts requests safely across UI and engine threads.
import java.util.concurrent.atomic.AtomicReference // Captures only a safe local navigation ID across UI and test threads.

/** Checks the actual new screens, real Room outcomes and simulated networking without touching production data. */
@RunWith(AndroidJUnit4::class) // Executes real UI events on the connected emulator.
class ScreenDeviceTest { // Uses an isolated in-memory database and generated fictitious preference input.
    /** Owns the Compose test activity for every case. */
    @get:Rule // Creates and closes one activity per test.
    val compose = createComposeRule() // Uses the already declared Compose testing dependency.

    private lateinit var database: DroidMentorDatabase // Holds only this test's Room rows.
    private lateinit var client: HttpClient // Owns only this test's simulated engine.
    private lateinit var repository: RoomChatRepository // Uses real local transactions and request coordination.
    private lateinit var preferences: MemorySettings // Supplies generated fake configuration through the contract.
    private val store = ViewModelStore() // Owns explicitly injected test models.
    private val models = mutableListOf<ViewModel>() // Records model jobs for deterministic cleanup.
    private val network = MutableStateFlow(ConnectivityStatus.CONNECTED) // Supplies a controllable validated-status stream.
    private val calls = AtomicInteger() // Counts only MockEngine requests.
    private val responseCode = AtomicInteger(200) // Selects a controlled success or failure response.
    private var replyGate: CompletableDeferred<Unit>? = null // Optionally pauses the response during a pending-state test.

    /** Constructs isolated production collaborators and a silent MockEngine client. */
    @Before // Creates no app history file and never reads a configured production key.
    fun prepare() { // Sets up one independent test graph.
        val context = InstrumentationRegistry.getInstrumentation().targetContext // Uses only the context required to create test Room storage.
        database = Room.inMemoryDatabaseBuilder(context, DroidMentorDatabase::class.java).build() // Keeps the real schema and transaction semantics.
        preferences = MemorySettings() // Generates a fake configured value in memory.
        val engine = MockEngine.config { // Uses the same engine-factory injection as the existing repository tests.
            addHandler { // Handles a request only after the real repository and source preflight gates.
                calls.incrementAndGet() // Records an actual simulated HTTP attempt.
                replyGate?.await() // Pauses only when the pending-state case installs a gate.
                respond("""{"candidates":[{"content":{"role":"model","parts":[{"text":"Mentor UI test reply"}]},"finishReason":"STOP"}]}""", HttpStatusCode.fromValue(responseCode.get()), headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) // Supplies harmless content through the real response mapper.
            } // Ends simulated response handling.
        } // Ends MockEngine configuration.
        val silent = object : Logger { // Keeps all request diagnostics out of test output.
            /** Discards diagnostics without printing input or credentials. */
            override fun log(message: String) = Unit // Never writes the supplied message.
        } // Ends the silent logger.
        client = HttpClientFactory.create(false, engine, silent) // Uses the production client configuration with logging disabled.
        val connected: suspend () -> Boolean = { network.value == ConnectivityStatus.CONNECTED } // Checks current test connectivity on every actual attempt.
        repository = RoomChatRepository(database, preferences, GeminiRemoteDataSource(client, connected), connected) // Constructs all production dependencies manually around isolated state.
    } // Ends independent graph setup.

    /** Cancels models and waits for their cleanup before releasing Room and the owned client. */
    @After // Restores only resources created by this test.
    fun cleanup() { // Avoids closing any process-owned production dependency.
        val jobs = models.mapNotNull { it.viewModelScope.coroutineContext[Job] } // Captures the active model jobs before owner removal.
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() } // Cancels model ownership on Android's main thread.
        runBlocking { withTimeout(10_000) { jobs.forEach { it.join() } } } // Waits on the JUnit worker until repository cancellation cleanup has finished.
        client.close() // Releases only the client and MockEngine owned by this fixture.
        database.close() // Discards only the in-memory test history.
    } // Ends isolated test cleanup.

    /** Checks password semantics, explicit visibility and save/clear event bindings without printing input. */
    @Test // Exercises the actual Settings widgets with a replaceable in-memory contract.
    fun settingsMasksInputAndForwardsSaveAndClear() { // Starts without a configured production or test key.
        preferences.read.value = SettingsReadResult.Success(null) // Resets only this fixture's in-memory value.
        val model = own("settings", SettingsViewModel(preferences)) // Owns the real model without a production preference file.
        compose.setContent { DroidMentorTheme { SettingsScreen({}, model) } } // Displays the actual stateless form with explicit test injection.
        compose.waitUntil(5_000) { !model.state.value.loading } // Waits for the initial typed preferences snapshot.
        compose.onNode(hasSetTextAction()).performTextInput("fixture-${UUID.randomUUID()}") // Types a generated fictitious value rather than a source constant.
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)) // Checks masking without asserting raw field text.
        compose.onNodeWithText("Show").performClick() // Reveals only the newly entered fictitious draft through the real toggle.
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Password)) // Checks visibility changed without printing its value.
        compose.onNodeWithText("Hide").performClick() // Restores masking before any later diagnostic or screenshot.
        compose.onNodeWithText("Save key").performScrollTo().performClick() // Invokes the actual guarded save binding.
        compose.waitUntil(5_000) { model.state.value.configured && !model.state.value.saving } // Waits for successful fake persistence and presence observation.
        assertEquals(1, preferences.saves) // Checks exactly one save command reached the contract.
        assertTrue(model.state.value.draft.isEmpty()) // Checks successful input was cleared from memory.
        compose.onNode(hasSetTextAction()).assertDoesNotExist() // Does not ask for the just-saved key again.
        compose.onNodeWithText("API key configured").assertExists() // Confirms the successful saved status.
        compose.onNodeWithText("Clear key").performScrollTo().performClick() // Invokes explicit selective removal through the actual widget.
        compose.waitUntil(5_000) { !model.state.value.configured && !model.state.value.saving } // Waits for absent configuration without exposing a value.
        assertEquals(1, preferences.clears) // Checks the clear action reached the repository once.
    } // Ends actual settings interactions.

    /** Checks an existing key opens as configured and can be changed or left untouched. */
    @Test // Reproduces Settings re-entry with a generated saved configuration.
    fun configuredSettingsOnlyAskForAnExplicitReplacement() { // Uses the fixture's generated in-memory key.
        val original = (preferences.read.value as SettingsReadResult.Success).apiKey // Keeps a comparison value in test memory only.
        val model = own("settings", SettingsViewModel(preferences)) // Creates a fresh Settings owner over already configured preferences.
        compose.setContent { DroidMentorTheme { SettingsScreen({}, model) } } // Displays the actual settings screen.
        compose.waitUntil(5_000) { !model.state.value.loading } // Waits for confirmed key presence.
        compose.onNodeWithText("API key configured").assertExists() // Recognises the saved key immediately after reading.
        compose.onNode(hasSetTextAction()).assertDoesNotExist() // Shows no misleading empty key field.
        compose.onNodeWithText("Change key").performScrollTo().performClick() // Explicitly starts replacement.
        compose.onNode(hasSetTextAction()).performTextInput("fixture-${UUID.randomUUID()}") // Types a generated replacement without touching production preferences.
        compose.onNodeWithText("Cancel").performScrollTo().performClick() // Discards only the unsaved draft.
        compose.onNode(hasSetTextAction()).assertDoesNotExist() // Returns to the saved summary.
        assertTrue((preferences.read.value as SettingsReadResult.Success).apiKey == original) // Confirms cancellation preserves the original without printing it.
        assertEquals(0, preferences.saves) // Confirms no implicit save occurred.
        assertEquals(0, preferences.clears) // Confirms no implicit removal occurred.
        compose.onNodeWithText("Change key").performScrollTo().performClick() // Opens another explicit replacement form.
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)) // Starts the new form masked.
        compose.onNodeWithText("Save replacement").assertIsNotEnabled() // Does not accept an empty replacement.
        compose.onNode(hasSetTextAction()).performTextInput("fixture-${UUID.randomUUID()}") // Supplies a new generated key draft.
        compose.onNodeWithText("Save replacement").performScrollTo().performClick() // Saves only after explicit confirmation.
        compose.waitUntil(5_000) { !model.state.value.saving && !model.state.value.showKeyInput } // Waits for the saved summary to return.
        compose.onNodeWithText("API key configured").assertExists() // Leaves the replacement configured instead of asking for another entry.
        compose.onNode(hasSetTextAction()).assertDoesNotExist() // Removes completed sensitive input from the UI.
        assertEquals(1, preferences.saves) // Performs exactly one replacement save.
        assertEquals(0, preferences.clears) // Replaces atomically without deleting the old key first.
    } // Ends configured-key screen regression coverage.

    /** Checks a failed settings read is explained without falsely requesting a missing key. */
    @Test // Keeps storage errors separate from initial setup in the real UI.
    fun unreadableSettingsDoNotDisplayMissingKeySetup() { // Uses a typed error with no actual file failure.
        preferences.read.value = SettingsReadResult.StorageError // Makes only the isolated fixture unreadable.
        val model = own("settings", SettingsViewModel(preferences)) // Observes the typed error through production logic.
        compose.setContent { DroidMentorTheme { SettingsScreen({}, model) } } // Displays the actual error state.
        compose.waitUntil(5_000) { model.state.value.storageError } // Waits for initial observation.
        compose.onNodeWithText("Settings could not be read. Try again later.").assertExists() // Explains the real failure safely.
        compose.onNodeWithText("API key not configured").assertDoesNotExist() // Does not confuse unreadable preferences with absence.
        compose.onNode(hasSetTextAction()).assertDoesNotExist() // Does not request input that could overwrite an unknown saved key.
        assertEquals(0, preferences.saves) // Leaves preferences untouched.
        assertEquals(0, preferences.clears) // Does not remove potentially saved configuration.
    } // Ends unreadable-settings UI regression coverage.

    /** Checks the history action creates local storage before requesting navigation, even without prerequisites. */
    @Test // Covers the real new-chat widget, Room insert and one-shot event together.
    fun historyCreatesAndOpensASavedChatWhileOffline() { // Starts with empty isolated Room history and no configured key.
        preferences.read.value = SettingsReadResult.Success(null) // Removes only the in-memory fixture configuration.
        network.value = ConnectivityStatus.DISCONNECTED // Blocks all simulated remote operations.
        val model = own("history", ChatHistoryViewModel(repository)) // Uses the actual local history command logic.
        val opened = AtomicReference<String?>() // Captures only the local ID passed to the screen's navigation callback.
        compose.setContent { DroidMentorTheme { ChatHistoryScreen({ opened.set(it) }, {}, model) } } // Displays the actual saved-history screen with explicit test injection.
        compose.waitUntil(5_000) { !model.state.value.loading } // Waits for the initial empty Room snapshot.
        compose.onNodeWithText("No conversations yet. Start a new one.").assertExists() // Checks empty history is represented without sample rows.
        compose.onNodeWithText("New conversation").performClick() // Invokes the actual local creation action.
        compose.waitUntil(5_000) { opened.get() != null && !model.state.value.creating } // Waits for persistence acknowledgement before the navigation event.
        val id = opened.get() ?: error("Expected a saved local navigation ID") // Reads no message or credential value.
        runBlocking { assertTrue(database.chatDao().getById(id) != null) } // Confirms navigation referenced an existing Room row.
        assertEquals(0, calls.get()) // Confirms local creation required neither a key nor network access.
    } // Ends real history-to-persisted-ID interaction.

    /** Checks pending progress, disabled sending and a usable reply observed only after Room persistence. */
    @Test // Exercises the complete real ViewModel/repository/Room/source chain around MockEngine.
    fun chatShowsPendingThenThePersistedReply() { // Holds the simulated provider response after the optimistic local write.
        replyGate = CompletableDeferred() // Keeps the real send command suspended at its remote boundary.
        val (id, model) = showChat() // Creates one saved local conversation and its actual screen.
        compose.onNode(hasSetTextAction()).performTextInput("UI test question") // Supplies harmless unsent text through the actual field.
        compose.onNodeWithText("Send").performClick() // Starts the production command through the screen event.
        compose.waitUntil(5_000) { calls.get() == 1 && model.state.value.pending } // Waits for the saved user row and actual simulated request.
        compose.onNodeWithText("Send").assertIsNotEnabled() // Checks a second UI send is blocked while the attempt owns the chat.
        compose.onNodeWithText("Waiting for a reply…").assertExists() // Checks in-flight feedback is visible.
        runBlocking { assertEquals(MessageStatus.SENDING, database.messageDao().getMessages(id).single().status) } // Checks progress came from a real pending Room row.
        replyGate?.complete(Unit) // Supplies the simulated reply after pending-state assertions.
        compose.waitUntil(5_000) { model.state.value.messages.size == 2 && !model.state.value.working } // Waits for atomic Room completion and local command cleanup.
        compose.onNodeWithText("Mentor UI test reply").assertExists() // Checks persisted model content reached the actual UI.
        runBlocking { assertTrue(database.messageDao().getMessages(id).all { it.status == MessageStatus.SENT }) } // Checks both saved turns settled together.
    } // Ends the full pending-to-reply device interaction.

    /** Checks a persisted provider failure enables same-row retry without adding a duplicate user turn. */
    @Test // Covers actual error text and Retry widget bindings.
    fun savedFailureCanBeRetriedThroughTheScreen() { // Supplies a controlled HTTP failure for the first attempt.
        responseCode.set(500) // Uses the real mapper's provider-error branch.
        val (id, model) = showChat() // Creates isolated saved history and real screen content.
        compose.onNode(hasSetTextAction()).performTextInput("Retry test question") // Supplies harmless user text.
        compose.onNodeWithText("Send").performClick() // Starts one real repository attempt.
        compose.waitUntil(5_000) { model.state.value.failed && !model.state.value.working } // Waits for the persisted error rather than a raw network event.
        val failedId = runBlocking { database.messageDao().getMessages(id).single().id } // Captures only the safe local row identity.
        compose.onNodeWithText("The provider is temporarily unavailable. Retry later.").assertExists() // Checks the saved category's safe resource text.
        compose.onNodeWithText("Send").assertIsNotEnabled() // Checks the unresolved failed row blocks a new turn.
        responseCode.set(200) // Makes the next explicit attempt usable.
        compose.onNodeWithText("Retry").assertIsEnabled().performClick() // Invokes the actual same-row retry action.
        compose.waitUntil(5_000) { model.state.value.messages.size == 2 && !model.state.value.working } // Waits for the settled retry and model row.
        runBlocking { // Reads only the isolated local database after UI-driven completion.
            val saved = database.messageDao().getMessages(id) // Retrieves the production DAO's ordered rows.
            assertEquals(failedId, saved.first().id) // Checks the original failed user row was reused.
            assertEquals(listOf(Role.USER, Role.MODEL), saved.map { it.role }) // Checks retry created no duplicate user turn.
        } // Ends same-row persistence assertions.
        assertEquals(2, calls.get()) // Checks only the two explicitly requested attempts reached the engine.
    } // Ends error-to-retry device verification.

    /** Checks offline history stays readable and the draft remains editable while Send is disabled. */
    @Test // Covers the actual banner and input guard after a completed local exchange.
    fun offlineChatKeepsSavedMessagesAndEditableDraft() { // Starts with one successful simulated exchange.
        val (_, model) = showChat() // Creates the actual selected-conversation UI.
        compose.onNode(hasSetTextAction()).performTextInput("Saved question") // Supplies one harmless online question.
        compose.onNodeWithText("Send").performClick() // Persists its complete simulated exchange through the real repository.
        compose.waitUntil(5_000) { model.state.value.messages.size == 2 && !model.state.value.working } // Waits for saved history and draft cleanup.
        network.value = ConnectivityStatus.DISCONNECTED // Changes only this fixture's status, leaving the user's device network untouched.
        compose.waitUntil(5_000) { !model.state.value.connected } // Waits for the actual ViewModel's status projection.
        compose.onNodeWithText("You are offline. New interactions are unavailable.").assertExists() // Checks the persistent banner.
        compose.onNodeWithText("Mentor UI test reply").assertExists() // Checks saved content remains visible without an API call.
        compose.onNode(hasSetTextAction()).performTextInput("Editable offline question") // Keeps offline draft editing available.
        compose.onNodeWithText("Send").assertIsNotEnabled() // Checks the visible action shares the current connectivity gate.
        assertEquals("Editable offline question", model.state.value.draft) // Checks the unsent ordinary text was preserved.
        assertEquals(1, calls.get()) // Checks going offline and editing started no additional network attempt.
    } // Ends actual offline UI verification.

    /** Checks missing key configuration blocks interactions without blocking unsent draft editing. */
    @Test // Covers the key prerequisite in the actual conversation layout.
    fun unconfiguredChatKeepsDraftButBlocksSending() { // Removes only the generated in-memory fixture value.
        preferences.read.value = SettingsReadResult.Success(null) // Leaves all production preferences untouched.
        val (_, model) = showChat() // Displays a readable saved empty conversation.
        compose.waitUntil(5_000) { !model.state.value.loading } // Waits for the first local history snapshot.
        compose.onNode(hasSetTextAction()).performTextInput("Question without configuration") // Keeps editing available before configuration.
        compose.onNodeWithText("Configure an API key in Settings before sending.").assertExists() // Explains the missing prerequisite safely.
        compose.onNodeWithText("Send").assertIsNotEnabled() // Checks no request can start from the widget.
        assertEquals(0, calls.get()) // Checks the remote engine received nothing.
    } // Ends unconfigured UI verification.

    private fun showChat(): Pair<String, ActiveChatViewModel> { // Creates one isolated saved chat before displaying its real screen.
        val id = runBlocking { // Runs the suspend local insert on the JUnit worker.
            when (val result = repository.createChat("UI test conversation")) { // Creates real empty Room history without a network operation.
                is ChatCommandResult.Created -> result.chatId // Receives only the persisted local ID.
                else -> error("Could not create isolated device-test conversation") // Reports no credential or raw database details.
            } // Ends local creation handling.
        } // Ends test-only creation.
        val model = own(id, ActiveChatViewModel(id, repository, preferences, network)) // Constructs the real model with explicit isolated dependencies.
        compose.setContent { DroidMentorTheme { ActiveChatScreen(id, {}, model) } } // Tests actual widgets without the production app database or key.
        compose.waitUntil(5_000) { !model.state.value.loading && model.state.value.connected } // Waits for local observation and the injected initial network snapshot.
        return id to model // Returns only local identity and the controlled model for persistence assertions.
    } // Ends actual chat test composition.

    private fun <T : ViewModel> own(key: String, model: T): T { // Records explicit test ownership rather than global app lifetime.
        store.put(key, model) // Lets cleanup cancel the same model used by Compose.
        models.add(model) // Tracks its coroutine job for deterministic shutdown.
        return model // Preserves its concrete type for safe state assertions.
    } // Ends manual test model ownership.

    private class MemorySettings : SettingsRepository { // Holds only generated fictitious preferences in memory.
        val read = MutableStateFlow<SettingsReadResult>(SettingsReadResult.Success("fixture-${UUID.randomUUID()}")) // Starts with no source constant or actual API key.
        var saves = 0 // Counts explicit save bindings without logging values.
        var clears = 0 // Counts explicit clear bindings.
        override val readResult: Flow<SettingsReadResult> = read // Exposes the controlled typed snapshot.
        override val apiKey = read.map { (it as? SettingsReadResult.Success)?.apiKey } // Implements the nullable key contract only for the isolated remote source.
        override suspend fun saveApiKey(value: String): SettingsWriteResult { // Models the repository's nonblank validation without a production file.
            saves += 1 // Records the widget's explicit command.
            if (value.isBlank()) return SettingsWriteResult.EMPTY_KEY // Rejects empty input without altering configuration.
            read.value = SettingsReadResult.Success(value.trim()) // Publishes only this fixture's successful write.
            return SettingsWriteResult.SUCCESS // Returns typed completion rather than the key value.
        } // Ends fake saving.
        override suspend fun clearApiKey(): SettingsWriteResult { // Models selective removal of the fixture value.
            clears += 1 // Records the widget's explicit removal command.
            read.value = SettingsReadResult.Success(null) // Publishes absence without touching any production preferences.
            return SettingsWriteResult.SUCCESS // Returns typed completion.
        } // Ends fake clearing.
    } // Ends generated in-memory configuration.
} // Ends isolated real-screen device tests.
