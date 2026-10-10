package com.EdS.DhizukuF.dish

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.server.DhizukuState
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Device Owner commands, executed inside the Dhizuku process (which is the Device Owner).
 *
 * Every answer says clearly what happened: "OK: ..." when the action was done, "NOT DONE: ..."
 * (exit code 1) when it was not, and "Usage: ..." (exit code 2) for a wrong command line.
 */
@SuppressLint("NewApi")
class DishCommands(private val context: Context, private val uid: Int = -1) {
    class Result(val out: String = "", val err: String = "", val code: Int = 0)

    private class UsageException(val usage: String) : Exception()

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin: ComponentName get() = DhizukuState.admin

    private fun str(id: Int, vararg args: Any) = context.getString(id, *args)

    // ------------------------------------------------------------------ answers

    private fun done(msg: String) = Result(out = "OK: $msg\n")

    private fun notDone(msg: String) = Result(err = str(R.string.dish_err_failed, msg) + "\n", code = 1)

    private fun outcome(success: Boolean, okMsg: String, failMsg: String) =
        if (success) done(okMsg) else notDone(failMsg)

    private fun unsupported() = Result(err = str(R.string.dish_err_unsupported) + "\n", code = 1)

    private fun yesNo(v: Boolean) = str(if (v) R.string.dish_yes else R.string.dish_no)

    private fun fact(label: String, v: Boolean) = Result(out = "$label: ${yesNo(v)}\n")

    private fun need(a: List<String>, n: Int, usage: String) {
        if (a.size < n) throw UsageException(usage)
    }

    private fun fail(t: Throwable): Result {
        val hint = if (t is SecurityException) " (the system did not allow this for Device Owner)" else ""
        return notDone("${t.javaClass.simpleName}: ${t.message ?: ""}$hint")
    }

    fun execute(args: List<String>): Result {
        if (args.isEmpty()) return help(emptyList())
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
        "status" -> status()
        "wipe" -> notDone(str(R.string.dish_err_method_denied, "wipe"))

        "lock" -> {
            dpm.lockNow()
            done("the screen is locked")
        }

        "reboot" -> {
            dpm.reboot(admin)
            done("the device is rebooting now")
        }

        "hide", "unhide" -> {
            need(a, 1, USAGE.getValue(cmd))
            val hide = cmd == "hide"
            outcome(
                dpm.setApplicationHidden(admin, a[0], hide),
                "${a[0]} is now ${if (hide) "hidden" else "visible"}",
                "could not ${if (hide) "hide" else "unhide"} ${a[0]} (package not found or protected)"
            )
        }

        "is-hidden" -> {
            need(a, 1, USAGE.getValue(cmd))
            fact("${a[0]} hidden", dpm.isApplicationHidden(admin, a[0]))
        }

        "suspend", "unsuspend" -> setSuspended(a, cmd == "suspend")

        "is-suspended" -> {
            need(a, 1, USAGE.getValue(cmd))
            fact("${a[0]} suspended", dpm.isPackageSuspended(admin, a[0]))
        }

        "block-uninstall", "allow-uninstall" -> {
            need(a, 1, USAGE.getValue(cmd))
            val block = cmd == "block-uninstall"
            dpm.setUninstallBlocked(admin, a[0], block)
            outcome(
                dpm.isUninstallBlocked(admin, a[0]) == block,
                "uninstalling ${a[0]} is now ${if (block) "blocked" else "allowed"}",
                "the system did not change the uninstall state of ${a[0]}"
            )
        }

        "is-uninstall-blocked" -> {
            need(a, 1, USAGE.getValue(cmd))
            fact("${a[0]} uninstall blocked", dpm.isUninstallBlocked(admin, a[0]))
        }

        "list" -> list(a)
        "app-info" -> appInfo(a)

        "enable-system-app" -> {
            need(a, 1, USAGE.getValue(cmd))
            dpm.enableSystemApp(admin, a[0])
            done("system app ${a[0]} is enabled")
        }

        "install-existing" -> {
            need(a, 1, USAGE.getValue(cmd))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) unsupported()
            else outcome(
                dpm.installExistingPackage(admin, a[0]),
                "${a[0]} is installed for this user",
                "could not install ${a[0]} (it is not present on the device)"
            )
        }

        "clear-data" -> clearData(a)
        "permission" -> permission(a)
        "permission-policy" -> permissionPolicy(a)
        "restriction" -> restriction(a)

        "camera" -> toggle(
            a, "camera", USAGE.getValue(cmd),
            { dpm.setCameraDisabled(admin, it) }, { dpm.getCameraDisabled(admin) }
        )

