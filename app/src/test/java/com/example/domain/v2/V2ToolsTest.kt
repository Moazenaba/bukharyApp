package com.example.domain.v2

import com.example.data.model.Committee
import com.example.data.model.StagesMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2ToolsTest {
    private fun allowed(vararg actions: String): (String) -> Boolean = { it in actions }

    @Test fun `each role gets its own four tabs`() {
        assertEquals(listOf(V2Tab.NOW, V2Tab.MINE, V2Tab.CEREMONIES, V2Tab.ME), V2Tools.tabs("sheikh"))
        assertEquals(listOf(V2Tab.NOW, V2Tab.MINE, V2Tab.TEAM, V2Tab.ME), V2Tools.tabs("committee_head"))
        assertEquals(listOf(V2Tab.BOARD, V2Tab.COMMITTEES, V2Tab.PEOPLE, V2Tab.MORE), V2Tools.tabs("admin"))
        // A sheikh with a temporary head grant gets the team tab, like a committee head.
        assertEquals(listOf(V2Tab.NOW, V2Tab.MINE, V2Tab.TEAM, V2Tab.ME), V2Tools.tabs("sheikh", heads = true))
        assertEquals(V2Tools.tabs("admin"), V2Tools.tabs("super_admin"))
        assertTrue(V2Tools.tabs("anything-else").all { it.ordinal <= V2Tab.ME.ordinal })
    }

    @Test fun `a plain sheikh gets no management tools`() {
        val t = V2Tools.tools(allowed("committees.read", "tasks.read"))
        assertFalse(V2Tool.NEXT_STEP in t)
        assertFalse(V2Tool.STAFF in t)
        assertFalse(V2Tool.OBSERVERS in t)
        assertFalse(V2Tool.EXAM_PAPER in t)
        assertFalse(V2Tool.OBSERVER_NOTES in t)
        assertTrue(V2Tool.MAPS in t && V2Tool.CALL_HEAD in t)
    }

    @Test fun `an observer gets the paper and notes, still nothing that writes to the committee`() {
        val t = V2Tools.tools(allowed("committees.read"), observesExam = true, observesAny = true)
        assertTrue(V2Tool.EXAM_PAPER in t && V2Tool.OBSERVER_NOTES in t)
        assertFalse(V2Tool.NEXT_STEP in t || V2Tool.STAFF in t || V2Tool.OBSERVERS in t)
    }

    @Test fun `a committee head runs his committees`() {
        val t = V2Tools.tools(allowed("committees.reception", "committees.scoring", "committees.finalize", "committees.members", "assignments.manage", "users.read", "exams.file"))
        assertTrue(listOf(V2Tool.NEXT_STEP, V2Tool.STAFF, V2Tool.OBSERVERS, V2Tool.USERS, V2Tool.EXAM_PAPER).all { it in t })
        assertFalse(V2Tool.AUDIT in t || V2Tool.SEND_NOTICE in t || V2Tool.JOIN_REQUESTS in t)
    }

    @Test fun `an admin always gets the audit log entry, others only when the server allows`() {
        assertTrue(V2Tool.AUDIT in V2Tools.tools(allowed("notifications.create"), role = "admin"))
        assertFalse(V2Tool.AUDIT in V2Tools.tools(allowed("notifications.create"), role = "sheikh"))
    }

    @Test fun `audit and database only for whoever the server allows`() {
        assertTrue(V2Tool.AUDIT in V2Tools.tools(allowed("audit.read", "database.admin")))
        assertFalse(V2Tool.AUDIT in V2Tools.tools(allowed("notifications.create")))
    }

    private fun committee(n: Int, done: Int, name: String = "لجنة $n") = Committee(
        id = "c$n", committeeNumber = n, committeeName = name,
        stages = com.example.domain.STAGE_ORDER.take(done).fold(StagesMap()) { s, k -> s.updateStage(k, true) }
    )

    @Test fun `stage label and board groups follow the first stage not done`() {
        assertEquals("في انتظار الامتحان", V2Stages.label(committee(1, 0)))
        assertEquals("التصحيح", V2Stages.label(committee(2, 2)))
        assertEquals("اكتملت", V2Stages.label(committee(3, 9)))
        val counts = V2Stages.counts(listOf(committee(1, 0), committee(2, 1), committee(3, 3), committee(4, 8), committee(5, 9)))
        assertEquals(2, counts[V2Stages.Group.EXAM])
        assertEquals(1, counts[V2Stages.Group.SCORING])
        assertEquals(1, counts[V2Stages.Group.CEREMONY])
        assertEquals(0, counts[V2Stages.Group.SHEET])
    }

    @Test fun `search by number, with or without #, and by name`() {
        val c = committee(12, 0, "الأربعون النووية")
        assertTrue(V2Stages.matches(c, "12") && V2Stages.matches(c, "#12") && V2Stages.matches(c, "النووية") && V2Stages.matches(c, " "))
        assertFalse(V2Stages.matches(c, "1"))
    }
}
