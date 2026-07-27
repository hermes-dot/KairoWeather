package com.yuzheng.kairoweather.ui.theme.weather.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.yuzheng.kairoweather.R
import com.yuzheng.kairoweather.domain.model.HourlyForecast

@Composable
fun HourlyForecastRow(
    items: List<HourlyForecast>,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.hourly_item_spacing)),
        //contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.page_horizontal_padding)),
    ) {
        items(items) { hour ->
            Card {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(hour.time, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(4.dp))
                    Icon(
                        imageVector = qWeatherIcon(hour.iconCode),
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(R.dimen.hourly_icon_size)),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(hour.temperature, style = MaterialTheme.typography.bodySmall)
                    Text(hour.pop, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
