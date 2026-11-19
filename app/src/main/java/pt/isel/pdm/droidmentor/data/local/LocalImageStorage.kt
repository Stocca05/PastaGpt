package pt.isel.pdm.droidmentor.data.local // Stores conversation image files safely inside app-private storage.

import android.content.Context // Opens picked URIs via ContentResolver and locates app filesDir.
import android.net.Uri // Parses picked gallery or camera content URIs inside the Android storage adapter.
import java.io.File // References app-private files inside filesDir/images.
import java.io.InputStream // Reads picked media bytes without holding full Bitmaps unnecessarily.
import java.util.Base64 // Encodes saved image bytes to Base64 strictly in memory (minSdk 26+).
import java.util.UUID // Generates unique filenames inside filesDir/images.
import kotlinx.coroutines.CancellationException // Preserves structured coroutine cancellation during file I/O.
import kotlinx.coroutines.CoroutineDispatcher // Allows unit tests to inject a deterministic IO dispatcher.
import kotlinx.coroutines.Dispatchers // Moves file reads and writes off the main thread.
import kotlinx.coroutines.withContext // Runs storage work on the injected IO dispatcher.

/** Contract for app-private conversation image storage; Room stores only relative paths (Constraint #10). */
interface ImageStorage { // Keeps ViewModels and repositories independent of Android ContentResolver details.
    /** Copies picked media into app-private storage and returns a relative path, or null on failure. */
    suspend fun importFromUri(uriString: String): String? // Never stores raw Base64 or external content:// URIs in Room.

    /** Allocates a unique relative target inside filesDir/images/ for camera capture via FileProvider. */
    suspend fun createCaptureTarget(): String? // Prepares the target path before TakePicture runs.

    /** Verifies a completed capture on disk or removes an aborted/empty file. */
    suspend fun finalizeCapture(relativePath: String, succeeded: Boolean): String? // Cleans up cancelled captures cleanly.

    /** Resolves a stored relative path into an existing file inside app storage, rejecting traversal. */
    fun resolveFile(relativePath: String): File? // Keeps stored paths sandbox-safe across app launches.

    /** Deletes one stored relative image file if it exists inside app storage. */
    fun deleteRelativePath(relativePath: String): Boolean // Cleans up uncommitted, deleted or rewritten attachments.

    /** Reads bytes from [relativePath] and encodes Base64 in memory only for Gemini inline_data. */
    suspend fun encodeAsBase64(relativePath: String): Pair<String, String>? = null // Keeps Base64 out of Room per V9.
} // Ends app-private image storage contract.

