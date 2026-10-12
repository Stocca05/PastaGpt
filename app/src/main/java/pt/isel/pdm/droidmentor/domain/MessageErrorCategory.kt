package pt.isel.pdm.droidmentor.domain // Groups the domain models.

/**
 * Simple error labels that can be saved with a failed message.
 *
 * These labels do not contain the key or exception details.
 * The data layer chooses the label. The UI turns it into a readable message.
 */
enum class MessageErrorCategory { // Lists the possible values for MessageErrorCategory.
    /** The configured key was rejected or did not grant access to the requested operation. */
    INVALID_API_KEY, // The service rejected the key or its access permissions.

    /** The service rejected the request as invalid. */
    BAD_REQUEST, // The request sent to the service was invalid.

    /** The service rejected the request because a quota or rate limit was reached. */
    RATE_LIMITED, // The service quota or request rate was exceeded.

    /** The service reported a server-side failure. */
    SERVER_ERROR, // The remote service reported a server error.

    /** The request could not complete because of connectivity or transport IO failure. */
    NETWORK_UNAVAILABLE, // A connection or transport problem stopped the request.

    /** The request exceeded its configured timeout. */
    TIMEOUT, // The request took longer than the allowed time.

    /** The service response could not be decoded into the expected format. */
    MALFORMED_RESPONSE, // The response could not be read in the expected format.

    /** The service returned no usable text reply. */
    EMPTY_RESPONSE, // The response contained no usable text.

    /** The service blocked the requested content or its generated reply. */
    BLOCKED_RESPONSE, // The service blocked the request or reply.

    /** The attempt failed for a reason outside the known categories. */
    UNKNOWN, // The error does not fit any of the known categories.
} // Ends this block.
