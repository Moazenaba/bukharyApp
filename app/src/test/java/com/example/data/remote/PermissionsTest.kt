package com.example.data.remote

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionsTest {
    @After
    fun reset() = Permissions.clear()

    @Test
    fun `committee-scoped users act only on assigned committees, by id`() {
        Permissions.applyServerScope(listOf("c-3", "c-4"), null)
        assertTrue(Permissions.inScope("c-3"))
        assertFalse(Permissions.inScope("c-5"))
    }

    @Test
    fun `the same committee number in another course is a different committee`() {
        Permissions.applyServerScope(listOf("course1-n1"), null)
        assertFalse(Permissions.inScope("course2-n1"))
    }

    @Test
    fun `all committees from the server means every committee`() {
        Permissions.applyServerScope("all", null)
        assertTrue(Permissions.inScope("anything"))
    }

    @Test
    fun `an unknown scope means no committee rather than all`() {
        Permissions.applyServerScope(listOf(1.0, 2.0), null)
        assertFalse(Permissions.inScope("c-1"))
    }

    @Test
    fun `an action is offered only when the server granted it and the committee is in scope`() {
        Permissions.actions = mapOf("committees.reception" to true, "committees.finalize" to false)
        Permissions.applyServerScope(listOf("c-1"), null)
        assertTrue(Permissions.canOn("committees.reception", "c-1"))
        assertFalse(Permissions.canOn("committees.reception", "c-2"))
        assertFalse(Permissions.can("committees.finalize"))
        assertFalse(Permissions.can("unknown.action"))
    }

    @Test
    fun `exam files follow the allow-list`() {
        Permissions.actions = mapOf("exams.file" to true)
        Permissions.applyServerScope("all", listOf("exam-a"))
        assertTrue(Permissions.canOpenExam("exam-a"))
        assertFalse(Permissions.canOpenExam("exam-b"))
    }

    @Test
    fun `an observer assignment is limited to its committee and gives no write scope`() {
        Permissions.applyServerScope(listOf("own"), null)
        Permissions.observing = listOf(ObservedRef("a1", "other", "exam"))
        assertEquals(listOf("a1"), Permissions.observingOn("other").map { it.id })
        assertTrue(Permissions.observingOn("third").isEmpty())
        // The assignment never widens the scope the server evaluated for writes.
        assertFalse(Permissions.inScope("other"))
        Permissions.clear()
        assertTrue(Permissions.observing.isEmpty())
    }
}
