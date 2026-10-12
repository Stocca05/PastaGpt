package pt.isel.pdm.droidmentor.domain // Groups the domain models.

/**
 * One text turn in the conversation history.
 *
 * The data layer later converts this turn to a request DTO. Local IDs and message
 * states stay in [Message], so this turn only holds a role and some text.
 *
 * @property role The user or model that wrote this turn.
 * @property text Text to include in the conversation history.
 */
data class ConversationTurn( // Defines the fields of ConversationTurn; Kotlin also provides copy and equality functions.
    val role: Role, // Tells us whether the user or the model wrote this text.
    val text: String, // The text written by this participant.
) // Ends the list of values passed above.
