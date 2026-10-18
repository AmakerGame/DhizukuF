package com.EdS.DhizukuF.ui.page.settings.activate

import android.app.admin.DeviceAdminInfo

data class ActivateViewData(
    val admin: DeviceAdminInfo,
    val enabled: Boolean
)