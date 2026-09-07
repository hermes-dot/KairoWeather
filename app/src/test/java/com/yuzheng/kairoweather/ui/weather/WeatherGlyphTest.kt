package com.yuzheng.kairoweather.ui.weather

import com.yuzheng.kairoweather.ui.weather.components.WeatherGlyph
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * WeatherGlyph 映射表单测。
 *
 * fromCode 不引用 R,是纯函数,JVM 即可测。
 * 期望值对照和风官方《天气现象》码表(dev.qweather.com/docs/api/weather/weather-conditions):
 * 断言的不是"码→枚举"的实现细节,而是"码→可视形"的语义决定
 * ——每个期望值旁边注释官方 text,防止未来改表时误伤。
 */
class WeatherGlyphTest {

    @Test
    fun `晴天系 - 100与900画太阳,150晴夜画月亮`() {
        assertEquals(WeatherGlyph.SUN, WeatherGlyph.fromCode("100"))
        assertEquals(WeatherGlyph.SUN, WeatherGlyph.fromCode("900"))
        assertEquals(WeatherGlyph.MOON, WeatherGlyph.fromCode("150"))
    }

    @Test
    fun `云系 - 多云少云晴间多云为白天云,151至153夜云,阴不分昼夜`() {
        assertEquals(WeatherGlyph.CLOUD_DAY, WeatherGlyph.fromCode("101"))
        assertEquals(WeatherGlyph.CLOUD_DAY, WeatherGlyph.fromCode("102"))
        assertEquals(WeatherGlyph.CLOUD_DAY, WeatherGlyph.fromCode("103"))
        assertEquals(WeatherGlyph.CLOUD_NIGHT, WeatherGlyph.fromCode("151"))
        assertEquals(WeatherGlyph.CLOUD_NIGHT, WeatherGlyph.fromCode("153"))
        assertEquals(WeatherGlyph.OVERCAST, WeatherGlyph.fromCode("104"))
        assertEquals(WeatherGlyph.OVERCAST, WeatherGlyph.fromCode("154"))
    }

    @Test
    fun `雨量梯度 - 小雨中雨大雨暴雨是四张不同的图,强度信息不坍缩`() {
        assertEquals(WeatherGlyph.DRIZZLE, WeatherGlyph.fromCode("309"))     // 毛毛雨/细雨
        assertEquals(WeatherGlyph.LIGHT_RAIN, WeatherGlyph.fromCode("305"))  // 小雨
        assertEquals(WeatherGlyph.RAIN, WeatherGlyph.fromCode("306"))        // 中雨
        assertEquals(WeatherGlyph.HEAVY_RAIN, WeatherGlyph.fromCode("307"))  // 大雨
        assertEquals(WeatherGlyph.DOWNPOUR, WeatherGlyph.fromCode("310"))    // 暴雨
        // 渐进档(314 小到中雨 → 318 大暴雨到特大暴雨)就近入档,不得比前档更轻
        assertEquals(WeatherGlyph.LIGHT_RAIN, WeatherGlyph.fromCode("314"))
        assertEquals(WeatherGlyph.RAIN, WeatherGlyph.fromCode("315"))
        assertEquals(WeatherGlyph.HEAVY_RAIN, WeatherGlyph.fromCode("316"))
        assertEquals(WeatherGlyph.DOWNPOUR, WeatherGlyph.fromCode("317"))
        assertEquals(WeatherGlyph.DOWNPOUR, WeatherGlyph.fromCode("318"))
    }

    @Test
    fun `阵类降水的夜图只由天文夜决定,与传入码无关`() {
        assertEquals(WeatherGlyph.SHOWER, WeatherGlyph.fromCode("300"))            // 阵雨·白天
        assertEquals(WeatherGlyph.SHOWER_NIGHT, WeatherGlyph.fromCode("300", isNight = true))
        assertEquals(WeatherGlyph.SHOWER_NIGHT, WeatherGlyph.fromCode("301", isNight = true)) // 强阵雨·夜
        assertEquals(WeatherGlyph.SNOW_FLURRY, WeatherGlyph.fromCode("407"))       // 阵雪·白天
        assertEquals(WeatherGlyph.SNOW_FLURRY_NIGHT, WeatherGlyph.fromCode("407", isNight = true))
        // 稳定性降水不分昼夜:小雨夜里还是小雨图,不该冒出月亮
        assertEquals(WeatherGlyph.LIGHT_RAIN, WeatherGlyph.fromCode("305", isNight = true))
    }

