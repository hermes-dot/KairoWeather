package com.yuzheng.kairoweather.ui.weather.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yuzheng.kairoweather.R
import com.yuzheng.kairoweather.ui.theme.KairoWeatherTheme

@Composable
fun WindCard(
    direction: String,
    angle: String,
    speed: String,
    scale: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.card_padding_horizontal)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("风向风力", style = MaterialTheme.typography.labelMedium)

            Spacer(Modifier.height(dimensionResource(R.dimen.card_label_spacing)))

            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = direction,
                modifier = Modifier
                    .size(dimensionResource(R.dimen.card_icon_size))
                    .rotate(angle.toFloatOrNull() ?: 0f),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.card_value_spacing)))

            Text(direction, style = MaterialTheme.typography.titleMedium)

            Text(
                text = "${scale}级 · ${speed}km/h",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun HumidityCard(
    humidityPct: Int,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.card_padding_horizontal)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("湿度", style = MaterialTheme.typography.labelMedium)

            Spacer(Modifier.height(dimensionResource(R.dimen.card_label_spacing)))

            Icon(
                imageVector = Icons.Default.Opacity,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.card_icon_size)),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.card_value_spacing)))

            Text("$humidityPct%", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun PressureCard(
    pressureHpa: Int,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.card_padding_horizontal)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("气压", style = MaterialTheme.typography.labelMedium)

            Spacer(Modifier.height(dimensionResource(R.dimen.card_label_spacing)))

            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.card_icon_size)),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.card_value_spacing)))

            Text("${pressureHpa}hPa", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun UvIndexCard(
    uvIndex: Int,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.card_padding_horizontal)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("紫外线", style = MaterialTheme.typography.labelMedium)

            Spacer(Modifier.height(dimensionResource(R.dimen.card_label_spacing)))

            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.card_icon_size)),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.card_value_spacing)))

            Text("指数 $uvIndex", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun MoonPhaseCard(
    phaseEmoji: String,
    phaseName: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.card_padding_horizontal)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("月相", style = MaterialTheme.typography.labelMedium)

            Spacer(Modifier.height(dimensionResource(R.dimen.card_label_spacing)))

            Text(phaseEmoji, style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(dimensionResource(R.dimen.card_value_spacing)))

            Text(phaseName, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ── Preview ──

@Preview(name = "WindCard - Light", showBackground = true)
@Composable
private fun WindCardPreview() {
    KairoWeatherTheme {
        WindCard(
            direction = "东北风",
            angle = "45",
            speed = "10",
            scale = "3",
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Preview(name = "WindCard - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WindCardDarkPreview() {
    KairoWeatherTheme {
        WindCard(
            direction = "西北风",
            angle = "315",
            speed = "12",
            scale = "4",
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}
