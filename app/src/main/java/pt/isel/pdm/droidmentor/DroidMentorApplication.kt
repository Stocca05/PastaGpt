package pt.isel.pdm.droidmentor // Places the app-owned service locator in the root package.

import android.app.Application // Provides the single application object created by Android for this process.
import android.content.Context // Allows activities and context wrappers to find that application object.
import androidx.datastore.core.DataStore // Exposes the asynchronous preference storage instance.
import androidx.datastore.preferences.core.Preferences // Identifies the dedicated key store's value type.
import io.ktor.client.HttpClient // Shares the configured Ktor client across API operations.
import kotlinx.coroutines.CoroutineScope // Launches startup orphan image cleanup without blocking the main thread (M3-15).
import kotlinx.coroutines.Dispatchers // Performs startup DB query and disk sweep on IO.
import kotlinx.coroutines.SupervisorJob // Prevents background cleanup errors from tearing down app scope.
import kotlinx.coroutines.flow.Flow // Exposes connectivity observation through the replaceable container contract.
import kotlinx.coroutines.launch // Schedules one-shot orphan file cleanup at app launch.
import pt.isel.pdm.droidmentor.data.connectivity.ConnectivityObserver // Supplies the real Android network observation and fresh send checks.
import pt.isel.pdm.droidmentor.data.local.DroidMentorDatabase // Owns the persistent Room history database.
import pt.isel.pdm.droidmentor.data.local.ImageStorage // Exposes app-private conversation image storage contract.
import pt.isel.pdm.droidmentor.data.local.LocalImageStorage // Copies picked/captured images into filesDir/images.
import pt.isel.pdm.droidmentor.data.preferences.ActiveSessionDataStore // Reuses the dedicated Preferences DataStore for session restoration.
import pt.isel.pdm.droidmentor.data.preferences.ApiKeyDataStore // Reuses the single dedicated Preferences DataStore provider.
import pt.isel.pdm.droidmentor.data.remote.GeminiRemoteDataSource // Sends stateless requests using the shared client and network check.
import pt.isel.pdm.droidmentor.data.remote.HttpClientFactory // Applies the already verified client configuration.
import pt.isel.pdm.droidmentor.data.repository.ChatRepository // Exposes the chat repository contract to callers.
import pt.isel.pdm.droidmentor.data.repository.DataStoreSessionRepository // Persists activeChatId separately from credentials.
import pt.isel.pdm.droidmentor.data.repository.DataStoreSettingsRepository // Connects preferences to the settings contract by hand.
import pt.isel.pdm.droidmentor.data.repository.RoomChatRepository // Connects all existing chat components by hand.
import pt.isel.pdm.droidmentor.data.repository.SessionRepository // Exposes active-session persistence without leaking DataStore.
import pt.isel.pdm.droidmentor.data.repository.SettingsRepository // Exposes settings without leaking the storage implementation.
import pt.isel.pdm.droidmentor.di.DependencyContainer // Supplies the contract used by manual ViewModel factories and their tests.
import pt.isel.pdm.droidmentor.domain.ConnectivityStatus // Uses the existing domain values for network status.

/** Owns the app's shared dependencies using manual construction and lazy process-wide instances. */
class DroidMentorApplication : Application(), DependencyContainer { // Android creates the real container through the application name registered in the manifest.

    /** Shared Room database; building its wrapper does not enable main-thread queries. */
    val database: DroidMentorDatabase by lazy { // Uses Kotlin's default synchronised lazy initialisation across callers.
        DroidMentorDatabase.create(this) // Uses application storage and the existing stable database name.
    } // Ends database construction on first access.

    /** Single Preferences DataStore dedicated to the user's Gemini key. */
    val apiKeyDataStore: DataStore<Preferences> by lazy { // Creates no separate store or credential field in the application.
        ApiKeyDataStore.get(this) // Reuses the provider's application-context instance.
    } // Ends key-store lookup on first access.

    /** Separate Preferences DataStore dedicated to M3-06 active conversation restoration. */
    val activeSessionDataStore: DataStore<Preferences> by lazy { // Never mixes UI session state with credentials.
        ActiveSessionDataStore.get(this) // Reuses the single process-owned session store.
    } // Ends session-store lookup on first access.

