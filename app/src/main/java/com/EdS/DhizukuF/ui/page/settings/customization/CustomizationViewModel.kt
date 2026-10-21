package com.EdS.DhizukuF.ui.page.settings.customization

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.EdS.DhizukuF.customization.AppearanceConfig
import com.EdS.DhizukuF.customization.AppearanceManager
import com.EdS.DhizukuF.customization.CustomIcons
import com.EdS.DhizukuF.customization.IconChoice
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

    fun collect() {
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

    // ---------------------------------------------------------------- name

    fun changeName(name: String) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepo.appDisplayName = name }
    }

    fun changeRemember(on: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepo.rememberAppearance = on }
    }

    // ---------------------------------------------------------------- icon

    /** The DhizukuF icon or the icon of the original Dhizuku: the launcher entry is switched. */
    fun chooseBuiltIn(context: Context, choice: IconChoice, done: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.selectedIcon = choice
            runCatching { AppearanceManager.applyBuiltIn(context, choice) }
            withContext(Dispatchers.Main) { done() }
        }
    }

    /** Adds a picture of the user. Returns the id through [done] (null: the file is not a picture, or too many). */
    fun addIcon(context: Context, uri: Uri, done: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settingsRepo.customIconIds
            val id = if (current.size >= CustomIcons.MAX) null else AppearanceManager.addCustomIcon(context, uri)
            if (id != null) settingsRepo.customIconIds = current + id
            withContext(Dispatchers.Main) { done(id) }
        }
    }

    /**
     * Uses a picture of the user: it becomes the icon inside DhizukuF and a launcher shortcut is requested.
     * The normal launcher entry (the DhizukuF icon, unless the original one was chosen) stays.
     */
    fun chooseCustom(context: Context, id: String, done: (AppearanceManager.PinResult) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.selectedIcon = IconChoice.Custom(id)
            // the normal launcher entry is the DhizukuF icon; the picture is an extra shortcut
            runCatching { AppearanceManager.applyBuiltIn(context, IconChoice.Default) }
            val label = settingsRepo.appearance.shownName ?: "DhizukuF"
            val result = AppearanceManager.pinShortcut(context, id, label)
            withContext(Dispatchers.Main) { done(result) }
        }
    }

    /** Deletes a picture of the user. The built-in icons are not in this list and cannot be deleted. */
    fun deleteCustom(context: Context, id: String, disabledMessage: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (settingsRepo.selectedIcon == IconChoice.Custom(id)) settingsRepo.selectedIcon = IconChoice.Default
            settingsRepo.customIconIds = settingsRepo.customIconIds - id
            AppearanceManager.deleteCustomIcon(context, id, disabledMessage)
        }
    }
}
