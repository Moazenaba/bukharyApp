package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * The screens are drawn with the light palette and every hand-picked color goes through tFg / tBg /
 * edge. These tests are the only way to see the dark look from here: they check the contrast of every
 * text color against every fill that exists in the app, in the dark look.
 */
class DarkPaletteTest {
    private fun c(hex: Long) = Color(0xFF000000 or hex)

    // Text and icon colors used in the screens (light palette).
    private val textColors = listOf(
        0x0F172A, 0x1E293B, 0x075985, 0x334155, 0x0E7490, 0x1E3A8A, 0x475569, 0x1E40AF, 0x1D4ED8, 0x2563EB,
        0x64748B, 0x94A3B8, 0x166534, 0x15803D, 0x16A34A, 0x78350F, 0x92400E, 0xB45309, 0xC2410C, 0xD97706,
        0x6B21A8, 0x7E22CE, 0x4338CA, 0x881337, 0x9F1239, 0xBE123C, 0xB91C1C, 0xE11D48, 0xDC2626, 0xEF4444,
        0x134E4A, 0x065F46, 0x115E59, 0x047857, 0x0F766E, 0x059669, 0x10B981, 0x00201D, 0x003833, 0x005C55, 0x0D1C2E
    ).map { c(it.toLong()) }

    // Pale fills and tints (light palette). Text sits on each of these.
    private val fills = listOf(
        0xFFFFFF, 0xF8F9FD, 0xF8FAFC, 0xF1F5F9, 0xE2E8F0, 0xFFFBEB, 0xFEF3C7, 0xEFF6FF, 0xF0FDF4, 0xF0FDFA,
        0xECFDF5, 0xD1FAE5, 0xDCFCE7, 0xCCFBF1, 0xFEE2E2, 0xFEF2F2, 0xFFF1F2, 0xEEF2FF, 0xF0F9FF, 0xFAFAFA,
        0xEEEEEE, 0xE9D5FF, 0xA7F3D0, 0xBBF7D0
    ).map { c(it.toLong()) }

    private fun channel(v: Float): Double = if (v <= 0.03928f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    private fun luminance(x: Color) = 0.2126 * channel(x.red) + 0.7152 * channel(x.green) + 0.0722 * channel(x.blue)
    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @After
    fun backToLight() {
        ThemeState.dark = false
    }

    @Test
    fun `the light look is the colors as written`() {
        ThemeState.dark = false
        (textColors + fills).forEach {
            assertEquals(it, tFg(it))
            assertEquals(it, tBg(it))
            assertEquals(it, tEdge(it))
        }
    }

    @Test
    fun `in the dark look every text color is readable on every fill`() {
        ThemeState.dark = true
        val problems = mutableListOf<String>()
        for (t in textColors) for (f in fills) {
            val r = ratio(tFg(t), tBg(f))
            if (r < 4.5) problems += "text #%06X on fill #%06X is %.2f:1".format(t.toArgb24(), f.toArgb24(), r)
        }
        assertTrue(problems.take(10).joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `muted and body text keep their rank below the main text`() {
        ThemeState.dark = true
        val main = luminance(tFg(c(0x0F172A)))
        val body = luminance(tFg(c(0x475569)))
        val muted = luminance(tFg(c(0x64748B)))
        assertTrue(main > body && body >= muted - 0.001)
    }

    @Test
    fun `cards are lighter than the page, and both are dark`() {
        ThemeState.dark = true
        val page = tBg(c(0xF8F9FD))
        val card = tBg(Color.White)
        assertTrue(luminance(card) > luminance(page))
        assertTrue(luminance(card) < 0.03)
    }

    @Test
    fun `borders are visible on a card but quieter than text`() {
        ThemeState.dark = true
        val card = tBg(Color.White)
        for (e in listOf(0xE2E8F0, 0xCBD5E1, 0xA7F3D0, 0xBFDBFE, 0xFECACA)) {
            val r = ratio(tEdge(c(e.toLong())), card)
            assertTrue("border #%06X is %.2f:1".format(e, r), r in 1.3..4.5)
        }
    }

    @Test
    fun `strong colors that carry white text stay the same`() {
        ThemeState.dark = true
        for (strong in listOf(Primary700, Primary800, c(0x0F766E), c(0xDC2626), c(0x059669))) {
            assertEquals(strong, tBg(strong))
        }
        assertEquals(Color.White, tFg(Color.White))
    }

    @Test
    fun `transparency is kept`() {
        ThemeState.dark = true
        assertEquals(0.4f, tBg(Color.White.copy(alpha = 0.4f)).alpha, 0.01f)
        assertEquals(0.5f, tFg(c(0x0F172A).copy(alpha = 0.5f)).alpha, 0.01f)
    }

    @Test
    fun `theme modes`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.DARK, ThemeMode.fromKey("dark"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromKey("garbage"))
        assertTrue(ThemeMode.isDark(ThemeMode.SYSTEM, true))
        assertTrue(!ThemeMode.isDark(ThemeMode.SYSTEM, false))
        assertTrue(!ThemeMode.isDark(ThemeMode.LIGHT, true))
        assertTrue(ThemeMode.isDark(ThemeMode.DARK, false))
    }

    private fun Color.toArgb24(): Int = ((red * 255).toInt() shl 16) or ((green * 255).toInt() shl 8) or (blue * 255).toInt()
}
