package com.yuzheng.kairoweather.ui.weather.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yuzheng.kairoweather.domain.model.HourlyForecast
import com.yuzheng.kairoweather.domain.model.TemperatureUnit
import com.yuzheng.kairoweather.domain.model.toPercentString
import com.yuzheng.kairoweather.domain.model.toTempString
import com.yuzheng.kairoweather.ui.theme.KairoWeatherTheme

@Composable
fun HourlyForecastRow(
    items: List<HourlyForecast>,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // P2-10: key 用 index 前缀,避免 API 返回重复 fxTime 时抛 "Key already used"
            itemsIndexed(items, key = { index, hour -> "$index-${hour.time}" }) { index, hour ->
                HourlyItem(hour, unit, Modifier.width(64.dp))
                if (index < items.lastIndex) {
                    VerticalDivider(
                        modifier = Modifier.height(44.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlyItem(
    hour: HourlyForecast,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier,
) {
    val isNow = hour.isNow
    val popValue = hour.popPct

    Column(
        modifier = modifier
            .height(116.dp)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isNow) "现在" else hour.time,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isNow) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isNow) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(6.dp))

        val pillModifier = if (isNow) {
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        } else {
            Modifier.size(32.dp)
        }
        Box(
            modifier = pillModifier,
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = qWeatherIcon(hour.iconCode),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (isNow) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = hour.tempCelsius.toTempString(unit),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isNow) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(2.dp))

        Box(Modifier.height(14.dp)) {
            if (popValue > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = hour.popPct.toPercentString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

// ── Preview ──

private val previewHours = listOf(
    HourlyForecast(time = "现在", tempCelsius = 24.0, iconCode = "100", popPct = 10, isNow = true),
    HourlyForecast(time = "13:00", tempCelsius = 25.0, iconCode = "101", popPct = 20, isNow = false),
    HourlyForecast(time = "14:00", tempCelsius = 26.0, iconCode = "300", popPct = 60, isNow = false),
    HourlyForecast(time = "15:00", tempCelsius = 24.0, iconCode = "400", popPct = 0, isNow = false),
)

@Preview(name = "HourlyForecastRow - Light", showBackground = true)
@Composable
private fun HourlyForecastRowPreview() {
    KairoWeatherTheme {
        HourlyForecastRow(previewHours, TemperatureUnit.CELSIUS, Modifier.padding(16.dp))
    }
}

@Preview(name = "HourlyForecastRow - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HourlyForecastRowDarkPreview() {
    KairoWeatherTheme {
        HourlyForecastRow(previewHours, TemperatureUnit.CELSIUS)
    }
}
