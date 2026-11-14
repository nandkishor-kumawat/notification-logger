package `in`.i8n.notificationlogger.capture

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.os.Build

/**
 * In-memory map from a logged notification's id to the PendingIntent its app attached to it
 * (what happens when you tap it in the shade). PendingIntents can't be stored in the database,
 * so this only works for recent notifications and is lost when the process dies.
 */
object PendingIntentHolder {
    private const val MAX_ENTRIES = 500

    private val map = object : LinkedHashMap<Long, PendingIntent>(64, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, PendingIntent>) =
            size > MAX_ENTRIES
    }

    fun put(notificationId: Long, intent: PendingIntent) {
        synchronized(map) {
            map.remove(notificationId) // re-insert so it counts as the newest entry
            map[notificationId] = intent
        }
    }

    fun has(notificationId: Long): Boolean = synchronized(map) { map.containsKey(notificationId) }

    fun clear() = synchronized(map) { map.clear() }

    /** True if the app's action was started; false if it is gone, expired or refused. */
    fun open(context: Context, notificationId: Long): Boolean {
        val intent = synchronized(map) { map[notificationId] } ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= 34) {
                // We send from the foreground, but say so explicitly for Android 14+ BAL rules.
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                intent.send(context, 0, null, null, null, null, options.toBundle())
            } else {
                intent.send()
            }
            true
        } catch (_: PendingIntent.CanceledException) {
            synchronized(map) { map.remove(notificationId) }
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
