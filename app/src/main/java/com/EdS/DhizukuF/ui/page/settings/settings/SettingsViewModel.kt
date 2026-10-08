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
            settingsRepo.flowDhizukuEnabled().collect { dhizukuEnabled ->
                state = state.copy(dhizukuEnabled = dhizukuEnabled)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowConfirmationDialog().collect { value ->
                state = state.copy(confirmationDialog = value)
            }
        }
    }

    fun setConfirmationDialog(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.isConfirmationDialog = enabled
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