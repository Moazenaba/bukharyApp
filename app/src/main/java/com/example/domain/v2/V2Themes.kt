package com.example.domain.v2

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Colour themes for «الواجهات 2». Each one is 90–95% neutral (paper, white sheets, near-black ink and their soft
 * in-betweens) with ONE refined accent kept for the action that matters (the CTA). Being the only colour on the
 * screen, it is still where the eye lands with the screen 90% blurred — without shouting.
 *
 * Colours are ARGB longs so the rule can be checked in plain unit tests.
 */
data class V2ThemeSpec(
    val key: String,
    val name: String,
    /** What the theme is made of, in words («رملي وبني، والزرار وردي»). */
    val story: String,
    val dark: Boolean,
    val paper: Long,
    val sheet: Long,
    val ink: Long,
    val muted: Long,
    val mist: Long,
    /** The accent: the one filled button of a view, the current tab and the current step; nothing else. */
    val cta: Long,
    val onCta: Long,
    /** The focal block («what to do now»): the paper tinted towards the accent, so its accent button is the one bright spot. */
    val hero: Long,
    val onHero: Long,
    val leaf: Long,
    val brick: Long
)

object V2Themes {
    // Mostly neutral (warm or cool off-white, white sheets, near-black ink) with ONE refined accent each.
    // The hero is the paper tinted towards the accent, so the filled accent button on it is the one spot of colour.
    val SAND = V2ThemeSpec(
        key = "sand", name = "رملي", story = "أبيض دافي وبني غامق، ولمسة وردي", dark = false,
        paper = 0xFFF6F3EE, sheet = 0xFFFFFFFF, ink = 0xFF2A211C, muted = 0xFF6B6058, mist = 0xFFEAE4DB,
        cta = 0xFFC2365F, onCta = 0xFFFFFFFF, hero = 0xFFFBEDF1, onHero = 0xFF2A211C, leaf = 0xFF2F6B45, brick = 0xFFA8402E
    )
    val INK = V2ThemeSpec(
        key = "ink", name = "نيلي", story = "أبيض بارد وكحلي، ولمسة أزرق", dark = false,
        paper = 0xFFF2F4F8, sheet = 0xFFFFFFFF, ink = 0xFF141B2D, muted = 0xFF586074, mist = 0xFFE3E7EF,
        cta = 0xFF2557D6, onCta = 0xFFFFFFFF, hero = 0xFFEAF0FD, onHero = 0xFF141B2D, leaf = 0xFF2A6E4C, brick = 0xFFAE3E2D
    )
    val OLIVE = V2ThemeSpec(
        key = "olive", name = "زيتوني", story = "كريمي وزيتوني غامق، ولمسة طوبي", dark = false,
        paper = 0xFFF3F3EE, sheet = 0xFFFFFFFF, ink = 0xFF1F261C, muted = 0xFF5C6455, mist = 0xFFE4E5DC,
        cta = 0xFFB8461B, onCta = 0xFFFFFFFF, hero = 0xFFFBEEE7, onHero = 0xFF1F261C, leaf = 0xFF2A6E4C, brick = 0xFFA83A2B
    )
    val NIGHT = V2ThemeSpec(
        key = "night", name = "ليلي", story = "رماهذه فحمي، ولمسة كهرماني", dark = true,
        paper = 0xFF0E1015, sheet = 0xFF1A1D24, ink = 0xFFF2F3F6, muted = 0xFFA3A8B5, mist = 0xFF2A2E37,
        cta = 0xFFF5B84A, onCta = 0xFF1E1400, hero = 0xFF22211E, onHero = 0xFFF6F2EA, leaf = 0xFF62C08E, brick = 0xFFF08A76
    )
    val COFFEE = V2ThemeSpec(
        key = "coffee", name = "قهوة", story = "بني غامق وكريمي، ولمسة وردي", dark = true,
        paper = 0xFF14110F, sheet = 0xFF201B18, ink = 0xFFF4EDE7, muted = 0xFFBCB0A6, mist = 0xFF332B26,
        cta = 0xFFF06A98, onCta = 0xFF2A0012, hero = 0xFF2C1F22, onHero = 0xFFF7EFEA, leaf = 0xFF6CC28E, brick = 0xFFF0907A
    )

    val ALL: List<V2ThemeSpec> = listOf(SAND, INK, OLIVE, NIGHT, COFFEE)

    /** «تلقائي»: the light or dark default, following the phone. */
    const val AUTO = "auto"

    fun of(key: String?, phoneIsDark: Boolean): V2ThemeSpec =
        ALL.firstOrNull { it.key == key } ?: if (phoneIsDark) NIGHT else INK

    // ---- the rule, as numbers ----------------------------------------------------------------------

    private fun channel(c: Long, shift: Int) = ((c shr shift) and 0xFF).toDouble() / 255.0

    private fun linear(v: Double) = if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)

    fun luminance(c: Long): Double =
        0.2126 * linear(channel(c, 16)) + 0.7152 * linear(channel(c, 8)) + 0.0722 * linear(channel(c, 0))

    /** WCAG contrast ratio, 1..21. */
    fun contrast(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /**
     * Chroma, 0..1: how far a colour is from grey. A pale beige and a deep brown both read as calm (low chroma);
     * a pink or yellow button does not, which is what still shows when the screen is blurred.
     */
    fun chroma(c: Long): Double {
        val r = channel(c, 16)
        val g = channel(c, 8)
        val b = channel(c, 0)
        return max(r, max(g, b)) - min(r, min(g, b))
    }

    /** The calm colours of a theme: everything the eye should slide over. */
    fun calm(t: V2ThemeSpec): List<Long> = listOf(t.paper, t.sheet, t.ink, t.muted, t.mist, t.hero)
}
