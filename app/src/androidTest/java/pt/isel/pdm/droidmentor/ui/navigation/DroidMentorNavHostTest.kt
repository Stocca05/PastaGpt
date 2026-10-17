package pt.isel.pdm.droidmentor.ui.navigation // Tests the production graph with harmless test-only screen controls.

import androidx.compose.foundation.layout.Column // Separates test controls so each actual callback can be clicked independently.
import androidx.compose.material3.Button // Lets tests trigger the actual callbacks supplied by the graph.
import androidx.compose.material3.Text // Identifies test destinations and the decoded chat ID.
import androidx.compose.runtime.Composable // Defines simple test-only destination content.
import androidx.compose.ui.test.junit4.StateRestorationTester // Recreates saved Compose state without adding active-session persistence.
import androidx.compose.ui.test.junit4.createComposeRule // Hosts the production NavHost in the existing Android test activity.
import androidx.compose.ui.test.onNodeWithText // Finds screen labels and navigation controls in the Compose tree.
import androidx.compose.ui.test.performClick // Exercises forward and return actions as UI events.
import androidx.navigation.NavHostController // Inspects the controller owned by the production graph.
import androidx.navigation.compose.rememberNavController // Uses the same saved controller creation as the real host.
import androidx.test.espresso.Espresso // Sends the actual Android Back action to the host activity.
import androidx.test.ext.junit.runners.AndroidJUnit4 // Runs Navigation Compose against real Android classes.
import org.junit.Assert.assertEquals // Checks destination patterns and preserved local ID values.
import org.junit.Assert.assertNull // Checks Title has no earlier screen entry to return to.
import org.junit.Assert.assertThrows // Checks unusable direct route construction fails before navigation.
import org.junit.Rule // Exposes the Compose activity lifecycle to JUnit.
import org.junit.Test // Marks each navigation acceptance case.
import org.junit.runner.RunWith // Selects the Android test runner.
import pt.isel.pdm.droidmentor.ui.theme.DroidMentorTheme // Applies the same theme used by MainActivity.

/** Verifies Figure 1 paths, real Back events, duplicate guards, ID encoding and saved controller restoration. */
@RunWith(AndroidJUnit4::class) // Executes the real graph without accessing Room, DataStore or Gemini.
class DroidMentorNavHostTest { // Test content contains controls only, never fake app history or preference values.
    /** Hosts each isolated graph in a Compose-enabled test activity. */
    @get:Rule // Creates and closes a separate activity for every case.
    val compose = createComposeRule() // Uses existing Compose test dependencies rather than a new navigation test library.

    private lateinit var controller: NavHostController // Captures the controller created inside the test composition.
    private var selectChat: (String) -> Unit = {} // Captures the history callback for blank and repeated selection tests.
    private var openHistory: () -> Unit = {} // Captures the Title callback for repeated forward-event tests.
    private var selectedId = "navigation-test-chat" // Holds only a harmless opaque local test ID.

    /** Checks the default entry is Title and all five route patterns exist in the graph. */
    @Test // Detects a missing destination or an incorrect start route.
    fun graphStartsOnTitleAndRegistersAllFiveDestinations() { // Sets up the production graph with test-only screen labels.
        showGraph() // Creates one remembered controller and the actual NavHost.
        assertScreen("Title screen", DroidMentorRoutes.TITLE) // Checks the first displayed destination and controller entry.
        compose.runOnIdle { // Reads the graph after Compose completed registration.
            assertEquals( // Compares the registered patterns rather than controller-generated integer IDs.
                setOf(DroidMentorRoutes.TITLE, DroidMentorRoutes.CHAT_HISTORY, DroidMentorRoutes.ACTIVE_CHAT, DroidMentorRoutes.ABOUT, DroidMentorRoutes.SETTINGS), // Lists exactly the five M2 destinations.
                controller.graph.map { it.route }.toSet(), // Reads the production graph's actual child destinations.
            ) // Ends registered destination comparison.
            assertNull(controller.previousBackStackEntry) // Checks Title is the root instead of a duplicate screen entry.
        } // Ends graph inspection.
    } // Ends start and registration verification.

