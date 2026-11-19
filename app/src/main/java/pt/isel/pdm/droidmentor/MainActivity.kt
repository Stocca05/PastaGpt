package pt.isel.pdm.droidmentor // Groups the main Activity code.

import android.os.Bundle // Holds the saved state passed to the Activity.
import androidx.activity.ComponentActivity // Base Activity class that supports Compose.
import androidx.activity.compose.setContent // Lets the Activity display Compose content.
import androidx.activity.enableEdgeToEdge // Lets the app background extend behind the system bars.
import androidx.compose.foundation.layout.Box // Displays a themed surface while reading the saved session ID.
import androidx.compose.foundation.layout.fillMaxSize // Fills the screen during the brief startup check.
import androidx.compose.material3.MaterialTheme // Uses the app theme background during startup lookup.
import androidx.compose.material3.Surface // Keeps startup transition smooth in both light and dark themes.
import androidx.compose.runtime.Composable // Marks functions that describe Compose UI.
import androidx.compose.runtime.LaunchedEffect // Reads the persisted active session off the main thread at launch.
import androidx.compose.runtime.getValue // Reads Compose state delegates.
import androidx.compose.runtime.mutableStateOf // Tracks whether startup session lookup completed.
import androidx.compose.runtime.remember // Retains the startup destination check across recompositions.
import androidx.compose.runtime.setValue // Updates the initial active chat ID once loaded.
import androidx.compose.ui.Modifier // Fills the Activity window.
import androidx.compose.ui.tooling.preview.Preview // Shows a composable preview in Android Studio.
import androidx.lifecycle.lifecycleScope // Persists and clears activeChatId outside composable scopes.
import kotlinx.coroutines.flow.first // Verifies a restored chat ID still exists in Room before opening it.
import kotlinx.coroutines.launch // Runs DataStore session updates off the main thread.
import pt.isel.pdm.droidmentor.data.repository.ChatReadResult // Checks whether a stored active chat still exists in Room.
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
                var initialActiveChatId by remember { mutableStateOf<String?>(null) } // Holds the validated active chat ID if present.
                var ready by remember { mutableStateOf(false) } // Delays mounting NavHost until the fast DataStore check completes.
                LaunchedEffect(Unit) { // Reads activeChatId off the main thread at Activity launch.
                    val savedId = applicationContext.app.sessionRepository.readActiveChatId() // Reads DataStore without blocking UI.
                    initialActiveChatId = if (!savedId.isNullOrBlank()) { // Confirms the saved chat was not deleted earlier.
                        val read = applicationContext.app.chatRepository.observeChat(savedId).first() // Queries Room safely.
                        if (read is ChatReadResult.Success && read.value != null) { // Restores only an existing saved conversation.
                            savedId // Routes directly to this conversation on launch.
                        } else { // Clears a stale ID whose chat no longer exists.
                            applicationContext.app.sessionRepository.clearActiveChatId() // Prevents opening a missing-chat screen.
                            null // Falls back to Title screen.
                        } // Ends Room existence check.
                    } else null // Starts on Title when no active session was saved.
                    ready = true // Marks conditional startup routing ready.
                } // Ends startup session check.
                if (ready) { // Mounts the navigation graph with conditional start routing.
                    DroidMentorNavHost( // Connects navigation entry/exit events to the dedicated session repository.
                        initialActiveChatId = initialActiveChatId, // Restores Active Chat when saved, or starts on Title.
                        onActiveChatEntered = { chatId -> // Saves activeChatId whenever a conversation is open.
                            lifecycleScope.launch { applicationContext.app.sessionRepository.setActiveChatId(chatId) } // Persists off main thread.
                        }, // Ends entry handler.
                        onActiveChatExited = { // Clears activeChatId only when the user explicitly leaves Active Chat.
                            lifecycleScope.launch { applicationContext.app.sessionRepository.clearActiveChatId() } // Clears off main thread.
                        }, // Ends explicit exit handler.
                    ) // Ends navigation host setup.
                } else { // Shows a themed surface during the brief DataStore read.
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Box(Modifier.fillMaxSize()) } // Avoids flashing Title before restoring Active Chat.
                } // Ends startup gate.
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
