package com.example.ui.screens.export

import com.example.data.model.CeremonyAttendee
import com.example.data.model.CeremonyDetails
import com.example.data.model.Committee
import com.example.data.model.ScheduleTask
import com.example.data.model.StagesMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Same cases as the web's tests/web/report-export.test.ts. */
class ReportSheetsTest {
    private fun committee(id: String, number: Int, stages: StagesMap = StagesMap().updateStage("registration", true), block: Committee.() -> Committee = { this }) =
        Committee(id = id, committeeNumber = number, committeeName = "لجنة", stages = stages).block()

    @Test
    fun examSheetListsOnlyCommitteesInTheExamStageWithATotalsRow() {
        val rows = ReportSheets.examRows(
            listOf(
                committee("a", 7) { copy(sheikhName = "الشيخ أحمد", sheikhCountry = "طنطا", studentsCount = 30, members = listOf("محمد", " ", "علي")) },
                committee("b", 3) { copy(studentsCount = 12, members = listOf("خالد")) },
                committee("c", 1, StagesMap().updateStage("registration", true).updateStage("exam", true))
            )
        )
        assertEquals(3, rows.size)
        assertEquals(listOf<Any?>(1, 3, "لجنة", "-", "-", 12, "خالد", 1), rows[0].take(8))
        assertEquals(listOf<Any?>("الشيخ أحمد", "طنطا", 30, "محمد، علي", 2), rows[1].subList(3, 8))
        assertEquals(42, rows[2][5])
        assertEquals(3, rows[2][7])
        assertTrue(ReportSheets.examRows(listOf(committee("c", 1, StagesMap().updateStage("exam", true)))).isEmpty())
    }

    @Test
    fun weekRunsSaturdayToFridayAndMonthCoversTheCalendarMonth() {
        assertEquals("2026-10-21" to "2026-10-21", ReportSheets.periodRange("day", "2026-10-21"))
        assertEquals("2026-10-17" to "2026-10-23", ReportSheets.periodRange("week", "2026-10-21"))
        assertEquals("2026-10-17" to "2026-10-23", ReportSheets.periodRange("week", "2026-10-17"))
        assertEquals("2026-02-01" to "2026-02-28", ReportSheets.periodRange("month", "2026-02-10"))
    }

    @Test
    fun ceremonySheetHoldsEveryDetailInsideThePeriodOrderedByDate() {
        val committees = listOf(
            committee("a", 4) {
                copy(
                    sheikhName = "الشيخ محمود", sheikhCountry = "المنصورة", studentsCount = 50, members = listOf("مراقب ١"),
                    ceremonyDetails = CeremonyDetails(
                        honoreesCount = 45, attendingSheikhs = listOf("الشيخ سالم"), venue = "مسجد النور",
                        attendees = listOf(CeremonyAttendee("u1", "خالد"))
                    )
                )
            },
            committee("b", 2) { copy(studentsCount = 20, ceremonyDetails = CeremonyDetails(date = "2026-10-18")) },
            committee("c", 9) { copy(ceremonyDetails = CeremonyDetails(date = "2026-11-02")) }
        )
        val tasks = listOf(ScheduleTask(id = "task-ceremony-a", title = "حفل", date = "2026-10-21", time = "8 مساءً", committeeId = "a", type = "ceremony"))
        val rows = ReportSheets.ceremonyRows(committees, tasks, "week", "2026-10-21")
        assertEquals(listOf<Any?>(2, 4), rows.map { it[5] })
        assertEquals(listOf<Any?>("2026-10-18", "الأحد"), rows[0].subList(1, 3))
        assertEquals(20, rows[0][9])
        assertEquals("مجدول", rows[0][14])
        assertEquals(
            listOf<Any?>("2026-10-21", "الأربعاء", "8 مساءً", "مسجد النور", 4, "لجنة", "الشيخ محمود", "المنصورة", 45, "الشيخ سالم", "خالد", "مراقب ١"),
            rows[1].subList(1, 13)
        )
        assertTrue(ReportSheets.ceremonyRows(committees, emptyList(), "day", "2026-10-21").isEmpty())
    }

    @Test
    fun examPrintPageEscapesTextAndEndsWithTheTotalsRow() {
        val html = ReportSheets.examPrintBody(listOf(committee("a", 1) { copy(sheikhName = "<b>الشيخ</b>", studentsCount = 5) }), "2026-10-21")
        assertTrue(html.contains("&lt;b&gt;الشيخ&lt;/b&gt;"))
        assertTrue(html.contains("2026-10-21 — 1 لجنة"))
        assertTrue(html.contains("<tr style='font-weight:bold;background:#f1f5f9'>"))
    }
}
