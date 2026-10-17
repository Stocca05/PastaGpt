package pt.isel.pdm.droidmentor.data.remote.dto // Groups the JSON models used by Gemini.

import kotlinx.serialization.Serializable // Generates the JSON serializer for this model.

/**
 * One conversation turn or the separately supplied system instruction.
 *
 * @property role The wire role, user or model; omitted for the system instruction.
 * @property parts Ordered message parts; an absent response list is treated as empty.
 */
@Serializable // Converts the content object to and from JSON.
data class Content( // Shares the API's content shape between request and response models.
    val role: String? = null, // Leaves the role unset when this content is a system instruction.
    val parts: List<Part> = emptyList(), // Keeps every returned part in its original order.
) // Ends the content model.
