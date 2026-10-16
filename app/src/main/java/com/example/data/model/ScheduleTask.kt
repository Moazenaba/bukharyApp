package com.example.data.model

data class ScheduleTask(
    val id: String,
    val title: String,
    val date: String, // YYYY-MM-DD
    val time: String = "09:30 ص",
    val committeeId: String? = null,
    val committeeNumber: Int? = null,
    val assignedAssistantId: String? = null,
    val assignedAssistantName: String? = null,
    val type: String = "general", // delivery, receive, exam, scoring, review, ceremony, general
    val priority: String = "normal", // urgent, high, normal
    val completed: Boolean = false,
    val completedAt: String? = null,
    val deliverable: String = "",
    val notes: String = ""
)
