package com.example.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTest {
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun `a higher version than the last one that ran is announced`() {
        assertTrue(WhatsNew.shouldAnnounce(12, 13, 0, 0))
    }

    @Test
    fun `the same version opened again is not announced`() {
        assertFalse(WhatsNew.shouldAnnounce(13, 13, 0, 5 * day))
    }

    @Test
    fun `a downgrade is not announced`() {
        assertFalse(WhatsNew.shouldAnnounce(14, 13, 0, 5 * day))
    }

    @Test
    fun `a fresh install is not announced`() {
        assertFalse(WhatsNew.shouldAnnounce(null, 13, 1_000, 1_000))
        assertFalse(WhatsNew.shouldAnnounce(null, 13, 1_000, 30_000))
    }

    @Test
    fun `the first run of this feature after an update is announced`() {
        // Nothing recorded yet, but the phone says it was installed days before it was last updated.
        assertTrue(WhatsNew.shouldAnnounce(null, 13, 0, 3 * day))
    }
}
