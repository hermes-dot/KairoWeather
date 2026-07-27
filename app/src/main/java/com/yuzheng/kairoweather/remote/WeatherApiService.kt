package com.yuzheng.kairoweather.remote

import com.yuzheng.kairoweather.data.model.GeoResponse
import com.yuzheng.kairoweather.data.model.QWeatherDailyResponse
import com.yuzheng.kairoweather.data.model.QWeatherHourlyResponse
import com.yuzheng.kairoweather.data.model.QWeatherNowResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WeatherApiService {

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
