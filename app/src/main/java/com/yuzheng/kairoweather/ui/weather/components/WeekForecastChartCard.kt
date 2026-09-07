package com.yuzheng.kairoweather.ui.weather.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yuzheng.kairoweather.domain.model.DailyForecast
import com.yuzheng.kairoweather.domain.model.MoonPhase
import com.yuzheng.kairoweather.domain.model.TemperatureUnit
import com.yuzheng.kairoweather.domain.model.toTempString
import com.yuzheng.kairoweather.ui.theme.KairoWeatherTheme

/**
 * 7 日预报卡(温度区间条版):每行 = 日期 · 图标 · 最低温 · [温度条] · 最高温。
 *
 * 数据前置:整周温度域 [weekMin, weekMax] 在这里从 [daily] 现算——
 * 温度条没有独立数据源,它的意义就是"把这一天的区间放回全周的尺度里",
 * 所以域必须来自同一个列表,在调用方算好反而容易传错。
 */
@Composable
fun WeekForecastChartCard(
    daily: List<DailyForecast>,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier,
    nowTempCelsius: Double? = null,
) {
    if (daily.isEmpty()) return

    // 全周温度域:决定每行条形的坐标映射。coerceAtLeast(1) 是关键防御——
    // 若全周高低温带完全重合(span=0),归一化除法会得 NaN/Infinity,
    // 整个 Canvas 内容会安静地画不出来(PlayWeather 原版就有这个 bug)。
    val weekMin = daily.minOf { it.lowTempCelsius }
    val weekMax = daily.maxOf { it.highTempCelsius }
    val span = (weekMax - weekMin).coerceAtLeast(1.0)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = "7 日预报",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp,
                ),
            )
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // 固定 7 行,forEach 一次组合(与 DailyForecastList 同一取舍:
            // 父级已是 LazyColumn,行数可预期就不嵌二级懒加载)
            daily.forEachIndexed { index, day ->
                WeekForecastRow(
                    day = day,
                    unit = unit,
                    weekMin = weekMin,
                    span = span,
                    // 白点(当前实测温度)只钉在"今天"这一行。
                    // PlayWeather 原版每行都画同温度的点,语义重复;
                    // "现在"是一个时间点,它属于今天。
                    nowTempCelsius = if (index == 0) nowTempCelsius else null,
                )
            }
        }
    }
}

@Composable
private fun WeekForecastRow(
    day: DailyForecast,
    unit: TemperatureUnit,
    weekMin: Double,
    span: Double,
    nowTempCelsius: Double?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = day.date.substring(0,2),
            modifier = Modifier.width(56.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        WeatherGlyphImage(
            code = day.iconCode,
            contentDescription = day.description,
            size = 22.dp,
        )
        // 左温度:死宽 + 右对齐。温度条两侧的数字要"贴着"各自端点,
        // weight 比例分宽适合内容列(日期/描述),对齐锚点列要给死宽。
        Text(
            text = day.lowTempCelsius.toTempString(unit),
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TemperatureBar(
            low = day.lowTempCelsius,
            high = day.highTempCelsius,
            weekMin = weekMin,
            span = span,
            nowTempCelsius = nowTempCelsius,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
        )
        Text(
            text = day.highTempCelsius.toTempString(unit),
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Start,
        )
    }
}

/**
 * 温度区间条:灰色底槽(全周域)+ 渐变区间段(当日域)+ 当前温度白点。
 *
 * 全部坐标都过同一个 xOf() 映射——数据域 [weekMin, weekMin+span] → 像素域 [0, width]。
 * 和 AirQualityCard 的 `aqi/500` 对照记:AQI 域天然从 0 开始,直接除域宽;
 * 温度有负数,必须**先减域起点**再归一化,否则 -5°C 会算出负坐标画到屏幕外。
 */
@Composable
private fun TemperatureBar(
    low: Double,
    high: Double,
    weekMin: Double,
    span: Double,
    nowTempCelsius: Double?,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val dotColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier.height(16.dp)) {
        val barHeight = 6.dp.toPx()          // DrawScope 本身就是 Density,dp→px 直接换算
        val y = size.height / 2f             // 显式垂直居中;原版画在 y=0 靠"Canvas 默认不裁剪"侥幸露出半条

        fun xOf(temp: Double): Float =
            size.width * ((temp - weekMin) / span).toFloat()

        // ① 底槽:整条 = 全周温度域,给读者"尺子"
        drawLine(
            color = trackColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = barHeight,
            cap = StrokeCap.Round,
        )

        // ② 当日区间段:渐变从该段左端温度的颜色过渡到右端,
        // 冷→暖的连续着色让"哪几天更热"不读数字也能感知
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(temperatureColor(low), temperatureColor(high)),
                start = Offset(xOf(low), y),
                end = Offset(xOf(high), y),
            ),
            start = Offset(xOf(low), y),
            end = Offset(xOf(high), y),
            strokeWidth = barHeight,
            cap = StrokeCap.Round,
        )

        // ③ 当前温度白点:钳进当日区间内——凌晨时刻"现在"属于旧的一天,
        // 实测温度可能落在今天预报高低带之外,不钳会点在底槽上冒充"最冷值"
        nowTempCelsius?.let { now ->
            val clamped = now.coerceIn(low, high)
            drawCircle(
                color = dotColor,
                radius = barHeight * 0.9f,
                center = Offset(xOf(clamped), y),
            )
        }
    }
}

