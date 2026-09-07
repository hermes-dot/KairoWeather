package com.yuzheng.kairoweather.ui.weather.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yuzheng.kairoweather.R

/**
 * 和风天气现象代码 → 天气符号。
 *
 * 码表来源:dev.qweather.com/docs/api/weather/weather-conditions(2026-09 实抓),
 * 逐码核对,不凭记忆——注意 301 是"强阵雨"、313 冻雨、314-318 是雨量渐进档
 * ("小到中雨"…"大暴雨到特大暴雨"),它们不是夜间/雷电码。
 *
 * 昼夜语义:API 码表里真正自带"夜"的只有 150-154(晴夜/多云夜/少云夜/晴间多云夜/阴夜),
 * 雨雪类码不分昼夜。阵类降水(300/301 阵雨、407 阵雪)的夜间图需要天文夜信息,
 * 由 [WeatherGlyph.fromCode] 的 isNight 参数表达——PlayWeather 用 isDarkMode() 判夜
 * 是错误来源(深色模式≠夜晚,且白天开深色会把太阳画成月亮),我们只接受真值。
 *
 * 每个 case 回答"画面上该出现什么元素",不回答"冷不冷":雾与霾共用云+横线图
 * (视觉无法区分);901 冷/999 未知落到 NODATA("?")诚实占位——旧映射兜底成太阳,
 * 沙尘暴页面显示晴天是错误信息。
 */
enum class WeatherGlyph {
    SUN, MOON, CLOUD_DAY, CLOUD_NIGHT, OVERCAST,
    DRIZZLE, LIGHT_RAIN, RAIN, HEAVY_RAIN, DOWNPOUR,
    SHOWER, SHOWER_NIGHT, THUNDER, THUNDER_HAIL, SLEET,
    SNOW_FLURRY, SNOW_FLURRY_NIGHT, LIGHT_SNOW, SNOW, HEAVY_SNOW, SNOWSTORM,
    FOG, HAZE, SAND, DUST,
    NODATA;

    companion object {
        /**
         * @param isNight 天文夜(当前时刻位于当地日出-日落区间之外)。默认 false,
         *   仅影响阵类降水的图(300/301/407);150-154 夜间码不受影响——它们自带夜语义。
         *   接线点在 WeatherViewModel:那里已有目标城市时区的 OffsetDateTime(P2-5)
         *   与当天 sunrise/sunset,判夜是一行比较;不要退化成本机 LocalTime。
         */
        fun fromCode(code: String, isNight: Boolean = false): WeatherGlyph =
            when (code.toIntOrNull() ?: -1) {
                100 -> SUN
                101, 102, 103 -> CLOUD_DAY       // 多云/少云/晴间多云:云仍是画面主角
                104 -> OVERCAST
                150 -> MOON
                151, 152, 153 -> CLOUD_NIGHT
                154 -> OVERCAST                  // 阴本身无昼夜观感
                300, 301 -> if (isNight) SHOWER_NIGHT else SHOWER   // 301 强阵雨:雨量已超"阵雨图"表达力,但形态仍是阵
                302, 303 -> THUNDER              // 303 强雷阵雨并图:闪电已表达强度上限
                304 -> THUNDER_HAIL              // 雷阵雨伴有冰雹
                305, 314 -> LIGHT_RAIN           // 305 小雨、314 小到中雨
                306, 315 -> RAIN                 // 306 中雨、315 中到大雨
                307, 308, 316 -> HEAVY_RAIN      // 307 大雨、308 极端降雨、316 大到暴雨
                309 -> DRIZZLE                   // 毛毛雨/细雨:唯一值得专图的低强度档
                310, 311, 312, 317, 318 -> DOWNPOUR  // 暴雨→特大暴雨全谱共用最高雨量图
                313 -> SLEET                     // 冻雨
                399 -> RAIN                      // 泛化"雨"
                400, 408 -> LIGHT_SNOW           // 408 小到中雪就近取小雪档
                401, 409 -> SNOW                 // 409 中到大雪就近取中雪档
                402, 410 -> HEAVY_SNOW           // 410 大到暴雪取大雪档
                403 -> SNOWSTORM
                404, 405, 406 -> SLEET           // 雨夹雪/雨雪天气/阵雨夹雪:混合降水共用
                407 -> if (isNight) SNOW_FLURRY_NIGHT else SNOW_FLURRY
                456 -> SNOW_FLURRY               // 和风 Icons 站扩展码(天气现象表未列),前向兼容
                457 -> SNOW_FLURRY_NIGHT
                499 -> SNOW                      // 泛化"雪"
                500, 501, 509, 510, 514, 515 -> FOG     // 薄雾→特强浓雾:横线云图按"能不能看穿"分级,符号层面不再分
                502, 511, 512, 513 -> HAZE              // 霾→严重霾
                503, 507, 508 -> SAND                   // 扬沙/沙尘暴/强沙尘暴:共用风沙图
                504 -> DUST                             // 浮尘:静态悬浮,区别于 503 主动扬起
                900 -> SUN                              // 热:太阳是唯一诚实的可视形
                else -> NODATA                          // 901 冷/999 未知/脏数据:不装懂
            }
    }
}

