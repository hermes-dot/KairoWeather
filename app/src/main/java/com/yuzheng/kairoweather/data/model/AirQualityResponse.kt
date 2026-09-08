package com.yuzheng.kairoweather.data.model

import kotlinx.serialization.Serializable

/**
 * 实时空气质量(`/airquality/v1/current/{lat}/{lon}`,新版端点)。
 * 响应不再有旧版 `code` 字段——HTTP 状态码即结果,非 2xx 由 Retrofit 抛异常,
 * 所以这里只声明业务数据。
 *
 * `indexes` 是同一时刻的多套国家标准 AQI(如中国 cn-mee、美国 us-epa、
 * 和风通用 qaqi),国内 App 取 cn-mee;`pollutants` 浓度卡面不展示,
 * 依 ignoreUnknownKeys 忽略,只留 primaryPollutant 与 health(AQI 卡用)。
 */
@Serializable
data class AirQualityResponse(
    val indexes: List<AirIndex> = emptyList(),
)

@Serializable
data class AirIndex(
    val code: String = "",
    val aqi: Double? = null,
    val category: String = "",
    val primaryPollutant: AirPollutantRef? = null,
    val health: AirHealth? = null,
)

/**
 * 健康影响与建议。官方文档标注三个字段都可能为空(部分标准/等级下不返回),
 * 且 effect 在 JSON 里可以是显式 null——一律可空接收,mapper 侧归 ""。
 */
@Serializable
data class AirHealth(
    val effect: String? = null,
    val advice: AirHealthAdvice? = null,
)

@Serializable
data class AirHealthAdvice(
    val generalPopulation: String? = null,
    val sensitivePopulation: String? = null,
)

@Serializable
data class AirPollutantRef(
    val name: String = "",
)
