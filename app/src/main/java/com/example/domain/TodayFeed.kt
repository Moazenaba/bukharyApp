package com.example.domain

import com.example.data.model.Committee
import com.example.data.model.ScheduleTask

enum class FeedKind { ACTION, EXAM, CEREMONY, TASK }

/** One line of «اليوم»: an exam, a ceremony, a task, or a step this person can do. */
data class FeedItem(
    val key: String,
    val kind: FeedKind,
    val title: String,
    val subtitle: String,
    /** YYYY-MM-DD, or empty when the item has no day (a step that can be done now). */
    val date: String,
    /** As written («09:30 ص»), may be empty. */
    val time: String,
    val committeeId: String?,
    val taskId: String?,
    val overdue: Boolean,
    val urgent: Boolean,
    val today: Boolean
)

/** [now] is the one thing to look at first; [later] is the rest, in the order it matters. */
data class TodayFeed(val now: FeedItem?, val later: List<FeedItem>, val total: Int)

object TodayFeedBuilder {
    /** The day of a committee's ceremony: its calendar task, else the ceremony details. */
    fun ceremonyDateOf(c: Committee, tasks: List<ScheduleTask>): String {
        val task = tasks.find { it.id == "task-ceremony-${c.id}" } ?: tasks.find { it.type == "ceremony" && it.committeeId == c.id }
        return task?.date?.takeIf { it.isNotBlank() } ?: c.ceremonyDetails.date
    }

    /** "09:30 ص" / "7:00 م" / "19:00" (Arabic digits too) as minutes of the day; unknown times sort last. */
    fun minutesOf(time: String): Int {
        val latin = time.map { ch ->
            val i = "٠١٢٣٤٥٦٧٨٩".indexOf(ch)
            if (i >= 0) ('0' + i) else ch
        }.joinToString("")
        val m = Regex("""(\d{1,2}):(\d{2})""").find(latin) ?: return 24 * 60
        var hour = m.groupValues[1].toInt()
        val minute = m.groupValues[2].toInt()
        val pm = latin.contains("م") || latin.contains("PM", ignoreCase = true)
        val am = latin.contains("ص") || latin.contains("AM", ignoreCase = true)
        if (pm && hour < 12) hour += 12
        if (am && hour == 12) hour = 0
        return hour * 60 + minute
    }

    private fun label(c: Committee) = "لجنة #${c.committeeNumber}"

    /**
     * [committees] and [tasks] are already limited to this person by the server. [canOn] answers
     * "may I do this server action on that committee?". Each committee gives at most one exam-or-step
     * line, so the same committee is never listed twice for the same thing.
     */
    fun build(
        committees: List<Committee>,
        tasks: List<ScheduleTask>,
        myId: String?,
        canOn: (action: String, committeeId: String) -> Boolean,
        today: String,
        laterLimit: Int = 8
    ): TodayFeed {
        val items = mutableListOf<FeedItem>()

        for (c in committees) {
            val step = NextStep.of(c) { action -> canOn(action, c.id) }
            val examPending = !c.stages.getStage("exam").completed
            val examDate = c.examDate?.take(10).orEmpty()

            if (step.canDo && step.kind != StepKind.DONE) {
                val sub = if (examPending && examDate.isNotBlank()) "${c.committeeName} — الامتحان $examDate" else c.committeeName
                items += FeedItem(
                    key = "action-${c.id}", kind = FeedKind.ACTION, title = "${label(c)}: ${step.title}", subtitle = sub,
                    date = if (examPending) examDate else "", time = "", committeeId = c.id, taskId = null,
                    overdue = false, urgent = false, today = false
                )
            } else if (examPending && examDate.isNotBlank()) {
                items += FeedItem(
                    key = "exam-${c.id}", kind = FeedKind.EXAM, title = "امتحان ${label(c)}", subtitle = c.committeeName,
                    date = examDate, time = "", committeeId = c.id, taskId = null, overdue = false, urgent = false, today = false
                )
            }

            val ceremonyDay = ceremonyDateOf(c, tasks).take(10)
            val mine = myId != null && c.ceremonyDetails.attendees.any { it.id == myId }
            if (!c.stages.getStage("ceremony").completed && ceremonyDay.isNotBlank() && (mine || canOn("committees.finalize", c.id))) {
                val place = c.ceremonyDetails.venue
                items += FeedItem(
                    key = "ceremony-${c.id}", kind = FeedKind.CEREMONY, title = "حفل تكريم ${label(c)}",
                    subtitle = if (place.isBlank()) c.committeeName else "${c.committeeName} — $place",
                    date = ceremonyDay, time = c.ceremonyDetails.time, committeeId = c.id, taskId = null,
                    overdue = false, urgent = false, today = false
                )
            }
        }

        for (t in tasks) {
            // The ceremony's own calendar task is the ceremony line above.
            if (t.completed || t.id.startsWith("task-ceremony-") || t.type == "ceremony") continue
            val sub = listOfNotNull(
                t.committeeNumber?.let { "لجنة #$it" },
                t.deliverable.takeIf { it.isNotBlank() }
            ).joinToString(" — ")
            items += FeedItem(
                key = "task-${t.id}", kind = FeedKind.TASK, title = t.title, subtitle = sub, date = t.date.take(10), time = t.time,
                committeeId = t.committeeId, taskId = t.id, overdue = false, urgent = t.priority == "urgent", today = false
            )
        }

        val marked = items.map {
            it.copy(overdue = it.date.isNotBlank() && it.date < today, today = it.date == today)
        }
        // Late first (the most late first), then today (by hour), then steps with no day, then the future.
        fun rank(i: FeedItem) = when {
            i.overdue -> 0
            i.today -> 1
            i.date.isBlank() -> 2
            else -> 3
        }
        val ordered = marked.sortedWith(
            compareBy<FeedItem> { rank(it) }
                .thenBy { it.date }
                .thenBy { if (it.urgent) 0 else 1 }
                .thenBy { minutesOf(it.time) }
                .thenBy { it.key }
        )
        return TodayFeed(now = ordered.firstOrNull(), later = ordered.drop(1).take(laterLimit), total = ordered.size)
    }
}
