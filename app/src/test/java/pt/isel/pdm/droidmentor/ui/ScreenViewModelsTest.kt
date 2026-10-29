package pt.isel.pdm.droidmentor.ui // Tests screen behaviour with simple in-memory repository contracts.

import androidx.lifecycle.ViewModel // Types the ViewModels stored by the test owner.
import androidx.lifecycle.ViewModelStore // Clears ViewModel jobs after each isolated case.
import kotlinx.coroutines.CompletableDeferred // Pauses a fake command to test repeated events and cancellation.
import kotlinx.coroutines.Dispatchers // Replaces only the test's Main dispatcher.
import kotlinx.coroutines.ExperimentalCoroutinesApi // Opts into coroutine test dispatcher control.
import kotlinx.coroutines.flow.Flow // Implements repository observations without Android storage.
import kotlinx.coroutines.flow.MutableStateFlow // Publishes controlled local snapshots.
import kotlinx.coroutines.flow.first // Reads a single buffered screen event.
import kotlinx.coroutines.flow.map // Projects fake settings into their nullable key contract.
import kotlinx.coroutines.test.StandardTestDispatcher // Runs ViewModel coroutines on a deterministic scheduler.
import kotlinx.coroutines.test.advanceUntilIdle // Waits for scheduled collector and command work.
import kotlinx.coroutines.test.resetMain // Restores the normal Main dispatcher after each test.
import kotlinx.coroutines.test.runTest // Runs suspend assertions without an Android device.
import kotlinx.coroutines.test.setMain // Supplies the test dispatcher used by viewModelScope.
import org.junit.After // Releases the test owner's ViewModels and dispatcher.
import org.junit.Assert.assertEquals // Compares safe IDs, counts and typed labels.
import org.junit.Assert.assertFalse // Checks guards and privacy without printing key values.
import org.junit.Assert.assertTrue // Checks loading, read errors and fake dependency behaviour.
import org.junit.Before // Creates an isolated owner and scheduler.
import org.junit.Test // Marks screen behaviour cases.
import pt.isel.pdm.droidmentor.data.repository.ChatCommandResult // Supplies typed local fake command acknowledgements.
import pt.isel.pdm.droidmentor.data.repository.ChatReadResult // Supplies typed fake Room snapshots.
import pt.isel.pdm.droidmentor.data.repository.ChatRepository // Implements the replaceable chat contract.
import pt.isel.pdm.droidmentor.data.repository.SettingsReadResult // Supplies typed fake preferences snapshots.
import pt.isel.pdm.droidmentor.data.repository.SettingsRepository // Implements the replaceable settings contract.
import pt.isel.pdm.droidmentor.data.repository.SettingsWriteResult // Supplies typed save and clear outcomes.
import pt.isel.pdm.droidmentor.domain.Chat // Supplies one harmless local conversation.
import pt.isel.pdm.droidmentor.domain.ChatSummary // Types saved history summaries.
import pt.isel.pdm.droidmentor.domain.ConnectivityStatus // Supplies a fake status without registering network callbacks.
import pt.isel.pdm.droidmentor.domain.Message // Supplies persisted message fixtures.
import pt.isel.pdm.droidmentor.domain.MessageErrorCategory // Supplies a saved failure label for retry.
import pt.isel.pdm.droidmentor.domain.MessageStatus // Distinguishes persisted failed and completed turns.
import pt.isel.pdm.droidmentor.domain.Role // Identifies the saved user message.
import pt.isel.pdm.droidmentor.ui.components.UiEvent // Reads typed notices and navigation requests.
import pt.isel.pdm.droidmentor.ui.components.UiNotice // Checks safe feedback labels.
import pt.isel.pdm.droidmentor.ui.screens.activechat.ActiveChatViewModel // Exercises Room-only content and draft/retry handling.
import pt.isel.pdm.droidmentor.ui.screens.chathistory.ChatHistoryViewModel // Exercises saved-history observation and local creation.
import pt.isel.pdm.droidmentor.ui.screens.settings.SettingsViewModel // Exercises key presence and explicit writes.
import java.util.UUID // Generates fictitious credentials in memory rather than source constants.

