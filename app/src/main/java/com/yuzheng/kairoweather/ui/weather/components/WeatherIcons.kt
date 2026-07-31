package com.yuzheng.kairoweather.ui.weather.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.concurrent.ConcurrentHashMap

private val iconCache = ConcurrentHashMap<String, ImageVector>()

fun qWeatherIcon(code: String): ImageVector = iconCache.getOrPut(code) {
    when (code.toIntOrNull() ?: 999) {
        100 -> Icons.Default.WbSunny
        101, 102, 103 -> Icons.Default.WbCloudy
        104 -> Icons.Default.Cloud
        150 -> Icons.Default.NightlightRound
        151, 152, 153, 154 -> Icons.Default.Cloud
        in 300..399 -> Icons.Default.Umbrella
        in 400..499 -> Icons.Default.AcUnit
        in 500..599 -> Icons.Default.Cloud
        else -> Icons.Default.WbSunny
    }
}
