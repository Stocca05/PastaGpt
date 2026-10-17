package pt.isel.pdm.droidmentor.data.repository // Groups tests of settings error handling.

import androidx.datastore.core.DataStore // Allows a small failing storage fake to be injected.
import androidx.datastore.preferences.core.Preferences // Supplies the preference snapshot type.
import java.io.IOException // Simulates a storage IO failure without credential text.
import java.util.UUID // Creates fictitious values in memory instead of committing credentials.
import kotlinx.coroutines.CancellationException // Simulates cancellation of a storage operation.
import kotlinx.coroutines.flow.Flow // Defines the fake read stream.
import kotlinx.coroutines.flow.first // Reads one result for the assertion.
import kotlinx.coroutines.flow.flow // Creates a stream that fails when collected.
import kotlinx.coroutines.test.runTest // Runs suspend repository methods in a coroutine test.
import org.junit.Assert.assertEquals // Compares safe enum outcomes rather than credential values.
import org.junit.Assert.assertNull // Checks that no value is supplied on a read failure.
import org.junit.Assert.assertTrue // Checks conditions without printing stored text on failure.
import org.junit.Test // Marks each test case.

/** Checks typed storage failures and cancellation without using real credentials. */
class SettingsRepositoryFailureTest { // Groups the repository error-path tests.
    /** Checks that an unreadable file is reported distinctly from an absent key. */
    @Test // Exercises a failed DataStore read.
    fun readFailureIsTypedAndSuppliesNoKey() = runTest { // Runs the suspend Flow reads.
        val repository = DataStoreSettingsRepository(FailingDataStore(IOException("Storage unavailable"))) // Injects a read failure.
        assertTrue(repository.readResult.first() is SettingsReadResult.StorageError) // Checks the explicit failure outcome.
        assertNull(repository.apiKey.first()) // Checks callers cannot use a value from a failed read.
    } // Ends the read-failure test.

    /** Checks that save and clear operations return storage failures instead of throwing. */
    @Test // Exercises IO errors in the preference edit path.
    fun writeFailuresAreTyped() = runTest { // Calls the suspend write methods.
        val repository = DataStoreSettingsRepository(FailingDataStore(IOException("Storage unavailable"))) // Injects failed edits.
        val dummy = UUID.randomUUID().toString() // Creates an invalid-for-Gemini test value only in memory.
        assertEquals(SettingsWriteResult.STORAGE_ERROR, repository.saveApiKey(dummy)) // Checks the safe save outcome.
        assertEquals(SettingsWriteResult.STORAGE_ERROR, repository.clearApiKey()) // Checks the safe deletion outcome.
    } // Ends the IO write-failure test.

    /** Checks unexpected storage exceptions do not expose their details to callers. */
    @Test // Exercises a non-IO storage failure.
    fun unexpectedWriteFailureIsTyped() = runTest { // Calls the repository on a coroutine test scope.
        val repository = DataStoreSettingsRepository(FailingDataStore(IllegalStateException("Store unavailable"))) // Injects an unexpected failure.
        assertEquals(SettingsWriteResult.STORAGE_ERROR, repository.saveApiKey(UUID.randomUUID().toString())) // Checks the save is still typed.
        assertEquals(SettingsWriteResult.STORAGE_ERROR, repository.clearApiKey()) // Checks clearing uses the same protection.
    } // Ends the unexpected-failure test.

    /** Checks blank input is rejected before attempting any edit. */
    @Test // Exercises local nonempty validation.
    fun blankInputDoesNotAttemptAWrite() = runTest { // Calls the save method with whitespace only.
        val store = FailingDataStore(IOException("Storage unavailable")) // Tracks calls while rejecting any attempted edit.
        val repository = DataStoreSettingsRepository(store) // Uses the tracked fake.
        assertEquals(SettingsWriteResult.EMPTY_KEY, repository.saveApiKey(" \n\t ")) // Checks the input outcome is not a storage failure.
        assertEquals(0, store.updateCalls) // Checks validation stopped before editing storage.
    } // Ends the blank-input test.

    /** Checks cancellation remains normal coroutine control flow for reads and writes. */
    @Test // Ensures cancelled work is not turned into a misleading storage error.
    fun cancellationIsNotConvertedToStorageError() = runTest { // Calls each operation with a cancelled fake.
        val repository = DataStoreSettingsRepository(FailingDataStore(CancellationException("Cancelled"))) // Injects cancellation only.
        expectCancellation { repository.readResult.first() } // Checks a cancelled reader stops.
        expectCancellation { repository.saveApiKey(UUID.randomUUID().toString()) } // Checks a cancelled save stops.
        expectCancellation { repository.clearApiKey() } // Checks a cancelled clear stops.
    } // Ends the cancellation test.

    /** Checks the successful read object's text representation never contains its value. */
    @Test // Checks the diagnostic representation with a generated fictitious value.
    fun readResultTextHidesItsValue() { // Does not write any value to a file.
        val dummy = UUID.randomUUID().toString() // Creates a test-only in-memory value.
        val result = SettingsReadResult.Success(dummy) // Builds the result that will be passed to callers.
        assertTrue("Read-result text must hide its value", !result.toString().contains(dummy)) // Uses a boolean assertion to avoid echoing the value.
    } // Ends the safe-diagnostic test.

    private suspend fun expectCancellation(action: suspend () -> Unit) { // Checks that cancellation is propagated without exposing data.
        var cancelled = false // Records whether the operation stopped by cancellation.
        try { // Calls the selected read or write.
            action() // Executes the suspend operation.
        } catch (error: CancellationException) { // Catches the expected control-flow exception in test code.
            cancelled = true // Records that cancellation was preserved.
        } // Ends the expected-cancellation handling.
        assertTrue("Cancellation must propagate", cancelled) // Checks the operation did not report success or storage failure.
    } // Ends the cancellation helper.

    private class FailingDataStore(private val failure: Exception) : DataStore<Preferences> { // Provides only the failure behaviour needed by these tests.
        /** Fails when the repository attempts to collect preferences. */
        override val data: Flow<Preferences> = flow { // Creates a cold read stream.
            throw failure // Uses a generic test exception without including input values.
        } // Ends the fake read stream.

        var updateCalls = 0 // Counts edits so blank-input tests can check no write was attempted.
            private set // Lets only this fake change its counter.

        /** Rejects every update using the configured generic test exception. */
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences { // Implements the DataStore edit contract.
            updateCalls += 1 // Records that a write was requested.
            throw failure // Rejects the write before running its transformation.
        } // Ends the failing update method.
    } // Ends the storage fake.
} // Ends the repository failure test class.
