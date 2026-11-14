package `in`.i8n.notificationlogger.capture

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.os.Build
import androidx.core.app.NotificationManagerCompat

object ServiceCheckUtil {

    /**
     * If access is granted but the listener is not bound (it can get stuck unbound after the
     * app is updated or killed by an OEM task killer), ask the system to bind it again.
     * Does nothing when it is already connected.
     */
    fun requestRebindIfEnabled(context: Context) {
        if (!isListenerEnabled(context)) return
        NotificationListenerService.requestRebind(
            ComponentName(context, LoggerNotificationListenerService::class.java),
        )
    }


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
