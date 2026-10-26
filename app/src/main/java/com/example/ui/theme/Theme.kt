package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.ThemeMode
import com.example.model.WeatherSceneType

private val AtmosDarkScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF041322),
    primaryContainer = Color(0xFF0D3356),
    onPrimaryContainer = Color(0xFFD8F3FF),
    secondary = SolarAmber,
    onSecondary = Color(0xFF281900),
    tertiary = GlacialIce,
    background = ObsidianNight,
    onBackground = TextPrimaryDark,
    surface = Color(0xFF0D182B),
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF17263F),
    onSurfaceVariant = TextSecondaryDark
)

private val AtmosLightScheme = lightColorScheme(
    primary = Color(0xFF0369A1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF082F49),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    tertiary = Color(0xFF0284C7),
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun Atmos3DTheme(
    themeMode: ThemeMode = ThemeMode.DYNAMIC,
    sceneType: WeatherSceneType = WeatherSceneType.SUNNY,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> AtmosLightScheme
        ThemeMode.DARK -> AtmosDarkScheme
        ThemeMode.DYNAMIC -> {
            val accent = Color(sceneType.accentColorHex)
            val secondaryAccent = Color(sceneType.secondaryColorHex)
            AtmosDarkScheme.copy(
                primary = accent,
                secondary = secondaryAccent,
                background = Color(sceneType.skyTopHex),
                surface = Color(sceneType.skyBottomHex)
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
