package com.EdS.DhizukuF.dish

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.server.DhizukuState
import java.lang.reflect.InvocationTargetException
import java.util.Locale

/** Device Owner commands, executed inside the Dhizuku process (which is the Device Owner). */
class DishCommands(private val context: Context) {
    class Result(val out: String = "", val err: String = "", val code: Int = 0)

    private class UsageException(val usage: String) : Exception()

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin: ComponentName get() = DhizukuState.admin

    private fun str(id: Int, vararg args: Any) = context.getString(id, *args)

    private fun ok() = Result(out = str(R.string.dish_ok) + "\n")

    private fun yesNo(v: Boolean) = str(if (v) R.string.dish_yes else R.string.dish_no)

    private fun bool(v: Boolean) = Result(out = yesNo(v) + "\n")

    private fun need(a: List<String>, n: Int, usage: String) {
        if (a.size < n) throw UsageException(usage)
    }

    private fun fail(t: Throwable) = Result(
        err = str(R.string.dish_err_failed, "${t.javaClass.simpleName}: ${t.message ?: ""}") + "\n",
        code = 1
    )

    fun execute(args: List<String>): Result {
        if (args.isEmpty()) return Result(out = str(R.string.dish_help))
        val cmd = args[0].lowercase(Locale.ROOT)
        val a = args.drop(1)
        return try {
            dispatch(cmd, a, args[0])
        } catch (e: UsageException) {
            Result(err = str(R.string.dish_err_usage, e.usage) + "\n", code = 2)
        } catch (e: InvocationTargetException) {
            fail(e.targetException ?: e)
        } catch (e: Throwable) {
            fail(e)
        }
    }

    private fun dispatch(cmd: String, a: List<String>, raw: String): Result = when (cmd) {
        "help", "-h", "--help" -> Result(out = str(R.string.dish_help))

        "status" -> Result(
            out = str(
                R.string.dish_status,
                context.packageName,
                yesNo(dpm.isDeviceOwnerApp(context.packageName)),
                yesNo(dpm.isProfileOwnerApp(context.packageName)),
                Build.VERSION.SDK_INT
            )
        )

        "lock" -> {
            dpm.lockNow()
            ok()
        }

        "reboot" -> {
            dpm.reboot(admin)
            ok()
        }

        "hide", "unhide" -> {
            need(a, 1, "$cmd PKG")
            bool(dpm.setApplicationHidden(admin, a[0], cmd == "hide"))
        }

        "is-hidden" -> {
            need(a, 1, "is-hidden PKG")
            bool(dpm.isApplicationHidden(admin, a[0]))
        }

        "suspend", "unsuspend" -> {
            need(a, 1, "$cmd PKG...")
            val failed = dpm.setPackagesSuspended(admin, a.toTypedArray(), cmd == "suspend")
            if (failed.isEmpty()) ok()
            else Result(err = str(R.string.dish_err_failed, failed.joinToString(", ")) + "\n", code = 1)
        }

        "block-uninstall", "allow-uninstall" -> {
            need(a, 1, "$cmd PKG")
            dpm.setUninstallBlocked(admin, a[0], cmd == "block-uninstall")
            ok()
        }

        "is-uninstall-blocked" -> {
            need(a, 1, "is-uninstall-blocked PKG")
            bool(dpm.isUninstallBlocked(admin, a[0]))
        }

        "restriction" -> restriction(a)

        "camera" -> toggle(
            a, "camera disable|enable|status",
            { dpm.setCameraDisabled(admin, it) }, { dpm.getCameraDisabled(admin) }
        )

        "screen-capture" -> toggle(
            a, "screen-capture disable|enable|status",
            { dpm.setScreenCaptureDisabled(admin, it) }, { dpm.getScreenCaptureDisabled(admin) }
        )

        "permission" -> permission(a)

        "settings" -> settings(a)

        "timezone" -> {
            need(a, 1, "timezone ID")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                Result(err = str(R.string.dish_err_unsupported) + "\n", code = 1)
            } else {
                bool(dpm.setTimeZone(admin, a[0]))
            }
        }

        "call" -> call(a)

