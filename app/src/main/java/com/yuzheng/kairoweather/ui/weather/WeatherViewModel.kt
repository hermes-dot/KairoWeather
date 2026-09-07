package com.yuzheng.kairoweather.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuzheng.kairoweather.data.location.LocationTracker
import com.yuzheng.kairoweather.data.preferences.UserPreferences
import com.yuzheng.kairoweather.data.repository.WeatherRepository
import com.yuzheng.kairoweather.domain.model.AirQuality
import com.yuzheng.kairoweather.domain.model.CurrentWeather
import com.yuzheng.kairoweather.domain.model.DailyForecast
import com.yuzheng.kairoweather.domain.model.HourlyForecast
import com.yuzheng.kairoweather.domain.model.TemperatureUnit
import com.yuzheng.kairoweather.ui.weather.WeatherUiState.Companion.DEFAULT_CITY_NAME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    /**
     * 温度单位由 UI 层直接订阅 DataStore 流,不再 collect 镜像进页面状态——
     * 旧设计里同一数据活在 SettingsViewModel 与 WeatherUiState 两处(双写源),
     * 且有一个 collect 延迟窗口。单位切换不重拉数据:P2-A 后 domain 存原始摄氏值,
     * 组件读此 flow 在 UI 层即时重新格式化。
     */
    val temperatureUnit: Flow<TemperatureUnit> = preferences.temperatureUnit

    private var lastLocation: String = DEFAULT_LOCATION

    /**
     * 防重入旗标:覆盖"定位+地名反查+四路天气"的完整流程。
     * 不能用 state is Loading 判定——初始态就是 Loading 会挡掉首次加载;
     * Ready.isRefreshing 也只覆盖刷新指示,盖不住冷启动在途。独立标志语义最干净。
     */
    private var inFlight = false

    /**
     * 地名先于天气到达时的暂存(冷启动竞态:Ready 还不存在,无处安放)。
     * 每次进入加载流程重置,防止上一城市的名字漏进新城市的 Ready。
     */
    private var pendingCityName: String? = null

    fun loadFromCurrentLocation() {
        if (inFlight) return
        inFlight = true
        viewModelScope.launch {
            try {
                markLoading()
                locationTracker.getCurrentLocation()
                    .onSuccess { loc ->
                        val coords = locationTracker.formatLocation(loc)
                        lastLocation = coords
                        runLoad(coords, forceRefresh = false)
                    }
                    .onFailure {
                        // 定位失败时回退到默认城市,保证页面可用
                        lastLocation = DEFAULT_LOCATION
                        runLoad(DEFAULT_LOCATION, forceRefresh = false)
                    }
                clearIndicator()
            } finally {
                inFlight = false
            }
        }
    }

    fun loadingWeather(location: String) {
        if (inFlight) return
        inFlight = true
        lastLocation = location
        viewModelScope.launch {
            try {
                markLoading()
                runLoad(location, forceRefresh = false)
                clearIndicator()
            } finally {
                inFlight = false
            }
        }
    }

    /**
     * 下拉刷新/错误重试:强制回源(forceRefresh = true 绕过 TTL 缓存),并保证指示器
     * 至少展示 [MIN_REFRESH_INDICATOR_MS] 毫秒,避免缓存命中/瞬时返回时一闪而过。
     * 最小延迟在 clearIndicator 之前等待——指示器时长才是这段延迟的存在意义。
     */
    fun refresh() {
        if (inFlight) return
        inFlight = true
        viewModelScope.launch {
            try {
                markLoading()
                val start = System.currentTimeMillis()
                runLoad(lastLocation, forceRefresh = true)
                val elapsed = System.currentTimeMillis() - start
                if (elapsed < MIN_REFRESH_INDICATOR_MS) {
                    delay(MIN_REFRESH_INDICATOR_MS - elapsed)
                }
                clearIndicator()
            } finally {
                inFlight = false
            }
        }
    }

    /**
     * 指示状态复位:Ready 有内容时转圈旗标归位;Loading/Failed 的指示器就是页面态本身,
     * 数据到达时由 [loadWeatherInternal] 的投影直接迁移,无需清理。
     */
    private fun markLoading() {
        pendingCityName = null
        _uiState.update {
            when (it) {
                is WeatherUiState.Ready -> it.copy(isRefreshing = true)
                else -> WeatherUiState.Loading
            }
        }
    }

    private fun clearIndicator() {
        _uiState.update {
            if (it is WeatherUiState.Ready) it.copy(isRefreshing = false) else it
        }
    }

    /** 地名反查与天气加载并行执行,避免反查慢拖延天气展示(P2-7)。 */
    private suspend fun runLoad(location: String, forceRefresh: Boolean) = coroutineScope {
        launch { resolveLocationName(location) }
        loadWeatherInternal(location, forceRefresh)
    }

    private suspend fun resolveLocationName(coords: String) {
        repository.reverseGeocode(coords)
            .onSuccess { name ->
                _uiState.update {
                    if (it is WeatherUiState.Ready) {
                        it.copy(locationName = name)
                    } else {
                        // Ready 尚不存在(冷启动天气未到):暂存,投影时消费
                        pendingCityName = name
                        it
                    }
                }
            }
    }

    /**
     * 四路并行拉取,awaitAll 后一次投影——页面级原子快照:
     * UI 不存在"current 已到、air 未落"的中间帧,各卡级降级(air=null 不渲染)
     * 由 [WeatherUiState.Ready] 内部字段表达。
     *
     * 失败分级(P2-6):current 失败才整页 Failed;hourly/daily/air 失败以
     * 空列表/缺席兜底进 Ready。刷新已有内容时 current 失败保持 Ready 静默
     * (旧数据可看,好过把内容打回错误页)。
     */
    private suspend fun loadWeatherInternal(location: String, forceRefresh: Boolean) {
        // 具名而非 List:异构 Result<T> 塞进一个 list 会擦除成 Any
        val (currentRes, hourlyRes, dailyRes, airRes) = coroutineScope {
            val now = async { repository.getCurrentWeather(location, forceRefresh) }
            val hours = async { repository.getHourlyForecast(location, forceRefresh) }
            val days = async { repository.getDailyForecast(location, forceRefresh) }
            val air = async { repository.getAirQuality(location, forceRefresh) }
            LoadedWeather(now.await(), hours.await(), days.await(), air.await())
        }

        val current = currentRes.getOrNull()
        if (current == null) {
            _uiState.update {
                when (it) {
                    is WeatherUiState.Ready -> it
                    else -> WeatherUiState.Failed(
                        currentRes.exceptionOrNull()?.message ?: "天气获取失败"
                    )
                }
            }
            return
        }

        val hourly = (hourlyRes.getOrNull() ?: emptyList())
            .map { hour -> hour.copy(isNow = isCurrentHour(hour)) }
        val daily = dailyRes.getOrNull() ?: emptyList()
        val airQuality = airRes.getOrNull()
        val sunProgress = calcSunProgress(daily.firstOrNull()?.sunrise, daily.firstOrNull()?.sunset)

        _uiState.update { s ->
            if (s is WeatherUiState.Ready) {
                // 原地刷新:locationName 由反查流自行维护(isRefreshing 由入口管),此处不碰
                s.copy(
                    current = current, hourly = hourly, daily = daily,
                    air = airQuality, sunProgress = sunProgress,
                )
            } else {
                WeatherUiState.Ready(
                    current = current, hourly = hourly, daily = daily,
                    air = airQuality, sunProgress = sunProgress,
                    locationName = pendingCityName ?: DEFAULT_CITY_NAME,
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

    /** 四路并行请求的具名结果束,仅本类投影用(异构 Result 不能混进同一 List)。 */
    private data class LoadedWeather(
        val current: Result<CurrentWeather>,
        val hourly: Result<List<HourlyForecast>>,
        val daily: Result<List<DailyForecast>>,
        val air: Result<AirQuality>,
    )

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
