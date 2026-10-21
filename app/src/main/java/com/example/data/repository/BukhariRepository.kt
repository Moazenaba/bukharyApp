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
    fun send(method: String, path: String, bodyJson: String?, idempotencyKey: String?): ApiClient.SendResult
}

/** A queued write the server refused, kept for the user to retry or discard (MOB-03). */
data class RefusedWrite(val id: Long, val label: String, val message: String?, val httpCode: Int?, val createdAt: Long)

class BukhariRepository(
    private val db: AppDatabase,
    private val api: ApiService = ApiClient.apiService,
    private val sender: RequestSender = RequestSender { method, path, body, key -> ApiClient.send(method, path, body, key) },
    /** The signed-in user; queued writes are tagged with it and only that user's session sends them. */
    private val userIdProvider: () -> String? = { ApiClient.tokenStore?.sessionUserId }
) {
    private val tag = "BukhariRepository"
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
        ApiClient.skippedRecords = 0
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
            // A screen the user may not open is emptied, so nothing cached under an older role stays visible.
            val exams = if (Permissions.can("exams.read")) api.getExams().bodyIfOk()?.exams else emptyList()
            val tasks = api.getTasks().bodyIfOk()?.tasks
            val notifications = api.getNotifications().bodyIfOk()?.notifications
            val customPermissions = api.getCustomPermissions().bodyIfOk()?.permissions
            if (Permissions.can("summerRows.read")) api.getSheetRows().bodyIfOk()?.rows?.let { _sheetRows.value = it }
            else _sheetRows.value = emptyList()

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

    /** Queues a write for the signed-in user and tries to deliver it (and anything queued before it) right away. */
    private suspend fun push(label: String, method: String, path: String, body: Any? = null, versionOf: String? = null): WriteResult {
        val json = body?.let { ApiClient.moshi.adapter(Any::class.java).toJson(it) }
        val id = db.pendingOpDao().insert(
            PendingOpEntity(label = label, method = method, path = path, bodyJson = json, versionOf = versionOf, userId = userIdProvider())
        )
        return flushPendingOps().outcomes[id] ?: WriteResult.Queued
    }

    /**
     * Sends this user's queued writes in order. Only one flush runs at a time in the whole process
     * (the app and the background worker share one lock), each write is claimed before it is sent,
     * and every write carries its idempotency key, so nothing is applied twice (MOB-04).
     * - unreachable server: stop, keep everything for later;
     * - 401: the session ended; keep everything and tell the app (MOB-01);
     * - 5xx/429: count the attempt; park after [MAX_ATTEMPTS] so it does not block the rest;
     * - other 4xx: keep it as refused, with the server's message, for the user to retry or discard (MOB-03);
     *   a 404 for a delete means it is already gone, which is what was wanted.
     */
    suspend fun flushPendingOps(): FlushReport = withContext(Dispatchers.IO) {
        queueMutex.withLock {
            val outcomes = mutableMapOf<Long, WriteResult>()
            val userId = userIdProvider()
            for (op in db.pendingOpDao().getActive(userId)) {
                val now = System.currentTimeMillis()
                if (db.pendingOpDao().claim(op.id, now, now - CLAIM_TIMEOUT_MS) == 0) continue
                val res = sender.send(op.method, op.path, bodyWithVersion(op), op.idempotencyKey.ifBlank { null })
                val code = res.code
                when {
                    code == null -> {
                        db.pendingOpDao().release(op.id)
                        break
                    }
                    code in 200..299 || (code == 404 && op.method == "DELETE") -> {
                        db.pendingOpDao().deleteById(op.id)
                        outcomes[op.id] = WriteResult.Sent
                        storeServerVersion(res.body)
                    }
                    code == 401 -> {
                        db.pendingOpDao().release(op.id)
                        ApiClient.onUnauthorized?.invoke()
                        break
                    }
                    code == 426 -> {
                        // This app version is too old for the server: keep the write until the app is updated.
                        db.pendingOpDao().recordFailure(op.id, op.attempts, false, res.message ?: "يرجى تحديث التطبيق")
                        outcomes[op.id] = WriteResult.Rejected(res.message ?: "يرجى تحديث التطبيق إلى أحدث إصدار")
                        break
                    }
                    code >= 500 || code == 429 -> {
                        val attempts = op.attempts + 1
                        val park = attempts >= MAX_ATTEMPTS
                        db.pendingOpDao().recordFailure(op.id, attempts, park, res.message ?: "HTTP $code")
                        if (!park) break
                    }
                    else -> {
                        Log.w(tag, "${op.label} refused by server: HTTP $code")
                        val message = res.message ?: "رفض الخادم هذا التعديل"
                        db.pendingOpDao().markRejected(op.id, code, message)
                        outcomes[op.id] = WriteResult.Rejected(message)
                    }
                }
            }
            FlushReport(outcomes, db.pendingOpDao().countActive(userId))
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

    /** Writes the server refused, with its message, waiting for the user's decision (MOB-03). */
    suspend fun refusedWrites(): List<RefusedWrite> = withContext(Dispatchers.IO) {
        db.pendingOpDao().getRejected().map { RefusedWrite(it.id, it.label, it.lastError, it.httpCode, it.createdAt) }
    }

    /** Sends a refused write again, after a sync brought the record's current version. */
    suspend fun retryRefusedWrite(id: Long): FlushReport = withContext(Dispatchers.IO) {
        db.pendingOpDao().unreject(id)
        flushPendingOps()
    }

    suspend fun discardRefusedWrite(id: Long) = withContext(Dispatchers.IO) { db.pendingOpDao().deleteById(id) }

    /** Writes that have not reached the server, of any user (shown before logout and at sign-in). */
    suspend fun unsentCount(): Int = withContext(Dispatchers.IO) { db.pendingOpDao().countUnsent() }

    /** Users other than [userId] who left unsent work on this device. */
    suspend fun otherUsersWithUnsentWork(userId: String): List<String> = withContext(Dispatchers.IO) {
        db.pendingOpDao().unsentUserIds().filter { it != userId }
    }

    /** Deletes unsent work of users other than [userId], after the person who signed in agreed. */
    suspend fun discardOtherUsersWork(userId: String) = withContext(Dispatchers.IO) { db.pendingOpDao().deleteOtherUsers(userId) }

    /**
     * Removes the cached server data. Unsent work stays unless [keepQueue] is false, which only
     * happens when the user explicitly chose to discard it (MOB-01, MOB-02).
     */
    suspend fun clearAllData(keepQueue: Boolean = true) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.courseDao().clearAll()
            db.committeeDao().clearAll()
            db.appUserDao().clearAll()
            db.examDao().clearAll()
            db.scheduleTaskDao().clearAll()
            db.notificationDao().clearAll()
            db.customPermissionDao().clearAll()
            if (!keepQueue) db.pendingOpDao().clearAll()
        }
        _sheetRows.value = emptyList()
        lastSyncAt = null
        lastSyncOnline = false
    }

    /** ISO-8601 UTC, the format the server stores (MOB-15); the server stamps its own time on stage changes. */
    private fun now(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        .format(Date())

    // ------------------------------------------------------------------ committees

    /** Creates a committee or saves its base data (the server keeps stages and documents it already has). */
    suspend fun saveCommittee(committee: Committee): WriteResult = withContext(Dispatchers.IO) {
        val entity = CommitteeEntity.fromDomain(committee)
        db.committeeDao().insertCommittee(entity)
        push("saveCommittee", "POST", "api/committees", entity, versionOf = "committee:${entity.id}")
    }

    /** Workflow intents go to POST /api/committees/:id/actions; the server applies the rule (MOB-07). */
    private suspend fun committeeAction(label: String, committeeId: String, action: Map<String, Any?>): WriteResult =
        push(label, "POST", "api/committees/${enc(committeeId)}/actions", mapOf("action" to action.filterValues { it != null }))

    /** Completes or reopens one stage; the server checks the stage order and records who and when. */
    suspend fun updateStage(committeeId: String, stageKey: String, completed: Boolean): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
        val updatedStages = entity.stages.updateStage(stageKey, completed, if (completed) now() else null)
        db.committeeDao().updateCommittee(entity.copy(stages = updatedStages))
        committeeAction(
            "updateStage", committeeId,
            mapOf("type" to if (completed) "completeStage" else "reopenStage", "stage" to stageKey)
        )
    }

    /**
     * Records that the assistant received the committee (or, with [cancel], clears it). Like the web,
     * this does not complete any stage; stages are completed explicitly (D-03).
     */
    suspend fun recordAssistantHandover(committeeId: String, me: AppUser, phone: String, cancel: Boolean = false): WriteResult =
        withContext(Dispatchers.IO) {
            val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
            val reception = if (cancel) null
            else com.example.data.model.AssistantReception(assistantId = me.id, receivedByName = me.name, receivedAt = now(), phone = phone)
            db.committeeDao().updateCommittee(entity.copy(receivedByAssistant = reception))
            committeeAction(
                "recordAssistantHandover", committeeId,
                if (cancel) mapOf("type" to "cancelReception") else mapOf("type" to "receive", "status" to "received", "phone" to phone.ifBlank { null })
            )
        }

    suspend fun approveExamReadiness(committeeId: String, me: AppUser, revoke: Boolean = false): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
        val approval = if (revoke) null else com.example.data.model.ExamApproval(assistantId = me.id, approvedByName = me.name, approvedAt = now())
        db.committeeDao().updateCommittee(entity.copy(examApprovedBy = approval))
        committeeAction("approveExamReadiness", committeeId, mapOf("type" to if (revoke) "revokeReadiness" else "approveReadiness"))
    }

    /**
     * Ticks one question. When every question is scored the scoring stage is completed too, and
     * reopened when one is unticked: the same rule as the web.
     */
    suspend fun updateQuestion(committeeId: String, questionId: String, completed: Boolean): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
        val questions = entity.scoringDetails.questions.map {
            if (it.id == questionId) it.copy(completed = completed, completedAt = if (completed) now() else null) else it
        }
        val scoring = entity.scoringDetails.copy(questions = questions)
        db.committeeDao().updateCommittee(entity.copy(scoringDetails = scoring))
        val result = committeeAction("updateQuestion", committeeId, mapOf("type" to "updateQuestion", "questionId" to questionId, "completed" to completed))
        val allDone = questions.isNotEmpty() && questions.all { it.completed }
        if (result !is WriteResult.Rejected && allDone != entity.stages.scoring.completed && Permissions.canOn("committees.scoring", committeeId)) {
            updateStage(committeeId, "scoring", allDone)
        }
        result
    }

    /**
     * Replaces the scoring document (examiner names, the list of questions). This replaces the whole
     * document, so it carries the version the phone has; a newer edit elsewhere is refused (API-11).
     */
    suspend fun updateScoring(committeeId: String, scoring: ScoringDetails): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
        db.committeeDao().updateCommittee(entity.copy(scoringDetails = scoring))
        push("updateScoring", "PATCH", "api/committees/${enc(committeeId)}", mapOf("scoringDetails" to scoring), versionOf = "committee:$committeeId")
    }

    /** Aggregation details; the aggregation stage itself is completed separately, as on the web (D-03). */
    suspend fun updateAggregation(committeeId: String, aggregation: AggregationDetails): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
        db.committeeDao().updateCommittee(entity.copy(aggregationDetails = aggregation))
        committeeAction(
            "updateAggregation", committeeId,
            mapOf(
                "type" to "updateAggregation",
                "aggregatorName" to aggregation.aggregatorName.ifBlank { null },
                "scoringReviewerName" to aggregation.scoringReviewerName.ifBlank { null },
                "aggregationReviewerName" to aggregation.reviewerName.ifBlank { null },
                "reviewConfirmed" to aggregation.isConfirmedMatch,
                "date" to aggregation.date.ifBlank { null },
                "notes" to aggregation.notes.ifBlank { null }
            )
        )
    }

    /**
     * Schedules the ceremony. The server stores it and keeps its single calendar task in step; the
     * ceremony stage is completed explicitly, not by the date (D-03, WEB-08).
     */
    suspend fun updateCeremony(committeeId: String, ceremony: CeremonyDetails): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
        db.committeeDao().updateCommittee(entity.copy(ceremonyDetails = ceremony))
        val attendeesChanged = ceremony.attendees.map { it.id } != entity.ceremonyDetails.attendees.map { it.id }
        committeeAction(
            "updateCeremony", committeeId,
            mapOf(
                "type" to "scheduleCeremony",
                "ceremonyDate" to ceremony.date.ifBlank { null },
                "ceremonyTime" to ceremony.time.ifBlank { null },
                "ceremonyPlace" to ceremony.venue.ifBlank { null },
                "honoredStudentsCount" to ceremony.honoreesCount,
                "attendingSheikhs" to ceremony.attendingSheikhs,
                "notes" to ceremony.notes.ifBlank { null },
                "attendeeUserIds" to if (attendeesChanged) ceremony.attendees.map { it.id } else null
            )
        )
    }

    /**
     * Places committee heads and sheikhs on a committee: [userIds] is the full list the caller can
     * place (people the caller cannot place keep their membership). Everyone added or removed is notified.
     */
    suspend fun setCommitteeMembers(committeeId: String, userIds: List<String>): WriteResult = withContext(Dispatchers.IO) {
        push("setCommitteeMembers", "PUT", "api/committees/${enc(committeeId)}/members", mapOf("userIds" to userIds))
    }

    /** Invites people to (or removes them from) a committee's ceremony without changing its details. */
    suspend fun setCeremonyAttendees(committeeId: String, add: List<String>, remove: List<String>): WriteResult = withContext(Dispatchers.IO) {
        push("setCeremonyAttendees", "POST", "api/committees/${enc(committeeId)}/ceremony-attendees", mapOf("add" to add, "remove" to remove))
    }

    /** Saves base data together with the sheet stage's Excel link (the server keeps it on the sheet stage). */
    suspend fun saveCommitteeWithExcelUrl(committee: Committee, excelUrl: String?): WriteResult = withContext(Dispatchers.IO) {
        val entity = CommitteeEntity.fromDomain(committee)
        db.committeeDao().insertCommittee(entity)
        val json = ApiClient.moshi.adapter(CommitteeEntity::class.java).toJsonValue(entity) as? Map<*, *> ?: emptyMap<String, Any?>()
        val body = json.entries.associate { (k, v) -> k.toString() to v }.toMutableMap()
        body["excelUrl"] = excelUrl?.ifBlank { null } ?: ""
        push("saveCommittee", "POST", "api/committees", body, versionOf = "committee:${entity.id}")
    }

    /** Completes a stage with a note (e.g. the imported grade sheet), through the workflow like the web. */
    suspend fun completeStageWithNotes(committeeId: String, stageKey: String, notes: String): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
        val stage = entity.stages.getStage(stageKey).copy(completed = true, completedAt = now(), notes = notes)
        val stages = entity.stages.updateStage(stageKey, true, stage.completedAt).let { updated ->
            when (stageKey) { "sheet" -> updated.copy(sheet = stage); else -> updated }
        }
        db.committeeDao().updateCommittee(entity.copy(stages = stages))
        committeeAction("completeStageNotes", committeeId, mapOf("type" to "completeStage", "stage" to stageKey, "notes" to notes))
    }

    /** One question's scorers, score and notes (web: updateQuestion intent with scorers). */
    suspend fun updateQuestionDetails(
        committeeId: String, questionId: String, completed: Boolean, scorers: List<String>, score: Double?, notes: String?
    ): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.committeeDao().getCommitteeById(committeeId) ?: return@withContext WriteResult.Rejected("اللجنة غير موجودة")
        val questions = entity.scoringDetails.questions.map {
            if (it.id == questionId) it.copy(completed = completed, scorers = scorers, score = score, notes = notes) else it
        }
        db.committeeDao().updateCommittee(entity.copy(scoringDetails = entity.scoringDetails.copy(questions = questions)))
        committeeAction(
            "updateQuestionDetails", committeeId,
            mapOf("type" to "updateQuestion", "questionId" to questionId, "completed" to completed, "scorers" to scorers, "score" to score, "notes" to notes)
        )
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

    /** Downloads an exam's PDF (allowed roles, within their exam allow-list). */
    suspend fun downloadExamFile(examId: String): ByteArray? = withContext(Dispatchers.IO) {
        ApiClient.download("api/exams/${enc(examId)}/file")
    }

    // ------------------------------------------------------------------ tasks

    /** The server takes the committee and the assignee by id and fills in number and name (MOB-06). */
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

    /** Identity fields the server requires on every user save; access fields are added only when they change. */
    private fun identity(entity: AppUserEntity): MutableMap<String, Any?> =
        mutableMapOf("id" to entity.id, "name" to entity.name, "phone" to entity.phone, "email" to entity.email.ifBlank { null })

    /**
     * Approves an applicant. Only what changed is sent (MOB-09): the role only when a super admin
     * changed it, committees by id, permissions only when chosen. Nothing else is overwritten.
     */
    suspend fun approveUser(userId: String, committeeIds: List<String>?, permissions: List<String>?, role: UserRole?): WriteResult =
        withContext(Dispatchers.IO) {
            val entity = db.appUserDao().getUserById(userId) ?: return@withContext WriteResult.Rejected("المستخدم غير موجود")
            val body = identity(entity)
            body["status"] = "approved"
            if (role != null && role.roleKey != entity.roleKey) body["role"] = role.roleKey
            if (committeeIds != null) body["assignedCommitteeIds"] = committeeIds
            if (permissions != null) body["permissions"] = permissions
            db.appUserDao().updateUser(
                entity.copy(
                    status = "approved",
                    roleKey = role?.roleKey ?: entity.roleKey,
                    roleId = role?.roleKey ?: entity.roleId,
                    assignedCommitteeIds = committeeIds ?: entity.assignedCommitteeIds,
                    permissions = permissions ?: entity.permissions
                )
            )
            push("approveUser", "POST", "api/users", body, versionOf = "user:$userId")
        }

    /** Rejection keeps the account (status "rejected"), with the reason, the same as on the web. */
    suspend fun rejectUser(userId: String, reason: String?): WriteResult = withContext(Dispatchers.IO) {
        val entity = db.appUserDao().getUserById(userId) ?: return@withContext WriteResult.Rejected("المستخدم غير موجود")
        db.appUserDao().updateUser(entity.copy(status = "rejected"))
        val body = identity(entity)
        body["status"] = "rejected"
        body["rejectionReason"] = reason?.ifBlank { null }
        push("rejectUser", "POST", "api/users", body, versionOf = "user:$userId")
    }

    // ------------------------------------------------------------------ notifications

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
            pendingWrites = db.pendingOpDao().countActive(userIdProvider()),
            failedWrites = db.pendingOpDao().getFailed().map { FailedWrite(it.id, it.label, it.lastError) },
            refusedWrites = db.pendingOpDao().getRejected().map { RefusedWrite(it.id, it.label, it.lastError, it.httpCode, it.createdAt) },
            skippedRecords = ApiClient.skippedRecords,
            localTables = local,
            server = server
        )
    }

    companion object {
        const val MAX_ATTEMPTS = 5

        /** A claim older than this belongs to a flusher that died; the write may be sent again. */
        private const val CLAIM_TIMEOUT_MS = 2 * 60 * 1000L

        /** One lock for every repository instance in the process: the app and the background worker (MOB-04). */
        private val queueMutex = Mutex()
    }
}
