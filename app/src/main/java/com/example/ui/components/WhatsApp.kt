package com.example.ui.components

import com.example.data.model.Committee
import com.example.data.model.ScheduleTask
import java.net.URLEncoder

/** WhatsApp chat links with a ready message (web: src/utils/whatsapp.ts). Nothing is sent automatically. */
object WhatsApp {
    /** A phone as WhatsApp wants it (country code, digits only); Egyptian mobiles 01x → 201x. Null if unusable. */
    fun number(phone: String?): String? {
        val digits = (phone ?: "").map { ch -> val i = "٠١٢٣٤٥٦٧٨٩".indexOf(ch); if (i >= 0) ('0' + i) else ch }
            .joinToString("").replace(Regex("[^\\d+]"), "")
        if (digits.isEmpty()) return null
        Regex("^01\\d{9}$").matchEntire(digits)?.let { return "2$digits" }
        Regex("^\\+?20(1\\d{9})$").matchEntire(digits)?.let { return "20${it.groupValues[1]}" }
        if (Regex("^00\\d{8,15}$").matches(digits)) return digits.drop(2)
        if (Regex("^\\+\\d{8,15}$").matches(digits)) return digits.drop(1)
        return if (Regex("^\\d{10,15}$").matches(digits)) digits else null
    }

    fun link(phone: String?, text: String): String? =
        number(phone)?.let { "https://wa.me/$it?text=${URLEncoder.encode(text, "UTF-8").replace("+", "%20")}" }

    /** The ceremony invitation: committee, sheikh, day, time and place. */
    fun ceremonyInvitation(c: Committee, task: ScheduleTask?, guestName: String? = null): String {
        val d = c.ceremonyDetails
        val date = (task?.date?.ifBlank { null } ?: d.date).take(10)
        val time = task?.time?.ifBlank { null } ?: d.time
        val day = if (date.isNotBlank()) "${arabicDayName(date)} $date" else "سيُحدد لاحقاً"
        return listOf(
            "السلام عليكم${guestName?.let { " $it" } ?: ""}،",
            "ندعوكم لحضور حفل تكريم طلاب لجنة ${c.committeeNumber} (${c.committeeName})${if (c.sheikhName.isNotBlank()) " — الشيخ ${c.sheikhName}" else ""}.",
            "📅 $day${if (time.isNotBlank()) " — $time" else ""}",
            if (d.venue.isNotBlank()) "📍 ${d.venue}" else "",
            "مسابقات مسجد الإمام البخاري"
        ).filter { it.isNotBlank() }.joinToString("\n")
    }
}
