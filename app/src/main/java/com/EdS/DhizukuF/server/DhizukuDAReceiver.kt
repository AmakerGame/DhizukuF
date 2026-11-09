package com.EdS.DhizukuF.server

import android.annotation.SuppressLint
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
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
        super.onEnabled(context, intent)
        // This broadcast fires as soon as setActiveAdmin lands — before a following
        // setDeviceOwner/setProfileOwner call finishes, and even when that later step
        // fails. Re-check after a short delay and only confirm the state the user
        // actually asked for; a mismatch means the Activate page already reported
        // the error, so no toast is shown at all.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            showActivationToast(context)
        }, 1200)
    }

    private fun showActivationToast(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return
        val expected = pendingKind?.takeIf {
            SystemClock.elapsedRealtime() - pendingAt < PENDING_TTL_MS
        }
        pendingKind = null
        pendingAt = 0
        val actualKind = when {
            dpm.isDeviceOwnerApp(BuildConfig.APPLICATION_ID) -> KIND_DEVICE
            dpm.isProfileOwnerApp(BuildConfig.APPLICATION_ID) -> KIND_PROFILE
            dpm.isAdminActive(DhizukuState.admin) -> KIND_ADMIN
            else -> null
        }
        if (expected != null) {
            // An activation started from the Activate page only earns a toast when the
            // final state matches what was requested; otherwise the dialog shows the error.
            if (actualKind != expected) return
        } else {
            // Manual toggle through the system UI: the highest role wins the label, so an
            // already-owner app would be announced as "Device Owner" even though the user
            // only flipped the Administrator switch. Sync first (the process may be cold),
            // then stay quiet when the app already held this right — nothing changed.
            DhizukuState.sync(context)
            val known = DhizukuState.state
            val alreadyHad = when (actualKind) {
                KIND_DEVICE, KIND_PROFILE -> known.isOwner
                KIND_ADMIN -> known.isAdministrator
                else -> false
            }
            if (alreadyHad) return
        }
        when (actualKind) {
            KIND_DEVICE, KIND_PROFILE -> Toast.makeText(
                context,
                context.getString(
                    R.string.home_status_owner_granted,
                    context.getString(
                        if (actualKind == KIND_DEVICE) R.string.confirm_device_owner
                        else R.string.confirm_profile_owner
                    )
                ),
                Toast.LENGTH_LONG
            ).show()
            KIND_ADMIN -> Toast.makeText(
                context,
                context.getString(R.string.home_status_administrator),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private const val TAG = "DhizukuDAReceiver"
        internal const val KIND_DEVICE = "device"
        internal const val KIND_PROFILE = "profile"
        internal const val KIND_ADMIN = "admin"
        private const val PENDING_TTL_MS = 15_000L

        @Volatile
        private var pendingKind: String? = null

        @Volatile
        private var pendingAt: Long = 0

        /**
         * Called by the Activate page right before it grants a right to THIS app, so the
         * delayed toast confirms the intended result instead of a transient intermediate
         * state (or nothing at all when the grant failed).
         */
        fun expectActivation(kind: String) {
            pendingKind = kind
            pendingAt = SystemClock.elapsedRealtime()
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
}
