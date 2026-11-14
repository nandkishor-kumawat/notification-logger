package `in`.i8n.notificationlogger.capture

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import `in`.i8n.notificationlogger.data.db.AppDatabase
import `in`.i8n.notificationlogger.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LoggerNotificationListenerService : NotificationListenerService() {

    private var saver: NotificationSaver? = null

    override fun onCreate() {
        super.onCreate()
        saver = NotificationSaver(
            db = AppDatabase.get(this),
            settings = SettingsRepository(this),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
    }

    override fun onDestroy() {
        saver?.close()
        saver = null
        super.onDestroy()
    }

    override fun onListenerConnected() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        saver?.submit(
            CaptureEvent.Sync(
                activeKeys = active.map { it.key() }.toSet(),
                contentIntents = active.mapNotNull { sbn ->
                    sbn.notification.contentIntent?.let { sbn.key() to it }
                }.toMap(),
            ),
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Never log our own notifications.
        if (sbn.packageName == packageName) return
        val parsed = NotificationParser.parse(
            packageName = sbn.packageName,
            time = sbn.postTime,
            extras = sbn.notification.extras ?: return,
        ) ?: return
        saver?.submit(CaptureEvent.Posted(parsed, sbn.id, sbn.tag.orEmpty(), sbn.notification.contentIntent))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        saver?.submit(CaptureEvent.Removed(sbn.key()))
    }

    private fun StatusBarNotification.key() = SystemKey(packageName, id, tag.orEmpty())
}
