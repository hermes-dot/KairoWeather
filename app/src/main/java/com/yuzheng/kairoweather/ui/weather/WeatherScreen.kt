package com.yuzheng.kairoweather.ui.weather

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yuzheng.kairoweather.R
import com.yuzheng.kairoweather.ui.weather.components.DailyForecastList
import com.yuzheng.kairoweather.ui.weather.components.HourlyForecastRow
import com.yuzheng.kairoweather.ui.weather.components.HumidityCard
import com.yuzheng.kairoweather.ui.weather.components.MoonPhaseCard
import com.yuzheng.kairoweather.ui.weather.components.PressureCard
import com.yuzheng.kairoweather.ui.weather.components.SunArcCard
import com.yuzheng.kairoweather.ui.weather.components.UvIndexCard
import com.yuzheng.kairoweather.ui.weather.components.WeatherHeader
import com.yuzheng.kairoweather.ui.weather.components.WeatherTopBar
import com.yuzheng.kairoweather.ui.weather.components.WindCard
import com.yuzheng.kairoweather.ui.theme.weatherSkyGradient

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.any { it }) {
            viewModel.loadFromCurrentLocation()
        } else {
            viewModel.loadingWeather(WeatherViewModel.DEFAULT_LOCATION) // 拒绝则用默认坐标
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            viewModel.loadFromCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(weatherSkyGradient())),
        color = Color.Transparent,
    ) {
        when {
            state.isLoading && state.current == null -> LoadingContent()
            state.error != null && state.current == null ->
                ErrorContent(state.error!!, onRetry = { viewModel.refresh() })
            state.current != null -> WeatherContent(state, onRefresh = { viewModel.refresh() })
        }
    }
}

private fun hasLocationPermission(context: android.content.Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun WeatherContent(state: WeatherUiState, onRefresh: () -> Unit) {
    val weather = state.current ?: return
    val dailyFirst = state.daily.firstOrNull()

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.page_horizontal_padding)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "topbar") { WeatherTopBar(locationName = state.locationName, onRefresh = onRefresh) }

            item(key = "header") {
                WeatherHeader(
                    iconCode = weather.iconCode,
                    temperature = weather.temperature,
                    condition = weather.condition,
                    feelLike = weather.feelLike,
                )
            }

            // ── 逐小时 ──
            if (state.hourly.isNotEmpty()) {
                item(key = "spacer_hourly_top") { Spacer(Modifier.height(dimensionResource(R.dimen.section_spacing))) }
                item(key = "hourly_title") {
                    Text(
                        "逐小时预报", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = dimensionResource(R.dimen.page_horizontal_padding)),
                    )
                }
                item(key = "spacer_hourly_mid") { Spacer(Modifier.height(8.dp)) }
                item(key = "hourly") { HourlyForecastRow(state.hourly) }
            }

            // ── 逐日预报 ──
            if (state.daily.isNotEmpty()) {
                item(key = "spacer_daily_top") { Spacer(Modifier.height(dimensionResource(R.dimen.section_spacing))) }
                item(key = "daily") { DailyForecastList(state.daily) }
            }

            item(key = "spacer_detail_top") { Spacer(Modifier.height(dimensionResource(R.dimen.section_spacing))) }

            // ── 详情卡片网格 (2列) ──
            item(key = "details") {
                Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.card_grid_spacing))) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.card_grid_spacing)),
                    ) {
                        WindCard(
                            weather.wind,
                            weather.windAngle,
                            weather.windSpeedRaw,
                            weather.windScale,
                            Modifier.weight(1f).height(120.dp),
                        )
                        HumidityCard(weather.humidity, Modifier.weight(1f).height(120.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.card_grid_spacing)),
                    ) {
                        PressureCard(weather.pressure, Modifier.weight(1f).height(120.dp))
                        dailyFirst?.let {
                            UvIndexCard(it.uvIndex, Modifier.weight(1f).height(120.dp))
                        }
                    }
                    dailyFirst?.moonPhase?.emoji?.let {
                        MoonPhaseCard(
                            it,
                            dailyFirst.moonPhase.label,
                            Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // ── 日出日落 ──
            if (dailyFirst != null && dailyFirst.sunrise.isNotEmpty()) {
                item(key = "spacer_sun_top") { Spacer(Modifier.height(dimensionResource(R.dimen.section_spacing))) }
                item(key = "sun") {
                    SunArcCard(dailyFirst.sunrise, dailyFirst.sunset, state.sunProgress, Modifier.fillMaxWidth())
                }
            }

            item(key = "spacer_bottom") { Spacer(Modifier.height(32.dp)) }
        }
    }
}