    /** Shared client owned for this application process rather than recreated per request. */
    val httpClient: HttpClient by lazy { // Defers creating the Ktor engine until a caller needs networking dependencies.
        HttpClientFactory.create() // Uses the existing JSON, timeout and safe build-variant logging configuration.
    } // Ends HTTP client construction.

    /** Shared Android network component; its Flow owns callbacks only while collected. */
    val connectivityObserver: ConnectivityObserver by lazy { // Keeps no activity reference or eagerly registered network listener.
        ConnectivityObserver(this) // Obtains its service from the application context.
    } // Ends connectivity component construction.

    /** Connectivity stream exposed without coupling ViewModels to the concrete Android observer. */
    override val connectivityStatus: Flow<ConnectivityStatus> // Shares the same observer used by the repository's fresh network checks.
        get() = connectivityObserver.status // Does not register callbacks until a caller collects the stream.

    /** Settings operations backed only by the same dedicated Preferences DataStore. */
    override val settingsRepository: SettingsRepository by lazy { // Implements the replaceable contract while manually selecting the concrete implementation.
        DataStoreSettingsRepository(apiKeyDataStore) // Reads and changes the one app-owned key store.
    } // Ends settings repository construction.

    /** Active-session restoration backed by its own dedicated Preferences DataStore. */
    override val sessionRepository: SessionRepository by lazy { // Implements M3-06 persistence cleanly via manual DI.
        DataStoreSessionRepository(activeSessionDataStore) // Uses the separate session store.
    } // Ends session repository construction.

    /** Shared Gemini source using the shared client, validated Android connection check and in-memory Base64 encoder. */
    val geminiRemoteDataSource: GeminiRemoteDataSource by lazy { // Keeps credentials and histories per operation rather than in this locator.
        GeminiRemoteDataSource(httpClient, connectivityObserver::isConnected, encodeImageBase64 = imageStorage::encodeAsBase64) // Produces Base64 in memory only (V9).
    } // Ends remote source construction.

    /** One offline-first repository instance, including one operation gate per chat for the app. */
    override val chatRepository: ChatRepository by lazy { // Implements the replaceable contract and shares its per-chat locks across every screen.
        RoomChatRepository(database, settingsRepository, geminiRemoteDataSource, connectivityObserver::isConnected, imageStorage = imageStorage) // Wires image cleanup on deletes and rewrites.
    } // Ends chat repository construction.

    /** Shared app-private image storage rooted in filesDir/images. */
    override val imageStorage: ImageStorage by lazy { // Implements manual DI for gallery/camera attachment management.
        LocalImageStorage(this) // Keeps copied images private inside application storage.
    } // Ends image storage construction.

    private val maintenanceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) // Runs fire-and-forget startup orphan cleanup off main thread.

    override fun onCreate() { // Schedules background cleanup of orphaned image files on startup (M3-15).
        super.onCreate() // Performs standard Android Application initialization.
        maintenanceScope.launch { cleanupOrphanImages() } // Sweeps leftover unreferenced files from killed sessions off Main.
    } // Ends Application.onCreate.

    /** Removes image files in filesDir/images/ that are no longer referenced by any Room message. */
    internal suspend fun cleanupOrphanImages(): Int = try { // Exposes deterministic orphan cleanup for startup and tests.
        val referenced = database.messageDao().getAllReferencedImagePaths().toSet() // Reads every active relative imagePath in Room.
        imageStorage.cleanupOrphans(referenced) // Deletes unreferenced orphan files cleanly.
    } catch (_: Exception) { // Avoids crashing app startup if storage is temporarily unavailable.
        0 // Returns zero safely.
    } // Ends orphan cleanup helper.
} // Ends the application service locator.

/** Finds the manifest-registered locator through the application context without retaining an activity. */
val Context.app: DroidMentorApplication // Provides the agreed short accessor for UI and later factory construction.
    get() = applicationContext as DroidMentorApplication // Resolves wrapped or activity contexts to the same process-owned application.
