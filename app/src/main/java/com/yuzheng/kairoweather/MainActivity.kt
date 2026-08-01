package com.yuzheng.kairoweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.yuzheng.kairoweather.data.preferences.UserPreferences
import com.yuzheng.kairoweather.domain.model.ThemeMode
import com.yuzheng.kairoweather.ui.navigation.MainScreen
import com.yuzheng.kairoweather.ui.theme.KairoWeatherTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // P2-C7: produceState 挂起首帧直到 DataStore 出值,避免首帧按 "system" 闪变;
            // 主题已枚举化(批次C),ThemeMode.SYSTEM 仅作读取前的初始占位。
            val themeMode by produceState(initialValue = ThemeMode.SYSTEM) {
                value = preferences.themeMode.first()
            }
            KairoWeatherTheme(themeMode = themeMode) {
                MainScreen()
            }
        }
    }
}
