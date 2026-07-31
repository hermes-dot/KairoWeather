package com.yuzheng.kairoweather.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuzheng.kairoweather.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val temperatureUnit: String = "celsius",
    val themeMode: String = "system",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferences,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.temperatureUnit,
        preferences.themeMode,
    ) { unit, mode ->
        SettingsUiState(temperatureUnit = unit, themeMode = mode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setTemperatureUnit(unit: String) {
        viewModelScope.launch { preferences.setTemperatureUnit(unit) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }
}
