package `in`.i8n.notificationlogger.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.i8n.notificationlogger.capture.CaptureEvent
import `in`.i8n.notificationlogger.capture.NotificationSaver
import `in`.i8n.notificationlogger.capture.ParsedNotification
import `in`.i8n.notificationlogger.capture.PendingIntentHolder
import `in`.i8n.notificationlogger.capture.SystemKey
import `in`.i8n.notificationlogger.data.db.AppDatabase
import `in`.i8n.notificationlogger.data.settings.SettingsRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActionsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private lateinit var saver: NotificationSaver
    private lateinit var file: File

    @Before
    fun setUp() {
        PendingIntentHolder.clear()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        file = File.createTempFile("settings", ".preferences_pb").also { it.delete() }
        saver = NotificationSaver(db, SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file }), scope)
    }

    @After
    fun tearDown() {
        saver.close()
        scope.cancel()
        db.close()
        file.delete()
        PendingIntentHolder.clear()
    }

    private fun pi(oneShot: Boolean = false): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent("in.i8n.notificationlogger.TEST_ACTION").setPackage(context.packageName),
        PendingIntent.FLAG_IMMUTABLE or if (oneShot) PendingIntent.FLAG_ONE_SHOT else 0,
    )

    private fun n(text: String) = ParsedNotification("app", System.nanoTime(), "T", text)

    @Test
    fun unknownIdCannotBeOpened() {
        assertFalse(PendingIntentHolder.open(context, 12345))
    }

    @Test
    fun storedIntentOpensAndExpiredOneReportsFailure() {
        PendingIntentHolder.put(1, pi(oneShot = true))
        assertTrue(PendingIntentHolder.open(context, 1))
        assertFalse(PendingIntentHolder.open(context, 1)) // one-shot already used -> canceled
        assertFalse(PendingIntentHolder.has(1))
    }

    @Test
    fun oldestEntriesAreEvictedBeyondTheLimit() {
        for (i in 1L..600L) PendingIntentHolder.put(i, pi())
        assertFalse(PendingIntentHolder.has(1))
        assertFalse(PendingIntentHolder.has(100))
        assertTrue(PendingIntentHolder.has(101))
        assertTrue(PendingIntentHolder.has(600))
    }

    @Test
    fun savedNotificationGetsItsIntent_andUpdatesGetTheirOwn() = runBlocking {
        saver.handle(CaptureEvent.Posted(n("v1"), 1, "", pi()))
        val first = db.activeNotificationDao().find("app", 1)!!.previousNotificationItemId
        assertTrue(PendingIntentHolder.has(first))
        saver.handle(CaptureEvent.Posted(n("v2"), 1, "", pi()))
        val second = db.activeNotificationDao().find("app", 1)!!.previousNotificationItemId
        assertTrue(second != first && PendingIntentHolder.has(second))
    }

    @Test
    fun notificationWithoutIntentStoresNothing() = runBlocking {
        saver.handle(CaptureEvent.Posted(n("x"), 1, "", null))
        val id = db.activeNotificationDao().find("app", 1)!!.previousNotificationItemId
        assertFalse(PendingIntentHolder.has(id))
    }

    @Test
    fun syncRestoresIntentsOfActiveNotifications() = runBlocking {
        saver.handle(CaptureEvent.Posted(n("x"), 1, "", null))
        val id = db.activeNotificationDao().find("app", 1)!!.previousNotificationItemId
        assertFalse(PendingIntentHolder.has(id))
        val key = SystemKey("app", 1, "")
        saver.handle(CaptureEvent.Sync(setOf(key), mapOf(key to pi())))
        assertTrue(PendingIntentHolder.has(id))
    }
}
