package com.EdS.DhizukuF.server

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.EdS.DhizukuF.BuildConfig
import android.util.Log
import androidx.core.content.ContextCompat
import com.EdS.DhizukuF.data.common.util.has
import com.EdS.DhizukuF.dish.DishServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data object DhizukuState {
    data class State(val isDeviceOwner: Boolean = false, val isProfileOwner: Boolean = false) {
        val isOwner = isDeviceOwner || isProfileOwner
    }

    var state by mutableStateOf(State())
        private set

    var admin = ComponentName(BuildConfig.APPLICATION_ID, DhizukuDAReceiver::class.java.name)

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val syncMutex = Mutex()

    fun sync(context: Context) {
        val app = context.applicationContext
        val dpm = app.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        // Cheap: update the UI state right away.
        val newState = State(
            isDeviceOwner = dpm.isDeviceOwnerApp(admin.packageName),
            isProfileOwner = dpm.isProfileOwnerApp(admin.packageName)
        )
        state = newState
        // Expensive (many binder calls): never on the main thread, never concurrently.
        syncScope.launch {
            syncMutex.withLock {
                try {
                    onReceive(app, dpm, admin, newState)
                } catch (t: Throwable) {
                    Log.w("DhizukuState", "sync failed", t)
                }
            }
        }
    }

    private fun onReceive(
        context: Context,
        dpm: DevicePolicyManager,
        admin: ComponentName,
        current: State
    ) {
        if (current.isOwner) onEnabled(context, dpm, admin)
        else onDisabled(context, dpm, admin)

        syncDishServer(current.isOwner)
        autoDaemonService(context, current.isOwner)
    }

    private fun syncDishServer(isOwner: Boolean) {
        try {
            if (isOwner) DishServer.start() else DishServer.stop()
        } catch (t: Throwable) {
            Log.w("DhizukuState", "dish server toggle failed", t)
        }
    }

    private fun onEnabled(context: Context, dpm: DevicePolicyManager, admin: ComponentName) {
        grantPermissions(context, dpm, admin)
    }

    private fun onDisabled(context: Context, dpm: DevicePolicyManager, admin: ComponentName) {
    }

    private fun grantPermissions(context: Context, dpm: DevicePolicyManager, admin: ComponentName) {
        val permissions = getAllRequestedPermissions(context, admin).filter {
            it ?: return@filter false
            val permission = getPermissionInfo(context, it) ?: return@filter false
            return@filter if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                permission.protectionFlags.has(PermissionInfo.PROTECTION_DANGEROUS)
            } else {
                @Suppress("DEPRECATION")
                (permission.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS
            }
        }

        permissions.forEach {
            dpm.setPermissionGrantState(
                admin,
                admin.packageName,
                it,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
            )
        }
    }

    private fun getAllRequestedPermissions(context: Context, admin: ComponentName) = try {
        val packageInfo: PackageInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    admin.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    admin.packageName,
                    PackageManager.GET_PERMISSIONS
                )
            }
        packageInfo.requestedPermissions ?: emptyArray()
    } catch (_: Exception) {
        emptyArray()
    }

    private fun getPermissionInfo(context: Context, permission: String) = try {
        context.packageManager.getPermissionInfo(permission, 0)
    } catch (_: Exception) {
        null
    }

    private fun autoDaemonService(context: Context, isOwner: Boolean) {
        val intent = Intent(context, DaemonService::class.java)
        try {
            if (isOwner) ContextCompat.startForegroundService(context, intent)
            else context.stopService(intent)
        } catch (t: Throwable) {
            // e.g. ForegroundServiceStartNotAllowedException when started from background
            Log.w("DhizukuState", "daemon service toggle failed", t)
        }
    }
}
