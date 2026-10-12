package pt.isel.pdm.droidmentor // Places the app-owned service locator in the root package.

import android.app.Application // Provides the single application object created by Android for this process.
import android.content.Context // Allows activities and context wrappers to find that application object.
import androidx.datastore.core.DataStore // Exposes the asynchronous preference storage instance.
import androidx.datastore.preferences.core.Preferences // Identifies the dedicated key store's value type.
import io.ktor.client.HttpClient // Shares the configured Ktor client across API operations.
import kotlinx.coroutines.flow.Flow // Exposes connectivity observation through the replaceable container contract.
import pt.isel.pdm.droidmentor.data.connectivity.ConnectivityObserver // Supplies the real Android network observation and fresh send checks.
import pt.isel.pdm.droidmentor.data.local.DroidMentorDatabase // Owns the persistent Room history database.
import pt.isel.pdm.droidmentor.data.preferences.ApiKeyDataStore // Reuses the single dedicated Preferences DataStore provider.
import pt.isel.pdm.droidmentor.data.remote.GeminiRemoteDataSource // Sends stateless requests using the shared client and network check.
import pt.isel.pdm.droidmentor.data.remote.HttpClientFactory // Applies the already verified client configuration.
import pt.isel.pdm.droidmentor.data.repository.ChatRepository // Exposes the chat repository contract to callers.
import pt.isel.pdm.droidmentor.data.repository.DataStoreSettingsRepository // Connects preferences to the settings contract by hand.
import pt.isel.pdm.droidmentor.data.repository.RoomChatRepository // Connects all existing chat components by hand.
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

    /** Shared Gemini source using the shared client and actual validated Android connection check. */
    val geminiRemoteDataSource: GeminiRemoteDataSource by lazy { // Keeps credentials and histories per operation rather than in this locator.
        GeminiRemoteDataSource(httpClient, connectivityObserver::isConnected) // Requires the real suspend network gate before each POST.
    } // Ends remote source construction.

    /** One offline-first repository instance, including one operation gate per chat for the app. */
    override val chatRepository: ChatRepository by lazy { // Implements the replaceable contract and shares its per-chat locks across every screen.
        RoomChatRepository(database, settingsRepository, geminiRemoteDataSource, connectivityObserver::isConnected) // Supplies each dependency explicitly without any injection framework.
    } // Ends chat repository construction.
} // Ends the application service locator.

/** Finds the manifest-registered locator through the application context without retaining an activity. */
val Context.app: DroidMentorApplication // Provides the agreed short accessor for UI and later factory construction.
    get() = applicationContext as DroidMentorApplication // Resolves wrapped or activity contexts to the same process-owned application.
