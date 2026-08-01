package com.yuzheng.kairoweather.data.repository

import com.yuzheng.kairoweather.data.model.GeoLocation
import com.yuzheng.kairoweather.data.model.GeoResponse
import com.yuzheng.kairoweather.data.model.QWeatherDaily
import com.yuzheng.kairoweather.data.model.QWeatherDailyResponse
import com.yuzheng.kairoweather.data.model.QWeatherHourly
import com.yuzheng.kairoweather.data.model.QWeatherHourlyResponse
import com.yuzheng.kairoweather.data.model.QWeatherNow
import com.yuzheng.kairoweather.data.model.QWeatherNowResponse
import com.yuzheng.kairoweather.remote.WeatherApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRepositoryTest {

    private val api = mockk<WeatherApiService>()
    private val repository = WeatherRepository(api)

    @Test
    fun `getCurrentWeather maps non-200 to ApiError`(): Unit = runTest {
        coEvery { api.getCurrentWeather(any()) } returns QWeatherNowResponse(code = "401")

        val result = repository.getCurrentWeather("1,2")

        val error = result.exceptionOrNull()
        assertTrue(error is WeatherException.ApiError)
        assertEquals("401", (error as WeatherException.ApiError).code)
        assertEquals("API error: code=401", error.message)
    }

    @Test
    fun `getCurrentWeather maps missing now to EmptyDataError`(): Unit = runTest {
        coEvery { api.getCurrentWeather(any()) } returns QWeatherNowResponse(code = "200", now = null)

        val result = repository.getCurrentWeather("1,2")

        assertTrue(result.exceptionOrNull() is WeatherException.EmptyDataError)
    }

    @Test
    fun `reverseGeocode maps empty location to CityNotFoundError`(): Unit = runTest {
        coEvery { api.reverseGeocode(any()) } returns GeoResponse(code = "200", location = emptyList())

        val result = repository.reverseGeocode("1,2")

        assertTrue(result.exceptionOrNull() is WeatherException.CityNotFoundError)
    }

    @Test
    fun `getCurrentWeather returns domain model on success`(): Unit = runTest {
        coEvery { api.getCurrentWeather(any()) } returns QWeatherNowResponse(
            code = "200",
            now = QWeatherNow(temp = "24", text = "晴", icon = "100"),
        )

        val result = repository.getCurrentWeather("1,2")

        assertEquals(24.0, result.getOrNull()?.tempCelsius ?: 0.0, 0.0)
        assertEquals("晴", result.getOrNull()?.condition)
    }

    @Test
    fun `resultCatching does not swallow CancellationException`(): Unit = runTest {
        coEvery { api.getCurrentWeather(any()) } throws CancellationException("cancelled")

        val caught = try {
            repository.getCurrentWeather("1,2")
            null
        } catch (e: CancellationException) {
            e
        }

        assertEquals("cancelled", caught?.message)
    }

    // ── 内存 TTL 缓存 ──

    @Test
    fun `getCurrentWeather caches success within ttl`(): Unit = runTest {
        coEvery { api.getCurrentWeather(any()) } returns QWeatherNowResponse(
            code = "200",
            now = QWeatherNow(temp = "24", text = "晴", icon = "100"),
        )

        repository.getCurrentWeather("1,2")
        repository.getCurrentWeather("1,2")

        coVerify(exactly = 1) { api.getCurrentWeather("1,2") }
    }

    @Test
    fun `getCurrentWeather does not cache failure`(): Unit = runTest {
        coEvery { api.getCurrentWeather(any()) } returns QWeatherNowResponse(code = "500")

        repository.getCurrentWeather("1,2")
        repository.getCurrentWeather("1,2")

        // 失败结果不缓存,第二次仍走网络
        coVerify(exactly = 2) { api.getCurrentWeather("1,2") }
    }

    @Test
    fun `forceRefresh bypasses cache`(): Unit = runTest {
        coEvery { api.getCurrentWeather(any()) } returns QWeatherNowResponse(
            code = "200",
            now = QWeatherNow(temp = "24", text = "晴", icon = "100"),
        )

        // 第一次正常调用写入缓存
        repository.getCurrentWeather("1,2")
        // 第二次 forceRefresh = true 应跳过缓存读取,直接回源
        repository.getCurrentWeather("1,2", forceRefresh = true)

        coVerify(exactly = 2) { api.getCurrentWeather("1,2") }
    }

    @Test
    fun `getHourlyForecast caches success within ttl`(): Unit = runTest {
        coEvery { api.getHourlyWeather(any(), any()) } returns QWeatherHourlyResponse(
            code = "200",
            hourly = listOf(QWeatherHourly(fxTime = "2021-02-16T15:00+08:00", temp = "20", pop = "30")),
        )

        repository.getHourlyForecast("1,2")
        repository.getHourlyForecast("1,2")

        // getHourlyWeather 首个参数是 hours(默认 24h),location 是第二个参数
        coVerify(exactly = 1) { api.getHourlyWeather(any(), "1,2") }
    }

    @Test
    fun `getDailyForecast caches success within ttl`(): Unit = runTest {
        coEvery { api.getDailyWeather(any(), any()) } returns QWeatherDailyResponse(
            code = "200",
            daily = listOf(QWeatherDaily(fxDate = "2026-08-01", tempMax = "30", tempMin = "22")),
        )

        repository.getDailyForecast("1,2")
        repository.getDailyForecast("1,2")

        // getDailyWeather 首个参数是 days(默认 7d),location 是第二个参数
        coVerify(exactly = 1) { api.getDailyWeather(any(), "1,2") }
    }

    @Test
    fun `different locations use separate cache entries`(): Unit = runTest {
        coEvery { api.getCurrentWeather(any()) } returns QWeatherNowResponse(
            code = "200",
            now = QWeatherNow(temp = "24", text = "晴", icon = "100"),
        )

        repository.getCurrentWeather("1,2")
        repository.getCurrentWeather("3,4")

        coVerify(exactly = 1) { api.getCurrentWeather("1,2") }
        coVerify(exactly = 1) { api.getCurrentWeather("3,4") }
    }

    @Test
    fun `reverseGeocode is not cached`(): Unit = runTest {
        coEvery { api.reverseGeocode(any()) } returns GeoResponse(
            code = "200",
            location = listOf(GeoLocation(adm2 = "北京")),
        )

        repository.reverseGeocode("1,2")
        repository.reverseGeocode("1,2")

        coVerify(exactly = 2) { api.reverseGeocode("1,2") }
    }
}
