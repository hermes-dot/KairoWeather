package com.yuzheng.kairoweather.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.yuzheng.kairoweather.R
import com.yuzheng.kairoweather.domain.model.HourlyForecast

@Composable
fun HourlyForecastRow(
    items: List<HourlyForecast>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .height(100.dp),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.hourly_item_spacing)),
    ) {
        Spacer(Modifier.size(dimensionResource(R.dimen.page_horizontal_padding)))
        items.forEach { hour ->
            val isNow = hour.isNow
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isNow) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (isNow) "现在" else hour.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isNow) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Icon(
                    imageVector = qWeatherIcon(hour.iconCode),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.hourly_icon_size)),
                    tint = if (isNow) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    hour.temperature,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isNow) MaterialTheme.colorScheme.onPrimaryContainer
                    else Color.Unspecified,
                )
                Text(
                    hour.pop,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isNow) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.size(dimensionResource(R.dimen.page_horizontal_padding)))
    }
}
