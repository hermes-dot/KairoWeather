package com.yuzheng.kairoweather.data.location

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationTrackerTest {

    private fun location(lat: Double, lon: Double): Location = mockk<Location>().apply {
        every { latitude } returns lat
        every { longitude } returns lon
    }

    /** 立即完成的 Task 桩:成功携带 [value],或(isSuccessful=false)抛 [error]。 */
    private fun finishedTask(value: Location?, error: Exception? = null): Task<Location> {
        val task = mockk<Task<Location>>()
        every { task.isSuccessful } returns (error == null)
        every { task.result } returns value
        every { task.exception } returns error
        every { task.addOnCompleteListener(any()) } answers {
            firstArg<OnCompleteListener<Location>>().onComplete(task)
            task
        }
        return task
    }

    @Test
    fun `formatLocation uses dot decimal separator regardless of default locale`() {
        val originalLocale = Locale.getDefault()
        try {
            // 德语地区用逗号作为小数分隔符;修复前 "%.2f".format(...) 会输出 "116,41"
            Locale.setDefault(Locale.GERMANY)
            val tracker = LocationTracker(fusedClient = mockk<FusedLocationProviderClient>(relaxed = true))

            assertEquals("116.41,39.90", tracker.formatLocation(location(39.9042, 116.4074)))
            assertEquals("0.00,0.00", tracker.formatLocation(location(0.0, 0.0)))
            assertEquals("12.50,-8.25", tracker.formatLocation(location(-8.25, 12.5)))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    // ── lastKnown 新鲜度门槛 ──

    @Test
    fun `isFresh accepts fix within max age window`() {
        val now = System.currentTimeMillis()
        assertTrue(LocationTracker.isFresh(now - 60_000L, now))
        assertTrue(LocationTracker.isFresh(now - 9 * 60_000L, now))
    }

    @Test
    fun `isFresh rejects stale, missing and future timestamps`() {
        val now = System.currentTimeMillis()
        // 10 分钟门槛之外:可能钉死旧城市的陈旧缓存
        assertFalse(LocationTracker.isFresh(now - 11 * 60_000L, now))
        assertFalse(LocationTracker.isFresh(0L, now))
        // 时钟回拨/未来时间戳:age < 0 同样视为不可信
        assertFalse(LocationTracker.isFresh(now + 1000L, now))
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `fresh lastKnown short-circuits active fix`() {
        kotlinx.coroutines.runBlocking {
            val fresh = location(39.9, 116.4).let {
                every { it.time } returns System.currentTimeMillis()
                it
            }
            val client = mockk<FusedLocationProviderClient> {
                every { lastLocation } returns finishedTask(fresh)
            }
            val tracker = LocationTracker(client)

            val result = tracker.getCurrentLocation()

            assertTrue(result.isSuccess)
            assertEquals(fresh, result.getOrNull())
            // 新鲜缓存命中时不发起主动定位(省电路径保留)
            verify(exactly = 0) { client.getCurrentLocation(any<Int>(), null) }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `stale lastKnown triggers active fix`() {
        kotlinx.coroutines.runBlocking {
            val stale = location(39.9, 116.4).let {
                every { it.time } returns System.currentTimeMillis() - 60 * 60_000L
                it
            }
            val fresh = location(31.2, 121.5)
            val client = mockk<FusedLocationProviderClient> {
                every { lastLocation } returns finishedTask(stale)
                every { getCurrentLocation(any<Int>(), null) } returns finishedTask(fresh)
            }
            val tracker = LocationTracker(client)

            val result = tracker.getCurrentLocation()

            // 陈旧缓存不得短路返回——换城后继续用它就是初定位钉死 bug
            assertEquals(fresh, result.getOrNull())
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `active fix failure falls back to stale lastKnown instead of failing`() {
        kotlinx.coroutines.runBlocking {
            val stale = location(39.9, 116.4).let {
                every { it.time } returns System.currentTimeMillis() - 60 * 60_000L
                it
            }
            val client = mockk<FusedLocationProviderClient> {
                every { lastLocation } returns finishedTask(stale)
                every { getCurrentLocation(any<Int>(), null) } returns
                    finishedTask(null, error = Exception("no provider"))
            }
            val tracker = LocationTracker(client)

            val result = tracker.getCurrentLocation()

            // 旧坐标至少曾是真实位置,好过整页回退默认城市
            assertEquals(stale, result.getOrNull())
        }
    }

    @Test
    fun `distanceMeters delegates to Location distanceTo`() {
        val from = location(39.9, 116.4)
        val to = location(31.2, 121.5)
        every { from.distanceTo(to) } returns 1_200_000f
        val tracker = LocationTracker(fusedClient = mockk<FusedLocationProviderClient>(relaxed = true))

        assertEquals(1_200_000f, tracker.distanceMeters(from, to), 0f)
    }
}
