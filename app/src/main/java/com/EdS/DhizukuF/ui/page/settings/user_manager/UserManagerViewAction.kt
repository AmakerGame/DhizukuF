package com.EdS.DhizukuF.ui.page.settings.user_manager

import com.EdS.DhizukuF.data.account.entity.UserEntity

sealed class UserManagerViewAction {
    object Load : UserManagerViewAction()

    data class Remove(val user: UserEntity) : UserManagerViewAction()
}