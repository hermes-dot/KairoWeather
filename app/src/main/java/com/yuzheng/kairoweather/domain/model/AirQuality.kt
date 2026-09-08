package com.yuzheng.kairoweather.domain.model

import androidx.compose.runtime.Stable

/**
 * 实时空气质量(中国标准 AQI)。[aqi] 为解析后的原始数值,
 * 等级着色/刻度条由 UI 层按国标分段处理(与温度"域存数值、UI 管展示"的约定一致)。
 *
 * [category] 保证非空:和风文档明说该字段"可能为空",缺失时由 [categoryForAqi] 按国标补齐,
 * 使 UI 层不必为它写兜底分支。
 *
 * 健康建议三字段来自新版 API 的 `health`(官方标注可能为空)。缺失时归 ""——
 * "无建议"是合法数据形态(UI 按空串整段缺席),不像 category 那样能由 AQI 推导,
 * 所以不补假文案。
 */
@Stable
data class AirQuality(
    val aqi: Int,
    val category: String,
    val primaryPollutant: String,
    val healthEffect: String = "",
    val adviceGeneral: String = "",
    val adviceSensitive: String = "",
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

/**
 * 健康建议的展示行(纯函数,单测覆盖)。官方 cn-mee 文案自带人群主语
 * ("各类人群可正常活动""儿童、老年人及……"),不额外加"一般人群:"前缀;
 * 两人群建议相同(优级常态)时合并成一句——原样直出就是重复文案。
 * 敏感人群文案是"点名式"的,不同时补"敏感人群:"前缀辅助归类。
 * 全空返回空列表:UI 据此整段缺席。
 */
fun AirQuality.healthAdviceLines(): List<String> {
    val g = adviceGeneral.trim()
    val s = adviceSensitive.trim()
    return when {
        g.isNotEmpty() && s.isNotEmpty() && g != s -> listOf(g, "敏感人群:$s")
        g.isNotEmpty() -> listOf(g)
        s.isNotEmpty() -> listOf("敏感人群:$s")
        else -> emptyList()
    }
}
