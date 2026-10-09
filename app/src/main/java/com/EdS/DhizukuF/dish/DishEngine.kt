package com.EdS.DhizukuF.dish

import android.content.Context
import android.util.Log
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.data.common.util.getPackageInfoForUid
import com.EdS.DhizukuF.data.common.util.signature
import com.EdS.DhizukuF.data.settings.model.room.entity.AppEntity
import com.EdS.DhizukuF.data.settings.repo.AppRepo
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import com.EdS.DhizukuF.server.DhizukuState
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Brain of dish. The terminal client talks to the app through `am broadcast` (works from any
 * terminal and any SELinux domain, and wakes the app process up when it was killed).
 * The reply travels back as the broadcast result: result code + base64 body.
 */
object DishEngine : KoinComponent {
    private const val TAG = "DishEngine"

    const val CODE_NEED_AUTH = 100   // identity of the client not proven yet
    const val CODE_WAIT = 101        // waiting for the user's approval
    const val CODE_DENIED = 102      // final refusal (body = reason)

    class Reply(val code: Int, val body: String)

    private sealed interface Verdict {
        data object Allowed : Verdict
        data object NeedPermission : Verdict
        class Denied(val message: String) : Verdict
    }

    private val appRepo by inject<AppRepo>()
    private val settingsRepo by inject<SettingsRepo>()

    private fun str(context: Context, id: Int, vararg args: Any) = context.getString(id, *args)

    /**
     * @param sentFromUid uid of the broadcast sender when Android reports it (API 34+), else -1.
     * @param argsEncoded command arguments joined with NUL, base64 (URL safe).
     */
    fun handle(context: Context, token: String?, argsEncoded: String?, sentFromUid: Int): Reply {
        if (!DishSessions.isValidToken(token)) {
            return Reply(CODE_DENIED, "Invalid dish token. Export the dish files again.")
        }
        token!!

        if (DishSessions.uidOf(token) == null && sentFromUid >= 0) {
            DishApproval.results.remove(sentFromUid)
            DishSessions.claim(token, sentFromUid)
        }
        val uid = DishSessions.uidOf(token) ?: return Reply(CODE_NEED_AUTH, "")
        if (sentFromUid >= 0 && sentFromUid != uid) {
            return Reply(CODE_DENIED, "This token belongs to another caller")
        }

        // Registration request: the terminal shows up in the app list as "dish (<app>)".
        DishRegistry.register(uid, label(context, uid))

        when (val verdict = authorize(context, uid)) {
            Verdict.Allowed -> Unit
            is Verdict.Denied -> return Reply(CODE_DENIED, verdict.message)
            Verdict.NeedPermission -> {
                if (DishApproval.results.remove(uid) == false) {
                    return Reply(CODE_DENIED, str(context, R.string.dish_err_denied))
                }
                val name = label(context, uid)
                return Reply(
                    CODE_WAIT,
                    if (settingsRepo.isConfirmationWindow) {
                        str(context, R.string.dish_need_permission, name)
                    } else {
                        str(context, R.string.dish_need_permission_list, name)
                    }
                )
            }
        }

        val args = decodeArgs(argsEncoded)
        val result = DishCommands(context).execute(args)
        return Reply(result.code, result.out + "\u0000" + result.err)
    }

    private fun decodeArgs(encoded: String?): List<String> {
        if (encoded.isNullOrEmpty()) return emptyList()
        val raw = android.util.Base64.decode(encoded, android.util.Base64.URL_SAFE)
        return String(raw, Charsets.UTF_8).split('\u0000')
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
