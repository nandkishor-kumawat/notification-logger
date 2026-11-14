package `in`.i8n.notificationlogger.capture

import androidx.room.withTransaction
import `in`.i8n.notificationlogger.data.db.AppDatabase
import `in`.i8n.notificationlogger.data.model.ActiveNotificationItem
import `in`.i8n.notificationlogger.data.model.NotificationItem
import `in`.i8n.notificationlogger.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Identifies a notification on the system's side. [tag] is "" when the app set none. */
data class SystemKey(val packageName: String, val id: Int, val tag: String)

sealed interface CaptureEvent {
    data class Posted(val notification: ParsedNotification, val id: Int, val tag: String) : CaptureEvent
    data class Removed(val key: SystemKey) : CaptureEvent

    /** The full set of notifications the system currently shows (sent on listener connect). */
    data class Sync(val activeKeys: Set<SystemKey>) : CaptureEvent
}

/**
 * Writes captured notifications to the database. Events are queued and handled one at a
 * time on [scope], so writes are serialized and keep the order the system delivered them.
 */
class NotificationSaver(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    scope: CoroutineScope,
) {
    private val events = Channel<CaptureEvent>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (event in events) {
                // One bad event must not stop the queue.
                runCatching { handle(event) }
            }
        }
    }

    fun submit(event: CaptureEvent) {
        events.trySend(event)
    }

    /** Stops accepting events; already queued ones are still written. */
    fun close() {
        events.close()
    }

    suspend fun handle(event: CaptureEvent) {
        when (event) {
            is CaptureEvent.Posted -> onPosted(event)
            is CaptureEvent.Removed -> db.activeNotificationDao().remove(
                event.key.packageName, event.key.id, event.key.tag,
            )
            is CaptureEvent.Sync -> onSync(event.activeKeys)
        }
    }

    private suspend fun onPosted(event: CaptureEvent.Posted) {
        val n = event.notification
        if (!settings.shouldLog(n.packageName)) return
        val notifications = db.notificationDao()
        val activeDao = db.activeNotificationDao()
        db.withTransaction {
            val active = activeDao.find(n.packageName, event.id, event.tag)
            val previous = active?.let { notifications.getById(it.previousNotificationItemId) }
            // Apps often re-post a notification unchanged (e.g. to bump a timestamp).
            if (previous != null && previous.hasSameContentAs(n)) return@withTransaction

            val newId = notifications.insert(
                NotificationItem(
                    packageName = n.packageName,
                    time = n.time,
                    title = n.title,
                    text = n.text,
                    progress = n.progress,
                    progressMax = n.progressMax,
                    progressIndeterminate = n.progressIndeterminate,
                ),
                previousVersionId = previous?.id,
            )
            if (active == null) {
                activeDao.insert(
                    ActiveNotificationItem(
                        appPackageName = n.packageName,
                        systemId = event.id,
                        systemTag = event.tag,
                        previousNotificationItemId = newId,
                    ),
                )
            } else {
                activeDao.updateLastId(active.id, newId)
            }
        }
    }

    /** Drops tracking rows for notifications that were dismissed while we were not listening. */
    private suspend fun onSync(activeKeys: Set<SystemKey>) {
        val activeDao = db.activeNotificationDao()
        db.withTransaction {
            for (row in activeDao.getAll()) {
                val key = SystemKey(row.appPackageName, row.systemId, row.systemTag)
                if (key !in activeKeys) activeDao.remove(key.packageName, key.id, key.tag)
            }
        }
    }

    private fun NotificationItem.hasSameContentAs(n: ParsedNotification) =
        title == n.title && text == n.text && progress == n.progress &&
            progressMax == n.progressMax && progressIndeterminate == n.progressIndeterminate
}
