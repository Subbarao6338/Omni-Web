package com.nature.browser.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme {
    Default, ClearStream, Canopy, TwilightForest, Coastal, HighAlpine
}

private val ClearStreamColorScheme = lightColorScheme(
    primary = RiverTeal,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = SkyBlue,
    onSecondary = LightOnSecondary,
    background = ClearStreamBg,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface
)

private val CanopyColorScheme = darkColorScheme(
    primary = CanopyPrimary,
    background = CanopyBg,
    onBackground = CloudWhite,
    surface = CanopyBg,
    onSurface = CloudWhite
)

private val TwilightColorScheme = darkColorScheme(
    primary = TwilightPrimary,
    background = TwilightBg,
    onBackground = CloudWhite,
    surface = TwilightBg,
    onSurface = CloudWhite
)

private val CoastalColorScheme = lightColorScheme(
    primary = CoastalPrimary,
    background = CoastalBg,
    onBackground = StormGrey
)

private val AlpineColorScheme = lightColorScheme(
    primary = AlpinePrimary,
    background = AlpineBg,
    onBackground = StormGrey
)

@Composable
fun NatureBrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.Default,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && appTheme == AppTheme.Default && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> when (appTheme) {
            AppTheme.ClearStream -> ClearStreamColorScheme
            AppTheme.Canopy -> CanopyColorScheme
            AppTheme.TwilightForest -> TwilightColorScheme
            AppTheme.Coastal -> CoastalColorScheme
            AppTheme.HighAlpine -> AlpineColorScheme
            else -> if (darkTheme) CanopyColorScheme else ClearStreamColorScheme
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