/**
 * 温度→颜色的粗粒度色阶。和 AQI 同理:这是"冷暖"的物理语义而非品牌色,
 * 不跟随 MaterialTheme,深浅两模式共用;取整档而非连续插值,
 * 因为相邻两档的差异在 6dp 高的条上肉眼分不出,徒增噪声。
 */
private fun temperatureColor(tempCelsius: Double): Color = when {
    tempCelsius < -10 -> Color(0xFF1A5CF9)  // 严寒
    tempCelsius < 0 -> Color(0xFF41AEFA)    // 冰点下
    tempCelsius < 10 -> Color(0xFF56C9CD)   // 冷
    tempCelsius < 20 -> Color(0xFF97C98E)   // 凉
    tempCelsius < 28 -> Color(0xFFF7C422)   // 暖
    tempCelsius < 35 -> Color(0xFFFD8A0B)   // 热
    else -> Color(0xFFF83C1E)               // 酷暑
}

// ── Preview ──

private val previewWeek = listOf(
    DailyForecast("今天", 5.0, -3.0, "100", "晴", 0, 3, MoonPhase.WAXING_GIBBOUS, "07:10", "17:06"),
    DailyForecast("周二 9/8", 8.0, 1.0, "100", "晴", 0, 3, MoonPhase.FULL_MOON, "07:11", "17:04"),
    DailyForecast("周三 9/9", 12.0, 2.0, "101", "多云", 10, 4, MoonPhase.WANING_GIBBOUS, "07:12", "17:03"),
    DailyForecast("周四 9/10", 7.0, -3.0, "104", "阴", 20, 2, MoonPhase.WANING_GIBBOUS, "07:13", "17:01"),
    DailyForecast("周五 9/11", 11.0, -4.0, "305", "小雨", 70, 2, MoonPhase.LAST_QUARTER, "07:14", "17:00"),
    DailyForecast("周六 9/12", 12.0, -2.0, "101", "多云", 30, 4, MoonPhase.WANING_CRESCENT, "07:15", "16:58"),
    DailyForecast("周日 9/13", 10.0, -2.0, "300", "阵雨", 60, 3, MoonPhase.WANING_CRESCENT, "07:16", "16:57"),
)

@Preview(name = "WeekForecastChartCard - Light", showBackground = true)
@Composable
private fun WeekForecastChartCardPreview() {
    KairoWeatherTheme {
        WeekForecastChartCard(
            daily = previewWeek,
            unit = TemperatureUnit.CELSIUS,
            nowTempCelsius = 3.0,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(
    name = "WeekForecastChartCard - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun WeekForecastChartCardDarkPreview() {
    KairoWeatherTheme {
        WeekForecastChartCard(
            daily = previewWeek,
            unit = TemperatureUnit.CELSIUS,
            nowTempCelsius = 3.0,
            modifier = Modifier.padding(16.dp),
        )
    }
}
