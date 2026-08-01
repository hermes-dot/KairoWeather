package com.yuzheng.kairoweather.ui.weather

import android.location.Location
import com.yuzheng.kairoweather.data.location.LocationTracker
import com.yuzheng.kairoweather.data.preferences.UserPreferences
import com.yuzheng.kairoweather.data.repository.WeatherRepository
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    private val hourly = listOf(
        HourlyForecast(time = "12:00", tempCelsius = 24.0, iconCode = "100", popPct = 10, isNow = false),
        HourlyForecast(time = "13:00", tempCelsius = 25.0, iconCode = "100", popPct = 20, isNow = false),
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

    private fun viewModel(unit: TemperatureUnit = TemperatureUnit.CELSIUS): WeatherViewModel {
        every { preferences.temperatureUnit } returns flowOf(unit)
        return WeatherViewModel(repository, locationTracker, preferences)
    }

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
    }

    private fun calcSunProgress(viewModel: WeatherViewModel, rise: String?, set: String?): Float {
        val method = WeatherViewModel::class.java
            .getDeclaredMethod("calcSunProgress", String::class.java, String::class.java)
        method.isAccessible = true
        return method.invoke(viewModel, rise, set) as Float
    }

    // ── 加载流程 ──

    @Test
    fun `initial state uses defaults`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
        val vm = viewModel()
        runCurrent()

        val state = vm.uiState.value
        assertNull(state.current)
        assertTrue(state.hourly.isEmpty())
        assertTrue(state.daily.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(0f, state.sunProgress)
        assertEquals("北京", state.locationName)
        assertEquals(TemperatureUnit.CELSIUS, state.temperatureUnit)
    }

    @Test
    fun `loadFromCurrentLocation loads weather and resolves location name`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            val location = mockLocation()
            coEvery { locationTracker.getCurrentLocation() } returns Result.success(location)
            every { locationTracker.formatLocation(location) } returns "116.41,39.90"
            coEvery { repository.reverseGeocode("116.41,39.90") } returns Result.success("北京")
            stubWeatherSuccess()

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(current, state.current)
            assertEquals(hourly, state.hourly)
            assertEquals(daily, state.daily)
            assertEquals("北京", state.locationName)
            assertFalse(state.isLoading)
            assertNull(state.error)

            verify(exactly = 1) { locationTracker.formatLocation(location) }
            coVerify(exactly = 1) { repository.reverseGeocode("116.41,39.90") }
            coVerify(exactly = 1) { repository.getCurrentWeather("116.41,39.90") }
            coVerify(exactly = 1) { repository.getHourlyForecast("116.41,39.90") }
            coVerify(exactly = 1) { repository.getDailyForecast("116.41,39.90") }
        }

    @Test
    fun `location failure falls back to default location`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { locationTracker.getCurrentLocation() } returns
                Result.failure(SecurityException("permission denied"))
            coEvery { repository.reverseGeocode(WeatherViewModel.DEFAULT_LOCATION) } returns
                Result.success("北京")
            stubWeatherSuccess()

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            advanceUntilIdle()

            assertEquals("北京", vm.uiState.value.locationName)
            assertNull(vm.uiState.value.error)
            assertFalse(vm.uiState.value.isLoading)
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
            coEvery { repository.reverseGeocode(any()) } returns Result.failure(Exception("geo down"))
            stubWeatherSuccess()

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            advanceUntilIdle()

            assertEquals("北京", vm.uiState.value.locationName)
            assertEquals(current, vm.uiState.value.current)
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
            // gate 控制 current 请求返回时机,验证刷新期间 isLoading 持续为 true
            val gate = CompletableDeferred<Result<CurrentWeather>>()
            coEvery { repository.getCurrentWeather(any(), any()) } coAnswers { gate.await() }
            coEvery { repository.getHourlyForecast(any(), any()) } returns Result.success(hourly)
            coEvery { repository.getDailyForecast(any(), any()) } returns Result.success(daily)

            val vm = viewModel()
            vm.refresh()
            runCurrent()

            assertTrue("刷新期间 isLoading 应为 true", vm.uiState.value.isLoading)

            // 放行网络请求:此时网络已返回,但最小指示时长未到,isLoading 应保持 true
            gate.complete(Result.success(current))
            runCurrent()
            assertTrue("最小指示时长内 isLoading 应保持 true", vm.uiState.value.isLoading)

            // 推进虚拟时间越过最小指示时长,isLoading 才结束
            advanceTimeBy(600)
            runCurrent()

            assertFalse("刷新完成后 isLoading 应为 false", vm.uiState.value.isLoading)
            // 三个请求均走 forceRefresh = true 路径
            coVerify(exactly = 1) {
                repository.getCurrentWeather(WeatherViewModel.DEFAULT_LOCATION, true)
            }
            coVerify(exactly = 1) {
                repository.getHourlyForecast(WeatherViewModel.DEFAULT_LOCATION, true)
            }
            coVerify(exactly = 1) {
                repository.getDailyForecast(WeatherViewModel.DEFAULT_LOCATION, true)
            }
        }

    // ── 加载中状态与防重入 ──

    @Test
    fun `loadFromCurrentLocation shows loading and guards re-entry`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Result<Location>>()
            coEvery { locationTracker.getCurrentLocation() } coAnswers { gate.await() }
            coEvery { repository.reverseGeocode(any()) } returns Result.success("北京")
            stubWeatherSuccess()

            val vm = viewModel()
            vm.loadFromCurrentLocation()
            runCurrent()

            assertTrue(vm.uiState.value.isLoading)

            // 加载期间再次触发加载应被忽略
            vm.loadFromCurrentLocation()
            vm.loadingWeather("9.00,9.00")
            runCurrent()

            coVerify(exactly = 1) { locationTracker.getCurrentLocation() }

            gate.complete(Result.failure(IllegalStateException("stop")))
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isLoading)
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

            val result = vm.uiState.value.hourly
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

            val result = vm.uiState.value.hourly
            assertEquals(2, result.size)
            assertTrue("当前小时应被标记 isNow", result[0].isNow)
            assertFalse("下一小时不应标记", result[1].isNow)
        }

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

            // 当前实现:逐小时失败被静默忽略,error 仅由当前天气失败触发
            assertNull(vm.uiState.value.error)
            assertTrue(vm.uiState.value.hourly.isEmpty())
            assertEquals(current, vm.uiState.value.current)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `current weather failure sets error and keeps loading false`(): Unit = runTest(mainDispatcherRule.testDispatcher) {
            coEvery { repository.getCurrentWeather(any()) } returns
                Result.failure(Exception("api boom"))
            coEvery { repository.getHourlyForecast(any()) } returns Result.success(hourly)
            coEvery { repository.getDailyForecast(any()) } returns Result.success(daily)
            stubNameSuccess()

            val vm = viewModel()
            vm.loadingWeather("116.41,39.92")
            advanceUntilIdle()

            assertEquals("api boom", vm.uiState.value.error)
            assertNull(vm.uiState.value.current)
            assertFalse(vm.uiState.value.isLoading)
        }

    // ── 温度单位(P1-2:单位切换不重拉数据,仅 UI 层重格式化) ──

    @Test
    fun `temperature unit flow updates state without refetching`(): Unit =
        runTest(mainDispatcherRule.testDispatcher) {
            val unitFlow = MutableStateFlow(TemperatureUnit.CELSIUS)
            every { preferences.temperatureUnit } returns unitFlow
            stubNameSuccess()
            stubWeatherSuccess()

            val vm = WeatherViewModel(repository, locationTracker, preferences)
            runCurrent()
            assertEquals(TemperatureUnit.CELSIUS, vm.uiState.value.temperatureUnit)

            // 单位切换后 state 立即反映新单位,但不应触发任何网络请求
            unitFlow.value = TemperatureUnit.FAHRENHEIT
            runCurrent()
            assertEquals(TemperatureUnit.FAHRENHEIT, vm.uiState.value.temperatureUnit)
            coVerify(exactly = 0) { repository.getCurrentWeather(any()) }

            // 后续加载不带单位参数
            vm.loadingWeather("116.41,39.92")
            advanceUntilIdle()

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

            assertEquals(0f, vm.uiState.value.sunProgress)
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

            assertTrue(vm.uiState.value.sunProgress in 0f..1f)
        }
}
