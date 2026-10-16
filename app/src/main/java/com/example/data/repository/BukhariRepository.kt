package com.example.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AppUserEntity
import com.example.data.local.entities.CommitteeEntity
import com.example.data.local.entities.CustomPermissionEntity
import com.example.data.local.entities.ExamEntity
import com.example.data.local.entities.PendingOpEntity
import com.example.data.local.entities.ScheduleTaskEntity
import com.example.data.model.AggregationDetails
import com.example.data.model.AppUser
import com.example.data.model.CeremonyDetails
import com.example.data.model.Committee
import com.example.data.model.Course
import com.example.data.model.CustomPermission
import com.example.data.model.DatabaseStatus
import com.example.data.model.DatabaseTableCount
import com.example.data.model.Exam
import com.example.data.model.FailedWrite
import com.example.data.model.NotificationItem
import com.example.data.model.ScheduleTask
import com.example.data.model.ScoringDetails
import com.example.data.model.ServerDatabaseInfo
import com.example.data.model.StageItem
import com.example.data.model.UserRole
import com.example.data.remote.ApiClient
import com.example.data.remote.ApiService
import com.example.data.remote.Permissions
import com.example.data.remote.SheetRowDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Outcome of a write as far as the server is concerned. */
sealed interface WriteResult {
    /** The server accepted it. */
    data object Sent : WriteResult

    /** Saved on the phone; will be sent when the server is reachable. */
    data object Queued : WriteResult

    /** The server refused it (no permission, invalid, conflict). The local change is undone by the next sync. */
    data class Rejected(val message: String) : WriteResult
}

data class SyncResult(val online: Boolean, val message: String)

/**
 * Local-first data layer: reads come from Room (filled from the server by [syncWithRemoteApi]);
 * writes are applied to Room, queued in `pending_ops`, and sent to the server in order.
 */
/** Sends one request. [ApiClient] in the app; a scripted fake in tests. */
fun interface RequestSender {
    fun send(method: String, path: String, bodyJson: String?): ApiClient.SendResult
}

