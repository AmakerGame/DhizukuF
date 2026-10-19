package com.EdS.DhizukuF.data.settings.model.preferences.impl

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.EdS.DhizukuF.data.common.util.asFlow
import com.EdS.DhizukuF.data.common.util.devicePrefs
import com.EdS.DhizukuF.data.settings.repo.AuthMode
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import com.EdS.DhizukuF.logging.LogCategory
import com.EdS.DhizukuF.logging.LogConfig
import com.EdS.DhizukuF.logging.LogLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val ALL_CATEGORIES = LogCategory.entries.joinToString(",") { it.name }

class SettingsRepoImpl : SettingsRepo, KoinComponent {
    private val context by inject<Context>()

    // Any change of a setting drops the cached log configuration, so the next event reads it again.
    private val logListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        cachedLogConfig = null
    }

    @Volatile
    private var cachedLogConfig: LogConfig? = null

    private val prefs: SharedPreferences by lazy {
        context.devicePrefs("dhizuku_settings").also { it.registerOnSharedPreferenceChangeListener(logListener) }
    }

    override fun flowWhitelistMode(): Flow<Boolean> =
        prefs.asFlow("whitelist_mode", false)

    override fun flowDhizukuEnabled(): Flow<Boolean> =
        prefs.asFlow("dhizuku_enabled", true)

    override fun flowDangerousEnabled(): Flow<Boolean> =
        prefs.asFlow("dangerous_enabled", false)

    override fun flowDishEnabled(): Flow<Boolean> =
        prefs.asFlow("dish_enabled", true)

    override fun flowDonateButtonHidden(): Flow<Boolean> =
        prefs.asFlow("donate_button_hidden", false)

    override fun flowShowDishLabel(): Flow<Boolean> =
        prefs.asFlow("show_dish_label", true)

    override fun flowAdvancedConfirmation(): Flow<Boolean> =
        prefs.asFlow("advanced_confirmation", false)

    override fun flowAuthMode(): Flow<AuthMode> =
        prefs.asFlow("auth_mode", AuthMode.Off.name).map { AuthMode.parse(it) }

    override fun flowLogConfig(): Flow<LogConfig> = combine(
        prefs.asFlow("log_enabled", false),
        prefs.asFlow("log_level", LogLevel.Normal.name),
        prefs.asFlow("log_categories", ALL_CATEGORIES),
        prefs.asFlow("log_max_mb", LogConfig.DEFAULT_MAX_SIZE_MB)
    ) { enabled, level, categories, maxMb ->
        buildLogConfig(enabled, level, categories, maxMb)
    }

    override val logConfig: LogConfig
        get() = cachedLogConfig ?: buildLogConfig(
            prefs.getBoolean("log_enabled", false),
            prefs.getString("log_level", null),
            prefs.getString("log_categories", ALL_CATEGORIES),
            prefs.getInt("log_max_mb", LogConfig.DEFAULT_MAX_SIZE_MB)
        ).also { cachedLogConfig = it }

    private fun buildLogConfig(enabled: Boolean, level: String?, categories: String?, maxMb: Int) = LogConfig(
        enabled = enabled,
        level = LogLevel.entries.firstOrNull { it.name == level } ?: LogLevel.Normal,
        categories = (categories ?: ALL_CATEGORIES).split(',')
            .mapNotNull { name -> LogCategory.entries.firstOrNull { it.name == name } }
            .toSet(),
        maxSizeMb = maxMb.takeIf { it in LogConfig.SIZE_CHOICES_MB } ?: LogConfig.DEFAULT_MAX_SIZE_MB
    )

    override var isLogsEnabled: Boolean
        get() = prefs.getBoolean("log_enabled", false)
        set(value) = prefs.edit(true) { putBoolean("log_enabled", value) }

    override var logLevel: LogLevel
        get() = logConfig.level
        set(value) = prefs.edit(true) { putString("log_level", value.name) }

    override var logCategories: Set<LogCategory>
        get() = logConfig.categories
        set(value) = prefs.edit(true) { putString("log_categories", value.joinToString(",") { it.name }) }

    override var logMaxSizeMb: Int
        get() = logConfig.maxSizeMb
        set(value) = prefs.edit(true) { putInt("log_max_mb", value) }

    override var authMode: AuthMode
        get() = AuthMode.parse(prefs.getString("auth_mode", AuthMode.Off.name))
        set(value) = prefs.edit(true) {
            putString("auth_mode", value.name)
        }

    override var isShowDishLabel: Boolean
        get() = prefs.getBoolean("show_dish_label", true)
        set(value) = prefs.edit(true) {
            putBoolean("show_dish_label", value)
        }

    override var isAdvancedConfirmation: Boolean
        get() = prefs.getBoolean("advanced_confirmation", false)
        set(value) = prefs.edit(true) {
            putBoolean("advanced_confirmation", value)
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

    override var isDangerousEnabled: Boolean
        get() = prefs.getBoolean("dangerous_enabled", false)
        set(value) = prefs.edit(true) {
            putBoolean("dangerous_enabled", value)
        }

    override var isDishEnabled: Boolean
        get() = prefs.getBoolean("dish_enabled", true)
        set(value) = prefs.edit(true) {
            putBoolean("dish_enabled", value)
        }

    override var isDonateButtonHidden: Boolean
        get() = prefs.getBoolean("donate_button_hidden", false)
        set(value) = prefs.edit(true) {
            putBoolean("donate_button_hidden", value)
        }
}