package com.yuzheng.kairoweather.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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
    themeMode: String = "system",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
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
