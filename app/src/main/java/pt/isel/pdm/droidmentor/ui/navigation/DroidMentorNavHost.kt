package pt.isel.pdm.droidmentor.ui.navigation // Owns route registration and navigation callbacks for the five M2 screens.

import androidx.compose.foundation.layout.Arrangement // Spaces the temporary destination label and return button.
import androidx.compose.foundation.layout.Column // Holds the temporary destination's minimal content.
import androidx.compose.foundation.layout.fillMaxSize // Lets each destination use the full host area.
import androidx.compose.foundation.layout.padding // Adds the shared screen padding.
import androidx.compose.foundation.layout.safeDrawingPadding // Keeps temporary content outside system bars and cutouts.
import androidx.compose.material3.MaterialTheme // Reuses the app's existing colours and typography.
import androidx.compose.material3.Surface // Gives the navigation host the app's themed background.
import androidx.compose.material3.Text // Displays each temporary destination's label.
import androidx.compose.material3.TextButton // Provides a minimal visible return action before real screens are implemented.
import androidx.compose.runtime.Composable // Marks the host and screen content as Compose UI.
import androidx.compose.ui.Alignment // Centres the temporary destination content.
import androidx.compose.ui.Modifier // Describes the host and content layout.
import androidx.compose.ui.res.stringResource // Resolves destination labels from Android resources.
import androidx.navigation.NavHostController // Receives the controller owned by this host instead of passing it into screens.
import androidx.navigation.NavType // Declares that the active conversation argument is a required string.
import androidx.navigation.compose.NavHost // Registers destinations and integrates the system Back action.
import androidx.navigation.compose.composable // Adds each Compose destination to the graph.
import androidx.navigation.compose.rememberNavController // Retains navigation state across recomposition and saved-state restoration.
import androidx.navigation.navArgument // Describes the active conversation's required route argument.
import pt.isel.pdm.droidmentor.R // Resolves app and destination label resources.
import pt.isel.pdm.droidmentor.ui.screens.title.TitleScreen // Supplies the real main menu while other screen tasks remain separate.
import pt.isel.pdm.droidmentor.ui.theme.DroidMentorSpacing // Uses the existing shared spacing values.

/**
 * Hosts the M2 graph with Title as its initial destination and callbacks matching the assignment's Figure 1.
 *
 * Content slots keep this task limited to navigation: later screen tasks replace the minimal placeholders.
 * Screens receive events and a local chat ID, never the controller or a network result.
 * @param navController The host's controller; normally created and saved by Compose.
 * @param titleContent Content receiving history, settings and about navigation callbacks, in that order.
 * @param chatHistoryContent Content receiving chat selection and Back callbacks, in that order.
 * @param activeChatContent Content receiving the decoded local chat ID and the Back callback.
 * @param aboutContent Content receiving the Back callback to the previous Title destination.
 * @param settingsContent Content receiving the Back callback to the previous Title destination.
 */
