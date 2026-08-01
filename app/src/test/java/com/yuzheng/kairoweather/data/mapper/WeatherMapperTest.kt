package com.yuzheng.kairoweather.data.mapper

import com.yuzheng.kairoweather.data.model.QWeatherDaily
import com.yuzheng.kairoweather.data.model.QWeatherHourly
import com.yuzheng.kairoweather.data.model.QWeatherNow
import com.yuzheng.kairoweather.domain.model.MoonPhase
import com.yuzheng.kairoweather.domain.model.TemperatureUnit
import com.yuzheng.kairoweather.domain.model.toFeelLikeString
import com.yuzheng.kairoweather.domain.model.toPercentString
import com.yuzheng.kairoweather.domain.model.toTempString
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherMapperTest {

    // ── 当前天气 ──

    @Test
    fun `now toDomain parses numeric fields`() {
        val dto = QWeatherNow(
            temp = "24",
            feelsLike = "26",
            humidity = "60",
            text = "晴",
            windDir = "东北风",
            windScale = "3",
            icon = "100",
        )

        val domain = dto.toDomain()

        assertEquals(24.0, domain.tempCelsius, 0.0)
        assertEquals(26.0, domain.feelsLikeCelsius, 0.0)
        assertEquals(60, domain.humidityPct)
        assertEquals("东北风", domain.wind)
        assertEquals("100", domain.iconCode)
        assertEquals("晴", domain.condition)
    }

    @Test
    fun `now toDomain keeps negative temperatures as-is`() {
        val dto = QWeatherNow(temp = "-10", feelsLike = "-5")

        val domain = dto.toDomain()

        assertEquals(-10.0, domain.tempCelsius, 0.0)
        assertEquals(-5.0, domain.feelsLikeCelsius, 0.0)
    }

    // ── 逐小时 ──

    @Test
    fun `hourly toDomain extracts local time and numeric pop`() {
        val dto = QWeatherHourly(
            fxTime = "2021-02-16T15:00+08:00",
            temp = "20",
            pop = "30",
            icon = "305",
        )

        val domain = dto.toDomain()

        assertEquals("15:00", domain.time)
        assertEquals(30, domain.popPct)
        assertEquals(20.0, domain.tempCelsius, 0.0)
        assertEquals("305", domain.iconCode)
        assertEquals(false, domain.isNow)
        // 原始 fxTime 透传到 rawTime,供 ViewModel 做时区感知的 isNow 判断
        assertEquals("2021-02-16T15:00+08:00", domain.rawTime)
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

        val domain = dto.toDomain(0)

        assertEquals("今天", domain.date)
        assertEquals(30.0, domain.highTempCelsius, 0.0)
        assertEquals(22.0, domain.lowTempCelsius, 0.0)
    }

    @Test
    fun `daily toDomain formats future date with weekday`() {
        val date = LocalDate.of(2026, 8, 1)
        val dto = QWeatherDaily(fxDate = date.toString())

        val domain = dto.toDomain(1)

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

        val domain = dto.toDomain(0)

        assertEquals(35, domain.popPct)
        assertEquals(6, domain.uvIndexValue)
        assertEquals(MoonPhase.FULL_MOON, domain.moonPhase)
        assertEquals("05:00", domain.sunrise)
        assertEquals("19:00", domain.sunset)
    }

    @Test
    fun `daily pop zero or blank stays zero`() {
        assertEquals(0, QWeatherDaily(pop = "0").toDomain(0).popPct)
        assertEquals(0, QWeatherDaily(pop = "").toDomain(0).popPct)
    }

    @Test
    fun `daily pop handles negative and non numeric input as zero`() {
        assertEquals(0, QWeatherDaily(pop = "-5").toDomain(0).popPct)
        assertEquals(0, QWeatherDaily(pop = "abc").toDomain(0).popPct)
        assertEquals(5, QWeatherDaily(pop = "5").toDomain(0).popPct)
    }

    // ── 辅助函数 ──

    @Test
    fun `moon phase maps known values and defaults for unknown`() {
        assertEquals(MoonPhase.NEW_MOON, "新月".toMoonPhase())
        assertEquals(MoonPhase.FIRST_QUARTER, "上弦月".toMoonPhase())
        assertEquals(MoonPhase.WANING_CRESCENT, "残月".toMoonPhase())
        assertEquals(MoonPhase.NEW_MOON, "未知月相".toMoonPhase())
    }

    @Test
    fun `moon phase maps all eight phases`() {
        assertEquals(MoonPhase.NEW_MOON, "新月".toMoonPhase())
        assertEquals(MoonPhase.WAXING_CRESCENT, "蛾眉月".toMoonPhase())
        assertEquals(MoonPhase.FIRST_QUARTER, "上弦月".toMoonPhase())
        assertEquals(MoonPhase.WAXING_GIBBOUS, "盈凸月".toMoonPhase())
        assertEquals(MoonPhase.FULL_MOON, "满月".toMoonPhase())
        assertEquals(MoonPhase.WANING_GIBBOUS, "亏凸月".toMoonPhase())
        assertEquals(MoonPhase.LAST_QUARTER, "下弦月".toMoonPhase())
        assertEquals(MoonPhase.WANING_CRESCENT, "残月".toMoonPhase())
        assertEquals(MoonPhase.NEW_MOON, "".toMoonPhase())
    }

    // ── 空值 / 非法值边界 ──

    @Test
    fun `now toDomain falls back to zero for empty or invalid fields`() {
        val dto = QWeatherNow(
            temp = "",
            feelsLike = "abc",
            humidity = "",
            windDir = "",
            pressure = "x",
        )

        val domain = dto.toDomain()

        assertEquals(0.0, domain.tempCelsius, 0.0)
        assertEquals(0.0, domain.feelsLikeCelsius, 0.0)
        assertEquals(0, domain.humidityPct)
        assertEquals(0, domain.pressureHpa)
        assertEquals("", domain.wind)
    }

    @Test
    fun `hourly toDomain handles missing T or timezone suffix`() {
        assertEquals("15:00", QWeatherHourly(fxTime = "2021-02-16T15:00").toDomain().time)
        assertEquals("15:00", QWeatherHourly(fxTime = "15:00").toDomain().time)
        assertEquals("", QWeatherHourly(fxTime = "").toDomain().time)
    }

    @Test
    fun `daily toDomain handles blank or invalid fields`() {
        val domain = QWeatherDaily(
            fxDate = "",
            tempMax = "abc",
            tempMin = "",
            pop = "abc",
            moonPhase = "?",
        ).toDomain(0)

        assertEquals("今天", domain.date)
        assertEquals(0.0, domain.highTempCelsius, 0.0)
        assertEquals(0.0, domain.lowTempCelsius, 0.0)
        assertEquals(0, domain.popPct)
        assertEquals(MoonPhase.NEW_MOON, domain.moonPhase)
    }

    // ── 展示格式化工具(原 formatTemp 迁移) ──

    @Test
    fun `toTempString handles zero negative and fractional celsius`() {
        assertEquals("0°", 0.0.toTempString(TemperatureUnit.CELSIUS))
        assertEquals("-3°", (-3.9).toTempString(TemperatureUnit.CELSIUS))
        assertEquals("24°", 24.7.toTempString(TemperatureUnit.CELSIUS))
        assertEquals("0°", (-0.5).toTempString(TemperatureUnit.CELSIUS))
    }

    @Test
    fun `toTempString converts fahrenheit boundaries correctly`() {
        assertEquals("32°", 0.0.toTempString(TemperatureUnit.FAHRENHEIT))
        assertEquals("-40°", (-40.0).toTempString(TemperatureUnit.FAHRENHEIT))
        assertEquals("212°", 100.0.toTempString(TemperatureUnit.FAHRENHEIT))
        assertEquals("14°", (-10.0).toTempString(TemperatureUnit.FAHRENHEIT))
        assertEquals("76°", 24.7.toTempString(TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun `toTempString handles large numeric values`() {
        assertEquals("100°", 100.0.toTempString(TemperatureUnit.CELSIUS))
        assertEquals("212°", 100.0.toTempString(TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun `toPercentString formats percentage`() {
        assertEquals("60%", 60.toPercentString())
        assertEquals("0%", 0.toPercentString())
    }

    @Test
    fun `toFeelLikeString prefixes feel like label`() {
        assertEquals("体感26°", 26.0.toFeelLikeString(TemperatureUnit.CELSIUS))
        assertEquals("体感78°", 26.0.toFeelLikeString(TemperatureUnit.FAHRENHEIT))
        assertEquals("体感23°", (-5.0).toFeelLikeString(TemperatureUnit.FAHRENHEIT))
    }

    // ── formatFxDate 边界 ──

    @Test
    fun `formatFxDate passes through short or empty input`() {
        assertEquals("", formatFxDate(""))
        assertEquals("2026", formatFxDate("2026"))
        assertEquals("08-01", formatFxDate("08-01"))
    }

    @Test
    fun `formatFxDate renders a known weekday`() {
        assertEquals("周一 1/1", formatFxDate("2024-01-01"))
    }

    @Test
    fun `formatFxDate passes through malformed inputs instead of crashing`() {
        // 长度≥10 但格式非法/日期非法,应原样返回而非抛 NumberFormatException
        assertEquals("2026-08-01T00:00", formatFxDate("2026-08-01T00:00"))
        assertEquals("2026-08-01T00:00+08:00", formatFxDate("2026-08-01T00:00+08:00"))
        assertEquals("2026-08", formatFxDate("2026-08"))
        assertEquals("2026-13-40", formatFxDate("2026-13-40"))
        assertEquals("2026-00-10", formatFxDate("2026-00-10"))
        assertEquals("abcd-ef-gh", formatFxDate("abcd-ef-gh"))
        assertEquals("2026-08-01-extra", formatFxDate("2026-08-01-extra"))
    }
}
