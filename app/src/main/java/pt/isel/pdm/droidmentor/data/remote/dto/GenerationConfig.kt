package pt.isel.pdm.droidmentor.data.remote.dto // Groups the JSON models used by Gemini.

import kotlinx.serialization.Serializable // Generates the JSON serializer for this model.

/**
 * Optional text-generation settings; null values leave the model's defaults in place.
 *
 * @property temperature Optional sampling temperature supported by the selected model.
 * @property maxOutputTokens Optional maximum number of tokens generated for the response.
 */
@Serializable // Converts the configuration to the generationConfig JSON object.
data class GenerationConfig( // Holds settings without choosing a generation policy in this task.
    val temperature: Double? = null, // Omits a temperature override unless the caller explicitly supplies one.
    val maxOutputTokens: Int? = null, // Omits an output limit unless the caller explicitly supplies one.
) // Ends the generation configuration model.
