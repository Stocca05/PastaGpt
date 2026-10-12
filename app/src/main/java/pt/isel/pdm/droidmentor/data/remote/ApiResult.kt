package pt.isel.pdm.droidmentor.data.remote // Keeps network outcomes in the data layer.

import pt.isel.pdm.droidmentor.domain.MessageErrorCategory // Converts failures to the safe labels already supported by saved messages.

/** A usable API value or a typed failure containing no raw server body or exception. */
sealed interface ApiResult<out T> { // Allows callers to handle every possible outcome explicitly.

    /**
     * A successfully decoded value.
     *
     * @property value Usable data returned by the operation.
     */
    data class Success<T>(val value: T) : ApiResult<T> { // Carries only the decoded value needed by the repository.
        /** Hides the value when the result is converted to diagnostic text. */
        override fun toString(): String = "ApiResult.Success(value=<hidden>)" // Avoids printing model text through an automatic data-class representation.
    } // Ends the success result.

    /** HTTP 401 or 403 rejected the configured key or its access permissions. */
    data object InvalidApiKey : ApiResult<Nothing> // Contains no rejected credential or raw server message.

    /** HTTP 400 rejected the request. */
    data object BadRequest : ApiResult<Nothing> // Leaves translation into a readable UI message to the UI layer.

    /**
     * HTTP 429 rejected the request because of rate or quota limits.
     *
     * @property retryAfterSeconds Optional non-negative wait suggested by the server; no retry is performed here.
     */
    data class RateLimited(val retryAfterSeconds: Long? = null) : ApiResult<Nothing> // Retains only safe numeric retry metadata.

    /** An HTTP status between 500 and 599 reported a server failure. */
    data object ServerError : ApiResult<Nothing> // Contains no server response body.

    /** A transport IO failure prevented the call from completing. */
    data object NetworkUnavailable : ApiResult<Nothing> // Contains no IOException or potentially sensitive exception text.

    /** A Ktor request, connection or socket timeout stopped the operation. */
    data object Timeout : ApiResult<Nothing> // Represents network time limits separately from coroutine cancellation.

    /** The response could not be decoded as the expected Gemini format. */
    data object MalformedResponse : ApiResult<Nothing> // Contains no raw JSON or serializer exception.

    /** The response had no usable answer text. */
    data object EmptyResponse : ApiResult<Nothing> // Does not fabricate a model message for an empty response.

    /** The server blocked the prompt or selected response candidate. */
    data object BlockedResponse : ApiResult<Nothing> // Does not expose blocked text or raw policy feedback.

    /** The HTTP status, finish reason or ordinary failure did not match a known category. */
    data object Unknown : ApiResult<Nothing> // Gives callers a safe fallback without exposing exception details.
} // Ends the typed network outcomes.

/** Converts a network failure to its persisted label; success has no error category. */
fun ApiResult<*>.toMessageErrorCategory(): MessageErrorCategory? { // Lets the repository save a label instead of the network result or its raw details.
    return when (this) { // Covers every result so adding a new type also requires updating this mapping.
        is ApiResult.Success -> null // A usable response is not a failed message.
        ApiResult.InvalidApiKey -> MessageErrorCategory.INVALID_API_KEY // Records rejected credentials or access permissions.
        ApiResult.BadRequest -> MessageErrorCategory.BAD_REQUEST // Records an invalid request.
        is ApiResult.RateLimited -> MessageErrorCategory.RATE_LIMITED // Records the quota failure without storing HTTP headers in Room.
        ApiResult.ServerError -> MessageErrorCategory.SERVER_ERROR // Records a remote server failure.
        ApiResult.NetworkUnavailable -> MessageErrorCategory.NETWORK_UNAVAILABLE // Records a connection or transport failure.
        ApiResult.Timeout -> MessageErrorCategory.TIMEOUT // Records an expired network time limit.
        ApiResult.MalformedResponse -> MessageErrorCategory.MALFORMED_RESPONSE // Records unreadable or invalid response data.
        ApiResult.EmptyResponse -> MessageErrorCategory.EMPTY_RESPONSE // Records the lack of usable answer text.
        ApiResult.BlockedResponse -> MessageErrorCategory.BLOCKED_RESPONSE // Records a blocked prompt or reply.
        ApiResult.Unknown -> MessageErrorCategory.UNKNOWN // Records an otherwise unclassified failure.
    } // Ends the exhaustive persisted error mapping.
} // Ends conversion to the message error label.
