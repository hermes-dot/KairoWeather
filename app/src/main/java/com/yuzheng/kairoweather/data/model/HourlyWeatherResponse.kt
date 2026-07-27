package com.yuzheng.kairoweather.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QWeatherHourlyResponse(
    val code: String,
    val updateTime: String = "",
    val hourly: List<QWeatherHourly> = emptyList()
)

@Serializable
data class QWeatherHourly(
    val fxTime: String = "",
    val temp: String = "0",
    val icon: String = "",
    val text: String = "",
    val wind360: String = "0",
    val windDir: String = "",
    val windScale: String = "0",
    val windSpeed: String = "0",
    val humidity: String = "0",
    val pop: String = "0",
    val precip: String = "0.0",
    val pressure: String = "0",
    val cloud: String = "0",
    val dew: String = "0"
)
