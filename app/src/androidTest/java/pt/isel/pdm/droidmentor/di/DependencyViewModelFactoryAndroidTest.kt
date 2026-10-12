package pt.isel.pdm.droidmentor.di // Verifies the default factory path against Android's manifest-created Application.

import android.app.Application // Supplies an incompatible Application for a wiring error test.
import androidx.lifecycle.ViewModel // Defines a harmless test-only ViewModel without screen behaviour.
import androidx.lifecycle.ViewModelProvider // Supplies the standard Application creation extras key.
import androidx.lifecycle.viewmodel.MutableCreationExtras // Builds the same Application entry provided by an Android owner.
import androidx.test.ext.junit.runners.AndroidJUnit4 // Executes with real Android classes and the target app graph.
import androidx.test.platform.app.InstrumentationRegistry // Retrieves the target Application context.
import org.junit.Assert.assertEquals // Checks constructor inputs and safe wiring diagnostics.
import org.junit.Assert.assertSame // Checks the production container and exact shared graph references.
import org.junit.Assert.assertThrows // Checks an incompatible Application is rejected before ViewModel creation.
import org.junit.Test // Marks Android factory integration cases.
import org.junit.runner.RunWith // Selects the instrumentation runner.
import pt.isel.pdm.droidmentor.app // Resolves the already registered service locator from the target context.

/** Checks production Application extras and rejection of an Application that lacks the container contract. */
@RunWith(AndroidJUnit4::class) // Runs against the actual app manifest rather than a unit-test Android stub.
class DependencyViewModelFactoryAndroidTest { // Performs no history command, settings read or network operation.
    /** Checks the default initializer resolves the actual Application as a DependencyContainer. */
    @Test // Verifies the production path needs no explicit test replacement.
    fun applicationExtrasSupplyTheRealSharedContainer() { // Uses the standard Application key supplied by Android screen owners.
        val application = InstrumentationRegistry.getInstrumentation().targetContext.app // Gets the manifest-created service locator.
        val extras = MutableCreationExtras() // Supplies only the Application entry needed by this factory.
        extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] = application // Matches the owner's normal creation extras contract.
        val factory = dependencyViewModelFactory { ContainerViewModel(it) } // Uses the production default instead of passing a container directly.
        val viewModel = factory.create(ContainerViewModel::class.java, extras) // Resolves the Application through the initializer's extras.
        assertSame(application, viewModel.dependencies) // Checks the container itself is the actual registered Application.
        assertSame(application.chatRepository, viewModel.dependencies.chatRepository) // Checks the repository remains process-owned and shared.
        assertSame(application.settingsRepository, viewModel.dependencies.settingsRepository) // Checks settings use the same manually wired repository.
        assertSame(application.connectivityObserver.status, viewModel.dependencies.connectivityStatus) // Checks the contract exposes the existing cold connectivity stream.
    } // Ends actual Application factory binding verification.

    /** Checks an Application without the required interface fails with a safe construction error. */
    @Test // Detects incorrect Application registration without invoking the supplied constructor.
    fun incompatibleApplicationFailsBeforeConstruction() { // Uses a plain Android Application without the app's container interface.
        val extras = MutableCreationExtras() // Builds one isolated test extras map.
        extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] = Application() // Supplies an incompatible class without creating any dependencies.
        val factory = dependencyViewModelFactory<ContainerViewModel> { error("Constructor must not run with an incompatible Application") } // Makes premature constructor invocation detectable.
        val failure = assertThrows(IllegalStateException::class.java) { // Checks the missing contract is handled explicitly.
            factory.create(ContainerViewModel::class.java, extras) // Requests the registered class using the wrong application.
        } // Ends incompatible Application assertion.
        assertEquals("ViewModel creation requires a DependencyContainer or Application creation extras", failure.message) // Checks no Application internals are included in the diagnostic.
    } // Ends incompatible Application verification.

    private class ContainerViewModel(val dependencies: DependencyContainer) : ViewModel() // Retains only the container passed to its test-only constructor.
} // Ends production factory integration tests.
