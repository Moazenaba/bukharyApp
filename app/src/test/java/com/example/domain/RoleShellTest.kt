package com.example.domain

import com.example.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleShellTest {
    @Test
    fun `only sheikhs and committee heads are field roles`() {
        assertTrue(RoleShell.isFieldRole(UserRole.SHEIKH))
        assertTrue(RoleShell.isFieldRole(UserRole.COMMITTEE_HEAD))
        assertFalse(RoleShell.isFieldRole(UserRole.ADMIN))
        assertFalse(RoleShell.isFieldRole(UserRole.SUPER_ADMIN))
    }

    @Test
    fun `by default nobody's app changes until the new look is approved`() {
        assertFalse(RoleShell.SIMPLE_BY_DEFAULT_FOR_FIELD_ROLES)
        UserRole.values().forEach { assertFalse(RoleShell.useSimple(it, ShellPref.AUTO)) }
    }

    @Test
    fun `once approved, field roles get it by default and admins keep the full app`() {
        assertTrue(RoleShell.useSimple(UserRole.SHEIKH, ShellPref.AUTO, simpleByDefault = true))
        assertTrue(RoleShell.useSimple(UserRole.COMMITTEE_HEAD, ShellPref.AUTO, simpleByDefault = true))
        assertFalse(RoleShell.useSimple(UserRole.ADMIN, ShellPref.AUTO, simpleByDefault = true))
    }

    @Test
    fun `a person's own choice wins for every role`() {
        UserRole.values().forEach {
            assertTrue(RoleShell.useSimple(it, ShellPref.SIMPLE, simpleByDefault = false))
            assertFalse(RoleShell.useSimple(it, ShellPref.FULL, simpleByDefault = true))
        }
    }

    @Test
    fun `saved choices are read back, and anything else means auto`() {
        assertEquals(ShellPref.SIMPLE, ShellPref.fromKey("simple"))
        assertEquals(ShellPref.FULL, ShellPref.fromKey("full"))
        assertEquals(ShellPref.AUTO, ShellPref.fromKey(null))
        assertEquals(ShellPref.AUTO, ShellPref.fromKey("x"))
        assertEquals(ShellPref.V2, ShellPref.fromKey("v2"))
    }

    @org.junit.Test
    fun `the new screens (V2) never turn on the simple look`() {
        UserRole.values().forEach { assertFalse(RoleShell.useSimple(it, ShellPref.V2, simpleByDefault = true)) }
    }

    @Test
    fun `four tabs in a fixed order`() {
        assertEquals(listOf("اليوم", "لجاني", "الحفلات", "الإشعارات"), SimpleTab.values().map { it.label })
    }
}
