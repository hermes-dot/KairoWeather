package com.yuzheng.kairoweather.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yuzheng.kairoweather.domain.model.HourlyForecast

@Composable
fun HourlyForecastRow(
    items: List<HourlyForecast>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(12.dp))
            items.forEachIndexed { index, hour ->
                HourlyItem(hour, Modifier.width(64.dp))
                if (index < items.lastIndex) {
                    VerticalDivider(
                        modifier = Modifier.height(44.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun HourlyItem(
    hour: HourlyForecast,
    modifier: Modifier = Modifier,
) {
    val isNow = hour.isNow
    val popValue = hour.pop.removeSuffix("%").toIntOrNull() ?: 0

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

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isNow) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent
                ),
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
            text = hour.temperature,
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
                        text = hour.pop,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
