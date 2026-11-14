package com.example.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateStatusTest {
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun `only a higher code with an apk counts as an upgrade`() {
        assertTrue(UpdateDecision.isNewer(12, 13, "app/a.apk"))
        assertFalse(UpdateDecision.isNewer(13, 13, "app/a.apk"))
        assertFalse(UpdateDecision.isNewer(14, 13, "app/a.apk"))
        assertFalse(UpdateDecision.isNewer(12, 13, ""))
    }

    @Test
    fun `the permission question is asked again after a day, not once forever`() {
        assertTrue(UpdateDecision.shouldAskPermission(0L, 1_000))
        assertFalse(UpdateDecision.shouldAskPermission(1_000, 1_000 + day - 1))
        assertTrue(UpdateDecision.shouldAskPermission(1_000, 1_000 + day))
    }

    @Test
    fun `every status has a message and names the versions it needs`() {
        for (s in UpdateStatus.values()) {
            val text = UpdateText.title(UpdateReport(s, installed = "1.1.10", latest = "1.1.13"))
            assertTrue("$s has no message", text.isNotBlank())
        }
        assertTrue(UpdateText.title(UpdateReport(UpdateStatus.READY, "1.1.10", "1.1.13")).contains("1.1.13"))
        assertTrue(UpdateText.title(UpdateReport(UpdateStatus.UP_TO_DATE, "1.1.10")).contains("1.1.10"))
    }

    @Test
    fun `the wrong signature message tells the person to install by hand`() {
        val text = UpdateText.title(UpdateReport(UpdateStatus.WRONG_SIGNATURE, "1.1.10", "1.1.13"))
        assertTrue(text.contains("ثبّتها"))
        assertTrue(UpdateText.actionable(UpdateStatus.WRONG_SIGNATURE))
        assertTrue(UpdateText.actionable(UpdateStatus.READY))
        assertFalse(UpdateText.actionable(UpdateStatus.UP_TO_DATE))
        assertEquals(10, UpdateStatus.values().size)
    }
}
