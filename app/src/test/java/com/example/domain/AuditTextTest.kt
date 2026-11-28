package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AuditTextTest {
    @Test fun `known actions are said in Arabic`() {
        assertEquals("تغيير الحالة الإدارية للجنة", AuditText.action("committee.lifecycle"))
        assertEquals("إرسال تنبيه", AuditText.action("notification.send"))
    }

    @Test fun `an unknown action shows as it is, never hidden`() {
        assertEquals("something.new", AuditText.action("something.new"))
    }

    @Test fun `the filter starts with everything`() {
        assertEquals("" to "الكل", AuditText.ENTITIES.first())
    }
}
