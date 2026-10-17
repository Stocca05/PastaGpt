package pt.isel.pdm.droidmentor.data.remote.dto // Groups the JSON models used by Gemini.

import kotlinx.serialization.Serializable // Generates serializers for the error envelope and its body.

/**
 * The JSON envelope returned for an HTTP API error; it stays in the data layer.
 *
 * @property error Server error fields, without converting them into UI messages.
 */
@Serializable // Decodes an HTTP error body using kotlinx.serialization.
data class ApiErrorEnvelope( // Does not store or log the received error.
    val error: ApiError, // Requires the error object instead of accepting an unrelated JSON response.
) // Ends the error envelope.

/**
 * Supported server error fields; raw details must never be logged or saved in Room.
 *
 * @property code Optional numeric code returned by the server.
 * @property message Optional raw server text, used only in memory by the data layer.
 * @property status Optional server status name, such as INVALID_ARGUMENT.
 */
@Serializable // Decodes the server's error fields and ignores extra fields through the client configuration.
data class ApiError( // Keeps diagnostic text private to data processing rather than UI state.
    val code: Int? = null, // Preserves a numeric error code when the body contains it.
    val message: String? = null, // May contain sensitive server text and must never be printed.
    val status: String? = null, // Keeps the server status available without choosing a result category here.
) { // Adds a safe text representation to this data model.
    /** Returns fixed text so diagnostic conversion cannot disclose server error fields. */
    override fun toString(): String { // Overrides Kotlin's generated representation of the fields.
        return "ApiError(details=<hidden>)" // Hides all fields, including anything echoed by the server.
    } // Ends the safe text representation.
} // Ends the server error model.
