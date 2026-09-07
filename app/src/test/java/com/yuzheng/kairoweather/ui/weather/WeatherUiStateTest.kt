package com.yuzheng.kairoweather.ui.weather

import com.yuzheng.kairoweather.domain.model.AirQuality
import com.yuzheng.kairoweather.domain.model.CurrentWeather
import com.yuzheng.kairoweather.domain.model.HourlyForecast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WeatherUiState sealed 状态机形状测试。
 *
 * 旧版针对可空字段的断言(默认值表、解构顺序、copy 语义)随平铺 data class
 * 一起退役——sealed 后"非法状态不可表示"由编译器保证,这里只测 Ready 内部
 * 卡级字段的默认降级形状与状态间不互等(状态机三态互斥的运行时侧影)。
 */
class WeatherUiStateTest {

    private val current = CurrentWeather(
        tempCelsius = 24.0,
        condition = "晴",
        feelsLikeCelsius = 26.0,
        humidityPct = 60,
        wind = "东北风",
        windAngle = "45",
        windSpeedRaw = "10",
        windScale = "3",
        iconCode = "100",
        pressureHpa = 1013,
    )

    @Test
    fun `Ready requires current and defaults the rest to renderable-empty`() {
        val ready = WeatherUiState.Ready(current = current)

        assertEquals(current, ready.current)
        assertTrue(ready.hourly.isEmpty())
        assertTrue(ready.daily.isEmpty())
        // air=null:卡级缺席是合法默认(AQI 卡不渲染),不是页面错误
        assertNull(ready.air)
        // 刷新旗标默认关:进 Ready ≠ 在转圈
        assertFalse(ready.isRefreshing)
        assertEquals(0f, ready.sunProgress)
        assertEquals(WeatherUiState.DEFAULT_CITY_NAME, ready.locationName)
    }

    @Test
    fun `copy only overrides provided fields`() {
        val ready = WeatherUiState.Ready(current = current).copy(
            air = AirQuality(aqi = 42, category = "优", primaryPollutant = "o3"),
            isRefreshing = true,
        )

        assertEquals(42, ready.air?.aqi)
        assertTrue(ready.isRefreshing)
        assertEquals(current, ready.current)
        assertTrue(ready.hourly.isEmpty())
    }

    @Test
    fun `three states are mutually unequal`() {
        // 显式提到接口类型:绕开 JUnit assertNotEquals(long,long) 重载决议陷阱
        val a: WeatherUiState = WeatherUiState.Loading
        val b: WeatherUiState = WeatherUiState.Failed("boom")
        val c: WeatherUiState = WeatherUiState.Ready(current = current)
        assertNotEquals(a, b)
        assertNotEquals(b, c)
        assertNotEquals(c, a)
    }

    @Test
    fun `ready equality is structural for data-class fields, loading is singleton`() {
        assertEquals(
            WeatherUiState.Ready(current = current, isRefreshing = true),
            WeatherUiState.Ready(current = current, isRefreshing = true),
        )
        assertNotEquals(
            WeatherUiState.Ready(current = current, isRefreshing = true),
            WeatherUiState.Ready(current = current, isRefreshing = false),
        )
        assertEquals(WeatherUiState.Loading, WeatherUiState.Loading)
    }
}
