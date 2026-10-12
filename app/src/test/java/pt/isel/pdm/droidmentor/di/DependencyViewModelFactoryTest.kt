package pt.isel.pdm.droidmentor.di // Checks manual factory construction without an Android application or real storage.

import androidx.lifecycle.ViewModel // Supplies harmless test-only ViewModel types.
import androidx.lifecycle.ViewModelProvider // Exercises real AndroidX factory and store behaviour.
import androidx.lifecycle.ViewModelStore // Owns instances independently for each simulated screen owner.
import androidx.lifecycle.viewmodel.CreationExtras // Allows tests to omit all Android application extras.
import kotlinx.coroutines.flow.Flow // Implements the container's stream contracts in memory.
import kotlinx.coroutines.flow.MutableStateFlow // Supplies a fake connectivity stream with no Android callbacks.
import kotlinx.coroutines.flow.flowOf // Supplies empty repository snapshots without Room or DataStore.
import org.junit.Assert.assertEquals // Checks how many times a constructor actually ran.
import org.junit.Assert.assertNotSame // Checks separate owners do not share their ViewModels.
import org.junit.Assert.assertSame // Checks exact fake dependency and same-owner instance reuse.
import org.junit.Assert.assertThrows // Checks missing configuration and unsupported types fail clearly.
import org.junit.Test // Marks each factory behaviour test.
import pt.isel.pdm.droidmentor.data.repository.ChatCommandResult // Supplies harmless local acknowledgements in the fake.
import pt.isel.pdm.droidmentor.data.repository.ChatReadResult // Supplies typed empty history snapshots.
import pt.isel.pdm.droidmentor.data.repository.ChatRepository // Implements the replaceable chat contract for tests.
import pt.isel.pdm.droidmentor.data.repository.SettingsReadResult // Supplies an unconfigured preferences snapshot.
import pt.isel.pdm.droidmentor.data.repository.SettingsRepository // Implements the replaceable settings contract for tests.
import pt.isel.pdm.droidmentor.data.repository.SettingsWriteResult // Supplies unused fake settings command outcomes.
import pt.isel.pdm.droidmentor.domain.Chat // Types the fake's optional conversation snapshot.
import pt.isel.pdm.droidmentor.domain.ChatSummary // Types the fake's empty history list.
import pt.isel.pdm.droidmentor.domain.ConnectivityStatus // Supplies the initial offline test state.
import pt.isel.pdm.droidmentor.domain.Message // Types the fake's empty message list.

/** Verifies fake injection, lazy construction, owner scoping and configuration errors through AndroidX. */
class DependencyViewModelFactoryTest { // Uses no application, database, preference file or HTTP client.
    /** Checks factory creation does not construct a ViewModel before a provider requests one. */
    @Test // Protects normal lazy construction rather than creating a singleton ViewModel in the graph.
    fun factoryDefersTheConstructorUntilCreation() { // Counts calls to the explicitly supplied constructor.
        val dependencies = FakeDependencyContainer() // Provides only in-memory implementations.
        var creations = 0 // Starts with no constructed ViewModels.
        val factory = dependencyViewModelFactory(dependencies) { container -> // Captures dependencies without calling the initializer.
            creations += 1 // Counts one actual provider request for a fresh instance.
            RepositoryViewModel(container) // Builds the test ViewModel by hand.
        } // Ends the factory's constructor definition.
        assertEquals(0, creations) // Checks creating the factory itself did no ViewModel work.
        factory.create(RepositoryViewModel::class.java, CreationExtras.Empty) // Requests one instance without an Application.
        assertEquals(1, creations) // Checks the initializer called the constructor exactly once.
    } // Ends lazy constructor verification.

