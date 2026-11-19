package pt.isel.pdm.droidmentor.data.local // Stores conversation image files safely inside app-private storage.

import android.content.Context // Opens picked URIs via ContentResolver and locates app filesDir.
import android.net.Uri // Parses picked gallery or camera content URIs inside the Android storage adapter.
import java.io.File // References app-private files inside filesDir/images.
import java.io.InputStream // Reads picked media bytes without holding full Bitmaps unnecessarily.
import java.util.UUID // Generates unique filenames inside filesDir/images.
import kotlinx.coroutines.CancellationException // Preserves structured coroutine cancellation during file I/O.
import kotlinx.coroutines.CoroutineDispatcher // Allows unit tests to inject a deterministic IO dispatcher.
import kotlinx.coroutines.Dispatchers // Moves file reads and writes off the main thread.
import kotlinx.coroutines.withContext // Runs storage work on the injected IO dispatcher.

/** Contract for app-private conversation image storage; Room stores only relative paths (Constraint #10). */
interface ImageStorage { // Keeps ViewModels and repositories independent of Android ContentResolver details.
    /** Copies picked media into app-private storage and returns a relative path, or null on failure. */
    suspend fun importFromUri(uriString: String): String? // Never stores raw Base64 or external content:// URIs in Room.

    /** Resolves a stored relative path into an existing file inside app storage, rejecting traversal. */
    fun resolveFile(relativePath: String): File? // Keeps stored paths sandbox-safe across app launches.

    /** Deletes one stored relative image file if it exists inside app storage. */
    fun deleteRelativePath(relativePath: String): Boolean // Cleans up uncommitted, deleted or rewritten attachments.
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

    companion object { // Exposes the relative subdirectory name used across storage and FileProvider checks.
        const val IMAGES_SUBDIR = "images" // Stores all imported/captured chat images under filesDir/images/.

        private fun openContentStream(context: Context, raw: String): InputStream? = try { // Opens picked/captured URIs safely without referencing instance state.
            context.contentResolver.openInputStream(Uri.parse(raw)) // Delegates stream opening to Android ContentResolver.
        } catch (_: Exception) { // Traps missing files or revoked URI permissions cleanly.
            null // Signals import failure cleanly to caller.
        } // Ends safe stream opener helper.
    } // Ends companion constants.
} // Ends LocalImageStorage.