/** Checks meaningful screen transitions without opening an emulator or performing real IO. */
@OptIn(ExperimentalCoroutinesApi::class) // Controls Main only within these unit tests.
class ScreenViewModelsTest { // Uses no Room file, DataStore file or HTTP client.
    private val dispatcher = StandardTestDispatcher() // Creates a deterministic main scheduler.
    private lateinit var store: ViewModelStore // Owns all test-created ViewModel jobs.

    /** Installs the scheduler and a fresh owner before each test. */
    @Before // Keeps test lifetimes independent.
    fun prepare() { // Sets up only test infrastructure.
        Dispatchers.setMain(dispatcher) // Lets viewModelScope run without a device Main looper.
        store = ViewModelStore() // Creates one test-owned lifetime container.
    } // Ends test setup.

    /** Cancels collectors before restoring the normal Main dispatcher. */
    @After // Prevents background state collectors leaking between cases.
    fun cleanup() { // Releases only resources created by this test.
        store.clear() // Cancels every stored ViewModel's coroutine scope.
        Dispatchers.resetMain() // Restores dispatcher lookup after cancellation.
    } // Ends test cleanup.

    /** Checks a configured key never pre-fills the input or appears in diagnostic state text. */
    @Test // Covers the screen's credential boundary.
    fun configuredKeyIsRepresentedOnlyByPresence() = runTest { // Uses a fictitious value held only in memory.
        val fake = FakeSettings() // Creates a preferences contract without a file.
        val credential = "fixture-${UUID.randomUUID()}" // Generates a fresh fake credential for this test.
        fake.read.value = SettingsReadResult.Success(credential) // Simulates an already configured key.
        val model = own(SettingsViewModel(fake)) // Creates a screen model with explicit fake injection.
        advanceUntilIdle() // Lets the initial settings read complete.
        assertTrue(model.state.value.configured) // Checks only the presence flag is exposed.
        assertTrue(model.state.value.draft.isEmpty()) // Checks the stored key was not copied into the editable field.
        model.beginEdit() // Opens replacement input explicitly before checking draft privacy.
        model.updateDraft(credential) // Simulates newly typed in-memory input.
        assertFalse(model.state.value.toString().contains(credential)) // Checks accidental state diagnostics reveal no key value.
    } // Ends configured-key privacy verification.

    /** Checks a saved key does not require input or another save when Settings opens. */
    @Test // Reproduces the unnecessary re-entry reported by the author.
    fun configuredKeyNeedsNoNewInput() = runTest { // Uses only a generated in-memory preference value.
        val fake = FakeSettings() // Creates an isolated settings contract.
        fake.read.value = SettingsReadResult.Success("fixture-${UUID.randomUUID()}") // Starts with a saved fictitious key.
        val model = own(SettingsViewModel(fake)) // Opens the real settings state logic.
        advanceUntilIdle() // Completes the initial preference read.
        assertTrue(model.state.value.configured) // Confirms the saved key is recognised.
        assertFalse(model.state.value.showKeyInput) // Keeps the empty replacement field hidden.
        model.save() // Simulates a stale save event without a new draft.
        advanceUntilIdle() // Allows any incorrectly started write to run.
        assertEquals(0, fake.saves) // Confirms no empty overwrite or redundant save occurred.
        assertTrue(model.state.value.configured) // Keeps the existing configuration available.
    } // Ends existing-key regression coverage.

