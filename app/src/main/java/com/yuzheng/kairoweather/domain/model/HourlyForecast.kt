package com.yuzheng.kairoweather.domain.model

import androidx.compose.runtime.Stable

@Stable
data class HourlyForecast(
    val time: String,
    val temperature: String,
    val iconCode: String,
    val pop: String,
    val isNow: Boolean
)