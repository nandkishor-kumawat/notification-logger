package `in`.i8n.notificationlogger.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.i8n.notificationlogger.data.db.AppDatabase
import `in`.i8n.notificationlogger.data.export.NotificationExporter
import `in`.i8n.notificationlogger.data.model.NotificationItem
import `in`.i8n.notificationlogger.data.model.NotificationSorting
import `in`.i8n.notificationlogger.data.model.VersionHandling
import `in`.i8n.notificationlogger.data.retention.RetentionCleaner
import `in`.i8n.notificationlogger.data.settings.AppSettings
import `in`.i8n.notificationlogger.data.settings.SettingsRepository
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataManagementTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var file: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        file = File.createTempFile("settings", ".preferences_pb").also { it.delete() }
        settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
        file.delete()
    }

    private suspend fun add(pkg: String, text: String, time: Long, prev: Long? = null) =
        db.notificationDao().insert(NotificationItem(packageName = pkg, time = time, title = "T", text = text), prev)

    private suspend fun export(
        pkg: String? = null,
        sorting: NotificationSorting = NotificationSorting.NEWEST_FIRST,
        versions: VersionHandling = VersionHandling.SHOW_ALL,
        hide: Boolean = false,
        header: String? = null,
        chunk: Int = 50,
    ): Pair<String, Int> {
        val out = StringBuilder()
        val count = NotificationExporter(db.notificationDao(), chunk)
            .export(out, pkg, sorting, versions, hide, { "App-$it" }, { "t$it" }, header)
        return out.toString() to count
    }

    // --- retention ---

    @Test
    fun retentionDeletesOnlyOlderThanKeepingDays() = runBlocking {
        val now = 100L * TimeUnit.DAYS.toMillis(1)
        add("a", "old", now - TimeUnit.DAYS.toMillis(31))
        add("a", "edge-new", now - TimeUnit.DAYS.toMillis(29))
        add("a", "new", now)
        settings.setKeepingDays(30)
        val deleted = RetentionCleaner(db.notificationDao(), settings).run(now)
        assertEquals(1, deleted)
        val (text, count) = export(sorting = NotificationSorting.OLDEST_FIRST)
        assertEquals(2, count)
        assertTrue(text.contains("edge-new") && text.contains("new") && !text.contains("old\n"))
    }

    @Test
    fun retentionNeverKeepsEverything() = runBlocking {
        add("a", "x", 1L)
        assertEquals(AppSettings.KEEP_FOREVER, settings.current().keepingDays)
        assertEquals(0, RetentionCleaner(db.notificationDao(), settings).run(Long.MAX_VALUE / 2))
        assertEquals(1, export().second)
    }

    // --- export ---

    @Test
    fun exportFormatForOneAppAndForAllApps() = runBlocking {
        add("a", "hello", 1L)
        add("b", "world", 2L)
        val (one, n1) = export(pkg = "a", header = "App-a")
        assertEquals(1, n1)
        assertEquals("App-a\n\nT\nhello\nt1\n\n", one)
        val (all, n2) = export(sorting = NotificationSorting.OLDEST_FIRST)
        assertEquals(2, n2)
        assertEquals("T\nhello\nApp-a - t1\n\nT\nworld\nApp-b - t2\n\n", all)
    }

    @Test
    fun exportHonorsSortVersionAndDuplicateSettings() = runBlocking {
        val v1 = add("a", "p1", 1L)
        val v2 = add("a", "p2", 2L, v1)
        add("a", "p2", 3L)      // duplicate of v2's content (different notification)
        add("a", "other", 4L)
        assertEquals(listOf("other", "p2", "p2", "p1"), lines(export().first))
        assertEquals(listOf("p1", "p2", "p2", "other"), lines(export(sorting = NotificationSorting.OLDEST_FIRST).first))
        assertEquals(listOf("other", "p2", "p2"), lines(export(versions = VersionHandling.NEWEST_ONLY).first))
        assertEquals(listOf("other", "p2", "p1"), lines(export(hide = true).first))
        assertTrue(v2 > v1)
    }

    private fun lines(text: String) = text.split("\n\n").filter { it.isNotBlank() }.map { it.lines()[1] }

    @Test
    fun exportsThousandsOfEntriesAcrossChunksWithoutGapsOrRepeats() = runBlocking {
        for (i in 1..2503) add("p${i % 7}", "n$i", i.toLong())
        val (text, count) = export(sorting = NotificationSorting.OLDEST_FIRST)
        assertEquals(2503, count)
        assertEquals((1..2503).map { "n$it" }, lines(text))
    }

    @Test
    fun exportIsASnapshotOfRowsExistingAtStart() = runBlocking {
        for (i in 1..120) add("a", "n$i", i.toLong())
        val dao = db.notificationDao()
        val snapshot = dao.maxId()
        val out = StringBuilder()
        // A row added after the snapshot id must not appear even with unlucky chunk boundaries.
        add("a", "late", 999L)
        val rows = dao.list(
            `in`.i8n.notificationlogger.data.db.NotificationQuery.build(
                null, NotificationSorting.OLDEST_FIRST, VersionHandling.SHOW_ALL, false, maxId = snapshot,
            ),
        )
        assertEquals(120, rows.size)
        assertTrue(rows.none { it.text == "late" })
        assertTrue(out.isEmpty())
    }

    @Test
    fun clearingByAppAndAll() = runBlocking {
        add("a", "1", 1L); add("b", "2", 2L)
        db.notificationDao().deleteByApp("a")
        assertEquals(1, export().second)
        db.notificationDao().deleteAll()
        assertEquals(0, export().second)
    }
}
