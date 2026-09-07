package com.yuzheng.kairoweather.ui.weather

import androidx.compose.runtime.Stable
import com.yuzheng.kairoweather.domain.model.AirQuality
import com.yuzheng.kairoweather.domain.model.CurrentWeather
import com.yuzheng.kairoweather.domain.model.DailyForecast
import com.yuzheng.kairoweather.domain.model.HourlyForecast
import com.yuzheng.kairoweather.domain.model.TemperatureUnit

@Stable
data class WeatherUiState(
    val current: CurrentWeather? = null,
    val hourly: List<HourlyForecast> = emptyList(),
    val daily: List<DailyForecast> = emptyList(),
    /** 空气质量独立端点,失败静默(P2-6 语义延伸):null = 无数据,AQI 卡整卡不渲染 */
    val air: AirQuality? = null,
    val isLoading: Boolean = false,
    /** 整页错误信息,仅由 current 请求失败时写入(P2-6);hourly/daily 失败不写此字段 */
    val error: String? = null,
    val sunProgress: Float = 0f,
    val locationName: String = "北京",
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
)
