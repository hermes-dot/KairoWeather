package com.yuzheng.kairoweather.data.location

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationTrackerTest {

    private fun location(lat: Double, lon: Double): Location = mockk<Location>().apply {
        every { latitude } returns lat
        every { longitude } returns lon
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
}
