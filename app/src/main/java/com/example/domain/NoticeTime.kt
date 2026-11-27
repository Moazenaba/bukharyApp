package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * When a notice arrived, the way people say it («منذ 5 دقائق», «أمس 3:15 م»), and the inbox group it falls in.
 * Cairo time, Latin digits like the rest of the app. Pure: [now] is passed in, so it is tested without a clock.
 */
object NoticeTime {
    enum class Group(val label: String) { TODAY("اليوم"), YESTERDAY("أمس"), EARLIER("أقدم") }

    private val CAIRO: TimeZone = TimeZone.getTimeZone("Africa/Cairo")
    private val ARABIC = Locale.forLanguageTag("ar-u-nu-latn")

    /**
     * Server timestamps (ISO, UTC); null when unreadable. Fractions of a second are cut to milliseconds first:
     * SimpleDateFormat reads «.123456» as 123456 milliseconds (wrong by two minutes) or refuses it.
     */
    fun parse(iso: String): Date? {
        val text = iso.trim().replace(Regex("""(\.\d{3})\d+"""), "$1")
        return listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX")
            .firstNotNullOfOrNull { f -> runCatching { SimpleDateFormat(f, Locale.US).apply { isLenient = false }.parse(text) }.getOrNull() }
    }

    private fun dayIndex(time: Long): Long {
        val c = Calendar.getInstance(CAIRO).apply { timeInMillis = time }
        return c.get(Calendar.YEAR) * 400L + c.get(Calendar.DAY_OF_YEAR)
    }

    private fun daysAgo(time: Long, now: Long): Int {
        val a = Calendar.getInstance(CAIRO).apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0) }
        val b = Calendar.getInstance(CAIRO).apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0) }
        return Math.round((b.timeInMillis - a.timeInMillis) / 86_400_000.0).toInt()
    }

    fun group(iso: String, now: Long): Group {
        val t = parse(iso)?.time ?: return Group.EARLIER
        return when (daysAgo(t, now)) {
            0 -> Group.TODAY
            1 -> Group.YESTERDAY
            else -> if (dayIndex(t) > dayIndex(now)) Group.TODAY else Group.EARLIER
        }
    }

    private fun clock(t: Date) = SimpleDateFormat("h:mm a", ARABIC).apply { timeZone = CAIRO }.format(t)

    /** «الآن», «منذ 5 دقائق», «منذ 3 ساعات» (today), «أمس 3:15 م», «الخميس 13/11». */
    fun short(iso: String, now: Long): String {
        val date = parse(iso) ?: return ""
        val minutes = ((now - date.time) / 60_000).toInt()
        return when {
            minutes < 1 -> "الآن"
            daysAgo(date.time, now) == 0 && minutes < 60 -> "منذ ${countWord(minutes, "دقيقة", "دقيقتين", "دقائق")}"
            daysAgo(date.time, now) == 0 -> "منذ ${countWord(minutes / 60, "ساعة", "ساعتين", "ساعات")}"
            daysAgo(date.time, now) == 1 -> "أمس ${clock(date)}"
            else -> SimpleDateFormat("EEEE d/M", ARABIC).apply { timeZone = CAIRO }.format(date)
        }
    }

    /** The full time, for the detail view: «الخميس 13 نوفمبر، 3:15 م». */
    fun full(iso: String): String {
        val date = parse(iso) ?: return ""
        return SimpleDateFormat("EEEE d MMMM، h:mm a", ARABIC).apply { timeZone = CAIRO }.format(date)
    }

    private fun countWord(n: Int, one: String, two: String, few: String) = when {
        n <= 1 -> one
        n == 2 -> two
        n in 3..10 -> "$n $few"
        else -> "$n $one"
    }
}