    /** Checks cancelling a replacement discards its draft without removing the saved key. */
    @Test // Protects the current configuration while the user changes their mind.
    fun cancelReplacementKeepsTheSavedKey() = runTest { // Does not use a preference file.
        val fake = FakeSettings() // Supplies a controllable saved configuration.
        val original = "fixture-${UUID.randomUUID()}" // Generates the original fictitious value in memory.
        fake.read.value = SettingsReadResult.Success(original) // Represents an existing saved key.
        val model = own(SettingsViewModel(fake)) // Creates the screen owner.
        advanceUntilIdle() // Reads the saved status.
        model.beginEdit() // Opens an empty optional replacement form.
        model.updateDraft("fixture-${UUID.randomUUID()}") // Types a different generated draft.
        model.cancelEdit() // Abandons the draft without a storage command.
        assertTrue(model.state.value.configured) // Keeps the stored status visible.
        assertFalse(model.state.value.showKeyInput) // Returns to the configured summary.
        assertTrue(model.state.value.draft.isEmpty()) // Removes the abandoned draft from memory.
        assertTrue((fake.read.value as SettingsReadResult.Success).apiKey == original) // Compares without printing either value.
        assertEquals(0, fake.saves) // Confirms cancellation did not save.
        assertEquals(0, fake.clears) // Confirms cancellation did not delete the old key.
    } // Ends safe replacement cancellation.

    /** Checks a successful replacement is recognised again by a newly opened screen. */
    @Test // Covers re-entry into Settings after a saved change.
    fun replacementStaysConfiguredWhenSettingsReopens() = runTest { // Shares only the fake persisted contract between owners.
        val fake = FakeSettings() // Supplies preferences that publish completed saves.
        fake.read.value = SettingsReadResult.Success("fixture-${UUID.randomUUID()}") // Starts with a generated saved key.
        val model = own(SettingsViewModel(fake)) // Creates the first screen owner.
        advanceUntilIdle() // Completes its initial read.
        model.beginEdit() // Explicitly requests a replacement.
        val replacement = "fixture-${UUID.randomUUID()}" // Generates a replacement without a source constant.
        model.updateDraft(replacement) // Supplies the new draft.
        model.save() // Commits the replacement through the settings contract.
        advanceUntilIdle() // Waits for acknowledgement and observation.
        assertTrue(model.state.value.configured) // Confirms the replacement is available immediately.
        assertFalse(model.state.value.showKeyInput) // Closes the completed replacement form.
        assertTrue(model.state.value.draft.isEmpty()) // Removes the saved draft from UI memory.
        assertTrue((fake.read.value as SettingsReadResult.Success).apiKey == replacement) // Checks storage without printable credential arguments.
        val reopened = own(SettingsViewModel(fake)) // Replaces the old owner with a new settings screen.
        advanceUntilIdle() // Reads the stored replacement in the new owner.
        assertTrue(reopened.state.value.configured) // Recognises the saved replacement on re-entry.
        assertFalse(reopened.state.value.showKeyInput) // Does not ask for the key again.
    } // Ends replacement persistence at the screen boundary.

    /** Checks a failed replacement leaves the old key usable and the new draft editable. */
    @Test // Protects existing configuration when a storage write fails.
    fun failedReplacementKeepsOriginalKeyAndDraft() = runTest { // Uses a typed fake write failure.
        val fake = FakeSettings() // Supplies isolated saved preferences.
        val original = "fixture-${UUID.randomUUID()}" // Generates an original fictitious key.
        fake.read.value = SettingsReadResult.Success(original) // Starts configured.
        fake.writeResult = SettingsWriteResult.STORAGE_ERROR // Rejects the attempted replacement.
        val model = own(SettingsViewModel(fake)) // Opens the real command logic.
        advanceUntilIdle() // Completes settings observation.
        model.beginEdit() // Starts an explicit replacement.
        val draft = "fixture-${UUID.randomUUID()}" // Generates an unsaved replacement.
        model.updateDraft(draft) // Places it only in the input state.
        model.save() // Attempts the failing write.
        advanceUntilIdle() // Lets failure feedback and cleanup complete.
        assertTrue(model.state.value.configured) // Keeps the previous saved status.
        assertTrue(model.state.value.showKeyInput) // Leaves replacement input available for correction or cancellation.
        assertTrue(model.state.value.draft == draft) // Checks retention without printing the input.
        assertTrue((fake.read.value as SettingsReadResult.Success).apiKey == original) // Confirms the old key was not cleared first.
        assertEquals(UiEvent.Notice(UiNotice.STORAGE_ERROR), model.events.first()) // Reports only the safe storage failure.
    } // Ends failed-replacement regression coverage.

