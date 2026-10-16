package com.example.data.remote

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionsTest {
    @After
    fun reset() = Permissions.clear()

    @Test
    fun `heads and assistants act only on assigned committees`() {
        Permissions.role = "assistant"
        Permissions.assignedCommittees = listOf(3, 4)
        assertTrue(Permissions.inScope(3))
        assertFalse(Permissions.inScope(5))
    }

    @Test
    fun `super admins and teachers are not limited by committee`() {
        Permissions.role = "super_admin"
        Permissions.assignedCommittees = emptyList()
        assertTrue(Permissions.inScope(99))
    }

    @Test
    fun `an action is offered only when the server granted it`() {
        Permissions.actions = mapOf("committees.reception" to true, "committees.finalize" to false)
        assertTrue(Permissions.can("committees.reception"))
        assertFalse(Permissions.can("committees.finalize"))
        assertFalse(Permissions.can("unknown.action"))
    }
}