    /** Checks explicit injection uses the exact fake repository and connectivity references. */
    @Test // Demonstrates that screen tests do not need the real service locator.
    fun explicitContainerSuppliesAllFakeDependencies() { // Supplies a complete fake contract and no creation extras.
        val dependencies = FakeDependencyContainer() // Creates independent in-memory test dependencies.
        val factory = dependencyViewModelFactory(dependencies) { RepositoryViewModel(it) } // Selects repositories explicitly in the test ViewModel constructor.
        val viewModel = factory.create(RepositoryViewModel::class.java, CreationExtras.Empty) // Creates through the required AndroidX factory API.
        assertSame(dependencies.chatRepository, viewModel.chatRepository) // Checks the real chat graph was not substituted for the fake.
        assertSame(dependencies.settingsRepository, viewModel.settingsRepository) // Checks settings remain in memory without accessing a credential file.
        assertSame(dependencies.connectivityStatus, viewModel.connectivityStatus) // Checks connectivity needs no Android service.
    } // Ends fake dependency binding verification.

    /** Checks one owner's ViewModel is reused while a second owner gets its own instance. */
    @Test // Guards against factories incorrectly owning ViewModel lifetime or global instance caches.
    fun providersReuseOnlyTheirOwnStoredViewModel() { // Uses the real provider to exercise AndroidX ownership semantics.
        val dependencies = FakeDependencyContainer() // Shares dependencies as the app does, while separating ViewModel owners.
        var creations = 0 // Counts constructors across both stores.
        val factory = dependencyViewModelFactory(dependencies) { container -> // Registers one constructor for the requested class.
            creations += 1 // Records actual ViewModel construction rather than factory allocation.
            RepositoryViewModel(container) // Creates a fresh owner-scoped instance.
        } // Ends constructor definition.
        val firstStore = ViewModelStore() // Simulates the first screen's owner.
        val secondStore = ViewModelStore() // Simulates a different owner.
        try { // Always clears test-owned ViewModels after assertions.
            val firstProvider = ViewModelProvider.create(firstStore, factory, CreationExtras.Empty) // Uses the fake dependencies without any Android extras.
            val secondProvider = ViewModelProvider.create(secondStore, factory, CreationExtras.Empty) // Gives the same factory an independent store.
            val first = firstProvider[RepositoryViewModel::class.java] // Creates the first owner's instance.
            assertSame(first, firstProvider[RepositoryViewModel::class.java]) // Checks later lookups reuse that owner's saved instance.
            assertNotSame(first, secondProvider[RepositoryViewModel::class.java]) // Checks another owner does not receive the first ViewModel.
            assertEquals(2, creations) // Checks exactly one constructor ran per owner.
        } finally { // Releases only the stores created by this test.
            firstStore.clear() // Clears the first owner's ViewModel.
            secondStore.clear() // Clears the second owner's ViewModel.
        } // Ends store lifetime handling.
    } // Ends owner scoping verification.

    /** Checks omitted dependencies and Application extras report a configuration error. */
    @Test // Covers accidental use outside an Android owner without a supplied test container.
    fun missingContainerFailsBeforeCallingTheConstructor() { // Makes any unexpected constructor invocation fail the test.
        val factory = dependencyViewModelFactory<RepositoryViewModel> { error("Constructor must not run without dependencies") } // Uses the production default with no container available.
        val failure = assertThrows(IllegalStateException::class.java) { // Expects a safe and explicit construction error.
            factory.create(RepositoryViewModel::class.java, CreationExtras.Empty) // Omits both dependency injection paths.
        } // Ends missing dependency assertion.
        assertEquals("ViewModel creation requires a DependencyContainer or Application creation extras", failure.message) // Checks the error explains the missing configuration.
    } // Ends missing configuration verification.

    /** Checks a factory registered for one class refuses unrelated ViewModel types. */
    @Test // Protects against returning a different ViewModel under an incompatible provider key.
    fun factoryRejectsAnUnregisteredViewModelType() { // Requests a class that has no initializer in this factory.
        val factory = dependencyViewModelFactory(FakeDependencyContainer()) { RepositoryViewModel(it) } // Registers only the repository-backed test type.
        assertThrows(IllegalArgumentException::class.java) { // Checks AndroidX rejects the unsupported class.
            factory.create(OtherViewModel::class.java, CreationExtras.Empty) // Supplies a valid container but the wrong ViewModel type.
        } // Ends unsupported type assertion.
    } // Ends registered type verification.