    /** Checks each Title branch returns to the existing Title entry through its supplied Back callback. */
    @Test // Covers the three bidirectional branches drawn in Figure 1.
    fun titleBranchesReturnWithoutAddingAnotherTitle() { // Visits every menu branch using real graph callbacks.
        showGraph() // Starts a fresh graph at Title.
        val branches = listOf( // Keeps the three equivalent Title branch checks together.
            Triple("Open history", "History screen", DroidMentorRoutes.CHAT_HISTORY), // Covers the saved conversation list path.
            Triple("Open settings", "Settings screen", DroidMentorRoutes.SETTINGS), // Covers the settings path without any key access.
            Triple("Open about", "About screen", DroidMentorRoutes.ABOUT), // Covers the application information path.
        ) // Ends the branch cases.
        for ((button, screen, route) in branches) { // Exercises each branch from the same root entry.
            compose.onNodeWithText(button).performClick() // Invokes the actual forward callback passed to Title content.
            assertScreen(screen, route) // Checks the destination was opened.
            compose.onNodeWithText("Return").performClick() // Invokes that destination's actual pop callback.
            assertScreen("Title screen", DroidMentorRoutes.TITLE) // Checks the return displays the initial screen.
            compose.runOnIdle { assertNull(controller.previousBackStackEntry) } // Checks the return did not push another Title onto the stack.
        } // Ends branch round-trip verification.
    } // Ends Title branch verification.

    /** Checks an encoded chat selection returns to history and then Title without losing the original local ID. */
    @Test // Covers the history-to-active-chat path and its two return steps.
    fun selectedChatPreservesItsIdAndReturnsThroughHistory() { // Uses reserved characters that must stay inside one route argument.
        selectedId = "local/chat?part=#50% ü" // Contains no history text or credential, only a test ID.
        showGraph() // Creates the production graph.
        compose.onNodeWithText("Open history").performClick() // Enters history through Title's callback.
        compose.onNodeWithText("Select chat").performClick() // Sends the opaque ID through the actual history callback.
        assertScreen("Active screen", DroidMentorRoutes.ACTIVE_CHAT) // Checks the argument route matched the active destination.
        compose.onNodeWithText("Local ID: $selectedId").assertExists() // Checks screen content received the exact decoded ID.
        compose.runOnIdle { // Reads the entry arguments on the host's main thread.
            assertEquals(selectedId, controller.currentBackStackEntry?.arguments?.getString(DroidMentorRoutes.CHAT_ID)) // Checks Navigation kept the local ID unchanged.
        } // Ends argument inspection.
        compose.onNodeWithText("Return").performClick() // Leaves the active chat through the graph's explicit return callback.
        assertScreen("History screen", DroidMentorRoutes.CHAT_HISTORY) // Checks the saved list remains the previous entry.
        compose.onNodeWithText("Return").performClick() // Leaves history without pushing a new Title.
        assertScreen("Title screen", DroidMentorRoutes.TITLE) // Checks the second return reaches the initial root.
    } // Ends active conversation round-trip verification.

    /** Checks Android's Back action follows the same return paths as explicit screen callbacks. */
    @Test // Covers the actual system callback installed by NavHost.
    fun systemBackReturnsFromActiveChatAndTitleBranches() { // Exercises the hardware or gesture equivalent through Espresso.
        showGraph() // Starts the host in a real Android activity.
        compose.onNodeWithText("Open history").performClick() // Adds history above Title.
        compose.onNodeWithText("Select chat").performClick() // Adds the active chat above history.
        assertScreen("Active screen", DroidMentorRoutes.ACTIVE_CHAT) // Waits for navigation before dispatching Back.
        Espresso.pressBack() // Sends one Android Back event to NavHost.
        assertScreen("History screen", DroidMentorRoutes.CHAT_HISTORY) // Checks active chat returns to history.
        Espresso.pressBack() // Sends the next Back event.
        assertScreen("Title screen", DroidMentorRoutes.TITLE) // Checks history returns to the root.
        compose.onNodeWithText("Open settings").performClick() // Opens the settings branch.
        assertScreen("Settings screen", DroidMentorRoutes.SETTINGS) // Waits for that branch to settle.
        Espresso.pressBack() // Uses the same system return action for Settings.
        assertScreen("Title screen", DroidMentorRoutes.TITLE) // Checks no custom Back interception is needed.
        compose.onNodeWithText("Open about").performClick() // Opens the information branch.
        assertScreen("About screen", DroidMentorRoutes.ABOUT) // Waits for the information screen.
        Espresso.pressBack() // Returns through the Navigation component.
        assertScreen("Title screen", DroidMentorRoutes.TITLE) // Checks every branch returns correctly.
    } // Ends system Back verification.

