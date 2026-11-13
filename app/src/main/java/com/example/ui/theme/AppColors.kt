package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Which look the person chose in the menu. */
enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "حسب النظام"),
    LIGHT("light", "فاتح"),
    DARK("dark", "داكن");

    companion object {
        /** Light until the person picks otherwise: the dark look is opt-in. */
        fun fromKey(key: String?): ThemeMode = values().firstOrNull { it.key == key } ?: LIGHT

        /** Whether this choice means the dark look, given what the phone itself is set to. */
        fun isDark(mode: ThemeMode, systemIsDark: Boolean): Boolean = when (mode) {
            SYSTEM -> systemIsDark
            LIGHT -> false
            DARK -> true
        }
    }
}

/**
 * The look in force. A snapshot state, so anything that reads a color through [tFg], [tBg] or [tEdge]
 * while composing is redrawn when this changes, and the same functions work outside composition.
 * MainActivity sets it before the first frame.
 */
object ThemeState {
    var dark: Boolean by mutableStateOf(false)
}

// The screens were drawn with the light palette. Every hand-picked color is wrapped in the function
// for its role, which returns it unchanged in the light look and a dark counterpart in the dark look:
//   tFg   text and icons      (dark shades become light ones)
//   tBg   fills and cards     (light shades become dark, tinted ones; strong accents stay)
//   tEdge borders and dividers
// Strong accents (buttons and badges that carry white text) are the same in both looks.

fun tFg(c: Color): Color = if (ThemeState.dark) DarkPalette.foreground(c) else c
fun tBg(c: Color): Color = if (ThemeState.dark) DarkPalette.background(c) else c
fun tEdge(c: Color): Color = if (ThemeState.dark) DarkPalette.edge(c) else c

/** The dark counterparts. Pure math on colors, so the contrast of every pair is unit-tested. */
object DarkPalette {
    /** Hue of the brand's teal, used for the neutral (gray / slate) surfaces. */
    private const val BRAND_HUE = 172f

    private class Hsl(val h: Float, val s: Float, val l: Float)

    private fun toHsl(c: Color): Hsl {
        val r = c.red
        val g = c.green
        val b = c.blue
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val l = (mx + mn) / 2f
        val d = mx - mn
        if (d < 1e-6f) return Hsl(0f, 0f, l)
        val s = d / (1f - abs(2f * l - 1f))
        val h = when (mx) {
            r -> ((g - b) / d) % 6f
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } * 60f
        return Hsl(if (h < 0f) h + 360f else h, s, l)
    }

    private fun fromHsl(h: Float, s: Float, l: Float, alpha: Float): Color {
        val ll = l.coerceIn(0f, 1f)
        val ss = s.coerceIn(0f, 1f)
        val c = (1f - abs(2f * ll - 1f)) * ss
        val hp = (((h % 360f) + 360f) % 360f) / 60f
        val x = c * (1f - abs(hp % 2f - 1f))
        val m = ll - c / 2f
        val (r, g, b) = when {
            hp < 1f -> Triple(c, x, 0f)
            hp < 2f -> Triple(x, c, 0f)
            hp < 3f -> Triple(0f, c, x)
            hp < 4f -> Triple(0f, x, c)
            hp < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color((r + m).coerceIn(0f, 1f), (g + m).coerceIn(0f, 1f), (b + m).coerceIn(0f, 1f), alpha)
    }

    /** Grays and slate blues: the neutrals that become the app's dark teal-gray. */
    private fun neutral(h: Float, s: Float) = s < 0.12f || (h in 195f..240f && s < 0.6f)

    /** Text and icons: dark shades are lightened, keeping their hue (light ones are already fine on dark). */
    fun foreground(c: Color): Color {
        val hsl = toHsl(c)
        if (hsl.l >= 0.62f) return c
        val floor = if (hsl.s >= 0.5f) 0.72f else 0.64f
        val l = min(0.93f, max(floor, 0.97f - 0.8f * hsl.l))
        return if (neutral(hsl.h, hsl.s)) {
            fromHsl(if (hsl.s < 0.12f) hsl.h else 200f, min(hsl.s, 0.30f) * 0.9f, l, c.alpha)
        } else {
            fromHsl(hsl.h, hsl.s * 0.9f, l, c.alpha)
        }
    }

    /** Fills: pale backgrounds and tints become dark, faintly tinted ones; strong colors stay as they are. */
    fun background(c: Color): Color {
        val hsl = toHsl(c)
        if (c.red > 0.999f && c.green > 0.999f && c.blue > 0.999f) return fromHsl(BRAND_HUE, 0.16f, 0.115f, c.alpha)
        if (hsl.l < 0.80f) return c
        return if (neutral(hsl.h, hsl.s)) {
            fromHsl(BRAND_HUE, 0.16f, 0.085f + (1f - hsl.l) * 0.45f, c.alpha)
        } else {
            fromHsl(hsl.h, min(hsl.s * 0.5f, 0.35f), 0.075f + (1f - hsl.l) * 0.32f, c.alpha)
        }
    }

    /** Borders and dividers: light lines become dim ones, visible against a dark card but not loud. */
    fun edge(c: Color): Color {
        val hsl = toHsl(c)
        if (hsl.l < 0.75f) return c
        val hue = if (neutral(hsl.h, hsl.s)) BRAND_HUE else hsl.h
        return fromHsl(hue, min(hsl.s * 0.4f, 0.2f), 0.24f + (1f - hsl.l) * 0.5f, c.alpha)
    }
}
