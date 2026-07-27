package com.yuzheng.kairoweather.ui.theme.weather

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yuzheng.kairoweather.R
import com.yuzheng.kairoweather.ui.theme.weather.components.HourlyForecastRow
import com.yuzheng.kairoweather.ui.theme.weather.components.HumidityCard
import com.yuzheng.kairoweather.ui.theme.weather.components.MoonPhaseCard
import com.yuzheng.kairoweather.ui.theme.weather.components.PressureCard
import com.yuzheng.kairoweather.ui.theme.weather.components.SunArcCard
import com.yuzheng.kairoweather.ui.theme.weather.components.UvIndexCard
import com.yuzheng.kairoweather.ui.theme.weather.components.WeatherHeader
import com.yuzheng.kairoweather.ui.theme.weather.components.WeatherTopBar
import com.yuzheng.kairoweather.ui.theme.weather.components.WindCard

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = hiltViewModel(),
    location: String = "116.41,39.92",
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.any { it }) {
            viewModel.loadFromCurrentLocation()
        } else {
            viewModel.loadingWeather(location) // 拒绝则用默认坐标
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && state.current == null -> LoadingContent()
            state.error != null && state.current == null -> ErrorContent(state.error!!)
            state.current != null -> WeatherContent(state, onRefresh = { viewModel.refresh() })
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun WeatherContent(state: WeatherUiState, onRefresh: () -> Unit) {
    val weather = state.current ?: return
    val dailyFirst = state.daily.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensionResource(R.dimen.page_horizontal_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WeatherTopBar(
            locationName = state.locationName,
            onRefresh = onRefresh,
        )

        // ── 顶部：图标 + 温度 + 描述 ──
        WeatherHeader(
            iconCode = weather.iconCode,
            temperature = weather.temperature,
            condition = weather.condition,
            feelLike = weather.feelLike,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.section_spacing)))

        // ── 详情卡片网格 (2列) ──
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.card_grid_spacing)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.card_grid_spacing)),
            ) {
                WindCard(
                    direction = weather.wind,
                    angle = weather.windAngle,
                    speed = weather.windSpeedRaw,
                    scale = weather.windScale,
                    modifier = Modifier.weight(1f),
                )
                HumidityCard(humidity = weather.humidity, modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.card_grid_spacing)),
            ) {
                PressureCard(pressure = weather.pressure, modifier = Modifier.weight(1f))
                dailyFirst?.moonPhase?.emoji?.let {
                    MoonPhaseCard(
                        phaseEmoji = it,
                        phaseName = dailyFirst.moonPhase.label,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

        }

        // ── 日出日落 ──
        if (dailyFirst != null && dailyFirst.sunrise.isNotEmpty()) {
            Spacer(Modifier.height(dimensionResource(R.dimen.section_spacing)))
            SunArcCard(
                sunrise = dailyFirst.sunrise,
                sunset = dailyFirst.sunset,
                progress = state.sunProgress,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ── 逐小时 ──
        if (state.hourly.isNotEmpty()) {
            Spacer(Modifier.height(dimensionResource(R.dimen.section_spacing)))
            Text(
                "逐小时预报",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.page_horizontal_padding)),
            )
            Spacer(Modifier.height(8.dp))
            HourlyForecastRow(state.hourly)
        }

        Spacer(Modifier.height(32.dp))
    }
}
