package com.yuzheng.kairoweather.ui.theme.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuzheng.kairoweather.data.location.LocationTracker
import com.yuzheng.kairoweather.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val locationTracker: LocationTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var lastLocation: String = "116.41,39.92"

    fun loadFromCurrentLocation() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            locationTracker.getCurrentLocation()
                .onSuccess { loc ->
                    val coords = locationTracker.formatLocation(loc)
                    lastLocation = coords
                    resolveLocationName(coords)
                    loadWeatherInternal(coords)
                }
                .onFailure {
                    _uiState.update { s -> s.copy(isLoading = false, error = "定位失败：${it.message}") }
                }
        }
    }

    private suspend fun resolveLocationName(coords: String) {
        repository.reverseGeocode(coords)
            .onSuccess { name -> _uiState.update { it.copy(locationName = name) } }
    }

    fun loadingWeather(location: String) {
        if (_uiState.value.isLoading) return
        lastLocation = location
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            loadWeatherInternal(location)
        }
    }

    fun refresh() {
        loadingWeather(lastLocation)
    }

    private suspend fun loadWeatherInternal(location: String) = coroutineScope {
        val currentJob = launch { loadCurrent(location) }
        val hourlyJob = launch { loadHourly(location) }
        val dailyJob = launch { loadDaily(location) }

        currentJob.join()
        hourlyJob.join()
        dailyJob.join()

        _uiState.update { it.copy(isLoading = false) }
    }

    private suspend fun loadCurrent(location: String) {
        repository.getCurrentWeather(location)
            .onSuccess { _uiState.update { s -> s.copy(current = it) } }
            .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
    }

    private suspend fun loadHourly(location: String) {
        repository.getHourlyForecast(location)
            .onSuccess { _uiState.update { s -> s.copy(hourly = it) } }
    }

    private suspend fun loadDaily(location: String) {
        repository.getDailyForecast(location)
            .onSuccess { days ->
                _uiState.update { s ->
                    s.copy(
                        daily = days,
                        sunProgress = calcSunProgress(
                            days.firstOrNull()?.sunrise,
                            days.firstOrNull()?.sunset
                        )
                    )
                }
            }
    }

    private fun calcSunProgress(rise: String?, set: String?): Float {
        if (rise.isNullOrEmpty() || set.isNullOrEmpty()) return 0f
        val r = runCatching { LocalTime.parse(rise) }.getOrNull() ?: return 0f
        val s = runCatching { LocalTime.parse(set) }.getOrNull() ?: return 0f
        val total = s.toSecondOfDay() - r.toSecondOfDay()
        if (total <= 0) return 0f
        val elapsed = LocalTime.now().toSecondOfDay() - r.toSecondOfDay()
        return (elapsed.toFloat() / total).coerceIn(0f, 1f)
    }
}
