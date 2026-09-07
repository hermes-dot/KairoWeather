package com.yuzheng.kairoweather.data.model

import kotlinx.serialization.Serializable

/**
 * 实时空气质量(`/v7/air/now`)。字段对照官方响应样例与 PlayWeather 的 AirNowBean 交叉核实。
 * 只声明用到的字段——pm2p5 等 6 种污染物浓度卡面不展示,依 ignoreUnknownKeys 直接忽略。
 */
@Serializable
data class AirQualityResponse(
    val code: String,
    val updateTime: String = "",
    val now: QWeatherAirNow? = null,
)

@Serializable
data class QWeatherAirNow(
    val aqi: String = "",
    val category: String = "",
    val primary: String = "",
)
