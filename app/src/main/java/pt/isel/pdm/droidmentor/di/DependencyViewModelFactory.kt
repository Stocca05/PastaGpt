package pt.isel.pdm.droidmentor.di // Keeps manual ViewModel construction beside the dependency contract.

import androidx.lifecycle.ViewModel // Limits the factory to AndroidX ViewModel types.
import androidx.lifecycle.ViewModelProvider // Supplies the factory contract and the Application extras key.
import androidx.lifecycle.viewmodel.initializer // Registers the constructor for the requested ViewModel type.
import androidx.lifecycle.viewmodel.viewModelFactory // Builds the required AndroidX factory without a DI framework.

/**
 * Builds a factory for one ViewModel type using a hand-written constructor lambda.
 *
 * The default resolves the container from the owner's Application creation extras.
 * Tests can pass [dependencies] explicitly to use fake repositories without an Android Application.
 * The constructor runs only when ViewModelProvider needs a new instance in its owner's store.
 * @param dependencies An optional replacement for the app-owned dependency container.
 * @param create The constructor that selects and supplies this ViewModel's dependencies.
 */
inline fun <reified VM : ViewModel> dependencyViewModelFactory( // Registers the concrete type selected at the call site.
    dependencies: DependencyContainer? = null, // Uses Android's Application by default and permits an explicit test container.
    crossinline create: (DependencyContainer) -> VM, // Lets each screen select only the dependencies its constructor needs.
): ViewModelProvider.Factory = viewModelFactory { // Delegates ownership and instance reuse to AndroidX.
    initializer { // Runs with the creation extras supplied by the ViewModel owner.
        val container = dependencies // Prefers a supplied fake so tests never initialise the real app graph.
            ?: (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? DependencyContainer) // Resolves the manifest-created Application through its interface.
            ?: error("ViewModel creation requires a DependencyContainer or Application creation extras") // Reports a construction mistake without exposing dependency internals.
        create(container) // Calls the screen's explicit constructor with the selected container.
    } // Ends this ViewModel type's initializer.
} // Ends manual factory construction.
