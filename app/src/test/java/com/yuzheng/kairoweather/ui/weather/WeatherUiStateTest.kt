package com.yuzheng.kairoweather.ui.weather

import com.yuzheng.kairoweather.domain.model.CurrentWeather
import com.yuzheng.kairoweather.domain.model.HourlyForecast
import com.yuzheng.kairoweather.domain.model.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `defaults are empty and safe for first render`() {
        val state = WeatherUiState()

        assertNull(state.current)
        assertTrue(state.hourly.isEmpty())
        assertTrue(state.daily.isEmpty())
        assertNull(state.air)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(0f, state.sunProgress)
        assertEquals("北京", state.locationName)
        assertEquals(TemperatureUnit.CELSIUS, state.temperatureUnit)
    }

    @Test
    fun `copy only overrides provided fields`() {
        val state = WeatherUiState().copy(
            current = current,
            isLoading = true,
            error = "boom",
        )

        assertEquals(current, state.current)
        assertTrue(state.isLoading)
        assertEquals("boom", state.error)
        // 未指定字段保持默认值
        assertTrue(state.hourly.isEmpty())
        assertTrue(state.daily.isEmpty())
        assertNull(state.air)
        assertEquals(0f, state.sunProgress)
        assertEquals("北京", state.locationName)
        assertEquals(TemperatureUnit.CELSIUS, state.temperatureUnit)
    }

    @Test
    fun `data class equality and hashCode`() {
        assertEquals(WeatherUiState(), WeatherUiState())
        assertEquals(WeatherUiState().hashCode(), WeatherUiState().hashCode())
        assertNotEquals(WeatherUiState(isLoading = true), WeatherUiState())
        assertNotEquals(
            WeatherUiState(locationName = "上海"),
            WeatherUiState(locationName = "北京"),
        )
    }

    @Test
    fun `component accessors match declaration order`() {
        val hourly = listOf(
            HourlyForecast(time = "12:00", tempCelsius = 24.0, iconCode = "100", popPct = 10, isNow = true),
        )
        val air = com.yuzheng.kairoweather.domain.model.AirQuality(
            aqi = 42, category = "优", primaryPollutant = "o3",
        )
        val state = WeatherUiState(
            current = current,
            hourly = hourly,
            daily = emptyList(),
            air = air,
            isLoading = true,
            error = "err",
            sunProgress = 0.5f,
            locationName = "上海",
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
        )

        assertEquals(current, state.component1())
        assertEquals(hourly, state.component2())
        assertEquals(emptyList<HourlyForecast>(), state.component3())
        assertEquals(air, state.component4())
        assertTrue(state.component5())
        assertEquals("err", state.component6())
        assertEquals(0.5f, state.component7())
        assertEquals("上海", state.component8())
        assertEquals(TemperatureUnit.FAHRENHEIT, state.component9())
    }
}
