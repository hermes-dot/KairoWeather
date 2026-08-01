package com.yuzheng.kairoweather.di

import com.yuzheng.kairoweather.BuildConfig
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.yuzheng.kairoweather.remote.WeatherApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val BASE_URL: String = BuildConfig.QWEATHER_BASE_URL
        .ifEmpty { error("请在项目根目录的 qweather.properties 中配置 baseUrl（https://console.qweather.com/setting 获取）") }
        // P2-E: Retrofit 要求 baseUrl 以 / 结尾,否则运行时抛 IllegalArgumentException。
        // 配置漏写尾斜杠时自动补上,避免启动崩溃;若想严格校验可改为抛错。
        .let { if (it.endsWith("/")) it else "$it/" }
    private val API_KEY: String = BuildConfig.QWEATHER_API_KEY
        .ifEmpty { error("请在项目根目录的 qweather.properties 中配置 apiKey（https://console.qweather.com 获取）") }

    private val json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .apply {
            // 仅 Debug 构建打印请求/响应日志，Release 不输出敏感信息
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    // P2-12: 不用 BODY——BODY 会打印完整请求/响应体(含按位置请求的
                    // 用户经纬度与天气数据)以及请求头(X-QW-Api-Key)。HEADERS 只打印
                    // 请求行与头,保留调试所需的关键信息同时不输出响应体数据。
                    level = HttpLoggingInterceptor.Level.HEADERS
                })
            }
        }
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("X-QW-Api-Key", API_KEY)
                    .build()
            )
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideWeatherApiService(retrofit: Retrofit): WeatherApiService =
        retrofit.create(WeatherApiService::class.java)
}
