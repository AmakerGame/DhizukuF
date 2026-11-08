package com.EdS.DhizukuF.server

import android.annotation.SuppressLint
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.EdS.DhizukuF.BuildConfig
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.logging.LogEvent
import com.EdS.DhizukuF.logging.LogManager
import com.rosan.dhizuku.server_api.LockTaskModeListeners
import org.koin.core.component.KoinComponent

class DhizukuDAReceiver : DeviceAdminReceiver(), KoinComponent {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        DaemonReceiver().onReceive(context, intent)
    }

    override fun onEnabled(context: Context, intent: Intent) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        super.onEnabled(context, intent)
        dpm ?: return
        val ownerTypeRes = when {
            dpm.isDeviceOwnerApp(BuildConfig.APPLICATION_ID) -> R.string.confirm_device_owner
            dpm.isProfileOwnerApp(BuildConfig.APPLICATION_ID) -> R.string.confirm_profile_owner
            else -> null
        }
        if (ownerTypeRes != null) {
            Toast.makeText(
                context,
                context.getString(R.string.home_status_owner_granted, context.getString(ownerTypeRes)),
                Toast.LENGTH_LONG
            ).show()
        } else if (dpm.isAdminActive(DhizukuState.admin)) {
            // Plain device administrator (no Device / Profile Owner): the most limited mode.
            Toast.makeText(
                context,
                context.getString(R.string.home_status_administrator),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        LockTaskModeListeners.notifyLockTaskModeChanged(true, pkg)
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        LockTaskModeListeners.notifyLockTaskModeChanged(false, null)
    }

    // Administrator-mode monitoring: the system reports these to an active device admin when the
    // watch-login policy is on. They are only a signal for the journal, never an action.
    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        securityNote("wrong screen-lock attempt")
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        securityNote("screen lock opened")
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onPasswordExpiring(context: Context, intent: Intent) {
        super.onPasswordExpiring(context, intent)
        securityNote("screen lock is about to expire")
    }

    private fun securityNote(what: String) {
        Log.i(TAG, what)
        LogManager.log(LogEvent.SecurityAlert, message = what)
    }

    private companion object {
        const val TAG = "DhizukuDAReceiver"
    }
}