    /** Checks unknown or unreadable settings never ask the user to replace a possibly saved key. */
    @Test // Covers the initial-read flicker and a later read failure.
    fun unreadableSettingsDoNotRequestAnotherKey() = runTest { // Controls when initial observation runs.
        val fake = FakeSettings() // Creates an initially configured contract.
        fake.read.value = SettingsReadResult.Success("fixture-${UUID.randomUUID()}") // Publishes generated test configuration.
        val model = own(SettingsViewModel(fake)) // Has not yet run its scheduled initial read.
        assertTrue(model.state.value.loading) // Distinguishes pending IO from missing configuration.
        assertFalse(model.state.value.showKeyInput) // Keeps setup hidden until the read completes.
        advanceUntilIdle() // Reads the existing key presence.
        fake.read.value = SettingsReadResult.StorageError // Makes the next observation unreadable.
        advanceUntilIdle() // Applies the safe read error.
        assertTrue(model.state.value.configured) // Retains the last known presence rather than claiming deletion.
        assertTrue(model.state.value.storageError) // Exposes the real reason the status cannot be refreshed.
        assertFalse(model.state.value.showKeyInput) // Does not request a replacement for a read error.
        assertFalse(model.state.value.canChangeKey) // Protects configuration while storage is unreadable.
    } // Ends unknown-status regression coverage.

    /** Checks a successful acknowledgement updates presence before a delayed observer catches up. */
    @Test // Prevents the just-saved form from remaining in initial setup.
    fun acknowledgedSaveClosesSetupBeforeObserverEmission() = runTest { // Holds back only fake Flow publication.
        val fake = FakeSettings() // Starts without a saved key.
        fake.publishWrites = false // Simulates a successful save whose observed snapshot is delayed.
        val model = own(SettingsViewModel(fake)) // Uses the actual write acknowledgement handling.
        advanceUntilIdle() // Opens initial setup after the first read.
        model.updateDraft("fixture-${UUID.randomUUID()}") // Types generated fictitious input.
        model.save() // Receives a successful storage acknowledgement.
        advanceUntilIdle() // Completes state and feedback updates.
        assertTrue(model.state.value.configured) // Reflects the completed save without waiting for another emission.
        assertFalse(model.state.value.showKeyInput) // Does not request the just-saved key again.
        assertFalse(model.state.value.saving) // Leaves the summary usable after completion.
    } // Ends acknowledged-save regression coverage.

    /** Checks a failed save retains the draft and reports only a typed local failure. */
    @Test // Protects unsaved input against storage errors.
    fun failedKeySaveKeepsTheDraft() = runTest { // Executes a rejected preferences command.
        val fake = FakeSettings() // Supplies a writable contract with a controlled outcome.
        fake.writeResult = SettingsWriteResult.STORAGE_ERROR // Simulates a DataStore write failure.
        val model = own(SettingsViewModel(fake)) // Injects the fake directly.
        advanceUntilIdle() // Completes the initial read before enabling writes.
        val credential = "fixture-${UUID.randomUUID()}" // Generates an isolated in-memory value.
        model.updateDraft(credential) // Supplies unsaved input.
        model.save() // Requests one explicit save.
        advanceUntilIdle() // Lets the failed command release its guard.
        assertTrue(model.state.value.draft == credential) // Checks retention without printable expected/actual credential arguments.
        assertFalse(model.state.value.saving) // Checks the field becomes editable again.
        assertEquals(UiEvent.Notice(UiNotice.STORAGE_ERROR), model.events.first()) // Checks feedback includes no raw input or exception.
    } // Ends failed-save draft verification.

