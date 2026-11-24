package com.EdS.DhizukuF.ui.page.settings.security

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.customization.AppearanceManager
import com.EdS.DhizukuF.data.common.util.toast
import com.EdS.DhizukuF.data.settings.repo.AuthMode
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SecurityViewModel : ViewModel(), KoinComponent {
    private val settingsRepo by inject<SettingsRepo>()
    private val context by inject<Context>()

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
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowSecretCodeEnabled().collect { state = state.copy(secretCodeEnabled = it) }
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowAppHidden().collect { state = state.copy(appHidden = it) }
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

    /**
     * "Open with a secret code". Turning it off while the app is hidden shows the app first: without the code
     * nobody could open a hidden app again.
     */
    fun setSecretCodeEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!enabled && settingsRepo.isAppHidden) {
                settingsRepo.isAppHidden = false
                AppearanceManager.show(context, settingsRepo)
            }
            settingsRepo.isSecretCodeEnabled = enabled
        }
    }

    /** "Hide the app": only while the secret code is on. The flag is set before the icon goes, never after. */
    fun setAppHidden(hidden: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (hidden) {
                if (!settingsRepo.isSecretCodeEnabled) return@launch
                settingsRepo.isAppHidden = true
                if (!AppearanceManager.hide(context)) {
                    // The system did not take the icon away: do not claim that the app is hidden.
                    settingsRepo.isAppHidden = false
                    AppearanceManager.show(context, settingsRepo)
                    context.toast(R.string.hide_app_failed, android.widget.Toast.LENGTH_LONG)
                }
            } else {
                settingsRepo.isAppHidden = false
                AppearanceManager.show(context, settingsRepo)
            }
        }
    }
}
