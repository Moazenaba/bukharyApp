package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeTimeTest {
    // 2026-11-27 12:00 Cairo (UTC+2) = 10:00 UTC
    private val now = NoticeTime.parse("2026-11-27T10:00:00.000Z")!!.time

    @Test fun `minutes and hours today`() {
        assertEquals("الآن", NoticeTime.short("2026-11-27T09:59:40.000Z", now))
        assertEquals("منذ دقيقة", NoticeTime.short("2026-11-27T09:58:59.000Z", now))
        assertEquals("منذ 5 دقائق", NoticeTime.short("2026-11-27T09:55:00.000Z", now))
        assertEquals("منذ ساعتين", NoticeTime.short("2026-11-27T08:00:00.000Z", now))
    }

    @Test fun `groups follow the Cairo day, not UTC`() {
        // 23:30 UTC on the 26th is already the 27th in Cairo.
        assertEquals(NoticeTime.Group.TODAY, NoticeTime.group("2026-11-26T23:30:00.000Z", now))
        assertEquals(NoticeTime.Group.YESTERDAY, NoticeTime.group("2026-11-26T08:00:00.000Z", now))
        assertEquals(NoticeTime.Group.EARLIER, NoticeTime.group("2026-11-20T08:00:00.000Z", now))
        assertEquals(NoticeTime.Group.EARLIER, NoticeTime.group("not a date", now))
    }

    @Test fun `yesterday shows the clock, older shows the day`() {
        assertTrue(NoticeTime.short("2026-11-26T08:00:00.000Z", now).startsWith("أمس "))
        assertTrue(NoticeTime.short("2026-11-20T08:00:00.000Z", now).contains("20/11"))
    }

    @Test fun `server timestamps with microseconds are read`() {
        assertTrue(NoticeTime.parse("2026-11-27T09:55:00.123456Z") != null)
    }
}