        "screen-capture" -> toggle(
            a, "screen capture", USAGE.getValue(cmd),
            { dpm.setScreenCaptureDisabled(admin, it) }, { dpm.getScreenCaptureDisabled(admin) }
        )

        "keyguard" -> toggle(
            a, "keyguard", USAGE.getValue(cmd),
            { dpm.setKeyguardDisabled(admin, it) }, null
        )

        "status-bar" -> toggle(
            a, "status bar", USAGE.getValue(cmd),
            { dpm.setStatusBarDisabled(admin, it) }, null
        )

        "mute" -> onOff(
            a, "mute", USAGE.getValue(cmd),
            { dpm.setMasterVolumeMuted(admin, it) }, { dpm.isMasterVolumeMuted(admin) }
        )

        "adb" -> globalFlag(
            a, "adb", USAGE.getValue(cmd), Settings.Global.ADB_ENABLED, "1", "0",
            enableWord = "enable", disableWord = "disable", onLabel = "enabled", offLabel = "disabled"
        )

        "stay-awake" -> globalFlag(
            a, "stay-awake", USAGE.getValue(cmd), Settings.Global.STAY_ON_WHILE_PLUGGED_IN, "7", "0",
            enableWord = "on", disableWord = "off", onLabel = "on", offLabel = "off"
        )

        "auto-time" -> autoTime(a, zone = false)
        "auto-timezone" -> autoTime(a, zone = true)

