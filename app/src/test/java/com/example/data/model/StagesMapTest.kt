package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StagesMapTest {
    @Test
    fun `changing a stage keeps the notes written on it`() {
        val start = StagesMap(exam = StageItem(completed = false, notes = "ملاحظة مهمة"))
        val updated = start.updateStage("exam", completed = true, completedAt = "2026/10/16 - 10:00 ص")
        assertTrue(updated.exam.completed)
        assertEquals("ملاحظة مهمة", updated.exam.notes)
        assertEquals("2026/10/16 - 10:00 ص", updated.exam.completedAt)
    }

    @Test
    fun `other stages are untouched`() {
        val updated = StagesMap().updateStage("scoring", completed = true)
        assertFalse(updated.exam.completed)
        assertTrue(updated.scoring.completed)
    }
}
