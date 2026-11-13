package com.example.domain

import com.example.data.model.Committee

/** The nine stages, in order (the same list as the app's stage stepper and the shared rules.json). */
val STAGE_ORDER: List<String> =
    listOf("registration", "exam", "scoring", "aggregation", "review", "sheet", "accreditation", "announcement", "ceremony")

enum class StepKind { RECEIVE, EXAM_READY, EXAM_HELD, SCORING, AGGREGATION, REVIEW, SHEET, ACCREDITATION, ANNOUNCEMENT, CEREMONY, DONE }

/** The server action that lets someone do a step (committee-scoped). */
enum class StepRight(val action: String) {
    RECEPTION("committees.reception"),
    SCORING("committees.scoring"),
    FINALIZE("committees.finalize")
}

/**
 * What a committee is waiting for. The nine stages hide that the first one or two things are not
 * stages at all (reception and exam readiness are recorded on the committee), so a head had to know
 * which tab held the next job. This answers it in one place.
 */
data class NextStep(
    val kind: StepKind,
    /** The stage this step completes, when completing the stage is the step. */
    val stageKey: String?,
    /** What to do, written for the person who can do it. */
    val title: String,
    /** What the committee waits for, written for anyone. */
    val waiting: String,
    val right: StepRight?,
    /** This user may do it on this committee. */
    val canDo: Boolean,
    /** One tap is enough; otherwise a form is needed. */
    val oneTap: Boolean,
    val done: Int,
    val total: Int = STAGE_ORDER.size
) {
    companion object {
        /** [canOn] answers "may I do this server action on this committee?". */
        fun of(c: Committee, canOn: (String) -> Boolean): NextStep {
            val done = STAGE_ORDER.count { c.stages.getStage(it).completed }
            fun step(kind: StepKind, stage: String?, title: String, waiting: String, right: StepRight, oneTap: Boolean) =
                NextStep(kind, stage, title, waiting, right, canOn(right.action), oneTap, done)

            return when (STAGE_ORDER.firstOrNull { !c.stages.getStage(it).completed }) {
                null -> NextStep(StepKind.DONE, null, "اكتملت كل المراحل", "اكتملت كل مراحل اللجنة", null, false, true, done)
                "registration", "exam" -> when {
                    c.receivedByAssistant == null ->
                        step(StepKind.RECEIVE, null, "توثيق استلام اللجنة", "اللجنة في انتظار توثيق استلامها", StepRight.RECEPTION, true)
                    c.examApprovedBy == null ->
                        step(StepKind.EXAM_READY, null, "اعتماد جاهزية الامتحان", "في انتظار اعتماد جاهزية الامتحان", StepRight.RECEPTION, true)
                    else ->
                        step(StepKind.EXAM_HELD, "exam", "تسجيل انعقاد الامتحان", "في انتظار انعقاد الامتحان", StepRight.RECEPTION, true)
                }
                "scoring" -> step(StepKind.SCORING, "scoring", "التصحيح ورصد الدرجات", "في انتظار التصحيح", StepRight.SCORING, false)
                "aggregation" -> step(StepKind.AGGREGATION, "aggregation", "تجميع الدرجات", "في انتظار تجميع الدرجات", StepRight.SCORING, false)
                "review" -> step(StepKind.REVIEW, "review", "المراجعة والتدقيق", "في انتظار المراجعة والتدقيق", StepRight.SCORING, true)
                "sheet" -> step(StepKind.SHEET, "sheet", "كتابة الكشف", "في انتظار كتابة الكشف", StepRight.FINALIZE, false)
                "accreditation" -> step(StepKind.ACCREDITATION, "accreditation", "اعتماد الكشف", "في انتظار الاعتماد", StepRight.FINALIZE, true)
                "announcement" -> step(StepKind.ANNOUNCEMENT, "announcement", "إعلان النتيجة", "في انتظار إعلان النتيجة", StepRight.FINALIZE, true)
                else -> step(StepKind.CEREMONY, "ceremony", "الحفل والتكريم", "في انتظار إقامة الحفل", StepRight.FINALIZE, false)
            }
        }
    }
}
