package com.example.domain

import com.example.data.model.CeremonyAttendee
import com.example.data.model.CeremonyDetails
import com.example.data.model.Committee
import com.example.data.model.ScheduleTask
import com.example.data.model.StagesMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayFeedTest {
    private val today = "2026-11-13"

    private fun committee(
        id: String, number: Int, examDate: String? = null, stages: StagesMap = StagesMap().updateStage("registration", true),
        ceremony: CeremonyDetails = CeremonyDetails()
    ) = Committee(id = id, committeeNumber = number, committeeName = "لجنة $number", examDate = examDate, stages = stages, ceremonyDetails = ceremony)

    private fun task(id: String, date: String, time: String = "09:30 ص", priority: String = "normal", type: String = "general", done: Boolean = false) =
        ScheduleTask(id = id, title = "مهمة $id", date = date, time = time, priority = priority, type = type, completed = done)

    private val cannot: (String, String) -> Boolean = { _, _ -> false }
    private val can: (String, String) -> Boolean = { _, _ -> true }

    private fun feed(
        committees: List<Committee> = emptyList(), tasks: List<ScheduleTask> = emptyList(), myId: String? = "u1",
        canOn: (String, String) -> Boolean = cannot
    ) = TodayFeedBuilder.build(committees, tasks, myId, canOn, today)

    @Test
    fun `nothing to do gives an empty feed`() {
        val f = feed()
        assertNull(f.now)
        assertEquals(0, f.total)
    }

    @Test
    fun `late comes first, then today, then steps with no day, then the future`() {
        val f = feed(
            committees = listOf(committee("c1", 1)), // head can receive it: a step with no exam day
            tasks = listOf(
                task("future", "2026-11-20"),
                task("today", today),
                task("late", "2026-11-10"),
            ),
            canOn = can
        )
        val order = listOf(f.now!!) + f.later
        assertEquals(listOf("task-late", "task-today", "action-c1", "task-future"), order.map { it.key })
        assertTrue(order[0].overdue)
        assertTrue(order[1].today)
    }

    @Test
    fun `an exam day for someone who cannot act is an exam line, and a head gets one step line instead`() {
        val c = committee("c1", 1, examDate = "2026-11-15")
        val sheikh = feed(listOf(c), canOn = cannot).now!!
        assertEquals(FeedKind.EXAM, sheikh.kind)
        assertEquals("2026-11-15", sheikh.date)

        val head = feed(listOf(c), canOn = can)
        assertEquals(1, head.total)
        assertEquals(FeedKind.ACTION, head.now!!.kind)
        assertEquals("2026-11-15", head.now!!.date)
        assertTrue(head.now!!.title.contains("توثيق استلام"))
    }

    @Test
    fun `a ceremony shows for the invited person, or a head who can finish it, and not for others`() {
        val details = CeremonyDetails(date = "2026-11-18", venue = "المسجد الكبير", attendees = listOf(CeremonyAttendee("u1", "أنا")))
        val c = committee("c1", 1, ceremony = details)
        assertEquals(FeedKind.CEREMONY, feed(listOf(c), myId = "u1").now!!.kind)
        assertNull(feed(listOf(c), myId = "u2").now)
        assertEquals(FeedKind.CEREMONY, feed(listOf(c.copy(ceremonyDetails = details.copy(attendees = emptyList()))), myId = "u2") { a, _ -> a == "committees.finalize" }.now!!.kind)
    }

    @Test
    fun `the calendar task of a ceremony is not listed twice`() {
        val c = committee("c1", 1, ceremony = CeremonyDetails(date = "2026-11-18", attendees = listOf(CeremonyAttendee("u1", "أنا"))))
        val f = feed(listOf(c), tasks = listOf(task("task-ceremony-c1", "2026-11-19", type = "ceremony")))
        assertEquals(1, f.total)
        assertEquals("2026-11-19", f.now!!.date) // the calendar day wins over the stored detail
    }

    @Test
    fun `done tasks and finished ceremonies are left out`() {
        val done = StagesMap().updateStage("registration", true).updateStage("ceremony", true)
        val c = committee("c1", 1, stages = done, ceremony = CeremonyDetails(date = "2026-11-18", attendees = listOf(CeremonyAttendee("u1", "أنا"))))
        assertEquals(0, feed(listOf(c), tasks = listOf(task("t", today, done = true))).total)
    }

    @Test
    fun `the later list is capped and the total is not`() {
        val tasks = (1..20).map { task("t$it", "2026-12-${it.toString().padStart(2, '0')}") }
        val f = feed(tasks = tasks)
        assertEquals(20, f.total)
        assertEquals(8, f.later.size)
        assertEquals("task-t1", f.now!!.key)
    }

    @Test
    fun `urgent comes before normal on the same day and hour`() {
        val f = feed(tasks = listOf(task("a", today, priority = "normal"), task("b", today, priority = "urgent")))
        assertEquals("task-b", f.now!!.key)
        assertFalse(f.later.first().urgent)
    }

    @Test
    fun `times of day are read in Arabic and Latin forms`() {
        assertEquals(570, TodayFeedBuilder.minutesOf("09:30 ص"))
        assertEquals(1140, TodayFeedBuilder.minutesOf("7:00 م"))
        assertEquals(0, TodayFeedBuilder.minutesOf("12:00 ص"))
        assertEquals(750, TodayFeedBuilder.minutesOf("12:30 م"))
        assertEquals(1140, TodayFeedBuilder.minutesOf("19:00"))
        assertEquals(570, TodayFeedBuilder.minutesOf("٠٩:٣٠ ص"))
        assertEquals(1440, TodayFeedBuilder.minutesOf(""))
    }
}
