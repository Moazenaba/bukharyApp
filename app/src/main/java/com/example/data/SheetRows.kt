package com.example.data

import com.example.data.model.Committee
import com.example.data.remote.SheetRowDto

/** A row shown in the course sheet; [virtual] rows are committees that have no saved row yet. */
data class SheetRow(val row: SheetRowDto, val virtual: Boolean = false)

/** Sheet column -> committee stage (web: ROW_STAGE). */
val ROW_STAGE = mapOf("delivered" to "exam", "corrected" to "scoring", "reviewed" to "review", "announced" to "announcement", "ceremony" to "ceremony")

/** Same rules as the web's committeeSync.ts. */
object SheetRules {
    private fun anyDone(c: Committee, keys: List<String>) = keys.any { c.stages.getStage(it).completed }

    fun delivered(c: Committee) = c.receivedByAssistant?.status == "received" ||
        anyDone(c, listOf("exam", "scoring", "aggregation", "review", "sheet", "accreditation", "announcement", "ceremony"))
    fun corrected(c: Committee) = anyDone(c, listOf("scoring", "aggregation", "review", "sheet", "accreditation", "announcement", "ceremony"))
    fun reviewed(c: Committee) = anyDone(c, listOf("review", "sheet", "accreditation", "announcement", "ceremony"))
    fun announced(c: Committee) = anyDone(c, listOf("announcement", "ceremony"))
    fun ceremony(c: Committee) = c.stages.ceremony.completed

    /** «المكان» of a committee: the sheikh's place stored on the committee, else its branch. */
    fun place(c: Committee) = c.sheikhCountry.trim().ifBlank { c.branch.trim() }

    private fun SheetRowDto.withFlags(c: Committee) = copy(
        delivered = delivered(c), corrected = corrected(c), reviewed = reviewed(c), announced = announced(c), ceremony = ceremony(c)
    )

    /**
     * Saved rows linked to a committee show the committee's stages (the committee is the source of
     * truth); unlinked saved rows keep their own flags; committees without a row appear as virtual rows.
     */
    fun merge(saved: List<SheetRowDto>, committees: List<Committee>): List<SheetRow> {
        val byId = committees.associateBy { it.id }
        val linked = mutableSetOf<String>()
        val out = saved.map { r ->
            val c = r.committeeId?.let { byId[it] }
            if (c == null) SheetRow(r) else {
                linked.add(c.id)
                SheetRow(
                    r.copy(
                        number = c.committeeNumber,
                        // The committee holds the data people edit, so its values win; the row's own
                        // copy is only a fallback and is never deleted (web: committeeSync.ts).
                        sheikh_name = c.sheikhName.ifBlank { r.sheikh_name.ifBlank { c.committeeName } },
                        place = place(c).ifBlank { r.place },
                        book = c.competitionBook.ifBlank { r.book },
                        students_count = if (c.studentsCount > 0) c.studentsCount else r.students_count,
                        courseId = c.courseId, courseName = c.courseName
                    ).withFlags(c)
                )
            }
        }.toMutableList()
        committees.filter { it.id !in linked }.forEach { c ->
            out.add(
                SheetRow(
                    SheetRowDto(
                        id = "committee:${c.id}", number = c.committeeNumber, sheikh_name = c.sheikhName.ifBlank { c.committeeName },
                        place = place(c), book = c.competitionBook, students_count = c.studentsCount,
                        committeeId = c.id, courseId = c.courseId, courseName = c.courseName
                    ).withFlags(c),
                    virtual = true
                )
            )
        }
        return out.sortedBy { it.row.number }
    }

    val EXPORT_HEADER = listOf("م", "اسم الشيخ", "المكان", "الكتاب", "عدد الطلاب", "تم الامتحان", "تم التصحيح", "تم المراجعة", "تم الإعلان", "تم الحفل", "مشطوب", "يحتاج تدقيق", "ملاحظات")
    val EXPORT_WIDTHS = listOf(6, 25, 22, 26, 12, 14, 13, 13, 13, 13, 12, 14, 20)

