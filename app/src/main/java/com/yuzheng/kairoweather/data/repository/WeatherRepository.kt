package com.yuzheng.kairoweather.data.repository

import com.yuzheng.kairoweather.data.mapper.toDomain
import com.yuzheng.kairoweather.data.resultCatching
import com.yuzheng.kairoweather.domain.model.CurrentWeather
import com.yuzheng.kairoweather.domain.model.DailyForecast
import com.yuzheng.kairoweather.domain.model.HourlyForecast
import com.yuzheng.kairoweather.remote.WeatherApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(private val api: WeatherApiService) {

    companion object {
        /** 天气数据内存缓存有效期:15 分钟(QWeather 免费套餐更新频率约 10 分钟) */
        private const val CACHE_TTL_MILLIS = 15 * 60 * 1000L
    }

    private val cache = WeatherCache()

    suspend fun getCurrentWeather(location: String, forceRefresh: Boolean = false): Result<CurrentWeather> =
        cached("$location|current", forceRefresh) {
            resultCatching {
                val response = api.getCurrentWeather(location)
                if (response.code != "200") throw WeatherException.ApiError(response.code)
                val now = response.now ?: throw WeatherException.EmptyDataError()
                now.toDomain()
            }
        }

    suspend fun getHourlyForecast(location: String, forceRefresh: Boolean = false): Result<List<HourlyForecast>> =
        cached("$location|hourly", forceRefresh) {
            resultCatching {
                val response = api.getHourlyWeather(location = location)
                if (response.code != "200") throw WeatherException.ApiError(response.code)
                response.hourly.map { it.toDomain() }
            }
        }

    suspend fun getDailyForecast(location: String, forceRefresh: Boolean = false): Result<List<DailyForecast>> =
        cached("$location|daily", forceRefresh) {
            resultCatching {
                val response = api.getDailyWeather(location = location)
                if (response.code != "200") throw WeatherException.ApiError(response.code)
                response.daily.mapIndexed { i, d -> d.toDomain(i) }
            }
        }

    // 地名反查不缓存:随用户搜索/定位变化、时效性强,且不影响主页面数据量,
    // 不缓存以保证地名实时准确(天气数据相对稳定,15 分钟 TTL 足够)。
    suspend fun reverseGeocode(location: String): Result<String> = resultCatching {
        val response = api.reverseGeocode(location)
        if (response.code != "200") throw WeatherException.GeoApiError(response.code)
        val city = response.location.firstOrNull() ?: throw WeatherException.CityNotFoundError()
        when {
            city.adm2.isNotEmpty() -> city.adm2
            city.adm1.isNotEmpty() -> city.adm1
            else -> city.name
        }
    }

    /**
     * 内存 TTL 缓存包装:命中且未过期直接返回缓存;未命中/过期执行 [block] 回源,
     * 仅成功结果写入缓存。[forceRefresh] 为 true 时跳过缓存读取直接回源(刷新),
     * 成功结果仍写入缓存。inline 使 [block] 内可调用 suspend 函数(与 [resultCatching] 同机制)。
     */
    private inline fun <T> cached(key: String, forceRefresh: Boolean = false, block: () -> Result<T>): Result<T> {
        if (!forceRefresh) {
            val hit = cache.get<Result<T>>(key, CACHE_TTL_MILLIS)
            if (hit != null) return hit
        }
        val result = block()
        if (result.isSuccess) cache.put(key, result)
        return result
    }
}
