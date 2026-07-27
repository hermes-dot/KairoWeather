package com.yuzheng.kairoweather.domain.model

data class HourlyForecast(
    val time: String,
    val temperature: String,
    val iconCode: String,
    val iconUrl: String,
    val pop: String,
    val isNow: Boolean
)