    /** Checks rapid save taps perform one write and success clears only that command's draft. */
    @Test // Exercises the immediate command guard before coroutine execution.
    fun repeatedSavesUseOneWrite() = runTest { // Holds the fake write until both user events have arrived.
        val fake = FakeSettings() // Creates a controlled preferences fake.
        fake.gate = CompletableDeferred() // Pauses the first storage write.
        val model = own(SettingsViewModel(fake)) // Uses one screen owner for both events.
        advanceUntilIdle() // Completes initial preferences observation.
        model.updateDraft("fixture-${UUID.randomUUID()}") // Supplies a generated nonblank input.
        model.save() // Starts one explicit save.
        model.save() // Repeats the tap before the first coroutine completes.
        advanceUntilIdle() // Runs the single pending repository call up to its gate.
        assertEquals(1, fake.saves) // Checks the repository was invoked once.
        fake.gate?.complete(Unit) // Allows the storage command to finish.
        advanceUntilIdle() // Runs success feedback and cleanup.
        assertTrue(model.state.value.draft.isEmpty()) // Checks successful input was removed from memory.
        assertEquals(UiEvent.Notice(UiNotice.KEY_SAVED), model.events.first()) // Checks a single safe success event.
    } // Ends duplicate-write verification.

    /** Checks new-chat navigation uses the saved ID without fabricating a Room list entry. */
    @Test // Keeps history observation separate from command acknowledgements.
    fun newChatNavigationWaitsForPersistence() = runTest { // Uses a fake local command that returns an already saved ID.
        val fake = FakeChats() // Starts with an empty observed list.
        val model = own(ChatHistoryViewModel(fake)) // Injects only the local repository contract.
        advanceUntilIdle() // Collects the initial empty saved history.
        model.createChat("New conversation") // Requests one local insert through the ViewModel.
        advanceUntilIdle() // Waits for the command acknowledgement.
        assertEquals(UiEvent.OpenChat("saved"), model.events.first()) // Checks navigation uses the acknowledged local ID.
        assertTrue(model.state.value.chats.isEmpty()) // Checks only an actual repository Flow can populate saved history.
        assertFalse(model.state.value.creating) // Checks the creation guard is released.
    } // Ends saved-ID navigation verification.

    /** Checks a database read failure is not displayed as ordinary empty history. */
    @Test // Covers the distinction needed by the history screen's error state.
    fun historyReadFailureHasAnExplicitState() = runTest { // Publishes a typed local read failure.
        val fake = FakeChats() // Supplies a controllable Room-like stream.
        fake.summaries.value = ChatReadResult.StorageError // Simulates an unreadable history query.
        val model = own(ChatHistoryViewModel(fake)) // Creates one isolated screen model.
        advanceUntilIdle() // Lets the read outcome reach state.
        assertTrue(model.state.value.storageError) // Checks the screen can show storage feedback separately.
        assertFalse(model.state.value.loading) // Checks failure finishes initial loading.
    } // Ends history read-failure verification.

    /** Checks a locally settled send clears input but never supplies model content outside Room. */
    @Test // Protects the offline-first UI boundary.
    fun sendAcknowledgementDoesNotInventMessages() = runTest { // Keeps the observed message stream empty even after the fake command settles.
        val fake = FakeChats() // Supplies one saved empty conversation.
        val model = active(fake) // Creates the selected-conversation ViewModel with fake status and settings.
        advanceUntilIdle() // Waits for both initial Room snapshots.
        model.updateDraft("Explain Compose state") // Supplies harmless message text.
        model.send() // Requests an explicit repository operation.
        advanceUntilIdle() // Lets the local acknowledgement settle.
        assertEquals(1, fake.sends) // Checks exactly one repository send was requested.
        assertTrue(model.state.value.draft.isEmpty()) // Checks the locally settled draft was cleared.
        assertTrue(model.state.value.messages.isEmpty()) // Checks the acknowledgement did not bypass Room observation.
    } // Ends Room-only send-content verification.