    fun exportRows(rows: List<SheetRowDto>, excel: Boolean): List<List<Any?>> = rows.map { r ->
        val yes = if (excel) "نعم ✓" else "نعم ✓"
        val no = if (excel) "لا ✗" else "لا"
        listOf(
            r.number, r.sheikh_name, r.place, r.book, r.students_count,
            if (r.delivered) yes else no, if (r.corrected) yes else no, if (r.reviewed) yes else no,
            if (r.announced) yes else no, if (r.ceremony) yes else no,
            if (r.struck_out) (if (excel) "نعم (ملغي)" else "نعم") else (if (excel) "-" else "لا"),
            if (r.uncertain) (if (excel) "نعم (غير مؤكد)" else "نعم") else (if (excel) "-" else "لا"),
            r.notes
        )
    }

    /** Rows from an imported sheet, matching the web's flexible column names (parseCourseRowsFromFile). */
    fun parseImported(table: List<List<String>>): List<SheetRowDto> {
        if (table.size < 2) return emptyList()
        val header = table.first().map { it.trim() }
        fun col(names: List<String>) = names.firstNotNullOfOrNull { n -> header.indexOf(n).takeIf { it >= 0 } }
        val cNum = col(listOf("م", "الرقم", "رقم", "number", "id"))
        val cSheikh = col(listOf("اسم الشيخ", "الشيخ", "المحاضر", "sheikh_name", "sheikh"))
        val cPlace = col(listOf("المكان", "المسجد", "القاعة", "place", "location"))
        val cBook = col(listOf("الكتاب", "المتن", "الكتاب المقرر", "المقرر", "book"))
        val cStudents = col(listOf("عدد الطلاب", "الطلاب", "عدد الطلبة", "الطلبة", "students_count", "students", "studentsCount"))
        val cDelivered = col(listOf("تم الامتحان", "الامتحان", "التسليم للمقر", "التسليم", "تم التسليم", "delivered"))
        val cCorrected = col(listOf("تم التصحيح", "التصحيح", "مصحح", "corrected"))
        val cReviewed = col(listOf("تم المراجعة", "المراجعة", "مراجعة", "reviewed"))
        val cAnnounced = col(listOf("تم الإعلان", "الإعلان", "إعلان", "announced"))
        val cCeremony = col(listOf("تم الحفل", "الحفل", "حفل", "ceremony"))
        val cStruck = col(listOf("مشطوب", "ملغي", "struck_out", "struck"))
        val cUncertain = col(listOf("يحتاج تدقيق", "غير مؤكد", "uncertain"))
        val cNotes = col(listOf("ملاحظات", "ملاحظة", "notes"))
        fun isTrue(v: String): Boolean {
            val l = v.lowercase()
            return l == "true" || l == "1" || l.contains("نعم") || l.contains("✓") || l.contains("صح") || l == "yes"
        }
        return table.drop(1).mapIndexed { i, r ->
            fun v(c: Int?) = c?.let { r.getOrNull(it)?.trim() } ?: ""
            val num = v(cNum).toIntOrNull()?.takeIf { it > 0 } ?: (i + 1)
            SheetRowDto(
                id = "",
                number = num,
                sheikh_name = v(cSheikh).ifBlank { "الشيخ ${i + 1}" },
                place = v(cPlace), book = v(cBook),
                students_count = v(cStudents).toIntOrNull()?.takeIf { it >= 0 } ?: 0,
                delivered = isTrue(v(cDelivered)), corrected = isTrue(v(cCorrected)), reviewed = isTrue(v(cReviewed)),
                announced = isTrue(v(cAnnounced)), ceremony = isTrue(v(cCeremony)),
                struck_out = isTrue(v(cStruck)), uncertain = isTrue(v(cUncertain)), notes = v(cNotes)
            )
        }
    }
}
