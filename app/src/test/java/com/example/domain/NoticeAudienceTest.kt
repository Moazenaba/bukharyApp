package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoticeAudienceTest {
    @Test fun `everyone needs nothing else`() {
        assertEquals(mapOf("kind" to "all"), NoticeAudience.of(NoticeTo.ALL, emptySet(), emptySet()))
    }

    @Test fun `a category needs at least one pick, one pick uses the older field`() {
        assertNull(NoticeAudience.of(NoticeTo.GROUP, emptySet(), emptySet()))
        assertEquals(mapOf("kind" to "staffRole", "staffRole" to "observer"), NoticeAudience.of(NoticeTo.GROUP, setOf("observer"), emptySet()))
    }

    @Test fun `several categories go together in a fixed order`() {
        assertEquals(
            mapOf("kind" to "staffRole", "staffRoles" to listOf("observer", "ceremony_sheikh")),
            NoticeAudience.of(NoticeTo.GROUP, setOf("ceremony_sheikh", "observer"), emptySet())
        )
    }

    @Test fun `people need at least one pick, one person is a list of one`() {
        assertNull(NoticeAudience.of(NoticeTo.PEOPLE, emptySet(), emptySet()))
        assertEquals(mapOf("kind" to "users", "userIds" to listOf("u1")), NoticeAudience.of(NoticeTo.PEOPLE, emptySet(), setOf("u1")))
    }

    @Test fun `heading says who`() {
        assertEquals("إرسال تنبيه لشخص", NoticeAudience.heading(NoticeTo.PEOPLE, 1))
        assertEquals("إرسال تنبيه لأشخاص", NoticeAudience.heading(NoticeTo.PEOPLE, 3))
    }
}