/**
 * [WeatherGlyph] → drawable 资源。资源 x_* 共 26 张移植自 PlayWeather
 * (github.com/zhujiang521/PlayWeather, MIT, 作者朱江;pathData 未改动,
 * 每个 xml 文件头保留来源注释),本映射表按 KairoWeather 核对过的官方码表重写。
 */
@DrawableRes
internal fun WeatherGlyph.drawableRes(): Int = when (this) {
    WeatherGlyph.SUN -> R.drawable.x_sunny
    WeatherGlyph.MOON -> R.drawable.x_yewan_qing
    WeatherGlyph.CLOUD_DAY -> R.drawable.x_cloud
    WeatherGlyph.CLOUD_NIGHT -> R.drawable.x_cloudy_night
    WeatherGlyph.OVERCAST -> R.drawable.x_overcast
    WeatherGlyph.DRIZZLE -> R.drawable.x_small_rain
    WeatherGlyph.LIGHT_RAIN -> R.drawable.x_small_rain
    WeatherGlyph.RAIN -> R.drawable.x_middle_rain
    WeatherGlyph.HEAVY_RAIN -> R.drawable.x_big_rain
    WeatherGlyph.DOWNPOUR -> R.drawable.x_storm
    WeatherGlyph.SHOWER -> R.drawable.x_shower
    WeatherGlyph.SHOWER_NIGHT -> R.drawable.x_yejian_zhenyu
    WeatherGlyph.THUNDER -> R.drawable.x_thunder_rain
    WeatherGlyph.THUNDER_HAIL -> R.drawable.x_thunder_rain
    WeatherGlyph.SLEET -> R.drawable.x_rain_ice
    WeatherGlyph.SNOW_FLURRY -> R.drawable.x_snow_flurry
    WeatherGlyph.SNOW_FLURRY_NIGHT -> R.drawable.x_snow_flurry_night
    WeatherGlyph.LIGHT_SNOW -> R.drawable.x_light_snow
    WeatherGlyph.SNOW -> R.drawable.x_moderate_snow
    WeatherGlyph.HEAVY_SNOW -> R.drawable.x_heavy_snow
    WeatherGlyph.SNOWSTORM -> R.drawable.x_snow_storm
    WeatherGlyph.FOG -> R.drawable.x_fog
    WeatherGlyph.HAZE -> R.drawable.x_haze
    WeatherGlyph.SAND -> R.drawable.x_yangsha
    WeatherGlyph.DUST -> R.drawable.x_fuchen
    WeatherGlyph.NODATA -> R.drawable.x_nodata
}

/**
 * 多色天气符号。
 *
 * 为什么是 Image 而不是 Icon:Material `Icon` 会把图形拍平到单一 tint——
 * 这套图的辨识度一半来自配色(黄=太阳/闪电、蓝=雨滴、白=云体),
 * 用 Icon 等于把彩色照片变成印章。因此它不参与 colorScheme,
 * 深浅主题下同一资源直接渲染(云体近白,任何底色均可读)。
 *
 * 尺寸策略沿用旧 P2-C6 结论:位图由 painter 缓存,组合期零开销,不做映射缓存。
 */
@Composable
fun WeatherGlyphImage(
    code: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isNight: Boolean = false,
) {
    Image(
        painter = painterResource(WeatherGlyph.fromCode(code, isNight).drawableRes()),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
    )
}

// ── Preview ──

private val previewCodes = listOf(
    "100", "150", "101", "152", "104", "302", "306", "308", "310", "400", "403", "501", "507", "999",
)

@Preview(name = "Glyph - 深色底检阅", showBackground = true, backgroundColor = 0xFF202840)
@Composable
private fun WeatherGlyphDarkBgPreview() {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        previewCodes.forEach { WeatherGlyphImage(it, it, size = 36.dp) }
    }
}

@Preview(name = "Glyph - 浅色底检阅", showBackground = true, backgroundColor = 0xFFF2F4F8)
@Composable
private fun WeatherGlyphLightBgPreview() {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        previewCodes.forEach { WeatherGlyphImage(it, it, size = 36.dp) }
    }
}
