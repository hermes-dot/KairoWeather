package com.yuzheng.kairoweather.ui.weather.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

// P2-C6: QWeather code 有限(~30 个),去掉进程级 ConcurrentHashMap 缓存,
// 改回纯 when 映射;ImageVector 本身是单例,组合期引用零开销,缓存无收益。
// 调用方如需避免重复映射,由各自 remember 处理。
fun qWeatherIcon(code: String): ImageVector = when (code.toIntOrNull() ?: 999) {
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
