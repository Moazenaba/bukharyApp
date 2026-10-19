package com.example.data.remote

import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

/**
 * Management calls that, like on the web, need the server to confirm before anything is shown as
 * saved: courses, books, user accounts, permission profiles, notifications, the audit log, the
 * course sheet and table extraction. Field work (stages, reception, scoring, tasks) keeps going
 * through the offline queue in [com.example.data.repository.BukhariRepository].
 */
object OnlineApi {

    /** A failed call: the server's message, or a connection message when it could not be reached. */
    class ApiFailure(message: String, val httpCode: Int?) : Exception(message)

    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter = ApiClient.moshi.adapter<Map<String, Any?>>(mapType)

    private fun enc(id: String): String = URLEncoder.encode(id, "UTF-8").replace("+", "%20")

    private fun toJson(body: Any?): String? = body?.let { ApiClient.moshi.adapter(Any::class.java).toJson(it) }

    private suspend fun call(method: String, path: String, body: Any? = null): String = withContext(Dispatchers.IO) {
        val res = ApiClient.send(method, path, toJson(body))
        when {
            res.code == null -> throw ApiFailure("تعذر الاتصال بالخادم؛ تحقق من الإنترنت ثم أعد المحاولة", null)
            res.code == 401 -> {
                ApiClient.onUnauthorized?.invoke()
                throw ApiFailure("انتهت الجلسة، يرجى تسجيل الدخول مرة أخرى", 401)
            }
            res.code !in 200..299 -> throw ApiFailure(res.message ?: "رفض الخادم الطلب (HTTP ${res.code})", res.code)
            else -> res.body.orEmpty()
        }
    }

    private suspend fun <T> get(path: String, key: String, element: Class<T>): List<T> {
        val text = call("GET", path)
        val type = Types.newParameterizedType(List::class.java, element)
        val map = mapAdapter.fromJson(text) ?: return emptyList()
        val raw = map[key] ?: return emptyList()
        return ApiClient.moshi.adapter<List<T>>(type).fromJsonValue(raw) ?: emptyList()
    }

    private suspend fun <T> one(method: String, path: String, body: Any?, key: String, cls: Class<T>): T? {
        val text = call(method, path, body)
        val raw = mapAdapter.fromJson(text)?.get(key) ?: return null
        return ApiClient.moshi.adapter(cls).fromJsonValue(raw)
    }

    // ---------------------------------------------------------------- courses

    suspend fun saveCourse(course: Map<String, Any?>) { call("POST", "api/courses", course) }

    /** A course with sheet rows answers 409 with a question; [deleteSheetRows] confirms it. */
    suspend fun deleteCourse(id: String, deleteSheetRows: Boolean = false) {
        call("DELETE", "api/courses/${enc(id)}" + if (deleteSheetRows) "?deleteSheetRows=true" else "")
    }

    // ---------------------------------------------------------------- books

    suspend fun books(): List<BookDto> = get("api/books", "books", BookDto::class.java)
    suspend fun saveBook(name: String): BookDto? =
        one("POST", "api/books", mapOf("id" to "book-${java.util.UUID.randomUUID()}", "name" to name.trim()), "book", BookDto::class.java)
    suspend fun deleteBook(id: String) { call("DELETE", "api/books/${enc(id)}") }

    // ---------------------------------------------------------------- exams

    /** Uploads (or replaces) an exam's PDF. */
    suspend fun uploadExamFile(examId: String, bytes: ByteArray, fileName: String) = withContext(Dispatchers.IO) {
        val res = ApiClient.sendBytes(
            "PUT", "api/exams/${enc(examId)}/file", bytes, "application/pdf",
            mapOf("X-File-Name" to enc(fileName))
        )
        when {
            res.code == null -> throw ApiFailure("تعذر الاتصال بالخادم لرفع الملف", null)
            res.code !in 200..299 -> throw ApiFailure(res.message ?: "تعذر رفع الملف (HTTP ${res.code})", res.code)
        }
    }

    suspend fun deleteExamFile(examId: String) { call("DELETE", "api/exams/${enc(examId)}/file") }

    // ---------------------------------------------------------------- users

    suspend fun users(): List<StaffUserDto> = get("api/users", "users", StaffUserDto::class.java)

    /** POST /api/users with the identity fields plus only what changed (WEB-03). */
    suspend fun saveUser(body: Map<String, Any?>): StaffUserDto? = one("POST", "api/users", body, "user", StaffUserDto::class.java)

    suspend fun deleteUser(id: String) { call("DELETE", "api/users/${enc(id)}") }

    suspend fun resetPassword(id: String, temporaryPassword: String) {
        call("POST", "api/users/${enc(id)}/reset-password", mapOf("password" to temporaryPassword))
    }

    // ---------------------------------------------------------------- permission profiles

    suspend fun profiles(): List<PermissionProfileDto> = get("api/permission-profiles", "profiles", PermissionProfileDto::class.java)
    suspend fun saveProfile(p: PermissionProfileDto) { call("POST", "api/permission-profiles", p) }
    suspend fun deleteProfile(id: String) { call("DELETE", "api/permission-profiles/${enc(id)}") }

