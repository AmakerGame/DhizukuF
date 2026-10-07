package com.EdS.DhizukuF.di

import com.EdS.DhizukuF.ui.page.settings.activate.ActivateViewModel
import com.EdS.DhizukuF.ui.page.settings.app_management.AppManagementViewModel
import com.EdS.DhizukuF.ui.page.settings.account_manager.AccountManagerViewModel
import com.EdS.DhizukuF.ui.page.settings.settings.SettingsViewModel
import com.EdS.DhizukuF.ui.page.settings.user_manager.UserManagerViewModel

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        AppManagementViewModel()
    }
    viewModel {
        ActivateViewModel()
    }
    viewModel {
        SettingsViewModel()
    }
    viewModel {
        UserManagerViewModel()
    }
    viewModel { parameters ->
        AccountManagerViewModel(parameters.get())
    }
}
