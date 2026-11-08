package com.EdS.DhizukuF.security

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import org.koin.mp.KoinPlatform

/**
 * "Ask first" for a sensitive action, when the user chose *Only on confirmation* or *Confirmation and opening
 * the app* in Security. Without a screen lock there is nothing to ask, so the action simply goes ahead.
 */
class AuthGate(private val context: Context) {
    private val settingsRepo: SettingsRepo get() = KoinPlatform.getKoin().get()

    /** Whether the action has to be confirmed right now. */
    fun required(): Boolean = settingsRepo.authMode.onConfirm && DeviceAuth.canAuthenticate(context)

    /** Runs [onGranted] at once, or after the user has passed the system prompt. */
    fun confirm(title: String, subtitle: String? = null, onGranted: () -> Unit) {
        if (!required()) {
            onGranted()
            return
        }
        val activity = context.findFragmentActivity() ?: return
        DeviceAuth.authenticate(activity, title, subtitle) { ok -> if (ok) onGranted() }
    }

    /** Always asks (when the device can), whatever the mode is: used to change the Security settings themselves. */
    fun confirmAlways(title: String, subtitle: String? = null, onGranted: () -> Unit) {
        if (!DeviceAuth.canAuthenticate(context)) {
            onGranted()
            return
        }
        val activity = context.findFragmentActivity() ?: return
        DeviceAuth.authenticate(activity, title, subtitle) { ok -> if (ok) onGranted() }
    }
}

@Composable
fun rememberAuthGate(): AuthGate {
    val context = LocalContext.current
    return remember(context) { AuthGate(context) }
}
