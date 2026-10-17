package pt.isel.pdm.droidmentor.di // Groups the contracts used for manual dependency injection.

import kotlinx.coroutines.flow.Flow // Lets ViewModels observe connectivity without knowing the Android network API.
import pt.isel.pdm.droidmentor.data.repository.ChatRepository // Exposes chat operations through their replaceable interface.
import pt.isel.pdm.droidmentor.data.repository.SettingsRepository // Exposes settings operations through their replaceable interface.
import pt.isel.pdm.droidmentor.domain.ConnectivityStatus // Describes connectivity with the existing domain enum.

/** Supplies the dependencies needed by screen ViewModels; the Application owns the real implementations. */
interface DependencyContainer { // A test can implement this contract without creating Room, DataStore or a network client.
    /** Shared offline-first chat operations and persisted history streams. */
    val chatRepository: ChatRepository // Keeps database entities and API responses out of factory construction.

    /** Shared settings operations backed by Preferences DataStore in the app. */
    val settingsRepository: SettingsRepository // Allows settings tests to supply an in-memory fake.

    /** Validated connectivity updates, collected only while a caller needs them. */
    val connectivityStatus: Flow<ConnectivityStatus> // Avoids exposing the concrete Android connectivity observer to ViewModels.
} // Ends the manually replaceable dependency contract.
