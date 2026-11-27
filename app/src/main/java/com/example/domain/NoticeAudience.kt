package com.example.domain

/** Who a notice goes to: everyone, a category of staff, or people picked by hand (one or many). */
enum class NoticeTo(val label: String) { ALL("للجميع"), GROUP("لفئة معينة"), PEOPLE("لأشخاص محددين") }

object NoticeAudience {
    /** The categories (server `staffRole`): jobs on committees. */
    val GROUPS: List<Pair<String, String>> = listOf(
        "observer" to "المراقبون",
        "ceremony_sheikh" to "مشايخ الحفلات",
        "owner" to "مشايخ اللجان (أصحاب اللجان)"
    )

    /** The server's `audience` for a choice, or null while the choice is incomplete (nothing picked yet). */
    fun of(to: NoticeTo, groups: Set<String>, people: Set<String>): Map<String, Any?>? = when (to) {
        NoticeTo.ALL -> mapOf("kind" to "all")
        NoticeTo.GROUP -> when (groups.size) {
            0 -> null
            1 -> mapOf("kind" to "staffRole", "staffRole" to groups.first())
            else -> mapOf("kind" to "staffRole", "staffRoles" to GROUPS.map { it.first }.filter { it in groups })
        }
        NoticeTo.PEOPLE -> if (people.isEmpty()) null else mapOf("kind" to "users", "userIds" to people.toList())
    }

    fun heading(to: NoticeTo, pickedCount: Int): String = when (to) {
        NoticeTo.ALL -> "إرسال تنبيه للجميع"
        NoticeTo.GROUP -> "إرسال تنبيه لفئة"
        NoticeTo.PEOPLE -> if (pickedCount == 1) "إرسال تنبيه لشخص" else "إرسال تنبيه لأشخاص"
    }
}
