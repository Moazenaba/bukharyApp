package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DayTextTest {
    private val today = "2026-11-13" // a Friday

    @Test
    fun `days between dates, across month and year ends`() {
        assertEquals(0, DayText.daysBetween("2026-11-13", "2026-11-13"))
        assertEquals(1, DayText.daysBetween("2026-11-30", "2026-12-01"))
        assertEquals(-3, DayText.daysBetween("2026-11-13", "2026-11-10"))
        assertEquals(1, DayText.daysBetween("2026-12-31", "2027-01-01"))
        assertNull(DayText.daysBetween("", "2026-11-13"))
    }

    @Test
    fun `weekday and short date`() {
        assertEquals("الجمعة", DayText.weekday("2026-11-13"))
        assertEquals("الأحد", DayText.weekday("2026-11-15"))
        assertEquals("13/11", DayText.shortDate("2026-11-13"))
        assertEquals("الجمعة 13/11", DayText.full("2026-11-13T10:00:00Z"))
        assertEquals("", DayText.full("not a date"))
    }

    @Test
    fun `relative words`() {
        assertEquals("اليوم", DayText.relative("2026-11-13", today))
        assertEquals("بكرة", DayText.relative("2026-11-14", today))
        assertEquals("أمس", DayText.relative("2026-11-12", today))
        assertEquals("يوم الأحد", DayText.relative("2026-11-15", today))
        assertEquals("متأخر يومين", DayText.relative("2026-11-11", today))
        assertEquals("متأخر 5 أيام", DayText.relative("2026-11-08", today))
        assertEquals("متأخر 12 يوم", DayText.relative("2026-11-01", today))
        assertEquals("الجمعة 20/11", DayText.relative("2026-11-20", today))
        assertEquals("", DayText.relative("", today))
    }
}
