package `in`.i8n.notificationlogger.data.settings

import `in`.i8n.notificationlogger.data.model.AppSorting
import `in`.i8n.notificationlogger.data.model.NotificationSorting
import `in`.i8n.notificationlogger.data.model.VersionHandling

data class AppSettings(
    /** false = blacklist mode (log everything except [invertedApps]); true = whitelist mode. */
    val whitelistMode: Boolean = false,
    /** Apps that deviate from the default: blacklisted or whitelisted depending on the mode. */
    val invertedApps: Set<String> = emptySet(),
    val appSorting: AppSorting = AppSorting.NEWEST_FIRST,
    val notificationSorting: NotificationSorting = NotificationSorting.NEWEST_FIRST,
    val versionHandling: VersionHandling = VersionHandling.SHOW_ALL,
    val openNotifications: Boolean = false,
    /** Days to keep notifications; [KEEP_FOREVER] = never delete. */
    val keepingDays: Int = KEEP_FOREVER,
    val hideDuplicates: Boolean = false,
    val requireAuth: Boolean = false,
) {
    fun shouldLog(packageName: String): Boolean =
        invertedApps.contains(packageName) == whitelistMode

    companion object {
        const val KEEP_FOREVER = -1
    }
}
