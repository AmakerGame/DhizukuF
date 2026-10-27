package com.EdS.DhizukuF.server

import com.EdS.DhizukuF.logging.LogManager

import com.EdS.DhizukuF.logging.LogLevel

import com.EdS.DhizukuF.logging.LogEvent

import com.EdS.DhizukuF.logging.LogCategory

import android.content.ComponentName
import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.Process
import android.util.Log

import com.rosan.dhizuku.aidl.IDhizukuClient
import com.EdS.DhizukuF.data.common.util.getPackageInfoForUid
import com.EdS.DhizukuF.data.common.util.signature
import com.EdS.DhizukuF.data.settings.repo.AppRepo
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import com.rosan.dhizuku.server_api.DhizukuService

import kotlinx.coroutines.runBlocking

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MyDhizukuService(context: Context, admin: ComponentName?, client: IDhizukuClient?) :
    DhizukuService(context, admin, client), KoinComponent {
    private val context by inject<Context>()
    private val appRepo by inject<AppRepo>()
    private val settingsRepo by inject<SettingsRepo>()

    private companion object {
        const val TAG = "MyDhizukuService"

        /**
         * With *Enable dangerous commands* on, the whole API is open (that is what the switch means). Set this to
         * true to keep wipe / factory reset / removing a user blocked even then, as `dish api` does.
         */
        const val BLOCK_CATASTROPHIC_EVEN_WHEN_ON = false
    }

    override fun getVersionName(): String = "$versionCode"

    /**
     * The real action of an API client is the Binder transaction it sends through `remote_transact`.
     * The Dhizuku library does not tell the server which method that is, so it is recognised here
     * (see [DangerousCalls]). Own calls and calls of unapproved apps are not judged here: the first need no
     * switch, the others are refused by the normal permission check.
     */
    override fun onRemoteTransact(binder: IBinder, code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        val uid = Binder.getCallingUid()
        val foreign = uid != Process.myUid()
        if (foreign) {
            val dangerousOn = settingsRepo.isDangerousEnabled
            if (!dangerousOn || BLOCK_CATASTROPHIC_EVEN_WHEN_ON) {
                // refuse strangers first, so that they learn nothing about the rules
                enforceCallingPermission("remote_transact")
                val method = DangerousCalls.dangerousMethod(binder, code, catastrophicOnly = dangerousOn)
                if (method != null) {
                    Log.w(TAG, "blocked a dangerous call: $method (uid $uid)")
                    LogManager.log(
                        LogEvent.CommandBlocked, uid = uid, command = method,
                        message = "Needs \"Enable dangerous commands\" in Security"
                    )
                    throw SecurityException(
                        "$method is a dangerous call: switch on \"Enable dangerous commands\" in DhizukuF settings"
                    )
                }
            }
        }
        // The journal (Settings > Logs). When it is off, nothing below costs more than one cached lookup.
        if (!foreign || !LogManager.isActive(LogCategory.Command)) {
            return super.onRemoteTransact(binder, code, data, reply, flags)
        }
        // A stranger throws here; that refusal is already in the journal as a denied permission.
        enforceCallingPermission("remote_transact")
        val command = DangerousCalls.methodLabel(binder, code) ?: "transaction $code"
        LogManager.log(LogEvent.CommandFromApp, uid = uid, command = command)
        try {
            val ok = super.onRemoteTransact(binder, code, data, reply, flags)
            LogManager.log(
                if (ok) LogEvent.CommandCompleted else LogEvent.CommandFailed,
                uid = uid, command = command, result = if (ok) "OK" else "The system refused the call"
            )
            return ok
        } catch (t: Throwable) {
            LogManager.log(
                LogEvent.CommandFailed, uid = uid, command = command,
                result = "Exception", error = t.toString()
            )
            throw t
        }
    }

    // One refusal of an app is written once per few seconds, not once per call of an app that keeps retrying.
    private val lastDenied = java.util.concurrent.ConcurrentHashMap<Int, Long>()

    private fun deny(uid: Int, func: String?, reason: String): Boolean {
        if (LogManager.isActive(LogCategory.Permission, LogLevel.Warning)) {
            val now = android.os.SystemClock.elapsedRealtime()
            val last = lastDenied[uid]
            if (last == null || now - last > 5_000) {
                lastDenied[uid] = now
                LogManager.log(
                    LogEvent.PermissionDenied, uid = uid, level = LogLevel.Warning,
                    message = reason, command = func
                )
            }
        }
        return false
    }

    override fun checkCallingPermission(func: String?, callingUid: Int, callingPid: Int): Boolean {
        if (!settingsRepo.isDhizukuEnabled) {
            return deny(callingUid, func, "Dhizuku is switched off in Settings")
        }
        // Functions that run client code as the owner or hand out owner powers cannot be filtered by method,
        // so they are dangerous as a whole. The calls of remote_transact are judged in onRemoteTransact.
        if (!settingsRepo.isDangerousEnabled && func in DangerousCalls.UNFILTERABLE_FUNCTIONS) {
            Log.w(TAG, "blocked a dangerous call: $func (uid $callingUid)")
            LogManager.log(
                LogEvent.CommandBlocked, uid = callingUid, command = func,
                message = "Needs \"Enable dangerous commands\" in Security"
            )
            return false
        }

        val entity = runBlocking { appRepo.findByUID(callingUid) }
        if (entity == null) {
            return deny(callingUid, func, "The app is not approved")
        }

        if (!entity.allowApi || entity.blocked) {
            return deny(callingUid, func, if (entity.blocked) "The app is blocked" else "The app is not allowed")
        }
        // Whitelist mode is enforced above: only allowApi apps pass. Blocked entries never pass.

        // Never cache the signature: an updated app with another certificate must be refused.
        val signature = context.packageManager.getPackageInfoForUid(callingUid)?.signature
        if (signature == null) {
            return deny(callingUid, func, "The signature of the app cannot be read")
        }

        if (signature != entity.signature) {
            return deny(callingUid, func, "The app was updated with another signature")
        }

        // Debug level only: every allowed call of the API passes here.
        if (func != null && func != "remote_transact") {
            LogManager.log(
                if (func in DangerousCalls.UNFILTERABLE_FUNCTIONS) LogEvent.CommandFromApp else LogEvent.DebugInfo,
                uid = callingUid, command = func,
                message = "Permission check passed"
            )
        }
        return true
    }
}
