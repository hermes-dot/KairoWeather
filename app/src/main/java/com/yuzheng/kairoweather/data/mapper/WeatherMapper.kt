package com.yuzheng.kairoweather.data.mapper

import com.yuzheng.kairoweather.data.model.AirQualityResponse
import com.yuzheng.kairoweather.data.model.QWeatherNow
import com.yuzheng.kairoweather.data.model.QWeatherHourly
import com.yuzheng.kairoweather.data.model.QWeatherDaily
import com.yuzheng.kairoweather.domain.model.AirQuality
import com.yuzheng.kairoweather.domain.model.categoryForAqi
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

/**
 * 空气质量 DTO → domain。新版响应是多标准索引数组,取中国国标 cn-mee(卡面文案
 * 明确写"AQI(CN)",换标准等于换语义,不做配置项)。
 *
 * 索引缺失或 aqi 非数字时返回 null,由 Repository 转成 EmptyDataError——
 * 不猜 0,因为 0 在 AQI 语义里是"最优",谎报比缺失更糟。
 */
fun AirQualityResponse.toDomain(): AirQuality? {
    val index = indexes.firstOrNull { it.code == CN_MEE_INDEX } ?: return null
    val value = index.aqi?.toInt() ?: return null
    return AirQuality(
        aqi = value.coerceIn(0, 500),
        category = index.category.ifBlank { categoryForAqi(value) },
        primaryPollutant = index.primaryPollutant?.name ?: "",
        healthEffect = index.health?.effect ?: "",
        adviceGeneral = index.health?.advice?.generalPopulation ?: "",
        adviceSensitive = index.health?.advice?.sensitivePopulation ?: "",
    )
}

private const val CN_MEE_INDEX = "cn-mee"

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
