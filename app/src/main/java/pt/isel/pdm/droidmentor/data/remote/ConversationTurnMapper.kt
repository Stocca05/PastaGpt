package pt.isel.pdm.droidmentor.data.remote // Keeps conversion from domain turns in the data layer.

import pt.isel.pdm.droidmentor.data.remote.dto.Content // Provides the API's conversation-turn model.
import pt.isel.pdm.droidmentor.data.remote.dto.Part // Provides the API's text part model.
import pt.isel.pdm.droidmentor.domain.ConversationTurn // Reads a turn without adding network imports to the domain.
import pt.isel.pdm.droidmentor.domain.Role // Identifies the author using the domain enum.

/** Converts one already validated domain turn to Gemini text content without changing its text. */
fun ConversationTurn.toContent(): Content { // Converts only representation; history validation belongs to the history builder.
    val apiRole = when (role) { // Maps domain roles explicitly instead of relying on enum spelling or locale rules.
        Role.USER -> "user" // Uses the Gemini role for a turn written by the person.
        Role.MODEL -> "model" // Uses the Gemini role for a turn written by the mentor.
    } // Ends the exhaustive role mapping.
    return Content(role = apiRole, parts = listOf(Part(text = text))) // Keeps the complete text in one part, without trimming or filtering it.
} // Ends the turn conversion.
