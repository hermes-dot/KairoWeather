package com.yuzheng.kairoweather.data.repository

import com.yuzheng.kairoweather.data.mapper.toDomain
import com.yuzheng.kairoweather.domain.model.CurrentWeather
import com.yuzheng.kairoweather.domain.model.DailyForecast
import com.yuzheng.kairoweather.domain.model.HourlyForecast
import com.yuzheng.kairoweather.remote.WeatherApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(private val api: WeatherApiService) {

    suspend fun getCurrentWeather(location: String, unit: String): Result<CurrentWeather> = runCatching {
        val response = api.getCurrentWeather(location)
        if (response.code != "200" || response.now == null)
            throw Exception("API error: code=${response.code}")
        response.now.toDomain(unit)
    }

    suspend fun getHourlyForecast(location: String, unit: String): Result<List<HourlyForecast>> = runCatching {
        val response = api.getHourlyWeather(location = location)
        if (response.code != "200") throw Exception("API error: code=${response.code}")
        response.hourly.map { it.toDomain(unit) }
    }

    suspend fun getDailyForecast(location: String, unit: String): Result<List<DailyForecast>> = runCatching {
        val response = api.getDailyWeather(location = location)
        if (response.code != "200") throw Exception("API error: code=${response.code}")
        response.daily.mapIndexed { i, d -> d.toDomain(i, unit) }
    }

    suspend fun reverseGeocode(location: String): Result<String> = runCatching {
        val response = api.reverseGeocode(location)
        if (response.code != "200") throw Exception("GeoAPI error: code=${response.code}")
        val city = response.location.firstOrNull() ?: throw Exception("未找到城市")
        when {
            city.adm2.isNotEmpty() -> city.adm2
            city.adm1.isNotEmpty() -> city.adm1
            else -> city.name
        }
    }
}