    /** Checks repeated events for the same destination do not create duplicate history or active-chat entries. */
    @Test // Guards against rapid repeated taps making Back appear ineffective.
    fun repeatedForwardEventsDoNotDuplicateTheTopDestination() { // Invokes each production callback twice before another frame is drawn.
        showGraph() // Creates a graph with the launchSingleTop navigation policy.
        compose.runOnIdle { // Triggers rapid duplicate events on the controller's required main thread.
            openHistory() // Requests history once.
            openHistory() // Repeats the same event before screen recomposition.
        } // Ends repeated history events.
        assertScreen("History screen", DroidMentorRoutes.CHAT_HISTORY) // Checks history is displayed normally.
        compose.runOnIdle { // Triggers duplicate selection for the same chat.
            selectChat(selectedId) // Opens the selected local conversation once.
            selectChat(selectedId) // Repeats the same event before the next frame.
        } // Ends repeated chat events.
        assertScreen("Active screen", DroidMentorRoutes.ACTIVE_CHAT) // Checks the active screen is displayed.
        compose.onNodeWithText("Return").performClick() // Returns once from the active destination.
        assertScreen("History screen", DroidMentorRoutes.CHAT_HISTORY) // Checks there was no duplicate active entry.
        compose.onNodeWithText("Return").performClick() // Returns once from history.
        assertScreen("Title screen", DroidMentorRoutes.TITLE) // Checks there was no duplicate history entry either.
    } // Ends duplicate event verification.

    /** Checks blank IDs supplied by screen events leave history and its back stack unchanged. */
    @Test // Protects the user-facing callback from malformed selections.
    fun blankSelectionDoesNotNavigateOrInvokeActiveContent() { // Supplies both empty and whitespace-only selections.
        showGraph() // Starts a graph without any persisted conversation dependency.
        compose.onNodeWithText("Open history").performClick() // Enters the only screen that selects conversations.
        assertScreen("History screen", DroidMentorRoutes.CHAT_HISTORY) // Waits until its callback is available.
        compose.runOnIdle { // Calls the UI selection callback directly with invalid input.
            selectChat("") // Rejects an empty ID without trying to build a route.
            selectChat("   ") // Rejects a whitespace-only ID without changing destinations.
        } // Ends invalid selection events.
        assertScreen("History screen", DroidMentorRoutes.CHAT_HISTORY) // Checks the graph stayed on history.
        compose.onNodeWithText("Return").performClick() // Checks its original return path is still intact.
        assertScreen("Title screen", DroidMentorRoutes.TITLE) // Checks no hidden active entry was added.
    } // Ends invalid selection verification.

    /** Checks the navigation controller restores its current ID and prior destinations from saved Compose state. */
    @Test // Covers Activity-style saved-state restoration without implementing M3 DataStore session persistence.
    fun savedControllerRestoresTheChatAndItsReturnStack() { // Recreates the composition with the controller's saved state.
        selectedId = "encoded%2F/local" // Checks an already percent-like ID is decoded only once after restoration.
        val restoration = StateRestorationTester(compose) // Controls disposal and restoration of the saved composition.
        restoration.setContent { GraphContent() } // Installs the production graph through the restoration harness.
        compose.onNodeWithText("Open history").performClick() // Adds the saved history entry.
        compose.onNodeWithText("Select chat").performClick() // Adds the active conversation and its ID.
        assertScreen("Active screen", DroidMentorRoutes.ACTIVE_CHAT) // Waits for the complete navigation state.
        restoration.emulateSavedInstanceStateRestore() // Disposes the composition and supplies its saved state to a fresh one.
        assertScreen("Active screen", DroidMentorRoutes.ACTIVE_CHAT) // Checks restoration did not reset this saved Activity state to Title.
        compose.onNodeWithText("Local ID: $selectedId").assertExists() // Checks the exact original local ID survived restoration.
        compose.onNodeWithText("Return").performClick() // Leaves the restored active destination.
        assertScreen("History screen", DroidMentorRoutes.CHAT_HISTORY) // Checks the earlier history entry was restored too.
        compose.onNodeWithText("Return").performClick() // Leaves the restored history destination.
        assertScreen("Title screen", DroidMentorRoutes.TITLE) // Checks the root entry remains available.
    } // Ends saved controller state verification.

