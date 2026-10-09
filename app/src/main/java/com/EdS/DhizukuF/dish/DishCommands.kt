package com.EdS.DhizukuF.dish

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import android.os.UserManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import android.provider.Settings
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.server.DhizukuState
import java.lang.reflect.InvocationTargetException
import java.util.Locale

/** Device Owner commands, executed inside the Dhizuku process (which is the Device Owner). */
@SuppressLint("NewApi")
class DishCommands(private val context: Context, private val uid: Int = -1) {
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
        "help", "-h", "--help", "?" -> help(a)

        "ping" -> Result()

        "version", "-v", "--version" -> version()

        "id", "whoami" -> whoami()

        "device" -> device()

        "wipe" -> Result(err = str(R.string.dish_err_method_denied, "wipe") + "\n", code = 1)

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

        "list" -> list(a)

        "app-info" -> appInfo(a)

        "is-suspended" -> {
            need(a, 1, USAGE.getValue("is-suspended"))
            bool(dpm.isPackageSuspended(admin, a[0]))
        }

        "enable-system-app" -> {
            need(a, 1, USAGE.getValue("enable-system-app"))
            dpm.enableSystemApp(admin, a[0])
            ok()
        }

        "install-existing" -> {
            need(a, 1, USAGE.getValue("install-existing"))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                Result(err = str(R.string.dish_err_unsupported) + "\n", code = 1)
            } else {
                bool(dpm.installExistingPackage(admin, a[0]))
            }
        }

        "clear-data" -> clearData(a)

        "permission-policy" -> permissionPolicy(a)

        "keyguard" -> toggle(
            a, USAGE.getValue("keyguard"),
            { dpm.setKeyguardDisabled(admin, it) }, null
        )

        "status-bar" -> toggle(
            a, USAGE.getValue("status-bar"),
            { dpm.setStatusBarDisabled(admin, it) }, null
        )

        "mute" -> onOff(
            a, USAGE.getValue("mute"),
            { dpm.setMasterVolumeMuted(admin, it) }, { dpm.isMasterVolumeMuted(admin) }
        )

        "adb" -> globalFlag(a, USAGE.getValue("adb"), Settings.Global.ADB_ENABLED, "1", "0",
            enableWord = "enable", disableWord = "disable", onLabel = "enabled", offLabel = "disabled")

        "stay-awake" -> globalFlag(a, USAGE.getValue("stay-awake"),
            Settings.Global.STAY_ON_WHILE_PLUGGED_IN, "7", "0",
            enableWord = "on", disableWord = "off", onLabel = "on", offLabel = "off")

        "auto-time" -> autoTime(a, zone = false)

        "auto-timezone" -> autoTime(a, zone = true)

        "private-dns" -> privateDns(a)

        "owner-info" -> ownerInfo(a)

        "org-name" -> orgName(a)

        "call" -> call(a)

        else -> {
            val hints = USAGE.keys.filter { it.startsWith(cmd.take(2)) }.take(5)
            val extra = if (hints.isEmpty()) "" else "Did you mean: " + hints.joinToString(", ") + "?\n"
            Result(err = str(R.string.dish_err_unknown_command, raw) + "\n" + extra, code = 127)
        }
    }

    private fun toggle(
        a: List<String>,
        usage: String,
        set: (Boolean) -> Unit,
        get: (() -> Boolean)?
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

            "status" -> {
                if (get == null) throw UsageException(usage)
                Result(out = str(if (get()) R.string.dish_disabled else R.string.dish_enabled) + "\n")
            }

            else -> throw UsageException(usage)
        }
    }

    /** on|off|status for a feature where "on" means the feature is active. */
    private fun onOff(
        a: List<String>,
        usage: String,
        set: (Boolean) -> Unit,
        get: () -> Boolean
    ): Result {
        need(a, 1, usage)
        return when (a[0]) {
            "on" -> { set(true); ok() }
            "off" -> { set(false); ok() }
            "status" -> Result(out = (if (get()) "on" else "off") + "\n")
            else -> throw UsageException(usage)
        }
    }

    private fun globalFlag(
        a: List<String>,
        usage: String,
        name: String,
        on: String,
        off: String,
        enableWord: String,
        disableWord: String,
        onLabel: String,
        offLabel: String
    ): Result {
        need(a, 1, usage)
        return when (a[0]) {
            enableWord -> { dpm.setGlobalSetting(admin, name, on); ok() }
            disableWord -> { dpm.setGlobalSetting(admin, name, off); ok() }
            "status" -> {
                val v = Settings.Global.getString(context.contentResolver, name)
                Result(out = (if (v != null && v != "0") onLabel else offLabel) + "\n")
            }
            else -> throw UsageException(usage)
        }
    }

    private fun restriction(a: List<String>): Result {
        val usage = USAGE.getValue("restriction")
        need(a, 1, usage)
        return when (a[0]) {
            "available" -> {
                val names = UserManager::class.java.fields
                    .filter { it.name.startsWith("DISALLOW_") && it.type == String::class.java }
                    .mapNotNull { runCatching { it.get(null) as? String }.getOrNull() }
                    .sorted()
                Result(out = names.joinToString("\n") + "\n")
            }

            "has" -> {
                need(a, 2, usage)
                bool(dpm.getUserRestrictions(admin).getBoolean(a[1]))
            }

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
        val usage = USAGE.getValue("settings")
        if (a.size == 2 && a[0] == "list" && (a[1] == "global" || a[1] == "secure")) {
            return settingsList(a[1])
        }
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

    // ------------------------------------------------------------------ extra commands

    private fun help(a: List<String>): Result {
        if (a.isEmpty()) return Result(out = str(R.string.dish_help))
        val usage = USAGE[a[0].lowercase(Locale.ROOT)]
            ?: return Result(err = str(R.string.dish_err_unknown_command, a[0]) + "\n", code = 127)
        return Result(out = "Usage: $usage\n")
    }

    private fun appVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (_: Throwable) {
        "?"
    }

    private fun version() = Result(out = "DhizukuF ${appVersion()}\ndish protocol 1\n")

    private fun whoami(): Result {
        val pkgs = context.packageManager.getPackagesForUid(uid)?.joinToString(", ") ?: "?"
        val name = DishRegistry.displayName(uid, pkgs)
        return Result(out = "uid=$uid\napp=$name\npackages=$pkgs\n")
    }

    private fun device() = Result(
        out = "Model: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
            "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
            "Security patch: ${Build.VERSION.SECURITY_PATCH}\n" +
            "Build: ${Build.DISPLAY}\n" +
            "ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}\n"
    )

    @Suppress("DEPRECATION")
    private fun list(a: List<String>): Result {
        val usage = USAGE.getValue("list")
        need(a, 1, usage)
        val kind = a[0]
        if (kind !in setOf("all", "hidden", "suspended", "uninstall-blocked", "system", "user")) {
            throw UsageException(usage)
        }
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)
        val names = apps.filter {
            val system = it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
            when (kind) {
                "all" -> true
                "system" -> system
                "user" -> !system
                "hidden" -> runCatching { dpm.isApplicationHidden(admin, it.packageName) }.getOrDefault(false)
                "suspended" -> runCatching { dpm.isPackageSuspended(admin, it.packageName) }.getOrDefault(false)
                else -> runCatching { dpm.isUninstallBlocked(admin, it.packageName) }.getOrDefault(false)
            }
        }.map { it.packageName }.sorted()
        return Result(out = names.joinToString("\n") + if (names.isEmpty()) "" else "\n")
    }

    @Suppress("DEPRECATION")
    private fun appInfo(a: List<String>): Result {
        need(a, 1, USAGE.getValue("app-info"))
        val pkg = a[0]
        val pm = context.packageManager
        val info = try {
            pm.getPackageInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        } catch (_: PackageManager.NameNotFoundException) {
            return Result(err = str(R.string.dish_err_failed, "no such package: $pkg") + "\n", code = 1)
        }
        val system = (info.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
        val sb = StringBuilder()
        sb.append("Package: $pkg\n")
        sb.append("Version: ${info.versionName ?: "?"}\n")
        sb.append("System app: ${yesNo(system)}\n")
        sb.append("Hidden: ${yesNo(runCatching { dpm.isApplicationHidden(admin, pkg) }.getOrDefault(false))}\n")
        sb.append("Suspended: ${yesNo(runCatching { dpm.isPackageSuspended(admin, pkg) }.getOrDefault(false))}\n")
        sb.append("Uninstall blocked: ${yesNo(runCatching { dpm.isUninstallBlocked(admin, pkg) }.getOrDefault(false))}\n")
        return Result(out = sb.toString())
    }

    private fun clearData(a: List<String>): Result {
        need(a, 1, USAGE.getValue("clear-data"))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return Result(err = str(R.string.dish_err_unsupported) + "\n", code = 1)
        }
        val latch = CountDownLatch(1)
        var success = false
        dpm.clearApplicationUserData(admin, a[0], { it.run() }) { _, ok ->
            success = ok
            latch.countDown()
        }
        if (!latch.await(15, TimeUnit.SECONDS)) {
            return Result(err = str(R.string.dish_err_failed, "timeout") + "\n", code = 1)
        }
        return bool(success)
    }

    private fun permissionPolicy(a: List<String>): Result {
        val usage = USAGE.getValue("permission-policy")
        need(a, 1, usage)
        return when (a[0]) {
            "prompt" -> { dpm.setPermissionPolicy(admin, DevicePolicyManager.PERMISSION_POLICY_PROMPT); ok() }
            "grant" -> { dpm.setPermissionPolicy(admin, DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT); ok() }
            "deny" -> { dpm.setPermissionPolicy(admin, DevicePolicyManager.PERMISSION_POLICY_AUTO_DENY); ok() }
            "status" -> Result(
                out = when (dpm.getPermissionPolicy(admin)) {
                    DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT -> "grant"
                    DevicePolicyManager.PERMISSION_POLICY_AUTO_DENY -> "deny"
                    else -> "prompt"
                } + "\n"
            )
            else -> throw UsageException(usage)
        }
    }

    private fun autoTime(a: List<String>, zone: Boolean): Result {
        val usage = USAGE.getValue(if (zone) "auto-timezone" else "auto-time")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return Result(err = str(R.string.dish_err_unsupported) + "\n", code = 1)
        }
        return onOff(
            a, usage,
            { if (zone) dpm.setAutoTimeZoneEnabled(admin, it) else dpm.setAutoTimeEnabled(admin, it) },
            { if (zone) dpm.getAutoTimeZoneEnabled(admin) else dpm.getAutoTimeEnabled(admin) }
        )
    }

    private fun privateDns(a: List<String>): Result {
        val usage = USAGE.getValue("private-dns")
        need(a, 1, usage)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return Result(err = str(R.string.dish_err_unsupported) + "\n", code = 1)
        }
        if (a[0] == "status") {
            val mode = when (dpm.getGlobalPrivateDnsMode(admin)) {
                DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC -> "auto"
                DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME ->
                    "host " + (dpm.getGlobalPrivateDnsHost(admin) ?: "?")
                DevicePolicyManager.PRIVATE_DNS_MODE_OFF -> "off"
                else -> "unknown"
            }
            return Result(out = mode + "\n")
        }
        val code = if (a[0] == "auto") dpm.setGlobalPrivateDnsModeOpportunistic(admin)
        else dpm.setGlobalPrivateDnsModeSpecifiedHost(admin, a[0])
        return if (code == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR) ok()
        else Result(err = str(R.string.dish_err_failed, "private DNS error $code") + "\n", code = 1)
    }

    private fun ownerInfo(a: List<String>): Result {
        val usage = USAGE.getValue("owner-info")
        need(a, 1, usage)
        return when (a[0]) {
            "get" -> Result(out = (dpm.getDeviceOwnerLockScreenInfo()?.toString() ?: "") + "\n")
            "clear" -> { dpm.setDeviceOwnerLockScreenInfo(admin, null); ok() }
            "set" -> {
                need(a, 2, usage)
                dpm.setDeviceOwnerLockScreenInfo(admin, a.drop(1).joinToString(" "))
                ok()
            }
            else -> throw UsageException(usage)
        }
    }

    private fun orgName(a: List<String>): Result {
        val usage = USAGE.getValue("org-name")
        need(a, 1, usage)
        return when (a[0]) {
            "get" -> Result(out = (dpm.getOrganizationName(admin)?.toString() ?: "") + "\n")
            "clear" -> { dpm.setOrganizationName(admin, null); ok() }
            "set" -> {
                need(a, 2, usage)
                dpm.setOrganizationName(admin, a.drop(1).joinToString(" "))
                ok()
            }
            else -> throw UsageException(usage)
        }
    }

    private fun settingsList(space: String): Result {
        val uri = if (space == "global") Settings.Global.CONTENT_URI else Settings.Secure.CONTENT_URI
        val lines = ArrayList<String>()
        context.contentResolver.query(uri, arrayOf("name", "value"), null, null, null)?.use { c ->
            while (c.moveToNext()) lines.add("${c.getString(0)}=${c.getString(1) ?: "null"}")
        }
        lines.sort()
        return Result(out = lines.joinToString("\n") + if (lines.isEmpty()) "" else "\n")
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
        /** One line of usage per command, shown by `help COMMAND` and used for hints. */
        val USAGE: Map<String, String> = linkedMapOf(
            "help" to "help [COMMAND]",
            "ping" to "ping    (silent check that dish may run)",
            "version" to "version",
            "id" to "id    (uid and app name of this terminal)",
            "whoami" to "whoami",
            "status" to "status",
            "device" to "device    (model, Android version, security patch)",
            "lock" to "lock",
            "reboot" to "reboot",
            "hide" to "hide PKG",
            "unhide" to "unhide PKG",
            "is-hidden" to "is-hidden PKG",
            "suspend" to "suspend PKG...",
            "unsuspend" to "unsuspend PKG...",
            "is-suspended" to "is-suspended PKG",
            "block-uninstall" to "block-uninstall PKG",
            "allow-uninstall" to "allow-uninstall PKG",
            "is-uninstall-blocked" to "is-uninstall-blocked PKG",
            "list" to "list all|user|system|hidden|suspended|uninstall-blocked",
            "app-info" to "app-info PKG",
            "enable-system-app" to "enable-system-app PKG",
            "install-existing" to "install-existing PKG",
            "clear-data" to "clear-data PKG",
            "permission" to "permission grant|deny|default|get PKG PERMISSION",
            "permission-policy" to "permission-policy prompt|grant|deny|status",
            "restriction" to "restriction add|clear NAME | has NAME | list | available",
            "camera" to "camera disable|enable|status",
            "screen-capture" to "screen-capture disable|enable|status",
            "keyguard" to "keyguard disable|enable",
            "status-bar" to "status-bar disable|enable",
            "mute" to "mute on|off|status",
            "adb" to "adb enable|disable|status",
            "stay-awake" to "stay-awake on|off|status",
            "auto-time" to "auto-time on|off|status",
            "auto-timezone" to "auto-timezone on|off|status",
            "timezone" to "timezone ID",
            "private-dns" to "private-dns auto|HOSTNAME|status",
            "owner-info" to "owner-info get|clear|set TEXT...",
            "org-name" to "org-name get|clear|set TEXT...",
            "settings" to "settings get|put global|secure NAME [VALUE] | settings list global|secure",
            "call" to "call --list | call METHOD [ARGS...]"
        )

        /** Destructive or ownership-changing calls are never exposed through `dish call`. */
        private val DENIED_METHODS = setOf(
            "wipeData", "wipeDevice", "clearDeviceOwnerApp", "clearProfileOwner",
            "removeActiveAdmin", "transferOwnership", "resetPassword",
            "resetPasswordWithToken", "setResetPasswordToken"
        )
    }
}
