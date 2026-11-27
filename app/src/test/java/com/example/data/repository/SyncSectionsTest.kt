package com.example.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyncSectionsTest {
    private val base = mapOf("me" to "m", "committees" to "c", "tasks" to "t", "exams" to "e", "books" to "b")

    @Test fun `first load, an older server, or a change to my own account loads everything`() {
        assertNull(SyncSections.changed(null, base))
        assertNull(SyncSections.changed(base, null))
        assertNull(SyncSections.changed(base, base + ("me" to "m2")))
    }

    @Test fun `only the lists whose part changed are loaded`() {
        assertEquals(setOf("tasks"), SyncSections.changed(base, base + ("tasks" to "t2")))
        assertEquals(setOf("committees", "books"), SyncSections.changed(base, base + ("committees" to "c2") + ("books" to "b2")))
    }

    @Test fun `nothing changed loads nothing`() {
        assertEquals(emptySet<String>(), SyncSections.changed(base, base))
    }

    @Test fun `a list the server added since the last load is loaded`() {
        assertEquals(setOf("sheet"), SyncSections.changed(base, base + ("sheet" to "s")))
    }
}
