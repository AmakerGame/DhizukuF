package com.EdS.DhizukuF.server

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import com.EdS.DhizukuF.ui.activity.MainActivity
import org.koin.mp.KoinPlatform

class DaemonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val action = intent?.action ?: return

        // Dialer secret code (*#*#3449858#*#*). It does nothing until Security > "Open with a secret code" is on.
        if (action == Telephony.Sms.Intents.SECRET_CODE_ACTION) {
            val enabled = runCatching { KoinPlatform.getKoin().get<SettingsRepo>().isSecretCodeEnabled }
                .getOrDefault(false)
            if (enabled) {
                context.startActivity(
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return
        }

        // DhizukuDAReceiver forwards every device-admin broadcast here (lock task, password
        // failed/succeeded/expiring, ...). Only the ones that can change the owner state may
        // trigger a full sync, otherwise each wrong screen-lock attempt would cost many binder calls.
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED &&
            action != DeviceAdminReceiver.ACTION_DEVICE_ADMIN_DISABLE_REQUESTED &&
            action != DeviceAdminReceiver.ACTION_DEVICE_ADMIN_DISABLED &&
            action != DevicePolicyManager.ACTION_DEVICE_OWNER_CHANGED &&
            action != DevicePolicyManager.ACTION_PROFILE_OWNER_CHANGED &&
            action != DeviceAdminReceiver.ACTION_PROFILE_PROVISIONING_COMPLETE
        ) return
        DhizukuState.sync(context)
    }
}
