package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** WCAG contrast of the colors the screens rely on: 4.5:1 for text, 3:1 for borders and state icons. */
class ContrastTest {
    private fun channel(c: Float): Double = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    private fun luminance(c: Color) = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = luminance(a).let { la -> luminance(b).let { lb -> if (la >= lb) la to lb else lb to la } }
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertText(name: String, fg: Color, bg: Color = SurfacePure) =
        assertTrue("$name is ${"%.2f".format(ratio(fg, bg))}:1, text needs 4.5:1", ratio(fg, bg) >= 4.5)

    @Test
    fun `muted text reads on the white card and on the page background`() {
        assertText("TextMuted on white", TextMuted)
        assertText("TextMuted on page", TextMuted, AppBackground)
    }

    @Test
    fun `field borders are visible against white`() {
        // The theme draws every text field's border with TextMuted (Theme.kt, outline).
        assertTrue(ratio(TextMuted, SurfacePure) >= 3.0)
        // The soft border is only for decoration; it must not be used for controls.
        assertTrue(ratio(BorderSoft, SurfacePure) < 3.0)
    }

    @Test
    fun `status captions on white pass`() {
        assertText("green caption", Color(0xFF15803D))
        assertText("amber caption", Color(0xFFB45309))
        assertText("red text", Color(0xFFB91C1C))
        assertText("priority urgent", PriorityUrgentText)
        assertText("priority normal", PriorityNormalText)
    }
}