    /** Checks an offline prerequisite rejection retains the unsaved draft and reports safe feedback. */
    @Test // Covers the repository's fresh check independently of an earlier UI status snapshot.
    fun rejectedSendKeepsItsDraft() = runTest { // Simulates connectivity changing before the repository's preflight check.
        val fake = FakeChats() // Supplies local conversation observation.
        fake.commandResult = ChatCommandResult.Offline // Rejects before any saved optimistic write.
        val model = active(fake) // Starts with a previously connected UI snapshot.
        advanceUntilIdle() // Loads saved history and prerequisite flags.
        model.updateDraft("Keep this draft") // Supplies text that should survive the rejection.
        model.send() // Lets the repository perform its current prerequisite check.
        advanceUntilIdle() // Waits for the typed rejection and cleanup.
        assertEquals("Keep this draft", model.state.value.draft) // Checks no unsent content was discarded.
        assertEquals(UiEvent.Notice(UiNotice.OFFLINE), model.events.first()) // Checks the current rejection is understandable to the screen.
    } // Ends rejected-send retention verification.

    /** Checks a failed saved turn blocks new sends and retry uses its same ID without clearing a newer draft. */
    @Test // Covers the minimal retry semantics required by M2.
    fun retryTargetsTheSavedFailedUserRow() = runTest { // Publishes a persisted failed turn rather than a network event.
        val fake = FakeChats() // Supplies one saved conversation.
        fake.messages.value = ChatReadResult.Success(listOf(Message("failed-row", "saved", Role.USER, "Saved question", 0, MessageStatus.FAILED, 1, MessageErrorCategory.TIMEOUT))) // Supplies the error category from the local message stream.
        val model = active(fake) // Uses the saved row to derive retry availability.
        advanceUntilIdle() // Publishes the combined Room snapshot.
        model.updateDraft("A later draft") // Leaves a separately editable future input in memory.
        model.send() // Attempts a new turn while the saved failure remains unresolved.
        assertEquals(0, fake.sends) // Checks the ViewModel did not bypass the failed-row guard.
        model.retry() // Requests the repository's same-row retry operation.
        advanceUntilIdle() // Runs the retry acknowledgement.
        assertEquals("failed-row", fake.retriedId) // Checks the selected local row was reused.
        assertEquals("A later draft", model.state.value.draft) // Checks retry did not consume a new draft.
    } // Ends saved-row retry verification.

    /** Checks losing validated connectivity blocks send and retry while retaining an editable draft. */
    @Test // Covers the UI gate independently of the repository's fresh preflight check.
    fun offlineStatusBlocksCommandsButKeepsDraftEditing() = runTest { // Changes a controlled status stream after initial loading.
        val fake = FakeChats() // Supplies locally readable history.
        val settings = FakeSettings() // Supplies a configured in-memory preferences contract.
        settings.read.value = SettingsReadResult.Success("fixture-${UUID.randomUUID()}") // Generates a fictitious value without a file.
        val network = MutableStateFlow(ConnectivityStatus.CONNECTED) // Starts with a validated UI status.
        val model = own(ActiveChatViewModel("saved", fake, settings, network)) // Injects the controlled status stream by hand.
        advanceUntilIdle() // Loads the initial snapshots and prerequisites.
        network.value = ConnectivityStatus.DISCONNECTED // Simulates losing validation before the next event.
        advanceUntilIdle() // Applies the disconnected status to immutable state.
        model.updateDraft("Editable offline draft") // Keeps typing available without a connection.
        model.send() // Attempts the guarded event while offline.
        assertFalse(model.state.value.canSend) // Checks the screen's Send action can be disabled.
        assertEquals(0, fake.sends) // Checks the ViewModel never invoked the repository.
        fake.messages.value = ChatReadResult.Success(listOf(Message("failed-row", "saved", Role.USER, "Saved question", 0, MessageStatus.FAILED, 1, MessageErrorCategory.NETWORK_UNAVAILABLE))) // Publishes locally saved failure content while offline.
        advanceUntilIdle() // Applies the saved failure without needing an API result.
        model.retry() // Attempts the same-row event while prerequisites remain unavailable.
        assertFalse(model.state.value.canRetry) // Checks the visible retry action shares the gate.
        assertTrue(fake.retriedId == null) // Checks no retry repository command was started.
        assertEquals("Editable offline draft", model.state.value.draft) // Checks saved history changes did not discard unsent input.
    } // Ends offline command gating verification.

