package pt.isel.pdm.droidmentor.data.remote.dto // Groups the JSON models used by Gemini.

import kotlinx.serialization.SerialName // Maps optional multimodal inline_data wire field cleanly.
import kotlinx.serialization.Serializable // Generates the JSON serializer for this model.

/**
 * One content portion of a Gemini message (text or ephemeral in-memory inline_data image).
 *
 * @property text Text when this part contains text; null for image-only or unsupported parts.
 * @property inlineData Ephemeral Base64 image payload built only in memory at request time (V9).
 * @property thought Whether the server marks this part as thought rather than answer text.
 */
@Serializable // Lets kotlinx.serialization encode and decode the supported fields.
data class Part( // Represents one part without ever persisting Base64 image data in Room.
    val text: String? = null, // Holds text content when supplied.
    @SerialName("inline_data") // Uses snake_case wire key expected by Gemini REST.
    val inlineData: InlineData? = null, // Carries ephemeral Base64 bytes on outgoing multimodal user turns.
    val thought: Boolean? = null, // Preserves the server marker so the data source can identify answer text.
) // Ends the part model.
