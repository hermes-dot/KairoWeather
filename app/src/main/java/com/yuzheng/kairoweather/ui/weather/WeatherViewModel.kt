package com.yuzheng.kairoweather.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuzheng.kairoweather.data.location.LocationTracker
import com.yuzheng.kairoweather.data.preferences.UserPreferences
import com.yuzheng.kairoweather.data.repository.WeatherRepository
import com.yuzheng.kairoweather.domain.model.HourlyForecast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val locationTracker: LocationTracker,
    preferences: UserPreferences,
) : ViewModel() {
    companion object {
        /** 默认城市坐标(北京),权限被拒绝或定位失败时使用 */
        const val DEFAULT_LOCATION = "116.41,39.92"

        /**
         * 下拉刷新最小指示时长:即使网络瞬时返回(如本地缓存/极快响应),
         * 指示器也至少停留该时长,保证用户可感知刷新已触发。
         */
        private const val MIN_REFRESH_INDICATOR_MS = 500L
    }

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var lastLocation: String = DEFAULT_LOCATION

    init {
        viewModelScope.launch {
            preferences.temperatureUnit.collect { unit ->
                _uiState.update { it.copy(temperatureUnit = unit) }
            }
        }
    }

    /**
     * 单位切换不需要重拉数据:P2-A 后 domain 存原始摄氏数值,组件读 [WeatherUiState.temperatureUnit]
     * 在 UI 层即时重新格式化即可。这里仍把单位收集进 state,供组件格式化时读取。
     */
    fun loadFromCurrentLocation() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            locationTracker.getCurrentLocation()
                .onSuccess { loc ->
                    val coords = locationTracker.formatLocation(loc)
                    lastLocation = coords
                    loadWeatherAndResolveName(coords)
                }
                .onFailure {
                    // 定位失败时回退到默认城市,保证页面可用
                    lastLocation = DEFAULT_LOCATION
                    loadWeatherAndResolveName(DEFAULT_LOCATION)
                }
        }
    }

    fun loadingWeather(location: String) {
        if (_uiState.value.isLoading) return
        lastLocation = location
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            loadWeatherAndResolveName(location)
        }
    }

    /**
     * 下拉刷新:强制回源(forceRefresh = true 绕过 TTL 缓存),并保证指示器
     * 至少展示 [MIN_REFRESH_INDICATOR_MS] 毫秒,避免缓存命中/瞬时返回时一闪而过。
     */
    fun refresh() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val start = System.currentTimeMillis()
            loadWeatherInternal(lastLocation, forceRefresh = true)
            val elapsed = System.currentTimeMillis() - start
            if (elapsed < MIN_REFRESH_INDICATOR_MS) {
                delay(MIN_REFRESH_INDICATOR_MS - elapsed)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /** 地名反查与天气加载并行执行,避免反查慢拖延天气展示(P2-7)。 */
    private suspend fun loadWeatherAndResolveName(location: String) = coroutineScope {
        launch { resolveLocationName(location) }
        loadWeatherInternal(location)
        _uiState.update { it.copy(isLoading = false) }
    }

    private suspend fun resolveLocationName(coords: String) {
        repository.reverseGeocode(coords)
            .onSuccess { name -> _uiState.update { it.copy(locationName = name) } }
    }

    private suspend fun loadWeatherInternal(location: String, forceRefresh: Boolean = false) = coroutineScope {
        val currentJob = launch { loadCurrent(location, forceRefresh) }
        val hourlyJob = launch { loadHourly(location, forceRefresh) }
        val dailyJob = launch { loadDaily(location, forceRefresh) }

        currentJob.join(); hourlyJob.join(); dailyJob.join()
    }

    private suspend fun loadCurrent(location: String, forceRefresh: Boolean = false) {
        repository.getCurrentWeather(location, forceRefresh)
            .onSuccess { _uiState.update { s -> s.copy(current = it) } }
            // P2-6: error 仅由 current 请求写入,整页错误只反映当前天气失败;
            // hourly/daily 失败保持静默,不覆盖已加载的数据。
            .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
    }

    private suspend fun loadHourly(location: String, forceRefresh: Boolean = false) {
        repository.getHourlyForecast(location, forceRefresh)
            .onSuccess { hours ->
                _uiState.update { s ->
                    s.copy(
                        hourly = hours.map { hour -> hour.copy(isNow = isCurrentHour(hour)) }
                    )
                }
            }
    }

    private suspend fun loadDaily(location: String, forceRefresh: Boolean = false) {
        repository.getDailyForecast(location, forceRefresh)
            .onSuccess { days ->
                _uiState.update { s ->
                    s.copy(
                        daily = days,
                        sunProgress = calcSunProgress(days.firstOrNull()?.sunrise, days.firstOrNull()?.sunset)
                    )
                }
            }
    }

    /**
     * 判断该小时是否"当前小时"。
     *
     * 优先用带时区偏移的原始 fxTime([HourlyForecast.rawTime])与当前时刻比对:
     * 把 [OffsetDateTime.now] 换算到该小时数据所在时区后再比较小时,避免设备时区
     * 与目标城市时区不一致时把"当前小时"标错(P2-5)。无时区信息时退回设备时区粗略比较。
     */
    private fun isCurrentHour(hour: HourlyForecast): Boolean {
        val odt = hour.rawTime.takeIf { it.isNotEmpty() }
            ?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
            ?: return hour.time.take(2).toIntOrNull() == LocalTime.now().hour
        val nowInTargetZone = OffsetDateTime.now().withOffsetSameInstant(odt.offset)
        return nowInTargetZone.toLocalDate() == odt.toLocalDate() && nowInTargetZone.hour == odt.hour
    }

    private fun calcSunProgress(rise: String?, set: String?): Float {
        if (rise.isNullOrEmpty() || set.isNullOrEmpty()) return 0f
        val r = runCatching { LocalTime.parse(rise) }.getOrNull() ?: return 0f
        val s = runCatching { LocalTime.parse(set) }.getOrNull() ?: return 0f
        val total = s.toSecondOfDay() - r.toSecondOfDay()
        // P2-9: 日落早于日出属跨午夜场景(如极地 22:00 日出、02:00 日落),total<=0。
        // 这里保持返回 0(不返回负数);更精确可把 total 计为 s + 86400 - r,但极地场景
        // 对进度条无实际意义,且当前时刻归属当日/次日有歧义,故按简单方案处理。
        if (total <= 0) return 0f
        val elapsed = LocalTime.now().toSecondOfDay() - r.toSecondOfDay()
        return (elapsed.toFloat() / total).coerceIn(0f, 1f)
    }
}
