package com.EdS.DhizukuF.data.settings.model.preferences.impl

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.EdS.DhizukuF.data.common.util.asFlow
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsRepoImpl : SettingsRepo, KoinComponent {
    private val context by inject<Context>()

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("dhizuku_settings", Context.MODE_PRIVATE)
    }

    override fun flowWhitelistMode(): Flow<Boolean> =
        prefs.asFlow("whitelist_mode", false)

    override fun flowDhizukuEnabled(): Flow<Boolean> =
        prefs.asFlow("dhizuku_enabled", true)

    override fun flowDonateButtonHidden(): Flow<Boolean> =
        prefs.asFlow("donate_button_hidden", false)

    override fun flowConfirmationWindow(): Flow<Boolean> =
        prefs.asFlow("confirmation_window", true)

    override var isConfirmationWindow: Boolean
        get() = prefs.getBoolean("confirmation_window", true)
        set(value) = prefs.edit(true) {
            putBoolean("confirmation_window", value)
        }

    override var isWhitelistMode: Boolean
        get() = prefs.getBoolean("whitelist_mode", false)
        set(value) = prefs.edit(true) {
            putBoolean("whitelist_mode", value)
        }

    override var isDhizukuEnabled: Boolean
        get() = prefs.getBoolean("dhizuku_enabled", true)
        set(value) = prefs.edit(true) {
            putBoolean("dhizuku_enabled", value)
        }

    override var isDonateButtonHidden: Boolean
        get() = prefs.getBoolean("donate_button_hidden", false)
        set(value) = prefs.edit(true) {
            putBoolean("donate_button_hidden", value)
        }
}