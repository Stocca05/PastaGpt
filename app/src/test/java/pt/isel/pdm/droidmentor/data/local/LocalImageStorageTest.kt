package pt.isel.pdm.droidmentor.data.local // Tests app-private gallery/camera image storage without Robolectric or a device.

import java.io.ByteArrayInputStream // Simulates picked image streams cleanly in JVM tests.
import java.io.IOException // Simulates mid-stream read errors during import.
import java.io.InputStream // Wraps test streams for LocalImageStorage.
import kotlinx.coroutines.test.runTest // Runs suspend storage tests deterministically.
import org.junit.Assert.assertArrayEquals // Verifies copied file bytes match the picked image bytes.
import org.junit.Assert.assertFalse // Verifies deleted or traversal files report false.
import org.junit.Assert.assertNotNull // Confirms valid imports return a relative path.
import org.junit.Assert.assertNull // Confirms invalid streams and traversal paths are rejected.
import org.junit.Assert.assertTrue // Confirms files live under filesDir/images/ and are removed on delete.
import org.junit.Rule // Owns a clean temporary root folder per test case.
import org.junit.Test // Marks JUnit storage tests.
import org.junit.rules.TemporaryFolder // Simulates context.filesDir cleanly.

/** Verifies M3-11 relative path import, traversal safety and cleanup in LocalImageStorage. */
class LocalImageStorageTest { // Runs directly on the JVM without Android dependencies.
    /** Creates an isolated temporary storage root for each test. */
    @get:Rule // Guarantees zero leftover files after each test.
    val tempFolder = TemporaryFolder() // Replaces context.filesDir in unit tests.

    /** Checks valid URI stream is copied into images/<uuid>.jpg and resolved cleanly. */
    @Test // Protects M3-11 unique relative-path storage inside private filesDir.
    fun importCopiesBytesIntoImagesSubdirectoryAndDeletesCleanly() = runTest { // Uses real temp directory + in-memory payload bytes.
        val sampleBytes = byteArrayOf(1, 2, 3, 4, 5, 42) // Supplies distinctive mock JPEG bytes.
        val storage = LocalImageStorage(tempFolder.root, streamOpener = { uri -> // Returns sample bytes for valid content URI.
            if (uri == "content://picker/1") ByteArrayInputStream(sampleBytes) else null // Rejects unexpected URIs.
        }) // Builds storage under temporary root.
        val relative = storage.importFromUri("content://picker/1") // Copies picked stream into private storage.
        assertNotNull(relative) // Confirms import succeeded.
        val safePath = relative ?: error("Expected relative path") // Avoids !! in assertions.
        assertTrue(safePath.startsWith("images/") && safePath.endsWith(".jpg")) // Enforces sandbox-safe relative path format.
        val resolved = storage.resolveFile(safePath) // Resolves relative path against filesRoot.
        assertNotNull(resolved) // Confirms valid relative path resolves.
        assertTrue(resolved?.exists() == true) // Confirms file exists on disk.
        assertArrayEquals(sampleBytes, resolved?.readBytes()) // Confirms full byte copy fidelity.
        assertTrue(storage.deleteRelativePath(safePath)) // Deletes the stored file cleanly.
        assertFalse(resolved?.exists() == true) // Confirms file was removed from disk.
    } // Ends valid gallery import and cleanup test.

    /** Checks missing, empty or failing streams fail cleanly without leaving partial files behind. */
    @Test // Prevents orphaned zero-byte files after failed imports.
    fun failedOrEmptyStreamsReturnNullWithoutLeavingFiles() = runTest { // Covers absent stream, empty stream and IO failure.
        val storage = LocalImageStorage(tempFolder.root, streamOpener = { uri -> // Varies stream behaviour by test URI.
            when (uri) { // Selects failure mode cleanly.
                "content://empty" -> ByteArrayInputStream(ByteArray(0)) // Zero-byte stream.
                "content://error" -> object : InputStream() { override fun read(): Int = throw IOException("Broken pipe") } // Mid-copy failure.
                else -> null // Missing stream.
            } // Ends failure stream mapping.
        }) // Builds isolated storage adapter.
        assertNull(storage.importFromUri("content://missing")) // Rejects null InputStream.
        assertNull(storage.importFromUri("content://empty")) // Rejects zero-byte stream.
        assertNull(storage.importFromUri("content://error")) // Traps IO failure cleanly.
        val files = tempFolder.root.resolve("images").listFiles().orEmpty() // Inspects images subdirectory.
        assertTrue("Failed imports must leave no orphan files", files.isEmpty()) // Confirms zero leftover files remain.
    } // Ends failed import cleanup test.

    /** Checks traversal or out-of-directory relative paths are never resolved or deleted. */
    @Test // Guards against path traversal into parent app directories.
    fun resolveAndDeleteRejectPathTraversal() { // Supplies malicious and unexpected relative paths.
        val storage = LocalImageStorage(tempFolder.root, streamOpener = { null }) // Builds storage adapter.
        assertNull(storage.resolveFile("../secret.txt")) // Rejects parent traversal.
        assertNull(storage.resolveFile("images/../secret.txt")) // Rejects embedded traversal.
        assertNull(storage.resolveFile("/etc/passwd")) // Rejects absolute paths.
        assertNull(storage.resolveFile("other/pic.jpg")) // Rejects non-images subdirectories.
        assertFalse(storage.deleteRelativePath("../secret.txt")) // Refuses deleting outside images/.
    } // Ends path traversal guard verification.
} // Ends LocalImageStorageTest.