class BukhariRepository(
    private val db: AppDatabase,
    private val api: ApiService = ApiClient.apiService,
    private val sender: RequestSender = RequestSender { method, path, body -> ApiClient.send(method, path, body) }
) {
    private val tag = "BukhariRepository"
    private val queueMutex = Mutex()
    private var lastSyncAt: Long? = null
    private var lastSyncOnline = false

    val allCourses: Flow<List<Course>> = db.courseDao().getAllCourses().map { list -> list.map { it.toDomain() } }
    val allCommittees: Flow<List<Committee>> = db.committeeDao().getAllCommittees().map { list -> list.map { it.toDomain() } }
    val allUsers: Flow<List<AppUser>> = db.appUserDao().getAllUsers().map { list -> list.map { it.toDomain() } }
    val allExams: Flow<List<Exam>> = db.examDao().getAllExams().map { list -> list.map { it.toDomain() } }
    val allTasks: Flow<List<ScheduleTask>> = db.scheduleTaskDao().getAllTasks().map { list -> list.map { it.toDomain() } }
    val allNotifications: Flow<List<NotificationItem>> =
        db.notificationDao().getAllNotifications().map { list -> list.map { it.toDomain() } }
    val allCustomPermissions: Flow<List<CustomPermission>> =
        db.customPermissionDao().getAllCustomPermissions().map { list -> list.map { it.toDomain() } }

    /** Saved course-sheet rows from the server (read-only on mobile; kept in memory). */
    private val _sheetRows = MutableStateFlow<List<SheetRowDto>>(emptyList())
    val sheetRows: StateFlow<List<SheetRowDto>> = _sheetRows.asStateFlow()

    // ------------------------------------------------------------------ sync

    /**
     * Sends queued writes, then — if none are still waiting — replaces each local table
     * with the server's copy, so edits and deletions from the web show up here. A table
     * the user may not read is left as it is.
     */
    suspend fun syncWithRemoteApi(): SyncResult = withContext(Dispatchers.IO) {
        if (!ApiClient.isConfigured) return@withContext SyncResult(false, "لم يتم ضبط عنوان الخادم")
        val waiting = flushPendingOps().waiting
        if (waiting > 0) {
            lastSyncOnline = false
            return@withContext SyncResult(false, "توجد $waiting تعديلات بانتظار الإرسال؛ سيتم التحديث بعد إرسالها")
        }
        try {
            val coursesRes = api.getCourses()
            if (coursesRes.code() == 401) {
                ApiClient.onUnauthorized?.invoke()
                return@withContext SyncResult(false, "انتهت الجلسة")
            }
            if (!coursesRes.isSuccessful) {
                lastSyncOnline = false
                return@withContext SyncResult(false, "تعذر التحديث من الخادم (HTTP ${coursesRes.code()})")
            }
            val courses = coursesRes.body()?.courses
            val committees = api.getCommittees().bodyIfOk()?.committees
            val users = if (Permissions.can("users.read")) api.getUsers().bodyIfOk()?.users else null
            val exams = api.getExams().bodyIfOk()?.exams
            val tasks = api.getTasks().bodyIfOk()?.tasks
            val notifications = api.getNotifications().bodyIfOk()?.notifications
            val customPermissions = api.getCustomPermissions().bodyIfOk()?.permissions
            api.getSheetRows().bodyIfOk()?.rows?.let { _sheetRows.value = it }

            db.withTransaction {
                courses?.let { db.courseDao().clearAll(); db.courseDao().insertCourses(it) }
                committees?.let { db.committeeDao().clearAll(); db.committeeDao().insertCommittees(it) }
                users?.let { db.appUserDao().clearAll(); db.appUserDao().insertUsers(it) }
                exams?.let { db.examDao().clearAll(); db.examDao().insertExams(it) }
                tasks?.let { db.scheduleTaskDao().clearAll(); db.scheduleTaskDao().insertTasks(it) }
                notifications?.let { db.notificationDao().clearAll(); db.notificationDao().insertNotifications(it) }
                customPermissions?.let { list ->
                    db.customPermissionDao().clearAll()
                    db.customPermissionDao().insertAll(list.map { CustomPermissionEntity(it.id, it.label, it.description) })
                }
            }
            lastSyncAt = System.currentTimeMillis()
            lastSyncOnline = true
            SyncResult(true, "تم التحديث من الخادم")
        } catch (e: Exception) {
            Log.w(tag, "Sync failed: ${e.message}")
            lastSyncOnline = false
            SyncResult(false, "تعذر الاتصال بالخادم؛ تعرض آخر بيانات محفوظة على الجهاز")
        }
    }

    private fun <T> Response<T>.bodyIfOk(): T? = if (isSuccessful) body() else null

    // ------------------------------------------------------------------ write queue

    data class FlushReport(val outcomes: Map<Long, WriteResult>, val waiting: Int)

    private fun enc(id: String): String = URLEncoder.encode(id, "UTF-8").replace("+", "%20")

    /** Queues a write and tries to deliver it (and anything queued before it) right away. */
    private suspend fun push(label: String, method: String, path: String, body: Any? = null, versionOf: String? = null): WriteResult {
        val json = body?.let { ApiClient.moshi.adapter(Any::class.java).toJson(it) }
        val id = db.pendingOpDao().insert(PendingOpEntity(label = label, method = method, path = path, bodyJson = json, versionOf = versionOf))
        return flushPendingOps().outcomes[id] ?: WriteResult.Queued
    }

    /**
     * Sends queued writes in order. Stops when the server is unreachable or the session
     * expired. A write that keeps failing with server errors is parked after [MAX_ATTEMPTS]
     * so it does not block everything behind it; refused writes are dropped and reported.
     */
    suspend fun flushPendingOps(): FlushReport = withContext(Dispatchers.IO) {
        queueMutex.withLock {
            val outcomes = mutableMapOf<Long, WriteResult>()
            for (op in db.pendingOpDao().getActive()) {
                val res = sender.send(op.method, op.path, bodyWithVersion(op))
                val code = res.code
                when {
                    code == null -> break
                    code in 200..299 -> {
                        db.pendingOpDao().deleteById(op.id)
                        outcomes[op.id] = WriteResult.Sent
                        storeServerVersion(res.body)
                    }
                    code == 401 -> {
                        ApiClient.onUnauthorized?.invoke()
                        break
                    }
                    code >= 500 || code == 429 -> {
                        val attempts = op.attempts + 1
                        val park = attempts >= MAX_ATTEMPTS
                        db.pendingOpDao().recordFailure(op.id, attempts, park, res.message ?: "HTTP $code")
                        if (!park) break
                    }
                    else -> {
                        Log.w(tag, "${op.label} refused by server: HTTP $code ${res.message}")
                        db.pendingOpDao().deleteById(op.id)
                        outcomes[op.id] = WriteResult.Rejected(res.message ?: "رفض الخادم هذا التعديل")
                    }
                }
            }
            FlushReport(outcomes, db.pendingOpDao().countActive())
        }
    }

    /** The body to send: adds the record's current server version when the write is versioned. */
    private suspend fun bodyWithVersion(op: PendingOpEntity): String? {
        val key = op.versionOf ?: return op.bodyJson
        val updatedAt = currentVersion(key) ?: return op.bodyJson
        return WriteBody.withExpectedUpdatedAt(op.bodyJson, updatedAt) ?: op.bodyJson
    }

    private suspend fun currentVersion(key: String): String? {
        val (kind, id) = key.split(":", limit = 2).takeIf { it.size == 2 } ?: return null
        return when (kind) {
            "committee" -> db.committeeDao().getCommitteeById(id)?.updatedAt
            "user" -> db.appUserDao().getUserById(id)?.updatedAt
            else -> null
        }
    }

    /** Keeps the local copy's version in step with the server, so the next write is not refused as stale. */
    private suspend fun storeServerVersion(responseBody: String?) {
        val v = WriteBody.serverVersion(responseBody) ?: return
        when (v.kind) {
            "committee" -> db.committeeDao().setUpdatedAt(v.id, v.updatedAt)
            "user" -> db.appUserDao().setUpdatedAt(v.id, v.updatedAt)
        }
    }

    suspend fun retryFailedWrites(): FlushReport = withContext(Dispatchers.IO) {
        db.pendingOpDao().unparkAll()
        flushPendingOps()
    }

    suspend fun discardFailedWrites() = withContext(Dispatchers.IO) { db.pendingOpDao().deleteFailed() }

    /** Removes everything this user had on the device (logout, expired session). */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        _sheetRows.value = emptyList()
        lastSyncAt = null
        lastSyncOnline = false
    }

    private fun now(): String = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")).format(Date())

    // ------------------------------------------------------------------ committees

    suspend fun saveCommittee(committee: Committee): WriteResult = withContext(Dispatchers.IO) {
        val entity = CommitteeEntity.fromDomain(committee)
        db.committeeDao().insertCommittee(entity)
        push("saveCommittee", "POST", "api/committees", entity, versionOf = "committee:${entity.id}")
    }

    /** Sends only the stage that changed; the server merges stages per key. */
    suspend fun updateStage(committeeId: String, stageKey: String, completed: Boolean): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
        val updatedStages = entity.stages.updateStage(stageKey, completed, if (completed) now() else null)
        db.committeeDao().updateCommittee(entity.copy(stages = updatedStages))
        push("updateStage", "PATCH", "api/committees/${enc(committeeId)}", mapOf("stages" to mapOf(stageKey to updatedStages.getStage(stageKey))))
    }

    /** The server records who received the committee from the session; only notes are sent. */
    suspend fun recordAssistantHandover(committeeId: String, me: AppUser, phone: String): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
        val stamp = now()
        val reception = com.example.data.model.AssistantReception(assistantId = me.id, receivedByName = me.name, receivedAt = stamp, phone = phone)
        val stages = mapOf(
            "registration" to entity.stages.registration.copy(completed = true, completedAt = entity.stages.registration.completedAt ?: stamp),
            "exam" to entity.stages.exam.copy(completed = true, completedAt = entity.stages.exam.completedAt ?: stamp)
        )
        db.committeeDao().updateCommittee(
            entity.copy(receivedByAssistant = reception, stages = entity.stages.copy(registration = stages.getValue("registration"), exam = stages.getValue("exam")))
        )
        push(
            "recordAssistantHandover", "PATCH", "api/committees/${enc(committeeId)}",
            mapOf("receivedByAssistant" to mapOf("status" to "received", "phone" to phone.ifBlank { null }), "stages" to stages)
        )
    }

    suspend fun approveExamReadiness(committeeId: String, me: AppUser): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
        val approval = com.example.data.model.ExamApproval(assistantId = me.id, approvedByName = me.name, approvedAt = now())
        db.committeeDao().updateCommittee(entity.copy(examApprovedBy = approval))
        push("approveExamReadiness", "PATCH", "api/committees/${enc(committeeId)}", mapOf("examApprovedBy" to emptyMap<String, Any>()))
    }

    /** Scoring stage is complete exactly when every question is corrected (same rule as the web). */
    suspend fun updateScoring(committeeId: String, scoring: ScoringDetails): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
        val allDone = scoring.questions.isNotEmpty() && scoring.questions.all { it.completed }
        val patch = mutableMapOf<String, Any>("scoringDetails" to scoring)
        var stages = entity.stages
        if (allDone != entity.stages.scoring.completed) {
            stages = stages.updateStage("scoring", allDone, if (allDone) now() else null)
            patch["stages"] = mapOf("scoring" to stages.scoring)
        }
        db.committeeDao().updateCommittee(entity.copy(scoringDetails = scoring, stages = stages))
        push("updateScoring", "PATCH", "api/committees/${enc(committeeId)}", patch, versionOf = "committee:$committeeId")
    }

    suspend fun updateAggregation(committeeId: String, aggregation: AggregationDetails): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
        val patch = mutableMapOf<String, Any>("aggregationDetails" to aggregation)
        var stages = entity.stages
        if (aggregation.isConfirmedMatch != entity.stages.aggregation.completed) {
            stages = stages.updateStage("aggregation", aggregation.isConfirmedMatch, if (aggregation.isConfirmedMatch) now() else null)
            patch["stages"] = mapOf("aggregation" to stages.aggregation)
        }
        db.committeeDao().updateCommittee(entity.copy(aggregationDetails = aggregation, stages = stages))
        push("updateAggregation", "PATCH", "api/committees/${enc(committeeId)}", patch, versionOf = "committee:$committeeId")
    }

    /**
     * Saves the ceremony details. The ceremony stage counts as done only once its date has
     * arrived; a future date just schedules it (and its calendar task).
     */
    suspend fun updateCeremony(committeeId: String, ceremony: CeremonyDetails): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val held = ceremony.date.isNotBlank() && ceremony.date <= today
        val patch = mutableMapOf<String, Any>("ceremonyDetails" to ceremony)
        var stages = entity.stages
        if (held != entity.stages.ceremony.completed) {
            stages = stages.updateStage("ceremony", held, if (held) "${ceremony.date} ${ceremony.time}".trim() else null)
            patch["stages"] = mapOf("ceremony" to stages.ceremony)
        }
        db.committeeDao().updateCommittee(entity.copy(ceremonyDetails = ceremony, stages = stages))
        val result = push("updateCeremony", "PATCH", "api/committees/${enc(committeeId)}", patch, versionOf = "committee:$committeeId")
        if (result !is WriteResult.Rejected && ceremony.date.isNotBlank() && Permissions.can("tasks.write")) {
            saveTask(
                ScheduleTask(
                    // Same id the web app uses, so both update one task.
                    id = "task-ceremony-${entity.id}",
                    title = "حفل تكريم لجنة #${entity.committeeNumber} (${entity.sheikhName.ifBlank { entity.committeeName }})",
                    date = ceremony.date,
                    time = ceremony.time,
                    committeeId = entity.id,
                    committeeNumber = entity.committeeNumber,
                    type = "ceremony",
                    priority = "urgent",
                    deliverable = listOfNotNull(
                        ceremony.honoreesCount.takeIf { it > 0 }?.let { "تكريم $it طالباً" },
                        ceremony.attendingSheikhs.takeIf { it.isNotEmpty() }?.let { "حضور: ${it.joinToString("، ")}" }
                    ).joinToString(" • "),
                    notes = listOfNotNull(ceremony.venue.ifBlank { null }?.let { "مكان الحفل: $it" }, ceremony.notes.ifBlank { null }).joinToString(". "),
                    completed = held
                )
            )
        }
        result
    }

    suspend fun deleteCommittee(committeeId: String): WriteResult = withContext(Dispatchers.IO) {
        db.committeeDao().deleteCommitteeById(committeeId)
        push("deleteCommittee", "DELETE", "api/committees/${enc(committeeId)}")
    }

    // ------------------------------------------------------------------ exams

    suspend fun saveExam(exam: Exam): WriteResult = withContext(Dispatchers.IO) {
        val entity = ExamEntity.fromDomain(exam)
        db.examDao().insertExam(entity)
        push("saveExam", "POST", "api/exams", entity)
    }

    suspend fun deleteExam(examId: String): WriteResult = withContext(Dispatchers.IO) {
        db.examDao().deleteExamById(examId)
        push("deleteExam", "DELETE", "api/exams/${enc(examId)}")
    }

    /** Downloads an exam's PDF (super admins and committee heads only). */
    suspend fun downloadExamFile(examId: String): ByteArray? = withContext(Dispatchers.IO) {
        ApiClient.download("api/exams/${enc(examId)}/file")
    }

    // ------------------------------------------------------------------ tasks

    suspend fun saveTask(task: ScheduleTask): WriteResult = withContext(Dispatchers.IO) {
        val entity = ScheduleTaskEntity.fromDomain(task)
        db.scheduleTaskDao().insertTask(entity)
        push("saveTask", "POST", "api/tasks", entity)
    }

    suspend fun toggleTask(taskId: String): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.scheduleTaskDao().getTaskById(taskId) ?: return@withContext WriteResult.Rejected("المهمة غير موجودة")
        val newStatus = !entity.completed
        db.scheduleTaskDao().updateTask(entity.copy(completed = newStatus, completedAt = if (newStatus) now() else null))
        push("toggleTask", "PATCH", "api/tasks/${enc(taskId)}/toggle", mapOf("completed" to newStatus))
    }

    suspend fun deleteTask(taskId: String): WriteResult = withContext(Dispatchers.IO) {
        db.scheduleTaskDao().deleteTaskById(taskId)
        push("deleteTask", "DELETE", "api/tasks/${enc(taskId)}")
    }

    // ------------------------------------------------------------------ users

    /** Stores the logged-in user locally so the session survives restarts (non-admins can't list users). */
    suspend fun cacheUser(user: AppUser) = withContext(Dispatchers.IO) {
        db.appUserDao().insertUser(AppUserEntity.fromDomain(user))
    }

    suspend fun getLocalUser(id: String): AppUser? = withContext(Dispatchers.IO) { db.appUserDao().getUserById(id)?.toDomain() }

    suspend fun approveUser(userId: String, assignedCommittees: List<Int>, permissions: List<String>, role: UserRole): WriteResult =
        withContext(Dispatchers.IO) {
            val entity = db.appUserDao().getUserById(userId) ?: return@withContext WriteResult.Rejected("المستخدم غير موجود")
            val updated = entity.copy(status = "approved", assignedCommittees = assignedCommittees, permissions = permissions, roleKey = role.roleKey, roleId = role.roleKey)
            db.appUserDao().updateUser(updated)
            push("approveUser", "POST", "api/users", updated, versionOf = "user:$userId")
        }

    /** Rejection keeps the account (status "rejected"), the same as on the web. */
    suspend fun rejectUser(userId: String, reason: String?): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.appUserDao().getUserById(userId) ?: return@withContext WriteResult.Rejected("المستخدم غير موجود")
        val updated = entity.copy(status = "rejected")
        db.appUserDao().updateUser(updated)
        val body = ApiClient.moshi.adapter(AppUserEntity::class.java).toJsonValue(updated) as Map<*, *>
        push("rejectUser", "POST", "api/users", body + ("rejectionReason" to reason?.ifBlank { null }), versionOf = "user:$userId")
    }

    // ------------------------------------------------------------------ settings & notifications

    suspend fun saveCustomPermission(name: String, description: String): WriteResult = withContext(Dispatchers.IO) {
        val entity = CustomPermissionEntity(id = "perm_${java.util.UUID.randomUUID()}", name = name.trim(), description = description.trim())
        db.customPermissionDao().insertCustomPermission(entity)
        push("saveCustomPermission", "POST", "api/custom-permissions", mapOf("id" to entity.id, "label" to entity.name, "description" to entity.description))
    }

    suspend fun markNotificationRead(id: String): WriteResult = withContext(Dispatchers.IO) {
        db.notificationDao().markAsRead(id)
        push("markNotificationRead", "PATCH", "api/notifications/${enc(id)}/read")
    }

    // ------------------------------------------------------------------ status

    /** Real numbers only: local row counts, the write queue, and (for super admins) the server's database. */
    suspend fun getDatabaseStatus(): DatabaseStatus = withContext(Dispatchers.IO) {
        val local = listOf(
            DatabaseTableCount("committees", "اللجان", db.committeeDao().getAllCommittees().first().size.toLong()),
            DatabaseTableCount("courses", "الدورات", db.courseDao().getAllCourses().first().size.toLong()),
            DatabaseTableCount("app_users", "المستخدمون", db.appUserDao().getAllUsers().first().size.toLong()),
            DatabaseTableCount("exams", "الامتحانات", db.examDao().getAllExams().first().size.toLong()),
            DatabaseTableCount("schedule_tasks", "المهام", db.scheduleTaskDao().getAllTasks().first().size.toLong()),
            DatabaseTableCount("notifications", "التنبيهات", db.notificationDao().getAllNotifications().first().size.toLong()),
        )
        val server = if (Permissions.can("database.admin")) {
            try {
                api.getDatabaseStatus().bodyIfOk()?.let { s ->
                    ServerDatabaseInfo(
                        database = s.database,
                        postgresVersion = s.postgresVersion,
                        latencyMs = s.latencyMs,
                        tls = s.tls,
                        pendingMigrations = s.migrations.pending,
                        tables = s.tables.map { DatabaseTableCount(it.name, it.name, it.count) }
                    )
                }
            } catch (e: Exception) {
                null
            }
        } else null
        DatabaseStatus(
            isOnline = lastSyncOnline,
            message = if (lastSyncOnline) "آخر تحديث من الخادم نجح" else "لم يتم التحديث من الخادم في هذه الجلسة",
            serverUrl = ApiClient.baseUrl,
            lastSyncAt = lastSyncAt,
            pendingWrites = db.pendingOpDao().countActive(),
            failedWrites = db.pendingOpDao().getFailed().map { FailedWrite(it.id, it.label, it.lastError) },
            localTables = local,
            server = server
        )
    }

    companion object {
        const val MAX_ATTEMPTS = 5
    }
}
