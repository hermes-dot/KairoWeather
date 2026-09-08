package com.yuzheng.kairoweather.ui.weather.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yuzheng.kairoweather.R
import com.yuzheng.kairoweather.domain.model.AirQuality
import com.yuzheng.kairoweather.domain.model.healthAdviceLines
import com.yuzheng.kairoweather.ui.theme.KairoWeatherTheme

/**
 * 空气质量卡:标题 → 大数值 → 说明 → 刻度条 → 健康建议 五层信息结构。
 *
 * 设计约定(与 [WindCard]/[HumidityCard] 等一致):组件只接收"已解析好的原始值",
 * 不做网络、不做字符串转数字——AQI 的 DTO→domain 解析发生在 mapper 边界,
 * 这里拿到的一定是合法 Int,所以不需要任何 ?: 兜底。
 */
@Composable
fun AirQualityCard(
    air: AirQuality,
    modifier: Modifier = Modifier
) {
    val aqi = air.aqi
    val healthLines = air.healthAdviceLines()
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.card_padding_horizontal),
                    vertical = 12.dp,
                ),
        ) {
            // ① 标题:小字 + 次要色,主动"退后"给数值让位
            Text(
                text = "空气质量",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.card_label_spacing)))

            // ② 主数值:大字号 + 按污染等级着色。颜色是这张卡的"状态语义",
            // 一眼扫过去不用读文字就知道空气好坏
            Text(
                text = "$aqi · ${air.category}",
                style = MaterialTheme.typography.headlineSmall,
                color = aqiStatusColor(aqi),
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.card_value_spacing)))

            // ③ 说明:回到次要色,给"数值→结论"补一句依据。API 给了健康影响
            // (effect)时以它优先——"空气质量令人满意"比"当前 AQI 为 33"信息量大,
            // 首要污染物并进括号兜底(优级下 API 常无首要污染物)
            val pollutantSuffix = air.primaryPollutant
                .takeIf { it.isNotBlank() && it != "-" }
                ?.let { ",首要污染物 $it" } ?: ""
            Text(
                text = air.healthEffect.ifBlank { "当前 AQI(CN) 为 $aqi$pollutantSuffix" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            // ④ 可视化:整卡唯一需要"画"的部分,见 AqiScaleBar
            AqiScaleBar(aqi)

            // ⑤ 健康建议:来自 API 的分人群指引。整段可缺席(字段可能为空),
            // 出现时轻分隔 + 小字,视觉权重最低——数值和颜色仍是主角
            if (healthLines.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                healthLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

/**
 * AQI 刻度条:渐变底 + 白色游标。
 *
 * 为什么底色不走 Canvas 而是 Modifier.background:渐变的"色标停靠点"本身携带数据语义
 * (0.1 = AQI 50 良界、0.2 = 100 轻度界……),这是"背景"不是"内容",
 * 交给 Modifier 层画,Canvas 只负责那个会动的白点——两层各管一半,职责最干净。
 */
@Composable
private fun AqiScaleBar(aqi: Int, modifier: Modifier = Modifier) {
    // 域→画布的归一化:先钳到 [0,500] 防脏数据,再算比例。
    // AQI 域天然从 0 开始,所以公式是 v/domainMax;若域有负数(如温度)要先减起点,
    // 对照 WeekForecastChartCard 的 xOf() 看两条公式的差异。
    val fraction = (aqi.coerceIn(0, 500)) / 500f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .background(
                brush = Brush.horizontalGradient(colorStops = AqiColorStops),
                shape = RoundedCornerShape(percent = 50),
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.height / 2f
            // 白点钳制在 [r, width-r]:两端半圆是"圆头"不是"平顶",
            // 不钳的话 AQI=0 或 500 时圆点会被裁成半个,看着像 bug
            val x = (size.width * fraction).coerceIn(radius, size.width - radius)
            // drawCircle 就是"画实心圆"的直白写法。
            // PlayWeather 原文用 drawPoints(PointMode.Points)+strokeWidth+cap=Round
            // 达到同样效果(线帽圆角 × 粗线宽 = 圆),等价但绕,学习期两种都值得见。
            drawCircle(
                color = Color.White,
                radius = radius,
                center = Offset(x, size.height / 2f),
            )
        }
    }
}

/**
 * 国标六级色。刻意不跟随 MaterialTheme——AQI 颜色是法规语义(绿=优 全球通行),
 * 不是品牌色,深色模式下也不该变,所以放本文件常量而非 theme/Color.kt。
 */
private fun aqiStatusColor(aqi: Int): Color = when {
    aqi <= 50 -> Color(0xFF689F38)   // 优
    aqi <= 100 -> Color(0xFFF9A825)  // 良
    aqi <= 150 -> Color(0xFFEF6C00)  // 轻度污染
    aqi <= 200 -> Color(0xFFD32F2F)  // 中度污染
    aqi <= 300 -> Color(0xFF7B1FA2)  // 重度污染
    else -> Color(0xFF6D1B1B)        // 严重污染
}

private val AqiColorStops = arrayOf(
    0.0f to Color(0xFF8BC34A),  // 优      ≤50
    0.1f to Color(0xFFFFEB3B),  // 良      ≤100   ← 停靠点位置 = 50/500,编码国标分段
    0.2f to Color(0xFFFF9800),  // 轻度    ≤150
    0.3f to Color(0xFFF44336),  // 中度    ≤200
    0.4f to Color(0xFF9C27B0),  // 重度    ≤300   ← 300 之后域宽只剩 200 但颜色区间更"贵",
    1.0f to Color(0xFF8F0000),  // 严重    >300      非线性停靠模拟污染越界越危险观感
)

// ── Preview ──

@Preview(name = "AirQualityCard - 优", showBackground = true)
@Composable
private fun AirQualityCardGoodPreview() {
    KairoWeatherTheme {
        AirQualityCard(
            air = AirQuality(
                aqi = 23, category = "优", primaryPollutant = "pm10",
                healthEffect = "空气质量令人满意，基本无空气污染。",
                adviceGeneral = "各类人群可正常活动。",
                adviceSensitive = "各类人群可正常活动。",
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "AirQualityCard - 中度污染", showBackground = true)
@Composable
private fun AirQualityCardMidPreview() {
    KairoWeatherTheme {
        AirQualityCard(
            air = AirQuality(
                aqi = 168, category = "中度污染", primaryPollutant = "o3",
                healthEffect = "进一步加剧易感人群症状，可能对健康人群心脏、呼吸系统有影响。",
                adviceGeneral = "一般人群适量减少户外运动。",
                adviceSensitive = "儿童、老年人及心脏病、呼吸系统疾病患者避免长时间、高强度的户外锻炼。",
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "AirQualityCard - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AirQualityCardDarkPreview() {
    KairoWeatherTheme {
        AirQualityCard(
            air = AirQuality(
                aqi = 320, category = "严重污染", primaryPollutant = "pm2.5",
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
