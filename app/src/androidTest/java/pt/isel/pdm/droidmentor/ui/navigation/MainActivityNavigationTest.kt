package pt.isel.pdm.droidmentor.ui.navigation // Checks the real Activity uses the new graph's default Title content.

import androidx.compose.ui.test.junit4.createAndroidComposeRule // Launches MainActivity through the actual app manifest.
import androidx.compose.ui.test.onNodeWithText // Reads the app's visible root destination label.
import androidx.compose.ui.test.performClick // Exercises the real menu rather than replacing it with test content.
import androidx.test.ext.junit.runners.AndroidJUnit4 // Executes against the installed application.
import org.junit.Rule // Owns the Activity lifecycle through the Compose rule.
import org.junit.Test // Marks the Activity-to-graph integration test.
import org.junit.runner.RunWith // Selects the Android test runner.
import pt.isel.pdm.droidmentor.MainActivity // Uses the production host instead of a test-only Activity.

/** Checks MainActivity displays the real Title menu and forwards its three navigation events. */
@RunWith(AndroidJUnit4::class) // Uses the real app theme, manifest and Activity content.
class MainActivityNavigationTest { // Does not access any configured key or saved history.
    /** Launches the app's actual Compose Activity. */
    @get:Rule // Starts and closes MainActivity for this integration check.
    val compose = createAndroidComposeRule<MainActivity>() // Keeps the production content already installed by onCreate.

    /** Checks the M2 graph starts with its default Title content and has no in-app Back button at the root. */
    @Test // Detects incorrect host wiring or a non-Title start destination.
    fun actualActivityStartsOnTheDefaultTitleContent() { // Reads the actual screen without replacing its composition.
        compose.onNodeWithText("DroidMentor").assertExists() // Checks the graph's initial app label is visible.
        compose.onNodeWithText("Back").assertDoesNotExist() // Checks the root has no return action to a nonexistent earlier screen.
        compose.onNodeWithText("Conversations").assertExists() // Checks the main saved-history action is available.
        compose.onNodeWithText("Settings").assertExists() // Checks the configuration action is available.
        compose.onNodeWithText("About").assertExists() // Checks the app information action is available.
    } // Ends actual Activity root verification.

    /** Checks each real menu button opens its destination and returns to the same root menu. */
    @Test // Protects the callback bindings used by the production Title screen.
    fun realMenuActionsOpenTheirDestinations() { // Performs only navigation without accessing history or keys.
        for (label in listOf("Conversations", "Settings", "About")) { // Exercises the three required actions one at a time.
            compose.onNodeWithText(label).performClick() // Invokes the actual Title button's callback.
            compose.onNodeWithText("Back").performClick() // Pops the destination through its actual return action.
            compose.onNodeWithText("DroidMentor").assertExists() // Checks the existing root menu is visible again.
        } // Ends the menu round trips.
    } // Ends real menu callback verification.
} // Ends Activity-to-navigation integration verification.
