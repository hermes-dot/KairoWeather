package com.yuzheng.kairoweather.data.model

import kotlinx.serialization.Serializable

/**
 * 实时空气质量(`/airquality/v1/current/{lat}/{lon}`,新版端点)。
 * 响应没有旧版 `code` 字段——HTTP 状态码即结果,非 2xx 由 Retrofit 抛异常,
 * 所以这里只声明业务数据。
 *
 * `indexes` 是同一时刻的多套国家标准 AQI(如中国 cn-mee、美国 us-epa、
 * 和风通用 qaqi),国内 App 取 cn-mee;`pollutants` 浓度与 `health` 建议
 * 卡面暂不展示,依 ignoreUnknownKeys 忽略,只留 primaryPollutant 需要的名字。
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
)

@Serializable
data class AirPollutantRef(
    val name: String = "",
)
