package com.EdS.DhizukuF.di

import com.EdS.DhizukuF.data.account.model.ShizukuUserService
import com.EdS.DhizukuF.data.account.repo.UserService
import com.EdS.DhizukuF.data.settings.model.preferences.impl.SettingsRepoImpl
import com.EdS.DhizukuF.data.settings.model.room.DhizukuRoom
import com.EdS.DhizukuF.data.settings.model.room.impl.AppRepoImpl
import com.EdS.DhizukuF.data.settings.repo.AppRepo
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo

import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

val dataModule = module {
    single {
        DhizukuRoom.createInstance()
    }

    single<AppRepo> {
        val room = get<DhizukuRoom>()
        AppRepoImpl(room.appDao)
    }

    single<SettingsRepo> {
        SettingsRepoImpl()
    }

    single<UserService> {
        ShizukuUserService(androidContext())
    }
}
