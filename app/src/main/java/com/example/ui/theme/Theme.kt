package com.example.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme(val displayName: String) {
    NEON("Neonowy"),
    SUNSET("Zachód Słońca"),
    OCEAN("Ocean"),
    ROYAL("Królewski")
}

private fun getSportColorScheme(theme: AppTheme): ColorScheme = when (theme) {
    AppTheme.NEON -> darkColorScheme(
        primary = SportGreen,
        secondary = SportCyan,
        tertiary = SportDarkGreen,
        background = SportMidnight,
        surface = SportSurface,
        onPrimary = SportMidnight,
        onSecondary = SportMidnight,
        onBackground = SportTextPrimary,
        onSurface = SportTextPrimary,
        surfaceVariant = SportSurfaceVariant,
        onSurfaceVariant = SportTextSecondary,
        outline = SportBorder
    )
    AppTheme.SUNSET -> darkColorScheme(
        primary = SunsetPrimary,
        secondary = SunsetSecondary,
        tertiary = SunsetPrimary,
        background = SunsetBackground,
        surface = SunsetSurface,
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White,
        surfaceVariant = SunsetSurfaceVariant,
        onSurfaceVariant = Color(0xFFFFCCAA),
        outline = SunsetOutline
    )
    AppTheme.OCEAN -> darkColorScheme(
        primary = OceanPrimary,
        secondary = OceanSecondary,
        tertiary = OceanPrimary,
        background = OceanBackground,
        surface = OceanSurface,
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White,
        surfaceVariant = OceanSurfaceVariant,
        onSurfaceVariant = Color(0xFF90CAF9),
        outline = OceanOutline
    )
    AppTheme.ROYAL -> darkColorScheme(
        primary = RoyalPrimary,
        secondary = RoyalSecondary,
        tertiary = RoyalPrimary,
        background = RoyalBackground,
        surface = RoyalSurface,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White,
        surfaceVariant = RoyalSurfaceVariant,
        onSurfaceVariant = Color(0xFFE1BEE7),
        outline = RoyalOutline
    )
}

@Composable
fun MyApplicationTheme(
  selectedTheme: AppTheme = AppTheme.NEON,
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = getSportColorScheme(selectedTheme)

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.background.toArgb()
      window.navigationBarColor = colorScheme.background.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
      WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
