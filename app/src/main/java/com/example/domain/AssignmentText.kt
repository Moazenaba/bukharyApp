package com.example.domain

/** The words for assignments, in one place so every screen says the same thing. */
object AssignmentText {
    fun task(task: String): String = when (task) {
        "exam" -> "مراقبة امتحان"
        "ceremony" -> "متابعة حفل"
        else -> task
    }

    fun status(status: String): String = when (status) {
        "active" -> "نشط"
        "ended" -> "منتهي"
        "cancelled" -> "ملغي"
        else -> status
    }

    fun dates(startsOn: String, endsOn: String): String =
        listOf(startsOn.takeIf { it.isNotBlank() }?.let { "من $it" }, endsOn.takeIf { it.isNotBlank() }?.let { "حتى $it" })
            .filterNotNull().joinToString(" ").ifBlank { "بدون تاريخ محدد" }

    /** «أنت مكلّف في هذه اللجنة: مراقبة امتحان و متابعة حفل» */
    fun mine(tasks: List<String>): String = "أنت مكلّف في هذه اللجنة: " + tasks.distinct().joinToString(" و ") { task(it) }
}
