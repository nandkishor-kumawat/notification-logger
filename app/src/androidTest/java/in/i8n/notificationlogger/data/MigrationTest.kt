package `in`.i8n.notificationlogger.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.i8n.notificationlogger.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the database upgrade path. Schemas are exported to app/schemas/ on every build.
 * When the version is bumped, add the new Migration to MIGRATIONS and a test that creates the
 * old version with data, migrates, and checks the data survived.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(instrumentation, AppDatabase::class.java)

    @Test
    fun version1SchemaMatchesTheExportedSchema() {
        helper.createDatabase(dbName, 1).close()
        // Fails if the entities drifted from schemas/.../1.json without a version bump.
        helper.runMigrationsAndValidate(dbName, 1, true).close()
    }

    @Test
    fun version1DatabaseWithDataOpensWithTheCurrentSchema() = runBlocking {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO notifications (package_name, time, title, text, progress, progress_max, " +
                    "progress_indeterminate, is_oldest_version, is_newest_version, duplicate_group_id) " +
                    "VALUES ('pkg', 123, 'T', 'X', 0, 0, 0, 1, 1, 1)",
            )
            close()
        }
        val context = instrumentation.targetContext
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*MIGRATIONS)
            .build()
        try {
            val item = db.notificationDao().getLastOfApp("pkg")!!
            assertEquals("T", item.title)
            assertEquals(123L, item.time)
        } finally {
            db.close()
            context.deleteDatabase(dbName)
        }
    }

    private companion object {
        val MIGRATIONS = emptyArray<androidx.room.migration.Migration>()
    }
}
