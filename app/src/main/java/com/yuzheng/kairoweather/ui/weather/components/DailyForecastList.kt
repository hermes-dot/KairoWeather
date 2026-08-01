package com.yuzheng.kairoweather.ui.weather.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yuzheng.kairoweather.domain.model.DailyForecast
import com.yuzheng.kairoweather.domain.model.MoonPhase
import com.yuzheng.kairoweather.domain.model.TemperatureUnit
import com.yuzheng.kairoweather.domain.model.toPercentString
import com.yuzheng.kairoweather.domain.model.toTempString
import com.yuzheng.kairoweather.ui.theme.KairoWeatherTheme

@Composable
fun DailyForecastList(
    daily: List<DailyForecast>,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        // P2-C5: 数据量上限 7 天(QWeather API 固定返回),刻意用 forEach 一次组合,
        // 避免在父 LazyColumn 内再嵌 LazyColumn 的嵌套滚动开销;若未来放宽天数上限再改 LazyColumn。
        daily.forEach { day ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(day.date, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Icon(
                    imageVector = qWeatherIcon(day.iconCode),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    day.description,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (day.popPct > 0) {
                    Text(
                        day.popPct.toPercentString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    "${day.lowTempCelsius.toTempString(unit)} / ${day.highTempCelsius.toTempString(unit)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// ── Preview ──

private val previewDaily = listOf(
    DailyForecast(
        date = "今天",
        highTempCelsius = 30.0,
        lowTempCelsius = 22.0,
        iconCode = "100",
        description = "晴",
        popPct = 0,
        uvIndexValue = 6,
        moonPhase = MoonPhase.FULL_MOON,
        sunrise = "05:00",
        sunset = "19:00",
    ),
    DailyForecast(
        date = "周日 8/2",
        highTempCelsius = 28.0,
        lowTempCelsius = 21.0,
        iconCode = "101",
        description = "多云",
        popPct = 30,
        uvIndexValue = 5,
        moonPhase = MoonPhase.WAXING_CRESCENT,
        sunrise = "05:01",
        sunset = "18:59",
    ),
    DailyForecast(
        date = "周一 8/3",
        highTempCelsius = 26.0,
        lowTempCelsius = 20.0,
        iconCode = "300",
        description = "雷阵雨",
        popPct = 80,
        uvIndexValue = 3,
        moonPhase = MoonPhase.FIRST_QUARTER,
        sunrise = "05:02",
        sunset = "18:58",
    ),
)

@Preview(name = "DailyForecastList - Light", showBackground = true)
@Composable
private fun DailyForecastListPreview() {
    KairoWeatherTheme {
        DailyForecastList(previewDaily, TemperatureUnit.CELSIUS, Modifier.padding(16.dp))
    }
}

@Preview(name = "DailyForecastList - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DailyForecastListDarkPreview() {
    KairoWeatherTheme {
        DailyForecastList(previewDaily, TemperatureUnit.CELSIUS)
    }
}
