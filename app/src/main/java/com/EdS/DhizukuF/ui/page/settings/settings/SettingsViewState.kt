package com.EdS.DhizukuF.ui.page.settings.settings

data class SettingsViewState(
    val whitelistMode: Boolean = false,
    val dhizukuEnabled: Boolean = true,
    val dishEnabled: Boolean = true,
    val showDishLabel: Boolean = true,
    val advancedConfirmation: Boolean = false
)