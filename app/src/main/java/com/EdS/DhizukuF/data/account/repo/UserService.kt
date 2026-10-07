package com.EdS.DhizukuF.data.account.repo

import com.EdS.DhizukuF.data.account.entity.AccountAuthenticatorEntity
import com.EdS.DhizukuF.data.account.entity.AccountEntity
import com.EdS.DhizukuF.data.account.entity.UserEntity

interface UserService {
    suspend fun removeUser(user: UserEntity): Boolean

    suspend fun removeUser(userId: Int): Boolean

    suspend fun getUsers(): List<UserEntity>

    suspend fun getAccountAuthenticators(userId: Int): List<AccountAuthenticatorEntity>

    suspend fun getAccounts(userId: Int): List<AccountEntity>

    suspend fun removeAccount(account: AccountEntity): Boolean

    suspend fun setPackageEnabled(packageName: String, enabled: Boolean, userId: Int): Boolean

    suspend fun isPackageEnabled(packageName: String, userId: Int): Boolean
}
