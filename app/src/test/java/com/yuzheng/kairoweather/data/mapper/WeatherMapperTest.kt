package com.yuzheng.kairoweather.data.mapper

import com.yuzheng.kairoweather.data.model.QWeatherDaily
import com.yuzheng.kairoweather.data.model.QWeatherHourly
import com.yuzheng.kairoweather.data.model.QWeatherNow
import com.yuzheng.kairoweather.domain.model.MoonPhase
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherMapperTest {

    // ── 当前天气 ──

    @Test
    fun `now toDomain keeps celsius and formats fields`() {
        val dto = QWeatherNow(
            temp = "24",
            feelsLike = "26",
            humidity = "60",
            text = "晴",
            windDir = "东北风",
            windScale = "3",
            icon = "100",
        )

        val domain = dto.toDomain("celsius")

        assertEquals("24°", domain.temperature)
        assertEquals("体感26°", domain.feelLike)
        assertEquals("湿度60%", domain.humidity)
        assertEquals("东北风 3级", domain.wind)
        assertEquals("100", domain.iconCode)
        assertEquals("晴", domain.condition)
    }

    @Test
    fun `now toDomain converts to fahrenheit`() {
        val dto = QWeatherNow(temp = "24", feelsLike = "26")

        val domain = dto.toDomain("fahrenheit")

        assertEquals("75°", domain.temperature)
        assertEquals("体感78°", domain.feelLike)
    }

    // ── 逐小时 ──

    @Test
    fun `hourly toDomain extracts local time and pop`() {
        val dto = QWeatherHourly(
            fxTime = "2021-02-16T15:00+08:00",
            temp = "20",
            pop = "30",
            icon = "305",
        )

        val domain = dto.toDomain("celsius")

        assertEquals("15:00", domain.time)
        assertEquals("30%", domain.pop)
        assertEquals("20°", domain.temperature)
        assertEquals("305", domain.iconCode)
        assertEquals(false, domain.isNow)
    }

    // ── 逐日 ──

    @Test
    fun `daily toDomain marks today and formats date`() {
        val today = LocalDate.now()
        val dto = QWeatherDaily(
            fxDate = today.toString(),
            tempMax = "30",
            tempMin = "22",
            iconDay = "100",
            textDay = "晴",
        )

        val domain = dto.toDomain(0, "celsius")

        assertEquals("今天", domain.date)
        assertEquals("30°", domain.highTemp)
        assertEquals("22°", domain.lowTemp)
    }

    @Test
    fun `daily toDomain formats future date with weekday`() {
        val date = LocalDate.of(2026, 8, 1)
        val dto = QWeatherDaily(fxDate = date.toString())

        val domain = dto.toDomain(1, "celsius")

        val week = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val expected = "${week[date.dayOfWeek.value % 7]} 8/1"
        assertEquals(expected, domain.date)
    }

    @Test
    fun `daily toDomain maps pop uv and moon phase`() {
        val dto = QWeatherDaily(
            pop = "35",
            uvIndex = "6",
            moonPhase = "满月",
            sunrise = "05:00",
            sunset = "19:00",
        )

        val domain = dto.toDomain(0, "celsius")

        assertEquals("35%", domain.pop)
        assertEquals("6", domain.uvIndex)
        assertEquals(MoonPhase.FULL_MOON, domain.moonPhase)
        assertEquals("05:00", domain.sunrise)
        assertEquals("19:00", domain.sunset)
    }

    @Test
    fun `daily pop zero or blank stays zero`() {
        assertEquals("0%", QWeatherDaily(pop = "0").toDomain(0, "celsius").pop)
        assertEquals("0%", QWeatherDaily(pop = "").toDomain(0, "celsius").pop)
    }

    // ── 辅助函数 ──

    @Test
    fun `formatTemp falls back to raw value for invalid input`() {
        assertEquals("abc°", formatTemp("abc", "celsius"))
        assertEquals("abc°", formatTemp("abc", "fahrenheit"))
    }

    @Test
    fun `moon phase maps known values and defaults for unknown`() {
        assertEquals(MoonPhase.NEW_MOON, "新月".toMoonPhase())
        assertEquals(MoonPhase.FIRST_QUARTER, "上弦月".toMoonPhase())
        assertEquals(MoonPhase.WANING_CRESCENT, "残月".toMoonPhase())
        assertEquals(MoonPhase.NEW_MOON, "未知月相".toMoonPhase())
    }
}
