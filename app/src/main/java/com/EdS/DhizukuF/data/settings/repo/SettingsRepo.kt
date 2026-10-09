package com.EdS.DhizukuF.data.settings.repo

import kotlinx.coroutines.flow.Flow

interface SettingsRepo {
    fun flowWhitelistMode(): Flow<Boolean>
    fun flowDhizukuEnabled(): Flow<Boolean>
    fun flowDonateButtonHidden(): Flow<Boolean>
    fun flowShowDishLabel(): Flow<Boolean>
    fun flowAdvancedConfirmation(): Flow<Boolean>
    var isWhitelistMode: Boolean
    var isDhizukuEnabled: Boolean
    var isDonateButtonHidden: Boolean
    var isShowDishLabel: Boolean
    var isAdvancedConfirmation: Boolean
}