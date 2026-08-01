package com.yuzheng.kairoweather.ui.weather.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.yuzheng.kairoweather.ui.theme.KairoWeatherTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherTopBar(
    locationName: String,
    subtitle: String = "实时天气",
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = locationName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新",
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        modifier = modifier,
    )
}

// ── Preview ──

@Preview(name = "WeatherTopBar - Light", showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherTopBarPreview() {
    KairoWeatherTheme {
        WeatherTopBar(locationName = "北京", subtitle = "实时天气")
    }
}

@Preview(name = "WeatherTopBar - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherTopBarDarkPreview() {
    KairoWeatherTheme {
        WeatherTopBar(locationName = "上海", subtitle = "实时天气")
    }
}
