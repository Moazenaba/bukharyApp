package com.example.domain

/** Audit-log actions the way people say them. An action not listed shows as it is (never hidden). */
object AuditText {
    private val ACTIONS = mapOf(
        "committee.create" to "إنشاء لجنة", "committee.update" to "تعديل لجنة", "committee.patch" to "تعديل لجنة",
        "committee.delete" to "حذف لجنة", "committee.lifecycle" to "تغيير الحالة الإدارية للجنة",
        "committee.members" to "تعديل فريق اللجنة", "committee.driver" to "تعديل سائق اللجنة", "committee.action" to "تنفيذ خطوة في لجنة",
        "staff.add" to "إضافة شخص لفريق لجنة", "staff.remove" to "إزالة شخص من فريق لجنة", "staff.excuse" to "اعتذار عن تكليف",
        "observation.create" to "ملاحظة مراقبة", "assignment.create" to "تكليف مراقب",
        "grant.create" to "تكليف شيخ لجنة مؤقت", "grant.end" to "إنهاء تكليف شيخ لجنة مؤقت", "grant.decision" to "قرار في حفل",
        "user.create" to "إضافة مستخدم", "user.update" to "تعديل مستخدم", "user.delete" to "حذف مستخدم",
        "user.register" to "طلب انضمام", "user.change_password" to "تغيير كلمة مرور", "user.password_reset" to "إعادة تعيين كلمة مرور",
        "impersonation.start" to "دخول بعين مستخدم", "impersonation.end" to "إنهاء الدخول بعين مستخدم",
        "exam.save" to "حفظ امتحان", "exam.delete" to "حذف امتحان", "exam.file_upload" to "رفع ملف امتحان",
        "exam.file_delete" to "حذف ملف امتحان", "exam.file_download" to "تنزيل ملف امتحان",
        "task.save" to "حفظ مهمة", "task.toggle" to "تغيير حالة مهمة", "task.delete" to "حذف مهمة",
        "course.save" to "حفظ دورة", "course.delete" to "حذف دورة", "book.save" to "إضافة كتاب", "book.delete" to "حذف كتاب",
        "sheet_row.delete" to "حذف سطر من الكشف", "sheet_rows.replace" to "استبدال كشف الدورة",
        "notification.create" to "إرسال تنبيه", "notification.send" to "إرسال تنبيه",
        "profile.save" to "حفظ بروفايل صلاحيات", "profile.delete" to "حذف بروفايل صلاحيات",
        "custom_permission.save" to "حفظ صلاحية مخصصة", "custom_permission.delete" to "حذف صلاحية مخصصة",
        "settings.update" to "تعديل إعدادات النظام", "score.delete" to "حذف درجة"
    )

    fun action(code: String): String = ACTIONS[code] ?: code

    /** The «العنصر» filter of the log, as the server names its entities. */
    val ENTITIES = listOf(
        "" to "الكل", "committee" to "اللجان", "user" to "المستخدمون", "notification" to "التنبيهات", "exam" to "الامتحانات",
        "task" to "المهام", "course" to "الدورات", "auth" to "الدخول والجلسات"
    )
}
