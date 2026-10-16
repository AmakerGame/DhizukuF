package com.EdS.DhizukuF.server

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
        if (Binder.getCallingUid() != Process.myUid()) {
            val dangerousOn = settingsRepo.isDangerousEnabled
            if (!dangerousOn || BLOCK_CATASTROPHIC_EVEN_WHEN_ON) {
                // refuse strangers first, so that they learn nothing about the rules
                enforceCallingPermission("remote_transact")
                val method = DangerousCalls.dangerousMethod(binder, code, catastrophicOnly = dangerousOn)
                if (method != null) {
                    Log.w(TAG, "blocked a dangerous call: $method (uid ${Binder.getCallingUid()})")
                    throw SecurityException(
                        "$method is a dangerous call: switch on \"Enable dangerous commands\" in DhizukuF settings"
                    )
                }
            }
        }
        return super.onRemoteTransact(binder, code, data, reply, flags)
    }

    override fun checkCallingPermission(func: String?, callingUid: Int, callingPid: Int): Boolean {
        if (!settingsRepo.isDhizukuEnabled) {
            return false
        }
        // Functions that run client code as the owner or hand out owner powers cannot be filtered by method,
        // so they are dangerous as a whole. The calls of remote_transact are judged in onRemoteTransact.
        if (!settingsRepo.isDangerousEnabled && func in DangerousCalls.UNFILTERABLE_FUNCTIONS) {
            Log.w(TAG, "blocked a dangerous call: $func (uid $callingUid)")
            return false
        }

        val entity = runBlocking { appRepo.findByUID(callingUid) }
        if (entity == null) {
            return false
        }

        if (!entity.allowApi || entity.blocked) {
            return false
        }

        if (settingsRepo.isWhitelistMode && !entity.allowApi) {
            return false
        }

        // Never cache the signature: an updated app with another certificate must be refused.
        val signature = context.packageManager.getPackageInfoForUid(callingUid)?.signature
        if (signature == null) {
            return false
        }

        if (signature != entity.signature) {
            return false
        }

        return true
    }
}
