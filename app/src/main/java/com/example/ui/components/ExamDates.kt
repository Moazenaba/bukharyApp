package com.example.ui.components

import com.example.data.model.Committee
import java.util.TimeZone

/** What the committee's exam date means today (web: src/utils/examDate.ts examDateStatus). */
object ExamDates {
    enum class Tone { LATE, TODAY, SOON, LATER }
    data class Status(val label: String, val tone: Tone)

    private fun dayNumber(iso: String): Long? {
        val m = Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(iso) ?: return null
        val (y, mo, d) = m.destructured
        val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(y.toInt(), mo.toInt() - 1, d.toInt())
        }
        return cal.timeInMillis / 86_400_000L
    }

    /** «فات ميعاد الامتحان», «الامتحان النهارده», «الامتحان بكرة» or «الامتحان بعد N يوم»; null once the exam is done or without a date. */
    fun status(c: Committee, today: String = Dates.today()): Status? {
        val date = c.examDate?.take(10)?.ifBlank { null } ?: return null
        if (c.stages.exam.completed) return null
        val days = (dayNumber(date) ?: return null) - (dayNumber(today) ?: return null)
        return when {
            days < 0 -> Status("فات ميعاد الامتحان ($date) ولم يُسجَّل بعد", Tone.LATE)
            days == 0L -> Status("الامتحان النهارده", Tone.TODAY)
            days == 1L -> Status("الامتحان بكرة", Tone.SOON)
            else -> Status("الامتحان بعد $days يوم ($date)", if (days <= 3) Tone.SOON else Tone.LATER)
        }
    }
}