        else -> Result(err = str(R.string.dish_err_unknown_command, raw) + "\n", code = 127)
    }

    private fun toggle(
        a: List<String>,
        usage: String,
        set: (Boolean) -> Unit,
        get: () -> Boolean
    ): Result {
        need(a, 1, usage)
        return when (a[0]) {
            "disable" -> {
                set(true)
                ok()
            }

            "enable" -> {
                set(false)
                ok()
            }

            "status" -> Result(
                out = str(if (get()) R.string.dish_disabled else R.string.dish_enabled) + "\n"
            )

            else -> throw UsageException(usage)
        }
    }

    private fun restriction(a: List<String>): Result {
        val usage = "restriction add|clear NAME | restriction list"
        need(a, 1, usage)
        return when (a[0]) {
            "list" -> {
                val bundle = dpm.getUserRestrictions(admin)
                val keys = bundle.keySet().filter { bundle.getBoolean(it) }.sorted()
                Result(out = keys.joinToString("\n") + if (keys.isEmpty()) "" else "\n")
            }

            "add" -> {
                need(a, 2, usage)
                dpm.addUserRestriction(admin, a[1])
                ok()
            }

            "clear" -> {
                need(a, 2, usage)
                dpm.clearUserRestriction(admin, a[1])
                ok()
            }

            else -> throw UsageException(usage)
        }
    }

    private fun permission(a: List<String>): Result {
        val usage = "permission grant|deny|default|get PKG PERMISSION"
        need(a, 3, usage)
        val (action, pkg, perm) = a
        return when (action) {
            "grant" -> bool(
                dpm.setPermissionGrantState(
                    admin, pkg, perm, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                )
            )

            "deny" -> bool(
                dpm.setPermissionGrantState(
                    admin, pkg, perm, DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED
                )
            )

            "default" -> bool(
                dpm.setPermissionGrantState(
                    admin, pkg, perm, DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT
                )
            )

            "get" -> {
                val name = when (dpm.getPermissionGrantState(admin, pkg, perm)) {
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED -> "granted"
                    DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED -> "denied"
                    else -> "default"
                }
                Result(out = name + "\n")
            }

            else -> throw UsageException(usage)
        }
    }

    private fun settings(a: List<String>): Result {
        val usage = "settings get global|secure NAME | settings put global|secure NAME VALUE"
        need(a, 3, usage)
        val (action, space, name) = a
        if (space != "global" && space != "secure") throw UsageException(usage)
        return when (action) {
            "get" -> {
                val value = if (space == "global") Settings.Global.getString(context.contentResolver, name)
                else Settings.Secure.getString(context.contentResolver, name)
                Result(out = (value ?: "null") + "\n")
            }

            "put" -> {
                need(a, 4, usage)
                if (space == "global") dpm.setGlobalSetting(admin, name, a[3])
                else dpm.setSecureSetting(admin, name, a[3])
                ok()
            }

            else -> throw UsageException(usage)
        }
    }

    private fun call(a: List<String>): Result {
        val methods = DevicePolicyManager::class.java.methods.filter {
            it.parameterTypes.isNotEmpty() && it.parameterTypes[0] == ComponentName::class.java
        }
        if (a.isEmpty() || a[0] == "--list") {
            val names = methods.map { it.name }.filter { it !in DENIED_METHODS }.distinct().sorted()
            return Result(out = names.joinToString("\n") + "\n")
        }

        val name = a[0]
        if (name in DENIED_METHODS) {
            return Result(err = str(R.string.dish_err_method_denied, name) + "\n", code = 1)
        }
        val values = a.drop(1)
        val candidates = methods.filter { it.name == name && it.parameterTypes.size == values.size + 1 }
        if (candidates.isEmpty()) {
            return Result(err = str(R.string.dish_err_no_method, name) + "\n", code = 1)
        }
        for (method in candidates) {
            val converted = convert(method.parameterTypes.drop(1), values) ?: continue
            val result = method.invoke(dpm, admin, *converted.toTypedArray())
            return Result(out = format(result) + "\n")
        }
        return Result(err = str(R.string.dish_err_bad_args, name) + "\n", code = 1)
    }

    private fun convert(types: List<Class<*>>, values: List<String>): List<Any?>? {
        val out = ArrayList<Any?>()
        for ((i, type) in types.withIndex()) {
            val v = values[i]
            val converted: Any? = when (type) {
                String::class.java -> v
                Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> parseBool(v)
                Int::class.javaPrimitiveType, Int::class.javaObjectType -> v.toIntOrNull()
                Long::class.javaPrimitiveType, Long::class.javaObjectType -> v.toLongOrNull()
                Array<String>::class.java -> v.split(',').filter { it.isNotEmpty() }.toTypedArray()
                else -> null
            }
            if (converted == null) return null
            out.add(converted)
        }
        return out
    }

    private fun parseBool(v: String): Boolean? = when (v.lowercase(Locale.ROOT)) {
        "true", "on", "yes", "1" -> true
        "false", "off", "no", "0" -> false
        else -> null
    }

    @Suppress("DEPRECATION")
    private fun format(r: Any?): String = when (r) {
        null -> str(R.string.dish_ok)
        is Boolean -> yesNo(r)
        is Array<*> -> r.joinToString(", ")
        is IntArray -> r.joinToString(", ")
        is Collection<*> -> r.joinToString(", ")
        is Bundle -> r.keySet().joinToString(", ") { "$it=${r.get(it)}" }
        else -> r.toString()
    }

    companion object {
        /** Destructive or ownership-changing calls are never exposed through `dish call`. */
        private val DENIED_METHODS = setOf(
            "wipeData", "wipeDevice", "clearDeviceOwnerApp", "clearProfileOwner",
            "removeActiveAdmin", "transferOwnership", "resetPassword",
            "resetPasswordWithToken", "setResetPasswordToken"
        )
    }
}
