package pt.isel.pdm.droidmentor.data.local // Tests Room schema migrations between shipped versions using real schema JSON files.

import androidx.room.testing.MigrationTestHelper // Runs real SQLite schema creation and migration validation.
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory // Uses Android's framework SQLite implementation.
import androidx.test.ext.junit.runners.AndroidJUnit4 // Runs Room migration tests on an Android runtime.
import androidx.test.platform.app.InstrumentationRegistry // Supplies instrumentation context for exported schemas.
import org.junit.Assert.assertEquals // Compares legacy values and newly migrated imagePath defaults.
import org.junit.Assert.assertNull // Checks existing v1 rows migrate with null imagePath.
import org.junit.Rule // Registers MigrationTestHelper lifecycle cleanly.
import org.junit.Test // Marks Room migration verification cases.
import org.junit.runner.RunWith // Uses AndroidJUnit4 runner.

/** Verifies M3-13 Room schema migration from version 1 to version 2 preserves M2 rows cleanly. */
@RunWith(AndroidJUnit4::class) // Executes real SQLite migration against exported schema JSON assets.
class DatabaseMigrationTest { // Validates 1.json -> MIGRATION_1_2 -> 2.json automatically.
    /** Loads exported schemas from the instrumentation assets directory configured in build.gradle.kts. */
    @get:Rule // Closes opened SQLite databases automatically after each test.
    val helper = MigrationTestHelper( // Configures Room migration validator with compiled database metadata.
        InstrumentationRegistry.getInstrumentation(), // Supplies APK assets containing 1.json and 2.json.
        DroidMentorDatabase::class.java, // Validates migrated schema against the Room compiler schema.
        emptyList(), // Uses no auto-migration specs because MIGRATION_1_2 is explicit.
        FrameworkSQLiteOpenHelperFactory(), // Uses standard Android SQLite driver.
    ) // Ends MigrationTestHelper rule setup.

    /** Checks v1 chats and messages survive migration to v2 with imagePath defaulted cleanly to NULL. */
    @Test // Guards against destructive migration or broken SQL ALTER TABLE syntax.
    fun migrate1To2PreservesExistingRowsAndAddsNullableImagePath() { // Creates v1 DB, seeds rows, migrates to v2.
        helper.createDatabase(TEST_DB, 1).apply { // Creates authentic M2 schema version 1 database.
            execSQL("INSERT INTO chats (id, title, createdAt, updatedAt) VALUES ('c1', 'Legacy chat', 1000, 2000)") // Inserts legacy v1 chat.
            execSQL("INSERT INTO messages (id, chatId, role, text, seq, status, createdAt, errorCategory) VALUES ('m1', 'c1', 'USER', 'Legacy question', 0, 'SENT', 1500, NULL)") // Inserts legacy v1 message without imagePath.
            close() // Flushes v1 file before running migration.
        } // Ends v1 database creation.

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 2, true, DroidMentorDatabase.MIGRATION_1_2) // Executes MIGRATION_1_2 and validates schema 2.json.
        migratedDb.query("SELECT id, text, seq, status, imagePath FROM messages WHERE id = 'm1'").use { cursor -> // Queries migrated legacy row.
            check(cursor.moveToFirst()) // Requires legacy row to still exist after migration.
            assertEquals("Legacy question", cursor.getString(1)) // Checks original text survived.
            assertEquals(0L, cursor.getLong(2)) // Checks sequence number survived.
            assertEquals("SENT", cursor.getString(3)) // Checks status survived.
            assertNull(cursor.getString(4)) // Checks newly added imagePath defaulted to NULL.
        } // Closes cursor.
        migratedDb.execSQL("INSERT INTO messages (id, chatId, role, text, seq, status, createdAt, errorCategory, imagePath) VALUES ('m2', 'c1', 'USER', '', 1, 'SENDING', 2500, NULL, 'images/new.jpg')") // Inserts v2 row with relative path.
        migratedDb.query("SELECT imagePath FROM messages WHERE id = 'm2'").use { cursor -> // Reads new column back.
            check(cursor.moveToFirst()) // Requires newly inserted row.
            assertEquals("images/new.jpg", cursor.getString(0)) // Confirms relative path stored cleanly.
        } // Closes cursor.
        migratedDb.close() // Closes migrated database cleanly.
    } // Ends v1 -> v2 migration test.

    private companion object { // Holds isolated database file name for migration tests.
        const val TEST_DB = "migration-test.db" // Distinct from production droidmentor.db.
    } // Ends test constants.
} // Ends DatabaseMigrationTest.
