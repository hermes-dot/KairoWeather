package com.yuzheng.kairoweather.ui.weather.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yuzheng.kairoweather.R
import com.yuzheng.kairoweather.domain.model.TemperatureUnit
import com.yuzheng.kairoweather.domain.model.toFeelLikeString
import com.yuzheng.kairoweather.domain.model.toTempString
import com.yuzheng.kairoweather.ui.theme.KairoWeatherTheme

@Composable
fun WeatherHeader(
    iconCode: String,
    tempCelsius: Double,
    condition: String,
    feelsLikeCelsius: Double,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = qWeatherIcon(iconCode),
            contentDescription = condition,
            modifier = Modifier.size(dimensionResource(R.dimen.main_icon_size)),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.width(8.dp))

        Column {
            Text(
                text = tempCelsius.toTempString(unit),
                fontWeight = FontWeight.Light,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = condition,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = feelsLikeCelsius.toFeelLikeString(unit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Preview ──

@Preview(name = "WeatherHeader - Light", showBackground = true)
@Composable
private fun WeatherHeaderPreview() {
    KairoWeatherTheme {
        WeatherHeader(
            iconCode = "100",
            tempCelsius = 24.0,
            condition = "晴",
            feelsLikeCelsius = 26.0,
            unit = TemperatureUnit.CELSIUS,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "WeatherHeader - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WeatherHeaderDarkPreview() {
    KairoWeatherTheme {
        WeatherHeader(
            iconCode = "150",
            tempCelsius = 18.0,
            condition = "晴间多云",
            feelsLikeCelsius = 17.0,
            unit = TemperatureUnit.CELSIUS,
            modifier = Modifier.padding(16.dp),
        )
    }
}
