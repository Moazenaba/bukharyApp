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
    class ApiFailure(message: String, val httpCode: Int?, val errorCode: String? = null, val errorBody: String? = null) : Exception(message)

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
            res.code !in 200..299 -> throw ApiFailure(res.message ?: "رفض الخادم الطلب (HTTP ${res.code})", res.code, res.errorCode, res.errorBody)
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

    // ------------------------------------------------- committee flow (docs/PLAN_ROLES_AND_FLOW.md on the server)

    /** A JSON object as a map (numbers are Doubles). */
    fun parse(text: String?): Map<String, Any?> = text?.let { mapAdapter.fromJson(it) } ?: emptyMap()

    /** The people on a committee as the server lets this person see them, with the committee's step. */
    suspend fun committeePeople(committeeId: String): Map<String, Any?> = parse(call("GET", "api/committees/${enc(committeeId)}/people"))

    suspend fun lifecycleStep(committeeId: String, to: String) { call("POST", "api/committees/${enc(committeeId)}/lifecycle", mapOf("to" to to)) }

    /** Adds an observer or a ceremony sheikh; a same-day conflict fails with errorCode "schedule_conflict" until confirmed. */
    suspend fun addStaff(committeeId: String, userId: String, role: String, note: String?, confirmConflicts: Boolean) {
        call(
            "POST", "api/committees/${enc(committeeId)}/staff",
            buildMap<String, Any?> {
                put("userId", userId); put("role", role); put("confirmConflicts", confirmConflicts)
                if (!note.isNullOrBlank()) put("note", note)
            }
        )
    }

    suspend fun removeStaff(committeeId: String, staffId: String) { call("DELETE", "api/committees/${enc(committeeId)}/staff/${enc(staffId)}") }

    suspend fun excuse(committeeId: String, staffId: String, reason: String) {
        call("POST", "api/committees/${enc(committeeId)}/staff/${enc(staffId)}/excuse", mapOf("reason" to reason))
    }

    suspend fun setCanAssign(committeeId: String, value: Boolean) { call("PATCH", "api/committees/${enc(committeeId)}/owner/can-assign", mapOf("value" to value)) }

    suspend fun setDriver(committeeId: String, hasDriver: Boolean, name: String, phone: String) {
        call("PATCH", "api/committees/${enc(committeeId)}/driver", mapOf("hasDriver" to hasDriver, "name" to name, "phone" to phone))
    }

    suspend fun saveCeremony(committeeId: String, date: String, time: String, place: String) {
        call("POST", "api/committees/${enc(committeeId)}/ceremony", mapOf("ceremonyDate" to date, "ceremonyTime" to time, "ceremonyPlace" to place))
    }

    /** People who may be placed, with what keeps them busy on [date] (admins and the exams manager). */
    suspend fun availability(date: String): Map<String, Any?> = parse(call("GET", "api/people/availability?date=${enc(date)}"))

    // ------------------------------------------------- extra assignments («مراقب»)

    suspend fun assignments(committeeId: String? = null, userId: String? = null): List<com.example.data.model.CommitteeAssignment> {
        val query = listOfNotNull(committeeId?.let { "committeeId=${enc(it)}" }, userId?.let { "userId=${enc(it)}" }).joinToString("&")
        return get("api/assignments" + if (query.isEmpty()) "" else "?$query", "assignments", com.example.data.model.CommitteeAssignment::class.java)
    }

    suspend fun createAssignment(userId: String, committeeId: String, task: String, endsOn: String?): com.example.data.model.CommitteeAssignment? =
        one(
            "POST", "api/assignments",
            buildMap<String, Any?> {
                put("userId", userId); put("committeeId", committeeId); put("task", task)
                if (!endsOn.isNullOrBlank()) put("endsOn", endsOn)
            },
            "assignment", com.example.data.model.CommitteeAssignment::class.java
        )

    /** `how` is "end" (a normal finish) or "cancel"; both stop the powers at once and keep the history. */
    suspend fun finishAssignment(id: String, how: String) { call("POST", "api/assignments/${enc(id)}/$how") }

    suspend fun observations(assignmentId: String): List<com.example.data.model.AssignmentNote> =
        get("api/assignments/${enc(assignmentId)}/observations", "observations", com.example.data.model.AssignmentNote::class.java)

    suspend fun addObservation(assignmentId: String, note: String) { call("POST", "api/assignments/${enc(assignmentId)}/observations", mapOf("note" to note)) }

    // ------------------------------------------------- temporary heads («شيخ اللجنة»)

    /** The grants this person may see: all for an admin, the ones on their committees for a head, their own otherwise. */
    suspend fun headGrants(): List<com.example.data.model.HeadGrant> =
        get("api/grants", "grants", com.example.data.model.HeadGrant::class.java)

    /** "yes", "no" or "undecided". The server refuses a decision from anyone who may not make it. */
    suspend fun decideHeadGrant(id: String, decision: String): com.example.data.model.HeadGrant? =
        one("POST", "api/grants/${enc(id)}/decision", mapOf("decision" to decision), "grant", com.example.data.model.HeadGrant::class.java)

    // ---------------------------------------------------------------- courses

    /** Short fingerprint of all server data (GET /api/sync/state); null on an older server. */
    /** The server's data fingerprint, whole and per list ([parts] is null on an older server). */
    data class SyncState(val version: String?, val parts: Map<String, String>?)

    /** GET /api/sync/state: a few hundred bytes; null when the server could not be reached. */
    suspend fun syncState(): SyncState? = runCatching {
        val map = mapAdapter.fromJson(call("GET", "api/sync/state"))
        SyncState(
            map?.get("version") as? String,
            (map?.get("parts") as? Map<*, *>)?.entries?.associate { (k, v) -> k.toString() to v.toString() }
        )
    }.getOrNull()

    suspend fun saveCourse(course: Map<String, Any?>) { call("POST", "api/courses", course) }

    /** A course with sheet rows answers 409 with a question; [deleteSheetRows] confirms it. */
    suspend fun deleteCourse(id: String, deleteSheetRows: Boolean = false) {
        call("DELETE", "api/courses/${enc(id)}" + if (deleteSheetRows) "?deleteSheetRows=true" else "")
    }

    // ---------------------------------------------------------------- settings

    /** What admins set for everyone on the website («إعدادات النظام»). */
    suspend fun settings(): AppSettings {
        val raw = mapAdapter.fromJson(call("GET", "api/settings"))?.get("settings") as? Map<*, *> ?: return AppSettings()
        return AppSettings.from(raw)
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

    /** Approved accounts that can be invited to a ceremony. */
    suspend fun ceremonyCandidates(): List<AssignableUserDto> = get("api/users/ceremony-candidates", "users", AssignableUserDto::class.java)

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

    /** A message to a chosen audience (the server's `audience`: all / staffRole / users...). Returns how many people it reached. */
    suspend fun sendNotificationTo(title: String, description: String, audience: Map<String, Any?>, type: String = "info", committeeId: String? = null) {
        call("POST", "api/notifications", mapOf("title" to title, "description" to description, "type" to type, "committeeId" to committeeId, "audience" to audience))
    }

    /** How many people a choice of recipients reaches (POST /api/notifications/preview), before sending. */
    suspend fun previewAudience(audience: Map<String, Any?>): Int? =
        (mapAdapter.fromJson(call("POST", "api/notifications/preview", mapOf("audience" to audience)))?.get("recipients") as? Number)?.toInt()

    /** A message sent by hand and how many of its recipients opened it (GET /api/notifications/sent). */
    data class SentNotice(val batchId: String = "", val title: String = "", val description: String = "", val type: String = "info",
                          val createdAt: String = "", val recipients: Int = 0, val read: Int = 0, val sentBy: String = "")

    suspend fun sentNotices(): List<SentNotice> = get("api/notifications/sent", "sent", SentNotice::class.java)

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
    val role: String = "sheikh",
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
    val whatsapp: String? = null,
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
