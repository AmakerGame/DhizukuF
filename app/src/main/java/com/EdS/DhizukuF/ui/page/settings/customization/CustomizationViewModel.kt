package com.EdS.DhizukuF.ui.page.settings.customization

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.EdS.DhizukuF.customization.AppName
import com.EdS.DhizukuF.customization.AppearanceConfig
import com.EdS.DhizukuF.customization.AppearanceManager
import com.EdS.DhizukuF.customization.IconChoice
import com.EdS.DhizukuF.customization.LauncherEntry
import com.EdS.DhizukuF.customization.ThemeConfig
import com.EdS.DhizukuF.customization.ThemeMode
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CustomizationViewModel : ViewModel(), KoinComponent {
    private val settingsRepo by inject<SettingsRepo>()

    var theme by mutableStateOf(ThemeConfig())
        private set

    var appearance by mutableStateOf(AppearanceConfig())
        private set

    private var collecting = false

    fun collect() {
        if (collecting) return // the screen can call this again (rotation); one collector is enough
        collecting = true
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowThemeConfig().collect { theme = it }
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowAppearance().collect { appearance = it }
        }
    }

    // ---------------------------------------------------------------- theme

    fun changeThemeMode(mode: ThemeMode) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepo.themeMode = mode }
    }

    fun changeDynamicColors(on: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepo.dynamicColors = on }
    }

    fun changeRemember(on: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepo.rememberAppearance = on }
    }

    // ---------------------------------------------------------------- name and icon (independent of each other)

    /** The name that the launcher shows. The icon that is chosen stays as it is. */
    fun chooseName(context: Context, name: AppName, done: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.appName = name
            val switched = AppearanceManager.apply(context, LauncherEntry(name, settingsRepo.selectedIcon))
            withContext(Dispatchers.Main) { done(switched) }
        }
    }

    /** The icon: the new one or the one of the original Dhizuku. The name that is chosen stays as it is. */
    fun chooseIcon(context: Context, icon: IconChoice, done: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.selectedIcon = icon
            val switched = AppearanceManager.apply(context, LauncherEntry(settingsRepo.appName, icon))
            withContext(Dispatchers.Main) { done(switched) }
        }
    }
}
