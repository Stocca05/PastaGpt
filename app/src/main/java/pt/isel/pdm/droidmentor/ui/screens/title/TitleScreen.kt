package pt.isel.pdm.droidmentor.ui.screens.title // Groups the app's main menu content.

import androidx.compose.foundation.layout.Arrangement // Gives the menu consistent spacing between hero and card actions.
import androidx.compose.foundation.layout.Box // Centres the app mark inside its circular hero container.
import androidx.compose.foundation.layout.Column // Places hero, card menu and footer in vertical reading order.
import androidx.compose.foundation.layout.Spacer // Adds subtle icon-to-label spacing inside menu buttons.
import androidx.compose.foundation.layout.fillMaxSize // Uses the full screen area.
import androidx.compose.foundation.layout.fillMaxWidth // Makes menu actions easy to tap.
import androidx.compose.foundation.layout.height // Ensures generous >=52dp menu button touch targets.
import androidx.compose.foundation.layout.padding // Applies outer screen and inner card padding.
import androidx.compose.foundation.layout.safeDrawingPadding // Keeps content away from system bars and cutouts.
import androidx.compose.foundation.layout.size // Sizes the hero badge and button icons.
import androidx.compose.foundation.layout.width // Adds gap between button icon and label.
import androidx.compose.foundation.layout.widthIn // Keeps card comfortable on tablets while filling narrow phones.
import androidx.compose.foundation.rememberScrollState // Remembers the menu's scroll position when space is limited.
import androidx.compose.foundation.shape.CircleShape // Gives the mentor emblem a crisp circular badge.
import androidx.compose.foundation.shape.RoundedCornerShape // Rounds hero card and buttons cleanly.
import androidx.compose.foundation.verticalScroll // Keeps all actions reachable in landscape or with large text.
import androidx.compose.material3.Button // Emphasises the primary Conversations destination.
import androidx.compose.material3.ElevatedCard // Groups the three destinations in a modern lifted surface.
import androidx.compose.material3.FilledTonalButton // Styles Settings cleanly as secondary action.
import androidx.compose.material3.Icon // Renders crisp vector icons on hero and menu buttons.
import androidx.compose.material3.MaterialTheme // Uses the existing typography and theme palette.
import androidx.compose.material3.OutlinedButton // Styles About cleanly without competing with Conversations.
import androidx.compose.material3.Surface // Frames the hero icon badge and footer cleanly.
import androidx.compose.material3.Text // Displays resource-backed menu labels.
import androidx.compose.runtime.Composable // Marks the function as Compose screen content.
import androidx.compose.ui.Alignment // Centres the hero and menu column cleanly.
import androidx.compose.ui.Modifier // Describes layout constraints and padding.
import androidx.compose.ui.res.painterResource // Loads local vector icons without extra libraries.
import androidx.compose.ui.res.stringResource // Resolves destination labels and subtitle from strings.xml.
import androidx.compose.ui.text.font.FontWeight // Emphasises title and primary action cleanly.
import androidx.compose.ui.text.style.TextAlign // Centres wrapped headings cleanly.
import androidx.compose.ui.unit.dp // Specifies clean Material 3 dimensions.
import pt.isel.pdm.droidmentor.R // Provides app strings and drawable icons.
import pt.isel.pdm.droidmentor.ui.theme.DroidMentorSpacing // Reuses shared app spacing.

