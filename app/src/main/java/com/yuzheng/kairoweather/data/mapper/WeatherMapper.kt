package com.yuzheng.kairoweather.data.mapper

import com.yuzheng.kairoweather.data.model.QWeatherNow
import com.yuzheng.kairoweather.data.model.QWeatherHourly
import com.yuzheng.kairoweather.data.model.QWeatherDaily
import com.yuzheng.kairoweather.domain.model.CurrentWeather
import com.yuzheng.kairoweather.domain.model.DailyForecast
import com.yuzheng.kairoweather.domain.model.HourlyForecast
import com.yuzheng.kairoweather.domain.model.MoonPhase

/**
 * DTO → domain 转换:只做数值解析,不做单位换算与文案格式化。
 * 温度/百分比/气压/紫外线均以原始数值存入 domain 模型,单位与文案由 UI 层按 TemperatureUnit 处理。
 */
fun QWeatherNow.toDomain(): CurrentWeather = CurrentWeather(
    tempCelsius = temp.toDoubleOrNull() ?: 0.0,
    condition = text,
    feelsLikeCelsius = feelsLike.toDoubleOrNull() ?: 0.0,
    humidityPct = humidity.toIntOrNull() ?: 0,
    wind = windDir,
    windAngle = wind360,
    windSpeedRaw = windSpeed,
    windScale = windScale,
    iconCode = icon,
    pressureHpa = pressure.toIntOrNull() ?: 0
)

fun QWeatherHourly.toDomain(): HourlyForecast {
    val time = fxTime.substringAfter("T").substringBefore("+")
    return HourlyForecast(
        time = time,
        tempCelsius = temp.toDoubleOrNull() ?: 0.0,
        iconCode = icon,
        popPct = pop.toIntOrNull() ?: 0,
        isNow = false,
        rawTime = fxTime
    )
}

fun QWeatherDaily.toDomain(index: Int): DailyForecast = DailyForecast(
    date = if (index == 0) "今天" else formatFxDate(fxDate),
    highTempCelsius = tempMax.toDoubleOrNull() ?: 0.0,
    lowTempCelsius = tempMin.toDoubleOrNull() ?: 0.0,
    iconCode = iconDay,
    description = textDay,
    // 原 formatPop 语义:负数归零,保留"无降水概率不展示"的判断依据(popPct == 0)
    popPct = pop.toIntOrNull()?.coerceAtLeast(0) ?: 0,
    uvIndexValue = uvIndex.toIntOrNull() ?: 0,
    moonPhase = moonPhase.toMoonPhase(),
    sunrise = sunrise,
    sunset = sunset
)

internal fun formatFxDate(fxDate: String): String {
    if (fxDate.length < 10) return fxDate
    // 防御非法格式:如 "2026-08-01T00:00"、"2026-08"、"2026-13-40" 等,原样返回而非抛异常
    val date = runCatching { java.time.LocalDate.parse(fxDate) }.getOrNull() ?: return fxDate
    val week = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
    val dayOfWeek = date.dayOfWeek.value % 7
    return "${week[dayOfWeek]} ${date.monthValue}/${date.dayOfMonth}"
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
