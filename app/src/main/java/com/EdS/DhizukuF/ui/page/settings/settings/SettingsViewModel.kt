package com.EdS.DhizukuF.ui.page.settings.settings

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

    fun setDhizukuEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.isDhizukuEnabled = enabled
        }
    }
}