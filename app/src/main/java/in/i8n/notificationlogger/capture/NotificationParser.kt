package `in`.i8n.notificationlogger.capture

import android.app.Notification
import android.os.Bundle

object NotificationParser {

    /**
     * Extracts title, text and progress from a notification's [extras].
     * Returns null if there is nothing worth logging (no title, no text, no progress).
     */
    fun parse(packageName: String, time: Long, extras: Bundle): ParsedNotification? {
        val title = extras.str(Notification.EXTRA_TITLE) ?: extras.str(Notification.EXTRA_TITLE_BIG)
        // Prefer the expanded text: collapsed EXTRA_TEXT is often truncated by the app.
        val text = extras.str(Notification.EXTRA_BIG_TEXT)
            ?: extras.str(Notification.EXTRA_TEXT)
            ?: extras.str(Notification.EXTRA_SUB_TEXT)
        val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val hasProgress = indeterminate || max > 0
        if (title == null && text == null && !hasProgress) return null
        return ParsedNotification(
            packageName = packageName,
            time = time,
            title = title,
            text = text,
            progress = if (hasProgress) extras.getInt(Notification.EXTRA_PROGRESS, 0) else 0,
            progressMax = if (hasProgress) max else 0,
            progressIndeterminate = indeterminate,
        )
    }

    private fun Bundle.str(key: String): String? =
        getCharSequence(key)?.toString()?.takeIf { it.isNotBlank() }
}