@Composable // Displays the graph inside the app's existing theme.
fun DroidMentorNavHost( // Allows screen tasks and tests to supply content without duplicating graph logic.
    navController: NavHostController = rememberNavController(), // Uses Compose's saved controller instead of a process-wide navigation singleton.
    titleContent: @Composable (() -> Unit, () -> Unit, () -> Unit) -> Unit = { onHistory, onSettings, onAbout -> // Supplies the main menu with the graph's three navigation events.
        TitleScreen(onHistory, onSettings, onAbout) // Keeps navigation ownership in the graph and menu layout in its screen package.
    }, // Ends the real Title content.
    chatHistoryContent: @Composable ((String) -> Unit, () -> Unit) -> Unit = { _, onBack -> // Reserves chat selection for the Room-backed history screen task.
        NavigationPlaceholder(stringResource(R.string.navigation_chat_history), onBack) // Displays the destination with its actual return callback.
    }, // Ends the temporary history content.
    activeChatContent: @Composable (String, () -> Unit) -> Unit = { _, onBack -> // Reserves the chat ID for the future message screen and ViewModel.
        NavigationPlaceholder(stringResource(R.string.navigation_active_chat), onBack) // Displays no invented messages or sample conversation data.
    }, // Ends the temporary active conversation content.
    aboutContent: @Composable (() -> Unit) -> Unit = { onBack -> // Reserves information content for its separate screen task.
        NavigationPlaceholder(stringResource(R.string.navigation_about), onBack) // Keeps the return path available without adding future app credits.
    }, // Ends the temporary About content.
    settingsContent: @Composable (() -> Unit) -> Unit = { onBack -> // Reserves key editing for its separate settings task.
        NavigationPlaceholder(stringResource(R.string.navigation_settings), onBack) // Reads no credential and performs no preference operation.
    }, // Ends the temporary Settings content.
) { // Starts the shared navigation graph.
    Surface( // Applies the existing Material theme behind every destination.
        modifier = Modifier.fillMaxSize(), // Fills the Activity's Compose area.
        color = MaterialTheme.colorScheme.background, // Uses the selected light or dark background.
        contentColor = MaterialTheme.colorScheme.onBackground, // Uses matching readable content colours.
    ) { // Hosts the graph inside the themed background.
        NavHost( // Lets AndroidX manage screen lifetime, saved state and the system Back callback.
            navController = navController, // Associates these destinations with exactly one controller.
            startDestination = DroidMentorRoutes.TITLE, // Starts M2 on Title without implementing M3 active-session persistence.
        ) { // Registers all five destinations.
            composable(DroidMentorRoutes.TITLE) { // Keeps Title as the root entry for every normal forward path.
                titleContent( // Supplies only the three forward actions shown in Figure 1.
                    { navController.openDestination(DroidMentorRoutes.CHAT_HISTORY) }, // Opens history above Title.
                    { navController.openDestination(DroidMentorRoutes.SETTINGS) }, // Opens Settings above Title.
                    { navController.openDestination(DroidMentorRoutes.ABOUT) }, // Opens About above Title.
                ) // Ends Title callback binding.
            } // Ends the Title destination.
            composable(DroidMentorRoutes.CHAT_HISTORY) { // Registers the locally saved conversation list.
                chatHistoryContent( // Supplies chat selection and the return action without repository work in this graph.
                    { chatId -> // Receives the local ID of a conversation already created or selected by the history screen.
                        if (chatId.isNotBlank()) { // Ignores an unusable UI selection without changing the back stack.
                            navController.openDestination(DroidMentorRoutes.activeChat(chatId)) // Adds the selected conversation above history with its safely encoded ID.
                        } // Ends the selection guard.
                    }, // Ends active conversation navigation.
                    { navController.popBackStack() }, // Returns to the existing Title entry instead of creating another one.
                ) // Ends history callback binding.
            } // Ends the history destination.
            composable( // Registers a specific conversation rather than a global active-chat singleton.
                route = DroidMentorRoutes.ACTIVE_CHAT, // Uses the pattern shared with route construction.
                arguments = listOf(navArgument(DroidMentorRoutes.CHAT_ID) { type = NavType.StringType }), // Requires the local ID and lets Navigation decode its route segment.
            ) { entry -> // Receives this conversation's saved back-stack arguments.
                val chatId = requireNotNull(entry.arguments?.getString(DroidMentorRoutes.CHAT_ID)) // Reads the required decoded ID without asserting with double exclamation marks.
                activeChatContent(chatId) { navController.popBackStack() } // Returns to the existing history entry when the conversation is explicitly left.
            } // Ends the active conversation destination.
            composable(DroidMentorRoutes.ABOUT) { // Registers application information separately from Title.
                aboutContent { navController.popBackStack() } // Returns to the existing Title entry.
            } // Ends the About destination.
            composable(DroidMentorRoutes.SETTINGS) { // Registers credential settings without passing a key in its route.
                settingsContent { navController.popBackStack() } // Returns to the existing Title entry.
            } // Ends the Settings destination.
        } // Ends destination registration.
    } // Ends the themed navigation surface.
} // Ends the M2 navigation host.

private fun NavHostController.openDestination(route: String) { // Keeps the same small forward-navigation policy for each callback.
    navigate(route) { // Adds a forward destination without deleting the route it should return to.
        launchSingleTop = true // Repeated events for the same top destination do not add duplicate back-stack entries.
    } // Ends the forward navigation options.
} // Ends the shared navigation helper.

@Composable // Displays only minimal content until each screen's own M2 task is completed.
private fun NavigationPlaceholder(title: String, onBack: (() -> Unit)? = null) { // Keeps temporary UI separate from route and back-stack logic.
    Column( // Groups the destination label and optional Back action.
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(DroidMentorSpacing.large), // Keeps content centred within the safe, padded screen area.
        verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.medium, Alignment.CenterVertically), // Gives the return action a clear gap below the label.
        horizontalAlignment = Alignment.CenterHorizontally, // Aligns temporary content in the centre of the screen.
    ) { // Starts the minimal destination content.
        Text(title, style = MaterialTheme.typography.headlineMedium) // Displays only the app or destination name.
        if (onBack != null) { // Leaves the Title root without an in-app return action.
            TextButton(onClick = onBack) { // Uses the graph's actual pop action instead of navigating to a new Title.
                Text(stringResource(R.string.navigation_back)) // Reads the visible return label from resources.
            } // Ends the temporary Back button.
        } // Ends optional return content.
    } // Ends the temporary destination layout.
} // Ends the minimal navigation placeholder.
