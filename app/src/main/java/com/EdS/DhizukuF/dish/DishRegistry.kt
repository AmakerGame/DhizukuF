package com.EdS.DhizukuF.dish

import android.content.Context
import androidx.core.content.edit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Terminals that sent a dish registration request. They are shown in DhizukuF > App management
 * under the name "dish" (with the terminal app in brackets), even before they are approved.
 */
object DishRegistry : KoinComponent {
    private const val PREFIX = "uid_"
    private val context by inject<Context>()

    private val prefs by lazy {
        context.getSharedPreferences("dish_registry", Context.MODE_PRIVATE)
    }

    fun register(uid: Int, label: String) {
        if (prefs.getString(PREFIX + uid, null) == label) return
        prefs.edit { putString(PREFIX + uid, label) }
    }

    fun isDish(uid: Int): Boolean = prefs.contains(PREFIX + uid)

    fun all(): Map<Int, String> = prefs.all
        .filterKeys { it.startsWith(PREFIX) }
        .mapNotNull { (key, value) ->
            val uid = key.removePrefix(PREFIX).toIntOrNull() ?: return@mapNotNull null
            uid to (value as? String ?: "")
        }.toMap()

    /** Name shown in the list: "dish (Termux)". */
    fun displayName(uid: Int, appLabel: String?): String {
        val label = appLabel?.takeIf { it.isNotBlank() } ?: prefs.getString(PREFIX + uid, null) ?: "uid $uid"
        return "dish ($label)"
    }
}
