package com.EdS.DhizukuF.ui.page.settings.account_manager

import com.EdS.DhizukuF.data.account.entity.AccountAuthenticatorEntity
import com.EdS.DhizukuF.data.account.entity.AccountEntity

data class AccountManagerViewState(
    val authenticators: List<Authenticator> = emptyList(),
    val loading: Boolean = false
) {
    data class Authenticator(
        val auth: AccountAuthenticatorEntity,
        val accounts: List<AccountEntity>,
        val isFrozen: Boolean = false
    )
}