    @Test
    fun `雷电谱 - 302与303闪电图,304雷伴冰雹`() {
        assertEquals(WeatherGlyph.THUNDER, WeatherGlyph.fromCode("302"))
        assertEquals(WeatherGlyph.THUNDER, WeatherGlyph.fromCode("303"))
        assertEquals(WeatherGlyph.THUNDER_HAIL, WeatherGlyph.fromCode("304"))
    }

    @Test
    fun `雪量梯度与混合降水`() {
        assertEquals(WeatherGlyph.LIGHT_SNOW, WeatherGlyph.fromCode("400"))   // 小雪
        assertEquals(WeatherGlyph.SNOW, WeatherGlyph.fromCode("401"))         // 中雪
        assertEquals(WeatherGlyph.HEAVY_SNOW, WeatherGlyph.fromCode("402"))   // 大雪
        assertEquals(WeatherGlyph.SNOWSTORM, WeatherGlyph.fromCode("403"))    // 暴雪
        assertEquals(WeatherGlyph.SNOW, WeatherGlyph.fromCode("499"))         // 泛化雪
        // 404 雨夹雪/405 雨雪/406 阵雨夹雪/313 冻雨 → 混合降水谱共用雨夹雪图
        listOf("313", "404", "405", "406").forEach {
            assertEquals("码 $it", WeatherGlyph.SLEET, WeatherGlyph.fromCode(it))
        }
    }

    @Test
    fun `能见度谱 - 雾系霾系风沙系各归各图`() {
        listOf("500", "501", "509", "510", "514", "515").forEach {
            assertEquals("码 $it", WeatherGlyph.FOG, WeatherGlyph.fromCode(it))
        }
        listOf("502", "511", "512", "513").forEach {
            assertEquals("码 $it", WeatherGlyph.HAZE, WeatherGlyph.fromCode(it))
        }
        assertEquals(WeatherGlyph.SAND, WeatherGlyph.fromCode("503"))   // 扬沙
        assertEquals(WeatherGlyph.SAND, WeatherGlyph.fromCode("507"))   // 沙尘暴
        assertEquals(WeatherGlyph.SAND, WeatherGlyph.fromCode("508"))   // 强沙尘暴
        assertEquals(WeatherGlyph.DUST, WeatherGlyph.fromCode("504"))   // 浮尘
    }

    @Test
    fun `未知码与脏数据一律 NODATA,绝不兜底成晴天`() {
        // 999 是官方"未知",901 是"冷"(无通用可视形)——画太阳是在撒谎
        assertEquals(WeatherGlyph.NODATA, WeatherGlyph.fromCode("999"))
        assertEquals(WeatherGlyph.NODATA, WeatherGlyph.fromCode("901"))
        assertEquals(WeatherGlyph.NODATA, WeatherGlyph.fromCode(""))
        assertEquals(WeatherGlyph.NODATA, WeatherGlyph.fromCode("abc"))
        assertEquals(WeatherGlyph.NODATA, WeatherGlyph.fromCode("700"))
    }

    @Test
    fun `整表无空洞 - 官方码表每个码都必须有非默认归处`() {
        // 严格取自已抓取的官方天气现象表;注意 5xx 段是稀疏的(505/506 不存在),不可用连续区间
        val officialCodes = listOf(
            "100", "101", "102", "103", "104",
            "150", "151", "152", "153", "154",
            "300", "301", "302", "303", "304", "305", "306", "307", "308", "309",
            "310", "311", "312", "313", "314", "315", "316", "317", "318", "399",
            "400", "401", "402", "403", "404", "405", "406", "407", "408", "409", "410",
            "456", "457", "499",
            "500", "501", "502", "503", "504", "507", "508", "509", "510",
            "511", "512", "513", "514", "515",
            "900", "901", "999",
        )
        officialCodes.forEach { code ->
            val glyph = WeatherGlyph.fromCode(code)
            if (code != "901" && code != "999") {
                assertEquals("码 $code 不应落 NODATA", true, glyph != WeatherGlyph.NODATA)
            }
        }
    }

    @Test
    fun `每个枚举值都有资源映射且互不遗漏`() {
        // drawableRes() 引用 R,不能在 JVM 单测调用;这里校验枚举本身形状稳定:
        // 值数量变化(增删 glyph)时此测试红,提醒同步 drawableRes when 表与本文档。
        assertEquals(26, WeatherGlyph.entries.size)
    }
}
