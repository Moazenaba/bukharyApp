package com.example.domain.v2

import com.example.data.model.Committee
import com.example.domain.STAGE_ORDER

/**
 * «الواجهات 2»: which tabs and tools each person gets. Written from docs/V2-ROLES.md, and driven only by
 * the role and the server-evaluated actions, so the new screens never show what the server would refuse.
 */
enum class V2Tab(val label: String) {
    NOW("الآن"),
    MINE("لجاني"),
    CEREMONIES("الحفلات"),
    TEAM("الفريق"),
    ME("حسابي"),
    BOARD("لوحة"),
    COMMITTEES("اللجان"),
    PEOPLE("الناس"),
    MORE("المزيد")
}

enum class V2Tool {
    MAPS, CALL_HEAD, EXAM_PAPER, OBSERVER_NOTES, NEXT_STEP, STAFF, OBSERVERS,
    JOIN_REQUESTS, USERS, EXAMS, CALENDAR, SEND_NOTICE, COURSE, AUDIT, DATABASE, FULL_VIEW
}

object V2Tools {
    fun isAdmin(role: String): Boolean = role == "admin" || role == "super_admin"

    /** Four tabs at most, in the order a person of that role reaches for them. */
    /** [heads]: a sheikh holding a temporary head grant gets the head's team tab too. */
    fun tabs(role: String, heads: Boolean = false): List<V2Tab> = when {
        isAdmin(role) -> listOf(V2Tab.BOARD, V2Tab.COMMITTEES, V2Tab.PEOPLE, V2Tab.MORE)
        role == "committee_head" || heads -> listOf(V2Tab.NOW, V2Tab.MINE, V2Tab.TEAM, V2Tab.ME)
        else -> listOf(V2Tab.NOW, V2Tab.MINE, V2Tab.CEREMONIES, V2Tab.ME)
    }

    /**
     * The tools this person may use. [can] is the server's yes/no for an action (Permissions.can);
     * [observesExam] / [observesAny]: they hold an active «مراقب» assignment.
     */
    fun tools(can: (String) -> Boolean, observesExam: Boolean = false, observesAny: Boolean = false, role: String = ""): Set<V2Tool> = buildSet {
        add(V2Tool.MAPS)
        add(V2Tool.CALL_HEAD)
        add(V2Tool.CALENDAR)
        add(V2Tool.COURSE)
        add(V2Tool.FULL_VIEW)
        if (can("exams.file") || observesExam) add(V2Tool.EXAM_PAPER)
        if (observesAny) add(V2Tool.OBSERVER_NOTES)
        if (can("committees.reception") || can("committees.scoring") || can("committees.finalize")) add(V2Tool.NEXT_STEP)
        if (can("committees.members")) add(V2Tool.STAFF)
        if (can("assignments.manage")) add(V2Tool.OBSERVERS)
        if (can("users.manage")) add(V2Tool.JOIN_REQUESTS)
        if (can("users.read")) add(V2Tool.USERS)
        if (can("exams.read")) add(V2Tool.EXAMS)
        if (can("notifications.create")) add(V2Tool.SEND_NOTICE)
        // Admins read the audit log (without what super admins did); the server filters it.
        if (can("audit.read") || isAdmin(role)) add(V2Tool.AUDIT)
        if (can("database.admin")) add(V2Tool.DATABASE)
    }
}

/** The stage a committee is in now, named for people (the first stage not yet done). */
object V2Stages {
    val LABEL: Map<String, String> = mapOf(
        "registration" to "في انتظار الامتحان",
        "exam" to "تم الامتحان",
        "scoring" to "التصحيح",
        "aggregation" to "تجميع الدرجات",
        "review" to "المراجعة",
        "sheet" to "كتابة الكشف",
        "accreditation" to "الاعتماد",
        "announcement" to "إعلان النتيجة",
        "ceremony" to "الحفل"
    )

    /** The four groups the board counts, each opening the list filtered to its stages. */
    enum class Group(val label: String, val stages: Set<String>) {
        EXAM("في انتظار الامتحان", setOf("registration", "exam")),
        SCORING("التصحيح والتجميع", setOf("scoring", "aggregation", "review")),
        SHEET("الكشف والاعتماد", setOf("sheet", "accreditation", "announcement")),
        CEREMONY("الحفل", setOf("ceremony"))
    }

    /** null when every stage is done. */
    fun current(c: Committee): String? = STAGE_ORDER.firstOrNull { !c.stages.getStage(it).completed }

    fun label(c: Committee): String = current(c)?.let { LABEL[it] } ?: "اكتملت"

    fun groupOf(c: Committee): Group? = current(c)?.let { s -> Group.values().firstOrNull { s in it.stages } }

    fun counts(committees: List<Committee>): Map<Group, Int> =
        Group.values().associateWith { g -> committees.count { groupOf(it) == g } }

    /** Search by number, name, sheikh, place or book; blank matches all. */
    fun matches(c: Committee, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return c.committeeNumber.toString() == q.removePrefix("#") ||
            listOf(c.committeeName, c.sheikhName, c.sheikhCountry, c.associationName, c.competitionBook).any { it.contains(q) }
    }
}
