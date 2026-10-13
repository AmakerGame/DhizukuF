package com.EdS.DhizukuF.hub

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.EdS.DhizukuF.BuildConfig
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The Dhizuku Compatibility Hub: a tiny hidden app with the package name of the original Dhizuku
 * (com.rosan.dhizuku) that forwards the Dhizuku API to DhizukuF, so apps written for the original
 * Dhizuku work with DhizukuF. It is built together with this app, signed with the same key and has the
 * same version; the APK is carried in this app's assets and installed from there.
 */
object CompatHub {
    const val HUB_PACKAGE = "com.rosan.dhizuku"
    private const val ASSET = "dhizuku_compat_hub.apk"
    private const val TAG = "CompatHub"

    enum class Status {
        /** Not installed. */
        NOT_INSTALLED,

        /** Installed, signed with our key, same or newer version. */
        UP_TO_DATE,

        /** Installed, but older than this app: update it. */
        OUTDATED,

        /** Another app with the hub's package name (the original Dhizuku) is installed. */
        CONFLICT
    }

    class Outcome(val ok: Boolean, val message: String)

    @Suppress("DEPRECATION")
    fun status(context: Context): Status {
        val pm = context.packageManager
        val info = try {
            pm.getPackageInfo(HUB_PACKAGE, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return Status.NOT_INSTALLED
        }
        if (pm.checkSignatures(context.packageName, HUB_PACKAGE) != PackageManager.SIGNATURE_MATCH) {
            return Status.CONFLICT
        }
        val code = PackageInfoCompat.getLongVersionCode(info)
        return if (code >= BuildConfig.VERSION_CODE) Status.UP_TO_DATE else Status.OUTDATED
    }

    /** The name of the app that blocks the hub (the original Dhizuku), or null when nothing blocks it. */
    @Suppress("DEPRECATION")
    fun conflictLabel(context: Context): String? {
        if (status(context) != Status.CONFLICT) return null
        val pm = context.packageManager
        return try {
            pm.getApplicationLabel(pm.getApplicationInfo(HUB_PACKAGE, 0)).toString()
        } catch (_: Exception) {
            HUB_PACKAGE
        }
    }

    private fun hasAsset(context: Context): Boolean = try {
        context.assets.open(ASSET).use { true }
    } catch (_: Exception) {
        false
    }

    /** Installs or updates the hub from the bundled APK. Silent when DhizukuF is the Device Owner. */
    fun install(context: Context): Outcome {
        if (!hasAsset(context)) return Outcome(false, "this build does not contain the hub APK")
        val installer = context.packageManager.packageInstaller
        return awaitResult(context) { sender ->
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            params.setAppPackageName(HUB_PACKAGE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            val id = installer.createSession(params)
            installer.openSession(id).use { session ->
                context.assets.open(ASSET).use { input ->
                    session.openWrite("dhizuku_compat_hub.apk", 0, -1).use { out ->
                        input.copyTo(out)
                        session.fsync(out)
                    }
                }
                session.commit(sender)
            }
        }
    }

    /** Removes the hub. Only a hub signed with our key is removed; anything else needs [replaceConflicting]. */
    fun uninstall(context: Context): Outcome {
        if (status(context) == Status.CONFLICT) {
            return Outcome(false, "$HUB_PACKAGE is not our hub (another signature), it is not removed")
        }
        return removePackage(context)
    }

    private fun removePackage(context: Context): Outcome {
        val installer = context.packageManager.packageInstaller
        return awaitResult(context) { sender -> installer.uninstall(HUB_PACKAGE, sender) }
    }

    /**
     * Removes the app that blocks the hub (the original Dhizuku, same package name, other signature) and
     * installs the hub. Call it only after the user confirmed, naming the app that will be removed.
     */
    fun replaceConflicting(context: Context): Outcome {
        if (status(context) != Status.CONFLICT) return install(context)
        val removed = removePackage(context)
        if (!removed.ok) return removed
        return install(context)
    }

    /** Starts an installer operation and waits for its result (up to two minutes for a user prompt). */
    @Suppress("DEPRECATION")
    private fun awaitResult(context: Context, start: (IntentSender) -> Unit): Outcome {
        val app = context.applicationContext
        val action = "${app.packageName}.HUB_RESULT"
        val latch = CountDownLatch(1)
        var status = Int.MIN_VALUE
        var message = ""
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val st = i.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)
                if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                    // Not the Device Owner: Android wants the user to confirm. The final result follows.
                    val confirm: Intent? = i.getParcelableExtra(Intent.EXTRA_INTENT)
                    if (confirm != null) {
                        try {
                            c.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (e: Exception) {
                            Log.w(TAG, "cannot open the install confirmation", e)
                        }
                    }
                    return
                }
                status = st
                message = i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""
                latch.countDown()
            }
        }
        val filter = IntentFilter(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            app.registerReceiver(receiver, filter)
        }
        try {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val pending = PendingIntent.getBroadcast(
                app, 0, Intent(action).setPackage(app.packageName), flags
            )
            start(pending.intentSender)
            if (!latch.await(120, TimeUnit.SECONDS)) {
                return Outcome(false, "no answer from the installer (check the status again)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "installer operation failed", e)
            return Outcome(false, "${e.javaClass.simpleName}: ${e.message}")
        } finally {
            try {
                app.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // already gone
            }
        }
        return if (status == PackageInstaller.STATUS_SUCCESS) Outcome(true, "")
        else Outcome(false, if (message.isEmpty()) "status $status" else message)
    }
}
