package com.EdS.DhizukuF.data.settings.repo

import com.EdS.DhizukuF.customization.ThemeMode

import com.EdS.DhizukuF.customization.ThemeConfig

import com.EdS.DhizukuF.customization.IconChoice

import com.EdS.DhizukuF.customization.HomeLayout

import com.EdS.DhizukuF.customization.AppName
import com.EdS.DhizukuF.customization.AppearanceConfig

import com.EdS.DhizukuF.logging.LogCategory
import com.EdS.DhizukuF.logging.LogConfig
import com.EdS.DhizukuF.logging.LogLevel
import kotlinx.coroutines.flow.Flow

interface SettingsRepo {
    fun flowWhitelistMode(): Flow<Boolean>
    fun flowDhizukuEnabled(): Flow<Boolean>
    fun flowDishEnabled(): Flow<Boolean>
    fun flowDangerousEnabled(): Flow<Boolean>
    fun flowDonateButtonHidden(): Flow<Boolean>
    fun flowDaemonNotificationEnabled(): Flow<Boolean>
    fun flowShowDishLabel(): Flow<Boolean>
    fun flowAdvancedConfirmation(): Flow<Boolean>
    fun flowAuthMode(): Flow<AuthMode>

    /** Settings > Logs. [logConfig] is cached and cheap: it is read for every event that could be logged. */
    fun flowLogConfig(): Flow<LogConfig>
    val logConfig: LogConfig
    var isLogsEnabled: Boolean
    var logLevel: LogLevel
    var logCategories: Set<LogCategory>
    var logMaxSizeMb: Int

    /** Settings > Customization. */
    fun flowThemeConfig(): Flow<ThemeConfig>
    val themeConfig: ThemeConfig
    var themeMode: ThemeMode
    var dynamicColors: Boolean

    fun flowAppearance(): Flow<AppearanceConfig>
    val appearance: AppearanceConfig
    var appName: AppName
    var selectedIcon: IconChoice
    var rememberAppearance: Boolean

    /** The versionCode of DhizukuF at which the name and the icon were last put in place (see AppearanceManager). */
    var appearanceVersion: Int

    /** The order of the cards of the home screen and the hidden ones (Home > pencil). */
    fun flowHomeLayout(): Flow<HomeLayout>
    val homeLayout: HomeLayout
    var homeOrder: List<String>
    var homeHidden: Set<String>
    var isWhitelistMode: Boolean
    var isDhizukuEnabled: Boolean
    var isDishEnabled: Boolean
    var isDangerousEnabled: Boolean
    var isDonateButtonHidden: Boolean
    /** Persistent FOREGROUND service notification; Android still requires a minimal one when off. */
    var isDaemonNotificationEnabled: Boolean
    var isShowDishLabel: Boolean
    var isAdvancedConfirmation: Boolean
    var authMode: AuthMode
}