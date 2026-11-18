package com.example.domain.v2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The theme rule, checked for every theme: two calm colours and one loud action colour that still stands out
 * when the screen is blurred, with text readable everywhere.
 */
class V2ThemesTest {
    private fun fmt(d: Double) = String.format("%.2f", d)

    @Test fun `text is readable on every theme`() {
        V2Themes.ALL.forEach { t ->
            assertTrue("${t.key}: ink on paper ${fmt(V2Themes.contrast(t.ink, t.paper))}", V2Themes.contrast(t.ink, t.paper) >= 7.0)
            assertTrue("${t.key}: ink on sheet", V2Themes.contrast(t.ink, t.sheet) >= 7.0)
            assertTrue("${t.key}: muted on paper ${fmt(V2Themes.contrast(t.muted, t.paper))}", V2Themes.contrast(t.muted, t.paper) >= 4.5)
            assertTrue("${t.key}: muted on sheet", V2Themes.contrast(t.muted, t.sheet) >= 4.5)
            assertTrue("${t.key}: text on the action colour ${fmt(V2Themes.contrast(t.onCta, t.cta))}", V2Themes.contrast(t.onCta, t.cta) >= 4.5)
            assertTrue("${t.key}: text on the focal block", V2Themes.contrast(t.onHero, t.hero) >= 7.0)
            assertTrue("${t.key}: done labels ${fmt(V2Themes.contrast(t.leaf, t.paper))}", V2Themes.contrast(t.leaf, t.paper) >= 4.5)
            assertTrue("${t.key}: late labels ${fmt(V2Themes.contrast(t.brick, t.paper))}", V2Themes.contrast(t.brick, t.paper) >= 4.5)
        }
    }

    @Test fun `the action colour is the only loud thing (it survives a 90 percent blur)`() {
        V2Themes.ALL.forEach { t ->
            val calmest = V2Themes.calm(t).maxOf { V2Themes.chroma(it) }
            val loud = V2Themes.chroma(t.cta)
            assertTrue("${t.key}: action chroma ${fmt(loud)} vs calm ${fmt(calmest)}", loud - calmest >= 0.4)
            assertTrue("${t.key}: action vs paper ${fmt(V2Themes.contrast(t.cta, t.paper))}", V2Themes.contrast(t.cta, t.paper) >= 3.0)
            // The action button also sits on the focal block: it must stand out there too.
            assertTrue("${t.key}: action vs focal block ${fmt(V2Themes.contrast(t.cta, t.hero))}", V2Themes.contrast(t.cta, t.hero) >= 3.0)
        }
    }

    @Test fun `the hero is a calm tint, never a second loud block`() {
        V2Themes.ALL.forEach { t ->
            assertTrue("${t.key}: hero chroma ${fmt(V2Themes.chroma(t.hero))}", V2Themes.chroma(t.hero) <= 0.12)
            // The hero sits with the paper (a tint of it in light themes, a lifted surface in dark ones).
            assertTrue("${t.key}: hero vs paper ${fmt(V2Themes.contrast(t.hero, t.paper))}", V2Themes.contrast(t.hero, t.paper) < 1.6)
        }
    }

    @Test fun `the two calm colours belong together (same family, far apart in light)`() {
        V2Themes.ALL.forEach { t ->
            assertTrue("${t.key}: paper and sheet stay close", V2Themes.contrast(t.paper, t.sheet) < 1.25)
            assertTrue("${t.key}: the hairline is soft", V2Themes.contrast(t.mist, t.paper) < 1.6)
        }
    }

    @Test fun `auto follows the phone, a saved key wins, an unknown key falls back`() {
        assertEquals(V2Themes.SAND, V2Themes.of(null, phoneIsDark = false))
        assertEquals(V2Themes.NIGHT, V2Themes.of("auto", phoneIsDark = true))
        assertEquals(V2Themes.OLIVE, V2Themes.of("olive", phoneIsDark = true))
        assertEquals(V2Themes.SAND, V2Themes.of("gone", phoneIsDark = false))
        assertEquals(V2Themes.ALL.size, V2Themes.ALL.map { it.key }.toSet().size)
    }
}
