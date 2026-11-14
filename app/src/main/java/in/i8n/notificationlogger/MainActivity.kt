package `in`.i8n.notificationlogger

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import `in`.i8n.notificationlogger.data.settings.SettingsRepository
import `in`.i8n.notificationlogger.security.AuthState
import `in`.i8n.notificationlogger.security.Authenticator
import `in`.i8n.notificationlogger.security.LockScreen
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

class MainActivity : FragmentActivity() {
    private val settingsRepo by lazy { SettingsRepository(this) }
    private var prompting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotificationLoggerTheme {
                // null = settings not loaded yet: show nothing rather than risk showing the log.
                val requireAuth by remember { settingsRepo.settings.map { it.requireAuth } }
                    .collectAsState(initial = null)

                // Hide content in recents and screenshots whenever authentication is (or may be) on.
                LaunchedEffect(requireAuth) {
                    if (requireAuth == false) {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                // Without any screen lock there is nothing to ask for, so don't lock the user out.
                val locked = requireAuth == true && !AuthState.unlocked && Authenticator.isAvailable(this)
                LifecycleResumeEffect(locked) {
                    if (locked) promptForAuth()
                    onPauseOrDispose {}
                }

                when {
                    requireAuth == null -> Box(Modifier.fillMaxSize())
                    locked -> LockScreen(onUnlock = ::promptForAuth)
                    else -> {
                        var listenerEnabled by remember { mutableStateOf(ServiceCheckUtil.isListenerEnabled(this)) }
                        // Re-check whenever we come back from system settings.
                        LifecycleResumeEffect(Unit) {
                            listenerEnabled = ServiceCheckUtil.isListenerEnabled(this@MainActivity)
                            onPauseOrDispose {}
                        }
                        MainScreen(
                            permissionCard = {
                                if (!listenerEnabled) {
                                    PermissionCard(
                                        showRestrictedSettingsHint = ServiceCheckUtil.mayNeedRestrictedSettingsHint(this@MainActivity),
                                        onAllow = { openListenerSettings() },
                                        onOpenAppInfo = { openAppInfo() },
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    )
                                }
                            },
                            onRequireAuth = ::setRequireAuth,
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AuthState.onActivityStarted()
    }

    override fun onStop() {
        AuthState.onActivityStopped()
        super.onStop()
    }

    private fun promptForAuth() {
        if (prompting) return
        prompting = true
        Authenticator.prompt(
            this,
            onSuccess = { AuthState.unlocked = true },
            onFinished = { prompting = false },
        )
    }

    private fun setRequireAuth(enabled: Boolean) {
        if (enabled && !Authenticator.isAvailable(this)) {
            Toast.makeText(this, R.string.auth_setup_lock, Toast.LENGTH_LONG).show()
            return
        }
        if (!enabled && Authenticator.isAvailable(this)) {
            // Switching protection off must be as hard as getting in: ask again first.
            if (prompting) return
            prompting = true
            Authenticator.prompt(
                this,
                onSuccess = { lifecycleScope.launch { settingsRepo.setRequireAuth(false) } },
                onFinished = { prompting = false },
            )
            return
        }
        // The user is looking at the app right now, so turning it on shouldn't lock them out.
        if (enabled) AuthState.unlocked = true
        lifecycleScope.launch { settingsRepo.setRequireAuth(enabled) }
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
