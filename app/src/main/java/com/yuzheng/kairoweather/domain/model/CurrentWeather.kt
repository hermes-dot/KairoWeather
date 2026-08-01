package com.yuzheng.kairoweather.domain.model

import androidx.compose.runtime.Stable

/**
 * 当前天气。
 *
 * 数值字段存原始数值(摄氏温度 / 百分比 / 百帕),格式化由 UI 层按 [TemperatureUnit] 完成,
 * 使单位切换(摄氏 ↔ 华氏)无需重拉数据即可在界面层重解释。
 */
@Stable
data class CurrentWeather(
    val tempCelsius: Double,
    val condition: String,
    val feelsLikeCelsius: Double,
    val humidityPct: Int,
    val wind: String,
    val windAngle: String,
    val windSpeedRaw: String,
    val windScale: String,
    val iconCode: String,
    val pressureHpa: Int
)