/** Displays the polished main menu; navigation is supplied through events rather than a controller. */
@Composable // Draws the same stateless menu in light and dark themes.
fun TitleScreen( // Receives only the three actions belonging to the Title destination.
    onConversations: () -> Unit, // Opens the saved conversation list through the graph.
    onSettings: () -> Unit, // Opens the key configuration destination through the graph.
    onAbout: () -> Unit, // Opens the application information destination through the graph.
) { // Starts the main menu layout.
    Column( // Centres hero identity, elevated action card and subtle course footer.
        modifier = Modifier // Builds the screen layout in readable steps.
            .fillMaxSize() // Uses full screen area.
            .safeDrawingPadding() // Respects status/navigation bars and cutouts.
            .verticalScroll(rememberScrollState()) // Keeps every action reachable on rotation or large fonts.
            .padding(DroidMentorSpacing.large), // Adds outer breathing room.
        horizontalAlignment = Alignment.CenterHorizontally, // Centres hero and card horizontally.
        verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.large, Alignment.CenterVertically), // Centres short screens naturally.
    ) { // Renders hero header followed by action card.
        Surface( // Frames the app mark inside a soft circular container.
            shape = CircleShape, // Forms a crisp circular emblem.
            color = MaterialTheme.colorScheme.primaryContainer, // Matches theme accent cleanly.
            tonalElevation = 4.dp, // Lifts the hero badge subtly from background.
            modifier = Modifier.size(96.dp), // Sets comfortable hero badge size.
        ) { // Centres vector mark inside the circle.
            Box(contentAlignment = Alignment.Center) { // Aligns emblem centrally.
                Icon( // Reuses the local mentor conversation mark.
                    painter = painterResource(R.drawable.ic_mentor), // Loads conversation mark.
                    contentDescription = null, // Decorative because app title directly labels it below.
                    tint = MaterialTheme.colorScheme.onPrimaryContainer, // Ensures accessible contrast on primaryContainer.
                    modifier = Modifier.size(48.dp), // Balanced inner emblem size.
                ) // Ends hero icon.
            } // Ends centered Box.
        } // Ends circular hero badge.
        Column( // Groups title and subtitle tightly below badge.
            horizontalAlignment = Alignment.CenterHorizontally, // Centres title text.
            verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small), // Subtle vertical gap.
        ) { // Renders heading and supporting tagline.
            Text( // Displays application name strongly.
                text = stringResource(R.string.app_name), // Uses shared app name.
                style = MaterialTheme.typography.headlineLarge, // Strong heading scale.
                fontWeight = FontWeight.Bold, // Highlights app identity.
                textAlign = TextAlign.Center, // Centres wrapped heading cleanly.
            ) // Ends app title.
            Text( // Displays mentor tagline calmly.
                text = stringResource(R.string.title_subtitle), // Reads subtitle from resources.
                style = MaterialTheme.typography.bodyLarge, // Readable supporting size.
                color = MaterialTheme.colorScheme.onSurfaceVariant, // Softer hierarchy below headline.
                textAlign = TextAlign.Center, // Centres wrapped lines cleanly.
            ) // Ends supporting tagline.
        } // Ends title group.
        ElevatedCard( // Groups navigation buttons inside a modern rounded card.
            shape = RoundedCornerShape(24.dp), // Smooth modern card corners.
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(), // Caps card width on tablets.
        ) { // Displays each navigation action cleanly inside padded card.
            Column( // Arranges the three destination buttons with uniform spacing.
                modifier = Modifier.padding(DroidMentorSpacing.large), // Comfortable inner card padding.
                verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.medium), // Clear separation between touch targets.
            ) { // Renders Conversations, Settings and About buttons.
                val buttonShape = RoundedCornerShape(16.dp) // Uniform rounded button corners.
                Button( // Primary CTA leading to Conversations.
                    onClick = onConversations, // Opens ChatHistory destination.
                    shape = buttonShape, // Applies soft rounded shape.
                    modifier = Modifier.fillMaxWidth().height(52.dp), // Generous primary touch target.
                ) { // Places chat icon beside Conversations label.
                    Icon(painterResource(R.drawable.ic_chat_bubble), contentDescription = null, modifier = Modifier.size(20.dp)) // Decorative leading icon.
                    Spacer(Modifier.width(DroidMentorSpacing.small)) // Separates icon from label.
                    Text(stringResource(R.string.navigation_chat_history), style = MaterialTheme.typography.titleMedium) // Clear readable CTA.
                } // Ends Conversations button.
                FilledTonalButton( // Secondary tonal CTA leading to Settings.
                    onClick = onSettings, // Opens Settings destination.
                    shape = buttonShape, // Matches primary button silhouette.
                    modifier = Modifier.fillMaxWidth().height(52.dp), // Generous touch target.
                ) { // Places gear icon beside Settings label.
                    Icon(painterResource(R.drawable.ic_settings), contentDescription = null, modifier = Modifier.size(20.dp)) // Decorative settings icon.
                    Spacer(Modifier.width(DroidMentorSpacing.small)) // Separates icon from label.
                    Text(stringResource(R.string.navigation_settings), style = MaterialTheme.typography.titleMedium) // Clear label.
                } // Ends Settings button.
                OutlinedButton( // Subtle outlined action leading to About.
                    onClick = onAbout, // Opens About destination.
                    shape = buttonShape, // Matches sibling buttons.
                    modifier = Modifier.fillMaxWidth().height(52.dp), // Generous touch target.
                ) { // Places info icon beside About label.
                    Icon(painterResource(R.drawable.ic_info), contentDescription = null, modifier = Modifier.size(20.dp)) // Decorative info icon.
                    Spacer(Modifier.width(DroidMentorSpacing.small)) // Separates icon from label.
                    Text(stringResource(R.string.navigation_about), style = MaterialTheme.typography.titleMedium) // Clear label.
                } // Ends About button.
            } // Ends inner card column.
        } // Ends ElevatedCard.
        Text( // Subtle educational course label below card.
            text = stringResource(R.string.title_footer), // Reads subtle footer string.
            style = MaterialTheme.typography.labelMedium, // Quiet footnote style.
            color = MaterialTheme.colorScheme.onSurfaceVariant, // Calm low-contrast caption.
            textAlign = TextAlign.Center, // Centres on wrap.
        ) // Ends footer caption.
    } // Ends scrollable main menu.
} // Ends Title screen.
