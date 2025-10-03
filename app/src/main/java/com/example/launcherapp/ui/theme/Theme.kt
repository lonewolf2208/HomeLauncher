package com.example.launcherapp.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = SpaceBlack,
    secondary = NeonMagenta,
    onSecondary = SpaceBlack,
    tertiary = ElectricViolet,
    onTertiary = SoftWhite,
    background = SpaceBlack,
    onBackground = SoftWhite,
    surface = DeepIndigo,
    onSurface = SoftWhite,
    surfaceVariant = CosmicBlue,
    outline = NeonCyan.copy(alpha = 0.4f)
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricViolet,
    onPrimary = SoftWhite,
    secondary = SolarOrange,
    onSecondary = SpaceBlack,
    tertiary = NeonMagenta,
    onTertiary = SoftWhite,
    background = SoftWhite,
    onBackground = SpaceBlack,
    surface = SoftWhite,
    onSurface = SpaceBlack,
    outline = ElectricViolet.copy(alpha = 0.35f)
)

@Composable
fun LauncherAppTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
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
