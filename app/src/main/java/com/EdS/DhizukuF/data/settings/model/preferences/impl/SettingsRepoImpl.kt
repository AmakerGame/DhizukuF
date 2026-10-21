package com.EdS.DhizukuF.data.settings.model.preferences.impl

import com.EdS.DhizukuF.customization.ThemeMode

import com.EdS.DhizukuF.customization.ThemeConfig

import com.EdS.DhizukuF.customization.IconChoice

import com.EdS.DhizukuF.customization.HomeLayout

import com.EdS.DhizukuF.customization.CustomIcons

import com.EdS.DhizukuF.customization.AppearanceConfig

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

    // ---------------------------------------------------------------- Customization

    private fun parseTheme(name: String?) = ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.System

    private fun parseList(text: String?): List<String> =
        (text ?: "").split(',').map { it.trim() }.filter { it.isNotEmpty() }

    override fun flowThemeConfig(): Flow<ThemeConfig> = combine(
        prefs.asFlow("theme_mode", ThemeMode.System.name),
        prefs.asFlow("theme_dynamic", true)
    ) { mode, dynamic -> ThemeConfig(parseTheme(mode), dynamic) }

    override val themeConfig: ThemeConfig
        get() = ThemeConfig(parseTheme(prefs.getString("theme_mode", null)), prefs.getBoolean("theme_dynamic", true))

    override var themeMode: ThemeMode
        get() = themeConfig.mode
        set(value) = prefs.edit(true) { putString("theme_mode", value.name) }

    override var dynamicColors: Boolean
        get() = prefs.getBoolean("theme_dynamic", true)
        set(value) = prefs.edit(true) { putBoolean("theme_dynamic", value) }

    private fun buildAppearance(name: String?, icon: String?, remember: Boolean, icons: String?) = AppearanceConfig(
        displayName = name ?: "",
        icon = IconChoice.parse(icon),
        rememberForUpdates = remember,
        customIcons = parseList(icons).filter { CustomIcons.isValidId(it) }.distinct().take(CustomIcons.MAX)
    )

    override fun flowAppearance(): Flow<AppearanceConfig> = combine(
        prefs.asFlow("app_name", ""),
        prefs.asFlow("app_icon", IconChoice.Default.key),
        prefs.asFlow("appearance_remember", true),
        prefs.asFlow("custom_icons", "")
    ) { name, icon, remember, icons -> buildAppearance(name, icon, remember, icons) }

    override val appearance: AppearanceConfig
        get() = buildAppearance(
            prefs.getString("app_name", null),
            prefs.getString("app_icon", null),
            prefs.getBoolean("appearance_remember", true),
            prefs.getString("custom_icons", null)
        )

    override var appDisplayName: String
        get() = appearance.displayName
        set(value) = prefs.edit(true) { putString("app_name", value.take(40)) }

    override var selectedIcon: IconChoice
        get() = appearance.icon
        set(value) = prefs.edit(true) { putString("app_icon", value.key) }

    override var rememberAppearance: Boolean
        get() = prefs.getBoolean("appearance_remember", true)
        set(value) = prefs.edit(true) { putBoolean("appearance_remember", value) }

    override var customIconIds: List<String>
        get() = appearance.customIcons
        set(value) = prefs.edit(true) { putString("custom_icons", value.joinToString(",")) }

    override var appearanceVersion: Int
        get() = prefs.getInt("appearance_version", 0)
        set(value) = prefs.edit(true) { putInt("appearance_version", value) }

    override fun flowHomeLayout(): Flow<HomeLayout> = combine(
        prefs.asFlow("home_order", ""),
        prefs.asFlow("home_hidden", "")
    ) { order, hidden -> HomeLayout(parseList(order), parseList(hidden).toSet()) }

    override val homeLayout: HomeLayout
        get() = HomeLayout(parseList(prefs.getString("home_order", null)), parseList(prefs.getString("home_hidden", null)).toSet())

    override var homeOrder: List<String>
        get() = homeLayout.order
        set(value) = prefs.edit(true) { putString("home_order", value.joinToString(",")) }

    override var homeHidden: Set<String>
        get() = homeLayout.hidden
        set(value) = prefs.edit(true) { putString("home_hidden", value.joinToString(",")) }

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