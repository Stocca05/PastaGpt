package pt.isel.pdm.droidmentor.ui.screens.about // Groups the application information screen.

import androidx.compose.foundation.layout.Arrangement // Separates cards cleanly.
import androidx.compose.foundation.layout.Box // Centres app emblem inside circular badge.
import androidx.compose.foundation.layout.Column // Displays hero and information cards in reading order.
import androidx.compose.foundation.layout.Row // Places mentor badge beside app title and version.
import androidx.compose.foundation.layout.fillMaxSize // Uses the screen's available content area.
import androidx.compose.foundation.layout.fillMaxWidth // Gives cards full readable width.
import androidx.compose.foundation.layout.padding // Applies app bar and screen padding.
import androidx.compose.foundation.layout.size // Sizes hero icon badge cleanly.
import androidx.compose.foundation.rememberScrollState // Remembers scroll position.
import androidx.compose.foundation.shape.CircleShape // Rounds app emblem cleanly.
import androidx.compose.foundation.shape.RoundedCornerShape // Rounds info cards smoothly.
import androidx.compose.foundation.verticalScroll // Keeps credits accessible on small screens or rotation.
import androidx.compose.material3.ElevatedCard // Groups related project and tech credits cleanly.
import androidx.compose.material3.Icon // Draws the local mentor emblem.
import androidx.compose.material3.MaterialTheme // Reuses theme colours and typography.
import androidx.compose.material3.Surface // Frames the circular icon badge.
import androidx.compose.material3.Text // Displays version, author and library information.
import androidx.compose.runtime.Composable // Marks this static screen as Compose content.
import androidx.compose.ui.Alignment // Aligns hero badge and title text vertically.
import androidx.compose.ui.Modifier // Describes layout constraints.
import androidx.compose.ui.res.painterResource // Loads local mentor icon.
import androidx.compose.ui.res.stringResource // Reads visible labels from resources.
import androidx.compose.ui.text.font.FontWeight // Highlights card headings cleanly.
import androidx.compose.ui.unit.dp // Specifies clean Material 3 sizes.
import pt.isel.pdm.droidmentor.BuildConfig // Reads only the generated app version, never a credential.
import pt.isel.pdm.droidmentor.R // Supplies app information strings.
import pt.isel.pdm.droidmentor.ui.components.ScreenScaffold // Provides top bar and Back navigation.
import pt.isel.pdm.droidmentor.ui.theme.DroidMentorSpacing // Uses shared spacing values.

/** Displays polished public application information without accessing repositories or preferences. */
@Composable // Needs no mutable state or asynchronous business operation.
fun AboutScreen(onBack: () -> Unit) { // Receives only the return callback belonging to the About destination.
    ScreenScaffold(stringResource(R.string.navigation_about), onBack) { padding -> // Keeps navigation outside content.
        Column( // Uses a clean scrollable stack of hero card and info cards.
            modifier = Modifier // Builds scrollable body.
                .fillMaxSize() // Uses available screen area.
                .padding(padding) // Leaves room for app bar and system bars.
                .verticalScroll(rememberScrollState()) // Keeps full info reachable in landscape.
                .padding(DroidMentorSpacing.large), // Outer breathing room.
            verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.large), // Separates cards clearly.
        ) { // Displays app hero, course/author card and libraries card.
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) { // App hero summary card.
                Row( // Places circular badge beside app name and version.
                    modifier = Modifier.padding(DroidMentorSpacing.large), // Balanced card padding.
                    horizontalArrangement = Arrangement.spacedBy(DroidMentorSpacing.medium), // Separates badge from text.
                    verticalAlignment = Alignment.CenterVertically, // Centres badge and title group.
                ) { // Renders hero badge and version label.
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) { // Rounded emblem background.
                        Box(contentAlignment = Alignment.Center) { // Centres mentor vector icon.
                            Icon(painterResource(R.drawable.ic_mentor), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp)) // Decorative app icon.
                        } // Ends centered Box.
                    } // Ends circular badge.
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { // Groups app name and version badge.
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) // App title.
                        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // Gradle version string.
                    } // Ends title column.
                } // Ends hero Row.
            } // Ends hero ElevatedCard.
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) { // Author & academic details card.
                Column(modifier = Modifier.padding(DroidMentorSpacing.large), verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small)) { // Stacks author, course and institution cleanly.
                    Text(stringResource(R.string.about_author), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) // Student name and number.
                    Text(stringResource(R.string.about_course), style = MaterialTheme.typography.bodyLarge) // Course title.
                    Text(stringResource(R.string.about_institution), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // University name.
                } // Ends academic details Column.
            } // Ends author card.
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) { // Libraries and technology credits card.
                Column(modifier = Modifier.padding(DroidMentorSpacing.large), verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small)) { // Stacks credits heading and body cleanly.
                    Text(stringResource(R.string.about_credits_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) // Section title.
                    Text(stringResource(R.string.about_credits), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // Library summary.
                } // Ends credits Column.
            } // Ends libraries card.
        } // Ends info Column.
    } // Ends ScreenScaffold.
} // Ends About screen.
