package com.EdS.DhizukuF.ui.page.settings.security

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.EdS.DhizukuF.data.settings.repo.AuthMode
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SecurityViewModel : ViewModel(), KoinComponent {
    private val settingsRepo by inject<SettingsRepo>()

    var state by mutableStateOf(SecurityViewState())
        private set

    fun collect() {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowWhitelistMode().collect { state = state.copy(whitelistMode = it) }
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowDangerousEnabled().collect { state = state.copy(dangerousEnabled = it) }
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowAuthMode().collect { state = state.copy(authMode = it) }
        }
    }

    fun setWhitelistMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepo.isWhitelistMode = enabled }
    }

    fun setDangerousEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepo.isDangerousEnabled = enabled }
    }

    fun setAuthMode(mode: AuthMode) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepo.authMode = mode }
    }
}
