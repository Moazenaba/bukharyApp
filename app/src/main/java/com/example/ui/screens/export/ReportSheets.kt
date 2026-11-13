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

/** The «في انتظار الامتحان» and ceremonies sheets, the same columns and rules as the web's reportExport.ts. */
object ReportSheets {
    private val ARABIC_DAYS = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    val PERIOD_LABELS = linkedMapOf("day" to "يومي", "week" to "أسبوعي", "month" to "شهري")

    private fun names(list: List<String>) = list.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("، ").ifEmpty { "-" }
    private fun proctorCount(c: Committee) = c.members.count { it.isNotBlank() }

    private fun iso() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
    private fun calendarOf(date: String) = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = iso().parse(date)!! }
    private fun format(cal: Calendar) = iso().format(cal.time)

    val EXAM_HEADER = listOf("م", "رقم اللجنة", "اسم اللجنة", "اسم الشيخ", "المكان", "عدد الطلاب", "المراقبون", "عدد المراقبين", "الكتاب / المسابقة", "الجمعية", "نوع اللجنة")
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

    /** Body of the printable «في انتظار الامتحان» sheet (printHtml saves it as PDF). */
    fun examPrintBody(committees: List<Committee>, date: String): String {
        val rows = examRows(committees)
        val body = StringBuilder("<h2 class='center'>كشف لجان «في انتظار الامتحان» — مسابقات مسجد الإمام البخاري</h2><p class='center'>$date — ${rows.size - 1} لجنة</p><table><tr>")
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
        "م", "التاريخ", "اليوم", "الوقت", "مكان الحفل", "رقم اللجنة", "اسم اللجنة", "اسم الشيخ", "مكان الشيخ", "عدد الطلاب المكرمين",
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

    /** Columns of the ceremonies PDF: what fits on an A4 landscape page (web: ceremoniesPdfTable). */
    private val CEREMONY_PDF_COLUMNS = listOf(0, 1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 13, 14)
    val CEREMONY_PDF_HEADER = CEREMONY_PDF_COLUMNS.map { CEREMONY_HEADER[it] }
    val CEREMONY_PDF_WEIGHTS = listOf(3, 9, 7, 9, 12, 6, 14, 12, 7, 14, 14, 14, 7)

    fun ceremonyPdfRows(rows: List<List<Any?>>): List<List<Any?>> = rows.map { r -> CEREMONY_PDF_COLUMNS.map { r.getOrNull(it) } }

    fun ceremonyTitle(period: String) = "كشف الحفلات (${PERIOD_LABELS[period]}) — مسابقات مسجد الإمام البخاري"

    fun ceremonySubtitle(period: String, anchor: String, count: Int): String {
        val (from, to) = periodRange(period, anchor)
        return "${if (from == to) from else "من $from إلى $to"} — $count حفل"
    }

    /** Latin file name: some apps drop an Arabic name when a file is shared. */
    fun ceremonyPdfFileName(period: String, anchor: String): String {
        val (from, to) = periodRange(period, anchor)
        return "Bukhari-ceremonies-$period-${if (from == to) from else "${from}_to_$to"}.pdf"
    }

    /** Body of the printable ceremonies sheet (printHtml saves it as PDF). */
    fun ceremonyPrintBody(rows: List<List<Any?>>, period: String, anchor: String): String {
        val body = StringBuilder("<h2 class='center'>${escapeHtml(ceremonyTitle(period))}</h2><p class='center'>${escapeHtml(ceremonySubtitle(period, anchor, rows.size))}</p><table><tr>")
        CEREMONY_PDF_HEADER.forEach { body.append("<th>${escapeHtml(it)}</th>") }
        body.append("</tr>")
        ceremonyPdfRows(rows).forEach { r ->
            body.append("<tr>")
            r.forEach { body.append("<td>${escapeHtml(it?.toString() ?: "")}</td>") }
            body.append("</tr>")
        }
        return body.append("</table><style>@page{size:A4 landscape}</style>").toString()
    }

    // ---- «لم يتم الحفل» / «تم الحفل» report (web: ceremonyStatusPdfTable), laid out like the sheets.

    val STATUS_LABEL = mapOf("not_held" to "لم يتم الحفل", "held" to "تم الحفل")
    val NOT_HELD_HEADER = listOf("م", "رقم اللجنة", "اسم الشيخ", "المكان", "الكتاب المقرر", "عدد الطلاب", "المرحلة الحالية", "موعد الحفل", "مكان الحفل")
    val HELD_HEADER = listOf("م", "رقم اللجنة", "اسم الشيخ", "المكان", "الكتاب المقرر", "عدد الطلاب المكرمين", "تاريخ الحفل", "مكان الحفل", "المشايخ الحاضرون")
    val STATUS_WEIGHTS = listOf(3, 6, 14, 14, 14, 8, 12, 14, 14)

    fun statusHeader(status: String) = if (status == "held") HELD_HEADER else NOT_HELD_HEADER

    fun statusCommittees(committees: List<Committee>, status: String) =
        committees.filter { it.stages.ceremony.completed == (status == "held") }.sortedBy { it.committeeNumber }

    /** The sheikh's place stored on the committee, else its branch. */
    private fun place(c: Committee) = c.sheikhCountry.trim().ifBlank { c.branch.trim() }.ifBlank { "-" }

    private fun whenText(c: Committee, tasks: List<ScheduleTask>): String {
        val task = linkedCeremonyTask(c, tasks)
        val date = (task?.date?.takeIf { it.isNotBlank() } ?: c.ceremonyDetails.date).take(10)
        if (date.isEmpty()) return ""
        val time = task?.time?.takeIf { it.isNotBlank() } ?: c.ceremonyDetails.time
        val day = runCatching { ARABIC_DAYS[calendarOf(date).get(Calendar.DAY_OF_WEEK) - 1] }.getOrNull()
        return listOfNotNull(date, day?.let { "($it)" }, time.ifBlank { null }).joinToString(" ")
    }

    /** One row per committee in that state, then a total row; empty when there are none. */
    fun statusRows(committees: List<Committee>, tasks: List<ScheduleTask>, status: String): List<List<Any?>> {
        val held = status == "held"
        val list = statusCommittees(committees, status)
        if (list.isEmpty()) return emptyList()
        fun students(c: Committee) = if (held && c.ceremonyDetails.honoreesCount > 0) c.ceremonyDetails.honoreesCount else c.studentsCount
        val rows = list.mapIndexed { i, c ->
            if (held) listOf<Any?>(
                i + 1, c.committeeNumber, c.sheikhName.ifBlank { "-" }, place(c), c.competitionBook.ifBlank { "-" }, students(c),
                whenText(c, tasks).ifBlank { "-" }, c.ceremonyDetails.venue.ifBlank { "-" }, names(c.ceremonyDetails.attendingSheikhs)
            ) else listOf<Any?>(
                i + 1, c.committeeNumber, c.sheikhName.ifBlank { "-" }, place(c), c.competitionBook.ifBlank { "-" }, students(c),
                com.example.ui.theme.StageColorMap[currentStageKey(c.stages)]?.activeLabel ?: "",
                whenText(c, tasks).ifBlank { "لم يُحدد بعد" }, c.ceremonyDetails.venue.ifBlank { "-" }
            )
        }
        return rows + listOf(listOf<Any?>("", "", "الإجمالي: ${list.size} لجنة", "", "", list.sumOf { students(it) }, "", "", ""))
    }

    fun statusTitle(status: String) = "كشف «${STATUS_LABEL[status]}» — مسابقات مسجد الإمام البخاري"

    fun statusSubtitle(rows: List<List<Any?>>, courseName: String?, today: String): String {
        val count = (rows.size - 1).coerceAtLeast(0)
        val total = rows.lastOrNull()?.getOrNull(5) ?: 0
        return listOfNotNull(courseName?.ifBlank { null }, today, "$count لجنة", "$total طالب").joinToString(" — ")
    }

    fun statusFileName(status: String, today: String) =
        "Bukhari-${if (status == "held") "ceremony-held" else "ceremony-not-held"}-$today.pdf"

    /** Printable body (the print screen saves it as PDF). */
    fun statusPrintBody(rows: List<List<Any?>>, status: String, subtitle: String): String {
        val body = StringBuilder("<h2 class='center'>${escapeHtml(statusTitle(status))}</h2><p class='center'>${escapeHtml(subtitle)}</p><table><tr>")
        statusHeader(status).forEach { body.append("<th>${escapeHtml(it)}</th>") }
        body.append("</tr>")
        rows.forEachIndexed { i, r ->
            body.append(if (i == rows.lastIndex) "<tr style='font-weight:bold;background:#f1f5f9'>" else "<tr>")
            r.forEach { body.append("<td>${escapeHtml(it?.toString() ?: "")}</td>") }
            body.append("</tr>")
        }
        return body.append("</table><style>@page{size:A4 landscape}</style>").toString()
    }
}
