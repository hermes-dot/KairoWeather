package com.yuzheng.kairoweather.domain.model

import androidx.compose.runtime.Stable

@Stable
data class DailyForecast(
    val date: String,
    val highTemp: String,
    val lowTemp: String,
    val iconCode: String,
    val description: String,
    val pop: String,
    val uvIndex: String,
    val moonPhase: MoonPhase,
    val sunrise: String,
    val sunset: String
)

@Stable
enum class MoonPhase(val label: String, val emoji: String) {
    NEW_MOON("新月", "🌑"),
    WAXING_CRESCENT("蛾眉月", "🌒"),
    FIRST_QUARTER("上弦月", "🌓"),
    WAXING_GIBBOUS("盈凸月", "🌔"),
    FULL_MOON("满月", "🌕"),
    WANING_GIBBOUS("亏凸月", "🌖"),
    LAST_QUARTER("下弦月", "🌗"),
    WANING_CRESCENT("残月", "🌘")
}
