package com.EdS.DhizukuF.ui.page.settings.app_management

data class AppManagementViewState(
    val data: List<AppManagementViewData> = emptyList(),
    val loading: Boolean = true,
)