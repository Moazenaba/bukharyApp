package com.example.data.remote

/**
 * The current user's allowed actions as evaluated by the server (GET /api/auth/me).
 * UI shows an action only when the server will accept it; the server still enforces it.
 * Committee-scoped rules (heads/assistants only on assigned committees) are applied with
 * [inScope].
 */
object Permissions {
    @Volatile
    var actions: Map<String, Boolean> = emptyMap()

    @Volatile
    var role: String = ""

    @Volatile
    var assignedCommittees: List<Int> = emptyList()

    fun can(action: String): Boolean = actions[action] == true

    fun inScope(committeeNumber: Int): Boolean =
        (role != "committee_head" && role != "assistant") || committeeNumber in assignedCommittees

    fun clear() {
        actions = emptyMap()
        role = ""
        assignedCommittees = emptyList()
    }
}
