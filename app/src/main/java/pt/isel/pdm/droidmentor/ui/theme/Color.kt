package pt.isel.pdm.droidmentor.ui.theme // Groups the theme code.

import androidx.compose.material3.darkColorScheme // Creates a Material 3 palette for dark mode.
import androidx.compose.material3.lightColorScheme // Creates a Material 3 palette for light mode.
import androidx.compose.ui.graphics.Color // Represents a colour using an ARGB value.

/** Material 3 colour roles for the light DroidMentor theme. */
internal val DroidMentorLightColorScheme = lightColorScheme( // Creates the light palette; FF in each ARGB value means fully opaque.
    primary = Color(0xFF006A60), // Main colour for buttons and important controls.
    onPrimary = Color(0xFFFFFFFF), // Text and icons drawn on the primary colour.
    primaryContainer = Color(0xFF9EF2E1), // Background for a highlighted primary area.
    onPrimaryContainer = Color(0xFF00201A), // Text and icons inside that primary area.
    secondary = Color(0xFF4C635D), // Colour for less prominent controls.
    onSecondary = Color(0xFFFFFFFF), // Text and icons on the secondary colour.
    secondaryContainer = Color(0xFFCFE9E0), // Background for a secondary highlighted area.
    onSecondaryContainer = Color(0xFF09201A), // Text and icons inside that secondary area.
    tertiary = Color(0xFF426277), // Extra accent colour for distinct UI elements.
    onTertiary = Color(0xFFFFFFFF), // Text and icons on the tertiary colour.
    tertiaryContainer = Color(0xFFC7E7FF), // Background for a tertiary highlighted area.
    onTertiaryContainer = Color(0xFF041E2F), // Text and icons inside that tertiary area.
    background = Color(0xFFF5FBF8), // Background colour for the whole screen.
    onBackground = Color(0xFF171D1B), // Text and icons drawn directly on the screen background.
    surface = Color(0xFFF5FBF8), // Background colour for regular UI surfaces.
    onSurface = Color(0xFF171D1B), // Text and icons on regular surfaces.
    surfaceVariant = Color(0xFFDBE5DF), // Alternative surface colour when a different tone is needed.
    onSurfaceVariant = Color(0xFF3F4944), // Text and icons on the alternative surface.
    surfaceTint = Color(0xFF006A60), // Colour used to tint elevated surfaces.
    inverseSurface = Color(0xFF2B322F), // Surface colour that contrasts with the normal theme.
    inverseOnSurface = Color(0xFFF0F1EE), // Text and icons on that contrasting surface.
    inversePrimary = Color(0xFF80D5C7), // Primary accent used on the contrasting surface.
    error = Color(0xFFBA1A1A), // Colour used to show an error.
    onError = Color(0xFFFFFFFF), // Text and icons on the error colour.
    errorContainer = Color(0xFFFFDAD6), // Background for an error message or error area.
    onErrorContainer = Color(0xFF410002), // Text and icons inside that error area.
    outline = Color(0xFF707973), // Colour for visible borders and outlines.
    outlineVariant = Color(0xFFBFC9C2), // Softer colour for less prominent borders.
    scrim = Color(0xFF000000), // Base colour used to dim content behind a dialog.
    surfaceDim = Color(0xFFD6DBD7), // Dimmer version of the surface colour.
    surfaceBright = Color(0xFFF5FBF8), // Brighter version of the surface colour.
    surfaceContainerLowest = Color(0xFFFFFFFF), // Lowest-emphasis container background.
    surfaceContainerLow = Color(0xFFEFF5F1), // Low-emphasis container background.
    surfaceContainer = Color(0xFFE9EFEB), // Standard container background.
    surfaceContainerHigh = Color(0xFFE3E9E5), // Higher-emphasis container background.
    surfaceContainerHighest = Color(0xFFDEE4E0), // Highest-emphasis container background.
) // Ends the list of values passed above.

/** Material 3 colour roles for the dark DroidMentor theme. */
internal val DroidMentorDarkColorScheme = darkColorScheme( // Creates the dark palette using the same colour roles.
    primary = Color(0xFF80D5C7), // Main colour for buttons and important controls.
    onPrimary = Color(0xFF00382F), // Text and icons drawn on the primary colour.
    primaryContainer = Color(0xFF005047), // Background for a highlighted primary area.
    onPrimaryContainer = Color(0xFF9EF2E1), // Text and icons inside that primary area.
    secondary = Color(0xFFB3CDC4), // Colour for less prominent controls.
    onSecondary = Color(0xFF1F352E), // Text and icons on the secondary colour.
    secondaryContainer = Color(0xFF354C44), // Background for a secondary highlighted area.
    onSecondaryContainer = Color(0xFFCFE9E0), // Text and icons inside that secondary area.
    tertiary = Color(0xFFAFCBE0), // Extra accent colour for distinct UI elements.
    onTertiary = Color(0xFF123346), // Text and icons on the tertiary colour.
    tertiaryContainer = Color(0xFF2A4A5E), // Background for a tertiary highlighted area.
    onTertiaryContainer = Color(0xFFC7E7FF), // Text and icons inside that tertiary area.
    background = Color(0xFF101418), // Background colour for the whole screen.
    onBackground = Color(0xFFE0E4E2), // Text and icons drawn directly on the screen background.
    surface = Color(0xFF101418), // Background colour for regular UI surfaces.
    onSurface = Color(0xFFE0E4E2), // Text and icons on regular surfaces.
    surfaceVariant = Color(0xFF3F4944), // Alternative surface colour when a different tone is needed.
    onSurfaceVariant = Color(0xFFBFC9C2), // Text and icons on the alternative surface.
    surfaceTint = Color(0xFF80D5C7), // Colour used to tint elevated surfaces.
    inverseSurface = Color(0xFFE0E4E2), // Surface colour that contrasts with the normal theme.
    inverseOnSurface = Color(0xFF2B322F), // Text and icons on that contrasting surface.
    inversePrimary = Color(0xFF006A60), // Primary accent used on the contrasting surface.
    error = Color(0xFFFFB4AB), // Colour used to show an error.
    onError = Color(0xFF690005), // Text and icons on the error colour.
    errorContainer = Color(0xFF93000A), // Background for an error message or error area.
    onErrorContainer = Color(0xFFFFDAD6), // Text and icons inside that error area.
    outline = Color(0xFF89938C), // Colour for visible borders and outlines.
    outlineVariant = Color(0xFF3F4944), // Softer colour for less prominent borders.
    scrim = Color(0xFF000000), // Base colour used to dim content behind a dialog.
    surfaceDim = Color(0xFF101418), // Dimmer version of the surface colour.
    surfaceBright = Color(0xFF353B38), // Brighter version of the surface colour.
    surfaceContainerLowest = Color(0xFF0B0F0D), // Lowest-emphasis container background.
    surfaceContainerLow = Color(0xFF181D1A), // Low-emphasis container background.
    surfaceContainer = Color(0xFF1C211F), // Standard container background.
    surfaceContainerHigh = Color(0xFF272C29), // Higher-emphasis container background.
    surfaceContainerHighest = Color(0xFF313633), // Highest-emphasis container background.
) // Ends the list of values passed above.
