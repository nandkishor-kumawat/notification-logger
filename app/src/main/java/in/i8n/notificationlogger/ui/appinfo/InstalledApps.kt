package `in`.i8n.notificationlogger.ui.appinfo

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(val packageName: String, val label: String)

object InstalledApps {
    /**
     * Launchable apps plus [extraPackages] (apps that already have logged notifications but no
     * launcher icon, such as system services), sorted by label.
     */
    suspend fun load(context: Context, extraPackages: Collection<String>): List<AppEntry> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val packages = pm.queryIntentActivities(launcher, 0)
                .map { it.activityInfo.packageName }
                .toMutableSet()
            packages += extraPackages
            packages.remove(context.packageName) // we never log ourselves
            packages.map { AppEntry(it, AppInfoProvider.label(context, it)) }
                .sortedBy { it.label.lowercase() }
        }
}
