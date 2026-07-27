package com.yuzheng.kairoweather.ui.theme.weather.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

fun qWeatherIcon(code: String): ImageVector = when (code.toIntOrNull() ?: 999) {
    // 晴
    100 -> Icons.Default.WbSunny
    // 多云
    101, 102, 103 -> Icons.Default.WbCloudy
    // 阴
    104 -> Icons.Default.Cloud
    // 夜间晴
    150 -> Icons.Default.NightlightRound
    // 夜间多云/阴
    151, 152, 153, 154 -> Icons.Default.Cloud
    // 雨 (300-399)
    in 300..399 -> Icons.Default.Umbrella
    // 雪 (400-499)
    in 400..499 -> Icons.Default.AcUnit
    // 雾/霾/沙尘
    in 500..599 -> Icons.Default.Cloud
    else -> Icons.Default.WbSunny
}
