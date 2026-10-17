package pt.isel.pdm.droidmentor.data.remote.dto // Groups the JSON models used by Gemini.

import kotlinx.serialization.SerialName // Sets the exact request field required by the project.
import kotlinx.serialization.Serializable // Generates the JSON serializer for this model.

/**
 * A stateless text request whose caller supplies the complete validated conversation.
 *
 * @property systemInstruction Required mentor instructions, separate from conversation turns.
 * @property contents Full conversation history followed by the current user turn.
 * @property generationConfig Optional text-generation settings.
 */
@Serializable // Encodes the request with kotlinx.serialization only.
data class GenerateContentRequest( // Contains no credential, cached content or server session identifier.
    @SerialName("system_instruction") // Writes the mentor instructions under this exact JSON field name.
    val systemInstruction: Content, // Requires the caller to supply the persona for each request.
    val contents: List<Content>, // Requires the caller to supply the complete conversation on every call.
    val generationConfig: GenerationConfig? = null, // Omits generation settings when no override is supplied.
) // Ends the request model.