    // ---------------------------------------------------------------- notifications

    suspend fun sendNotification(title: String, description: String, type: String, committeeId: String?) {
        call("POST", "api/notifications", mapOf("title" to title, "description" to description, "type" to type, "committeeId" to committeeId))
    }

    suspend fun markAllNotificationsRead() { call("POST", "api/notifications/read-all") }

    /** Registers this phone for push notifications (FCM). */
    suspend fun registerDevice(token: String) {
        call("POST", "api/devices", mapOf("token" to token, "platform" to "android"))
    }

    suspend fun unregisterDevice(token: String) {
        call("POST", "api/devices/unregister", mapOf("token" to token))
    }

    // ---------------------------------------------------------------- audit log

    suspend fun auditLogs(entity: String?, before: String?, limit: Int = 50): List<AuditLogDto> {
        val q = buildList {
            add("limit=$limit")
            if (!entity.isNullOrBlank()) add("entity=${enc(entity)}")
            if (!before.isNullOrBlank()) add("before=${enc(before)}")
        }.joinToString("&")
        return get("api/audit-logs?$q", "logs", AuditLogDto::class.java)
    }

    // ---------------------------------------------------------------- course sheet

    suspend fun sheetRows(courseId: String?): List<SheetRowDto> =
        get("api/summer-course-rows" + (courseId?.let { "?courseId=${enc(it)}" } ?: ""), "rows", SheetRowDto::class.java)

    suspend fun saveSheetRow(row: SheetRowDto): SheetRowDto? = one("POST", "api/summer-course-rows", row, "row", SheetRowDto::class.java)

    suspend fun saveSheetRowsBulk(rows: List<SheetRowDto>, replaceCourseId: String?) {
        call("POST", "api/summer-course-rows/bulk", mapOf("rows" to rows, "replaceCourseId" to replaceCourseId))
    }

    suspend fun deleteSheetRow(id: String) { call("DELETE", "api/summer-course-rows/${enc(id)}") }

    /** Reads a table from a photo of a paper sheet (Gemini on the server). */
    suspend fun extractTable(imageBase64: String, mimeType: String): List<Map<String, Any?>> {
        val text = call("POST", "api/extract-table", mapOf("imageBase64" to imageBase64, "mimeType" to mimeType))
        val data = mapAdapter.fromJson(text)?.get("data") as? Map<*, *> ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        return (data["rows"] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList()
    }

    // ---------------------------------------------------------------- committees

    /** Base-data save, used when a committee's course is changed from the sheet. */
    suspend fun saveCommitteesBulk(committees: List<Map<String, Any?>>) {
        call("POST", "api/committees/bulk", mapOf("committees" to committees))
    }
}

data class BookDto(val id: String, val name: String = "", val createdAt: String? = null)

/** A user as the web's user management sees it (GET /api/users), with the join-request dossier. */
data class StaffUserDto(
    val id: String,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val role: String = "assistant",
    val status: String = "pending",
    val assignedCommittees: List<Int> = emptyList(),
    val assignedCommitteeIds: List<String> = emptyList(),
    val allCommitteesAllowed: Boolean = false,
    val allExamsAllowed: Boolean = false,
    val allowedExamIds: List<String> = emptyList(),
    val profileId: String? = null,
    val mustChangePassword: Boolean = false,
    val permissions: List<String> = emptyList(),
    val createdAt: String = "",
    val approvedAt: String? = null,
    val approvedBy: String? = null,
    val notes: String? = null,
    val requestCode: String? = null,
    val updatedAt: String? = null,
    val passportNumber: String? = null,
    val whatsapp: String? = null,
    val nationalId: String? = null,
    val quranLevel: String? = null,
    val qualification: String? = null,
    val experience: String? = null,
    val availability: String? = null,
    val preferredCommittees: List<Int>? = null,
    val interviewDate: String? = null,
    val interviewNotes: String? = null,
    val rejectionReason: String? = null,
    val rejectedAt: String? = null,
    val rejectedBy: String? = null
)

data class PermissionProfileDto(
    val id: String,
    val name: String = "",
    val description: String = "",
    val badgeColor: String? = null,
    val allowedCommittees: List<Int> = emptyList(),
    val allowedCommitteeIds: List<String> = emptyList(),
    val allCommitteesAllowed: Boolean = false,
    val allowedExamIds: List<String> = emptyList(),
    val allExamsAllowed: Boolean = false,
    val permissions: List<String> = emptyList(),
    val isDefault: Boolean = false,
    val createdBy: String? = null,
    val createdAt: String? = null
)

data class AuditLogDto(
    val id: String,
    val timestamp: String = "",
    val user: String? = null,
    val role: String? = null,
    val actorId: String? = null,
    val action: String = "",
    val details: String = "",
    val status: String = "",
    val entity: String? = null,
    val entityId: String? = null
)
