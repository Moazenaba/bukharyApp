package com.example.ui.v2

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * The «الواجهات 2» look, on its own: ink-blue text on cool paper, saffron only for the one thing to do now,
 * leaf for done and brick for late. Nothing here is shared with the classic screens.
 */
@Immutable
data class V2Palette(
    val ink: Color,
    val paper: Color,
    val sheet: Color,
    val saffron: Color,
    val onSaffron: Color,
    val leaf: Color,
    val brick: Color,
    val mist: Color,
    val muted: Color,
    val dark: Boolean
)

val V2Light = V2Palette(
    ink = Color(0xFF1E2B4F), paper = Color(0xFFF5F6FA), sheet = Color(0xFFFFFFFF), saffron = Color(0xFFD9971A),
    onSaffron = Color(0xFF2E2104), leaf = Color(0xFF2F7A55), brick = Color(0xFFB4412F), mist = Color(0xFFE2E6EF),
    muted = Color(0xFF5B6583), dark = false
)

val V2Dark = V2Palette(
    ink = Color(0xFFDCE3F5), paper = Color(0xFF121828), sheet = Color(0xFF1A2236), saffron = Color(0xFFE6A93A),
    onSaffron = Color(0xFF241A02), leaf = Color(0xFF5DB389), brick = Color(0xFFE07A66), mist = Color(0xFF2A3450),
    muted = Color(0xFF9AA5C3), dark = true
)

val LocalV2 = staticCompositionLocalOf { V2Light }

/** IBM Plex Sans Arabic (SIL Open Font License), bundled so every phone shows the same letters. */
val PlexArabic = FontFamily(
    Font(R.font.plex_arabic_regular, FontWeight.Normal),
    Font(R.font.plex_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.plex_arabic_bold, FontWeight.Bold)
)

/** One scale for every screen; nothing is smaller than 13sp. */
object V2Type {
    val title = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp)
    val hero = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 29.sp)
    val heading = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp)
    val body = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp)
    val bodyStrong = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp)
    val small = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp)
    val smallStrong = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 19.sp)
    val number = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp)
    val bigNumber = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp)
}

@Composable
fun V2Theme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val p = if (dark) V2Dark else V2Light
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.ink, onPrimary = p.paper, secondary = p.saffron, onSecondary = p.onSaffron,
            background = p.paper, onBackground = p.ink, surface = p.sheet, onSurface = p.ink,
            surfaceVariant = p.paper, onSurfaceVariant = p.muted, outline = p.mist, error = p.brick
        )
    } else {
        lightColorScheme(
            primary = p.ink, onPrimary = p.paper, secondary = p.saffron, onSecondary = p.onSaffron,
            background = p.paper, onBackground = p.ink, surface = p.sheet, onSurface = p.ink,
            surfaceVariant = p.paper, onSurfaceVariant = p.muted, outline = p.mist, error = p.brick
        )
    }
    val base = Typography()
    val typography = Typography(
        bodyLarge = V2Type.body, bodyMedium = V2Type.body, bodySmall = V2Type.small,
        titleLarge = V2Type.title, titleMedium = V2Type.heading, titleSmall = V2Type.bodyStrong,
        labelLarge = V2Type.bodyStrong, labelMedium = V2Type.smallStrong, labelSmall = V2Type.small,
        headlineSmall = V2Type.title, displaySmall = base.displaySmall.copy(fontFamily = PlexArabic)
    )
    CompositionLocalProvider(LocalV2 provides p, LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
