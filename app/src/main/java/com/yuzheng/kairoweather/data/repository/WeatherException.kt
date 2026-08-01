package com.yuzheng.kairoweather.data.repository

/**
 * 天气业务异常,作为 [kotlin.Result] 的 failure 载体。
 *
 * 将原始异常消息(如 "API error: code=xxx")从 Repository 透传改为结构化错误,
 * 便于上层按类型处理;同时保留用户可读的 [message] 供 UI 直接展示。
 */
sealed class WeatherException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** 天气业务接口返回非 200 */
    class ApiError(val code: String) : WeatherException("API error: code=$code")

    /** 地理编码接口返回非 200 */
    class GeoApiError(val code: String) : WeatherException("GeoAPI error: code=$code")

    /** 接口返回成功但数据缺失 */
    class EmptyDataError : WeatherException("数据为空")

    /** 地理编码未命中任何城市 */
    class CityNotFoundError : WeatherException("未找到城市")
}
