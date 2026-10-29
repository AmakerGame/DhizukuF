package com.EdS.DhizukuF.dish

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.location.LocationManager
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
class DishCommands(
    private val context: Context,
    private val uid: Int = -1,
    private val dangerousEnabled: Boolean = false
) {
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

    /** Commands that would lock DhizukuF itself out must not touch its own package. */
    private fun selfGuard(action: String, vararg pkgs: String): Result? =
        if (pkgs.any { it == context.packageName }) {
            notDone("refusing to $action DhizukuF itself (it would break the Device Owner app)")
        } else null

    private fun isDeviceOwner() = dpm.isDeviceOwnerApp(context.packageName)

    private fun isProfileOwner() = dpm.isProfileOwnerApp(context.packageName)

    private fun modeName() = when {
        isDeviceOwner() -> "Device Owner"
        isProfileOwner() -> "Profile Owner"
        else -> "not an owner"
    }

    /** Answer for a command that works only when DhizukuF is the Device Owner. */
    private fun needsDeviceOwner(what: String) = notDone(
        "$what needs Device Owner, but DhizukuF is ${modeName()}. " +
            "Make DhizukuF the Device Owner to use it."
    )

    private fun fail(t: Throwable): Result {
        val hint = when {
            t !is SecurityException -> ""
            isProfileOwner() && !isDeviceOwner() ->
                " (DhizukuF is Profile Owner; this call probably needs Device Owner)"
            else -> " (the system did not allow this for Device Owner)"
        }
        return notDone("${t.javaClass.simpleName}: ${t.message ?: ""}$hint")
    }

    fun execute(args: List<String>): Result {
        if (args.isEmpty()) return help(emptyList())
        return runSafely(args[0].lowercase(Locale.ROOT), args.drop(1), args[0])
    }

    private fun runSafely(cmd: String, a: List<String>, raw: String): Result = try {
        dispatch(cmd, a, raw)
    } catch (e: UsageException) {
        Result(err = str(R.string.dish_err_usage, e.usage) + "\n", code = 2)
    } catch (e: InvocationTargetException) {
        fail(e.targetException ?: e)
    } catch (e: Throwable) {
        fail(e)
    }

    private fun dispatch(cmd: String, a: List<String>, raw: String): Result {
        // Commands that change device-wide state work only for the Device Owner. Reading a value
        // (status / get) is still tried in Profile Owner mode.
        val readOnly = a.firstOrNull() in setOf("status", "get")
        if (cmd in DEVICE_OWNER_ONLY && !readOnly && !isDeviceOwner()) return needsDeviceOwner(cmd)
        if (!dangerousEnabled && isDangerous(cmd, a)) {
            return notDone(
                "'${"$cmd ${a.firstOrNull() ?: ""}".trim()}' is a dangerous command and is switched off. " +
                    "Turn on Enable dangerous commands in the DhizukuF settings to use it."
            )
        }
        return runCommand(cmd, a, raw)
    }

    /** Only a leading --list / --sig option makes `api` harmless; the words later on are just arguments. */
    private fun isApiReadOnly(a: List<String>): Boolean {
        var i = 0
        var readOnly = false
        while (i < a.size && a[i].startsWith("--")) {
            if (a[i] == "--list" || a[i] == "--sig") readOnly = true
            if (a[i] == "--on") i++
            i++
        }
        return readOnly
    }

    /** Risky forms: irreversible, security-lowering, or free-form. Off unless the user enables them. */
    private fun isDangerous(cmd: String, a: List<String>): Boolean {
        val first = a.firstOrNull()
        return when (cmd) {
            "reboot", "clear-data", "wipe" -> true
            "api", "call" -> !isApiReadOnly(a)
            "settings" -> first == "put"
            "permission" -> first == "grant" || first == "deny" || first == "default"
            "permission-policy" -> first == "grant" || first == "deny"
            "keyguard", "status-bar" -> first == "disable"
            "adb" -> first == "enable"
            "private-dns" -> first != null && first != "status"
            "uninstall" -> true
            "lock-task" -> first == "set" || first == "clear"
            "vpn" -> first == "on"
            "time" -> first == "set"
            "ime" -> first == "allow" || first == "reset"
            "adb-wifi" -> first == "enable"
            else -> false
        }
    }

    private fun runCommand(cmd: String, a: List<String>, raw: String): Result = when (cmd) {
        "help", "-h", "--help", "?" -> help(a)
        "ping" -> Result()
        "version", "-v", "--version" -> version()
        "id", "whoami" -> whoami()
        "device" -> device()
        "status" -> status()
        "wipe" -> wipe(a)

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
            if (hide) selfGuard("hide", a[0])?.let { return it }
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

        "log" -> log(a)
        "batch" -> batch(a)
        "uninstall" -> uninstall(a)
        "app-restrictions" -> appRestrictions(a)
        "bugreport" -> outcome(
            dpm.requestBugreport(admin),
            "a bug report was requested; the user is asked to confirm it",
            "the system refused the bug report (one is already running, or it is not allowed)"
        )
        "lock-task" -> lockTask(a)
        "vpn" -> vpn(a)
        "location" -> location(a)
        "backup" -> onOff(
            a, "backup", USAGE.getValue(cmd),
            { dpm.setBackupServiceEnabled(admin, it) }, { dpm.isBackupServiceEnabled(admin) }
        )
        "time" -> setTime(a)
        "lock-timeout" -> lockTimeout(a)
        "adb-wifi" -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) unsupported()
        else globalFlag(
            a, "adb-wifi", USAGE.getValue(cmd), "adb_wifi_enabled", "1", "0",
            enableWord = "enable", disableWord = "disable", onLabel = "enabled", offLabel = "disabled"
        )
        "ime" -> ime(a)

        else -> {
            val hints = USAGE.keys.filter { it.startsWith(cmd.take(2)) }.take(5)
            val extra = if (hints.isEmpty()) "" else "Did you mean: " + hints.joinToString(", ") + "?\n"
            Result(err = str(R.string.dish_err_unknown_command, raw) + "\n" + extra, code = 127)
        }
    }

    // ------------------------------------------------------------------ help and info

    /** `help` = names only, grouped. `help COMMAND` = description, usage, example. */
    private fun help(a: List<String>): Result {
        if (a.firstOrNull() == "-s") {
            val word = a.getOrNull(1)?.lowercase(Locale.ROOT) ?: throw UsageException("help -s WORD")
            val hits = ENTRIES.filter {
                it.name.contains(word) || it.desc.lowercase(Locale.ROOT).contains(word)
            }
            if (hits.isEmpty()) return Result(out = "No command matches \"$word\".\n")
            return Result(out = hits.joinToString("\n") { "${it.name} - ${it.desc}" } + "\n")
        }
        if (a.isEmpty()) {
            val sb = StringBuilder()
            sb.append("dish - Device Owner console\n")
            sb.append("help COMMAND shows what a command does. api = direct calls.\n")
            sb.append("Mode: ${modeName()}\n")
            sb.append("Dangerous commands: ${if (dangerousEnabled) "on" else "off"}\n\n")
            for ((group, items) in ENTRIES.groupBy { it.group }) {
                sb.append(group).append(":\n  ")
                    .append(items.joinToString(" ") {
                        it.name + (if (it.name in DEVICE_OWNER_ONLY) "*" else "") +
                            (if (it.name in DANGEROUS_COMMANDS) "!" else "")
                    })
                    .append("\n")
            }
            sb.append("\n* needs Device Owner (not available in Profile Owner mode)\n")
            sb.append("! some or all forms are dangerous: they need Enable dangerous commands (off by default)\n")
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
        val note = OWNER_NOTES[e.name]
            ?: if (e.name in DEVICE_OWNER_ONLY) "Device Owner only (status/get still work in Profile Owner mode)"
            else "Device Owner and Profile Owner (in Profile Owner mode it affects the managed profile only)"
        sb.append("Needs: ").append(note).append("\n")
        if (e.name in DANGEROUS_COMMANDS) {
            sb.append("Dangerous: some or all forms need Enable dangerous commands in the DhizukuF settings\n")
        }
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
                "Mode: ${modeName()}\n" +
                "Device owner: ${yesNo(isDeviceOwner())}\n" +
                "Profile owner: ${yesNo(isProfileOwner())}\n" +
                (if (isDeviceOwner()) "" else "Limited: commands marked * in help need Device Owner\n") +
                "Admin component: $adminName\n" +
                "Dangerous commands: ${if (dangerousEnabled) "on" else "off"}\n" +
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
        if (flag) selfGuard("suspend", *a.toTypedArray())?.let { return it }
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
        selfGuard("clear the data of", a[0])?.let { return it }
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
                // Read the value back when the feature has a getter (camera, screen capture); a setter
                // that returns nothing says nothing about the result.
                val applied = if (get != null) runCatching { get() == disable }.getOrDefault(r != false)
                else r != false
                outcome(
                    applied,
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
                if (space == "global" && !isDeviceOwner()) return needsDeviceOwner("settings put global")
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


    // ------------------------------------------------------------------ log, batch and more

    private fun log(a: List<String>): Result {
        val usage = USAGE.getValue("log")
        if (a.firstOrNull() == "clear") {
            DishAudit.clear()
            return done("the dish log is cleared")
        }
        val n = if (a.isEmpty()) 20 else a[0].toIntOrNull() ?: throw UsageException(usage)
        val lines = DishAudit.last(n.coerceIn(1, 200))
        if (lines.isEmpty()) return Result(out = "The dish log is empty.\n")
        return Result(out = lines.joinToString("\n") + "\n")
    }

    /** Splits a command line into words; understands 'single' and "double" quotes and backslashes. */
    private fun splitLine(line: String): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        var quote = '\u0000'
        var started = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (quote != '\u0000') {
                if (c == quote) quote = '\u0000'
                // like a shell: inside "..." a backslash only escapes " \ $ and `, so "C:\path" keeps its backslash
                else if (c == '\\' && quote == '"' && i + 1 < line.length && line[i + 1] in "\"\\$`") cur.append(line[++i])
                else cur.append(c)
            } else if (c == '\'' || c == '"') {
                quote = c
                started = true
            } else if (c == '\\' && i + 1 < line.length) {
                cur.append(line[++i])
                started = true
            } else if (c.isWhitespace()) {
                if (started || cur.isNotEmpty()) out.add(cur.toString())
                cur.setLength(0)
                started = false
            } else {
                cur.append(c)
            }
            i++
        }
        if (started || cur.isNotEmpty()) out.add(cur.toString())
        return out
    }

    /** Runs several command lines in a row: batch "hide com.a" "camera disable". */
    private fun batch(a: List<String>): Result {
        val usage = USAGE.getValue("batch")
        val keepGoing = a.firstOrNull() == "--keep-going"
        val lines = if (keepGoing) a.drop(1) else a
        if (lines.isEmpty()) throw UsageException(usage)
        if (lines.size > 50) return notDone("a batch can hold at most 50 commands")
        val out = StringBuilder()
        val err = StringBuilder()
        var worst = 0
        for ((i, line) in lines.withIndex()) {
            val words = splitLine(line)
            if (words.isEmpty()) continue
            out.append("[").append(i + 1).append("] ").append(line).append("\n")
            val name = words[0].lowercase(Locale.ROOT)
            val r = if (name == "batch") notDone("batches cannot be nested")
            else runSafely(name, words.drop(1), words[0])
            out.append(r.out)
            err.append(r.err)
            if (r.code != 0) {
                // Strongest code wins under --keep-going (e.g. 127 > 1).
                if (r.code > worst) worst = r.code
                if (!keepGoing) {
                    out.append("stopped after the first failure (use --keep-going to continue)\n")
                    break
                }
            }
        }
        return Result(out.toString(), err.toString(), worst)
    }

    private fun uninstall(a: List<String>): Result {
        need(a, 1, USAGE.getValue("uninstall"))
        val pkg = a[0]
        selfGuard("uninstall", pkg)?.let { return it }
        val action = "${context.packageName}.DISH_UNINSTALL"
        val latch = CountDownLatch(1)
        var status = Int.MIN_VALUE
        var message = ""
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)
                message = i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""
                latch.countDown()
            }
        }
        val filter = IntentFilter(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        try {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val pending = PendingIntent.getBroadcast(
                context, 0, Intent(action).setPackage(context.packageName), flags
            )
            context.packageManager.packageInstaller.uninstall(pkg, pending.intentSender)
            if (!latch.await(20, TimeUnit.SECONDS)) return notDone("uninstalling $pkg timed out")
        } finally {
            context.unregisterReceiver(receiver)
        }
        return when (status) {
            PackageInstaller.STATUS_SUCCESS -> done("$pkg is uninstalled")
            PackageInstaller.STATUS_PENDING_USER_ACTION ->
                notDone("Android asks the user to confirm the uninstall of $pkg, so it was not done silently")
            else -> notDone("could not uninstall $pkg (status $status${if (message.isEmpty()) "" else ": $message"})")
        }
    }

    @Suppress("DEPRECATION")
    private fun appRestrictions(a: List<String>): Result {
        val usage = USAGE.getValue("app-restrictions")
        need(a, 2, usage)
        val pkg = a[1]
        return when (a[0]) {
            "get" -> {
                val b = dpm.getApplicationRestrictions(admin, pkg)
                if (b.size() == 0) Result(out = "$pkg has no restrictions set.\n")
                else Result(out = b.keySet().sorted().joinToString("\n") { "$it=${b.get(it)}" } + "\n")
            }

            "set" -> {
                need(a, 4, usage)
                val b = Bundle(dpm.getApplicationRestrictions(admin, pkg))
                val key = a[2]
                val v = a[3]
                when {
                    v == "true" || v == "false" -> b.putBoolean(key, v == "true")
                    v.toIntOrNull() != null -> b.putInt(key, v.toInt())
                    else -> b.putString(key, v)
                }
                dpm.setApplicationRestrictions(admin, pkg, b)
                done("$pkg: $key = $v")
            }

            "clear" -> {
                dpm.setApplicationRestrictions(admin, pkg, Bundle())
                done("restrictions of $pkg are cleared")
            }

            else -> throw UsageException(usage)
        }
    }

    private fun lockTask(a: List<String>): Result {
        val usage = USAGE.getValue("lock-task")
        need(a, 1, usage)
        return when (a[0]) {
            "status" -> {
                val p = dpm.getLockTaskPackages(admin)
                Result(out = "lock task packages: " + (if (p.isEmpty()) "(none)" else p.joinToString(", ")) + "\n")
            }

            "set" -> {
                need(a, 2, usage)
                dpm.setLockTaskPackages(admin, a.drop(1).toTypedArray())
                done("lock task packages: ${a.drop(1).joinToString(", ")}")
            }

            "clear" -> {
                dpm.setLockTaskPackages(admin, emptyArray<String>())
                done("lock task packages are cleared")
            }

            else -> throw UsageException(usage)
        }
    }

    private fun vpn(a: List<String>): Result {
        val usage = USAGE.getValue("vpn")
        need(a, 1, usage)
        return when (a[0]) {
            "status" -> Result(out = "always-on VPN: ${dpm.getAlwaysOnVpnPackage(admin) ?: "off"}\n")

            "off" -> {
                dpm.setAlwaysOnVpnPackage(admin, null, false)
                done("always-on VPN is off")
            }

            "on" -> {
                need(a, 2, usage)
                val lockdown = a.getOrNull(2) == "lockdown"
                dpm.setAlwaysOnVpnPackage(admin, a[1], lockdown)
                done("always-on VPN is ${a[1]}" + if (lockdown) " (lockdown: no traffic without the VPN)" else "")
            }

            else -> throw UsageException(usage)
        }
    }

    @Suppress("DEPRECATION")
    private fun locationOn(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) lm.isLocationEnabled
        else Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, 0) != 0
    }

    private fun location(a: List<String>): Result {
        val usage = USAGE.getValue("location")
        need(a, 1, usage)
        return when (a[0]) {
            "status" -> Result(out = "location: ${if (locationOn()) "on" else "off"}\n")

            "on", "off" -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return unsupported()
                val on = a[0] == "on"
                dpm.setLocationEnabled(admin, on)
                outcome(locationOn() == on, "location is ${a[0]}", "the system did not turn location ${a[0]}")
            }

            else -> throw UsageException(usage)
        }
    }

    private fun setTime(a: List<String>): Result {
        val usage = USAGE.getValue("time")
        if (a.size < 2 || a[0] != "set") throw UsageException(usage)
        val millis = a[1].toLongOrNull() ?: throw UsageException(usage)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return unsupported()
        return outcome(
            dpm.setTime(admin, millis),
            "time is set to ${java.util.Date(millis)}",
            "the system refused to set the time (turn auto-time off first)"
        )
    }

    @Suppress("DEPRECATION")
    private fun lockTimeout(a: List<String>): Result {
        val usage = USAGE.getValue("lock-timeout")
        need(a, 1, usage)
        return when (a[0]) {
            "status" -> Result(out = "lock timeout: ${dpm.getMaximumTimeToLock(admin)} ms (0 = no limit)\n")

            "set" -> {
                need(a, 2, usage)
                val ms = a[1].toLongOrNull() ?: throw UsageException(usage)
                dpm.setMaximumTimeToLock(admin, ms)
                done("the screen locks after at most $ms ms")
            }

            else -> throw UsageException(usage)
        }
    }

    private fun ime(a: List<String>): Result {
        val usage = USAGE.getValue("ime")
        need(a, 1, usage)
        return when (a[0]) {
            "list" -> {
                val p = dpm.getPermittedInputMethods(admin)
                Result(out = if (p == null) "all input methods are allowed\n" else p.joinToString("\n") + "\n")
            }

            "allow" -> {
                need(a, 2, usage)
                outcome(
                    dpm.setPermittedInputMethods(admin, a.drop(1)),
                    "only these input methods are allowed: ${a.drop(1).joinToString(", ")}",
                    "the system refused the list (it must name installed input methods, and system ones stay allowed)"
                )
            }

            "reset" -> outcome(
                dpm.setPermittedInputMethods(admin, null),
                "all input methods are allowed again",
                "the system refused to reset the list"
            )

            else -> throw UsageException(usage)
        }
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

    /**
     * Hard blocks only when *Enable dangerous commands* is off.
     * With the switch on, dish api can call the same methods as the Dhizuku API.
     */
    private fun isDenied(name: String): Boolean {
        if (dangerousEnabled) return false
        val l = name.lowercase(Locale.ROOT)
        return name in DENIED_METHODS || l.contains("wipe") || l.contains("factoryreset") ||
            l.contains("removeuser")
    }

    private fun signature(m: Method, hideAdmin: Boolean): String {
        var params = m.parameterTypes.toList()
        if (hideAdmin && params.firstOrNull() == ComponentName::class.java) params = params.drop(1)
        return "${m.returnType.simpleName} ${m.name}(${params.joinToString(", ") { it.simpleName }})"
    }


    /**
     * Factory-reset style wipe via DevicePolicyManager. Requires *Enable dangerous commands*.
     * Usage: wipe [flags...]
     * Flags (optional, combined): external | euicc | reset-protection | silent | all
     * Default flags: 0 (wipe device data only, same as Settings factory reset data partition).
     */
    private fun wipe(a: List<String>): Result {
        var flags = 0
        for (tok in a) {
            when (tok.lowercase(Locale.ROOT)) {
                "external", "wipe_external_storage" ->
                    flags = flags or DevicePolicyManager.WIPE_EXTERNAL_STORAGE
                "euicc", "wipe_euicc" ->
                    flags = flags or DevicePolicyManager.WIPE_EUICC
                "reset-protection", "wipe_reset_protection_data" ->
                    flags = flags or DevicePolicyManager.WIPE_RESET_PROTECTION_DATA
                "silent", "wipe_silently" ->
                    flags = flags or DevicePolicyManager.WIPE_SILENTLY
                "all" -> flags = flags or
                    DevicePolicyManager.WIPE_EXTERNAL_STORAGE or
                    DevicePolicyManager.WIPE_EUICC or
                    DevicePolicyManager.WIPE_RESET_PROTECTION_DATA
                else -> return notDone(
                    """unknown wipe flag "$tok". Use: external | euicc | reset-protection | silent | all"""
                )
            }
        }
        dpm.wipeData(flags)
        return done("wipe started (flags=$flags); the device should reset now")
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
        // Outside dpm this can clear DhizukuF itself — only with dangerous commands on.
        if (!dangerousEnabled && on != "dpm" && name == "clearApplicationUserData") {
            return notDone(str(R.string.dish_err_method_denied, name))
        }
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
            Entry("log", GEN, "log [N] | log clear", "Show the last commands run through dish (kept in memory, newest last).",
                "log 10"),
            Entry("batch", GEN, "batch [--keep-going] \"CMD\" \"CMD\" ...",
                "Run several commands in a row; stops at the first failure unless --keep-going.",
                "batch \"hide com.a\" \"camera disable\""),
            Entry("wipe", GEN, "wipe [external|euicc|reset-protection|silent|all]...",
                "Factory-reset the device (DevicePolicyManager.wipeData). Needs Enable dangerous commands.",
                "wipe external euicc"),
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
            Entry("uninstall", APPS, "uninstall PKG", "Uninstall an app silently (as Device Owner).", "uninstall com.example.app"),
            Entry("app-restrictions", APPS, "app-restrictions get|set|clear PKG [KEY VALUE]",
                "Managed configuration of an app (true/false and numbers are typed).",
                "app-restrictions set com.example.app server https://example.org"),
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
            Entry("bugreport", DEV, "bugreport", "Ask Android to take a bug report (the user confirms it)."),
            Entry("lock-task", DEV, "lock-task status|set PKG...|clear", "Kiosk mode: apps allowed to pin themselves.",
                "lock-task set com.example.kiosk"),
            Entry("vpn", DEV, "vpn on PKG [lockdown] | off | status", "Always-on VPN; lockdown blocks traffic without it.",
                "vpn on org.example.vpn lockdown"),
            Entry("location", DEV, "location on|off|status", "Turn device location on or off."),
            Entry("backup", DEV, "backup on|off|status", "Turn the backup service on or off."),
            Entry("time", DEV, "time set EPOCH_MS", "Set the clock (turn auto-time off first).", "time set 1760000000000"),
            Entry("lock-timeout", DEV, "lock-timeout status | set MS", "Longest time before the screen locks.",
                "lock-timeout set 60000"),
            Entry("adb-wifi", DEV, "adb-wifi enable|disable|status", "Wireless debugging (Android 11+)."),
            Entry("ime", DEV, "ime list | allow PKG... | reset", "Restrict which keyboards may be used.",
                "ime allow com.google.android.inputmethod.latin"),
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
                    "cn:pkg/class strs:a,b ints:1,2 null @admin. Wipe/ownership need Enable dangerous commands.",
                "api setLockTaskPackages strs:com.a,com.b")
        )

        /** Commands that change device-wide state: Device Owner only. */
        private val DEVICE_OWNER_ONLY = setOf(
            "reboot", "adb", "stay-awake", "timezone", "private-dns",
            "keyguard", "status-bar", "owner-info", "auto-time", "auto-timezone",
            "bugreport", "lock-task", "location", "time", "adb-wifi"
        )

        /** Commands with at least one dangerous form (see isDangerous). */
        private val DANGEROUS_COMMANDS = setOf(
            "reboot", "clear-data", "wipe", "api", "settings", "permission", "permission-policy",
            "keyguard", "status-bar", "adb", "private-dns", "uninstall", "lock-task", "vpn", "time",
            "ime", "adb-wifi"
        )

        private val OWNER_NOTES = mapOf(
            "settings" to "Device Owner for: settings put global. Other forms work in both modes",
            "api" to "depends on the method; many DevicePolicyManager methods need Device Owner",
            "help" to "nothing special",
            "log" to "nothing special",
            "batch" to "each command inside keeps its own requirements",
            "ping" to "Device Owner or Profile Owner",
            "version" to "nothing special",
            "id" to "nothing special",
            "device" to "nothing special",
            "status" to "Device Owner or Profile Owner"
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
