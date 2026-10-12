package pt.isel.pdm.droidmentor.data.repository // Groups device tests of Preferences DataStore storage.

import androidx.datastore.core.DataStore // Holds the isolated store used by each test.
import androidx.datastore.preferences.core.PreferenceDataStoreFactory // Creates a real Preferences DataStore for a test file.
import androidx.datastore.preferences.core.Preferences // Supplies the immutable snapshot type.
import androidx.datastore.preferences.core.edit // Seeds an unrelated test preference without touching real app settings.
import androidx.datastore.preferences.core.stringPreferencesKey // Names that unrelated preference slot.
import androidx.test.ext.junit.runners.AndroidJUnit4 // Runs the tests on Android.
import androidx.test.platform.app.InstrumentationRegistry // Provides the app's private cache directory.
import java.io.File // Identifies only the preferences file created by this test.
import java.util.UUID // Generates test-only file names and fictitious values at runtime.
import kotlinx.coroutines.CompletableDeferred // Signals that an observer has received a snapshot.
import kotlinx.coroutines.CoroutineScope // Owns the test store's background work.
import kotlinx.coroutines.Dispatchers // Places actual preference IO on the IO dispatcher.
import kotlinx.coroutines.Job // Holds the lifetime of the current test store.
import kotlinx.coroutines.SupervisorJob // Keeps the storage scope separate from the test method's scope.
import kotlinx.coroutines.async // Collects Flow updates while the test writes preferences.
import kotlinx.coroutines.cancelAndJoin // Releases the old store before reopening its file.
import kotlinx.coroutines.flow.first // Reads one preference or repository result.
import kotlinx.coroutines.flow.onEach // Signals when initial and saved snapshots have arrived.
import kotlinx.coroutines.flow.take // Stops the observer after the required updates.
import kotlinx.coroutines.flow.toList // Collects those updates without printing their values.
import kotlinx.coroutines.runBlocking // Allows suspend storage calls in test methods only.
import kotlinx.coroutines.withTimeout // Bounds waits for expected storage emissions.
import org.junit.After // Marks cleanup after each test.
import org.junit.Assert.assertEquals // Compares only safe result enums and counts.
import org.junit.Assert.assertNull // Checks that no credential is supplied when absent or unreadable.
import org.junit.Assert.assertSame // Checks the production provider reuses its instance.
import org.junit.Assert.assertTrue // Checks values using boolean conditions so failed assertions do not echo them.
import org.junit.Before // Marks setup before each test.
import org.junit.Test // Marks each storage test case.
import org.junit.runner.RunWith // Selects the Android JUnit runner.
import pt.isel.pdm.droidmentor.data.preferences.ApiKeyDataStore // Provides the production singleton store.

/** Checks real preferences persistence using generated fictitious values and isolated files. */
@RunWith(AndroidJUnit4::class) // Runs this class on an Android device.
class SettingsRepositoryStorageTest { // Groups the DataStore persistence checks.
    private lateinit var file: File // Holds only the test's unique preferences file.
    private lateinit var job: Job // Owns the active store instance for that file.
    private lateinit var store: DataStore<Preferences> // Holds the current real test store.
    private lateinit var repository: SettingsRepository // Holds the production repository using that store.

    /** Creates one store instance for a unique test-only file. */
    @Before // Runs setup before each test.
    fun createStore() { // Creates an isolated preferences file location without any real key.
        val context = InstrumentationRegistry.getInstrumentation().targetContext // Uses the app's cache location for test cleanup.
        file = File(context.cacheDir, "droidmentor-key-test-${UUID.randomUUID()}.preferences_pb") // Avoids the production credentials file.
        openStore() // Starts one DataStore scope for this file.
    } // Ends test setup.

    /** Cancels the store before deleting only the file created by this test. */
    @After // Runs cleanup after successful or failed assertions.
    fun closeStore() { // Releases the file ownership before removing its temporary data.
        runBlocking { job.cancelAndJoin() } // Waits for background test-store work to finish.
        file.delete() // Removes only this generated test file, never the production file.
    } // Ends test cleanup.

    /** Checks an empty store is read successfully and differs from a storage error. */
    @Test // Exercises the unconfigured-key case using a real store.
    fun emptyStoreIsSuccessfullyUnconfigured() = runBlocking { // Calls the suspend Flow reads.
        val result = repository.readResult.first() // Reads the initial outcome.
        assertTrue(result is SettingsReadResult.Success && result.apiKey == null) // Checks absence without reporting an error.
        assertNull(repository.apiKey.first()) // Checks the required key Flow also supplies null.
    } // Ends the empty-store test.

    /** Checks a saved fictitious value survives closing and reopening the store instance. */
    @Test // Exercises actual Preferences DataStore file persistence.
    fun savedValueSurvivesReopening() = runBlocking { // Performs writes and scope shutdown on the test thread.
        val dummy = UUID.randomUUID().toString() // Generates a value that is not a real Gemini credential.
        assertEquals(SettingsWriteResult.SUCCESS, repository.saveApiKey(dummy)) // Waits for the committed preference update.
        job.cancelAndJoin() // Releases the first DataStore instance before opening another for the same file.
        openStore() // Recreates the store using the unchanged test file.
        assertTrue("Saved value should survive reopening", repository.apiKey.first() == dummy) // Compares in memory without printing the value.
    } // Ends the reopen-persistence test.

