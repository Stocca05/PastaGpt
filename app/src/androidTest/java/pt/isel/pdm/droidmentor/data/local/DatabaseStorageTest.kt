package pt.isel.pdm.droidmentor.data.local // Groups tests of the app's database configuration.

import android.content.Context // Provides the app's private database directory.
import android.database.sqlite.SQLiteDatabase // Reads the controlled test file after a rejected migration.
import androidx.sqlite.db.SimpleSQLiteQuery // Creates a direct query to check Room's main-thread guard.
import androidx.test.ext.junit.runners.AndroidJUnit4 // Runs these tests on an Android device.
import androidx.test.platform.app.InstrumentationRegistry // Provides app and test-APK contexts.
import java.util.UUID // Gives every test its own file name, separate from real user history.
import kotlinx.coroutines.flow.first // Reads a stored chat-list snapshot.
import kotlinx.coroutines.runBlocking // Lets test methods call suspend DAOs; used only in tests.
import kotlinx.coroutines.withTimeout // Limits waits for observable database results.
import kotlinx.serialization.json.Json // Reads the exported Room schema with the approved JSON library.
import kotlinx.serialization.json.boolean // Reads the exported index uniqueness flag.
import kotlinx.serialization.json.int // Reads the exported schema version.
import kotlinx.serialization.json.jsonArray // Reads the exported lists of fields, indexes and tables.
import kotlinx.serialization.json.jsonObject // Reads named values inside schema objects.
import kotlinx.serialization.json.jsonPrimitive // Reads simple schema values such as text and numbers.
import org.junit.After // Marks cleanup after each test.
import org.junit.Assert.assertEquals // Checks saved values and schema settings.
import org.junit.Assert.assertFalse // Checks that forbidden or computed-only fields are absent.
import org.junit.Assert.assertTrue // Checks guard behaviour and expected rows.
import org.junit.Before // Marks setup before each test.
import org.junit.Test // Marks each database test case.
import org.junit.runner.RunWith // Chooses the Android test runner.
import pt.isel.pdm.droidmentor.data.local.entity.ChatEntity // Supplies a parent row for persistence tests.
import pt.isel.pdm.droidmentor.data.local.entity.MessageEntity // Supplies the saved user message.
import pt.isel.pdm.droidmentor.domain.MessageErrorCategory // Supplies a typed saved failure.
import pt.isel.pdm.droidmentor.domain.MessageStatus // Supplies pending and failed exchange states.
import pt.isel.pdm.droidmentor.domain.Role // Supplies the user and model roles.

/** Checks schema export, disk persistence and the production database's safe defaults. */
@RunWith(AndroidJUnit4::class) // Runs the class using the Android JUnit runner.
class DatabaseStorageTest { // Keeps the production database checks together.
    private lateinit var context: Context // Holds the application storage context used by the tests.
    private lateinit var fileName: String // Holds a unique file name that never targets real app history.
    private lateinit var database: DroidMentorDatabase // Holds the real database instance used by the current test.

    /** Builds a new database using a test-only file name. */
    @Before // Runs setup before each test.
    fun createDatabase() { // Prepares an isolated disk-backed database.
        context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext // Uses the app's storage context.
        fileName = "droidmentor-test-${UUID.randomUUID()}.db" // Avoids sharing a file with another test or a real user.
        database = DroidMentorDatabase.create(context, fileName) // Uses the same builder as the app will use.
    } // Ends the setup method.

    /** Closes and removes only the file created by this test. */
    @After // Runs cleanup after the assertions, including failed cases.
    fun closeDatabase() { // Releases the test database and its file.
        database.close() // Closes any Room connection to this test file.
        context.deleteDatabase(fileName) // Deletes only this unique test database and its side files.
    } // Ends test cleanup.

