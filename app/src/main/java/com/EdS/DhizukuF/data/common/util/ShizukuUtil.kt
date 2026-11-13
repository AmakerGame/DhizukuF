package com.EdS.DhizukuF.data.common.util

import com.EdS.DhizukuF.BuildConfig

import android.content.pm.PackageManager
import android.content.Context
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.ServiceManager
import android.system.Os

import com.EdS.DhizukuF.data.common.model.exception.ShizukuNotWorkException

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

import moe.shizuku.server.IShizukuService

import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.sui.Sui

private suspend fun blockingRequestShizukuPermission() = callbackFlow {
    val requestCode = (Int.MIN_VALUE..Int.MAX_VALUE).random()
    val listener =
        Shizuku.OnRequestPermissionResultListener { _requestCode, grantResult ->
            if (_requestCode != requestCode) return@OnRequestPermissionResultListener
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
                trySend(Unit)
            else close(ShizukuNotWorkException("sui/shizuku permission denied"))
        }
    Shizuku.addRequestPermissionResultListener(listener)
    Shizuku.requestPermission(requestCode)
    awaitClose { Shizuku.removeRequestPermissionResultListener(listener) }
}.catch {
    throw it as? ShizukuNotWorkException ?: ShizukuNotWorkException(cause = it)
}.first()

suspend fun <T> requireShizukuPermissionGranted(action: suspend () -> T): T {
    Sui.init(BuildConfig.APPLICATION_ID)
    val binder = Shizuku.getBinder()
    if (binder == null || !binder.pingBinder())
        throw ShizukuNotWorkException("sui/shizuku isn't activated")
    if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
        blockingRequestShizukuPermission()
    return action()
}

suspend fun <T> requireShizukuPermissionGranted(context: Context, action: suspend () -> T): T {
    Sui.init(context.packageName)
    val binder = Shizuku.getBinder()
    if (binder == null || !binder.pingBinder())
        throw ShizukuNotWorkException("sui/shizuku isn't activated")
    if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
        blockingRequestShizukuPermission()
    return action()
}

fun shizukuBinder(name: String): IBinder =
    shizukuBinder(ServiceManager.getService(name))

fun shizukuBinder(binder: IBinder): IBinder = ShizukuBinderWrapper(binder)

fun checkShizukuWorked(): Boolean {
    val sui: Boolean = Sui.init(BuildConfig.APPLICATION_ID)
    val binder = Shizuku.getBinder()
    return (sui || binder != null)
}

data class ShizukuProcessResult(
    val code: Int,
    val output: String,
    val error: String
)

/** Runs a command through Shizuku's shell (uid 2000). Returns null when Shizuku cannot spawn it. */
fun shizukuExec(vararg command: String): ShizukuProcessResult? {
    val binder = Shizuku.getBinder()
    if (binder == null || !binder.pingBinder()) return null
    return try {
        if (Shizuku.isPreV11()) return null
        val service = IShizukuService.Stub.asInterface(binder) ?: return null
        val process = service.newProcess(command, null, null) ?: return null
        var error = ""
        val errorReader = Thread { error = process.errorStream?.readAllText().orEmpty() }
        errorReader.start()
        val output = process.inputStream?.readAllText()
        errorReader.join()
        ShizukuProcessResult(process.waitFor(), output.orEmpty(), error)
    } catch (_: Throwable) {
        null
    }
}

private fun ParcelFileDescriptor.readAllText(): String =
    ParcelFileDescriptor.AutoCloseInputStream(this).use { it.readBytes().decodeToString() }

/**
 * Safety net for an activation that was interrupted between the bypass and the restore:
 * when user_setup_complete is 0 while the device is provisioned, put it back.
 */
fun restoreStuckUserSetupComplete() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val userId = Os.getuid() / 100000
    val setup = shizukuExec(
        "settings", "--user", "$userId", "get", "secure", "user_setup_complete"
    )?.output?.trim()
    if (setup != "0") return
    val provisioned = shizukuExec(
        "settings", "--user", "$userId", "get", "global", "device_provisioned"
    )?.output?.trim()
    if (provisioned != "1") return
    shizukuExec(
        "settings", "--user", "$userId", "put", "secure", "user_setup_complete", "1"
    )
}
