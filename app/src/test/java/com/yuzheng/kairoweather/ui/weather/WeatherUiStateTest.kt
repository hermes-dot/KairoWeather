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
        val state = WeatherUiState(
            current = current,
            hourly = hourly,
            daily = emptyList(),
            isLoading = true,
            error = "err",
            sunProgress = 0.5f,
            locationName = "上海",
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
        )

        assertEquals(current, state.component1())
        assertEquals(hourly, state.component2())
        assertEquals(emptyList<HourlyForecast>(), state.component3())
        assertTrue(state.component4())
        assertEquals("err", state.component5())
        assertEquals(0.5f, state.component6())
        assertEquals("上海", state.component7())
        assertEquals(TemperatureUnit.FAHRENHEIT, state.component8())
    }
}
