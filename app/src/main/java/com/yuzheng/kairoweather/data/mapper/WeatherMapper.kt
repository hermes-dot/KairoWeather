package com.yuzheng.kairoweather.data.mapper

import com.yuzheng.kairoweather.data.model.QWeatherNow
import com.yuzheng.kairoweather.data.model.QWeatherHourly
import com.yuzheng.kairoweather.data.model.QWeatherDaily
import com.yuzheng.kairoweather.domain.model.CurrentWeather
import com.yuzheng.kairoweather.domain.model.DailyForecast
import com.yuzheng.kairoweather.domain.model.HourlyForecast
import com.yuzheng.kairoweather.domain.model.MoonPhase

fun QWeatherNow.toDomain(): CurrentWeather = CurrentWeather(
    temperature = "${temp}°",
    condition = text,
    feelLike = "体感${feelsLike}°",
    humidity = "湿度$humidity%",
    uvi = "",
    wind = "$windDir ${windScale}级",
    windAngle = wind360,
    windSpeedRaw = windSpeed,
    windScale = windScale,
    iconCode = icon,
    iconUrl = icon.toQWeatherIconUrl(),
    pressure = "气压${pressure}hPa"
)

fun QWeatherHourly.toDomain(): HourlyForecast {
    val time = fxTime.substringAfter("T").substringBefore("+")
    return HourlyForecast(
        time = time,
        temperature = "${temp}°",
        iconCode = icon,
        iconUrl = icon.toQWeatherIconUrl(),
        pop = "${pop}%",
        isNow = false
    )
}

fun QWeatherDaily.toDomain(index: Int): DailyForecast = DailyForecast(
    date = if (index == 0) "今天" else formatFxDate(fxDate),
    highTemp = "${tempMax}°",
    lowTemp = "${tempMin}°",
    iconCode = iconDay,
    iconUrl = iconDay.toQWeatherIconUrl(),
    description = textDay,
    pop = "0%", // 日预报无降水概率，用总降水量 precip 替代判断
    moonPhase = moonPhase.toMoonPhase(),
    sunrise = sunrise,
    sunset = sunset
)

private fun String.toQWeatherIconUrl(): String {
    if (isEmpty()) return ""
    return "https://icons.qweather.com/assets/icons/$this.png"
}

private fun formatFxDate(fxDate: String): String {
    if (fxDate.length < 10) return fxDate
    // "2021-11-15" → "周一 11/15"
    val week = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
    val parts = fxDate.split("-")
    val year = parts[0].toInt()
    val month = parts[1].toInt()
    val day = parts[2].toInt()
    val dayOfWeek = java.time.LocalDate.of(year, month, day).dayOfWeek.value % 7
    return "${week[dayOfWeek]} $month/$day"
}

private fun String.toMoonPhase(): MoonPhase = when (this) {
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
