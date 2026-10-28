package com.EdS.DhizukuF.server

import android.os.IBinder
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Modifier
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Decides which calls of the Dhizuku API are "dangerous" (the ones that need *Enable dangerous commands*).
 *
 * The Dhizuku library gives the server only the name of the API function (`remote_transact`, ...). The real
 * action is the Binder transaction that the client sends through `remote_transact`, so it is recognised here:
 * the interface of the target binder plus the transaction code are turned into the AIDL method name
 * (`IDevicePolicyManager.Stub.TRANSACTION_wipeData` -> `wipeData`) and compared with the lists below.
 */
object DangerousCalls {
    private const val TAG = "DangerousCalls"

    /**
     * Functions of the API that cannot be filtered by name: they run client code with the rights of the owner
     * (`remote_process`, `bind_user_service`) or give owner powers to any package (`set_delegated_scopes`).
     * Whoever may call them can do everything the switch is meant to stop, so they are dangerous as a whole.
     */
    val UNFILTERABLE_FUNCTIONS = setOf("remote_process", "bind_user_service", "set_delegated_scopes")

    private const val DPM = "android.app.admin.IDevicePolicyManager"
    private const val USER_MANAGER = "android.os.IUserManager"
    private const val PACKAGE_INSTALLER = "android.content.pm.IPackageInstaller"
    private const val PACKAGE_MANAGER = "android.content.pm.IPackageManager"
    private const val POWER_MANAGER = "android.os.IPowerManager"

    /** Methods that wipe the device or a user, or remove one. Matched by part of the name (they changed names between versions). */
    private val CATASTROPHIC_PARTS = listOf("wipe", "factoryreset", "removeuser")

    /** The same risky actions that the dish commands behind the switch cover, plus owner / lock-out ones. */
    private val DPM_DANGEROUS = setOf(
        "reboot", "clearApplicationUserData",
        "setGlobalSetting", "setSecureSetting", "setSystemSetting",
        "setPermissionGrantState", "setPermissionPolicy",
        "setKeyguardDisabled", "setStatusBarDisabled",
        "setGlobalPrivateDnsModeSpecifiedHost", "setGlobalPrivateDnsModeOpportunistic",
        "setLockTaskPackages", "setLockTaskFeatures",
        "setAlwaysOnVpnPackage", "setTime", "setTimeZone", "setPermittedInputMethods",
        "clearDeviceOwner", "clearDeviceOwnerApp", "clearProfileOwner", "transferOwnership",
        "forceRemoveActiveAdmin", "resetPassword", "resetPasswordWithToken",
        "setMaximumFailedPasswordsForWipe"
    ).map { it.lowercase(Locale.ROOT) }.toSet()

    private val OTHER_DANGEROUS: Map<String, Set<String>> = mapOf(
        PACKAGE_INSTALLER to setOf("uninstall", "uninstallExistingPackage"),
        PACKAGE_MANAGER to setOf("deletePackageAsUser", "deletePackageVersioned", "clearApplicationUserData"),
        POWER_MANAGER to setOf("reboot", "shutdown", "rebootSafeMode", "crash")
    ).mapValues { (_, v) -> v.map { it.lowercase(Locale.ROOT) }.toSet() }

    private val transactionNames = ConcurrentHashMap<String, Map<Int, String>>()

    /** Whether [method] of the AIDL interface [descriptor] needs the switch. Pure: no Binder, no reflection. */
    fun isDangerous(descriptor: String, method: String): Boolean {
        val m = method.lowercase(Locale.ROOT)
        return when (descriptor) {
            DPM -> m in DPM_DANGEROUS || isCatastrophic(m)
            USER_MANAGER -> m.startsWith("removeuser")
            else -> OTHER_DANGEROUS[descriptor]?.contains(m) == true
        }
    }

    /** Wipe, factory reset, removing a user. */
    fun isCatastrophic(method: String): Boolean {
        val m = method.lowercase(Locale.ROOT)
        return CATASTROPHIC_PARTS.any { m.contains(it) }
    }

    /**
     * The name of the dangerous method that transaction [code] of [binder] stands for, or null when the call is
     * not dangerous or cannot be recognised. With [catastrophicOnly] only wipe / factory reset / remove user count.
     */
    fun dangerousMethod(binder: IBinder, code: Int, catastrophicOnly: Boolean = false): String? {
        val descriptor = try {
            binder.interfaceDescriptor
        } catch (_: Throwable) {
            return null
        } ?: return null
        if (descriptor != DPM && descriptor != USER_MANAGER && descriptor !in OTHER_DANGEROUS) return null
        val names = transactionNames[descriptor] ?: loadNames(descriptor).also {
            // An empty answer is not remembered: the next call tries again.
            if (it.isNotEmpty()) transactionNames[descriptor] = it
        }
        val name = names[code] ?: return null
        val hit = if (catastrophicOnly) isCatastrophic(name) else isDangerous(descriptor, name)
        return if (hit) name else null
    }

    private val unresolvable = ConcurrentHashMap.newKeySet<String>()

    /**
     * "IDevicePolicyManager.setApplicationHidden" for the journal, or null when the name is not known. Used only
     * while logging is on. An interface whose names cannot be read is remembered, so it is tried once.
     */
    fun methodLabel(binder: IBinder, code: Int): String? {
        val descriptor = try {
            binder.interfaceDescriptor
        } catch (_: Throwable) {
            null
        } ?: return null
        val names = transactionNames[descriptor]
            ?: if (descriptor in unresolvable) emptyMap() else loadNames(descriptor).also {
                if (it.isNotEmpty()) transactionNames[descriptor] = it else unresolvable.add(descriptor)
            }
        val name = names[code] ?: return null
        return descriptor.substringAfterLast('.') + "." + name
    }

    /** `TRANSACTION_xxx` constants of the generated Stub class of the interface, by code. */
    private fun loadNames(descriptor: String): Map<Int, String> = try {
        // The constants are hidden API: without the exemption the system hides them from reflection (Android 9+),
        // and the names would stay unknown. It is also set at start-up; setting it twice does no harm.
        runCatching { HiddenApiBypass.addHiddenApiExemptions("") }
        val stub = Class.forName("$descriptor\$Stub")
        val result = HashMap<Int, String>()
        for (field in stub.declaredFields) {
            if (!Modifier.isStatic(field.modifiers) || field.type != Int::class.javaPrimitiveType) continue
            if (!field.name.startsWith("TRANSACTION_")) continue
            field.isAccessible = true
            result[field.getInt(null)] = field.name.removePrefix("TRANSACTION_")
        }
        result
    } catch (t: Throwable) {
        // Without the names nothing can be recognised: such a call is let through, so say so in the log.
        Log.w(TAG, "cannot read the transaction names of $descriptor", t)
        emptyMap()
    }
}
