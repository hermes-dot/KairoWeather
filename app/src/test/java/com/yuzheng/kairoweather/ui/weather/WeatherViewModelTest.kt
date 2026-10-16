package com.yuzheng.kairoweather.ui.weather

import android.location.Location
import com.yuzheng.kairoweather.data.location.LocationTracker
import com.yuzheng.kairoweather.data.preferences.UserPreferences
import com.yuzheng.kairoweather.data.repository.WeatherRepository
import com.yuzheng.kairoweather.domain.model.AirQuality
import com.yuzheng.kairoweather.domain.model.CurrentWeather
import com.yuzheng.kairoweather.domain.model.DailyForecast
import com.yuzheng.kairoweather.domain.model.HourlyForecast
import com.yuzheng.kairoweather.domain.model.MoonPhase
import com.yuzheng.kairoweather.domain.model.TemperatureUnit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<WeatherRepository>()
    private val locationTracker = mockk<LocationTracker>()
    private val preferences = mockk<UserPreferences>()

    private val current = CurrentWeather(
        tempCelsius = 24.0,
        condition = "晴",
        feelsLikeCelsius = 26.0,
        humidityPct = 60,
        wind = "东北风",
        windAngle = "45",
        windSpeedRaw = "10",
        windScale = "3",
        iconCode = "100",
        pressureHpa = 1013,
    )

    // rawTime 决定 isNow 的判定（见 WeatherViewModel.isCurrentHour）：给固定的历史时区时间戳，
    // isNow 在任何时刻都确定性地为 false，同时仍走"带时区偏移"的主路径而不是兜底分支。
    // 历史坑：这里原本是 rawTime 缺省（空串）+ time = "12:00"，会退回"设备时区比小时"的兜底分支，
    // 于是每天 12:00–12:59 跑必红、其余 23 小时全绿——CI 上表现为"看推送时间随机变红"。
    private val hourly = listOf(
        HourlyForecast(
            time = "12:00", tempCelsius = 24.0, iconCode = "100", popPct = 10, isNow = false,
            rawTime = "2020-01-01T12:00+08:00",
        ),
        HourlyForecast(
            time = "13:00", tempCelsius = 25.0, iconCode = "100", popPct = 20, isNow = false,
            rawTime = "2020-01-01T13:00+08:00",
        ),
    )

    private val daily = listOf(
        DailyForecast(
            date = "今天",
            highTempCelsius = 30.0,
            lowTempCelsius = 22.0,
            iconCode = "100",
            description = "晴",
            popPct = 10,
            uvIndexValue = 6,
            moonPhase = MoonPhase.FULL_MOON,
            sunrise = "05:00",
            sunset = "19:00",
        ),
    )

    private val air = AirQuality(aqi = 42, category = "优", primaryPollutant = "o3")

    init {
        // air 默认成功桩放 init 而非 viewModel() helper:有测试绕过 helper 直接构造 VM,
        // strict mock 缺省会抛异常被 resultCatching 吞成 null——测试"假绿"。
        // mockk 后写生效:失败路径用例在 VM 构造之后自行 coEvery 覆盖本桩。
        coEvery { repository.getAirQuality(any(), any()) } returns Result.success(air)
    }

    private fun viewModel(unit: TemperatureUnit = TemperatureUnit.CELSIUS): WeatherViewModel {
        every { preferences.temperatureUnit } returns flowOf(unit)
        return WeatherViewModel(repository, locationTracker, preferences)
    }

    /** 分段式首屏会先探 getCachedLocation:无缓存用例显式给 null,strict mock 缺省会抛。 */
    private fun stubNoCachedLocation() {
        coEvery { locationTracker.getCachedLocation() } returns null
    }

    /** sealed 后的统一读取口:非 Ready 即测试失败,失败信息带实际态。 */
    private fun ready(vm: WeatherViewModel): WeatherUiState.Ready =
        vm.uiState.value as? WeatherUiState.Ready
            ?: error("expected Ready, got ${vm.uiState.value::class.simpleName}")  // error 返回 Nothing;fail() 返回 Unit 不满足 elvis 类型

    private fun stubWeatherSuccess() {
        // 签名含 forceRefresh,用 any(), any() 让普通加载与强制刷新两种路径都命中
        coEvery { repository.getCurrentWeather(any(), any()) } returns Result.success(current)
        coEvery { repository.getHourlyForecast(any(), any()) } returns Result.success(hourly)
        coEvery { repository.getDailyForecast(any(), any()) } returns Result.success(daily)
    }

    /** loadingWeather 会并行反查地名,测试需为 reverseGeocode 打桩 */
    private fun stubNameSuccess() {
        coEvery { repository.reverseGeocode(any()) } returns Result.success("北京")
    }

    private fun mockLocation(): Location = mockk<Location>().apply {
        every { latitude } returns 39.9042
        every { longitude } returns 116.4074
        // 新逻辑会读 Location.time 做新鲜度判定:默认给"刚刚定位",避免 strict mock 缺省抛错
        every { time } returns System.currentTimeMillis()
    }

    private fun calcSunProgress(viewModel: WeatherViewModel, rise: String?, set: String?): Float {
        val method = WeatherViewModel::class.java
            .getDeclaredMethod("calcSunProgress", String::class.java, String::class.java)
        method.isAccessible = true
        return method.invoke(viewModel, rise, set) as Float
    }

    // ── 加载流程 ──

    @Test
    fun `initial state is Loading`() {
        val vm = viewModel()

        // sealed 状态机:首帧必为 Loading,不再存在"平铺字段全空"的第四种组合(P2-8)
        assertTrue(vm.uiState.value is WeatherUiState.Loading)
    }

    @Test
    fun `loadFromCurrentLocation loads weather and resolves location name`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            val location = mockLocation()
            stubNoCachedLocation()
            coEvery { locationTracker.getCurrentLocation() } returns Result.success(location)
            every { locationTracker.formatLocation(location) } returns "116.41,39.90"
            coEvery { repository.reverseGeocode("116.41,39.90") } returns Result.success("北京")
            stubWeatherSuccess()

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            advanceUntilIdle()

            val state = ready(vm)
            assertEquals(current, state.current)
            assertEquals(hourly, state.hourly)
            assertEquals(daily, state.daily)
            assertEquals("北京", state.locationName)
            assertFalse(state.isRefreshing)

            verify(exactly = 1) { locationTracker.formatLocation(location) }
            coVerify(exactly = 1) { repository.reverseGeocode("116.41,39.90") }
            coVerify(exactly = 1) { repository.getCurrentWeather("116.41,39.90") }
            coVerify(exactly = 1) { repository.getHourlyForecast("116.41,39.90") }
            coVerify(exactly = 1) { repository.getDailyForecast("116.41,39.90") }
        }

    @Test
    fun `location failure falls back to default location`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            stubNoCachedLocation()
            coEvery { locationTracker.getCurrentLocation() } returns
                Result.failure(SecurityException("permission denied"))
            coEvery { repository.reverseGeocode(WeatherViewModel.DEFAULT_LOCATION) } returns
                Result.success("北京")
            stubWeatherSuccess()

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            advanceUntilIdle()

            val state = ready(vm)
            assertEquals("北京", state.locationName)
            assertFalse(state.isRefreshing)
            coVerify(exactly = 1) {
                repository.getCurrentWeather(WeatherViewModel.DEFAULT_LOCATION)
            }
            coVerify(exactly = 1) {
                repository.getHourlyForecast(WeatherViewModel.DEFAULT_LOCATION)
            }
            coVerify(exactly = 1) {
                repository.getDailyForecast(WeatherViewModel.DEFAULT_LOCATION)
            }

            // refresh 复用回退后的默认城市(forceRefresh = true 走强制刷新路径)
            vm.refresh()
            advanceUntilIdle()
            coVerify(exactly = 2) {
                repository.getCurrentWeather(WeatherViewModel.DEFAULT_LOCATION, any())
            }
        }

    @Test
    fun `reverseGeocode failure keeps default location name`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
            val location = mockLocation()
            coEvery { locationTracker.getCurrentLocation() } returns Result.success(location)
            every { locationTracker.formatLocation(location) } returns "116.41,39.90"
            stubNoCachedLocation()
            coEvery { repository.reverseGeocode(any()) } returns Result.failure(Exception("geo down"))
            stubWeatherSuccess()

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            advanceUntilIdle()

            val state = ready(vm)
            assertEquals("北京", state.locationName)
            assertEquals(current, state.current)
        }

    @Test
    fun `city name resolved before weather arrives survives into Ready`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            // 冷启动竞态:反查快、current 慢。地名到达时无 Ready 可写,
            // 必须暂存并在天气投影时消费——否则地名被默认值覆盖丢失。
            val gate = CompletableDeferred<Result<CurrentWeather>>()
            coEvery { repository.getCurrentWeather(any(), any()) } coAnswers { gate.await() }
            coEvery { repository.getHourlyForecast(any(), any()) } returns Result.success(hourly)
            coEvery { repository.getDailyForecast(any(), any()) } returns Result.success(daily)
            coEvery { repository.reverseGeocode(any()) } returns Result.success("上海")

            val vm = viewModel()
            vm.loadingWeather("1.00,2.00")
            runCurrent()
            // 地名已回,天气未到:仍是 Loading
            assertTrue(vm.uiState.value is WeatherUiState.Loading)

            gate.complete(Result.success(current))
            advanceUntilIdle()
            assertEquals("上海", ready(vm).locationName)
        }

    @Test
    fun `loadingWeather loads the requested location and refresh reuses it`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
            stubNameSuccess()
            stubWeatherSuccess()

            val vm = viewModel()
            vm.loadingWeather("1.00,2.00")
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.getCurrentWeather("1.00,2.00") }

            vm.refresh()
            advanceUntilIdle()
            // 首次加载(force=false) + 刷新(force=true) 各一次
            coVerify(exactly = 2) { repository.getCurrentWeather("1.00,2.00", any()) }
        }

    @Test
    fun `refresh forces network and keeps indicator for min duration`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            stubWeatherSuccess()
            stubNameSuccess()

            val vm = viewModel()
            vm.loadingWeather("116.41,39.92")
            advanceUntilIdle()
            assertFalse(ready(vm).isRefreshing)

            // 有内容的热刷新:gate 卡住 current,验证 isRefreshing 旗标生命周期
            val gate = CompletableDeferred<Result<CurrentWeather>>()
            coEvery { repository.getCurrentWeather(any(), any()) } coAnswers { gate.await() }
            vm.refresh()
            runCurrent()
            assertTrue("刷新期间 isRefreshing 应为 true", ready(vm).isRefreshing)

            gate.complete(Result.success(current))
            runCurrent()
            assertTrue("最小指示时长内 isRefreshing 应保持 true", ready(vm).isRefreshing)

            advanceTimeBy(600)
            runCurrent()
            assertFalse("刷新完成后 isRefreshing 应为 false", ready(vm).isRefreshing)

            // 刷新路径全部 forceRefresh = true(首次加载的 4 路 + 刷新的 3 路共查)
            coVerify(exactly = 1) {
                repository.getCurrentWeather("116.41,39.92", true)
            }
            coVerify(exactly = 1) {
                repository.getHourlyForecast("116.41,39.92", true)
            }
            coVerify(exactly = 1) {
                repository.getDailyForecast("116.41,39.92", true)
            }
        }

    // ── 加载中状态与防重入 ──

    @Test
    fun `loadFromCurrentLocation shows loading and guards re-entry`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
            stubNoCachedLocation()
            val gate = CompletableDeferred<Result<Location>>()
            coEvery { locationTracker.getCurrentLocation() } coAnswers { gate.await() }
            coEvery { repository.reverseGeocode(any()) } returns Result.success("北京")
            stubWeatherSuccess()

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            runCurrent()

            assertTrue(vm.uiState.value is WeatherUiState.Loading)

            // 加载期间再次触发加载应被忽略
            vm.loadFromCurrentLocation()
            vm.loadingWeather("9.00,9.00")
            runCurrent()

            coVerify(exactly = 1) { locationTracker.getCurrentLocation() }

            gate.complete(Result.failure(IllegalStateException("stop")))
            advanceUntilIdle()

            assertEquals(current, ready(vm).current)
            // 期间未发起任何额外天气请求
            coVerify(exactly = 1) { repository.getCurrentWeather(any()) }
            coVerify(exactly = 0) { repository.getCurrentWeather("9.00,9.00") }
        }

    // ── isNow 标记 ──

    @Test
    fun `hourly item matching current hour is marked isNow`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
            val currentHour = LocalTime.now().hour
            val nowHour = "%02d".format(currentHour)
            val otherHour = "%02d".format((currentHour + 1) % 24)
            val hours = listOf(
                HourlyForecast(time = "$nowHour:00", tempCelsius = 20.0, iconCode = "100", popPct = 5, isNow = false),
                HourlyForecast(time = "$otherHour:00", tempCelsius = 21.0, iconCode = "100", popPct = 5, isNow = false),
                HourlyForecast(time = "xx:00", tempCelsius = 19.0, iconCode = "100", popPct = 5, isNow = false),
            )
            coEvery { repository.getCurrentWeather(any()) } returns Result.success(current)
            coEvery { repository.getHourlyForecast(any()) } returns Result.success(hours)
            coEvery { repository.getDailyForecast(any()) } returns Result.success(daily)
            stubNameSuccess()

            val vm = viewModel()
            vm.loadingWeather("116.41,39.92")
            advanceUntilIdle()

            val result = ready(vm).hourly
            assertEquals(3, result.size)
            assertTrue(result[0].isNow)
            assertFalse(result[1].isNow)
            assertFalse(result[2].isNow)
        }

    @Test
    fun `hourly item is marked isNow by the timezone carried in rawTime`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
            // 当前时刻换算到 UTC+8 的整点,模拟目标城市时区的"当前小时"
            val targetNow = OffsetDateTime.now()
                .withOffsetSameInstant(ZoneOffset.ofHours(8))
                .truncatedTo(ChronoUnit.HOURS)
            val hours = listOf(
                HourlyForecast(
                    time = targetNow.toLocalTime().truncatedTo(ChronoUnit.HOURS).toString(),
                    tempCelsius = 20.0, iconCode = "100", popPct = 5, isNow = false,
                    rawTime = targetNow.toString(),
                ),
                HourlyForecast(
                    time = targetNow.plusHours(1).toLocalTime().truncatedTo(ChronoUnit.HOURS).toString(),
                    tempCelsius = 21.0, iconCode = "100", popPct = 5, isNow = false,
                    rawTime = targetNow.plusHours(1).toString(),
                ),
            )
            coEvery { repository.getCurrentWeather(any()) } returns Result.success(current)
            coEvery { repository.getHourlyForecast(any()) } returns Result.success(hours)
            coEvery { repository.getDailyForecast(any()) } returns Result.success(daily)
            stubNameSuccess()

            val vm = viewModel()
            vm.loadingWeather("116.41,39.92")
            advanceUntilIdle()

            val result = ready(vm).hourly
            assertEquals(2, result.size)
            assertTrue("当前小时应被标记 isNow", result[0].isNow)
            assertFalse("下一小时不应标记", result[1].isNow)
        }

    // ── 失败分级(P2-6:current 整页失败;hourly/daily/air 卡级静默) ──

    @Test
    fun `hourly failure keeps hourly empty without crashing`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
            coEvery { repository.getCurrentWeather(any()) } returns Result.success(current)
            coEvery { repository.getHourlyForecast(any()) } returns
                Result.failure(Exception("hourly boom"))
            coEvery { repository.getDailyForecast(any()) } returns Result.success(daily)
            stubNameSuccess()

            val vm = viewModel()
            vm.loadingWeather("116.41,39.92")
            advanceUntilIdle()

            // hourly 失败静默:进 Ready、hourly 空、不触发 Failed
            val state = ready(vm)
            assertTrue(state.hourly.isEmpty())
            assertEquals(current, state.current)
        }

    @Test
    fun `current failure on empty page sets Failed`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
            coEvery { repository.getCurrentWeather(any()) } returns
                Result.failure(Exception("api boom"))
            coEvery { repository.getHourlyForecast(any()) } returns Result.success(hourly)
            coEvery { repository.getDailyForecast(any()) } returns Result.success(daily)
            stubNameSuccess()

            val vm = viewModel()
            vm.loadingWeather("116.41,39.92")
            advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue(state is WeatherUiState.Failed)
            assertEquals("api boom", (state as WeatherUiState.Failed).message)
        }

    @Test
    fun `current failure during refresh keeps Ready with old data`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            stubWeatherSuccess()
            stubNameSuccess()
            val vm = viewModel()
            vm.loadingWeather("116.41,39.92")
            advanceUntilIdle()

            // 刷新时 current 失败:已有内容保持 Ready,不打回 Failed(旧数据可看)
            coEvery { repository.getCurrentWeather(any(), any()) } returns
                Result.failure(Exception("network flaky"))
            vm.refresh()
            advanceUntilIdle()

            val state = ready(vm)
            assertEquals(current, state.current)
            assertFalse(state.isRefreshing)
        }

    @Test
    fun `air loaded with weather snapshot and exposed in Ready`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
            stubWeatherSuccess()
            stubNameSuccess()

            val vm = viewModel()
            vm.loadingWeather("116.41,39.92")
            advanceUntilIdle()

            assertEquals(air, ready(vm).air)
            coVerify(exactly = 1) { repository.getAirQuality("116.41,39.92") }
        }

    @Test
    fun `air failure stays silent and clears air without touching weather state`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            stubWeatherSuccess()
            stubNameSuccess()
            val vm = viewModel()
            // 覆盖 init 里的默认 air 成功桩(mockk 后写生效,必须在 VM 构造之后打)
            coEvery { repository.getAirQuality(any(), any()) } returns
                Result.failure(Exception("air boom"))
            vm.loadingWeather("116.41,39.92")
            advanceUntilIdle()

            // P2-6 静默语义:AQI 失败不落 Failed、不影响 current;air=null 即 AQI 卡不渲染
            val state = ready(vm)
            org.junit.Assert.assertNull(state.air)
            assertEquals(current, state.current)
        }

    // ── 温度单位(P1-2 延伸:VM 直暴 DataStore 流,不再镜像进页面态) ──

    @Test
    fun `temperature unit flow passes through without refetching`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            val unitFlow = MutableStateFlow(TemperatureUnit.CELSIUS)
            every { preferences.temperatureUnit } returns unitFlow
            stubNameSuccess()
            stubWeatherSuccess()

            val vm = WeatherViewModel(repository, locationTracker, preferences)
            assertEquals(TemperatureUnit.CELSIUS, vm.temperatureUnit.first())

            // 单位切换即时反映到流上,且不触发任何网络请求(domain 存原始值,UI 重格式化)
            unitFlow.value = TemperatureUnit.FAHRENHEIT
            assertEquals(TemperatureUnit.FAHRENHEIT, vm.temperatureUnit.first())
            coVerify(exactly = 0) { repository.getCurrentWeather(any()) }

            // 后续加载与单位无关(Ready 类型上不带单位:双写源已移除)
            vm.loadingWeather("116.41,39.92")
            advanceUntilIdle()
            assertEquals(TemperatureUnit.FAHRENHEIT, vm.temperatureUnit.first())
            coVerify(exactly = 1) { repository.getCurrentWeather("116.41,39.92") }
            coVerify(exactly = 1) { repository.getHourlyForecast("116.41,39.92") }
            coVerify(exactly = 1) { repository.getDailyForecast("116.41,39.92") }
        }

    // ── calcSunProgress 边界 ──

    @Test
    fun `calcSunProgress returns zero for missing or invalid input`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
            val vm = viewModel()

            assertEquals(0f, calcSunProgress(vm, null, null))
            assertEquals(0f, calcSunProgress(vm, "", ""))
            assertEquals(0f, calcSunProgress(vm, "invalid", "19:00"))
            assertEquals(0f, calcSunProgress(vm, "05:00", "invalid"))
            assertEquals(0f, calcSunProgress(vm, "19:00", "05:00"))
            assertEquals(0f, calcSunProgress(vm, "05:00", "05:00"))
        }

    @Test
    fun `calcSunProgress stays within zero and one for a valid window`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
            val vm = viewModel()

            val progress = calcSunProgress(vm, "05:00", "19:00")
            assertTrue(progress in 0f..1f)
        }

    @Test
    fun `sunProgress stays zero when daily forecast is empty`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
            coEvery { repository.getCurrentWeather(any()) } returns Result.success(current)
            coEvery { repository.getHourlyForecast(any()) } returns Result.success(hourly)
            coEvery { repository.getDailyForecast(any()) } returns Result.success(emptyList())
            stubNameSuccess()

            val vm = viewModel()
            vm.loadingWeather("116.41,39.92")
            advanceUntilIdle()

            assertEquals(0f, ready(vm).sunProgress)
        }

    @Test
    fun `sunProgress is computed from first daily forecast`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
            coEvery { repository.getCurrentWeather(any()) } returns Result.success(current)
            coEvery { repository.getHourlyForecast(any()) } returns Result.success(hourly)
            coEvery { repository.getDailyForecast(any()) } returns Result.success(daily)
            stubNameSuccess()

            val vm = viewModel()
            vm.loadingWeather("116.41,39.92")
            advanceUntilIdle()

            assertTrue(ready(vm).sunProgress in 0f..1f)
        }

    // ── 换城复查(回前台 recheckLocationIfMoved) ──

    /** 北京锚点 + 上海新定位,越阈值时复查应整页切到新坐标 */
    @Test
    fun `recheck switches city when moved beyond threshold`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            val beijing = mockLocation()
            val shanghai = mockk<Location>().apply {
                every { latitude } returns 31.2304
                every { longitude } returns 121.4737
                every { time } returns System.currentTimeMillis()
            }
            stubNoCachedLocation()
            coEvery { locationTracker.getCurrentLocation() } returnsMany
                listOf(Result.success(beijing), Result.success(shanghai))
            every { locationTracker.formatLocation(beijing) } returns "116.41,39.90"
            every { locationTracker.formatLocation(shanghai) } returns "121.47,31.23"
            every { locationTracker.distanceMeters(beijing, shanghai) } returns 1_000_000f
            stubWeatherSuccess()
            coEvery { repository.reverseGeocode("116.41,39.90") } returns Result.success("北京")
            coEvery { repository.reverseGeocode("121.47,31.23") } returns Result.success("上海")

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            advanceUntilIdle()
            assertEquals("北京", ready(vm).locationName)

            vm.recheckLocationIfMoved()
            advanceUntilIdle()

            // 新坐标完成整页加载,地名随反查更新
            coVerify(exactly = 1) { repository.getCurrentWeather("121.47,31.23") }
            assertEquals("上海", ready(vm).locationName)
        }

    @Test
    fun `recheck keeps current city when movement stays below threshold`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            val loc = mockLocation()
            stubNoCachedLocation()
            coEvery { locationTracker.getCurrentLocation() } returns Result.success(loc)
            every { locationTracker.formatLocation(loc) } returns "116.41,39.90"
            // 市内通勤/漂移:同一点返回 5km
            every { locationTracker.distanceMeters(any(), any()) } returns 5_000f
            stubWeatherSuccess()
            stubNameSuccess()

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            advanceUntilIdle()

            vm.recheckLocationIfMoved()
            advanceUntilIdle()

            // 未越阈值:不追发任何天气请求,页面保持原状且不闪 Loading
            coVerify(exactly = 1) { repository.getCurrentWeather("116.41,39.90") }
            assertTrue(vm.uiState.value is WeatherUiState.Ready)
            assertFalse(ready(vm).isRefreshing)
        }

    @Test
    fun `recheck recovers to real city when location service comes back`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            // 初定位失败(冷启动无信号)回退默认城市后,回前台复查不得因"无锚点"
            // 永久跳过——否则页面钉死默认城市,正是本 bug 的第三种表现。
            val loc = mockLocation()
            stubNoCachedLocation()
            coEvery { locationTracker.getCurrentLocation() } returnsMany listOf(
                Result.failure(IllegalStateException("定位超时,请重试")),
                Result.success(loc),
            )
            every { locationTracker.formatLocation(loc) } returns "116.41,39.90"
            stubWeatherSuccess()
            coEvery { repository.reverseGeocode(any()) } returns Result.success("北京")

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.getCurrentWeather(WeatherViewModel.DEFAULT_LOCATION) }

            vm.recheckLocationIfMoved()
            advanceUntilIdle()

            // 自救:成功定位后整页切到真实坐标
            coVerify(exactly = 1) { repository.getCurrentWeather("116.41,39.90") }
            assertEquals("北京", ready(vm).locationName)
        }

    @Test
    fun `recheck stays silent when re-location fails again`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            stubNoCachedLocation()
            coEvery { locationTracker.getCurrentLocation() } returns
                Result.failure(IllegalStateException("定位超时,请重试"))
            stubWeatherSuccess()
            stubNameSuccess()

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.getCurrentWeather(WeatherViewModel.DEFAULT_LOCATION) }

            // 复查再失败:不追发任何天气请求,页面保持原状
            vm.recheckLocationIfMoved()
            advanceUntilIdle()
            coVerify(exactly = 2) { locationTracker.getCurrentLocation() }
            coVerify(exactly = 1) { repository.getCurrentWeather(WeatherViewModel.DEFAULT_LOCATION) }
        }

    @Test
    fun `recheck never overrides manually selected city`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { locationTracker.getCurrentLocation() } returns Result.success(mockLocation())
            stubWeatherSuccess()
            stubNameSuccess()

            val vm = viewModel()
            vm.loadingWeather("1.00,2.00")
            advanceUntilIdle()

            vm.recheckLocationIfMoved()
            advanceUntilIdle()

            // 手动选城无定位锚点:复查不得触碰定位,用户选择不能被"抢回去"
            coVerify(exactly = 0) { locationTracker.getCurrentLocation() }
            coVerify(exactly = 1) { repository.getCurrentWeather("1.00,2.00") }
        }

    // ── 分段式首屏与刷新重定位(实测:主动定位室内可挂满 15s,首屏不同步等它) ──

    @Test
    fun `stale cached fix renders instantly then refines via recheck`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            val stale = mockLocation().apply {
                every { time } returns System.currentTimeMillis() - 60 * 60_000L
            }
            val shanghai = mockk<Location>().apply {
                every { latitude } returns 31.2304
                every { longitude } returns 121.4737
                every { time } returns System.currentTimeMillis()
            }
            coEvery { locationTracker.getCachedLocation() } returns stale
            coEvery { locationTracker.getCurrentLocation() } returns Result.success(shanghai)
            every { locationTracker.formatLocation(stale) } returns "116.41,39.90"
            every { locationTracker.formatLocation(shanghai) } returns "121.47,31.23"
            every { locationTracker.distanceMeters(stale, shanghai) } returns 1_000_000f
            stubWeatherSuccess()
            coEvery { repository.reverseGeocode(any()) } returns Result.success("城市")

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            advanceUntilIdle()

            // 首屏用缓存坐标出内容;过期 → 同一轮里自动后台复查精修到新坐标
            coVerify(exactly = 1) { repository.getCurrentWeather("116.41,39.90") }
            coVerify(exactly = 1) { repository.getCurrentWeather("121.47,31.23") }
        }

    @Test
    fun `fresh cached fix renders without any recheck`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            val fresh = mockLocation() // time=now → 过新鲜度门槛
            coEvery { locationTracker.getCachedLocation() } returns fresh
            every { locationTracker.formatLocation(fresh) } returns "116.41,39.90"
            stubWeatherSuccess()
            stubNameSuccess()

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.getCurrentWeather("116.41,39.90") }
            coVerify(exactly = 0) { locationTracker.getCurrentLocation() }
        }

    @Test
    fun `refresh in location mode re-locates before fetching`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            val loc = mockLocation()
            stubNoCachedLocation()
            coEvery { locationTracker.getCurrentLocation() } returns Result.success(loc)
            every { locationTracker.formatLocation(loc) } returns "121.47,31.23"
            every { locationTracker.distanceMeters(any(), any()) } returns 5_000f
            stubWeatherSuccess()
            stubNameSuccess()

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            advanceUntilIdle()

            vm.refresh()
            advanceUntilIdle()

            // 定位模式下刷新前重新定位:换城后下拉刷新刷的是新坐标而非旧城市
            coVerify(exactly = 2) { locationTracker.getCurrentLocation() }
            coVerify(exactly = 2) { repository.getCurrentWeather("121.47,31.23", any()) }
        }

    @Test
    fun `refresh falls back to old coords when re-location fails`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            val loc = mockLocation()
            stubNoCachedLocation()
            coEvery { locationTracker.getCurrentLocation() } returnsMany listOf(
                Result.success(loc),
                Result.failure(IllegalStateException("定位超时,请重试")),
            )
            every { locationTracker.formatLocation(loc) } returns "116.41,39.90"
            stubWeatherSuccess()
            stubNameSuccess()

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            advanceUntilIdle()

            // 刷新时定位失败:留在旧坐标完成刷新,不因此进 Failed
            vm.refresh()
            advanceUntilIdle()
            coVerify(exactly = 2) { repository.getCurrentWeather("116.41,39.90", any()) }
            assertTrue(vm.uiState.value is WeatherUiState.Ready)
        }
}