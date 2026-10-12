package pt.isel.pdm.droidmentor // Verifies the manifest and app-owned manual dependency graph.

import android.content.ContextWrapper // Checks locator lookup through wrapped contexts rather than activity references.
import androidx.test.ext.junit.runners.AndroidJUnit4 // Uses the real application instantiated from the installed manifest.
import androidx.test.platform.app.InstrumentationRegistry // Provides the target application's context.
import io.ktor.client.engine.okhttp.OkHttpEngine // Identifies OkHttp through its allowed Ktor engine type.
import kotlinx.coroutines.Dispatchers // Exercises concurrent getters from worker threads.
import kotlinx.coroutines.async // Requests the same lazy graph concurrently.
import kotlinx.coroutines.awaitAll // Waits for every competing first access to finish.
import kotlinx.coroutines.flow.first // Reads a persisted repository snapshot for the graph binding test.
import kotlinx.coroutines.runBlocking // Bridges JUnit to suspend methods only in test code.
import kotlinx.coroutines.withTimeout // Bounds Room observation and concurrent graph construction.
import org.junit.Assert.assertEquals // Compares harmless title and database-name metadata.
import org.junit.Assert.assertNull // Checks the test-owned chat was removed after the local round trip.
import org.junit.Assert.assertSame // Checks shared resource identity without printing credential values.
import org.junit.Assert.assertTrue // Checks manifest selection and supported engine types.
import org.junit.Test // Marks the manual graph verification cases.
import org.junit.runner.RunWith // Selects the Android test runner.
import pt.isel.pdm.droidmentor.data.local.DroidMentorDatabase // Supplies the stable production database name.
import pt.isel.pdm.droidmentor.data.preferences.ApiKeyDataStore // Checks the app shares the dedicated provider's existing instance.
import pt.isel.pdm.droidmentor.data.repository.ChatCommandResult // Reads local command acknowledgements instead of any API result.
import pt.isel.pdm.droidmentor.data.repository.ChatReadResult // Reads only persisted domain values from Room.

/** Checks actual application registration, context lookup, resource sharing and local repository wiring. */
@RunWith(AndroidJUnit4::class) // Executes against the target APK's actual application graph.
class DroidMentorApplicationTest { // Makes no API call and never reads or changes a configured credential value.
    private val context = InstrumentationRegistry.getInstrumentation().targetContext // Gets the target app rather than the instrumentation app.

    /** Checks Android instantiated the required application class from the manifest. */
    @Test // Detects a missing or incorrectly packaged android:name declaration.
    fun manifestCreatesTheManualServiceLocator() { // Reads the application object already created by Android.
        assertTrue(context.applicationContext is DroidMentorApplication) // Checks the real runtime class rather than a manually constructed test application.
        assertSame(context.applicationContext, context.app) // Checks the public accessor resolves that same locator.
    } // Ends manifest application verification.

    /** Checks nested context wrappers resolve the same application without retaining a wrapped owner. */
    @Test // Covers the accessor used by activities and later ViewModel factories.
    fun wrappedContextsResolveTheSameApplication() { // Wraps the app context without starting any new component.
        val wrapper = ContextWrapper(ContextWrapper(context)) // Creates nested context delegation similar to UI wrappers.
        assertSame(context.app, wrapper.app) // Checks lookup follows applicationContext rather than casting the wrapper itself.
        assertSame(context.app, context.applicationContext.app) // Checks direct application-context callers also reuse the same object.
    } // Ends context lookup verification.

    /** Checks concurrent callers share every lazy component and the dedicated key-store provider. */
    @Test // Guards against duplicate Room, DataStore, client or repository construction across screens.
    fun concurrentCallersShareTheSameDependencyGraph() = runBlocking { // Runs parallel getter access without sending network traffic.
        val application = context.app // Gets the one manifest-owned graph.
        val copies = withTimeout(10_000) { // Gives every worker a bounded time to initialise or reuse dependencies.
            (1..24).map { // Exercises more than one simultaneous first-access caller.
                async(Dispatchers.Default) { // Reads the lazy fields from a worker coroutine.
                    listOf(application.database, application.apiKeyDataStore, application.httpClient, application.connectivityObserver, application.settingsRepository, application.geminiRemoteDataSource, application.chatRepository) // Captures object identities only, never key or message values.
                } // Ends one concurrent graph lookup.
            }.awaitAll() // Waits until all graph lookups finished.
        } // Ends concurrent construction.
        val expected = copies.first() // Uses one caller's references as the identity baseline.
        for (copy in copies) { // Checks each worker received the same resource references.
            for (index in expected.indices) { // Covers each component in the shared graph.
                assertTrue("Every graph component must be shared across callers", expected[index] === copy[index]) // Uses boolean identity to avoid dumping dependency internals on failure.
            } // Ends component identity verification.
        } // Ends concurrent caller verification.
        assertSame(ApiKeyDataStore.get(context), application.apiKeyDataStore) // Checks the locator did not construct another preference store for the same file.
        assertTrue(application.httpClient.engine is OkHttpEngine) // Checks the production graph selects the allowed Ktor engine.
        assertEquals(DroidMentorDatabase.DATABASE_NAME, application.database.openHelper.databaseName) // Checks the shared database retains its stable production file name.
    } // Ends shared dependency verification.

    /** Checks the app's repository writes through the exact app-owned database and cleans up only its own row. */
    @Test // Verifies real graph binding with one temporary local chat and no key or HTTP operation.
    fun repositoryUsesTheAppOwnedDatabase() = runBlocking { // Runs only local persistence operations through the production graph.
        val application = context.app // Uses the actual registered service locator.
        val id = when (val result = application.chatRepository.createChat("Application graph test")) { // Creates one clearly test-owned local row without prerequisites.
            is ChatCommandResult.Created -> result.chatId // Captures its generated ID for precise cleanup.
            else -> error("Could not create the graph test's local row") // Reports no raw data or credential details if the command fails.
        } // Ends local creation result handling.
        try { // Deletes the row even when a graph assertion fails.
            assertEquals("Application graph test", application.database.chatDao().getById(id)?.title) // Checks the repository used this same shared database instance.
            val read = withTimeout(5_000) { application.chatRepository.observeChat(id).first() } // Reads the row through the public Room-backed stream.
            assertTrue("The graph's saved row must be observable", read is ChatReadResult.Success && read.value?.id == id) // Checks observation without printing unrelated stored chats.
        } finally { // Restores local history after this reversible binding test.
            assertEquals(ChatCommandResult.Stored, application.chatRepository.deleteChat(id)) // Deletes only the unique row created by this test.
        } // Ends temporary row lifetime.
        assertNull(application.database.chatDao().getById(id)) // Confirms the temporary row was removed while other history remains untouched.
    } // Ends actual repository-to-database graph verification.
} // Ends application graph tests without closing process-owned shared resources.
