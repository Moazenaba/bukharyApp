package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
    // Field and button borders need 3:1 against white; BorderSoft (1.2:1) stays for decorative dividers.
    outline = TextMuted,
    outlineVariant = BorderActive
)

// Surfaces match DarkPalette.background(): the page is the dark counterpart of #F8F9FD and a card
// that of white, so Material components and hand-colored ones sit on the same tones.
private val DarkColorScheme = darkColorScheme(
    primary = Primary500,
    onPrimary = Primary900,
    primaryContainer = Color(0xFF0F3D38),
    onPrimaryContainer = Primary100,
    secondary = Primary200,
    onSecondary = Primary900,
    secondaryContainer = Color(0xFF16332F),
    onSecondaryContainer = Primary100,
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3B2300),
    background = Color(0xFF141B1A),
    onBackground = Color(0xFFE3ECEA),
    surface = Color(0xFF192221),
    onSurface = Color(0xFFE3ECEA),
    surfaceVariant = Color(0xFF1F2B29),
    onSurfaceVariant = Color(0xFFA9BAB7),
    outline = Color(0xFF8FA3A0),
    outlineVariant = Color(0xFF3A4A47),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE3ECEA),
    inverseOnSurface = Color(0xFF141B1A)
)

val BukhariShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun BukhariTheme(
    darkTheme: Boolean = ThemeState.dark,
    lightBackgroundBehindStatusBar: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val window = activity.window
            val controller = WindowCompat.getInsetsController(window, view)
            // Dark icons only over a light background. The bars follow the app's look, not the phone's.
            controller.isAppearanceLightStatusBars = lightBackgroundBehindStatusBar && !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
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
