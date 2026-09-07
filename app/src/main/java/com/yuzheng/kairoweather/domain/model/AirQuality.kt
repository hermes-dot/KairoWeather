package com.yuzheng.kairoweather.domain.model

import androidx.compose.runtime.Stable

/**
 * 实时空气质量(中国标准 AQI)。[aqi] 为解析后的原始数值,
 * 等级着色/刻度条由 UI 层按国标分段处理(与温度"域存数值、UI 管展示"的约定一致)。
 *
 * [category] 保证非空:和风文档明说该字段"可能为空",缺失时由 [categoryForAqi] 按国标补齐,
 * 使 UI 层不必为它写兜底分支。
 */
@Stable
data class AirQuality(
    val aqi: Int,
    val category: String,
    val primaryPollutant: String,
)

/**
 * AQI → 国标 HJ 633-2012 六级类别。既是 API 缺 category 时的补全依据,
 * 也是 UI 等级着色的分段依据——一份规则两处用,不重复定义阈值。
 */
fun categoryForAqi(aqi: Int): String = when {
    aqi <= 50 -> "优"
    aqi <= 100 -> "良"
    aqi <= 150 -> "轻度污染"
    aqi <= 200 -> "中度污染"
    aqi <= 300 -> "重度污染"
    else -> "严重污染"
}
