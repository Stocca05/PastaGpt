package pt.isel.pdm.droidmentor.ui.screens.about // Groups the application information screen.

import androidx.compose.foundation.layout.Arrangement // Separates the information blocks.
import androidx.compose.foundation.layout.Column // Displays the information in reading order.
import androidx.compose.foundation.layout.fillMaxSize // Uses the screen's available content area.
import androidx.compose.foundation.layout.padding // Applies app bar and screen padding.
import androidx.compose.foundation.rememberScrollState // Remembers the information's scroll position.
import androidx.compose.foundation.verticalScroll // Keeps credits accessible on small screens or with larger fonts.
import androidx.compose.material3.MaterialTheme // Reuses the app's typography.
import androidx.compose.material3.Text // Displays version, author and library information.
import androidx.compose.runtime.Composable // Marks this static screen as Compose content.
import androidx.compose.ui.Modifier // Describes the scrollable content layout.
import androidx.compose.ui.res.stringResource // Reads all visible labels from resources.
import pt.isel.pdm.droidmentor.BuildConfig // Reads only the generated app version, never a credential.
import pt.isel.pdm.droidmentor.R // Supplies the app information strings.
import pt.isel.pdm.droidmentor.ui.components.ScreenScaffold // Provides a clear title and return action.
import pt.isel.pdm.droidmentor.ui.theme.DroidMentorSpacing // Uses the shared spacing values.

/** Displays public application information without accessing repositories or user preferences. */
@Composable // The screen needs no mutable state or asynchronous business operation.
fun AboutScreen(onBack: () -> Unit) { // Receives only the return callback belonging to the About destination.
    ScreenScaffold(stringResource(R.string.navigation_about), onBack) { padding -> // Keeps navigation outside the information content.
        Column( // Uses one simple scrollable list of text blocks.
            modifier = Modifier // Builds the layout in readable steps.
                .fillMaxSize() // Uses the available body area.
                .padding(padding) // Leaves room for the app bar and system bars.
                .verticalScroll(rememberScrollState()) // Keeps the full information reachable in landscape.
                .padding(DroidMentorSpacing.large), // Adds the app's outer screen padding.
            verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.medium), // Separates related text blocks.
        ) { // Displays the required app and author information.
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium) // Displays the shared application name.
            Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME)) // Displays the actual version declared by Gradle.
            Text(stringResource(R.string.about_author)) // Displays the author and student number already documented in the repository.
            Text(stringResource(R.string.about_course)) // Identifies the course.
            Text(stringResource(R.string.about_institution)) // Identifies ISEL.
            Text(stringResource(R.string.about_credits_title), style = MaterialTheme.typography.titleLarge) // Introduces the libraries used by this M2 app.
            Text(stringResource(R.string.about_credits)) // Credits the declared AndroidX, Kotlin and Ktor components without inventing versions.
        } // Ends the information text.
    } // Ends the screen frame.
} // Ends the About screen.
