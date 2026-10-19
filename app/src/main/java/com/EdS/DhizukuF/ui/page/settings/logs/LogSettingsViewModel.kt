package com.EdS.DhizukuF.ui.page.settings.logs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import com.EdS.DhizukuF.logging.LogCategory
import com.EdS.DhizukuF.logging.LogConfig
import com.EdS.DhizukuF.logging.LogLevel
import com.EdS.DhizukuF.logging.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LogSettingsViewModel : ViewModel(), KoinComponent {
    private val settingsRepo by inject<SettingsRepo>()

    var config by mutableStateOf(LogConfig())
        private set

    var usedBytes by mutableStateOf(0L)
        private set

    fun collect() {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.flowLogConfig().collect { config = it }
        }
        viewModelScope.launch(Dispatchers.IO) {
            usedBytes = LogManager.sizeBytes()
            LogManager.changes.collect { usedBytes = LogManager.sizeBytes() }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepo.isLogsEnabled = enabled }
    }

    fun setLevel(level: LogLevel) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepo.logLevel = level }
    }

    fun setCategory(category: LogCategory, on: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settingsRepo.logCategories
            settingsRepo.logCategories = if (on) current + category else current - category
        }
    }

    fun setMaxSize(mb: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.logMaxSizeMb = mb
            // a smaller limit is applied at once, not at the next event
            LogManager.enforceLimit()
        }
    }

    fun clear() {
        viewModelScope.launch(Dispatchers.IO) { LogManager.clear() }
    }
}
