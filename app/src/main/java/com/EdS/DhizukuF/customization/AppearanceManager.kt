package com.EdS.DhizukuF.customization

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.EdS.DhizukuF.BuildConfig
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo

/**
 * The name and the icon of DhizukuF in the launcher (Settings > Customization). The package name never changes.
 *
 * Android cannot rename or redraw the launcher entry of an installed app, so the manifest declares every choice
 * ahead of time: 4 names (DhizukuF, Dhizuku, Profile Owner, Device Owner) x 2 icons (the new one, the one of the
 * original Dhizuku) = 8 launcher entries (`activity-alias`), each with its own name, icon and TV banner. Exactly one
 * is switched on at a time. The new one is switched on first and the others off afterwards, so there is never a
 * moment without a launcher entry and the app cannot disappear. The name and the icon are chosen independently of
 * each other: changing one keeps the other.
 */
object AppearanceManager {
    private const val TAG = "AppearanceManager"

    private fun component(context: Context, entry: LauncherEntry) =
        ComponentName(context.packageName, LauncherAliases.className(entry))

    private fun isEnabled(context: Context, entry: LauncherEntry): Boolean = try {
        when (context.packageManager.getComponentEnabledSetting(component(context, entry))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            // not touched yet: as the manifest says (only the default entry is on)
            else -> entry == LauncherEntry.DEFAULT
        }
    } catch (_: Throwable) {
        false
    }

    /** The launcher entry that is switched on now (read from the system), or null when it cannot be told. */
    fun inUse(context: Context): LauncherEntry? {
        val on = LauncherAliases.all.filter { isEnabled(context, it) }
        return when {
            on.isEmpty() -> null
            on.size == 1 -> on.first()
            else -> on.firstOrNull { it == LauncherEntry.DEFAULT } ?: on.first()
        }
    }

    /** Switches to [entry]. Returns true only when the system really switched (the state is read back). */
    fun apply(context: Context, entry: LauncherEntry): Boolean = try {
        if (!switchOnce(context, entry)) {
            // OEM / race: try once more so leftover aliases are disabled
            Log.w(TAG, "launcher switch not confirmed, retrying")
            switchOnce(context, entry)
        }
        val ok = inUse(context) == entry
        if (!ok) Log.w(TAG, "launcher entry still not $entry after apply")
        ok
    } catch (t: Throwable) {
        Log.w(TAG, "cannot switch the launcher entry", t)
        false
    }

    private fun switchOnce(context: Context, entry: LauncherEntry): Boolean {
        val pm = context.packageManager
        // Enable the new entry first so the app never disappears from the launcher.
        pm.setComponentEnabledSetting(
            component(context, entry), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
        for (other in LauncherAliases.all) {
            if (other == entry) continue
            pm.setComponentEnabledSetting(
                component(context, other), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
            )
        }
        return inUse(context) == entry
    }

    /**
     * "Remember for updates". On every start the saved entry is put back if it is not in place. After an update (a
     * new versionCode) with the option off, the name and the icon go back to the default ones.
     */
    fun restore(context: Context, repo: SettingsRepo) {
        try {
            val plan = RestoreLogic.plan(
                config = repo.appearance,
                storedVersion = repo.appearanceVersion,
                currentVersion = BuildConfig.VERSION_CODE,
                inUse = inUse(context)
            )
            if (plan.resetToDefault) {
                repo.appName = AppName.DhizukuF
                repo.selectedIcon = IconChoice.Default
            }
            plan.switchTo?.let { apply(context, it) }
            if (plan.storeVersion) repo.appearanceVersion = BuildConfig.VERSION_CODE
        } catch (t: Throwable) {
            Log.w(TAG, "cannot restore the appearance", t)
        }
    }
}