    /** Checks clearing the configured key disables new sends without deleting saved history or a draft. */
    @Test // Covers preference changes observed after the conversation screen was opened.
    fun clearingTheKeyBlocksNewInteractions() = runTest { // Changes presence while connectivity remains validated.
        val fake = FakeChats() // Supplies saved conversation metadata.
        val settings = FakeSettings() // Supplies a controllable preference stream.
        settings.read.value = SettingsReadResult.Success("fixture-${UUID.randomUUID()}") // Starts with generated in-memory configuration.
        val model = own(ActiveChatViewModel("saved", fake, settings, MutableStateFlow(ConnectivityStatus.CONNECTED))) // Creates one selected-conversation owner.
        advanceUntilIdle() // Loads the configured state.
        model.updateDraft("Keep this unsent text") // Supplies an editable draft.
        assertTrue(model.state.value.canSend) // Checks the initially available interaction.
        settings.read.value = SettingsReadResult.Success(null) // Simulates an explicit successful key clear.
        advanceUntilIdle() // Applies absence to the UI prerequisite gate.
        model.send() // Requests a send after configuration was removed.
        assertFalse(model.state.value.canSend) // Checks the screen and event share current presence status.
        assertEquals(0, fake.sends) // Checks the repository was not invoked with absent prerequisites.
        assertEquals("Keep this unsent text", model.state.value.draft) // Checks the draft remains available after reconfiguration.
    } // Ends missing-key command gating verification.

    /** Checks unreadable preferences are reported separately and block sends conservatively. */
    @Test // Covers a failed preference stream while local Room history remains readable.
    fun settingsReadFailureBlocksSending() = runTest { // Publishes the existing typed settings error.
        val fake = FakeChats() // Keeps the saved conversation available.
        val settings = FakeSettings() // Supplies an initially unconfigured preferences stream.
        settings.read.value = SettingsReadResult.StorageError // Simulates a read failure without raw exception details.
        val model = own(ActiveChatViewModel("saved", fake, settings, MutableStateFlow(ConnectivityStatus.CONNECTED))) // Keeps connectivity independent of preferences.
        advanceUntilIdle() // Applies all initial local snapshots.
        model.updateDraft("Retain the draft") // Allows typing despite unavailable key storage.
        model.send() // Attempts a send through the conservative UI gate.
        assertTrue(model.state.value.settingsError) // Checks the screen can explain the actual prerequisite failure.
        assertFalse(model.state.value.canSend) // Checks unreadable settings are not treated as valid configuration.
        assertEquals(0, fake.sends) // Checks no interaction was started.
    } // Ends unreadable-settings gating verification.

    private fun active(fake: FakeChats): ActiveChatViewModel { // Supplies simple fake dependencies for active-chat cases.
        val settings = FakeSettings() // Creates a preferences contract without a file.
        settings.read.value = SettingsReadResult.Success("fixture-${UUID.randomUUID()}") // Represents a configured key without a source constant.
        return own(ActiveChatViewModel("saved", fake, settings, MutableStateFlow(ConnectivityStatus.CONNECTED))) // Provides only the local ID and contract implementations.
    } // Ends selected-chat test construction.

    private fun <T : ViewModel> own(model: T): T { // Makes the JUnit cleanup own this model's coroutine jobs.
        store.put("model", model) // Stores the single model used by each case.
        return model // Returns its concrete type for assertions.
    } // Ends test lifetime ownership.

