package pt.isel.pdm.droidmentor.ui.screens.title // Groups the app's main menu content.

import androidx.compose.foundation.layout.Arrangement // Gives the menu a consistent gap between elements.
import androidx.compose.foundation.layout.Column // Places the logo, title and actions in a simple vertical layout.
import androidx.compose.foundation.layout.fillMaxSize // Uses the full screen area.
import androidx.compose.foundation.layout.fillMaxWidth // Makes the menu actions easy to tap.
import androidx.compose.foundation.layout.padding // Adds the existing screen padding.
import androidx.compose.foundation.layout.safeDrawingPadding // Keeps content away from system bars.
import androidx.compose.foundation.layout.size // Sets the decorative logo's size.
import androidx.compose.foundation.layout.widthIn // Keeps buttons comfortable on wider screens.
import androidx.compose.foundation.rememberScrollState // Remembers the menu's scroll position when space is limited.
import androidx.compose.foundation.verticalScroll // Keeps all actions reachable in landscape or with large text.
import androidx.compose.material3.Button // Emphasises the main Conversations action.
import androidx.compose.material3.Icon // Draws the small themed conversation mark.
import androidx.compose.material3.MaterialTheme // Uses the existing typography and colours.
import androidx.compose.material3.OutlinedButton // Gives the secondary menu actions the same clear shape.
import androidx.compose.material3.Text // Displays resource-backed menu text.
import androidx.compose.runtime.Composable // Marks the function as Compose screen content.
import androidx.compose.ui.Alignment // Centres the menu horizontally.
import androidx.compose.ui.Modifier // Describes the menu's layout options.
import androidx.compose.ui.res.painterResource // Loads the local vector logo without a new image library.
import androidx.compose.ui.res.stringResource // Resolves the existing destination labels and new subtitle.
import androidx.compose.ui.text.style.TextAlign // Centres the title and subtitle when they wrap.
import androidx.compose.ui.unit.dp // Specifies the logo size and maximum menu width.
import pt.isel.pdm.droidmentor.R // Provides the app's string and drawable resources.
import pt.isel.pdm.droidmentor.ui.theme.DroidMentorSpacing // Reuses the shared screen spacing.

/** Displays the main menu; navigation is supplied through events rather than a controller or repository. */
@Composable // Draws the same stateless menu in either app theme.
fun TitleScreen( // Receives only the three actions belonging to the Title destination.
    onConversations: () -> Unit, // Opens the saved conversation list through the graph.
    onSettings: () -> Unit, // Opens the key configuration destination through the graph.
    onAbout: () -> Unit, // Opens the application information destination through the graph.
) { // Starts the main menu layout.
    Column( // Uses one straightforward layout instead of separate menu models or wrappers.
        modifier = Modifier // Builds the screen layout in readable steps.
            .fillMaxSize() // Uses the host's available area.
            .safeDrawingPadding() // Leaves space for system bars and cutouts.
            .verticalScroll(rememberScrollState()) // Allows scrolling when the content is taller than the screen.
            .padding(DroidMentorSpacing.large), // Adds consistent outer padding.
        horizontalAlignment = Alignment.CenterHorizontally, // Centres the logo and menu controls.
        verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.large, Alignment.CenterVertically), // Separates content while centring shorter menus.
    ) { // Displays the app identity followed by its three actions.
        Icon( // Reuses a small local vector rather than a raster or external asset.
            painter = painterResource(R.drawable.ic_mentor), // Loads the conversation mark.
            contentDescription = null, // Treats the logo as decorative because the visible title names the app.
            tint = MaterialTheme.colorScheme.primary, // Adapts the mark to light and dark themes.
            modifier = Modifier.size(80.dp), // Keeps the logo compact and readable.
        ) // Ends the decorative logo.
        Text( // Displays the application name from its existing resource.
            text = stringResource(R.string.app_name), // Uses one shared app name.
            style = MaterialTheme.typography.headlineLarge, // Gives the app identity the strongest text style.
            textAlign = TextAlign.Center, // Centres the title if it wraps on a narrow screen.
        ) // Ends the title.
        Text( // Briefly explains the app's purpose without implementation details.
            text = stringResource(R.string.title_subtitle), // Reads the mentor description from resources.
            style = MaterialTheme.typography.bodyLarge, // Uses a readable supporting text style.
            textAlign = TextAlign.Center, // Centres the subtitle's wrapped lines.
        ) // Ends the supporting description.
        Column( // Keeps the three navigation actions together.
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(), // Limits button width on tablets while filling smaller screens.
            verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.medium), // Separates individual tap targets.
        ) { // Displays each action explicitly so a student can follow its callback.
            Button(onClick = onConversations, modifier = Modifier.fillMaxWidth()) { // Invokes only the supplied history event.
                Text(stringResource(R.string.navigation_chat_history)) // Names the saved conversation destination.
            } // Ends the main menu action.
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { // Invokes only the supplied settings event.
                Text(stringResource(R.string.navigation_settings)) // Names the configuration destination.
            } // Ends the settings action.
            OutlinedButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) { // Invokes only the supplied information event.
                Text(stringResource(R.string.navigation_about)) // Names the application information destination.
            } // Ends the information action.
        } // Ends the menu actions.
    } // Ends the scrollable main menu.
} // Ends the Title screen.
