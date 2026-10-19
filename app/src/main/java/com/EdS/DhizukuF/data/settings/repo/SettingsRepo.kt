package com.EdS.DhizukuF.data.settings.repo

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
    var isWhitelistMode: Boolean
    var isDhizukuEnabled: Boolean
    var isDishEnabled: Boolean
    var isDangerousEnabled: Boolean
    var isDonateButtonHidden: Boolean
    var isShowDishLabel: Boolean
    var isAdvancedConfirmation: Boolean
    var authMode: AuthMode
}