package pt.isel.pdm.droidmentor.data.remote.dto // Groups the JSON models used by Gemini.

import kotlinx.serialization.Serializable // Generates the JSON serializer for this model.

/**
 * The text portion of a Gemini message; M2 does not send attachments or tools.
 *
 * @property text Text when the received part contains text; null for an unsupported part.
 * @property thought Whether the server marks this part as thought rather than answer text.
 */
@Serializable // Lets kotlinx.serialization encode and decode the supported fields.
data class Part( // Represents one part without storing image data or tool payloads.
    val text: String? = null, // Allows responses containing a non-text part to remain readable.
    val thought: Boolean? = null, // Preserves the server marker so the data source can identify answer text.
) // Ends the part model.
