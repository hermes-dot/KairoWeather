package com.yuzheng.kairoweather.domain.model

import androidx.compose.runtime.Stable

/** 逐小时预报,数值字段([tempCelsius]/[popPct])由 UI 层按单位与文案格式化。 */
@Stable
data class HourlyForecast(
    val time: String,
    val tempCelsius: Double,
    val iconCode: String,
    val popPct: Int,
    val isNow: Boolean,
    /** 原始 fxTime(如 "2021-02-16T15:00+08:00"),含时区偏移,用于跨时区判断当前小时 */
    val rawTime: String = ""
)
