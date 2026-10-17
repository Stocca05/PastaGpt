package pt.isel.pdm.droidmentor.ui.components // Groups small shared UI components.

import androidx.compose.foundation.layout.PaddingValues // Supplies the space reserved for the app bar and system insets.
import androidx.compose.material3.ExperimentalMaterial3Api // Marks the existing TopAppBar API's opt-in requirement.
import androidx.compose.material3.Scaffold // Arranges the app bar, content and snackbar host.
import androidx.compose.material3.SnackbarHost // Displays one-shot feedback above the screen content.
import androidx.compose.material3.SnackbarHostState // Shares feedback state with the caller.
import androidx.compose.material3.Text // Displays the title and return label.
import androidx.compose.material3.TextButton // Provides a labelled Back action without an extra icon dependency.
import androidx.compose.material3.TopAppBar // Keeps the title and return action at the top of the screen.
import androidx.compose.runtime.Composable // Marks this layout as Compose content.
import androidx.compose.runtime.remember // Keeps one snackbar state for this screen composition.
import androidx.compose.ui.res.stringResource // Resolves the shared Back label.
import pt.isel.pdm.droidmentor.R // Supplies the existing navigation resources.

/** Provides a screen title, Back action and optional snackbar feedback using the existing Material theme. */
@OptIn(ExperimentalMaterial3Api::class) // Uses the TopAppBar shipped by the declared Material 3 dependency.
@Composable // Hosts content supplied by each screen.
fun ScreenScaffold( // Keeps the repeated screen frame readable without moving business logic into a component.
    title: String, // Displays the screen's resource-backed title.
    onBack: () -> Unit, // Invokes the navigation callback supplied by the graph.
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }, // Allows a screen to provide its own feedback state.
    floatingActionButton: @Composable () -> Unit = {}, // Lets history display its explicit new-conversation action.
    content: @Composable (PaddingValues) -> Unit, // Lets the caller apply the app bar and system padding to its own layout.
) { // Starts the common screen frame.
    Scaffold( // Uses Material's system-inset handling rather than manual status-bar measurements.
        topBar = { // Defines the screen heading.
            TopAppBar( // Keeps the return action visible even when body content scrolls.
                title = { Text(title) }, // Displays the screen name.
                navigationIcon = { // Supplies the explicit return action.
                    TextButton(onClick = onBack) { Text(stringResource(R.string.navigation_back)) } // Pops the existing navigation entry through its callback.
                }, // Ends the return action.
            ) // Ends the screen app bar.
        }, // Ends the app bar slot.
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Displays typed feedback requested by a screen.
        floatingActionButton = floatingActionButton, // Places the caller's primary action above system insets.
        content = content, // Supplies the reserved padding to the screen's body.
    ) // Ends the common layout.
} // Ends the reusable screen frame.
