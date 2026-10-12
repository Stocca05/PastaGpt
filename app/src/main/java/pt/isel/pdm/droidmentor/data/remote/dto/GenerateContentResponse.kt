package pt.isel.pdm.droidmentor.data.remote.dto // Groups the JSON models used by Gemini.

import kotlinx.serialization.Serializable // Generates serializers for the response models.

/**
 * The supported fields of a Gemini text-generation response.
 *
 * @property candidates Generated alternatives; absent candidates leave an empty list.
 * @property usageMetadata Optional token counts reported by the server.
 * @property promptFeedback Optional metadata explaining a prompt-level block.
 */
@Serializable // Decodes the response while the client ignores unsupported extra fields.
data class GenerateContentResponse( // Keeps response metadata available for later error handling.
    val candidates: List<Candidate> = emptyList(), // Allows an empty or blocked response to be represented without invented text.
    val usageMetadata: UsageMetadata? = null, // Accepts responses that omit usage information.
    val promptFeedback: PromptFeedback? = null, // Preserves prompt blocking information even when no candidate is returned.
) // Ends the response envelope.

/**
 * One generated alternative, including candidates that contain no answer text.
 *
 * @property content Optional generated content.
 * @property finishReason Server reason for stopping; kept as a string to accept future values.
 */
@Serializable // Decodes each entry in the candidates array.
data class Candidate( // Represents the fields needed to find an answer or a blocked candidate.
    val content: Content? = null, // Accepts a candidate with a finish reason but no content.
    val finishReason: String? = null, // Preserves STOP, SAFETY and other server values without a fragile enum.
) // Ends the candidate model.

/**
 * Optional token usage counts; a missing value remains unknown rather than becoming zero.
 *
 * @property promptTokenCount Number of input tokens reported by the server.
 * @property candidatesTokenCount Number of tokens in the generated candidates.
 * @property totalTokenCount Total tokens reported for the request.
 * @property thoughtsTokenCount Number of thought tokens reported by thinking models.
 */
@Serializable // Decodes the supported usageMetadata fields.
data class UsageMetadata( // Keeps all counts optional because the server may omit them.
    val promptTokenCount: Int? = null, // Records the input count when provided.
    val candidatesTokenCount: Int? = null, // Records the generated answer count when provided.
    val totalTokenCount: Int? = null, // Records the server's total without recalculating it.
    val thoughtsTokenCount: Int? = null, // Records thought usage separately from answer text.
) // Ends the usage metadata model.

/**
 * Prompt-level feedback needed to recognise responses blocked before generation.
 *
 * @property blockReason Optional server block reason, including values added in the future.
 */
@Serializable // Decodes the supported promptFeedback field.
data class PromptFeedback( // Keeps only the block signal needed by the M2 error mapper.
    val blockReason: String? = null, // Preserves the reason without exposing a raw server explanation to the UI.
) // Ends the prompt feedback model.
