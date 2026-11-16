package com.example.data.model

/**
 * An extra job a person has on top of their role (they stay a sheikh): observing another committee's exam
 * or following its ceremony. Read-only powers inside that committee, for a limited time.
 */
data class CommitteeAssignment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val committeeId: String = "",
    val committeeNumber: Int = 0,
    val committeeName: String = "",
    /** "exam" or "ceremony". */
    val task: String = "exam",
    /** "active", "ended" or "cancelled". */
    val status: String = "active",
    val startsOn: String = "",
    val endsOn: String = "",
    val assignedBy: String = ""
)

/** A note the observer wrote on the committee they were sent to. */
data class AssignmentNote(val id: String = "", val note: String = "", val by: String = "", val at: String = "")
