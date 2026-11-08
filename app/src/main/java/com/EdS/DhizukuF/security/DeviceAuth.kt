package com.EdS.DhizukuF.security

import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Asks the user for the screen lock (PIN, pattern, password), a fingerprint or a face, whatever the device
 * offers. Nothing is stored: the system tells only "yes" or "no".
 */
object DeviceAuth {
    private const val TAG = "DeviceAuth"
    private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    /** True when the device has a PIN, pattern or password (many TV boxes have none). */
    fun hasScreenLock(context: Context): Boolean =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isDeviceSecure == true

    /** True when [authenticate] can really ask the user something. */
    fun canAuthenticate(context: Context): Boolean = hasScreenLock(context) &&
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Shows the system prompt. [onResult] gets true only when the user passed it; cancelling, an error and
     * too many wrong tries all give false.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String?,
        onResult: (Boolean) -> Unit
    ) {
        EntryLock.beginAuth()
        var reported = false
        fun report(ok: Boolean) {
            if (reported) return
            reported = true
            EntryLock.endAuth(ok)
            onResult(ok)
        }
        try {
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                        report(true)

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = report(false)
                }
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .apply { if (subtitle != null) setSubtitle(subtitle) }
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
            prompt.authenticate(info)
        } catch (t: Throwable) {
            // A prompt that cannot be shown must never count as a "yes".
            Log.w(TAG, "cannot show the authentication prompt", t)
            report(false)
        }
    }
}

/** The activity behind a Compose [Context], or null. */
fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Remembers, for the whole process, that the user has passed the "when opening the app" check, and locks again
 * after the app was in the background for a while. A system prompt (the PIN screen is an activity of its own)
 * does not count as leaving the app.
 */
object EntryLock {
    private const val RELOCK_AFTER_MS = 30_000L

    @Volatile
    private var unlocked = false

    @Volatile
    private var leftAt = 0L

    @Volatile
    private var busy = false

    fun isUnlocked(): Boolean = unlocked

    fun beginAuth() {
        busy = true
    }

    fun endAuth(success: Boolean) {
        busy = false
        leftAt = 0L
        if (success) unlocked = true
    }

    fun onLeft() {
        if (!busy) leftAt = SystemClock.elapsedRealtime()
    }

    fun onReturned() {
        if (busy || !unlocked || leftAt == 0L) return
        if (SystemClock.elapsedRealtime() - leftAt > RELOCK_AFTER_MS) unlocked = false
    }
}