/** Stores conversation images under filesDir/images/ using unique relative filenames. */
class LocalImageStorage internal constructor( // Accepts injectable files root and stream opener for pure JVM unit tests.
    private val filesRoot: File, // Points to context.filesDir in production and TemporaryFolder in unit tests.
    private val streamOpener: (String) -> InputStream?, // Opens external gallery or camera streams safely.
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO, // Executes disk copies off the main dispatcher.
) : ImageStorage { // Implements sandbox-safe local file management.

    /** Builds the production adapter backed by the Android application's filesDir and ContentResolver. */
    constructor(context: Context) : this( // Uses applicationContext properties cleanly without holding an Activity reference.
        filesRoot = context.filesDir, // Uses private internal app storage removed cleanly on uninstall.
        streamOpener = { raw -> openContentStream(context, raw) }, // Opens Photo Picker or MediaStore URIs safely.
    ) // Ends Android Context constructor.

    private val imagesDir: File // Groups all saved chat images under one subdirectory.
        get() = File(filesRoot, IMAGES_SUBDIR) // Uses the relative "images" directory required by the plan.

    /** Copies the source stream identified by [uriString] into filesDir/images/<uuid>.jpg. */
    override suspend fun importFromUri(uriString: String): String? { // Runs completely on the IO dispatcher.
        if (uriString.isBlank()) return null // Rejects blank URI input before touching disk.
        return withContext(ioDispatcher) { // Ensures file copy never blocks the main thread.
            val dir = imagesDir // Resolves target directory inside private storage.
            if (!dir.exists() && !dir.mkdirs()) return@withContext null // Fails cleanly if the directory cannot be created.
            val fileName = "${UUID.randomUUID()}.jpg" // Generates a unique local filename independent of provider display names.
            val target = File(dir, fileName) // Prepares destination file inside filesDir/images/.
            try { // Ensures partial files are removed if copying fails or produces an empty file.
                val input = streamOpener(uriString) ?: return@withContext null // Aborts if ContentResolver cannot open the URI.
                val copiedBytes = input.use { source -> // Closes input stream deterministically.
                    target.outputStream().use { dest -> source.copyTo(dest) } // Streams picked image bytes to private storage.
                } // Ends buffered copy.
                if (copiedBytes <= 0L) { // Rejects zero-byte streams cleanly.
                    target.delete() // Removes empty leftover file.
                    null // Reports failed import.
                } else { // Returns only the sandbox-relative path for ViewModel/Room persistence.
                    "$IMAGES_SUBDIR/$fileName" // Preserves relative path resilience across container/sandbox updates.
                } // Ends byte-count check.
            } catch (cancelled: CancellationException) { // Cleans up partial file while propagating cancellation.
                target.delete() // Avoids leaving corrupt temporary files when cancelled.
                throw cancelled // Preserves coroutine cancellation.
            } catch (_: Exception) { // Traps IO errors safely without surfacing raw paths/messages.
                target.delete() // Deletes partial destination file.
                null // Signals safe import failure to UI.
            } // Ends copy guard.
        } // Ends IO context.
    } // Ends gallery URI copy.

    /** Allocates a unique relative path inside filesDir/images/ for FileProvider camera capture. */
    override suspend fun createCaptureTarget(): String? = withContext(ioDispatcher) { // Ensures images directory exists off the main thread.
        val dir = imagesDir // Resolves filesDir/images/.
        if (!dir.exists() && !dir.mkdirs()) return@withContext null // Fails cleanly if directory creation fails.
        "$IMAGES_SUBDIR/${UUID.randomUUID()}.jpg" // Returns unique relative target path for FileProvider URI resolution.
    } // Ends camera capture target allocation.

    /** Commits a captured image when [succeeded] and non-empty, or deletes cancelled/empty files cleanly. */
    override suspend fun finalizeCapture(relativePath: String, succeeded: Boolean): String? = withContext(ioDispatcher) { // Validates disk state off main thread.
        val target = resolveFile(relativePath) ?: return@withContext null // Rejects invalid relative paths safely.
        if (!succeeded || !target.exists() || target.length() <= 0L) { // Cleans cancelled or empty output files.
            target.delete() // Prevents 0-byte orphan files in filesDir/images/.
            null // Signals cancelled or empty capture.
        } else { // Keeps valid non-empty captured photo on disk.
            relativePath // Returns confirmed sandbox-safe relative path.
        } // Ends capture finalization check.
    } // Ends camera capture validation.

    /** Resolves [relativePath] only if it stays safely within filesDir/images/. */
    override fun resolveFile(relativePath: String): File? { // Guards against path traversal or unexpected absolute paths.
        if (relativePath.isBlank() || !relativePath.startsWith("$IMAGES_SUBDIR/") || relativePath.contains("..")) return null // Rejects invalid relative paths early.
        val canonicalDir = runCatching { imagesDir.canonicalFile }.getOrNull() ?: return null // Resolves canonical base images directory.
        val candidate = runCatching { File(filesRoot, relativePath).canonicalFile }.getOrNull() ?: return null // Resolves canonical target file.
        if (candidate.parentFile != canonicalDir) return null // Guarantees single-level containment inside filesDir/images/.
        return candidate // Returns resolved file path safely.
    } // Ends safe relative-path resolution.

    /** Deletes the file at [relativePath] when present inside filesDir/images/. */
    override fun deleteRelativePath(relativePath: String): Boolean { // Removes unused or deleted image attachments.
        val target = resolveFile(relativePath) ?: return false // Ignores unsafe or invalid relative paths cleanly.
        return target.exists() && target.delete() // Deletes file if present on disk.
    } // Ends safe deletion.

    /** Reads [relativePath] off main thread and returns (mimeType, base64) strictly in memory. */
    override suspend fun encodeAsBase64(relativePath: String): Pair<String, String>? = withContext(ioDispatcher) { // Performs disk read and encoding off main dispatcher.
        val target = resolveFile(relativePath)?.takeIf { it.exists() && it.isFile } ?: return@withContext null // Stops cleanly if image file is missing.
        try { // Guards against unexpected disk read errors.
            val bytes = target.readBytes() // Reads saved image bytes transiently into memory.
            if (bytes.isEmpty()) return@withContext null // Rejects empty image files safely.
            val mimeType = detectMimeType(bytes) // Inspects file magic bytes cleanly without relying on extension only.
            mimeType to Base64.getEncoder().encodeToString(bytes) // Produces Base64 in memory only, never persisted to disk.
        } catch (cancelled: CancellationException) { // Preserves structured coroutine cancellation.
            throw cancelled // Propagates cancellation cleanly.
        } catch (_: Exception) { // Traps IO errors cleanly.
            null // Allows text-only fallback or typed rejection cleanly.
        } // Ends encoding guard.
    } // Ends transient Base64 encoding.

    private fun detectMimeType(bytes: ByteArray): String = when { // Matches standard PNG/WebP headers before defaulting to JPEG.
        bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "image/png" // PNG magic bytes.
        bytes.size >= 12 && bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() && bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() && bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte() -> "image/webp" // WEBP RIFF header.
        else -> "image/jpeg" // Defaults Photo Picker / camera JPEG captures to image/jpeg.
    } // Ends magic-byte MIME detection.

    companion object { // Exposes the relative subdirectory name used across storage and FileProvider checks.
        const val IMAGES_SUBDIR = "images" // Stores all imported/captured chat images under filesDir/images/.

        private fun openContentStream(context: Context, raw: String): InputStream? = try { // Opens picked/captured URIs safely without referencing instance state.
            context.contentResolver.openInputStream(Uri.parse(raw)) // Delegates stream opening to Android ContentResolver.
        } catch (_: Exception) { // Traps missing files or revoked URI permissions cleanly.
            null // Signals import failure cleanly to caller.
        } // Ends safe stream opener helper.
    } // Ends companion constants.
} // Ends LocalImageStorage.
