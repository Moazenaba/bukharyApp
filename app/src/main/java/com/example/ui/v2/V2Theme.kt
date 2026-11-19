package com.example.ui.v2

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
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
import com.example.domain.v2.V2ThemeSpec
import com.example.domain.v2.V2Themes

/**
 * The «الواجهات 2» look, on its own: soft and rounded. Every theme is mostly neutral (paper, white sheets, ink)
 * with one refined accent (`cta`) for the action that matters; blur the screen and the accent is still where to
 * tap. The themes themselves live in domain/v2/V2Themes (checked by tests).
 */
@Immutable
data class V2Palette(
    val ink: Color,
    val paper: Color,
    val sheet: Color,
    /** The accent: the one filled button of a view, the current tab and the current step. */
    val cta: Color,
    val onCta: Color,
    val leaf: Color,
    val brick: Color,
    val mist: Color,
    val muted: Color,
    val dark: Boolean,
    /** The focal block («what to do now»): calm and deep, so its action-coloured button is the one bright spot. */
    val hero: Color,
    val onHero: Color
) {
    /** The quiet fill of secondary buttons, marks, chips and avatars: ink, barely there. */
    val tonal: Color get() = ink.copy(alpha = if (dark) 0.13f else 0.08f)
}

fun V2ThemeSpec.palette() = V2Palette(
    ink = Color(ink), paper = Color(paper), sheet = Color(sheet), cta = Color(cta), onCta = Color(onCta),
    leaf = Color(leaf), brick = Color(brick), mist = Color(mist), muted = Color(muted), dark = dark,
    hero = Color(hero), onHero = Color(onHero)
)

val V2Light = V2Themes.INK.palette()

val LocalV2 = staticCompositionLocalOf { V2Light }

/** IBM Plex Sans Arabic (SIL Open Font License), bundled so every phone shows the same letters. */
val PlexArabic = FontFamily(
    Font(R.font.plex_arabic_regular, FontWeight.Normal),
    Font(R.font.plex_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.plex_arabic_bold, FontWeight.Bold)
)

/** One scale for every screen: a large title, 16sp reading text, nothing smaller than 13sp. */
object V2Type {
    val title = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp)
    val hero = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 33.sp)
    val heading = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp)
    val body = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp)
    val bodyStrong = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp)
    val small = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp)
    val smallStrong = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 21.sp)
    val label = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp)
    val number = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp)
    val bigNumber = TextStyle(fontFamily = PlexArabic, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp)
}

@Composable
fun V2Theme(theme: V2ThemeSpec = V2Themes.of(null, isSystemInDarkTheme()), content: @Composable () -> Unit) {
    val p = remember(theme) { theme.palette() }
    val scheme = if (p.dark) {
        darkColorScheme(
            primary = p.ink, onPrimary = p.paper, secondary = p.cta, onSecondary = p.onCta,
            background = p.paper, onBackground = p.ink, surface = p.sheet, onSurface = p.ink,
            surfaceVariant = p.paper, onSurfaceVariant = p.muted, outline = p.muted, outlineVariant = p.mist, error = p.brick,
            // Dialogs, menus and sheets read these: without them they come out in Material's default lavender/grey.
            surfaceTint = p.sheet, surfaceContainerLowest = p.sheet, surfaceContainerLow = p.sheet, surfaceContainer = p.sheet,
            surfaceContainerHigh = p.sheet, surfaceContainerHighest = p.sheet, surfaceBright = p.sheet, surfaceDim = p.paper
        )
    } else {
        lightColorScheme(
            primary = p.ink, onPrimary = p.paper, secondary = p.cta, onSecondary = p.onCta,
            background = p.paper, onBackground = p.ink, surface = p.sheet, onSurface = p.ink,
            surfaceVariant = p.paper, onSurfaceVariant = p.muted, outline = p.muted, outlineVariant = p.mist, error = p.brick,
            // Dialogs, menus and sheets read these: without them they come out in Material's default lavender/grey.
            surfaceTint = p.sheet, surfaceContainerLowest = p.sheet, surfaceContainerLow = p.sheet, surfaceContainer = p.sheet,
            surfaceContainerHigh = p.sheet, surfaceContainerHighest = p.sheet, surfaceBright = p.sheet, surfaceDim = p.paper
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
