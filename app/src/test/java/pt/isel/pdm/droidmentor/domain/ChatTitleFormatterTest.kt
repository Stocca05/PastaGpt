package pt.isel.pdm.droidmentor.domain // Tests pure-Kotlin conversation title formatting rules.

import org.junit.Assert.assertEquals // Compares formatted titles with expected text.
import org.junit.Test // Marks JUnit test cases.

/** Verifies automatic and manual title formatting rules for M3-02. */
class ChatTitleFormatterTest { // Covers short messages, truncation, whitespace and blank fallback.

    /** Checks short single-line text is trimmed and kept intact. */
    @Test // Verifies messages within the 40-character limit.
    fun shortMessageBecomesTitleWithoutEllipsis() { // Uses harmless sample text.
        assertEquals("How does Room work?", ChatTitleFormatter.fromFirstMessage("  How does Room work?  ")) // Trims outer whitespace only.
    } // Ends short-title test.

    /** Checks newlines and repeated spaces collapse into single spaces. */
    @Test // Verifies multiline questions stay single-line in chat lists.
    fun multilineWhitespaceCollapsesToSingleSpaces() { // Supplies tabs and line breaks.
        assertEquals("Explain ViewModel\nstate".replace("\n", " "), ChatTitleFormatter.fromFirstMessage("Explain\n  ViewModel\t\nstate")) // Collapses internal whitespace.
    } // Ends whitespace normalization test.

    /** Checks long messages are capped at MAX_LENGTH with an ellipsis. */
    @Test // Verifies long first prompts do not overflow history cards.
    fun longMessageIsTruncatedWithEllipsis() { // Supplies text longer than 40 characters.
        val input = "Explain how manual dependency injection works in Android without Hilt" // Exceeds 40 characters.
        val title = ChatTitleFormatter.fromFirstMessage(input) // Formats the automatic title.
        assertEquals(ChatTitleFormatter.MAX_LENGTH, title.length) // Stays within the 40-character cap.
        assertEquals("Explain how manual dependency injection…", title) // Ends cleanly with an ellipsis.
    } // Ends truncation test.

    /** Checks blank text falls back safely to the default title. */
    @Test // Guards against storing an empty title.
    fun blankTextFallsBackToDefaultTitle() { // Supplies whitespace only.
        assertEquals(ChatTitleFormatter.DEFAULT_TITLE, ChatTitleFormatter.fromFirstMessage("  \n\t ")) // Returns the default title.
    } // Ends blank-fallback test.
} // Ends title formatter tests.
