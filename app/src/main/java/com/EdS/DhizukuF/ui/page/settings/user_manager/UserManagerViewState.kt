package com.EdS.DhizukuF.ui.page.settings.user_manager

import com.EdS.DhizukuF.data.account.entity.UserEntity

data class UserManagerViewState(
    val users: List<UserEntity> = emptyList(),
    val cause: Throwable? = null,
    val loading: Boolean = false
)