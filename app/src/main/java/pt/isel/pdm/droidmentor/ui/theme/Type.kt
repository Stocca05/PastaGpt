package pt.isel.pdm.droidmentor.ui.theme // Groups the theme code.

import androidx.compose.material3.Typography // Groups the Material 3 text styles.
import androidx.compose.ui.text.TextStyle // Describes how a piece of text should look.
import androidx.compose.ui.text.font.FontFamily // Lets us choose the font family.
import androidx.compose.ui.text.font.FontWeight // Lets us choose normal or heavier text.
import androidx.compose.ui.unit.sp // Creates text sizes that follow the user font-size setting.

/** Text styles shared by every DroidMentor screen, using the device's sans-serif font. */
internal val DroidMentorTypography = Typography( // Groups all the text styles used by the app.
    displayLarge = textStyle(57, 64, letterSpacing = -0.25f), // displayLarge: 57 sp text with 64 sp line height.
    displayMedium = textStyle(45, 52), // displayMedium: 45 sp text with 52 sp line height.
    displaySmall = textStyle(36, 44), // displaySmall: 36 sp text with 44 sp line height.
    headlineLarge = textStyle(32, 40), // headlineLarge: 32 sp text with 40 sp line height.
    headlineMedium = textStyle(28, 36), // headlineMedium: 28 sp text with 36 sp line height.
    headlineSmall = textStyle(24, 32), // headlineSmall: 24 sp text with 32 sp line height.
    titleLarge = textStyle(22, 28), // titleLarge: 22 sp text with 28 sp line height.
    titleMedium = textStyle(16, 24, FontWeight.Medium, 0.15f), // titleMedium: 16 sp text with 24 sp line height. Medium weight.
    titleSmall = textStyle(14, 20, FontWeight.Medium, 0.1f), // titleSmall: 14 sp text with 20 sp line height. Medium weight.
    bodyLarge = textStyle(16, 24, letterSpacing = 0.5f), // bodyLarge: 16 sp text with 24 sp line height.
    bodyMedium = textStyle(14, 20, letterSpacing = 0.25f), // bodyMedium: 14 sp text with 20 sp line height.
    bodySmall = textStyle(12, 16, letterSpacing = 0.4f), // bodySmall: 12 sp text with 16 sp line height.
    labelLarge = textStyle(14, 20, FontWeight.Medium, 0.1f), // labelLarge: 14 sp text with 20 sp line height. Medium weight.
    labelMedium = textStyle(12, 16, FontWeight.Medium, 0.5f), // labelMedium: 12 sp text with 16 sp line height. Medium weight.
    labelSmall = textStyle(11, 16, FontWeight.Medium, 0.5f), // labelSmall: 11 sp text with 16 sp line height. Medium weight.
) // Ends the list of values passed above.

private fun textStyle( // Creates a text style from the values passed below.
    fontSize: Int, // Receives the size of the text.
    lineHeight: Int, // Receives the space occupied by each line of text.
    fontWeight: FontWeight = FontWeight.Normal, // Uses normal-weight text unless another weight is provided.
    letterSpacing: Float = 0f, // Uses no extra letter spacing unless a value is provided.
): TextStyle = TextStyle( // Returns a TextStyle containing these settings.
    fontFamily = FontFamily.SansSerif, // Uses the device sans-serif font.
    fontWeight = fontWeight, // Applies the requested font weight.
    fontSize = fontSize.sp, // Converts the text size to a unit that follows the user font setting.
    lineHeight = lineHeight.sp, // Uses the same scalable unit for the line height.
    letterSpacing = letterSpacing.sp, // Applies the requested space between letters.
) // Ends the list of values passed above.
