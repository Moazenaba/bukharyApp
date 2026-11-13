package com.example.domain

import java.util.Calendar
import java.util.TimeZone

/** Days written the way people say them: «اليوم», «بكرة», «يوم الأحد», «متأخر 3 أيام». */
object DayText {
    private val NAMES = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

    private fun parse(iso: String): Calendar? {
        val m = Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(iso) ?: return null
        val (y, mo, d) = m.destructured
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(y.toInt(), mo.toInt() - 1, d.toInt())
        }
    }

    /** [to] minus [from] in whole days; null when either is not a YYYY-MM-DD date. */
    fun daysBetween(from: String, to: String): Int? {
        val a = parse(from) ?: return null
        val b = parse(to) ?: return null
        return Math.round((b.timeInMillis - a.timeInMillis) / 86_400_000.0).toInt()
    }

    fun weekday(iso: String): String = parse(iso)?.let { NAMES[it.get(Calendar.DAY_OF_WEEK) - 1] } ?: ""

    /** 15/11 */
    fun shortDate(iso: String): String {
        val m = Regex("""^\d{4}-(\d{2})-(\d{2})""").find(iso) ?: return ""
        return "${m.groupValues[2]}/${m.groupValues[1]}"
    }

    /** «يومين», «3 أيام», «12 يوم». */
    fun daysWord(n: Int): String = when {
        n == 1 -> "يوم"
        n == 2 -> "يومين"
        n in 3..10 -> "$n أيام"
        else -> "$n يوم"
    }

    /** How far [iso] is from [today], in words. Empty when [iso] is not a date. */
    fun relative(iso: String, today: String): String {
        val day = iso.take(10)
        val d = daysBetween(today, day) ?: return ""
        return when {
            d == 0 -> "اليوم"
            d == 1 -> "بكرة"
            d == -1 -> "أمس"
            d in 2..6 -> "يوم ${weekday(day)}"
            d < -1 -> "متأخر ${daysWord(-d)}"
            else -> "${weekday(day)} ${shortDate(day)}"
        }
    }

    /** الخميس 13/11 */
    fun full(iso: String): String {
        val day = iso.take(10)
        val w = weekday(day)
        return if (w.isEmpty()) "" else "$w ${shortDate(day)}"
    }
}
