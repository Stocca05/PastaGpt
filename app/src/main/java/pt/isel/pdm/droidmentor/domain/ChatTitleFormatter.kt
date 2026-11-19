package pt.isel.pdm.droidmentor.domain // Keeps title formatting rules in pure Kotlin without Android imports.

/** Derives concise conversation titles from a user's first message or manual input. */
object ChatTitleFormatter { // Stateless helper shared by the domain tests and repository.
    /** Default title assigned before the first message or manual rename. */
    const val DEFAULT_TITLE = "New conversation" // Matches the initial translated title used on creation.

    /** Maximum display length for an auto-generated conversation title. */
    const val MAX_LENGTH = 40 // Keeps history list titles readable on phone screens.

    /** Generates a single-line title from the first characters of [messageText] or a photo fallback. */
    fun fromFirstMessage(messageText: String, hasImage: Boolean = false): String { // Collapses line breaks and repeated spaces cleanly.
        val normalized = normalize(messageText) // Normalizes whitespace before measuring length.
        if (normalized.isEmpty()) return if (hasImage) PHOTO_TITLE else DEFAULT_TITLE // Falls back safely for image-only prompts.
        return if (normalized.length <= MAX_LENGTH) { // Keeps short questions intact.
            normalized // Uses the full normalized question as the title.
        } else { // Shortens longer questions without splitting awkwardly at trailing whitespace.
            normalized.take(MAX_LENGTH - 1).trimEnd() + "…" // Appends an ellipsis within the 40-character cap.
        } // Ends length check.
    } // Ends auto-title formatting.

    /** Title used when the opening user message contains an attached photo without caption text. */
    private const val PHOTO_TITLE = "Photo question" // Keeps image-only conversations identifiable in history.

    /** Normalizes a manually entered title, collapsing newlines and extra spaces. */
    fun normalize(text: String): String { // Keeps both automatic and manual titles single-line.
        return text.trim().replace(Regex("\\s+"), " ") // Replaces tabs, newlines and runs of spaces with one space.
    } // Ends whitespace normalization.
} // Ends chat title formatter.
