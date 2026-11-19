package pt.isel.pdm.droidmentor.ui.components // Groups small shared UI components.

import androidx.compose.foundation.layout.Column // Places subtle hairline divider cleanly beneath TopAppBar.
import androidx.compose.foundation.layout.PaddingValues // Supplies the space reserved for the app bar and system insets.
import androidx.compose.foundation.layout.RowScope // Allows screens to add TopAppBar action buttons.
import androidx.compose.material3.ExperimentalMaterial3Api // Marks the existing TopAppBar API's opt-in requirement.
import androidx.compose.material3.HorizontalDivider // Separates app bar visually from scrolling content.
import androidx.compose.material3.Icon // Renders the Material back arrow cleanly.
import androidx.compose.material3.IconButton // Provides >=48dp touch target for navigation Back.
import androidx.compose.material3.MaterialTheme // Supplies surface and hairline outline colours.
import androidx.compose.material3.Scaffold // Arranges the app bar, content and snackbar host.
import androidx.compose.material3.SnackbarHost // Displays one-shot feedback above the screen content.
import androidx.compose.material3.SnackbarHostState // Shares feedback state with the caller.
import androidx.compose.material3.Text // Displays the truncated single-line screen title.
import androidx.compose.material3.TopAppBar // Keeps the title and return action at the top of the screen.
import androidx.compose.material3.TopAppBarDefaults // Uses clean surface colours aligned with Material 3.
import androidx.compose.runtime.Composable // Marks this layout as Compose content.
import androidx.compose.runtime.remember // Keeps one snackbar state for this screen composition.
import androidx.compose.ui.res.painterResource // Loads the local back-arrow vector asset.
import androidx.compose.ui.res.stringResource // Resolves the accessible Back label.
import androidx.compose.ui.text.style.TextOverflow // Truncates long chat titles cleanly in the app bar.
import pt.isel.pdm.droidmentor.R // Supplies the navigation icon and accessible strings.

/** Provides a polished Material 3 screen title, Back icon button, subtle divider and snackbar host. */
@OptIn(ExperimentalMaterial3Api::class) // Uses the TopAppBar shipped by the declared Material 3 dependency.
@Composable // Hosts content supplied by each screen.
fun ScreenScaffold( // Keeps the repeated screen frame readable without moving business logic into a component.
    title: String, // Displays the screen's resource-backed or saved title.
    onBack: () -> Unit, // Invokes the navigation callback supplied by the graph.
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }, // Allows a screen to provide its own feedback state.
    actions: @Composable RowScope.() -> Unit = {}, // Optional TopAppBar actions such as renaming a chat.
    floatingActionButton: @Composable () -> Unit = {}, // Lets history display its explicit new-conversation action.
    content: @Composable (PaddingValues) -> Unit, // Lets the caller apply the app bar and system padding to its own layout.
) { // Starts the common screen frame.
    Scaffold( // Uses Material's system-inset handling rather than manual status-bar measurements.
        containerColor = MaterialTheme.colorScheme.background, // Keeps consistent themed screen background.
        topBar = { // Defines the screen heading with clean visual separation.
            Column { // Stacks TopAppBar above a crisp 1dp divider.
                TopAppBar( // Keeps navigation visible when body content scrolls.
                    title = { // Displays single-line title cleanly.
                        Text( // Renders screen or conversation title.
                            text = title, // Supplies visible heading.
                            maxLines = 1, // Keeps TopAppBar single-height.
                            overflow = TextOverflow.Ellipsis, // Clips long titles gracefully.
                        ) // Ends title Text.
                    }, // Ends title slot.
                    navigationIcon = { // Supplies standard Material 3 back arrow with accessible label.
                        IconButton(onClick = onBack) { // Meets 48dp minimum touch target automatically.
                            Icon( // Renders clean vector arrow.
                                painter = painterResource(R.drawable.ic_arrow_back), // Loads vector back arrow.
                                contentDescription = stringResource(R.string.navigation_back), // Accessible "Back" description.
                            ) // Ends back Icon.
                        } // Ends IconButton.
                    }, // Ends navigationIcon slot.
                    actions = actions, // Displays optional destination-specific actions.
                    colors = TopAppBarDefaults.topAppBarColors( // Uses surface colour for calm modern header.
                        containerColor = MaterialTheme.colorScheme.surface, // Matches Material 3 flat top bar style.
                        titleContentColor = MaterialTheme.colorScheme.onSurface, // Ensures crisp text contrast.
                    ), // Ends TopAppBar colour setup.
                ) // Ends screen app bar.
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) // Adds crisp separation line.
            } // Ends topBar Column.
        }, // Ends the app bar slot.
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Displays typed feedback requested by a screen.
        floatingActionButton = floatingActionButton, // Places the caller's primary action above system insets.
        content = content, // Supplies the reserved padding to the screen's body.
    ) // Ends the common layout.
} // Ends the reusable screen frame.