    /** Checks that a completed exchange and its next sequence survive closing and reopening. */
    @Test // Checks actual file persistence rather than only in-memory storage.
    fun completedHistorySurvivesReopening() = runBlocking { // Allows suspend writes and reads from the test thread.
        database.chatDao().insert(chat()) // Saves the parent conversation on disk.
        val pending = database.messageDao().insert(message()) // Saves its first pending user message.
        assertTrue(database.messageDao().completeExchange(pending.id, "reply-1", "Mentor reply", 2000L)) // Saves the completed exchange.
        val savedChat = database.chatDao().getById("chat-1") // Reads the exact parent row that should survive.
        val savedHistory = database.messageDao().getMessages("chat-1") // Reads the exact message rows that should survive.
        database.close() // Closes the first Room instance.
        database = DroidMentorDatabase.create(context, fileName) // Opens a new instance for the same test file.
        assertEquals(savedChat, database.chatDao().getById("chat-1")) // Checks the parent was not recreated or lost.
        assertEquals(savedHistory, database.messageDao().getMessages("chat-1")) // Checks both messages and their metadata survived.
        val preview = withTimeout(5000L) { database.chatDao().observeChats().first() } // Reads the related preview after reopening.
        assertEquals("reply-1", preview.single().lastMessage?.id) // Checks the last-message relation still works.
        val next = database.messageDao().insert(message().copy(id = "user-2", createdAt = 3000L)) // Continues the persisted history.
        assertEquals(2L, next.seq) // Checks sequence allocation did not restart at zero.
        assertEquals(1, database.openHelper.readableDatabase.version) // Checks the actual file uses schema version one.
    } // Ends the history persistence test.

    /** Checks that a persisted failure can be recovered on the same row after reopening. */
    @Test // Checks optional error conversion through the production database file.
    fun savedFailureCanBeRetriedAfterReopening() = runBlocking { // Calls the suspend persistence methods.
        database.chatDao().insert(chat()) // Saves the failed message's parent.
        val pending = database.messageDao().insert(message()) // Saves the original row.
        assertTrue(database.messageDao().updateStatus(pending.id, MessageStatus.FAILED, MessageErrorCategory.TIMEOUT)) // Records a failure.
        database.close() // Ends the first connection.
        database = DroidMentorDatabase.create(context, fileName) // Reopens that same file with a new Room instance.
        val failed = database.messageDao().getMessages("chat-1").single() // Reads the saved failure.
        assertEquals(MessageStatus.FAILED, failed.status) // Checks its state survived closing.
        assertEquals(MessageErrorCategory.TIMEOUT, failed.errorCategory) // Checks the typed reason survived too.
        assertTrue(database.messageDao().updateStatus(pending.id, MessageStatus.SENDING)) // Prepares the existing row for retry.
        assertEquals(listOf(pending), database.messageDao().getMessages("chat-1")) // Checks its ID and sequence stayed unchanged and its error was cleared.
    } // Ends the saved-failure test.

    /** Checks that an unsupported file version is rejected without deleting saved chats. */
    @Test // Checks that the builder has no destructive-migration fallback.
    fun unsupportedVersionDoesNotEraseSavedData() = runBlocking { // Performs controlled file changes on the test thread.
        database.chatDao().insert(chat()) // Saves a row whose preservation can be checked.
        database.openHelper.writableDatabase.version = 2 // Simulates an unsupported newer schema only in this test file.
        database.close() // Closes the modified file before trying the normal app configuration.
        database = DroidMentorDatabase.create(context, fileName) // Builds the version-one Room instance again.
        var rejected = false // Records whether Room refused the unsupported downgrade.
        try { // Attempts to read through the incompatible configuration.
            database.chatDao().getById("chat-1") // Opens the file and triggers Room's version check.
        } catch (error: IllegalStateException) { // Catches the expected missing-migration error inside the test.
            rejected = true // Records that Room refused to recreate the tables.
        } // Ends the expected-error handling.
        assertTrue("An unsupported version must be rejected", rejected) // Checks the unsafe downgrade did not proceed.
        database.close() // Releases the failed Room instance before inspecting its file.
        val path = context.getDatabasePath(fileName).absolutePath // Locates only the database created by this test.
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use { file -> // Inspects it without changing its contents.
            file.rawQuery("SELECT title FROM chats WHERE id = ?", arrayOf("chat-1")).use { cursor -> // Reads the preserved parent row.
                assertTrue(cursor.moveToFirst()) // Checks that the row still exists.
                assertEquals("Saved chat", cursor.getString(0)) // Checks the value was not replaced by an empty schema.
            } // Closes the inspection cursor.
        } // Closes the read-only inspection connection.
    } // Ends the unsupported-version test.

