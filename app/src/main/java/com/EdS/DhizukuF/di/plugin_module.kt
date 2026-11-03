package com.EdS.DhizukuF.di

import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import com.EdS.DhizukuF.plugin.loader.PluginLoader
import com.EdS.DhizukuF.plugin.manager.PluginManager
import com.EdS.DhizukuF.server.DhizukuState
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val pluginModule = module {
    single { PluginLoader(androidContext()) }
    single {
        PluginManager(
            context = androidContext(),
            loader = get(),
            isDangerousCommandsEnabled = { get<SettingsRepo>().isDangerousEnabled },
            isOwner = { DhizukuState.state.isOwner }
        )
    }
}