    /** Checks direct callers cannot build a route with a blank conversation ID. */
    @Test // Covers programmer mistakes independently of the UI callback guard.
    fun directRouteConstructionRejectsBlankIds() { // Uses the Android URI encoder only with valid input.
        for (id in listOf("", " \n\t")) { // Checks the two blank input shapes without any credential data.
            assertThrows(IllegalArgumentException::class.java) { DroidMentorRoutes.activeChat(id) } // Requires direct route construction to fail before changing a controller.
        } // Ends invalid route cases.
    } // Ends route precondition verification.

    private fun showGraph() { // Installs the same graph for ordinary tests.
        compose.setContent { GraphContent() } // Keeps graph and controller ownership inside the Compose activity.
    } // Ends ordinary test content setup.

    @Composable // Supplies test-only controls while exercising the unmodified production graph.
    private fun GraphContent() { // Contains no repository, preferences, message or network implementation.
        controller = rememberNavController() // Creates a controller with the production save-and-restore behaviour.
        DroidMentorTheme { // Matches the real host's Material theme.
            DroidMentorNavHost( // Tests route registration and callback construction from the actual app source.
                navController = controller, // Allows main-thread assertions on the real controller state.
                titleContent = { onHistory, onSettings, onAbout -> // Receives the graph's actual Title events.
                    Column { // Places labels and buttons in separate rows rather than overlapping host children.
                        openHistory = onHistory // Captures the history callback for the rapid-event case.
                        Text("Title screen") // Labels test content without implementing the future Title screen.
                        Button(onClick = onHistory) { Text("Open history") } // Invokes the real history transition.
                        Button(onClick = onSettings) { Text("Open settings") } // Invokes the real settings transition.
                        Button(onClick = onAbout) { Text("Open about") } // Invokes the real information transition.
                    } // Ends the test destination control layout.
                }, // Ends the test-only Title controls.
                chatHistoryContent = { onSelect, onBack -> // Receives actual selection and return events.
                    Column { // Places labels and buttons in separate rows rather than overlapping host children.
                        selectChat = onSelect // Captures the selection callback for invalid and duplicate events.
                        Text("History screen") // Labels the history destination independently of real stored rows.
                        Button(onClick = { onSelect(selectedId) }) { Text("Select chat") } // Supplies only the harmless test ID through the production event.
                        Button(onClick = onBack) { Text("Return") } // Uses the graph's real popBackStack callback.
                    } // Ends the test destination control layout.
                }, // Ends the test-only history controls.
                activeChatContent = { id, onBack -> // Receives the decoded ID from the actual graph argument.
                    Column { // Places labels and buttons in separate rows rather than overlapping host children.
                        Text("Active screen") // Labels the active destination without starting a send operation.
                        Text("Local ID: $id") // Makes the harmless decoded ID observable to the test.
                        Button(onClick = onBack) { Text("Return") } // Uses the real active-to-history return callback.
                    } // Ends the test destination control layout.
                }, // Ends the test-only active chat content.
                aboutContent = { onBack -> // Receives the actual information return callback.
                    Column { // Places labels and buttons in separate rows rather than overlapping host children.
                        Text("About screen") // Labels the information destination without adding future app credits.
                        Button(onClick = onBack) { Text("Return") } // Uses the real About-to-Title return callback.
                    } // Ends the test destination control layout.
                }, // Ends the test-only information content.
                settingsContent = { onBack -> // Receives the actual settings return callback.
                    Column { // Places labels and buttons in separate rows rather than overlapping host children.
                        Text("Settings screen") // Labels settings without reading or changing a key.
                        Button(onClick = onBack) { Text("Return") } // Uses the real Settings-to-Title return callback.
                    } // Ends the test destination control layout.
                }, // Ends the test-only settings content.
            ) // Ends production graph setup with test-only content.
        } // Ends the test theme.
    } // Ends the test-only screen controls.

    private fun assertScreen(label: String, route: String) { // Checks both visible destination content and actual navigation state.
        compose.onNodeWithText(label).assertExists() // Waits for Compose idleness and checks the destination is rendered.
        compose.runOnIdle { assertEquals(route, controller.currentDestination?.route) } // Reads the real top entry on the controller's main thread.
    } // Ends destination assertions.
} // Ends M2 navigation graph tests.
