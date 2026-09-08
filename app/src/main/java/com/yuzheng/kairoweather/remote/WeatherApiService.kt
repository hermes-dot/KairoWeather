package com.yuzheng.kairoweather.remote

import com.yuzheng.kairoweather.data.model.AirQualityResponse
import com.yuzheng.kairoweather.data.model.GeoResponse
import com.yuzheng.kairoweather.data.model.QWeatherDailyResponse
import com.yuzheng.kairoweather.data.model.QWeatherHourlyResponse
import com.yuzheng.kairoweather.data.model.QWeatherNowResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WeatherApiService {

    /**
     * 实时空气质量(新版端点)。旧 `/v7/air/now` 已于 2026-09 下线(HTTP 403 Deprecated),
     * 新版改为路径参数且**纬度在前**:`airquality/v1/current/{lat}/{lon}`。
     * 响应不再有 `code` 字段,HTTP 状态码即结果(Retrofit 非 2xx 抛异常)。
     */
    @GET("airquality/v1/current/{latitude}/{longitude}")
    suspend fun getAirNow(
        @Path("latitude") latitude: String,
        @Path("longitude") longitude: String,
        @Query("lang") lang: String = "zh",
    ): AirQualityResponse

    @GET("/geo/v2/city/lookup")
    suspend fun reverseGeocode(
        @Query("location") location: String,
        @Query("lang") lang: String = "zh",
    ): GeoResponse

    @GET("/v7/weather/now")
    suspend fun getCurrentWeather(
        @Query("location") location: String,
        @Query("lang") lang: String = "zh"
    ): QWeatherNowResponse

    @GET("/v7/weather/{hours}")
    suspend fun getHourlyWeather(
        @Path("hours") hours: String = "24h",
        @Query("location") location: String,
        @Query("lang") lang: String = "zh"
    ): QWeatherHourlyResponse

    @GET("/v7/weather/{days}")
    suspend fun getDailyWeather(
        @Path("days") days: String = "7d",
        @Query("location") location: String,
        @Query("lang") lang: String = "zh"
    ): QWeatherDailyResponse
}