    /** Checks trimming, replacement, blank-input rejection and selective key removal. */
    @Test // Exercises edits without deleting an unrelated preference.
    fun replacementAndClearPreserveOtherPreferences() = runBlocking { // Calls atomic preference edits.
        val first = UUID.randomUUID().toString() // Creates the first fictitious in-memory value.
        val second = UUID.randomUUID().toString() // Creates a different replacement value.
        assertEquals(SettingsWriteResult.SUCCESS, repository.saveApiKey(first)) // Saves the first value through the repository.
        assertEquals(SettingsWriteResult.SUCCESS, repository.saveApiKey("  $second  ")) // Checks surrounding whitespace is handled.
        assertTrue("Replacement should be trimmed", repository.apiKey.first() == second) // Checks only the intended replacement was stored.
        assertEquals(SettingsWriteResult.EMPTY_KEY, repository.saveApiKey(" \t ")) // Rejects a blank replacement.
        assertTrue("Blank input must preserve the previous value", repository.apiKey.first() == second) // Checks rejection did not erase storage.
        val note = stringPreferencesKey("test_note") // Names an unrelated test preference, not a credential.
        store.edit { it[note] = "keep" } // Adds metadata used to check selective deletion.
        assertEquals(SettingsWriteResult.SUCCESS, repository.clearApiKey()) // Removes only the repository's key slot.
        assertNull(repository.apiKey.first()) // Checks no credential remains available.
        assertTrue("Other preferences must remain", store.data.first()[note] == "keep") // Checks the edit did not clear the file.
        assertEquals(SettingsWriteResult.SUCCESS, repository.clearApiKey()) // Checks clearing an already absent value is harmless.
        job.cancelAndJoin() // Releases the store after clearing.
        openStore() // Reopens the file to check deletion was persisted.
        assertNull(repository.apiKey.first()) // Checks the removed value does not return after reopening.
    } // Ends the preference-edit test.

    /** Checks the key Flow emits absence, a saved value and absence after clearing. */
    @Test // Exercises observation with no log or assertion that prints credential text.
    fun keyFlowEmitsSaveAndClear() = runBlocking { // Provides a scope for the observer and writes.
        val dummy = UUID.randomUUID().toString() // Creates one fictitious value in memory.
        val ready = CompletableDeferred<Unit>() // Signals that the initial null value was observed.
        val saved = CompletableDeferred<Unit>() // Signals that the saved value was observed before clearing.
        val observer = async { // Collects the production key Flow before any write.
            withTimeout(5000L) { // Prevents a broken observer from leaving the test waiting forever.
                repository.apiKey.onEach { value -> // Checks emissions only in memory.
                    ready.complete(Unit) // Signals the first snapshot is available.
                    if (value == dummy) saved.complete(Unit) // Signals the value was observed without printing it.
                }.take(3).toList() // Collects just the three expected changes.
            } // Ends the observer timeout.
        } // Ends observer setup.
        withTimeout(5000L) { ready.await() } // Waits until absence has been observed.
        assertEquals(SettingsWriteResult.SUCCESS, repository.saveApiKey(dummy)) // Saves the generated test value.
        withTimeout(5000L) { saved.await() } // Waits until the save emission arrives before clearing.
        assertEquals(SettingsWriteResult.SUCCESS, repository.clearApiKey()) // Removes the stored value.
        val values = observer.await() // Waits for the final absence emission.
        assertEquals(3, values.size) // Compares only a safe count.
        assertTrue("Save and clear must be observed", values[0] == null && values[1] == dummy && values[2] == null) // Checks the sequence without echoing values.
    } // Ends the observable-update test.

    /** Checks a corrupt test file reports failure without being silently replaced or cleared. */
    @Test // Exercises the real serializer failure path with non-credential bytes.
    fun corruptStoreIsReportedWithoutOverwritingIt() = runBlocking { // Controls the test store's lifetime.
        job.cancelAndJoin() // Releases the unused store before modifying its isolated file.
        val broken = byteArrayOf(0xFF.toByte()) // Creates an invalid truncated preferences record, not a key.
        file.writeBytes(broken) // Writes corruption only to this test file before DataStore opens it.
        openStore() // Reopens the file with the default non-destructive configuration.
        assertTrue(repository.readResult.first() is SettingsReadResult.StorageError) // Checks a typed failure instead of an empty successful read.
        assertNull(repository.apiKey.first()) // Checks callers receive no value from unreadable storage.
        assertTrue("Corrupt storage must remain untouched", file.readBytes().contentEquals(broken)) // Checks no replacement handler wiped the file.
    } // Ends the corrupt-file test.

    /** Checks the production provider reuses one instance without reading or changing its values. */
    @Test // Checks the single-instance DataStore rule.
    fun productionProviderReusesOneStore() { // Does not collect or edit the actual credentials file.
        val context = InstrumentationRegistry.getInstrumentation().targetContext // Uses the app's context.
        assertSame(ApiKeyDataStore.get(context), ApiKeyDataStore.get(context.applicationContext)) // Checks both lookups return the same owner.
    } // Ends the provider-instance test.

    private fun openStore() { // Creates one live store after any previous scope has been stopped.
        job = SupervisorJob() // Gives this test instance a cancellable background lifetime.
        store = PreferenceDataStoreFactory.create( // Uses the real Preferences serializer and storage implementation.
            scope = CoroutineScope(Dispatchers.IO + job), // Keeps disk work off Android's main thread.
            produceFile = { file }, // Supplies only the unique test file chosen during setup.
        ) // Ends creation of this store instance.
        repository = DataStoreSettingsRepository(store) // Injects the real isolated store into the production repository.
    } // Ends the test-store helper.
} // Ends the settings storage test class.
