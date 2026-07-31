package com.yuzheng.kairoweather.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuzheng.kairoweather.data.location.LocationTracker
import com.yuzheng.kairoweather.data.preferences.UserPreferences
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
    preferences: UserPreferences,
) : ViewModel() {
    companion object {
        /** 默认城市坐标（北京），权限被拒绝或定位失败时使用 */
        const val DEFAULT_LOCATION = "116.41,39.92"
    }

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var lastLocation: String = DEFAULT_LOCATION

    init {
        viewModelScope.launch {
            preferences.temperatureUnit.collect { unit ->
                _uiState.update { it.copy(temperatureUnit = unit) }
            }
        }
    }

    private val currentUnit: String get() = _uiState.value.temperatureUnit

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
                    // 定位失败时回退到默认城市，保证页面可用
                    lastLocation = DEFAULT_LOCATION
                    resolveLocationName(DEFAULT_LOCATION)
                    loadWeatherInternal(DEFAULT_LOCATION)
                }
        }
    }

    fun loadingWeather(location: String) {
        if (_uiState.value.isLoading) return
        lastLocation = location
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            loadWeatherInternal(location)
        }
    }

    fun refresh() = loadingWeather(lastLocation)

    private suspend fun resolveLocationName(coords: String) {
        repository.reverseGeocode(coords)
            .onSuccess { name -> _uiState.update { it.copy(locationName = name) } }
    }

    private suspend fun loadWeatherInternal(location: String) = coroutineScope {
        val unit = currentUnit
        val currentJob = launch { loadCurrent(location, unit) }
        val hourlyJob = launch { loadHourly(location, unit) }
        val dailyJob = launch { loadDaily(location, unit) }

        currentJob.join(); hourlyJob.join(); dailyJob.join()
        _uiState.update { it.copy(isLoading = false) }
    }

    private suspend fun loadCurrent(location: String, unit: String) {
        repository.getCurrentWeather(location, unit)
            .onSuccess { _uiState.update { s -> s.copy(current = it) } }
            .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
    }

    private suspend fun loadHourly(location: String, unit: String) {
        repository.getHourlyForecast(location, unit)
            .onSuccess { hours ->
                val currentHour = LocalTime.now().hour
                _uiState.update { s ->
                    s.copy(
                        hourly = hours.map { hour ->
                            hour.copy(isNow = hour.time.take(2).toIntOrNull() == currentHour)
                        }
                    )
                }
            }
    }

    private suspend fun loadDaily(location: String, unit: String) {
        repository.getDailyForecast(location, unit)
            .onSuccess { days ->
                _uiState.update { s ->
                    s.copy(
                        daily = days,
                        sunProgress = calcSunProgress(days.firstOrNull()?.sunrise, days.firstOrNull()?.sunset)
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
