package com.yuzheng.kairoweather.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuzheng.kairoweather.data.preferences.UserPreferences
import com.yuzheng.kairoweather.domain.model.TemperatureUnit
import com.yuzheng.kairoweather.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
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

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { preferences.setTemperatureUnit(unit) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }
}
