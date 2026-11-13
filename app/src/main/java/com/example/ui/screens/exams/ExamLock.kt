package com.example.ui.screens.exams

import com.example.data.model.Exam
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** The exam paper's lock line, like the web's exams screen (times shown in Cairo time). */
object ExamLock {
    private fun parse(iso: String): java.util.Date? = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX").firstNotNullOfOrNull { f ->
        runCatching { SimpleDateFormat(f, Locale.US).apply { isLenient = false }.parse(iso) }.getOrNull()
    }

    fun cairo(iso: String): String {
        val d = parse(iso) ?: return iso
        return SimpleDateFormat("EEEE d MMMM، h:mm a", Locale("ar")).apply { timeZone = TimeZone.getTimeZone("Africa/Cairo") }.format(d)
    }

    /** (locked, text): «🔒 مقفول — تُتاح الورقة …» while closed, «📅 موعد الامتحان …» once a time is set; null otherwise. */
    fun text(exam: Exam): Pair<Boolean, String>? = when {
        !exam.lockedUntil.isNullOrBlank() -> true to "🔒 مقفول — تُتاح الورقة ${cairo(exam.lockedUntil)} (بتوقيت القاهرة)"
        !exam.opensAt.isNullOrBlank() -> false to "📅 موعد الامتحان ${cairo(exam.opensAt)} (بتوقيت القاهرة)"
        else -> null
    }
}
