package com.EdS.DhizukuF.ui.page.settings.app_management

import android.content.Context
import android.content.pm.PackageManager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.EdS.DhizukuF.data.common.util.getPackageInfoForUid
import com.EdS.DhizukuF.data.common.util.signature
import com.EdS.DhizukuF.data.settings.model.room.entity.AppEntity
import com.EdS.DhizukuF.data.settings.repo.AppRepo
import com.rosan.dhizuku.shared.DhizukuVariables

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

class AppManagementViewModel : ViewModel(), KoinComponent {
    val context by lazy {
        get<Context>()
    }

    private val packageManager: PackageManager by lazy {
        context.packageManager
    }

    private val repo by inject<AppRepo>()

    var state by mutableStateOf(AppManagementViewState())
        private set

    fun collect() {
        collectRepo()
    }

    private var collectRepoJob: Job? = null

    private class Candidate(val applicationInfo: ApplicationInfo, val requested: Boolean)

    private val labelCache = java.util.concurrent.ConcurrentHashMap<Int, String>()

    // Scanning every installed package is slow, so it is done once per refresh
    // (not on every database change) and never on the main thread.
    private fun scanPackages(): List<Candidate> {
        return packageManager
            .getInstalledPackages(PackageManager.GET_PERMISSIONS)
            .mapNotNull { packageInfo ->
                if (packageInfo.packageName == context.packageName) return@mapNotNull null
                val applicationInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                Candidate(
                    applicationInfo,
                    packageInfo.requestedPermissions
                        ?.contains(DhizukuVariables.PERMISSION_API) ?: false
                )
            }
            .distinctBy { it.applicationInfo.packageName }
    }

    fun collectRepo() {
        state = state.copy(loading = true)
        collectRepoJob?.cancel()
        collectRepoJob = viewModelScope.launch(Dispatchers.Default) {
            val candidates = scanPackages()
            repo.flowAll().collect { entities ->
                val byUid = entities.associateBy { it.uid }
                val data = candidates.mapNotNull { c ->
                    val entity = byUid[c.applicationInfo.uid]
                    if (!c.requested && entity == null) return@mapNotNull null
                    AppManagementViewData(
                        applicationInfo = c.applicationInfo,
                        label = labelCache.getOrPut(c.applicationInfo.uid) {
                            c.applicationInfo.loadLabel(packageManager).toString()
                        },
                        enabled = entity?.allowApi ?: false,
                        blocked = entity?.blocked ?: false
                    )
                }.sortedBy { it.applicationInfo.packageName }
                state = state.copy(data = data, loading = false)
            }
        }
    }

    fun setEnabled(uid: Int, bool: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val signature = packageManager.getPackageInfoForUid(uid)?.signature ?: return@launch
            val entity = repo.findByUID(uid)
            if (entity == null)
                repo.insert(AppEntity(uid = uid, signature = signature, allowApi = bool))
            else
                repo.update(entity.copy(signature = signature, allowApi = bool))
        }
    }

    fun setBlocked(uid: Int, bool: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val signature = packageManager.getPackageInfoForUid(uid)?.signature ?: return@launch
            val entity = repo.findByUID(uid)
            if (entity == null)
                repo.insert(AppEntity(uid = uid, signature = signature, allowApi = false, blocked = bool))
            else
                repo.update(entity.copy(signature = signature, blocked = bool, allowApi = if (bool) false else entity.allowApi))
        }
    }
}