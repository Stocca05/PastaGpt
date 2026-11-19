package pt.isel.pdm.droidmentor.data.repository // Tests isolated DataStore persistence for M3-06 active session restoration.

import androidx.datastore.core.DataStore // Holds the isolated test preference store.
import androidx.datastore.preferences.core.PreferenceDataStoreFactory // Creates a file-backed Preferences DataStore for tests.
import androidx.datastore.preferences.core.Preferences // Types the stored preference snapshots.
import androidx.test.ext.junit.runners.AndroidJUnit4 // Runs against Android DataStore.
import androidx.test.platform.app.InstrumentationRegistry // Supplies the private test cache directory.
import java.io.File // Points to the unique test-only preferences file.
import java.util.UUID // Creates isolated file names and chat IDs.
import kotlinx.coroutines.CoroutineScope // Owns background DataStore IO in the test.
import kotlinx.coroutines.Dispatchers // Runs file IO off the main thread.
import kotlinx.coroutines.Job // Controls DataStore scope shutdown before reopening or deleting.
import kotlinx.coroutines.SupervisorJob // Isolates store lifecycle per openStore call.
import kotlinx.coroutines.cancelAndJoin // Closes one store cleanly before reopening the file.
import kotlinx.coroutines.flow.first // Reads emitted active session values.
import kotlinx.coroutines.runBlocking // Bridges suspend calls in Android JUnit4 tests.
import org.junit.After // Cleans up test storage after every case.
import org.junit.Assert.assertEquals // Compares restored chat IDs.
import org.junit.Assert.assertNull // Checks cleared and initial absent states.
import org.junit.Before // Creates an isolated store file before every case.
import org.junit.Test // Marks test methods.
import org.junit.runner.RunWith // Selects AndroidJUnit4 runner.

/** Verifies M3-06 active-chat persistence across DataStore reopenings and explicit clear. */
@RunWith(AndroidJUnit4::class) // Runs on Android with real Preferences DataStore.
class SessionRepositoryTest { // Never touches production droidmentor_session or droidmentor_credentials.
    private lateinit var file: File // Holds only this test's temporary preferences file.
    private lateinit var job: Job // Controls the active DataStore coroutine scope.
    private lateinit var store: DataStore<Preferences> // Holds the open test store.
    private lateinit var repository: SessionRepository // Tests the DataStoreSessionRepository implementation.

    /** Creates a fresh isolated preference file for each test. */
    @Before // Runs before every test method.
    fun setup() { // Prepares clean session storage.
        val context = InstrumentationRegistry.getInstrumentation().targetContext // Uses test cache directory.
        file = File(context.cacheDir, "droidmentor-session-test-${UUID.randomUUID()}.preferences_pb") // Avoids production files.
        openStore() // Opens the first DataStore instance.
    } // Ends test setup.

    /** Closes DataStore and removes the temporary test file. */
    @After // Runs after every test method.
    fun teardown() { // Releases file locks and deletes test file.
        runBlocking { job.cancelAndJoin() } // Stops background DataStore writers first.
        file.delete() // Deletes only this test's file.
    } // Ends test cleanup.

    /** Checks an empty session store starts with no active chat. */
    @Test // Verifies default Title startup state.
    fun emptySessionStartsAsNull() = runBlocking { // Calls suspend read.
        assertNull(repository.readActiveChatId()) // Expects no restored chat initially.
        assertNull(repository.activeChatId.first()) // Expects Flow to emit null too.
    } // Ends empty-session verification.

    /** Checks a saved active chat ID survives closing and reopening DataStore. */
    @Test // Verifies process-termination persistence semantics.
    fun activeChatSurvivesStoreReopening() = runBlocking { // Simulates app restart after process death.
        repository.setActiveChatId("chat-42") // Saves active conversation on entry.
        assertEquals("chat-42", repository.readActiveChatId()) // Confirms immediate read.
        job.cancelAndJoin() // Closes the first DataStore instance.
        openStore() // Reopens the same file as a new app process would.
        assertEquals("chat-42", repository.readActiveChatId()) // Confirms active chat survived restart.
    } // Ends reopening persistence test.

    /** Checks blank IDs are ignored and explicit clear removes the saved session. */
    @Test // Verifies explicit-exit cleanup and blank guard.
    fun explicitClearRemovesActiveChatAndBlankIsIgnored() = runBlocking { // Tests set, blank and clear.
        repository.setActiveChatId("chat-99") // Saves active chat.
        repository.setActiveChatId("   ") // Attempts saving an unusable blank ID.
        assertEquals("chat-99", repository.readActiveChatId()) // Confirms blank input did not overwrite valid state.
        repository.clearActiveChatId() // Simulates explicit Back exit from Active Chat.
        assertNull(repository.readActiveChatId()) // Confirms session is cleared.
        job.cancelAndJoin() // Closes the store.
        openStore() // Reopens the file.
        assertNull(repository.readActiveChatId()) // Confirms clearing persisted across restart.
    } // Ends explicit-clear verification.

    private fun openStore() { // Opens a DataStore backed by this test's temporary file.
        val supervisor = SupervisorJob() // Tracks this instance's IO job.
        job = supervisor // Stores reference for later shutdown.
        store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + supervisor), produceFile = { file }) // Builds isolated store.
        repository = DataStoreSessionRepository(store) // Wires the production repository class.
    } // Ends isolated store helper.
} // Ends session repository tests.
