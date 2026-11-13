package com.example.ui.components

import com.example.data.model.Committee
import com.example.data.model.ScheduleTask
import com.example.ui.screens.ceremonies.ceremonyDate
import com.example.ui.theme.StageColorMap
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** «لجان متأخرة»: the web's src/utils/overdue.ts, the same rules. */
object Overdue {
    data class Item(val committee: Committee, val stage: String, val days: Int, val reason: String)

    private fun utcDay(y: Int, m: Int, d: Int): Long =
        java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(y, m - 1, d) }.timeInMillis / 86_400_000L

    /** The local calendar day of an ISO date (YYYY-MM-DD) or time. */
    private fun localDay(iso: String, zone: TimeZone): Long? {
        if (iso.length <= 10) {
            val m = Regex("""^(\d{4})-(\d{2})-(\d{2})$""").find(iso) ?: return null
            val (y, mo, d) = m.destructured
            return utcDay(y.toInt(), mo.toInt(), d.toInt())
        }
        val date = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd'T'HH:mm:ss").firstNotNullOfOrNull { f ->
            runCatching { SimpleDateFormat(f, Locale.US).apply { isLenient = false; timeZone = zone }.parse(iso) }.getOrNull()
        } ?: return null
        val local = java.util.Calendar.getInstance(zone).apply { time = date }
        return utcDay(local.get(java.util.Calendar.YEAR), local.get(java.util.Calendar.MONTH) + 1, local.get(java.util.Calendar.DAY_OF_MONTH))
    }

    private fun daysSince(from: String, today: String, zone: TimeZone): Int? {
        val a = localDay(from, zone) ?: return null
        val b = localDay(today, zone) ?: return null
        return (b - a).toInt()
    }

    private fun name(key: String) = StageColorMap[key]?.name ?: key

    /**
     * Committees that are late, with why — at most one item per committee (its next stage):
     * the exam date passed and the exam is not recorded; the ceremony date passed and «تم الحفل»
     * is not recorded; any other stage: the previous stage was done more than [overdueDays] days ago.
     * Most late first.
     */
    fun committees(
        committees: List<Committee>,
        overdueDays: Int,
        tasks: List<ScheduleTask> = emptyList(),
        today: String = Dates.today(),
        zone: TimeZone = TimeZone.getDefault()
    ): List<Item> {
        val keys = OrderedStageKeys.map { it.first }
        val out = mutableListOf<Item>()
        for (c in committees) {
            val next = keys.firstOrNull { !c.stages.getStage(it).completed } ?: continue
            if (next == "registration") continue
            val prev = keys[keys.indexOf(next) - 1]
            if (next == "exam") {
                val date = c.examDate?.take(10)?.ifBlank { null } ?: continue
                val days = daysSince(date, today, zone)
                if (days != null && days > 0) out += Item(c, next, days, "فات ميعاد الامتحان ($date) من $days يوم ولم يُسجَّل")
                continue
            }
            if (next == "ceremony") {
                val date = ceremonyDate(c, tasks).take(10)
                if (date.isNotBlank()) {
                    val days = daysSince(date, today, zone)
                    if (days != null && days > 0) out += Item(c, next, days, "فات ميعاد الحفل ($date) من $days يوم ولم يُسجَّل «تم الحفل»")
                    continue
                }
            }
            val doneAt = c.stages.getStage(prev).completedAt?.ifBlank { null } ?: continue
            val since = daysSince(doneAt, today, zone) ?: continue
            if (since <= overdueDays) continue
            out += Item(c, next, since - overdueDays, "${name(next)}: منتظر من $since يوم (بعد «${name(prev)}»)")
        }
        return out.sortedWith(compareByDescending<Item> { it.days }.thenBy { it.committee.committeeNumber })
    }
}
