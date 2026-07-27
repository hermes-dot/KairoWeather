package com.yuzheng.kairoweather.ui.theme.weather

import com.yuzheng.kairoweather.domain.model.CurrentWeather
import com.yuzheng.kairoweather.domain.model.DailyForecast
import com.yuzheng.kairoweather.domain.model.HourlyForecast

data class WeatherUiState(
    val current: CurrentWeather? = null,
    val hourly: List<HourlyForecast> = emptyList(),
    val daily: List<DailyForecast> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sunProgress: Float = 0f,
    val locationName: String = "北京"
)
