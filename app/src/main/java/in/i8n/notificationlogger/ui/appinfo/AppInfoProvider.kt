package `in`.i8n.notificationlogger.ui.appinfo

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(val label: String, val icon: ImageBitmap?)

/**
 * Resolves an app's label and icon. Apps we cannot see (uninstalled, or not launchable and so
 * hidden by package visibility) fall back to their package name and no icon.
 */
object AppInfoProvider {
    private val cache = ConcurrentHashMap<String, AppInfo>()

    /** Blocking; call off the main thread. */
    fun load(context: Context, packageName: String): AppInfo = cache.getOrPut(packageName) {
        val pm = context.applicationContext.packageManager
        runCatching {
            val info = pm.getApplicationInfo(packageName, 0)
            AppInfo(
                label = pm.getApplicationLabel(info).toString(),
                icon = pm.getApplicationIcon(info).toBitmap(ICON_PX, ICON_PX).asImageBitmap(),
            )
        }.getOrElse { AppInfo(packageName, null) }
    }

    fun label(context: Context, packageName: String): String = load(context, packageName).label

    private const val ICON_PX = 96
}

/** Null while loading. */
@Composable
fun rememberAppInfo(packageName: String): State<AppInfo?> {
    val context = LocalContext.current
    return produceState<AppInfo?>(null, packageName) {
        value = withContext(Dispatchers.IO) { AppInfoProvider.load(context, packageName) }
    }
}
