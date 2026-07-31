package com.yuzheng.kairoweather.domain.model

import androidx.compose.runtime.Stable

@Stable
data class CurrentWeather(
    val temperature: String,
    val condition: String,
    val feelLike: String,
    val humidity: String,
    val wind: String,
    val windAngle: String,
    val windSpeedRaw: String,
    val windScale: String,
    val iconCode: String,
    val pressure: String
)
