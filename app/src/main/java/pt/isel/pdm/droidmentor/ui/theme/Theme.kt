package pt.isel.pdm.droidmentor.ui.theme // Groups the theme code.

import androidx.compose.foundation.isSystemInDarkTheme // Reads whether the device is using dark mode.
import androidx.compose.material3.MaterialTheme // Provides the app colours and text styles.
import androidx.compose.runtime.Composable // Marks functions that describe Compose UI.

/**
 * Applies the DroidMentor colour palette and typography to Compose content.
 *
 * @param darkTheme Whether to use dark colours; follows the system setting by default.
 * @param content The UI rendered within the theme.
 */
@Composable // This function describes UI that Compose can draw.
fun DroidMentorTheme( // Defines the reusable theme function for our screens.
    darkTheme: Boolean = isSystemInDarkTheme(), // Uses the device dark-mode setting unless the caller chooses a mode.
    content: @Composable () -> Unit, // Receives the Compose UI to display inside this theme.
) { // Starts the content displayed inside the component.
    MaterialTheme( // Passes our theme settings to Material 3 components.
        colorScheme = if (darkTheme) DroidMentorDarkColorScheme else DroidMentorLightColorScheme, // Chooses the dark palette when darkTheme is true, otherwise the light palette.
        typography = DroidMentorTypography, // Uses the shared text styles defined in Type.kt.
        content = content, // Displays the UI received as the content parameter.
    ) // Ends the list of values passed above.
} // Ends this block.
