package com.example.ui.components

import com.example.data.model.CeremonyDetails
import com.example.data.model.Committee
import com.example.data.model.ScheduleTask
import com.example.data.model.StagesMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/** Same cases as the web's tests/unit/overdue.test.ts. */
class OverdueTest {
    private val keys = OrderedStageKeys.map { it.first }
    private val today = "2026-11-13"
    private val utc = TimeZone.getTimeZone("UTC")

    /** First [n] stages done, the last of them on [at]. */
    private fun c(id: String, n: Int, at: String, block: Committee.() -> Committee = { this }): Committee {
        var stages = StagesMap()
        keys.take(n).forEach { stages = stages.updateStage(it, true, at) }
        return Committee(id = id, committeeNumber = id.filter(Char::isDigit).toIntOrNull() ?: 1, committeeName = id, stages = stages).block()
    }

    private fun late(list: List<Committee>, days: Int = 3, tasks: List<ScheduleTask> = emptyList()) =
        Overdue.committees(list, days, tasks, today, utc)

    @Test
    fun anExamDateThatHasPassedWithoutTheExam() {
        val x = late(listOf(c("c1", 1, "2026-11-01") { copy(examDate = "2026-11-10") })).single()
        assertEquals("exam", x.stage)
        assertEquals(3, x.days)
        assertTrue(x.reason.contains("فات ميعاد الامتحان"))
    }

    @Test
    fun anExamTodayOrLaterOrNoExamDateIsNotLate() {
        assertEquals(emptyList<Overdue.Item>(), late(listOf(
            c("c1", 1, "2026-11-01") { copy(examDate = today) },
            c("c2", 1, "2026-11-01") { copy(examDate = "2026-12-01") },
            c("c3", 1, "2026-10-01")
        )))
    }

    @Test
    fun aStageWaitingMoreThanTheAllowedDays() {
        val x = late(listOf(c("c1", 2, "2026-11-05T10:00:00Z"))).single()
        assertEquals("scoring", x.stage)
        assertEquals(5, x.days)
        assertTrue(x.reason, x.reason.contains("التصحيح: منتظر من 8 يوم"))
    }

    @Test
    fun exactlyTheAllowedDaysIsNotLateYet() {
        assertTrue(late(listOf(c("c1", 2, "2026-11-10"))).isEmpty())
        assertEquals(1, late(listOf(c("c1", 2, "2026-11-09"))).size)
    }

    @Test
    fun theAllowedDaysComeFromTheSetting() {
        assertTrue(late(listOf(c("c1", 2, "2026-11-05")), 10).isEmpty())
        assertEquals(1, late(listOf(c("c1", 2, "2026-11-05")), 1).size)
    }

    @Test
    fun theCeremonyIsLateOnlyAfterItsOwnDate() {
        fun waiting(date: String) = c("c1", 8, "2026-10-01") { copy(ceremonyDetails = CeremonyDetails(date = date)) }
        assertTrue(late(listOf(waiting("2026-11-20"))).isEmpty())
        val x = late(listOf(waiting("2026-11-11"))).single()
        assertEquals("ceremony", x.stage)
        assertEquals(2, x.days)
        // The calendar task's date wins over the stored detail.
        val task = ScheduleTask(id = "task-ceremony-c1", title = "حفل", date = "2026-11-12")
        assertEquals(1, late(listOf(waiting("2026-11-20")), 3, listOf(task)).single().days)
    }

    @Test
    fun aFinishedCommitteeIsNeverLateMostLateFirst() {
        val list = late(listOf(c("c1", 9, "2026-01-01"), c("c2", 2, "2026-11-09"), c("c3", 2, "2026-10-01")))
        assertEquals(listOf("c3", "c2"), list.map { it.committee.id })
    }
}
