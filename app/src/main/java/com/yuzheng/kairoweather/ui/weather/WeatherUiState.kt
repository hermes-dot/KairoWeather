package com.yuzheng.kairoweather.ui.weather

import androidx.compose.runtime.Stable
import com.yuzheng.kairoweather.domain.model.CurrentWeather
import com.yuzheng.kairoweather.domain.model.DailyForecast
import com.yuzheng.kairoweather.domain.model.HourlyForecast
import com.yuzheng.kairoweather.domain.model.TemperatureUnit

@Stable
data class WeatherUiState(
    val current: CurrentWeather? = null,
    val hourly: List<HourlyForecast> = emptyList(),
    val daily: List<DailyForecast> = emptyList(),
    val isLoading: Boolean = false,
    /** 整页错误信息,仅由 current 请求失败时写入(P2-6);hourly/daily 失败不写此字段 */
    val error: String? = null,
    val sunProgress: Float = 0f,
    val locationName: String = "北京",
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
)
