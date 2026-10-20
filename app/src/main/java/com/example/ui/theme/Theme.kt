package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary700,
    onPrimary = SurfacePure,
    primaryContainer = Primary100,
    onPrimaryContainer = Primary900,
    secondary = Primary600,
    onSecondary = SurfacePure,
    secondaryContainer = Primary50,
    onSecondaryContainer = Primary800,
    tertiary = GoldPrimary,
    onTertiary = SurfacePure,
    tertiaryContainer = GoldLight,
    onTertiaryContainer = GoldDark,
    background = AppBackground,
    onBackground = TextMain,
    surface = SurfacePure,
    onSurface = TextMain,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = TextMuted,
    outline = BorderSoft,
    outlineVariant = BorderActive
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary500,
    onPrimary = Primary900,
    primaryContainer = Primary800,
    onPrimaryContainer = Primary100,
    secondary = Primary200,
    onSecondary = Primary900,
    background = Primary900,
    onBackground = SurfacePure,
    surface = Primary800,
    onSurface = SurfacePure,
    surfaceVariant = Color(0xFF002B27),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF0F4D46)
)

val BukhariShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BukhariTheme(
    darkTheme: Boolean = false,
    lightBackgroundBehindStatusBar: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightBackgroundBehindStatusBar
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = BukhariShapes,
            content = content
        )
    }
}
