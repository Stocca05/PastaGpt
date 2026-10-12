package pt.isel.pdm.droidmentor.domain // Groups the domain models.

/**
 * A chat stored and identified by our app.
 *
 * @property id ID created by our app, not by the remote service.
 * @property title Display title of the conversation.
 * @property createdAt Creation time in milliseconds since the Unix epoch.
 * @property updatedAt Last update time in milliseconds since the Unix epoch.
 */
data class Chat( // Defines the fields of Chat; Kotlin also provides copy and equality functions.
    val id: String, // ID created by our app to identify this item.
    val title: String, // Title displayed for this chat.
    val createdAt: Long, // Creation time, stored as milliseconds since the Unix epoch.
    val updatedAt: Long, // Time of the last chat update, using the same unit.
) // Ends the list of values passed above.
