package com.example.data.remote

/**
 * The current user's allowed actions and committee scope, as evaluated by the server
 * (GET /api/auth/me). The UI offers an action only when the server will accept it; the server
 * still enforces every request.
 */
object Permissions {
    @Volatile
    var actions: Map<String, Boolean> = emptyMap()

    @Volatile
    var role: String = ""

    /**
     * Committees the user may act on, by id (numbers repeat across courses). Null means every
     * committee: the server said "all", or the role is not committee-scoped.
     */
    @Volatile
    var committeeScope: Set<String>? = emptySet()

    /** Exam ids whose file and content the user may open; null means all. */
    @Volatile
    var examScope: Set<String>? = emptySet()

    /** Roles the user may give to others (admins never give admin roles). */
    @Volatile
    var grantableRoles: List<String> = listOf("sheikh")

    fun can(action: String): Boolean = actions[action] == true

    fun inScope(committeeId: String): Boolean = committeeScope?.contains(committeeId) ?: true

    /** The action is allowed and the committee is in scope: what the server will accept. */
    fun canOn(action: String, committeeId: String): Boolean = can(action) && inScope(committeeId)

    fun canOpenExam(examId: String): Boolean = can("exams.file") && (examScope?.contains(examId) ?: true)

    /**
     * Applies the server's evaluated scope: "all", or a list of ids. Anything else (an older server,
     * a missing field) grants nothing until the next successful refresh: scopes fail closed.
     */
    fun applyServerScope(committeeScope: Any?, examScope: Any?) {
        this.committeeScope = when (committeeScope) {
            "all" -> null
            is List<*> -> committeeScope.mapNotNull { it as? String }.toSet()
            else -> emptySet()
        }
        this.examScope = when (examScope) {
            "all" -> null
            is List<*> -> examScope.mapNotNull { it as? String }.toSet()
            else -> emptySet()
        }
    }

    fun clear() {
        actions = emptyMap()
        role = ""
        grantableRoles = listOf("sheikh")
        committeeScope = emptySet()
        examScope = emptySet()
    }
}
