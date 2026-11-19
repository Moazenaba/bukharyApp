package com.example.ui.v2

import org.junit.Assert.assertEquals
import org.junit.Test

class V2TextTest {
    @Test fun `arabic counts agree with the number`() {
        val n = { k: Int -> v2Count(k, "إشعار جديد", "إشعارين جداد", "إشعارات جديدة", "إشعار جديد") }
        assertEquals("إشعار جديد", n(1))
        assertEquals("إشعارين جداد", n(2))
        assertEquals("3 إشعارات جديدة", n(3))
        assertEquals("10 إشعارات جديدة", n(10))
        assertEquals("11 إشعار جديد", n(11))
    }

    @Test fun `an initial skips titles and the article`() {
        assertEquals("م", v2Initial("الشيخ محمود"))
        assertEquals("أ", v2Initial("أحمد سليم"))
        assertEquals("ع", v2Initial("  عبد الله  "))
        assertEquals("ح", v2Initial("الحسن"))
        assertEquals("؟", v2Initial(" "))
    }

    @Test fun `end dates are picked by adding days`() {
        assertEquals("2026-11-26", v2PlusDays("2026-11-19", 7))
        assertEquals("2026-12-19", v2PlusDays("2026-11-19", 30))
        assertEquals("", v2PlusDays("not a date", 7))
    }
}
