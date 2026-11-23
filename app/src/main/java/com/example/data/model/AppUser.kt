package com.example.data.model

import com.example.ui.AppNavScreen

/**
 * The four roles (server/policy.ts): the super admin sees everything; an admin sees everything
 * except other admins; a committee head staffs the committees assigned to them with sheikhs;
 * a sheikh sees only the committees and ceremonies assigned to them.
 */
enum class UserRole(val roleKey: String, val titleAr: String, val emoji: String) {
    SUPER_ADMIN("super_admin", "المشرف العام (سوبر أدمن)", "👑"),
    ADMIN("admin", "أدمن", "🛡️"),
    /** «مدير إدارة الحفلات والامتحانات»: staffs observers and ceremony sheikhs, activates committees. */
    EXAMS_MANAGER("exams_manager", "مدير إدارة الحفلات والامتحانات", "🗂️"),
    COMMITTEE_HEAD("committee_head", "شيخ لجنة", "👨‍🏫"),
    SHEIKH("sheikh", "شيخ", "👤");

    companion object {
        /** Older role names (teacher, assistant, auditor) became sheikh on the server. */
        fun fromKey(key: String): UserRole {
            return entries.find { it.roleKey.equals(key, ignoreCase = true) } ?: SHEIKH
        }
    }
}

object SystemPermissions {
    const val DATABASE_SHEETS = "database_sheets"
    const val MANAGE_COMMITTEES = "manage_committees"
    const val ADD_NEW_COMMITTEE = "add_new_committee"
    const val EXAMS_PDF = "exams_pdf"
    const val MANAGE_COURSES = "manage_courses"
    const val CALENDAR_SCHEDULE = "calendar_schedule"
    const val PROGRESS_STATS = "progress_stats"
    const val MANAGE_ASSISTANTS = "manage_assistants"

    val ALL_PERMISSIONS = listOf(
        DATABASE_SHEETS to "قاعدة البيانات والكشوفات",
        MANAGE_COMMITTEES to "إدارة اللجان والمراحل التسع",
        ADD_NEW_COMMITTEE to "إضافة وتعديل اللجان",
        EXAMS_PDF to "الامتحانات ونماذج الـ PDF",
        MANAGE_COURSES to "إدارة وإضافة الدورات",
        CALENDAR_SCHEDULE to "الجدول وتقويم المواعيد",
        PROGRESS_STATS to "حساب ونسبة الإنجاز والإحصائيات",
        MANAGE_ASSISTANTS to "قبول طلبات الانضمام وتعديل بيانات الشيوخ"
    )
}

data class AppUser(
    val id: String,
    val name: String,
    val phone: String,
    val email: String = "",
    val passportNumber: String = "",
    val role: UserRole = UserRole.SHEIKH,
    val status: String = "approved", // pending, approved, rejected
    /** Committee numbers, for display only (numbers repeat across courses). */
    val assignedCommittees: List<Int> = emptyList(),
    /** Assigned committees by id: what the server checks. */
    val assignedCommitteeIds: List<String> = emptyList(),
    val allCommitteesAllowed: Boolean = false,
    /** After an admin password reset the user must choose a new password first. */
    val mustChangePassword: Boolean = false,
    val permissions: List<String> = emptyList(),
    val createdAt: String = "",
    val approvedAt: String? = null,
    val approvedBy: String? = null,
    val notes: String = "",
    /** Server version; sent back on save so a stale edit is refused. */
    val updatedAt: String? = null
) {
    /** A permission an admin granted to this user (super admins and admins have all). */
    fun hasPermission(permissionId: String): Boolean =
        role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN || permissions.contains(permissionId)

    /**
     * Which screens to show: the same rules as the web app's tabs. Actions inside a screen
     * are offered only when the server allows them ([com.example.data.remote.Permissions]).
     */
    fun canAccessScreen(screen: AppNavScreen): Boolean {
        if (role == UserRole.SUPER_ADMIN) return true
        val p = com.example.data.remote.Permissions
        return when (screen) {
            AppNavScreen.HOME, AppNavScreen.CEREMONIES, AppNavScreen.DAILY_ALERTS, AppNavScreen.COMMITTEES -> true
            AppNavScreen.COURSE_TRACKER -> p.can("summerRows.read")
            AppNavScreen.EXAMS -> p.can("exams.write") || p.can("exams.file")
            AppNavScreen.SCHEDULE -> p.can("tasks.read")
            AppNavScreen.ASSISTANTS -> p.can("users.read")
            AppNavScreen.STATISTICS -> p.can("stats.read")
            AppNavScreen.COURSES -> p.can("courses.write")
            AppNavScreen.AUDIT -> p.can("audit.read")
            // Device sync state, notifications and the menu are for everyone.
            AppNavScreen.DATABASE_INSPECTOR, AppNavScreen.NOTIFICATIONS, AppNavScreen.MENU -> true
        }
    }
}