    /** Checks construction errors retain their original identity instead of producing a broken ViewModel. */
    @Test // Keeps wiring mistakes observable during development and tests.
    fun constructorFailureIsPreserved() { // Throws a harmless test-only exception inside the constructor lambda.
        val expected = IllegalStateException("Test constructor failure") // Uses no raw repository, network or credential details.
        val factory = dependencyViewModelFactory<RepositoryViewModel>(FakeDependencyContainer()) { throw expected } // Provides valid dependencies but a failing constructor.
        val actual = assertThrows(IllegalStateException::class.java) { // Expects the original construction failure.
            factory.create(RepositoryViewModel::class.java, CreationExtras.Empty) // Runs the registered constructor.
        } // Ends constructor failure assertion.
        assertSame(expected, actual) // Checks the factory did not swallow or replace the failure.
    } // Ends construction error verification.

    private class RepositoryViewModel(dependencies: DependencyContainer) : ViewModel() { // Exists only in tests and implements no future screen behaviour.
        val chatRepository = dependencies.chatRepository // Selects the fake chat contract explicitly.
        val settingsRepository = dependencies.settingsRepository // Selects the fake settings contract explicitly.
        val connectivityStatus = dependencies.connectivityStatus // Selects the fake stream without Android services.
    } // Ends the test-only constructor example.

    private class OtherViewModel : ViewModel() // Supplies an unrelated class for unsupported-type verification.

    private class FakeDependencyContainer : DependencyContainer { // Replaces the entire screen-facing graph with simple in-memory contracts.
        override val connectivityStatus = MutableStateFlow(ConnectivityStatus.DISCONNECTED) // Starts offline without registering a network callback.
        override val settingsRepository = object : SettingsRepository { // Holds no key and opens no DataStore file.
            override val apiKey: Flow<String?> = flowOf(null) // Represents an unconfigured key without inventing a credential.
            override val readResult: Flow<SettingsReadResult> = flowOf(SettingsReadResult.Success(null)) // Provides the corresponding typed settings snapshot.
            override suspend fun saveApiKey(value: String) = SettingsWriteResult.STORAGE_ERROR // Performs no credential write in these construction tests.
            override suspend fun clearApiKey() = SettingsWriteResult.SUCCESS // Removes nothing because this fake holds no preference data.
        } // Ends the in-memory settings fake.
        override val chatRepository = object : ChatRepository { // Supplies empty local snapshots without creating Room.
            override fun observeChats(): Flow<ChatReadResult<List<ChatSummary>>> = flowOf(ChatReadResult.Success(emptyList())) // Represents an empty saved conversation list.
            override fun observeChat(chatId: String): Flow<ChatReadResult<Chat?>> = flowOf(ChatReadResult.Success(null)) // Represents an absent conversation.
            override fun observeMessages(chatId: String): Flow<ChatReadResult<List<Message>>> = flowOf(ChatReadResult.Success(emptyList())) // Represents an empty saved message list.
            override suspend fun createChat(title: String): ChatCommandResult = ChatCommandResult.StorageError // Keeps construction tests free from history writes.
            override suspend fun renameChat(chatId: String, title: String): ChatCommandResult = ChatCommandResult.NotFound // Has no stored row to rename.
            override suspend fun deleteChat(chatId: String): ChatCommandResult = ChatCommandResult.NotFound // Has no stored row to delete.
            override suspend fun sendMessage(chatId: String, text: String): ChatCommandResult = ChatCommandResult.Offline // Never starts HTTP from a factory test.
            override suspend fun retryMessage(chatId: String, messageId: String): ChatCommandResult = ChatCommandResult.NotFound // Has no failed message to replay.
        } // Ends the in-memory chat fake.
    } // Ends the complete fake dependency graph.
} // Ends manual ViewModel factory tests.
