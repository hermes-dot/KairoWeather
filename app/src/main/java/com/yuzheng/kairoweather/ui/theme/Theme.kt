package com.yuzheng.kairoweather.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.yuzheng.kairoweather.domain.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = SkyBlue,
    onPrimary = OnSkyBlue,
    primaryContainer = SkyBlueContainer,
    onPrimaryContainer = OnSkyBlueContainer,
    secondary = RainTeal,
    onSecondary = OnRainTeal,
    secondaryContainer = RainTealContainer,
    onSecondaryContainer = OnRainTealContainer,
    tertiary = SunAmber,
    onTertiary = OnSunAmber,
    tertiaryContainer = SunAmberContainer,
    onTertiaryContainer = OnSunAmberContainer,
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = Color(0xFF2E3135),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = NightSkyBlue,
    surfaceTint = SkyBlue,
    scrim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
)

private val DarkColorScheme = darkColorScheme(
    primary = NightSkyBlue,
    onPrimary = OnNightSkyBlue,
    primaryContainer = NightSkyBlueContainer,
    onPrimaryContainer = OnNightSkyBlueContainer,
    secondary = NightRainTeal,
    onSecondary = OnNightRainTeal,
    secondaryContainer = NightRainTealContainer,
    onSecondaryContainer = OnNightRainTealContainer,
    tertiary = NightSunAmber,
    onTertiary = OnNightSunAmber,
    tertiaryContainer = NightSunAmberContainer,
    onTertiaryContainer = OnNightSunAmberContainer,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkOnBackground,
    inverseOnSurface = Color(0xFF2E3135),
    inversePrimary = SkyBlue,
    surfaceTint = NightSkyBlue,
    scrim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF060C14),
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)

@Composable
fun KairoWeatherTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 系统栏图标对比度跟随 App 实际主题:enableEdgeToEdge 只按"系统深色开关"决定
    // 图标深浅,用户在设置里手动选浅色/深色时二者会拧着——浅蓝渐变上压浅色图标
    // (或夜蓝上压深色)直接看不见。渐变顶部:浅色=浅蓝底要深色图标,深色反之。
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/** 天气页天空渐变：顶部浅蓝 → 底部页面底色（暗色为深海蓝 → 夜蓝） */
@Composable
fun weatherSkyGradient(): List<Color> = listOf(
    MaterialTheme.colorScheme.primaryContainer,
    MaterialTheme.colorScheme.background,
)
