package pt.isel.pdm.droidmentor.data.remote.dto // Holds transient wire-only Base64 image data generated in memory.

import kotlinx.serialization.SerialName // Matches Gemini generateContent REST snake_case field names.
import kotlinx.serialization.Serializable // Generates kotlinx.serialization adapter with no Gson/Moshi.

/**
 * Ephemeral Base64 image payload attached only to outgoing Gemini requests in memory (V9).
 *
 * Never stored in Room, DataStore, logs or UI state.
 *
 * @property mimeType Media type of the encoded image (e.g. image/jpeg, image/png).
 * @property data Standard Base64-encoded image bytes produced right before POST.
 */
@Serializable // Serializes inline_data cleanly for Gemini multimodal requests.
data class InlineData( // Holds wire-only media type and Base64 payload.
    @SerialName("mime_type") // Required snake_case wire field for Gemini REST API.
    val mimeType: String, // Describes image encoding format.
    val data: String, // Base64 payload produced purely in memory during request serialization.
) // Ends InlineData DTO.
