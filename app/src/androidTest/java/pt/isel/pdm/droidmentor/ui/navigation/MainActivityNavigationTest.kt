package pt.isel.pdm.droidmentor.ui.navigation // Checks the real Activity uses the new graph's default Title content.

import androidx.compose.ui.test.junit4.createAndroidComposeRule // Launches MainActivity through the actual app manifest.
import androidx.compose.ui.test.onNodeWithText // Reads the app's visible root destination label.
import androidx.test.ext.junit.runners.AndroidJUnit4 // Executes against the installed application.
import org.junit.Rule // Owns the Activity lifecycle through the Compose rule.
import org.junit.Test // Marks the Activity-to-graph integration test.
import org.junit.runner.RunWith // Selects the Android test runner.
import pt.isel.pdm.droidmentor.MainActivity // Uses the production host instead of a test-only Activity.

/** Checks MainActivity displays the default navigation root before real screen content is implemented. */
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
    } // Ends actual Activity root verification.
} // Ends Activity-to-navigation integration verification.
