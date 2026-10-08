package com.EdS.DhizukuF.ui.page.settings.app_management

import android.content.pm.ApplicationInfo

data class AppManagementViewData(
    val applicationInfo: ApplicationInfo,
    val label: String,
    val enabled: Boolean,
    val blocked: Boolean
)