    /** Checks that the production database refuses direct queries on Android's main thread. */
    @Test // Checks Room's default thread guard without changing the builder settings.
    fun mainThreadQueriesAreRejected() = runBlocking { // Opens the database using a suspend DAO from the test thread.
        database.chatDao().insert(chat()) // Opens and prepares the file away from the UI thread.
        var rejected = false // Records whether Room blocked the direct main-thread query.
        InstrumentationRegistry.getInstrumentation().runOnMainSync { // Runs only the guarded call on the Android main thread.
            try { // Keeps the expected exception inside the test callback.
                database.query(SimpleSQLiteQuery("SELECT id FROM chats")).close() // Room should refuse this call before database IO.
            } catch (error: IllegalStateException) { // Catches Room's main-thread protection error.
                rejected = true // Records that the unsafe query was blocked.
            } // Ends the main-thread error handling.
        } // Waits for the main-thread callback to finish.
        assertTrue("Main-thread queries must be blocked", rejected) // Checks the builder did not enable unsafe queries.
    } // Ends the main-thread guard test.

    /** Checks that the schema packaged from app/schemas describes the agreed version-one tables. */
    @Test // Checks the exported artifact that will be kept in Git.
    fun exportedSchemaMatchesTheInitialDatabaseContract() { // Reads only generated schema metadata, not user rows.
        val assets = InstrumentationRegistry.getInstrumentation().context.assets // Uses test-APK assets containing the exported schema.
        val assetPath = "pt.isel.pdm.droidmentor.data.local.DroidMentorDatabase/1.json" // Selects the version-one app schema.
        val text = assets.open(assetPath).bufferedReader().use { it.readText() } // Reads and closes the schema file.
        val schema = Json.parseToJsonElement(text).jsonObject.getValue("database").jsonObject // Decodes metadata with kotlinx.serialization.
        assertEquals(1, schema.getValue("version").jsonPrimitive.int) // Checks the exported database version.
        val tables = schema.getValue("entities").jsonArray.associate { entity -> // Indexes the exported table definitions by name.
            val table = entity.jsonObject // Reads one table's metadata.
            table.getValue("tableName").jsonPrimitive.content to table // Associates that name with its complete definition.
        } // Ends the table-name mapping.
        assertEquals(setOf("chats", "messages"), tables.keys) // Checks that only history tables are exported.
        val messageTable = tables.getValue("messages") // Selects the exported child-table definition.
        val columns = messageTable.getValue("fields").jsonArray.map { it.jsonObject.getValue("columnName").jsonPrimitive.content }.toSet() // Reads its column names.
        assertEquals(setOf("id", "chatId", "role", "text", "seq", "status", "createdAt", "errorCategory"), columns) // Checks the M2 fields.
        assertFalse("apiKey" in columns || "imagePath" in columns || "lastMessageId" in columns) // Keeps credentials, attachments and query-only IDs out of storage.
        val foreignKey = messageTable.getValue("foreignKeys").jsonArray.single().jsonObject // Reads the parent relationship.
        assertEquals("chats", foreignKey.getValue("table").jsonPrimitive.content) // Checks the correct parent table.
        assertEquals("CASCADE", foreignKey.getValue("onDelete").jsonPrimitive.content) // Checks the exported deletion policy.
        val uniqueIndexes = messageTable.getValue("indices").jsonArray.map { it.jsonObject }.filter { it.getValue("unique").jsonPrimitive.boolean } // Reads uniqueness rules.
        val uniqueColumns = uniqueIndexes.single().getValue("columnNames").jsonArray.map { it.jsonPrimitive.content } // Reads the composite index columns.
        assertEquals(listOf("chatId", "seq"), uniqueColumns) // Checks positions are unique within their chat.
    } // Ends the exported-schema test.

    private fun chat(): ChatEntity { // Creates the parent row used by file persistence checks.
        return ChatEntity(id = "chat-1", title = "Saved chat", createdAt = 1000L, updatedAt = 1000L) // Uses fixed metadata for repeatable assertions.
    } // Ends the parent helper.

    private fun message(): MessageEntity { // Creates the pending user turn used by file persistence checks.
        return MessageEntity( // Builds the real message entity saved by the production DAO.
            id = "user-1", // Uses a local test-only ID.
            chatId = "chat-1", // References the parent created by the helper.
            role = Role.USER, // Represents the first user turn.
            text = "Test message", // Uses fixed content without credentials.
            seq = 0L, // The DAO will allocate the actual sequence when inserting.
            status = MessageStatus.SENDING, // Starts the exchange awaiting a reply.
            createdAt = 1000L, // Uses a fixed timestamp.
        ) // Ends the message constructor.
    } // Ends the message helper.
} // Ends the production database test class.
