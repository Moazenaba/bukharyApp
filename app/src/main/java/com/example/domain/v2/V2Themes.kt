package com.example.domain.v2

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Colour themes for «الواجهات 2». Each one follows the same rule: two colours that sit calmly together
 * (the paper and the ink, with their soft in-betweens) and ONE loud colour kept only for the action that matters
 * (the CTA). Blur the screen and the only thing that still pops is where to tap.
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
    /** The loud colour: primary buttons and nothing else. */
    val cta: Long,
    val onCta: Long,
    /** The focal block («what to do now»): calm and deep, so its action-coloured button is the brightest spot. */
    val hero: Long,
    val onHero: Long,
    val leaf: Long,
    val brick: Long
)

object V2Themes {
    val SAND = V2ThemeSpec(
        key = "sand", name = "رملي", story = "بيج وبني، والزرار وردي", dark = false,
        paper = 0xFFF3EBDD, sheet = 0xFFFBF7F0, ink = 0xFF4A3226, muted = 0xFF725B4B, mist = 0xFFE4D7C4,
        cta = 0xFFD12764, onCta = 0xFFFFFFFF, hero = 0xFF3B2820, onHero = 0xFFF8EFE3, leaf = 0xFF2F6640, brick = 0xFFA63B2A
    )
    val INK = V2ThemeSpec(
        key = "ink", name = "نيلي", story = "أبيض بارد وكحلي، والزرار برتقالي", dark = false,
        paper = 0xFFF4F5F9, sheet = 0xFFFFFFFF, ink = 0xFF1E2B4F, muted = 0xFF56607E, mist = 0xFFE1E5EE,
        cta = 0xFFFF6A13, onCta = 0xFF1F0D00, hero = 0xFF1E2B4F, onHero = 0xFFF4F5F9, leaf = 0xFF2A6E4C, brick = 0xFFAE3E2D
    )
    val OLIVE = V2ThemeSpec(
        key = "olive", name = "زيتوني", story = "كريمي وزيتوني، والزرار مرجاني", dark = false,
        paper = 0xFFF1F0E4, sheet = 0xFFFAF9F1, ink = 0xFF33402A, muted = 0xFF5D6852, mist = 0xFFDEDDCB,
        cta = 0xFFF0446A, onCta = 0xFF22000A, hero = 0xFF2C3824, onHero = 0xFFF4F3E8, leaf = 0xFF2A6E4C, brick = 0xFFAE3E2D
    )
    val NIGHT = V2ThemeSpec(
        key = "night", name = "ليلي", story = "كحلي غامق ورمادي فاتح، والزرار أصفر", dark = true,
        paper = 0xFF111621, sheet = 0xFF1A2130, ink = 0xFFE6EAF2, muted = 0xFFA3ACC0, mist = 0xFF2A3346,
        cta = 0xFFFFD02E, onCta = 0xFF1C1500, hero = 0xFF26304A, onHero = 0xFFF2F4F8, leaf = 0xFF62C08E, brick = 0xFFF08A76
    )
    val COFFEE = V2ThemeSpec(
        key = "coffee", name = "قهوة", story = "بني غامق وكريمي، والزرار فوشيا", dark = true,
        paper = 0xFF1C1512, sheet = 0xFF261D19, ink = 0xFFF1E6DA, muted = 0xFFC2B2A3, mist = 0xFF3A2E28,
        cta = 0xFFFF4F9A, onCta = 0xFF22000E, hero = 0xFF3A2B23, onHero = 0xFFF7EDE2, leaf = 0xFF6CC28E, brick = 0xFFF0907A
    )

    val ALL: List<V2ThemeSpec> = listOf(SAND, INK, OLIVE, NIGHT, COFFEE)

    /** «تلقائي»: the light or dark default, following the phone. */
    const val AUTO = "auto"

    fun of(key: String?, phoneIsDark: Boolean): V2ThemeSpec =
        ALL.firstOrNull { it.key == key } ?: if (phoneIsDark) NIGHT else SAND

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
