package com.example.data.model

/**
 * «شيخ اللجنة»: a sheikh made the head of some committees for a date window by an admin. Their powers follow the
 * server's evaluated permissions (headCommitteeIds). [ceremonyDecision] is "yes", "no" or "undecided".
 */
data class HeadGrant(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val committeeIds: List<String> = emptyList(),
    val startsOn: String = "",
    val endsOn: String = "",
    /** "active", "ended" or "cancelled". */
    val status: String = "active",
    val ceremonyDecision: String = "undecided",
    /** "admin", "head" or "holder": who made the current decision. */
    val decidedByRole: String? = null,
    val grantedBy: String = ""
)
