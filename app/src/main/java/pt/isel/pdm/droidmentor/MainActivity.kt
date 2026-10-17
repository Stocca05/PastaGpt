package pt.isel.pdm.droidmentor // Groups the main Activity code.

import android.os.Bundle // Holds the saved state passed to the Activity.
import androidx.activity.ComponentActivity // Base Activity class that supports Compose.
import androidx.activity.compose.setContent // Lets the Activity display Compose content.
import androidx.activity.enableEdgeToEdge // Lets the app background extend behind the system bars.
import androidx.compose.runtime.Composable // Marks functions that describe Compose UI.
import androidx.compose.ui.tooling.preview.Preview // Shows a composable preview in Android Studio.
import pt.isel.pdm.droidmentor.ui.navigation.DroidMentorNavHost // Displays the shared five-destination M2 graph.
import pt.isel.pdm.droidmentor.ui.theme.DroidMentorTheme // Applies our app theme to the UI.

/** Hosts the Compose UI and the shared DroidMentor theme. */
class MainActivity : ComponentActivity() { // Our main screen inherits the Android Activity behaviour.
    /** Creates the activity's initial themed content. */
    override fun onCreate(savedInstanceState: Bundle?) { // Android calls this when it creates the Activity.
        super.onCreate(savedInstanceState) // Initialises the Activity using its parent implementation.
        enableEdgeToEdge() // Allows the background to fill the screen behind the system bars.
        setContent { // Starts the Compose content displayed by this Activity.
            DroidMentorTheme { // Uses our colours and text styles for the content below.
                DroidMentorNavHost() // Displays the five-screen graph, starting at the main menu.
            } // Ends this block.
        } // Ends this block.
    } // Ends this block.
} // Ends this block.

@Preview(name = "Light theme", showBackground = true) // Adds a light-theme preview in Android Studio.
@Composable // This function describes UI that Compose can draw.
private fun LightThemePreview() { // Defines the content used by the light preview.
    DroidMentorTheme(darkTheme = false) { // Forces light colours for this preview.
        DroidMentorNavHost() // Displays the five-screen graph, starting at the main menu.
    } // Ends this block.
} // Ends this block.

@Preview(name = "Dark theme", showBackground = true) // Adds a dark-theme preview in Android Studio.
@Composable // This function describes UI that Compose can draw.
private fun DarkThemePreview() { // Defines the content used by the dark preview.
    DroidMentorTheme(darkTheme = true) { // Forces dark colours for this preview.
        DroidMentorNavHost() // Displays the five-screen graph, starting at the main menu.
    } // Ends this block.
} // Ends this block.
