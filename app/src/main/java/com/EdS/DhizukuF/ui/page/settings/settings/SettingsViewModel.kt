package com.EdS.DhizukuF.ui.page.settings.settings

import com.EdS.DhizukuF.logging.LogManager

import com.EdS.DhizukuF.logging.LogEvent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.EdS.DhizukuF.data.settings.repo.SettingsRepo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsViewModel : ViewModel(), KoinComponent {
    private val settingsRepo by inject<SettingsRepo>()

    var state by mutableStateOf(SettingsViewState())
        private set

    fun collect() {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowWhitelistMode().collect { whitelistMode ->
                state = state.copy(whitelistMode = whitelistMode)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowShowDishLabel().collect { showDishLabel ->
                state = state.copy(showDishLabel = showDishLabel)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowAdvancedConfirmation().collect { advancedConfirmation ->
                state = state.copy(advancedConfirmation = advancedConfirmation)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowDangerousEnabled().collect { dangerousEnabled ->
                state = state.copy(dangerousEnabled = dangerousEnabled)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowDishEnabled().collect { dishEnabled ->
                state = state.copy(dishEnabled = dishEnabled)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowDhizukuEnabled().collect { dhizukuEnabled ->
                state = state.copy(dhizukuEnabled = dhizukuEnabled)
            }
        }
    }

    fun setShowDishLabel(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.isShowDishLabel = enabled
        }
    }

    fun setAdvancedConfirmation(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.isAdvancedConfirmation = enabled
        }
    }

    fun setWhitelistMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.isWhitelistMode = enabled
        }
    }

    fun setDangerousEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.isDangerousEnabled = enabled
        }
    }

    fun setDishEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.isDishEnabled = enabled
            LogManager.log(LogEvent.ConnectionChanged, message = "dish terminal access switched " + if (enabled) "on" else "off")
        }
    }

    fun setDhizukuEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.isDhizukuEnabled = enabled
            LogManager.log(LogEvent.ConnectionChanged, message = "Dhizuku API access switched " + if (enabled) "on" else "off")
        }
    }
}