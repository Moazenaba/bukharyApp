package com.example.ui.screens.export

import com.example.data.model.Committee
import com.example.data.model.ScheduleTask
import com.example.ui.components.currentStageKey
import com.example.ui.components.escapeHtml
import com.example.ui.screens.ceremonies.linkedCeremonyTask
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** The «جاري الامتحان الآن» and ceremonies sheets, the same columns and rules as the web's reportExport.ts. */
object ReportSheets {
    private val ARABIC_DAYS = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    val PERIOD_LABELS = linkedMapOf("day" to "يومي", "week" to "أسبوعي", "month" to "شهري")

    private fun names(list: List<String>) = list.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("، ").ifEmpty { "-" }
    private fun proctorCount(c: Committee) = c.members.count { it.isNotBlank() }

    private fun iso() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
    private fun calendarOf(date: String) = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = iso().parse(date)!! }
    private fun format(cal: Calendar) = iso().format(cal.time)

    val EXAM_HEADER = listOf("م", "رقم اللجنة", "اسم اللجنة", "اسم الشيخ", "البلد", "عدد الطلاب", "المراقبون", "عدد المراقبين", "الكتاب / المسابقة", "الجمعية", "نوع اللجنة")
    val EXAM_WIDTHS = listOf(5, 10, 24, 24, 16, 11, 40, 12, 24, 20, 10)

    fun examInProgress(committees: List<Committee>) =
        committees.filter { currentStageKey(it.stages) == "exam" }.sortedBy { it.committeeNumber }

    /** One row per committee in the exam stage, then a totals row; empty when there are none. */
    fun examRows(committees: List<Committee>): List<List<Any?>> {
        val list = examInProgress(committees)
        if (list.isEmpty()) return emptyList()
        return list.mapIndexed { i, c ->
            listOf<Any?>(
                i + 1, c.committeeNumber, c.committeeName, c.sheikhName.ifBlank { "-" }, c.sheikhCountry.ifBlank { "-" },
                c.studentsCount, names(c.members), proctorCount(c), c.competitionBook.ifBlank { "-" },
                c.associationName.ifBlank { "-" }, if (c.isWomenCommittee) "نسائية" else "رجال"
            )
        } + listOf(listOf<Any?>("", "", "الإجمالي (${list.size} لجنة)", "", "", list.sumOf { it.studentsCount }, "", list.sumOf { proctorCount(it) }, "", "", ""))
    }

    /** Body of the printable «جاري الامتحان الآن» sheet (printHtml saves it as PDF). */
    fun examPrintBody(committees: List<Committee>, date: String): String {
        val rows = examRows(committees)
        val body = StringBuilder("<h2 class='center'>كشف لجان «جاري الامتحان الآن» — مسابقات مسجد الإمام البخاري</h2><p class='center'>$date — ${rows.size - 1} لجنة</p><table><tr>")
        EXAM_HEADER.take(8).forEach { body.append("<th>$it</th>") }
        body.append("</tr>")
        rows.forEachIndexed { i, r ->
            body.append(if (i == rows.lastIndex) "<tr style='font-weight:bold;background:#f1f5f9'>" else "<tr>")
            r.take(8).forEach { body.append("<td>${escapeHtml(it?.toString() ?: "")}</td>") }
            body.append("</tr>")
        }
        return body.append("</table>").toString()
    }

    /** First and last day (inclusive) of the day, week (Saturday to Friday) or month around [anchor]. */
    fun periodRange(period: String, anchor: String): Pair<String, String> {
        val cal = calendarOf(anchor)
        return when (period) {
            "week" -> {
                cal.add(Calendar.DAY_OF_MONTH, -(cal.get(Calendar.DAY_OF_WEEK) % 7)) // Saturday = 7
                val from = format(cal)
                cal.add(Calendar.DAY_OF_MONTH, 6)
                from to format(cal)
            }
            "month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val from = format(cal)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                from to format(cal)
            }
            else -> anchor to anchor
        }
    }

    val CEREMONY_HEADER = listOf(
        "م", "التاريخ", "اليوم", "الوقت", "مكان الحفل", "رقم اللجنة", "اسم اللجنة", "اسم الشيخ", "البلد", "عدد الطلاب المكرمين",
        "المشايخ الحاضرون", "المدعوون للحفل", "مراقبو اللجنة", "الكتاب / المسابقة", "الحالة", "ملاحظات"
    )
    val CEREMONY_WIDTHS = listOf(5, 12, 10, 18, 22, 10, 22, 22, 14, 12, 28, 28, 32, 22, 10, 28)

    /** Ceremonies dated inside the period, ordered by date and time, with every detail the committee holds. */
    fun ceremonyRows(committees: List<Committee>, tasks: List<ScheduleTask>, period: String, anchor: String): List<List<Any?>> {
        val (from, to) = periodRange(period, anchor)
        return committees.mapNotNull { c ->
            val task = linkedCeremonyTask(c, tasks)
            val date = (task?.date?.takeIf { it.isNotBlank() } ?: c.ceremonyDetails.date).take(10)
            if (date.isEmpty() || date < from || date > to) null
            else Triple(c, task, date)
        }.sortedWith(compareBy({ it.third }, { it.second?.time ?: it.first.ceremonyDetails.time }, { it.first.committeeNumber }))
            .mapIndexed { i, (c, task, date) ->
                val d = c.ceremonyDetails
                val time = task?.time?.takeIf { it.isNotBlank() } ?: d.time
                listOf<Any?>(
                    i + 1, date, ARABIC_DAYS[calendarOf(date).get(Calendar.DAY_OF_WEEK) - 1], time.ifBlank { "-" },
                    d.venue.ifBlank { "-" }, c.committeeNumber, c.committeeName, c.sheikhName.ifBlank { "-" },
                    c.sheikhCountry.ifBlank { "-" }, if (d.honoreesCount > 0) d.honoreesCount else c.studentsCount,
                    names(d.attendingSheikhs), names(d.attendees.map { it.name }), names(c.members),
                    c.competitionBook.ifBlank { "-" }, if (c.stages.ceremony.completed) "تم الحفل" else "مجدول",
                    d.notes.ifBlank { "-" } // the calendar task's notes hold the place (server syncCeremonyTask)
                )
            }
    }

    fun ceremonyFileName(period: String, anchor: String): String {
        val (from, to) = periodRange(period, anchor)
        return "كشف_الحفلات_${PERIOD_LABELS[period]}_${if (from == to) from else "${from}_إلى_$to"}.xlsx"
    }
}
