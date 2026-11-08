package com.EdS.DhizukuF.data.common.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * SharedPreferences that are readable before the first unlock (Direct Boot).
 *
 * The provider, the boot receiver and the daemon service are directBootAware and the database already
 * lives in device protected storage, but ordinary preferences live in credential protected storage,
 * where getSharedPreferences() throws IllegalStateException until the user unlocks the device.
 * Existing preferences are moved over once they become reachable.
 */
fun Context.devicePrefs(name: String): SharedPreferences {
    val app = applicationContext ?: this
    val protectedContext = if (app.isDeviceProtectedStorage) app else app.createDeviceProtectedStorageContext()
    if (protectedContext !== app) {
        try {
            protectedContext.moveSharedPreferencesFrom(app, name)
        } catch (t: Throwable) {
            // still locked: the move is tried again on the next start
            Log.w("StorageUtil", "cannot migrate preferences $name yet", t)
        }
    }
    return protectedContext.getSharedPreferences(name, Context.MODE_PRIVATE)
}
