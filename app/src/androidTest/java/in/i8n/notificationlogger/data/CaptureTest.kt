package `in`.i8n.notificationlogger.data

import android.app.Notification
import android.content.Context
import android.os.Bundle
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.i8n.notificationlogger.capture.CaptureEvent
import `in`.i8n.notificationlogger.capture.NotificationParser
import `in`.i8n.notificationlogger.capture.NotificationSaver
import `in`.i8n.notificationlogger.capture.ParsedNotification
import `in`.i8n.notificationlogger.capture.SystemKey
import `in`.i8n.notificationlogger.data.db.AppDatabase
import `in`.i8n.notificationlogger.data.db.NotificationQuery
import `in`.i8n.notificationlogger.data.model.NotificationItem
import `in`.i8n.notificationlogger.data.model.NotificationSorting
import `in`.i8n.notificationlogger.data.model.VersionHandling
import `in`.i8n.notificationlogger.data.settings.SettingsRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var saver: NotificationSaver
    private lateinit var file: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        file = File.createTempFile("settings", ".preferences_pb").also { it.delete() }
        settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
        saver = NotificationSaver(db, settings, scope)
    }

    @After
    fun tearDown() {
        saver.close()
        scope.cancel()
        db.close()
        file.delete()
    }

    private fun n(text: String, pkg: String = "app", progress: Int = 0, max: Int = 0) =
        ParsedNotification(pkg, time = System.nanoTime(), title = "T", text = text, progress = progress, progressMax = max)

    private fun all(): List<NotificationItem> = runBlocking {
        val q = NotificationQuery.build(null, NotificationSorting.OLDEST_FIRST, VersionHandling.SHOW_ALL, false)
        val r = db.notificationDao().paged(q).load(PagingSource.LoadParams.Refresh(null, 100, false))
        (r as PagingSource.LoadResult.Page).data
    }

    // --- parser ---

    @Test
    fun parserReadsTitleTextAndProgress() {
        val extras = Bundle().apply {
            putCharSequence(Notification.EXTRA_TITLE, "Download")
            putCharSequence(Notification.EXTRA_TEXT, "short")
            putInt(Notification.EXTRA_PROGRESS, 40)
            putInt(Notification.EXTRA_PROGRESS_MAX, 100)
        }
        val p = NotificationParser.parse("pkg", 5L, extras)!!
        assertEquals("Download", p.title)
        assertEquals("short", p.text)
        assertEquals(40, p.progress)
        assertEquals(100, p.progressMax)
        assertFalse(p.progressIndeterminate)
    }

    @Test
    fun parserPrefersBigTextAndFallsBackToSubText() {
        val big = Bundle().apply {
            putCharSequence(Notification.EXTRA_TEXT, "short")
            putCharSequence(Notification.EXTRA_BIG_TEXT, "the long expanded text")
        }
        assertEquals("the long expanded text", NotificationParser.parse("p", 1, big)!!.text)
        val sub = Bundle().apply { putCharSequence(Notification.EXTRA_SUB_TEXT, "sub") }
        assertEquals("sub", NotificationParser.parse("p", 1, sub)!!.text)
    }

    @Test
    fun parserIndeterminateProgressAndEmptyNotifications() {
        val indeterminate = Bundle().apply { putBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, true) }
        assertTrue(NotificationParser.parse("p", 1, indeterminate)!!.progressIndeterminate)
        assertNull(NotificationParser.parse("p", 1, Bundle()))
        val blank = Bundle().apply { putCharSequence(Notification.EXTRA_TITLE, "  ") }
        assertNull(NotificationParser.parse("p", 1, blank))
    }

    // --- saver ---

    @Test
    fun newNotificationCreatesRowAndActiveEntry() = runBlocking {
        saver.handle(CaptureEvent.Posted(n("hi"), 1, ""))
        assertEquals(1, all().size)
        assertNotNull(db.activeNotificationDao().find("app", 1, ""))
    }

    @Test
    fun updatingNotificationCreatesVersions() = runBlocking {
        saver.handle(CaptureEvent.Posted(n("10%", progress = 10, max = 100), 1, "t"))
        saver.handle(CaptureEvent.Posted(n("50%", progress = 50, max = 100), 1, "t"))
        saver.handle(CaptureEvent.Posted(n("100%", progress = 100, max = 100), 1, "t"))
        val rows = all()
        assertEquals(3, rows.size)
        assertEquals(listOf(true, false, false), rows.map { it.isOldestVersion })
        assertEquals(listOf(false, false, true), rows.map { it.isNewestVersion })
        assertEquals(rows.last().id, db.activeNotificationDao().find("app", 1, "t")!!.previousNotificationItemId)
    }

    @Test
    fun unchangedRepostIsNotLoggedAgain() = runBlocking {
        saver.handle(CaptureEvent.Posted(n("same"), 1, ""))
        saver.handle(CaptureEvent.Posted(n("same"), 1, ""))
        assertEquals(1, all().size)
    }

    @Test
    fun removalThenRepostStartsNewChain() = runBlocking {
        saver.handle(CaptureEvent.Posted(n("a"), 1, ""))
        saver.handle(CaptureEvent.Removed(SystemKey("app", 1, "")))
        assertNull(db.activeNotificationDao().find("app", 1, ""))
        saver.handle(CaptureEvent.Posted(n("b"), 1, ""))
        val rows = all()
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.isOldestVersion && it.isNewestVersion })
    }

    @Test
    fun filterSettingsAreRespected() = runBlocking {
        settings.setLogging("blocked", false)
        saver.handle(CaptureEvent.Posted(n("x", pkg = "blocked"), 1, ""))
        saver.handle(CaptureEvent.Posted(n("x", pkg = "ok"), 1, ""))
        assertEquals(listOf("ok"), all().map { it.packageName })
        assertNull(db.activeNotificationDao().find("blocked", 1, ""))
    }

    @Test
    fun syncDropsStaleActiveEntries() = runBlocking {
        saver.handle(CaptureEvent.Posted(n("a"), 1, ""))
        saver.handle(CaptureEvent.Posted(n("b"), 2, ""))
        saver.handle(CaptureEvent.Sync(setOf(SystemKey("app", 2, ""))))
        assertNull(db.activeNotificationDao().find("app", 1, ""))
        assertNotNull(db.activeNotificationDao().find("app", 2, ""))
        assertEquals(2, all().size) // history stays
    }

    @Test
    fun queuedEventsAreProcessedInOrder() {
        for (i in 1..20) saver.submit(CaptureEvent.Posted(n("v$i"), 1, ""))
        val deadline = System.currentTimeMillis() + 5000
        while (all().size < 20 && System.currentTimeMillis() < deadline) Thread.sleep(50)
        val rows = all()
        assertEquals(20, rows.size)
        assertEquals((1..20).map { "v$it" }, rows.map { it.text })
        assertEquals(1, rows.count { it.isNewestVersion })
    }
}