    private class FakeSettings : SettingsRepository { // Implements only in-memory preference behaviour.
        val read = MutableStateFlow<SettingsReadResult>(SettingsReadResult.Success(null)) // Starts without a configured key.
        var writeResult = SettingsWriteResult.SUCCESS // Controls whether the requested write succeeds.
        var saves = 0 // Counts actual save calls without retaining input for diagnostics.
        var clears = 0 // Counts selective removals without inspecting any real preference file.
        var publishWrites = true // Lets one regression case delay observed write publication.
        var gate: CompletableDeferred<Unit>? = null // Optionally pauses a write to test duplicate events.
        override val readResult: Flow<SettingsReadResult> = read // Exposes the controllable typed stream.
        override val apiKey = read.map { (it as? SettingsReadResult.Success)?.apiKey } // Implements the repository's nullable key projection.
        override suspend fun saveApiKey(value: String): SettingsWriteResult { // Simulates a write without disk IO.
            saves += 1 // Records one repository invocation.
            gate?.await() // Waits only when a test explicitly installs a gate.
            if (writeResult == SettingsWriteResult.SUCCESS && publishWrites) read.value = SettingsReadResult.Success(value) // Publishes successful preference changes unless the observer is deliberately delayed.
            return writeResult // Returns only the typed write outcome.
        } // Ends fake saving.
        override suspend fun clearApiKey(): SettingsWriteResult { // Simulates selective credential removal.
            clears += 1 // Counts removal separately from replacement saves.
            if (writeResult == SettingsWriteResult.SUCCESS) read.value = SettingsReadResult.Success(null) // Publishes absence after a successful clear.
            return writeResult // Preserves a controlled failure when requested by the test.
        } // Ends fake clearing.
    } // Ends the preferences fake.

    private class FakeChats : ChatRepository { // Supplies controlled local snapshots without an API client.
        val summaries = MutableStateFlow<ChatReadResult<List<ChatSummary>>>(ChatReadResult.Success(emptyList())) // Starts with empty saved history.
        val chat = MutableStateFlow<ChatReadResult<Chat?>>(ChatReadResult.Success(Chat("saved", "Saved conversation", 1, 1))) // Supplies one local chat selected by tests.
        val messages = MutableStateFlow<ChatReadResult<List<Message>>>(ChatReadResult.Success(emptyList())) // Lets tests publish actual local message state separately from acknowledgements.
        var commandResult: ChatCommandResult = ChatCommandResult.Stored // Controls send and retry acknowledgement outcomes.
        var sends = 0 // Counts requested send commands.
        var retriedId: String? = null // Records only the harmless local retry ID.
        override fun observeChats(): Flow<ChatReadResult<List<ChatSummary>>> = summaries // Supplies saved summary snapshots.
        override fun observeChat(chatId: String): Flow<ChatReadResult<Chat?>> = chat // Supplies saved conversation metadata.
        override fun observeMessages(chatId: String): Flow<ChatReadResult<List<Message>>> = messages // Supplies saved messages only.
        override suspend fun createChat(title: String): ChatCommandResult = ChatCommandResult.Created("saved") // Acknowledges a local ID without fabricating a list emission.
        override suspend fun renameChat(chatId: String, title: String): ChatCommandResult = ChatCommandResult.Stored // Leaves unused local operations harmless.
        override suspend fun deleteChat(chatId: String): ChatCommandResult = ChatCommandResult.Stored // Performs no real database deletion.
        override suspend fun sendMessage(chatId: String, text: String): ChatCommandResult { // Records a command without synthesising a model response.
            sends += 1 // Counts the ViewModel's explicit operation.
            return commandResult // Returns the controlled local acknowledgement.
        } // Ends the fake send operation.
        override suspend fun retryMessage(chatId: String, messageId: String): ChatCommandResult { // Records the selected persisted row.
            retriedId = messageId // Captures only the local ID needed by the retry assertion.
            return commandResult // Returns the controlled local retry acknowledgement.
        } // Ends the fake retry operation.
    } // Ends the Room-like repository fake.
} // Ends screen ViewModel unit verification.
