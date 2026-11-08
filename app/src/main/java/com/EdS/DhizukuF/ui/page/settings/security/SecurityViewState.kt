package com.EdS.DhizukuF.ui.page.settings.security

import com.EdS.DhizukuF.data.settings.repo.AuthMode

data class SecurityViewState(
    val whitelistMode: Boolean = false,
    val dangerousEnabled: Boolean = false,
    val authMode: AuthMode = AuthMode.Off
)
