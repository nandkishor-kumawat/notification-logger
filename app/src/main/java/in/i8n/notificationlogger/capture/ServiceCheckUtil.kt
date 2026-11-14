package `in`.i8n.notificationlogger.capture

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

object ServiceCheckUtil {

    /** True if the user has granted this app notification access. */
    fun isListenerEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /**
     * Android 13+ blocks notification access for apps installed outside an app store until the
     * user taps "Allow restricted settings" in App info. We can't query that state directly,
     * so we show the hint whenever the app was not installed by Google Play.
     */
    fun mayNeedRestrictedSettingsHint(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val installer = runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        }.getOrNull()
        return installer != "com.android.vending"
    }
}
