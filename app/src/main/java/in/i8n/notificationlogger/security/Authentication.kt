package `in`.i8n.notificationlogger.security

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import `in`.i8n.notificationlogger.R

/**
 * Whether the user has proven presence since the app was last in the foreground. Lives in the
 * process, so a killed app starts locked, while a rotation (which recreates the activity) does not.
 */
object AuthState {
    /** Grace period so quick stop/start cycles (rotation, permission dialogs) don't re-lock. */
    const val RELOCK_DELAY_MS = 500L

    var unlocked by mutableStateOf(false)

    private val handler = Handler(Looper.getMainLooper())
    private val relock = Runnable { unlocked = false }

    fun onActivityStarted() = handler.removeCallbacks(relock)
    fun onActivityStopped() { handler.postDelayed(relock, RELOCK_DELAY_MS) }
}

object Authenticator {
    private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    /** False if the device has no screen lock/biometric at all, so nothing could be asked. */
    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /** Shows the system prompt: biometrics with PIN/pattern/password as fallback. */
    fun prompt(activity: FragmentActivity, onSuccess: () -> Unit, onFinished: () -> Unit) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onFinished()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFinished()
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.auth_prompt_title))
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info)
    }
}
