package com.EdS.DhizukuF.server

import android.content.ComponentName
import android.content.Context
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
        val DANGEROUS_WORDS = listOf("wipe", "factory", "reset", "remove_user", "removeuser")
    }

    override fun getVersionName(): String = "$versionCode"

    private fun looksDangerous(func: String?): Boolean {
        val name = func?.lowercase() ?: return false
        return DANGEROUS_WORDS.any { name.contains(it) }
    }

    override fun checkCallingPermission(func: String?, callingUid: Int, callingPid: Int): Boolean {
        if (!settingsRepo.isDhizukuEnabled) {
            return false
        }
        // Best effort: the library passes only the name of the API function, not the
        // DevicePolicyManager method, so only dangerous function names can be recognised here.
        if (!settingsRepo.isDangerousEnabled && looksDangerous(func)) {
            Log.w("MyDhizukuService", "blocked a dangerous call: $func (uid $callingUid)")
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
