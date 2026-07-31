package com.yuzheng.kairoweather.data.model

import kotlinx.serialization.Serializable

@Serializable
data class QWeatherDailyResponse(
    val code: String,
    val updateTime: String = "",
    val daily: List<QWeatherDaily> = emptyList()
)

@Serializable
data class QWeatherDaily(
    val fxDate: String = "",
    val sunrise: String = "",
    val sunset: String = "",
    val moonrise: String = "",
    val moonset: String = "",
    val moonPhase: String = "",
    val tempMax: String = "0",
    val tempMin: String = "0",
    val iconDay: String = "",
    val textDay: String = "",
    val iconNight: String = "",
    val textNight: String = "",
    val windDirDay: String = "",
    val windScaleDay: String = "0",
    val windSpeedDay: String = "0",
    val windDirNight: String = "",
    val windScaleNight: String = "0",
    val windSpeedNight: String = "0",
    val humidity: String = "0",
    val precip: String = "0.0",
    val pressure: String = "0",
    val vis: String = "0",
    val cloud: String = "0",
    val uvIndex: String = "0",
    val pop: String = "0",
)
