package com.EdS.DhizukuF.customization

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.EdS.DhizukuF.BuildConfig
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import com.EdS.DhizukuF.ui.activity.MainActivity
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * The name and the icon of DhizukuF (Settings > Customization). The package name never changes.
 *
 * Android cannot rename or redraw the launcher entry of an installed app. So there are two ways:
 *  - **The two built-in icons** are two launcher entries of the manifest (`activity-alias`): the DhizukuF icon and the
 *    icon of the original Dhizuku. Exactly one is switched on at a time; the other is switched off, so there is always
 *    one launcher entry and the app can never disappear. Both come with a TV banner.
 *  - **An icon of the user** (a picture) and a custom name become a pinned launcher shortcut (phones and tablets; a TV
 *    box has no pinned shortcuts), and are also used inside DhizukuF. The normal launcher entry stays.
 */
object AppearanceManager {
    private const val TAG = "AppearanceManager"
    // The name of the old launcher activity: icons of earlier versions on the home screen point to it (see the manifest).
    private const val ALIAS_DEFAULT = "com.EdS.DhizukuF.ui.activity.SettingsActivity"
    private const val ALIAS_ORIGINAL = "com.EdS.DhizukuF.alias.LauncherOriginal"

    /** Side of the saved picture. */
    const val ICON_SIZE = 192
    private const val MAX_DECODE_SIDE = 2048
    private const val SHORTCUT_PREFIX = "custom_"

    enum class PinResult { Requested, Unsupported, Failed }

    // ---------------------------------------------------------------- the two built-in icons

    private fun component(context: Context, name: String) = ComponentName(context.packageName, name)

    private fun isEnabled(context: Context, name: String, enabledInManifest: Boolean): Boolean =
        when (context.packageManager.getComponentEnabledSetting(component(context, name))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            else -> enabledInManifest
        }

    /** Which of the two built-in launcher entries is on now. */
    fun builtInInUse(context: Context): IconChoice =
        if (isEnabled(context, ALIAS_ORIGINAL, false) && !isEnabled(context, ALIAS_DEFAULT, true)) IconChoice.Original
        else IconChoice.Default

    /**
     * Switches to the DhizukuF icon or to the icon of the original Dhizuku. The new entry is switched on first and
     * the old one off afterwards, so there is never a moment without a launcher entry. The app is not restarted.
     */
    fun applyBuiltIn(context: Context, choice: IconChoice): Boolean {
        val wanted = if (choice == IconChoice.Original) ALIAS_ORIGINAL else ALIAS_DEFAULT
        val other = if (wanted == ALIAS_ORIGINAL) ALIAS_DEFAULT else ALIAS_ORIGINAL
        return try {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                component(context, wanted), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                component(context, other), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
            )
            // Do not trust the call: read the state back, so a system that did not switch is not reported as done.
            builtInInUse(context) == (if (choice == IconChoice.Original) IconChoice.Original else IconChoice.Default)
        } catch (t: Throwable) {
            Log.w(TAG, "cannot switch the launcher entry", t)
            false
        }
    }

    // ---------------------------------------------------------------- icons of the user

    private fun customDir(context: Context) = File(context.filesDir, "custom_icons")

    private fun customFile(context: Context, id: String) = File(customDir(context), "$id.png")

    /**
     * Reads a picture, cuts the middle square out of it and saves it as a small PNG. Returns the id of the new icon,
     * or null when the file is not a picture. Never loads a huge picture whole.
     */
    fun addCustomIcon(context: Context, uri: Uri): String? {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_DECODE_SIDE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return null
            val side = min(decoded.width, decoded.height)
            val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
            val icon = Bitmap.createScaledBitmap(square, ICON_SIZE, ICON_SIZE, true)
            val id = CustomIcons.newId()
            customDir(context).mkdirs()
            customFile(context, id).outputStream().use { icon.compress(Bitmap.CompressFormat.PNG, 100, it) }
            if (square !== decoded) decoded.recycle()
            if (icon !== square) square.recycle()
            id
        } catch (t: Throwable) {
            Log.w(TAG, "cannot add the icon", t)
            null
        }
    }

    fun loadCustomIcon(context: Context, id: String): Bitmap? {
        if (!CustomIcons.isValidId(id)) return null
        return try {
            BitmapFactory.decodeFile(customFile(context, id).absolutePath)
        } catch (_: Throwable) {
            null
        }
    }

    /** Deletes the picture, and disables its launcher shortcut (a pinned shortcut can only be switched off). */
    fun deleteCustomIcon(context: Context, id: String, disabledMessage: String) {
        if (!CustomIcons.isValidId(id)) return
        customFile(context, id).delete()
        runCatching {
            ShortcutManagerCompat.disableShortcuts(context, listOf(SHORTCUT_PREFIX + id), disabledMessage)
        }
    }

    /**
     * Asks the launcher to add a shortcut with the icon and the name of the user (the system shows its own
     * confirmation). A TV box and some launchers cannot do this: [PinResult.Unsupported].
     */
    fun pinShortcut(context: Context, id: String, label: String): PinResult {
        val bitmap = loadCustomIcon(context, id) ?: return PinResult.Failed
        return try {
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return PinResult.Unsupported
            val info = ShortcutInfoCompat.Builder(context, SHORTCUT_PREFIX + id)
                .setShortLabel(label.take(24))
                .setLongLabel(label)
                .setIcon(IconCompat.createWithAdaptiveBitmap(padded(bitmap)))
                .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
                .build()
            // a shortcut that is already on the home screen gets the new name and picture
            runCatching { ShortcutManagerCompat.updateShortcuts(context, listOf(info)) }
            if (ShortcutManagerCompat.requestPinShortcut(context, info, null)) PinResult.Requested else PinResult.Failed
        } catch (t: Throwable) {
            Log.w(TAG, "cannot pin the shortcut", t)
            PinResult.Failed
        }
    }

    /** An adaptive icon is cut by the launcher: the picture goes into its safe middle (66 of 108) so that it all shows. */
    private fun padded(icon: Bitmap): Bitmap {
        val canvasSize = 432
        val inner = canvasSize * 66 / 108
        val out = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val scaled = Bitmap.createScaledBitmap(icon, inner, inner, true)
        Canvas(out).drawBitmap(scaled, ((canvasSize - inner) / 2).toFloat(), ((canvasSize - inner) / 2).toFloat(), null)
        return out
    }

    // ---------------------------------------------------------------- start-up

    /**
     * "Remember for updates". On every start the saved icon is put back if it is not in place. After an update
     * (a new versionCode) with the option off, the name and the icon go back to the default ones.
     */
    fun restore(context: Context, repo: SettingsRepo) {
        try {
            val plan = RestoreLogic.plan(
                config = repo.appearance,
                storedVersion = repo.appearanceVersion,
                currentVersion = BuildConfig.VERSION_CODE,
                inUse = builtInInUse(context)
            )
            if (plan.resetToDefault) {
                repo.appDisplayName = ""
                repo.selectedIcon = IconChoice.Default
            }
            plan.switchTo?.let { applyBuiltIn(context, it) }
            if (plan.storeVersion) repo.appearanceVersion = BuildConfig.VERSION_CODE
        } catch (t: Throwable) {
            Log.w(TAG, "cannot restore the appearance", t)
        }
    }
}
