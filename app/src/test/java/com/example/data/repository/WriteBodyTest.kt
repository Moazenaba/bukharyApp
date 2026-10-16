package com.example.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WriteBodyTest {
    @Test
    fun `adds the expected version and keeps whole numbers whole`() {
        val out = WriteBody.withExpectedUpdatedAt("""{"id":"c1","committeeNumber":7,"stages":{"exam":{"completed":true}}}""", "2026-10-16T10:00:00.000Z")!!
        assertTrue(out.contains("\"expectedUpdatedAt\":\"2026-10-16T10:00:00.000Z\""))
        assertTrue(out.contains("\"committeeNumber\":7"))
        assertTrue(!out.contains("7.0"))
    }

    @Test
    fun `a body that is not an object is sent unchanged`() {
        assertEquals("[1,2]", WriteBody.withExpectedUpdatedAt("[1,2]", "x"))
        assertNull(WriteBody.withExpectedUpdatedAt(null, "x"))
    }

    @Test
    fun `reads the committee or user version from a server response`() {
        val v = WriteBody.serverVersion("""{"success":true,"committee":{"id":"c9","updatedAt":"2026-10-16T11:00:00.000Z"}}""")!!
        assertEquals("committee", v.kind)
        assertEquals("c9", v.id)
        assertEquals("2026-10-16T11:00:00.000Z", v.updatedAt)
        assertNull(WriteBody.serverVersion("""{"success":true}"""))
    }
}
