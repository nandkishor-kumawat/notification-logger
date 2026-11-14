package `in`.i8n.notificationlogger

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import `in`.i8n.notificationlogger.capture.ServiceCheckUtil
import `in`.i8n.notificationlogger.ui.MainScreen
import `in`.i8n.notificationlogger.ui.onboarding.PermissionCard
import `in`.i8n.notificationlogger.ui.theme.NotificationLoggerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotificationLoggerTheme {
                var listenerEnabled by remember { mutableStateOf(ServiceCheckUtil.isListenerEnabled(this)) }
                // Re-check whenever we come back from system settings.
                LifecycleResumeEffect(Unit) {
                    listenerEnabled = ServiceCheckUtil.isListenerEnabled(this@MainActivity)
                    onPauseOrDispose {}
                }
                MainScreen(permissionCard = {
                    if (!listenerEnabled) {
                        PermissionCard(
                            showRestrictedSettingsHint = ServiceCheckUtil.mayNeedRestrictedSettingsHint(this@MainActivity),
                            onAllow = { openListenerSettings() },
                            onOpenAppInfo = { openAppInfo() },
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                })
            }
        }
    }

    private fun openListenerSettings() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            // Some OEM builds lack this screen; App info is the closest alternative.
            openAppInfo()
        }
    }

    private fun openAppInfo() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
        )
    }
}
