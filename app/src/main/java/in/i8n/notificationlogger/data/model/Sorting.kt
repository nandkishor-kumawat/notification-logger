package `in`.i8n.notificationlogger.data.model

enum class NotificationSorting { NEWEST_FIRST, OLDEST_FIRST }

/** Name sorting needs app labels, which are not in the DB, so it is applied by the caller. */
enum class AppSorting { NAME, NEWEST_FIRST, OLDEST_FIRST }

enum class VersionHandling { SHOW_ALL, OLDEST_ONLY, NEWEST_ONLY }

/** Row of the app list: an app that has logged notifications. */
data class AppSummary(
    val packageName: String,
    val lastTime: Long,
    val count: Int,
)
