package com.yuzheng.kairoweather.ui.weather

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuzheng.kairoweather.data.PerfLog
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

        /**
         * 换城判定阈值(米):回前台复查时,新定位与上次锚点位移超过 20km 才重新拉取。
         * 阈值要同时躲开两头的坑——GPS 漂移与市内通勤(地铁跨区约十几公里)不该反复
         * 触发重载,而高铁/航班换城动辄上百公里必然越线。宁可市内多刷一次,
         * 也不放走真换城:缓存 key 含坐标,同坐标重拉会命中 TTL,配额开销有限。
         */
        private const val CITY_SWITCH_THRESHOLD_M = 20_000f
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
     * 当前展示坐标对应的原始定位对象(换城复查的比较锚点)。只保留经纬度、不保留
     * provider/精度等字段:复查只关心"离上次位置多远"。搜索/手动选城没有定位对象,置 null。
     */
    private var lastFix: Location? = null

    /**
     * 当前城市是否来自用户手动选城(含权限被拒后显式回退默认城市)。
     * 定位模式(false)下,下拉刷新与回前台复查都可以静默重定位;
     * 手动模式(true)下定位结果永远不得覆盖用户选择——不能只靠 lastFix==null
     * 区分,因为"定位失败回退默认城市"同样是 lastFix==null,却必须允许后续自救。
     */
    private var manualCity = false

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

    /**
     * 首屏加载(分段式):有任意 lastKnown 就立刻用它渲染,新鲜度不够时后台
     * 再走换城复查精修——实测本机主动定位室内可能挂满 15s 超时,首屏同步等它
     * 就是"十秒白屏"(vivo/ColorOS,室内 BALANCED 定位)。旧坐标哪怕过期也
     * "曾经真实":先出内容再静默校准,严格优于干等。无任何缓存(真·冷启动)
     * 才退回同步主动定位。
     */
    fun loadFromCurrentLocation() {
        if (inFlight) return
        inFlight = true
        manualCity = false
        viewModelScope.launch {
            try {
                markLoading()
                val cached = locationTracker.getCachedLocation()
                if (cached != null) {
                    PerfLog.log("instant render cached age=${System.currentTimeMillis() - cached.time}ms")
                    lastFix = cached
                    val coords = locationTracker.formatLocation(cached)
                    lastLocation = coords
                    runLoad(coords, forceRefresh = false)
                    clearIndicator()
                    // 后台精修 = 一次换城复查(锚点是刚用的缓存坐标,静默、越阈值才切城)。
                    // 先 launch 再 return:父协程 finally 释放 inFlight 后子协程才会执行,
                    // 复查守卫必然放行;精修期间用户下拉刷新也不被占用。
                    if (!LocationTracker.isFresh(cached.time)) {
                        viewModelScope.launch { recheckLocationIfMoved() }
                    }
                    return@launch
                }
                locationTracker.getCurrentLocation()
                    .onSuccess { loc ->
                        lastFix = loc
                        val coords = locationTracker.formatLocation(loc)
                        lastLocation = coords
                        runLoad(coords, forceRefresh = false)
                    }
                    .onFailure {
                        // 定位失败时回退到默认城市,保证页面可用
                        lastFix = null
                        lastLocation = DEFAULT_LOCATION
                        runLoad(DEFAULT_LOCATION, forceRefresh = false)
                    }
                clearIndicator()
            } finally {
                inFlight = false
            }
        }
    }

    /**
     * 换城复查:App 回到前台时调用。用户坐高铁/飞机去了别的城市,页面不该继续
     * 显示初定位钉死的旧城市(初定位钉死 bug 的另一半:即便定位是新的,加载入口
     * 只在 LaunchedEffect(Unit) 触发一次,回前台不会重新定位)。
     *
     * 与 loadFromCurrentLocation 的关键区别是**静默**:
     * - 不 markLoading——旧城市数据在复查期间继续可看,只有确认换城才整页切换,
     *   否则每次回前台都白屏转圈,比"偶尔晚一屏"更糟;
     * - 定位失败/未越阈值都原样保留当前展示,不打扰用户;
     * - 复查本身不消耗下拉刷新指示器,走 forceRefresh=false,坐标变了必然未命中缓存。
     *
     * 手动选城(manualCity)时跳过:用户明确选的城不能被定位"抢回去"。
     * 定位失败回退默认城市**不算**手动——锚点为 null 时照常重定位,
     * 定位服务恢复后页面自动切回真实位置,而非永久钉死在默认城市。
     */
    fun recheckLocationIfMoved() {
        if (inFlight || manualCity) return
        inFlight = true
        viewModelScope.launch {
            try {
                val fresh = locationTracker.getCurrentLocation().getOrNull() ?: return@launch
                val anchor = lastFix
                // 锚点存在且未越阈值:市内通勤/GPS 漂移不打扰;锚点为 null 时
                // 这次成功定位就是自救,直接采纳。
                if (anchor != null &&
                    locationTracker.distanceMeters(anchor, fresh) < CITY_SWITCH_THRESHOLD_M
                ) return@launch
                lastFix = fresh
                val coords = locationTracker.formatLocation(fresh)
                lastLocation = coords
                markLoading()
                runLoad(coords, forceRefresh = false)
                clearIndicator()
            } finally {
                inFlight = false
            }
        }
    }

    fun loadingWeather(location: String) {
        if (inFlight) return
        inFlight = true
        // 手动坐标:撤销定位锚点并进入手动模式,复查/刷新都不应用定位覆盖用户选择
        lastFix = null
        manualCity = true
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
     *
     * 定位模式下刷新前先静默重定位:换城后下拉刷新是最自然的"更新"动作,
     * 若只重拉旧坐标,刷出来的仍是旧城市(初定位钉死 bug 的第三种表现)。
     */
    fun refresh() {
        if (inFlight) return
        inFlight = true
        viewModelScope.launch {
            try {
                markLoading()
                val start = System.currentTimeMillis()
                val target = if (manualCity) lastLocation else resolveRefreshCoords()
                runLoad(target, forceRefresh = true)
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
     * 刷新前的静默重定位:成功则把展示坐标与锚点一起切到新位置(刷新本就整页
     * 转圈,换城不额外闪屏);失败则留在旧坐标——指示器已经在转,不能因定位
     * 服务抖动把这次刷新变成 Failed。
     */
    private suspend fun resolveRefreshCoords(): String {
        val fresh = locationTracker.getCurrentLocation().getOrNull() ?: return lastLocation
        lastFix = fresh
        val coords = locationTracker.formatLocation(fresh)
        lastLocation = coords
        return coords
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
        val t0 = System.currentTimeMillis() // 诊断日志(临时)
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
            PerfLog.log("4-path done: ${System.currentTimeMillis() - t0}ms")
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
