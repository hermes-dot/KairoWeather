package com.yuzheng.kairoweather.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yuzheng.kairoweather.ui.settings.SettingsScreen
import com.yuzheng.kairoweather.ui.weather.WeatherScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        // content 顶到窗口 y=0:天气页渐变才能穿到状态栏后。
        // 默认 safeDrawing 会把 content 下推一个状态栏高度,而页面顶部 TopAppBar
        // 自身又吃一份 statusBars insets——双重叠加后标题距屏幕顶 2×状态栏高,
        // 渐变被推下、状态栏带只剩 window 底色(实机表现为不透明黑带)。
        // 顶部 insets 由各屏 TopAppBar(windowInsets 默认)自管,底部由 NavigationBar 自管。
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                Screen.bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == screen.route) screen.selectedIcon
                                    else screen.unselectedIcon,
                                contentDescription = screen.label,
                            )
                        },
                        label = { Text(screen.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Weather.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Weather.route) {
                WeatherScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
