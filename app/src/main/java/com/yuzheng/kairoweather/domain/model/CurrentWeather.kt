package com.yuzheng.kairoweather.domain.model

data class CurrentWeather(
    val temperature: String,
    val condition: String,
    val feelLike: String,
    val humidity: String,
    val uvi: String,
    val wind: String,
    val windAngle: String,
    val windSpeedRaw: String,
    val windScale: String,
    val iconCode: String,
    val iconUrl: String,
    val pressure: String
)
