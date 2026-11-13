package com.example.domain

import com.example.data.model.AssistantReception
import com.example.data.model.Committee
import com.example.data.model.ExamApproval
import com.example.data.model.StagesMap
import com.example.ui.components.OrderedStageKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NextStepTest {
    private fun stages(vararg done: String): StagesMap {
        var s = StagesMap()
        done.forEach { s = s.updateStage(it, true, "2026-11-01T10:00:00Z") }
        return s
    }

    private fun committee(
        stages: StagesMap = stages("registration"),
        received: Boolean = false,
        approved: Boolean = false
    ) = Committee(
        id = "c1", committeeNumber = 7, committeeName = "لجنة النور", stages = stages,
        receivedByAssistant = if (received) AssistantReception() else null,
        examApprovedBy = if (approved) ExamApproval() else null
    )

    private val everything: (String) -> Boolean = { true }
    private val nothing: (String) -> Boolean = { false }

    @Test
    fun `the stage order is the app's stage order`() {
        assertEquals(OrderedStageKeys.map { it.first }, STAGE_ORDER)
    }

    @Test
    fun `before the exam the first jobs are reception, then readiness, then holding the exam`() {
        assertEquals(StepKind.RECEIVE, NextStep.of(committee(), everything).kind)
        assertEquals(StepKind.EXAM_READY, NextStep.of(committee(received = true), everything).kind)
        val held = NextStep.of(committee(received = true, approved = true), everything)
        assertEquals(StepKind.EXAM_HELD, held.kind)
        assertEquals("exam", held.stageKey)
        assertTrue(held.oneTap)
    }

    @Test
    fun `each later stage has its own step and the right that gates it`() {
        val expected = listOf(
            "exam" to Triple(StepKind.SCORING, StepRight.SCORING, false),
            "scoring" to Triple(StepKind.AGGREGATION, StepRight.SCORING, false),
            "aggregation" to Triple(StepKind.REVIEW, StepRight.SCORING, true),
            "review" to Triple(StepKind.SHEET, StepRight.FINALIZE, false),
            "sheet" to Triple(StepKind.ACCREDITATION, StepRight.FINALIZE, true),
            "accreditation" to Triple(StepKind.ANNOUNCEMENT, StepRight.FINALIZE, true),
            "announcement" to Triple(StepKind.CEREMONY, StepRight.FINALIZE, false)
        )
        for ((lastDone, want) in expected) {
            val upTo = STAGE_ORDER.take(STAGE_ORDER.indexOf(lastDone) + 1).toTypedArray()
            val step = NextStep.of(committee(stages = stages(*upTo), received = true, approved = true), everything)
            assertEquals("after $lastDone", want.first, step.kind)
            assertEquals("after $lastDone", want.second, step.right)
            assertEquals("after $lastDone", want.third, step.oneTap)
            assertEquals(upTo.size, step.done)
        }
    }

    @Test
    fun `a finished committee has nothing left`() {
        val step = NextStep.of(committee(stages = stages(*STAGE_ORDER.toTypedArray())), everything)
        assertEquals(StepKind.DONE, step.kind)
        assertNull(step.right)
        assertFalse(step.canDo)
        assertEquals(9, step.done)
    }

    @Test
    fun `canDo follows the server action on that committee`() {
        val asked = mutableListOf<String>()
        val step = NextStep.of(committee()) { asked += it; false }
        assertFalse(step.canDo)
        assertEquals(listOf("committees.reception"), asked)
        assertTrue(step.waiting.isNotBlank())
        assertTrue(NextStep.of(committee()) { it == "committees.reception" }.canDo)
        assertFalse(NextStep.of(committee(), nothing).canDo)
    }
}
