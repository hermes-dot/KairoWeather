package com.yuzheng.kairoweather.domain.model

/**
 * 展示格式化工具:domain 模型只存原始数值,这里负责把数值转成 UI 展示文案。
 *
 * 温度换算与取整规则与原 `WeatherMapper.formatTemp` 保持一致:
 * 摄氏直接截断取整;华氏按 F = C*9/5+32 后截断取整(与原实现行为一致)。
 * 单位后缀跟随所选单位(°C/°F),避免华氏数值挂着摄氏单位的错标。
 */
fun Double.toTempString(unit: TemperatureUnit): String {
    val fahrenheit = unit == TemperatureUnit.FAHRENHEIT
    val display = if (fahrenheit) this * 9 / 5 + 32 else this
    return "${display.toInt()}°${if (fahrenheit) "F" else "C"}"
}

/** 百分比展示,如 60 -> "60%" */
fun Int.toPercentString(): String = "$this%"

/** 体感温度展示,如 26.0 -> "体感26°C"(摄氏) / "体感79°F"(华氏) */
fun Double.toFeelLikeString(unit: TemperatureUnit): String = "体感${toTempString(unit)}"
