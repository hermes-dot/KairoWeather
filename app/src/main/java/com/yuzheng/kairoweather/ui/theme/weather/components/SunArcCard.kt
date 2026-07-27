package com.yuzheng.kairoweather.ui.theme.weather.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.yuzheng.kairoweather.R

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

            Spacer(Modifier.height(4.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val startX = 50f
                val endX = size.width - 50f
                val midX = (startX + endX) / 2
                val topY = 20f
                val bottomY = size.height - 36f

                // ── 1. 虚线弧轨 ──
                val path = Path().apply {
                    moveTo(startX, bottomY)
                    quadraticTo(midX, topY, endX, bottomY)
                }
                drawPath(
                    path = path,
                    color = Color(0xFFAAAAAA),
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)),
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
                    radius = 26f,
                    center = Offset(sunX, sunY),
                )
                drawCircle(
                    color = Color(0xFFFFB300),
                    radius = 14f,
                    center = Offset(sunX, sunY),
                )

                // ── 4. 标签 ──
                val labelPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 34f
                    color = 0xFF555555.toInt()
                    typeface = Typeface.DEFAULT_BOLD
                }

                drawContext.canvas.nativeCanvas.drawText(
                    sunrise,
                    startX,
                    bottomY + 28f,
                    labelPaint.apply { textAlign = Paint.Align.LEFT },
                )
                drawContext.canvas.nativeCanvas.drawText(
                    sunset,
                    endX,
                    bottomY + 28f,
                    labelPaint.apply { textAlign = Paint.Align.RIGHT },
                )
            }
        }
    }
}
