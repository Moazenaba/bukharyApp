package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AssignmentTextTest {
    @Test fun `tasks and statuses have words, unknown ones are shown as they are`() {
        assertEquals("مراقبة امتحان", AssignmentText.task("exam"))
        assertEquals("متابعة حفل", AssignmentText.task("ceremony"))
        assertEquals("نشط", AssignmentText.status("active"))
        assertEquals("منتهي", AssignmentText.status("ended"))
        assertEquals("ملغي", AssignmentText.status("cancelled"))
        assertEquals("other", AssignmentText.task("other"))
    }

    @Test fun `dates read as a range, or say there are none`() {
        assertEquals("من 2026-11-01 حتى 2026-11-20", AssignmentText.dates("2026-11-01", "2026-11-20"))
        assertEquals("حتى 2026-11-20", AssignmentText.dates("", "2026-11-20"))
        assertEquals("بدون تاريخ محدد", AssignmentText.dates("", ""))
    }

    @Test fun `the observer's banner lists each task once`() {
        assertEquals("أنت مكلّف في هذه اللجنة: مراقبة امتحان", AssignmentText.mine(listOf("exam", "exam")))
        assertEquals("أنت مكلّف في هذه اللجنة: مراقبة امتحان و متابعة حفل", AssignmentText.mine(listOf("exam", "ceremony")))
    }
}
