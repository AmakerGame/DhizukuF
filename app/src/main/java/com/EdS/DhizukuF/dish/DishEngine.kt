package com.EdS.DhizukuF.dish

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.data.common.util.getPackageInfoForUid
import com.EdS.DhizukuF.data.common.util.signature
import com.EdS.DhizukuF.data.settings.model.room.entity.AppEntity
import com.EdS.DhizukuF.data.settings.repo.AppRepo
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import com.EdS.DhizukuF.server.DhizukuState
import com.EdS.DhizukuF.ui.activity.RequestPermissionActivity
import com.rosan.dhizuku.shared.DhizukuVariables
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Brain of dish. The client reaches it through DishBinder, so the caller's uid always comes from
 * Binder.getCallingUid() (Android guarantees it) and the caller is authorized exactly like any
 * other Dhizuku client.
 */
object DishEngine : KoinComponent {
    private const val TAG = "DishEngine"

    // Reply kinds (keep in sync with DishMain.java)
    const val KIND_EXECUTED = 0
    const val KIND_NEED_APPROVAL = 1
    const val KIND_DENIED = 2
    const val KIND_APPROVED = 3

    private const val APPROVAL_TIMEOUT_MS = 60_000L

    class Reply(
        val kind: Int,
        val code: Int = 0,
        val out: String = "",
        val err: String = "",
        val message: String = ""
    )

    private sealed interface Verdict {
        data object Allowed : Verdict
        data object NeedPermission : Verdict
        class Denied(val message: String) : Verdict
    }

    private val appRepo by inject<AppRepo>()
    private val settingsRepo by inject<SettingsRepo>()

    private var binder: DishBinder? = null

    @Synchronized
    fun binder(context: Context): DishBinder =
        binder ?: DishBinder(context.applicationContext).also { binder = it }

    private fun str(context: Context, id: Int, vararg args: Any) = context.getString(id, *args)

    /** Runs a command for [uid], or explains why it cannot run yet. */
    fun execute(context: Context, uid: Int, args: List<String>): Reply {
        // Registration request: the terminal shows up in the app list as "dish (<app>)".
        DishRegistry.register(uid, label(context, uid))

        return when (val verdict = authorize(context, uid)) {
            Verdict.Allowed -> {
                val result = DishCommands(context).execute(args)
                Reply(KIND_EXECUTED, result.code, result.out, result.err)
            }

            is Verdict.Denied -> Reply(KIND_DENIED, message = verdict.message)

            Verdict.NeedPermission -> {
                DishApproval.results.remove(uid)
                launchDialog(context, uid)
                val name = label(context, uid)
                Reply(
                    KIND_NEED_APPROVAL,
                    message = if (settingsRepo.isConfirmationWindow) {
                        str(context, R.string.dish_need_permission, name)
                    } else {
                        str(context, R.string.dish_need_permission_list, name)
                    }
                )
            }
        }
    }

    /** Blocks until the user approves (dialog or app list), refuses, or the time runs out. */
    fun awaitApproval(context: Context, uid: Int): Reply {
        val deadline = SystemClock.elapsedRealtime() + APPROVAL_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (DishApproval.results[uid] == false) {
                DishApproval.results.remove(uid)
                return Reply(KIND_DENIED, message = str(context, R.string.dish_err_denied))
            }
            when (val verdict = authorize(context, uid)) {
                Verdict.Allowed -> return Reply(KIND_APPROVED)
                is Verdict.Denied -> return Reply(KIND_DENIED, message = verdict.message)
                Verdict.NeedPermission -> Unit
            }
            Thread.sleep(300)
        }
        return Reply(KIND_DENIED, message = str(context, R.string.dish_err_denied))
    }

    private fun launchDialog(context: Context, uid: Int) {
        try {
            context.startActivity(
                Intent()
                    .setClassName(context.packageName, RequestPermissionActivity::class.java.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(DhizukuVariables.PARAM_CLIENT_UID, uid)
            )
        } catch (e: Exception) {
            // Background start can be refused; the app list still has the pending entry.
            Log.w(TAG, "cannot open the approval dialog", e)
        }
    }

    private fun authorize(context: Context, uid: Int): Verdict {
        if (!DhizukuState.state.isOwner) {
            return Verdict.Denied(str(context, R.string.dish_err_not_owner))
        }
        if (!settingsRepo.isDhizukuEnabled) {
            return Verdict.Denied(str(context, R.string.dish_err_disabled))
        }

        val signature = context.packageManager.getPackageInfoForUid(uid)?.signature
            ?: return Verdict.Denied(str(context, R.string.dish_err_unknown_caller))
        val entity = runBlocking { appRepo.findByUID(uid) }

        if (entity?.blocked == true) return Verdict.Denied(str(context, R.string.dish_err_blocked))
        if (entity != null && entity.allowApi && entity.signature == signature) return Verdict.Allowed

        // Make the caller visible in DhizukuF > App management (switch off) so it can also be
        // approved from the list, exactly like every other client app.
        registerPending(uid, signature, entity)

        if (settingsRepo.isWhitelistMode) {
            return Verdict.Denied(str(context, R.string.dish_err_whitelist))
        }
        return Verdict.NeedPermission
    }

    private fun registerPending(uid: Int, signature: String, entity: AppEntity?) {
        try {
            runBlocking {
                if (entity == null) {
                    appRepo.insert(AppEntity(uid = uid, signature = signature, allowApi = false))
                } else if (entity.signature != signature) {
                    appRepo.update(entity.copy(signature = signature, allowApi = false))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "cannot register pending app $uid", e)
        }
    }

    fun label(context: Context, uid: Int): String {
        val pm = context.packageManager
        val pkg = pm.getPackagesForUid(uid)?.firstOrNull() ?: return "uid $uid"
        return try {
            pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
        } catch (e: Exception) {
            pkg
        }
    }
}
