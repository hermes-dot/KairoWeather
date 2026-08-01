package com.yuzheng.kairoweather.ui.weather.components

import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yuzheng.kairoweather.R
import com.yuzheng.kairoweather.ui.theme.KairoWeatherTheme

@Composable
fun SunArcCard(
    sunrise: String,
    sunset: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding_horizontal)),
        ) {
            Text("日出 & 日落", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(8.dp))

            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            val trackColor = MaterialTheme.colorScheme.outline
            // P2-C4: 文字尺寸用 LocalDensity 把 dp 换算成像素,避免高密度屏上偏小
            val labelTextSize = with(LocalDensity.current) { 13.dp.toPx() }
            val labelPaint = remember(labelColor, labelTextSize) {
                Paint().apply {
                    isAntiAlias = true
                    textSize = labelTextSize
                    color = labelColor.toArgb()
                    typeface = Typeface.DEFAULT_BOLD
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                // P2-C4: DrawScope 实现 Density,坐标/半径/线宽统一用 dp.toPx() 换算,适配高密度屏
                val startX = 24.dp.toPx()
                val endX = size.width - 24.dp.toPx()
                val midX = (startX + endX) / 2
                val topY = 12.dp.toPx()
                val bottomY = size.height - 30.dp.toPx()

                // ── 1. 虚线弧轨 ──
                val path = Path().apply {
                    moveTo(startX, bottomY)
                    quadraticTo(midX, topY, endX, bottomY)
                }
                drawPath(
                    path = path,
                    color = trackColor,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())),
                    ),
                )

                // ── 2. 贝塞尔插值：太阳位置 ──
                val t = progress
                val oneMinusT = 1f - t
                val sunX = oneMinusT * oneMinusT * startX + 2 * oneMinusT * t * midX + t * t * endX
                val sunY = oneMinusT * oneMinusT * bottomY + 2 * oneMinusT * t * topY + t * t * bottomY

                // ── 3. 光晕 + 太阳 ──
                drawCircle(
                    color = Color(0x44FFB300),
                    radius = 10.dp.toPx(),
                    center = Offset(sunX, sunY),
                )
                drawCircle(
                    color = Color(0xFFFFB300),
                    radius = 5.dp.toPx(),
                    center = Offset(sunX, sunY),
                )

                // ── 4. 标签 ──
                drawContext.canvas.nativeCanvas.drawText(
                    sunrise,
                    startX,
                    bottomY + 18.dp.toPx(),
                    labelPaint.apply { textAlign = Paint.Align.LEFT },
                )
                drawContext.canvas.nativeCanvas.drawText(
                    sunset,
                    endX,
                    bottomY + 18.dp.toPx(),
                    labelPaint.apply { textAlign = Paint.Align.RIGHT },
                )
            }
        }
    }
}

// ── Preview ──

@Preview(name = "SunArcCard - Light", showBackground = true)
@Composable
private fun SunArcCardPreview() {
    KairoWeatherTheme {
        SunArcCard(
            sunrise = "05:00",
            sunset = "19:00",
            progress = 0.5f,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "SunArcCard - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SunArcCardDarkPreview() {
    KairoWeatherTheme {
        SunArcCard(
            sunrise = "06:12",
            sunset = "18:35",
            progress = 0.25f,
            modifier = Modifier.padding(16.dp),
        )
    }
}
