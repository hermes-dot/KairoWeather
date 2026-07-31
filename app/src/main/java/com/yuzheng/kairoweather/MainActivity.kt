package com.yuzheng.kairoweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import com.yuzheng.kairoweather.data.preferences.UserPreferences
import com.yuzheng.kairoweather.ui.navigation.MainScreen
import com.yuzheng.kairoweather.ui.theme.KairoWeatherTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by preferences.themeMode.collectAsStateWithLifecycle(initialValue = "system")
            KairoWeatherTheme(themeMode = themeMode) {
                MainScreen()
            }
        }
    }
}
