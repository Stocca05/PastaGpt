package pt.isel.pdm.droidmentor.domain // Groups the domain models.

/**
 * A text message and the current state of its exchange.
 *
 * The repository checks the history and changes message states.
 * A failed user message has an error category; pending and sent messages have none.
 * Model messages are stored only after a usable reply is received, with status [MessageStatus.SENT].
 *
 * @property id Client-generated identifier retained when retrying a failed send.
 * @property chatId Identifier of the local conversation that owns the message.
 * @property role Participant that authored the message.
 * @property text Message content, without image attachments in M2.
 * @property seq Message order inside its chat, assigned when saving it.
 * @property status Whether the exchange is pending, completed or failed.
 * @property createdAt Creation time in milliseconds since the Unix epoch.
 * @property errorCategory Type of failure saved with the message; null when there is no error.
 */
data class Message( // Defines the fields of Message; Kotlin also provides copy and equality functions.
    val id: String, // ID created by our app to identify this item.
    val chatId: String, // ID of the chat that owns this message.
    val role: Role, // Tells us whether the user or the model wrote this text.
    val text: String, // The text written by this participant.
    val seq: Long, // The message position inside its chat; Long stores the whole-number sequence.
    val status: MessageStatus, // Tells us whether the message is waiting, completed or failed.
    val createdAt: Long, // Creation time, stored as milliseconds since the Unix epoch.
    val errorCategory: MessageErrorCategory? = null, // The failure type; null means there is no recorded error.
) // Ends the list of values passed above.