        "timezone" -> {
            need(a, 1, USAGE.getValue(cmd))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) unsupported()
            else outcome(
                dpm.setTimeZone(admin, a[0]),
                "time zone is set to ${a[0]}",
                "the time zone ${a[0]} was refused (use an ID like Europe/Kyiv)"
            )
        }

        "private-dns" -> privateDns(a)
        "owner-info" -> ownerInfo(a)
        "org-name" -> orgName(a)
        "settings" -> settings(a)
        "api", "call" -> api(a)

        else -> {
            val hints = USAGE.keys.filter { it.startsWith(cmd.take(2)) }.take(5)
            val extra = if (hints.isEmpty()) "" else "Did you mean: " + hints.joinToString(", ") + "?\n"
            Result(err = str(R.string.dish_err_unknown_command, raw) + "\n" + extra, code = 127)
        }
    }

    // ------------------------------------------------------------------ help and info

    /** `help` = names only, grouped. `help COMMAND` = description, usage, example. */
    private fun help(a: List<String>): Result {
        if (a.isEmpty()) {
            val sb = StringBuilder()
            sb.append("dish - Device Owner console\n")
            sb.append("help COMMAND shows what a command does. api = direct calls.\n\n")
            for ((group, items) in ENTRIES.groupBy { it.group }) {
                sb.append(group).append(":\n  ").append(items.joinToString(" ") { it.name }).append("\n")
            }
            return Result(out = sb.toString())
        }
        val key = when (val k = a[0].lowercase(Locale.ROOT)) {
            "whoami" -> "id"
            "call" -> "api"
            else -> k
        }
        val e = ENTRIES.firstOrNull { it.name == key }
            ?: return Result(err = str(R.string.dish_err_unknown_command, a[0]) + "\n", code = 127)
        val sb = StringBuilder()
        sb.append(e.name).append(" - ").append(e.desc).append("\n")
        sb.append("Usage: ").append(e.usage).append("\n")
        if (e.example != null) sb.append("Example: ").append(e.example).append("\n")
        return Result(out = sb.toString())
    }

    @Suppress("DEPRECATION")
    private fun appVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (_: Throwable) {
        "?"
    }

    private fun version() = Result(out = "DhizukuF ${appVersion()}\ndish protocol 1\n")

    private fun status(): Result {
        val adminName = runCatching { admin.flattenToShortString() }.getOrDefault("?")
        return Result(
            out = "DhizukuF: ${context.packageName} ${appVersion()}\n" +
                "Device owner: ${yesNo(dpm.isDeviceOwnerApp(context.packageName))}\n" +
                "Profile owner: ${yesNo(dpm.isProfileOwnerApp(context.packageName))}\n" +
                "Admin component: $adminName\n" +
                "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n"
        )
    }

    private fun whoami(): Result {
        val pkgs = context.packageManager.getPackagesForUid(uid)?.joinToString(", ") ?: "?"
        val name = DishRegistry.displayName(uid, pkgs)
        return Result(out = "uid: $uid\napp: $name\npackages: $pkgs\n")
    }

    private fun device() = Result(
        out = "Model: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
            "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
            "Security patch: ${Build.VERSION.SECURITY_PATCH}\n" +
            "Build: ${Build.DISPLAY}\n" +
            "ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}\n"
    )

    // ------------------------------------------------------------------ apps

    private fun setSuspended(a: List<String>, flag: Boolean): Result {
        need(a, 1, USAGE.getValue(if (flag) "suspend" else "unsuspend"))
        val failed = dpm.setPackagesSuspended(admin, a.toTypedArray(), flag).toList()
        val good = a.filter { it !in failed }
        val word = if (flag) "suspended" else "unsuspended"
        val out = if (good.isEmpty()) "" else "OK: $word ${good.joinToString(", ")}\n"
        val err = if (failed.isEmpty()) ""
        else str(R.string.dish_err_failed, "could not change ${failed.joinToString(", ")} (not installed or protected)") + "\n"
        return Result(out, err, if (failed.isEmpty()) 0 else 1)
    }

    @Suppress("DEPRECATION")
    private fun list(a: List<String>): Result {
        val usage = USAGE.getValue("list")
        need(a, 1, usage)
        val kind = a[0]
        if (kind !in setOf("all", "hidden", "suspended", "uninstall-blocked", "system", "user")) {
            throw UsageException(usage)
        }
        val apps = context.packageManager.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)
        val names = apps.filter {
            val system = it.flags and ApplicationInfo.FLAG_SYSTEM != 0
            when (kind) {
                "all" -> true
                "system" -> system
                "user" -> !system
                "hidden" -> runCatching { dpm.isApplicationHidden(admin, it.packageName) }.getOrDefault(false)
                "suspended" -> runCatching { dpm.isPackageSuspended(admin, it.packageName) }.getOrDefault(false)
                else -> runCatching { dpm.isUninstallBlocked(admin, it.packageName) }.getOrDefault(false)
            }
        }.map { it.packageName }.sorted()
        if (names.isEmpty()) return Result(out = "No apps match \"$kind\".\n")
        return Result(out = names.joinToString("\n") + "\n${names.size} app(s)\n")
    }

    @Suppress("DEPRECATION")
    private fun appInfo(a: List<String>): Result {
        need(a, 1, USAGE.getValue("app-info"))
        val pkg = a[0]
        val info = try {
            context.packageManager.getPackageInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        } catch (_: PackageManager.NameNotFoundException) {
            return notDone("no such package: $pkg")
        }
        val system = (info.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
        fun q(f: () -> Boolean) = runCatching { f() }.getOrDefault(false)
        return Result(
            out = "Package: $pkg\n" +
                "Version: ${info.versionName ?: "?"}\n" +
                "System app: ${yesNo(system)}\n" +
                "Hidden: ${yesNo(q { dpm.isApplicationHidden(admin, pkg) })}\n" +
                "Suspended: ${yesNo(q { dpm.isPackageSuspended(admin, pkg) })}\n" +
                "Uninstall blocked: ${yesNo(q { dpm.isUninstallBlocked(admin, pkg) })}\n"
        )
    }

    private fun clearData(a: List<String>): Result {
        need(a, 1, USAGE.getValue("clear-data"))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return unsupported()
        val latch = CountDownLatch(1)
        var success = false
        dpm.clearApplicationUserData(admin, a[0], { it.run() }) { _, ok ->
            success = ok
            latch.countDown()
        }
        if (!latch.await(15, TimeUnit.SECONDS)) return notDone("clearing the data of ${a[0]} timed out")
        return outcome(
            success,
            "data of ${a[0]} is cleared",
            "could not clear the data of ${a[0]} (unknown package or protected app)"
        )
    }

    private fun permission(a: List<String>): Result {
        val usage = USAGE.getValue("permission")
        need(a, 3, usage)
        val (action, pkg, perm) = a
        val state = when (action) {
            "grant" -> DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
            "deny" -> DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED
            "default" -> DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT
            "get" -> {
                val name = when (dpm.getPermissionGrantState(admin, pkg, perm)) {
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED -> "granted"
                    DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED -> "denied"
                    else -> "default"
                }
                return Result(out = "$pkg $perm: $name\n")
            }
            else -> throw UsageException(usage)
        }
        return outcome(
            dpm.setPermissionGrantState(admin, pkg, perm, state),
            "permission $perm of $pkg is set to $action",
            "could not set $perm for $pkg (unknown package, or the app does not use this permission)"
        )
    }

    private fun permissionPolicy(a: List<String>): Result {
        val usage = USAGE.getValue("permission-policy")
        need(a, 1, usage)
        return when (a[0]) {
            "prompt" -> {
                dpm.setPermissionPolicy(admin, DevicePolicyManager.PERMISSION_POLICY_PROMPT)
                done("apps will ask for permissions")
            }

            "grant" -> {
                dpm.setPermissionPolicy(admin, DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT)
                done("permissions are granted automatically")
            }

            "deny" -> {
                dpm.setPermissionPolicy(admin, DevicePolicyManager.PERMISSION_POLICY_AUTO_DENY)
                done("permissions are denied automatically")
            }

            "status" -> Result(
                out = "permission policy: " + when (dpm.getPermissionPolicy(admin)) {
                    DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT -> "grant"
                    DevicePolicyManager.PERMISSION_POLICY_AUTO_DENY -> "deny"
                    else -> "prompt"
                } + "\n"
            )

            else -> throw UsageException(usage)
        }
    }

    // ------------------------------------------------------------------ restrictions and device

    private fun restrictionNames(): List<String> = UserManager::class.java.fields
        .filter { it.name.startsWith("DISALLOW_") && it.type == String::class.java }
        .mapNotNull { runCatching { it.get(null) as? String }.getOrNull() }
        .sorted()

    private fun restriction(a: List<String>): Result {
        val usage = USAGE.getValue("restriction")
        need(a, 1, usage)
        return when (a[0]) {
            "available" -> Result(out = restrictionNames().joinToString("\n") + "\n")

            "has" -> {
                need(a, 2, usage)
                val on = dpm.getUserRestrictions(admin).getBoolean(a[1])
                Result(out = "restriction ${a[1]}: ${if (on) "active" else "not active"}\n")
            }

            "list" -> {
                val bundle = dpm.getUserRestrictions(admin)
                val keys = bundle.keySet().filter { bundle.getBoolean(it) }.sorted()
                if (keys.isEmpty()) Result(out = "No restrictions are active.\n")
                else Result(out = keys.joinToString("\n") + "\n${keys.size} active\n")
            }

            "add", "clear" -> {
                need(a, 2, usage)
                val name = a[1]
                if (name !in restrictionNames() && !name.startsWith("no_")) {
                    return notDone("unknown restriction \"$name\". Run: restriction available")
                }
                val add = a[0] == "add"
                if (add) dpm.addUserRestriction(admin, name) else dpm.clearUserRestriction(admin, name)
                val on = dpm.getUserRestrictions(admin).getBoolean(name)
                outcome(
                    on == add,
                    "restriction $name is now ${if (add) "active" else "cleared"}",
                    "the system did not ${if (add) "apply" else "clear"} the restriction $name"
                )
            }

            else -> throw UsageException(usage)
        }
    }

    private fun toggle(
        a: List<String>,
        label: String,
        usage: String,
        set: (Boolean) -> Any?,
        get: (() -> Boolean)?
    ): Result {
        need(a, 1, usage)
        return when (a[0]) {
            "disable", "enable" -> {
                val disable = a[0] == "disable"
                val r = set(disable)
                outcome(
                    r != false,
                    "$label is ${if (disable) "disabled" else "enabled"}",
                    "the system refused to ${a[0]} $label"
                )
            }

            "status" -> {
                if (get == null) throw UsageException(usage)
                Result(out = "$label: " + str(if (get()) R.string.dish_disabled else R.string.dish_enabled) + "\n")
            }

            else -> throw UsageException(usage)
        }
    }

    /** on|off|status for a feature where "on" means the feature is active. */
    private fun onOff(
        a: List<String>,
        label: String,
        usage: String,
        set: (Boolean) -> Any?,
        get: () -> Boolean
    ): Result {
        need(a, 1, usage)
        return when (a[0]) {
            "on", "off" -> {
                val on = a[0] == "on"
                set(on)
                outcome(
                    get() == on,
                    "$label is ${if (on) "on" else "off"}",
                    "the system did not turn $label ${a[0]}"
                )
            }

            "status" -> Result(out = "$label: ${if (get()) "on" else "off"}\n")
            else -> throw UsageException(usage)
        }
    }

    private fun globalFlag(
        a: List<String>,
        label: String,
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
            enableWord, disableWord -> {
                val enable = a[0] == enableWord
                dpm.setGlobalSetting(admin, name, if (enable) on else off)
                val now = Settings.Global.getString(context.contentResolver, name)
                outcome(
                    now == (if (enable) on else off),
                    "$label is ${if (enable) onLabel else offLabel}",
                    "$label was not changed (current value: ${now ?: "null"})"
                )
            }

            "status" -> {
                val v = Settings.Global.getString(context.contentResolver, name)
                Result(out = "$label: ${if (v != null && v != "0") onLabel else offLabel}\n")
            }

            else -> throw UsageException(usage)
        }
    }

    private fun autoTime(a: List<String>, zone: Boolean): Result {
        val label = if (zone) "auto-timezone" else "auto-time"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return unsupported()
        return onOff(
            a, label, USAGE.getValue(label),
            { if (zone) dpm.setAutoTimeZoneEnabled(admin, it) else dpm.setAutoTimeEnabled(admin, it) },
            { if (zone) dpm.getAutoTimeZoneEnabled(admin) else dpm.getAutoTimeEnabled(admin) }
        )
    }

    private fun privateDns(a: List<String>): Result {
        val usage = USAGE.getValue("private-dns")
        need(a, 1, usage)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return unsupported()
        if (a[0] == "status") {
            val mode = when (dpm.getGlobalPrivateDnsMode(admin)) {
                DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC -> "auto"
                DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME ->
                    "host " + (dpm.getGlobalPrivateDnsHost(admin) ?: "?")
                DevicePolicyManager.PRIVATE_DNS_MODE_OFF -> "off"
                else -> "unknown"
            }
            return Result(out = "private DNS: $mode\n")
        }
        val code = if (a[0] == "auto") dpm.setGlobalPrivateDnsModeOpportunistic(admin)
        else dpm.setGlobalPrivateDnsModeSpecifiedHost(admin, a[0])
        return outcome(
            code == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR,
            if (a[0] == "auto") "private DNS is automatic" else "private DNS host is ${a[0]}",
            "private DNS was not changed (error code $code)"
        )
    }

    private fun ownerInfo(a: List<String>): Result {
        val usage = USAGE.getValue("owner-info")
        need(a, 1, usage)
        return when (a[0]) {
            "get" -> Result(out = "owner-info: " + (dpm.getDeviceOwnerLockScreenInfo()?.toString() ?: "") + "\n")
            "clear" -> {
                dpm.setDeviceOwnerLockScreenInfo(admin, null)
                done("lock screen text is removed")
            }

            "set" -> {
                need(a, 2, usage)
                dpm.setDeviceOwnerLockScreenInfo(admin, a.drop(1).joinToString(" "))
                done("lock screen text is set")
            }

            else -> throw UsageException(usage)
        }
    }

    private fun orgName(a: List<String>): Result {
        val usage = USAGE.getValue("org-name")
        need(a, 1, usage)
        return when (a[0]) {
            "get" -> Result(out = "org-name: " + (dpm.getOrganizationName(admin)?.toString() ?: "") + "\n")
            "clear" -> {
                dpm.setOrganizationName(admin, null)
                done("organization name is removed")
            }

            "set" -> {
                need(a, 2, usage)
                dpm.setOrganizationName(admin, a.drop(1).joinToString(" "))
                done("organization name is set")
            }

            else -> throw UsageException(usage)
        }
    }

    // ------------------------------------------------------------------ settings

    private fun readSetting(space: String, name: String): String? =
        if (space == "global") Settings.Global.getString(context.contentResolver, name)
        else Settings.Secure.getString(context.contentResolver, name)

    private fun settings(a: List<String>): Result {
        val usage = USAGE.getValue("settings")
        if (a.size == 2 && a[0] == "list" && (a[1] == "global" || a[1] == "secure")) {
            return settingsList(a[1])
        }
        need(a, 3, usage)
        val (action, space, name) = a
        if (space != "global" && space != "secure") throw UsageException(usage)
        return when (action) {
            "get" -> Result(out = (readSetting(space, name) ?: "null") + "\n")

            "put" -> {
                need(a, 4, usage)
                if (space == "global") dpm.setGlobalSetting(admin, name, a[3])
                else dpm.setSecureSetting(admin, name, a[3])
                val now = readSetting(space, name)
                outcome(
                    now == a[3],
                    "$space $name = ${a[3]}",
                    "$space $name was not changed (current value: ${now ?: "null"})"
                )
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
        if (lines.isEmpty()) return Result(out = "No $space settings could be read.\n")
        return Result(out = lines.joinToString("\n") + "\n${lines.size} setting(s)\n")
    }

    // ------------------------------------------------------------------ direct API

    private fun target(name: String): Pair<Class<*>, Any?>? {
        val (className, service) = when (name) {
            "dpm" -> "android.app.admin.DevicePolicyManager" to Context.DEVICE_POLICY_SERVICE
            "pm" -> "android.content.pm.PackageManager" to null
            "um" -> "android.os.UserManager" to Context.USER_SERVICE
            "am" -> "android.app.ActivityManager" to Context.ACTIVITY_SERVICE
            "audio" -> "android.media.AudioManager" to Context.AUDIO_SERVICE
            "power" -> "android.os.PowerManager" to Context.POWER_SERVICE
            "wifi" -> "android.net.wifi.WifiManager" to Context.WIFI_SERVICE
            "conn" -> "android.net.ConnectivityManager" to Context.CONNECTIVITY_SERVICE
            "notif" -> "android.app.NotificationManager" to Context.NOTIFICATION_SERVICE
            "tele" -> "android.telephony.TelephonyManager" to Context.TELEPHONY_SERVICE
            else -> return null
        }
        val obj = if (service == null) context.packageManager else context.getSystemService(service)
        return Class.forName(className) to obj
    }

    private fun isDenied(name: String): Boolean {
        val l = name.lowercase(Locale.ROOT)
        return name in DENIED_METHODS || l.contains("wipe") || l.contains("factoryreset")
    }

    private fun signature(m: Method, hideAdmin: Boolean): String {
        var params = m.parameterTypes.toList()
        if (hideAdmin && params.firstOrNull() == ComponentName::class.java) params = params.drop(1)
        return "${m.returnType.simpleName} ${m.name}(${params.joinToString(", ") { it.simpleName }})"
    }

    /**
     * Direct API: call any public method of DevicePolicyManager (or another system manager) by
     * name. For dpm the admin argument is added automatically. Typed arguments: s: i: l: f: d: b:
     * cn: strs: ints: null @admin. Untyped arguments are converted to the parameter type.
     */
    private fun api(a: List<String>): Result {
        val usage = USAGE.getValue("api")
        var on = "dpm"
        var noAdmin = false
        var list = false
        var sig = false
        var i = 0
        while (i < a.size && a[i].startsWith("--")) {
            when (a[i]) {
                "--list" -> list = true
                "--sig" -> sig = true
                "--no-admin" -> noAdmin = true
                "--on" -> {
                    i++
                    if (i >= a.size) throw UsageException(usage)
                    on = a[i].lowercase(Locale.ROOT)
                }

                else -> throw UsageException(usage)
            }
            i++
        }
        val rest = a.drop(i)
        val (clazz, obj) = target(on)
            ?: return notDone("unknown target \"$on\". Targets: dpm pm um am audio power wifi conn notif tele")
        val hideAdmin = on == "dpm"

        if (list || sig) {
            val filter = rest.firstOrNull()?.lowercase(Locale.ROOT)
            val lines = clazz.methods
                .filter { !isDenied(it.name) }
                .filter {
                    filter == null || if (sig) it.name.lowercase(Locale.ROOT) == filter
                    else it.name.lowercase(Locale.ROOT).contains(filter)
                }
                .map { signature(it, hideAdmin) }.distinct().sorted()
            if (lines.isEmpty()) return notDone("no method matches \"${filter ?: ""}\"")
            return Result(out = lines.joinToString("\n") + "\n${lines.size} method(s)\n")
        }

        need(rest, 1, usage)
        val name = rest[0]
        if (isDenied(name)) return notDone(str(R.string.dish_err_method_denied, name))
        val tokens = rest.drop(1)
        val hasAdminToken = tokens.any { it == "@admin" }
        val candidates = clazz.methods.filter { it.name == name }.sortedBy { it.parameterTypes.size }
        if (candidates.isEmpty()) return notDone(str(R.string.dish_err_no_method, name) + " (try: api --list $name)")

        for (m in candidates) {
            val types = m.parameterTypes
            val auto = hideAdmin && !noAdmin && !hasAdminToken &&
                types.isNotEmpty() && types[0] == ComponentName::class.java
            val skip = if (auto) 1 else 0
            if (tokens.size != types.size - skip) continue
            val args = ArrayList<Any?>()
            if (auto) args.add(admin)
            var matched = true
            for ((k, t) in tokens.withIndex()) {
                val v = parseArg(t, types[k + skip])
                if (v === NO_MATCH) {
                    matched = false
                    break
                }
                args.add(v)
            }
            if (!matched) continue
            val result = m.invoke(obj, *args.toTypedArray())
            if (m.returnType == Void.TYPE) return done("$on.$name called (no return value)")
            return Result(out = "OK: $on.$name returned:\n${format(result)}\n")
        }
        return notDone(str(R.string.dish_err_bad_args, name) + " (try: api --sig $name)")
    }

    private fun boxed(p: Class<*>): Class<*> = when (p) {
        Int::class.javaPrimitiveType -> Int::class.javaObjectType
        Long::class.javaPrimitiveType -> Long::class.javaObjectType
        Boolean::class.javaPrimitiveType -> Boolean::class.javaObjectType
        Float::class.javaPrimitiveType -> Float::class.javaObjectType
        Double::class.javaPrimitiveType -> Double::class.javaObjectType
        else -> p
    }

    private fun coerce(p: Class<*>, v: Any): Any? {
        val b = boxed(p)
        return when {
            b == Long::class.javaObjectType && v is Int -> v.toLong()
            b == Double::class.javaObjectType && v is Number -> v.toDouble()
            b == Float::class.javaObjectType && (v is Int || v is Long) -> (v as Number).toFloat()
            b.isInstance(v) -> v
            else -> null
        }
    }

    private fun intList(v: String): IntArray? {
        val parts = v.split(',').filter { it.isNotEmpty() }
        val out = IntArray(parts.size)
        for ((k, s) in parts.withIndex()) out[k] = s.toIntOrNull() ?: return null
        return out
    }

    private fun parseArg(t: String, p: Class<*>): Any? {
        if (t == "null") return if (p.isPrimitive) NO_MATCH else null
        if (t == "@admin") return if (p == ComponentName::class.java) admin else NO_MATCH
        val m = TYPED.matchEntire(t)
        if (m != null) {
            val v = m.groupValues[2]
            val typed: Any = when (m.groupValues[1]) {
                "s", "str" -> v
                "i", "int" -> v.toIntOrNull()
                "l", "long" -> v.toLongOrNull()
                "f", "float" -> v.toFloatOrNull()
                "d", "double" -> v.toDoubleOrNull()
                "b", "bool" -> parseBool(v)
                "cn" -> ComponentName.unflattenFromString(v)
                "strs" -> v.split(',').filter { it.isNotEmpty() }.toTypedArray()
                "ints" -> intList(v)
                else -> null
            } ?: return NO_MATCH
            return coerce(p, typed) ?: NO_MATCH
        }
        return when (p) {
            String::class.java, CharSequence::class.java -> t
            Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> parseBool(t)
            Int::class.javaPrimitiveType, Int::class.javaObjectType -> t.toIntOrNull()
            Long::class.javaPrimitiveType, Long::class.javaObjectType -> t.toLongOrNull()
            Float::class.javaPrimitiveType, Float::class.javaObjectType -> t.toFloatOrNull()
            Double::class.javaPrimitiveType, Double::class.javaObjectType -> t.toDoubleOrNull()
            ComponentName::class.java -> ComponentName.unflattenFromString(t)
            Array<String>::class.java -> t.split(',').filter { it.isNotEmpty() }.toTypedArray()
            IntArray::class.java -> intList(t)
            UserHandle::class.java -> t.toIntOrNull()?.let {
                runCatching {
                    UserHandle::class.java.getMethod("of", Int::class.javaPrimitiveType).invoke(null, it)
                }.getOrNull()
            }
            else -> null
        } ?: NO_MATCH
    }

    private fun parseBool(v: String): Boolean? = when (v.lowercase(Locale.ROOT)) {
        "true", "on", "yes", "1" -> true
        "false", "off", "no", "0" -> false
        else -> null
    }

    @Suppress("DEPRECATION")
    private fun format(r: Any?): String = when (r) {
        null -> "null"
        is Array<*> -> r.joinToString("\n")
        is IntArray -> r.joinToString(", ")
        is LongArray -> r.joinToString(", ")
        is Collection<*> -> r.joinToString("\n")
        is Bundle -> r.keySet().joinToString("\n") { "$it=${r.get(it)}" }
        else -> r.toString()
    }

    private class Entry(
        val name: String,
        val group: String,
        val usage: String,
        val desc: String,
        val example: String? = null
    )

    companion object {
        private val NO_MATCH = Any()

        private val TYPED = Regex(
            "^(s|str|i|int|l|long|f|float|d|double|b|bool|cn|strs|ints):(.*)$",
            RegexOption.DOT_MATCHES_ALL
        )

        private const val GEN = "General"
        private const val APPS = "Apps"
        private const val RESTR = "Restrictions"
        private const val DEV = "Device"
        private const val SETT = "Settings"
        private const val API = "API"

        private val ENTRIES: List<Entry> = listOf(
            Entry("help", GEN, "help [COMMAND]", "List the commands, or explain one command.", "help hide"),
            Entry("status", GEN, "status", "Show whether DhizukuF is the Device Owner and the admin component."),
            Entry("version", GEN, "version", "Show the DhizukuF and dish versions."),
            Entry("id", GEN, "id", "Show the uid and app name of this terminal as DhizukuF sees it."),
            Entry("device", GEN, "device", "Show model, Android version, security patch and ABI."),
            Entry("ping", GEN, "ping", "Silent access check. Prints nothing and returns 0 when dish may run."),
            Entry("lock", GEN, "lock", "Lock the screen now."),
            Entry("reboot", GEN, "reboot", "Reboot the device immediately."),

            Entry("list", APPS, "list all|user|system|hidden|suspended|uninstall-blocked",
                "List package names of the chosen kind.", "list hidden"),
            Entry("app-info", APPS, "app-info PKG", "Show version and the hidden, suspended and uninstall state of an app.",
                "app-info com.android.chrome"),
            Entry("hide", APPS, "hide PKG", "Hide an app: it leaves the launcher and cannot run.", "hide com.example.app"),
            Entry("unhide", APPS, "unhide PKG", "Show a hidden app again.", "unhide com.example.app"),
            Entry("is-hidden", APPS, "is-hidden PKG", "Tell whether an app is hidden."),
            Entry("suspend", APPS, "suspend PKG...", "Suspend apps: they cannot be opened and show a dialog.",
                "suspend com.a com.b"),
            Entry("unsuspend", APPS, "unsuspend PKG...", "Unsuspend apps."),
            Entry("is-suspended", APPS, "is-suspended PKG", "Tell whether an app is suspended."),
            Entry("block-uninstall", APPS, "block-uninstall PKG", "Forbid uninstalling an app."),
            Entry("allow-uninstall", APPS, "allow-uninstall PKG", "Allow uninstalling an app again."),
            Entry("is-uninstall-blocked", APPS, "is-uninstall-blocked PKG", "Tell whether uninstalling an app is blocked."),
            Entry("enable-system-app", APPS, "enable-system-app PKG", "Enable a disabled system app."),
            Entry("install-existing", APPS, "install-existing PKG",
                "Bring back an app that is on the device but removed for this user."),
            Entry("clear-data", APPS, "clear-data PKG", "Delete all data of an app (like Clear storage)."),
            Entry("permission", APPS, "permission grant|deny|default|get PKG PERMISSION",
                "Set or read a runtime permission of an app without asking the user.",
                "permission grant com.example.app android.permission.CAMERA"),
            Entry("permission-policy", APPS, "permission-policy prompt|grant|deny|status",
                "Choose how apps get runtime permissions by default."),

            Entry("restriction", RESTR, "restriction add|clear NAME | has NAME | list | available",
                "Turn a user restriction on or off, check it, list active ones or all names.",
                "restriction add no_install_apps"),
            Entry("camera", RESTR, "camera disable|enable|status", "Disable the camera for all apps."),
            Entry("screen-capture", RESTR, "screen-capture disable|enable|status", "Block screenshots and screen recording."),
            Entry("keyguard", RESTR, "keyguard disable|enable", "Turn the lock screen off or on (only when no screen lock is set)."),
            Entry("status-bar", RESTR, "status-bar disable|enable", "Hide or show the status bar and notification shade."),
            Entry("mute", RESTR, "mute on|off|status", "Mute or unmute the master volume."),

            Entry("adb", DEV, "adb enable|disable|status", "Turn USB debugging on or off."),
            Entry("stay-awake", DEV, "stay-awake on|off|status", "Keep the screen on while charging."),
            Entry("auto-time", DEV, "auto-time on|off|status", "Automatic date and time (Android 11+)."),
            Entry("auto-timezone", DEV, "auto-timezone on|off|status", "Automatic time zone (Android 11+)."),
            Entry("timezone", DEV, "timezone ID", "Set the time zone.", "timezone Europe/Kyiv"),
            Entry("private-dns", DEV, "private-dns auto|HOSTNAME|status", "Set Private DNS (Android 10+).",
                "private-dns dns.google"),
            Entry("owner-info", DEV, "owner-info get|clear|set TEXT...", "Text shown on the lock screen.",
                "owner-info set Call me: 123"),
            Entry("org-name", DEV, "org-name get|clear|set TEXT...", "Organization name shown in settings.",
                "org-name set My company"),

            Entry("settings", SETT, "settings get|put global|secure NAME [VALUE] | settings list global|secure",
                "Read or write a setting. Writes are checked by reading the value back.",
                "settings put global adb_enabled 1"),

            Entry("api", API, "api [--on dpm|pm|um|am|audio|power|wifi|conn|notif|tele] [--no-admin] METHOD [ARG...] | api --list [TEXT] | api --sig METHOD",
                "Direct call of any public method by name. On dpm the admin argument is added for you. " +
                    "Args: plain text is converted to the parameter type; typed forms s: i: l: f: d: b: " +
                    "cn:pkg/class strs:a,b ints:1,2 null @admin. Wipe and ownership changes are blocked.",
                "api setLockTaskPackages strs:com.a,com.b")
        )

        /** command name -> usage, used for errors and hints. */
        val USAGE: Map<String, String> = ENTRIES.associate { it.name to it.usage }

        /** Destructive or ownership-changing calls are never exposed through `dish api`. */
        private val DENIED_METHODS = setOf(
            "wipeData", "wipeDevice", "clearDeviceOwnerApp", "clearProfileOwner",
            "removeActiveAdmin", "transferOwnership", "resetPassword",
            "resetPasswordWithToken", "setResetPasswordToken"
        )
    }
}
