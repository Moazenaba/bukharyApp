package com.example.ui.components

import com.example.data.model.CeremonyDetails
import com.example.data.model.Committee
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Same numbers as the web's whatsapp tests. */
class WhatsAppTest {
    @Test
    fun numbers() {
        assertEquals("201012345678", WhatsApp.number("01012345678"))
        assertEquals("201012345678", WhatsApp.number("٠١٠١٢٣٤٥٦٧٨"))
        assertEquals("201012345678", WhatsApp.number("+20 101 234 5678"))
        assertEquals("966501234567", WhatsApp.number("00966501234567"))
        assertEquals("966501234567", WhatsApp.number("+966501234567"))
        assertNull(WhatsApp.number(""))
        assertNull(WhatsApp.number("123"))
    }

    @Test
    fun invitationLink() {
        val c = Committee(id = "c1", committeeNumber = 7, committeeName = "لجنة النور", sheikhName = "أحمد", sheikhPhone = "01012345678",
            ceremonyDetails = CeremonyDetails(date = "2026-11-20", time = "7:00 م", venue = "المسجد الكبير"))
        val text = WhatsApp.ceremonyInvitation(c, null, "الشيخ أحمد")
        assertTrue(text.contains("لجنة 7 (لجنة النور)"))
        assertTrue(text.contains("📍 المسجد الكبير"))
        assertTrue(text.contains("2026-11-20 — 7:00 م"))
        val link = WhatsApp.link(c.sheikhPhone, text)!!
        assertTrue(link.startsWith("https://wa.me/201012345678?text="))
        assertTrue(!link.contains("+"))
    }
}
