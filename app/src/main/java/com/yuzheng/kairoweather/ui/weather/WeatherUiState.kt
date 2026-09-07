package com.yuzheng.kairoweather.ui.weather

import androidx.compose.runtime.Stable
import com.yuzheng.kairoweather.domain.model.AirQuality
import com.yuzheng.kairoweather.domain.model.CurrentWeather
import com.yuzheng.kairoweather.domain.model.DailyForecast
import com.yuzheng.kairoweather.domain.model.HourlyForecast

/**
 * 天气页 UI 状态:sealed 三态声明页面级状态机——Loading/Failed/Ready 互斥,
 * "loading 同时有数据""error 同时有数据"这类非法组合在类型上不可表示,
 * UI 层 when(state) 穷尽即编译期完备,不再需要 else 首帧兜底分支(P2-8 退役)。
 *
 * 边界说明:
 * - 卡级缺席(air=null 不渲染 AQI 卡)是产品语义,仍用可空字段表达——
 *   页面态用 sealed 收,组件级降级用可空,两层语义不混装;
 * - isRefreshing(内容在、指示器转)是 Ready 的内部旗标,与冷启动 Loading 在类型上分家:
 *   下拉刷新永远不该把有内容的页面打回 Loading;
 * - temperatureUnit 不再进页面态:其真值源是 DataStore,由 VM 直接暴露 flow,
 *   消除旧设计"collect 镜像进 state"的双写源。
 */
sealed interface WeatherUiState {

    /** 冷启动/重试中:尚无任何可渲染内容。 */
    data object Loading : WeatherUiState

    /** 冷启动失败(current 请求失败且无已有内容):整页错误。 */
    data class Failed(val message: String) : WeatherUiState

    @Stable
    data class Ready(
        val current: CurrentWeather,
        val hourly: List<HourlyForecast> = emptyList(),
        val daily: List<DailyForecast> = emptyList(),
        /** 空气质量独立端点,失败静默(P2-6 语义延伸):null = 无数据,AQI 卡整卡不渲染 */
        val air: AirQuality? = null,
        /** 下拉刷新指示器:仅表示"内容在、正在拉新",不参与页面三态判定 */
        val isRefreshing: Boolean = false,
        val sunProgress: Float = 0f,
        val locationName: String = DEFAULT_CITY_NAME,
    ) : WeatherUiState

    companion object {
        /** 反查失败/未回来时的城市名占位,与 VM 默认坐标(DEFAULT_LOCATION=北京)语义配对。 */
        const val DEFAULT_CITY_NAME = "北京"
    }
}
