package com.yuzheng.kairoweather.data.mapper

import com.yuzheng.kairoweather.data.model.QWeatherNow
import com.yuzheng.kairoweather.data.model.QWeatherHourly
import com.yuzheng.kairoweather.data.model.QWeatherDaily
import com.yuzheng.kairoweather.domain.model.CurrentWeather
import com.yuzheng.kairoweather.domain.model.DailyForecast
import com.yuzheng.kairoweather.domain.model.HourlyForecast
import com.yuzheng.kairoweather.domain.model.MoonPhase

fun QWeatherNow.toDomain(unit: String): CurrentWeather = CurrentWeather(
    temperature = formatTemp(temp, unit),
    condition = text,
    feelLike = "体感${formatTemp(feelsLike, unit)}",
    humidity = "湿度$humidity%",
    wind = "$windDir ${windScale}级",
    windAngle = wind360,
    windSpeedRaw = windSpeed,
    windScale = windScale,
    iconCode = icon,
    pressure = "气压${pressure}hPa"
)

fun QWeatherHourly.toDomain(unit: String): HourlyForecast {
    val time = fxTime.substringAfter("T").substringBefore("+")
    return HourlyForecast(
        time = time,
        temperature = formatTemp(temp, unit),
        iconCode = icon,
        pop = "${pop}%",
        isNow = false
    )
}

fun QWeatherDaily.toDomain(index: Int, unit: String): DailyForecast = DailyForecast(
    date = if (index == 0) "今天" else formatFxDate(fxDate),
    highTemp = formatTemp(tempMax, unit),
    lowTemp = formatTemp(tempMin, unit),
    iconCode = iconDay,
    description = textDay,
    pop = formatPop(pop),
    uvIndex = uvIndex,
    moonPhase = moonPhase.toMoonPhase(),
    sunrise = sunrise,
    sunset = sunset
)

internal fun formatTemp(celsiusStr: String, unit: String): String {
    val celsius = celsiusStr.toDoubleOrNull() ?: return "$celsiusStr°"
    return if (unit == "fahrenheit") {
        "${(celsius * 9 / 5 + 32).toInt()}°"
    } else {
        "${celsius.toInt()}°"
    }
}

internal fun formatFxDate(fxDate: String): String {
    if (fxDate.length < 10) return fxDate
    val week = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
    val parts = fxDate.split("-")
    val year = parts[0].toInt()
    val month = parts[1].toInt()
    val day = parts[2].toInt()
    val dayOfWeek = java.time.LocalDate.of(year, month, day).dayOfWeek.value % 7
    return "${week[dayOfWeek]} $month/$day"
}

internal fun String.toMoonPhase(): MoonPhase = when (this) {
    "新月" -> MoonPhase.NEW_MOON
    "蛾眉月" -> MoonPhase.WAXING_CRESCENT
    "上弦月" -> MoonPhase.FIRST_QUARTER
    "盈凸月" -> MoonPhase.WAXING_GIBBOUS
    "满月" -> MoonPhase.FULL_MOON
    "亏凸月" -> MoonPhase.WANING_GIBBOUS
    "下弦月" -> MoonPhase.LAST_QUARTER
    "残月" -> MoonPhase.WANING_CRESCENT
    else -> MoonPhase.NEW_MOON
}

private fun formatPop(pop: String): String {
    val value = pop.toIntOrNull() ?: return "0%"
    return if (value > 0) "$value%" else "0%"
}
