package pt.isel.pdm.droidmentor.data.remote // Groups remote retry helpers with the Ktor Gemini data source.

import kotlin.random.Random // Adds positive jitter so repeated clients do not retry in lockstep.
import kotlinx.coroutines.delay // Suspends cancellably between transient retry attempts.

/**
 * Bounded exponential-backoff retry configuration for transient Gemini HTTP statuses (429 and 503).
 *
 * @property maxAttempts Maximum total HTTP attempts, including the initial request.
 * @property baseDelayMs Base delay before multiplying by 2^retryIndex when Retry-After is absent.
 * @property maxDelayMs Upper cap applied to both Retry-After values and exponential backoff.
 * @property jitterFraction Supplies a multiplier in 0.0..0.25 added to exponential delays.
 * @property delayFn Cancellable wait primitive invoked before each retry.
 */
class RetryPolicy( // Uses simple injectable lambdas so unit tests run deterministically without sleeping.
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS, // Allows one initial call plus two retries by default.
    private val baseDelayMs: Long = DEFAULT_BASE_DELAY_MS, // Starts backoff at 1 second for transient errors.
    private val maxDelayMs: Long = DEFAULT_MAX_DELAY_MS, // Caps any computed wait so a large Retry-After cannot stall callers.
    private val jitterFraction: () -> Double = { Random.nextDouble(0.0, MAX_JITTER_FRACTION) }, // Adds up to 25% positive jitter by default.
    private val delayFn: suspend (Long) -> Unit = { durationMs -> delay(durationMs) }, // Uses coroutine delay in production and test recorders in unit tests.
) { // Begins retry timing helpers.

    /** Suspends for the computed delay before retrying [retryIndex]. */
    suspend fun waitBeforeRetry(retryIndex: Int, retryAfterSeconds: Long?) { // Applies Retry-After or exponential backoff before the next attempt.
        delayFn(delayMsFor(retryIndex, retryAfterSeconds)) // Delegates to the cancellable wait function.
    } // Ends retry backoff pause.

    /** Computes the wait duration in milliseconds for [retryIndex] (0 before attempt 2, 1 before attempt 3). */
    internal fun delayMsFor(retryIndex: Int, retryAfterSeconds: Long?): Long { // Keeps delay math pure and easy to inspect in tests.
        if (retryAfterSeconds != null) { // Prefers explicit server guidance whenever 429 or 503 supplied Retry-After.
            return (retryAfterSeconds.coerceAtLeast(0L) * 1_000L).coerceAtMost(maxDelayMs) // Converts seconds to milliseconds under the safety cap.
        } // Ends server-guided wait branch.
        val multiplier = 1L shl retryIndex.coerceAtLeast(0).coerceAtMost(10) // Computes 2^retryIndex safely without integer overflow.
        val exponentialMs = (baseDelayMs * multiplier).coerceAtMost(maxDelayMs) // Computes capped exponential backoff in milliseconds.
        val jitterMs = (exponentialMs * jitterFraction().coerceIn(0.0, MAX_JITTER_FRACTION)).toLong() // Adds positive jitter up to 25% of the backoff step.
        return (exponentialMs + jitterMs).coerceAtMost(maxDelayMs) // Returns final delay capped at maxDelayMs.
    } // Ends delay calculation.

    private companion object { // Holds documented default retry policy bounds.
        const val DEFAULT_MAX_ATTEMPTS = 3 // Runs at most 3 attempts total (1 initial + 2 retries).
        const val DEFAULT_BASE_DELAY_MS = 1_000L // Uses 1s base delay -> 1s then 2s before jitter.
        const val DEFAULT_MAX_DELAY_MS = 30_000L // Caps server or computed wait at 30 seconds.
        const val MAX_JITTER_FRACTION = 0.25 // Keeps positive jitter bounded at 25% of the base step.
    } // Ends default constants.
} // Ends RetryPolicy.
