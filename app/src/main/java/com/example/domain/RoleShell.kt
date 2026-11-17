package com.example.domain

import com.example.data.model.UserRole

/** What the person chose for the look of the app (menu: «الواجهة المبسطة»). */
enum class ShellPref(val key: String) {
    /** Follow the app's default for the person's role. */
    AUTO("auto"),
    SIMPLE("simple"),
    FULL("full"),
    /** «الواجهات 2»: the new screens built from scratch (docs/V2-ROLES.md). */
    V2("v2");

    companion object {
        fun fromKey(key: String?): ShellPref = values().firstOrNull { it.key == key } ?: AUTO
    }
}

/** The four tabs of the simple look, in order. */
enum class SimpleTab(val label: String) {
    TODAY("اليوم"),
    COMMITTEES("لجاني"),
    CEREMONIES("الحفلات"),
    NOTIFICATIONS("الإشعارات")
}

object RoleShell {
    /**
     * Whether sheikhs and committee heads get the simple look without choosing it. It stays off until
     * the new screens have been seen on a phone and approved; anyone can try them before that from
     * the menu, so nobody's app changes by surprise.
     */
    const val SIMPLE_BY_DEFAULT_FOR_FIELD_ROLES = false

    /** The roles that find the full app complicated: they see only their own committees. */
    fun isFieldRole(role: UserRole): Boolean = role == UserRole.SHEIKH || role == UserRole.COMMITTEE_HEAD

    fun useSimple(role: UserRole, pref: ShellPref, simpleByDefault: Boolean = SIMPLE_BY_DEFAULT_FOR_FIELD_ROLES): Boolean =
        when (pref) {
            ShellPref.SIMPLE -> true
            ShellPref.FULL, ShellPref.V2 -> false
            ShellPref.AUTO -> simpleByDefault && isFieldRole(role)
        }
}
