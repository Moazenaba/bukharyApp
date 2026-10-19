package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.sync.QueueFlushWorker
import com.example.data.model.AggregationDetails
import com.example.data.model.AppUser
import com.example.data.model.CeremonyDetails
import com.example.data.model.Committee
import com.example.data.model.Course
import com.example.data.model.CustomPermission
import com.example.data.model.DatabaseStatus
import com.example.data.model.Exam
import com.example.data.model.NotificationItem
import com.example.data.model.ScheduleTask
import com.example.data.model.ScoringDetails
import com.example.data.model.UserRole
import com.example.data.remote.ApiClient
import com.example.data.remote.AssignableUserDto
import com.example.data.remote.AuthResult
import com.example.data.remote.AuthenticationService
import com.example.data.remote.SheetRowDto
import com.example.data.remote.AuditLogDto
import com.example.data.remote.BookDto
import com.example.data.remote.OnlineApi
import com.example.data.remote.PermissionProfileDto
import com.example.data.remote.StaffUserDto
import com.example.data.repository.BukhariRepository
import com.example.data.repository.WriteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class AppNavScreen(val route: String, val titleAr: String, val iconRes: String) {
    HOME("home", "الرئيسية", "home"),
    COURSE_TRACKER("course_tracker", "كشف الدورات", "table"),
    COMMITTEES("committees", "اللجان والمراحل", "groups"),
    EXAMS("exams", "الامتحانات", "menu_book"),
    SCHEDULE("schedule", "التقويم والمهام", "calendar"),
    ASSISTANTS("assistants", "المعاونون والصلاحيات", "badge"),
    DATABASE_INSPECTOR("database_inspector", "حالة البيانات والمزامنة", "database"),
    STATISTICS("statistics", "الإحصائيات الشاملة", "bar_chart"),
    COURSES("courses", "إضافة وإدارة الدورات", "event_repeat"),
    CEREMONIES("ceremonies", "الحفلات والتكريم", "celebration"),
    DAILY_ALERTS("daily_alerts", "التنبيهات والمهام اليومية", "notifications_active"),
    AUDIT("audit", "سجل التدقيق", "history"),
    NOTIFICATIONS("notifications", "الإشعارات", "notifications"),
    MENU("menu", "القائمة", "menu")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BukhariRepository(AppDatabase.getInstance(application))
    private val authService = AuthenticationService(application)
    private val syncMutex = Mutex()

    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    /** True until the saved session (if any) has been checked at startup. */
    private val _isRestoringSession = MutableStateFlow(true)
    val isRestoringSession: StateFlow<Boolean> = _isRestoringSession.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _currentNavScreen = MutableStateFlow(AppNavScreen.HOME)
    val currentNavScreen: StateFlow<AppNavScreen> = _currentNavScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStageFilter = MutableStateFlow("all")
    val selectedStageFilter: StateFlow<String> = _selectedStageFilter.asStateFlow()

    private val _selectedCourseId = MutableStateFlow<String?>(null)
    val selectedCourseId: StateFlow<String?> = _selectedCourseId.asStateFlow()

    private val _selectedCommitteeIdForDetail = MutableStateFlow<String?>(null)
    val selectedCommitteeIdForDetail: StateFlow<String?> = _selectedCommitteeIdForDetail.asStateFlow()

    private val _snackBarMessage = MutableStateFlow<String?>(null)
    val snackBarMessage: StateFlow<String?> = _snackBarMessage.asStateFlow()

    private val _databaseStatus = MutableStateFlow<DatabaseStatus?>(null)
    val databaseStatus: StateFlow<DatabaseStatus?> = _databaseStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /** Device has a validated internet connection (from ConnectivityManager). */
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    /** Result of the last sync attempt: true = data is current with the server. */
    private val _lastSyncOk = MutableStateFlow(false)
    val lastSyncOk: StateFlow<Boolean> = _lastSyncOk.asStateFlow()

    /** Writes on this device that have not reached the server (shown at sign-in and before logout). */
    private val _unsentCount = MutableStateFlow(0)
    val unsentCount: StateFlow<Int> = _unsentCount.asStateFlow()

    /** Set when logout was requested while writes are still unsent: the user decides (MOB-02). */
    private val _logoutConfirmation = MutableStateFlow<Int?>(null)
    val logoutConfirmation: StateFlow<Int?> = _logoutConfirmation.asStateFlow()

    /** Set when another account left unsent work on this device: kept or discarded by choice (MOB-01). */
    private val _foreignUnsentWork = MutableStateFlow(false)
    val foreignUnsentWork: StateFlow<Boolean> = _foreignUnsentWork.asStateFlow()

    /** People the current user may assign tasks to (from the server, limited to their committees). */
    private val _assignableUsers = MutableStateFlow<List<AssignableUserDto>>(emptyList())
    val assignableUsers: StateFlow<List<AssignableUserDto>> = _assignableUsers.asStateFlow()

    val courses: StateFlow<List<Course>> = repository.allCourses.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val committees: StateFlow<List<Committee>> = repository.allCommittees.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val users: StateFlow<List<AppUser>> = repository.allUsers.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val exams: StateFlow<List<Exam>> = repository.allExams.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val tasks: StateFlow<List<ScheduleTask>> = repository.allTasks.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val notifications: StateFlow<List<NotificationItem>> =
        repository.allNotifications.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val customPermissions: StateFlow<List<CustomPermission>> =
        repository.allCustomPermissions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val sheetRows: StateFlow<List<SheetRowDto>> = repository.sheetRows

    private val _filterInputs = combine(_searchQuery, _selectedStageFilter, _selectedCourseId) { q, st, c -> Triple(q, st, c) }

    /** Same filters and sorting as the web's committee list (search, stage, course, gender, mine, unscored, sort). */
    val filteredCommittees: StateFlow<List<Committee>> by lazy {
        combine(committees, _filterInputs, _committeeFilters) { commList, inputs, f ->
            val (query, stageFilter, courseId) = inputs
            val q = query.trim().lowercase()
            commList.filter { c ->
                val matchesCourse = courseId == null || c.courseId == courseId
                val matchesQuery = q.isEmpty() ||
                    c.committeeNumber.toString().contains(q) ||
                    c.committeeName.lowercase().contains(q) ||
                    c.competitionBook.lowercase().contains(q) ||
                    c.branch.lowercase().contains(q) ||
                    c.associationName.lowercase().contains(q) ||
                    c.members.any { it.lowercase().contains(q) } ||
                    c.sheikhName.lowercase().contains(q) ||
                    c.sheikhPhone.lowercase().contains(q) ||
                    c.sheikhCountry.lowercase().contains(q) ||
                    c.generalNotes.lowercase().contains(q)
                // Like the web: the stage filter shows committees whose current stage is that stage.
                val matchesStage = stageFilter == "all" || com.example.ui.components.currentStageKey(c.stages) == stageFilter
                val matchesGender = when (f.gender) { "women" -> c.isWomenCommittee; "men" -> !c.isWomenCommittee; else -> true }
                val matchesMine = !f.mineOnly || com.example.data.remote.Permissions.inScope(c.id)
                val matchesScoring = !f.needsScoring || c.scoringDetails.questions.any { !it.completed }
                matchesCourse && matchesQuery && matchesStage && matchesGender && matchesMine && matchesScoring
            }.let { list ->
                when (f.sortBy) {
                    "oldest", "committeeNumber" -> list.sortedBy { it.committeeNumber }
                    "book" -> list.sortedBy { it.competitionBook }
                    else -> list.sortedByDescending { it.committeeNumber }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    private val connectivity = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isNetworkAvailable.value = true
            // Deliver writes made offline as soon as the connection returns.
            if (_isLoggedIn.value) viewModelScope.launch { refreshFromServer() }
        }

        override fun onLost(network: Network) {
            _isNetworkAvailable.value = connectivity.activeNetwork != null
        }
    }

    init {
        ApiClient.onUnauthorized = { viewModelScope.launch { endSession("انتهت صلاحية الجلسة، يرجى تسجيل الدخول مرة أخرى", revoke = false) } }
        viewModelScope.launch { _unsentCount.value = repository.unsentCount() }
        connectivity.registerNetworkCallback(
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            networkCallback
        )
        _isNetworkAvailable.value = connectivity.activeNetwork != null

        viewModelScope.launch {
            // Restore the session before syncing so requests carry the saved token.
            val savedUserId = authService.getSavedUserId()
            val savedUser = if (authService.hasValidSession() && savedUserId != null) repository.getLocalUser(savedUserId) else null
            if (savedUser != null && savedUser.status == "approved") {
                authService.restoreUserPermissions(savedUser)
                _currentUser.value = savedUser
                _isLoggedIn.value = true
                _isRestoringSession.value = false
                refreshFromServer()
            } else {
                authService.forgetSession()
                // Cached data goes; unsent work stays for its author's next sign-in (MOB-01).
                repository.clearAllData(keepQueue = true)
                _unsentCount.value = repository.unsentCount()
                _isRestoringSession.value = false
            }
        }
    }

    override fun onCleared() {
        connectivity.unregisterNetworkCallback(networkCallback)
        ApiClient.onUnauthorized = null
        super.onCleared()
    }

    /** Sends queued writes, reloads data and the user's permissions from the server. */
    private suspend fun refreshFromServer(): Boolean = syncMutex.withLock {
        _isSyncing.value = true
        try {
            authService.refreshMe()?.let { me ->
                repository.cacheUser(me)
                _currentUser.value = me
            }
            val result = repository.syncWithRemoteApi()
            _lastSyncOk.value = result.online
            val status = repository.getDatabaseStatus()
            _databaseStatus.value = status
            _unsentCount.value = repository.unsentCount()
            // Writes the background worker sent and the server refused are reported, not lost (MOB-03).
            if (status.refusedWrites.isNotEmpty()) {
                showMessage("رفض الخادم ${status.refusedWrites.size} من التعديلات المرسلة؛ راجعها في «حالة البيانات والمزامنة»")
            }
            result.online
        } finally {
            _isSyncing.value = false
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val ok = refreshFromServer()
            showMessage(if (ok) "تم التحديث من الخادم" else "تعذر التحديث من الخادم؛ تعرض آخر بيانات محفوظة")
        }
    }

    /** Shows the outcome of a write; a refused write is undone by reloading the server's data. */
    private suspend fun report(result: WriteResult, success: String) {
        when (result) {
            WriteResult.Sent -> showMessage(success)
            WriteResult.Queued -> {
                QueueFlushWorker.enqueue(getApplication())
                showMessage("تم الحفظ على الجهاز، وسيُرسل للخادم عند توفر الاتصال")
            }
            is WriteResult.Rejected -> {
                showMessage(result.message)
                refreshFromServer()
            }
        }
        _databaseStatus.value = repository.getDatabaseStatus()
    }

    private fun write(success: String, block: suspend () -> WriteResult) {
        viewModelScope.launch { report(block(), success) }
    }

    fun navigateTo(screen: AppNavScreen) {
        val user = _currentUser.value ?: return
        _currentNavScreen.value = if (user.canAccessScreen(screen)) screen else AppNavScreen.HOME
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedStageFilter(filter: String) { _selectedStageFilter.value = filter }
    fun setSelectedCourseId(courseId: String?) { _selectedCourseId.value = courseId }
    fun openCommitteeDetail(committeeId: String) { _selectedCommitteeIdForDetail.value = committeeId }
    fun closeCommitteeDetail() { _selectedCommitteeIdForDetail.value = null }
    fun showMessage(message: String) { _snackBarMessage.value = message }
    fun clearMessage() { _snackBarMessage.value = null }

    fun login(phoneOrEmail: String, pass: String) {
        if (_isSubmitting.value) return
        if (phoneOrEmail.isBlank() || pass.isBlank()) {
            showMessage("يرجى إدخال رقم الهاتف أو البريد الإلكتروني وكلمة المرور")
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = authService.login(phoneOrEmail, pass)) {
                is AuthResult.Success -> {
                    // Never show a previous user's cached data; unsent work is kept per user (MOB-01).
                    repository.clearAllData(keepQueue = true)
                    _foreignUnsentWork.value = repository.otherUsersWithUnsentWork(result.user.id).isNotEmpty()
                    repository.cacheUser(result.user)
                    _currentUser.value = result.user
                    _currentNavScreen.value = AppNavScreen.HOME
                    _isLoggedIn.value = true
                    showMessage("أهلاً وسهلاً، ${result.user.name}")
                    refreshFromServer()
                }
                is AuthResult.Error -> showMessage(result.message)
            }
            _isSubmitting.value = false
        }
    }

    /** Calls [onDone] with the server's request code, or null when the request was refused. */
    fun registerJoinRequest(name: String, phone: String, email: String, role: UserRole, pass: String, notes: String, onDone: (String?) -> Unit) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            authService.register(name, phone, email, role, pass, notes)
                .onSuccess { code -> onDone(code) }
                .onFailure {
                    showMessage(it.message ?: "فشل تسجيل طلب الانضمام")
                    onDone(null)
                }
            _isSubmitting.value = false
        }
    }

    /** Looks up a join request by its code and reports a readable status. */
    fun trackJoinRequest(code: String, onResult: (String) -> Unit) {
        if (code.isBlank()) return
        viewModelScope.launch {
            authService.trackJoinRequest(code)
                .onSuccess { r -> onResult(if (r?.status == null) "لم يتم العثور على طلب بهذا الكود" else joinStatusLabel(r.status, r.interviewDate)) }
                .onFailure { onResult(it.message ?: "تعذر الاستعلام عن الطلب") }
        }
    }

    private fun joinStatusLabel(status: String, interviewDate: String?): String = when (status) {
        "pending" -> "قيد مراجعة المشرف العام"
        "interview" -> "تم تحديد موعد مقابلة شخصية" + (interviewDate?.let { ": $it" } ?: "")
        "approved" -> "تم اعتماد الحساب، يمكنك تسجيل الدخول الآن"
        "rejected" -> "تم الاعتذار عن قبول الطلب"
        else -> status
    }

    /** Asks first when writes have not reached the server yet (MOB-02). */
    fun requestLogout() {
        viewModelScope.launch {
            val unsent = repository.unsentCount()
            if (unsent > 0) _logoutConfirmation.value = unsent else endSession("تم تسجيل الخروج", revoke = true)
        }
    }

    fun cancelLogout() {
        _logoutConfirmation.value = null
    }

    /** Tries to send the waiting writes, then asks again only if some are still unsent. */
    fun syncThenLogout() {
        _logoutConfirmation.value = null
        viewModelScope.launch {
            refreshFromServer()
            val unsent = repository.unsentCount()
            if (unsent > 0) _logoutConfirmation.value = unsent else endSession("تم إرسال التعديلات وتسجيل الخروج", revoke = true)
        }
    }

    /**
     * Logs out. [discardUnsent] deletes the unsent writes (the user chose it); otherwise they stay on
     * the device and are sent after this user signs in again.
     */
    fun logout(discardUnsent: Boolean = false) {
        _logoutConfirmation.value = null
        viewModelScope.launch {
            endSession(
                if (discardUnsent) "تم تسجيل الخروج وحذف التعديلات غير المرسلة"
                else if (repository.unsentCount() > 0) "تم تسجيل الخروج؛ التعديلات غير المرسلة محفوظة وستُرسل بعد دخولك مرة أخرى"
                else "تم تسجيل الخروج",
                revoke = true,
                discardUnsent = discardUnsent
            )
        }
    }

    fun keepForeignUnsentWork() {
        _foreignUnsentWork.value = false
    }

    fun discardForeignUnsentWork() {
        val me = _currentUser.value ?: return
        viewModelScope.launch {
            repository.discardOtherUsersWork(me.id)
            _foreignUnsentWork.value = false
            _unsentCount.value = repository.unsentCount()
            showMessage("تم حذف تعديلات الحساب الآخر غير المرسلة")
        }
    }

    /**
     * Ends the session. Cached server data and downloaded exam files are removed; unsent writes are
     * kept unless the user chose to discard them, even when the server ended the session (MOB-01).
     */
    private suspend fun endSession(message: String, revoke: Boolean, discardUnsent: Boolean = false) {
        if (!_isLoggedIn.value && _currentUser.value == null) return
        _isLoggedIn.value = false
        _currentUser.value = null
        closeCommitteeDetail()
        if (revoke) authService.logout() else authService.forgetSession()
        repository.clearAllData(keepQueue = !discardUnsent)
        deleteExamFiles()
        _databaseStatus.value = null
        _assignableUsers.value = emptyList()
        val unsent = repository.unsentCount()
        _unsentCount.value = unsent
        showMessage(if (!revoke && unsent > 0) "$message — لديك $unsent تعديلات محفوظة ستُرسل بعد تسجيل الدخول" else message)
    }

    /** Downloaded exam PDFs are not left on the device after the session (MOB-12). */
    private suspend fun deleteExamFiles() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        java.io.File(getApplication<Application>().cacheDir, "exams").deleteRecursively()
    }

    /** Changes the own password (and completes a forced change after an admin reset). */
    fun changePassword(current: String, next: String, onDone: (Boolean) -> Unit) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            authService.changePassword(current, next)
                .onSuccess { me ->
                    repository.cacheUser(me)
                    _currentUser.value = me
                    showMessage("تم تغيير كلمة المرور")
                    onDone(true)
                    refreshFromServer()
                }
                .onFailure {
                    showMessage(it.message ?: "تعذر تغيير كلمة المرور")
                    onDone(false)
                }
            _isSubmitting.value = false
        }
    }

    /** Loads who the current user may assign tasks to. */
    fun loadAssignableUsers() {
        viewModelScope.launch { _assignableUsers.value = authService.assignableUsers() }
    }

    // ---------------- committees

    fun toggleStage(committeeId: String, stageKey: String, currentStatus: Boolean) {
        val label = com.example.ui.theme.StageColorMap[stageKey]?.name ?: stageKey
        write(if (!currentStatus) "تم إنجاز مرحلة $label ✓" else "تم إلغاء إنجاز مرحلة $label") {
            repository.updateStage(committeeId, stageKey, !currentStatus)
        }
    }

    fun saveCommittee(committee: Committee) =
        write("تم حفظ بيانات اللجنة رقم (${committee.committeeNumber}) ✓") { repository.saveCommittee(committee) }

    fun deleteCommittee(committeeId: String) {
        closeCommitteeDetail()
        write("تم حذف اللجنة") { repository.deleteCommittee(committeeId) }
    }

    fun recordAssistantReception(committeeId: String, phone: String, cancel: Boolean = false) {
        val me = _currentUser.value ?: return
        write(if (cancel) "تم إلغاء توثيق الاستلام" else "تم توثيق استلام اللجنة ✓") {
            repository.recordAssistantHandover(committeeId, me, phone, cancel)
        }
    }

    fun approveExamReadiness(committeeId: String, revoke: Boolean = false) {
        val me = _currentUser.value ?: return
        write(if (revoke) "تم إلغاء اعتماد الجاهزية" else "تم اعتماد جاهزية الامتحان ✓") {
            repository.approveExamReadiness(committeeId, me, revoke)
        }
    }

    /** Ticks or unticks one question; only that question is sent. */
    fun updateQuestion(committeeId: String, questionId: String, completed: Boolean) =
        write(if (completed) "تم تصحيح السؤال ✓" else "تم إلغاء تصحيح السؤال") { repository.updateQuestion(committeeId, questionId, completed) }

    fun updateScoring(committeeId: String, scoring: ScoringDetails) =
        write("تم حفظ التصحيح ✓") { repository.updateScoring(committeeId, scoring) }

    fun updateAggregation(committeeId: String, aggregation: AggregationDetails) =
        write("تم حفظ تجميع الدرجات ✓") { repository.updateAggregation(committeeId, aggregation) }

    fun updateCeremony(committeeId: String, ceremony: CeremonyDetails) =
        write("تم حفظ تفاصيل الحفل ✓") { repository.updateCeremony(committeeId, ceremony) }

    // ---------------- exams

    fun saveExam(exam: Exam) = write("تم حفظ الامتحان ✓") { repository.saveExam(exam) }

    fun deleteExam(examId: String) = write("تم حذف الامتحان") { repository.deleteExam(examId) }

    /** Downloads an exam's PDF into the app cache and hands it back for opening. */
    fun openExamFile(exam: Exam, onReady: (java.io.File) -> Unit) {
        viewModelScope.launch {
            val bytes = repository.downloadExamFile(exam.id)
            if (bytes == null) {
                showMessage("تعذر تنزيل ملف الأسئلة (تحقق من الاتصال أو الصلاحية)")
                return@launch
            }
            val dir = java.io.File(getApplication<Application>().cacheDir, "exams").apply { mkdirs() }
            val file = java.io.File(dir, "${exam.id}.pdf")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // Only the exam being opened is kept on the device (MOB-12).
                dir.listFiles()?.filter { it != file }?.forEach { it.delete() }
                file.writeBytes(bytes)
            }
            onReady(file)
        }
    }

    // ---------------- tasks

    fun saveTask(task: ScheduleTask) = write("تمت جدولة المهمة ✓") { repository.saveTask(task) }

    fun toggleTask(taskId: String) = write("تم تحديث حالة المهمة") { repository.toggleTask(taskId) }

    fun deleteTask(taskId: String) = write("تم حذف المهمة") { repository.deleteTask(taskId) }

    // ---------------- users & permissions

    /** Null arguments are left unchanged on the server (MOB-09). */
    fun approveUser(user: AppUser, committeeIds: List<String>?, permissions: List<String>?, role: UserRole?) =
        write("تم اعتماد حساب ${user.name} ✓") { repository.approveUser(user.id, committeeIds, permissions, role) }

    fun rejectUser(user: AppUser, reason: String? = null) =
        write("تم تسجيل الاعتذار عن طلب ${user.name}") { repository.rejectUser(user.id, reason) }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    // ---------------- data & sync status

    fun refreshDatabaseStatus() {
        viewModelScope.launch { _databaseStatus.value = repository.getDatabaseStatus() }
    }

    fun retryFailedWrites() {
        viewModelScope.launch {
            repository.retryFailedWrites()
            refreshFromServer()
            showMessage("تمت إعادة محاولة إرسال التعديلات")
        }
    }

    fun discardFailedWrites() {
        viewModelScope.launch {
            repository.discardFailedWrites()
            refreshFromServer()
            showMessage("تم تجاهل التعديلات التي تعذر إرسالها واستعادة بيانات الخادم")
        }
    }

    /** Sends a refused write again after reloading the server's current data (e.g. after a conflict). */
    fun retryRefusedWrite(id: Long) {
        viewModelScope.launch {
            refreshFromServer()
            val report = repository.retryRefusedWrite(id)
            val outcome = report.outcomes[id]
            showMessage(if (outcome is WriteResult.Rejected) "رفض الخادم التعديل مرة أخرى: ${outcome.message}" else "تمت إعادة إرسال التعديل")
            refreshFromServer()
        }
    }

    fun discardRefusedWrite(id: Long) {
        viewModelScope.launch {
            repository.discardRefusedWrite(id)
            refreshFromServer()
            showMessage("تم حذف التعديل المرفوض واستعادة بيانات الخادم")
        }
    }

    // ================================================================ management (online, like the web)

    /** A question the screen must ask before going on (e.g. deleting a course with sheet rows). */
    data class ConfirmRequest(val title: String, val message: String, val confirmLabel: String, val onConfirm: () -> Unit)

    private val _confirmRequest = MutableStateFlow<ConfirmRequest?>(null)
    val confirmRequest: StateFlow<ConfirmRequest?> = _confirmRequest.asStateFlow()
    fun dismissConfirm() { _confirmRequest.value = null }

    private val _books = MutableStateFlow<List<BookDto>>(emptyList())
    val books: StateFlow<List<BookDto>> = _books.asStateFlow()

    private val _staffUsers = MutableStateFlow<List<StaffUserDto>>(emptyList())
    val staffUsers: StateFlow<List<StaffUserDto>> = _staffUsers.asStateFlow()

    private val _profiles = MutableStateFlow<List<PermissionProfileDto>>(emptyList())
    val profiles: StateFlow<List<PermissionProfileDto>> = _profiles.asStateFlow()

    private val _staffLoading = MutableStateFlow(false)
    val staffLoading: StateFlow<Boolean> = _staffLoading.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLogDto>>(emptyList())
    val auditLogs: StateFlow<List<AuditLogDto>> = _auditLogs.asStateFlow()
    private val _auditLoading = MutableStateFlow(false)
    val auditLoading: StateFlow<Boolean> = _auditLoading.asStateFlow()
    private val _auditHasMore = MutableStateFlow(true)
    val auditHasMore: StateFlow<Boolean> = _auditHasMore.asStateFlow()

    /** Committees list filters beyond search/stage (web: SearchAndFilters and the assistant bar). */
    data class CommitteeFilters(
        val sortBy: String = "newest",
        val gender: String = "all",
        val mineOnly: Boolean = false,
        val needsScoring: Boolean = false
    )
    private val _committeeFilters = MutableStateFlow(CommitteeFilters())
    val committeeFilters: StateFlow<CommitteeFilters> = _committeeFilters.asStateFlow()
    fun setCommitteeFilters(f: CommitteeFilters) { _committeeFilters.value = f }

    /**
     * Runs a call the server must confirm before anything is shown as done (web: attempt()).
     * Success reloads the data, so every screen shows what is really stored.
     */
    private fun online(success: String?, onDone: (Boolean) -> Unit = {}, reload: Boolean = true, block: suspend () -> Unit) {
        viewModelScope.launch {
            val ok = try {
                block()
                true
            } catch (e: OnlineApi.ApiFailure) {
                showMessage(e.message ?: "تعذر تنفيذ الطلب")
                false
            } catch (e: Exception) {
                showMessage("تعذر تنفيذ الطلب: ${e.message ?: "خطأ غير متوقع"}")
                false
            }
            if (ok) success?.let { showMessage(it) }
            onDone(ok)
            if (ok && reload) refreshFromServer()
        }
    }

    // ---------------- courses

    private fun courseBody(c: Course): Map<String, Any?> = mapOf(
        "id" to c.id, "name" to c.name, "season" to c.season, "year" to c.year,
        "startDate" to c.startDate.ifBlank { null }, "endDate" to c.endDate.ifBlank { null }, "status" to c.status,
        "description" to c.description, "targetCommitteesCount" to c.targetCommitteesCount, "isDefault" to c.isDefault
    )

    fun saveCourse(course: Course, onDone: (Boolean) -> Unit = {}) =
        online(if (courses.value.any { it.id == course.id }) "تم حفظ الدورة" else "تمت إضافة الدورة: ${course.name}", onDone) {
            OnlineApi.saveCourse(courseBody(course))
        }

    fun setDefaultCourse(course: Course) = saveCourse(course.copy(isDefault = true, status = "active"))

    /** A course with sheet rows is deleted only after the user confirms the rows go too (API-27). */
    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            try {
                OnlineApi.deleteCourse(course.id)
                showMessage("تم حذف الدورة")
                if (_selectedCourseId.value == course.id) _selectedCourseId.value = null
                refreshFromServer()
            } catch (e: OnlineApi.ApiFailure) {
                if (e.httpCode == 409 && e.message?.contains("كشف") == true) {
                    _confirmRequest.value = ConfirmRequest("حذف الدورة مع كشفها", e.message ?: "", "حذف الدورة والكشف") {
                        online("تم حذف الدورة") { OnlineApi.deleteCourse(course.id, deleteSheetRows = true) }
                    }
                } else showMessage(e.message ?: "تعذر حذف الدورة")
            }
        }
    }

    // ---------------- books & exam files

    fun loadBooks() {
        viewModelScope.launch { try { _books.value = OnlineApi.books() } catch (_: Exception) { } }
    }

    fun addBook(name: String) {
        if (name.isBlank() || _books.value.any { it.name == name.trim() }) return
        online("تمت إضافة الكتاب", reload = false) { OnlineApi.saveBook(name); _books.value = OnlineApi.books() }
    }

    fun deleteBook(book: BookDto) = online("تم حذف الكتاب", reload = false) { OnlineApi.deleteBook(book.id); _books.value = OnlineApi.books() }

    private suspend fun readUri(uri: android.net.Uri): Pair<ByteArray, String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: throw OnlineApi.ApiFailure("تعذر قراءة الملف المختار", null)
        var name = "exam.pdf"
        resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0)?.let { name = it }
        }
        bytes to name
    }

    fun uploadExamFile(examId: String, uri: android.net.Uri) = online("تم رفع ملف الامتحان") {
        val (bytes, name) = readUri(uri)
        if (bytes.size > 20 * 1024 * 1024) throw OnlineApi.ApiFailure("حجم الملف كبير جداً", null)
        OnlineApi.uploadExamFile(examId, bytes, name)
    }

    /** Saves the exam, then uploads its PDF; one outcome message, as on the web (WEB-24). */
    fun saveExamWithFile(exam: Exam, pdf: android.net.Uri?) {
        viewModelScope.launch {
            val result = repository.saveExam(exam)
            if (pdf == null) { report(result, "تم حفظ الامتحان ✓"); addBookIfNew(exam.book); return@launch }
            when (result) {
                WriteResult.Sent -> try {
                    val (bytes, name) = readUri(pdf)
                    OnlineApi.uploadExamFile(exam.id, bytes, name)
                    showMessage("تم حفظ الامتحان ورفع الملف")
                    refreshFromServer()
                } catch (e: Exception) {
                    showMessage("تم حفظ بيانات الامتحان، لكن تعذر رفع ملف PDF: ${e.message}. ارفعه من بطاقة الامتحان.")
                }
                WriteResult.Queued -> {
                    report(result, "")
                    showMessage("تم حفظ الامتحان على الجهاز؛ ارفع ملف PDF من بطاقة الامتحان بعد الاتصال")
                }
                is WriteResult.Rejected -> report(result, "")
            }
            addBookIfNew(exam.book)
        }
    }

    private fun addBookIfNew(book: String) {
        if (book.isNotBlank() && com.example.data.remote.Permissions.can("books.write") && _books.value.none { it.name == book.trim() }) addBook(book)
    }

    // ---------------- users & profiles

    fun loadStaff() {
        viewModelScope.launch {
            _staffLoading.value = true
            try {
                if (com.example.data.remote.Permissions.can("users.read")) _staffUsers.value = OnlineApi.users()
                if (com.example.data.remote.Permissions.can("profiles.read")) _profiles.value = OnlineApi.profiles()
            } catch (e: OnlineApi.ApiFailure) {
                showMessage(e.message ?: "تعذر تحميل المستخدمين")
            } catch (_: Exception) {
            }
            _staffLoading.value = false
        }
    }

    fun saveStaffUser(body: Map<String, Any?>, success: String, onDone: (Boolean) -> Unit) =
        online(success, { ok -> onDone(ok); if (ok) loadStaff() }) { OnlineApi.saveUser(body) }

    fun deleteStaffUser(u: StaffUserDto) = online("تم حذف المستخدم", { ok -> if (ok) loadStaff() }) { OnlineApi.deleteUser(u.id) }

    fun resetStaffPassword(u: StaffUserDto, password: String, onDone: (Boolean) -> Unit) =
        online("تمت إعادة تعيين كلمة مرور ${u.name}. أبلغه بكلمة المرور المؤقتة؛ سيُطلب منه تغييرها عند الدخول.", { ok -> onDone(ok); if (ok) loadStaff() }, reload = false) {
            OnlineApi.resetPassword(u.id, password)
        }

    fun saveProfile(p: PermissionProfileDto, onDone: (Boolean) -> Unit) =
        online("تم حفظ البروفايل", { ok -> onDone(ok); if (ok) loadStaff() }, reload = false) { OnlineApi.saveProfile(p) }

    fun deleteProfile(p: PermissionProfileDto) = online("تم حذف البروفايل", { ok -> if (ok) loadStaff() }, reload = false) { OnlineApi.deleteProfile(p.id) }

    // ---------------- notifications

    fun markAllNotificationsRead() = online(null) { OnlineApi.markAllNotificationsRead() }

    fun sendNotification(title: String, description: String, type: String, committeeId: String?, onDone: (Boolean) -> Unit) =
        online("تم إرسال التنبيه", onDone) { OnlineApi.sendNotification(title, description, type, committeeId) }

    // ---------------- audit log

    fun loadAuditLogs(entity: String, reset: Boolean) {
        if (_auditLoading.value) return
        viewModelScope.launch {
            _auditLoading.value = true
            try {
                val before = if (reset) null else _auditLogs.value.lastOrNull()?.timestamp
                val page = OnlineApi.auditLogs(entity.ifBlank { null }, before, 50)
                _auditLogs.value = if (reset) page else _auditLogs.value + page
                _auditHasMore.value = page.size == 50
            } catch (e: OnlineApi.ApiFailure) {
                showMessage(e.message ?: "تعذر تحميل السجل")
            } catch (_: Exception) {
            }
            _auditLoading.value = false
        }
    }

    // ---------------- ceremony & stages from the ceremonies screens

    fun toggleCeremonyDone(committee: Committee) = toggleStage(committee.id, "ceremony", committee.stages.ceremony.completed)

    // ---------------- course sheet (web: SummerCourseTrackerView)

    fun saveSheetRow(row: SheetRowDto, onDone: (Boolean) -> Unit) = online(null, onDone) {
        val toSave = if (row.id.isBlank() || row.id.startsWith("committee:")) row.copy(id = "row-${java.util.UUID.randomUUID()}") else row
        OnlineApi.saveSheetRow(toSave)
    }

    fun deleteSheetRow(row: SheetRowDto) = online("تم حذف الصف رقم ${row.number}") { OnlineApi.deleteSheetRow(row.id) }

    /** Imports rows into a course; with [replace] the server deletes that course's old rows in the same transaction. */
    fun importSheetRows(rows: List<SheetRowDto>, courseId: String, replace: Boolean, onDone: (Boolean) -> Unit) =
        online(
            when {
                rows.isEmpty() && replace -> "تم تفريغ صفوف الكشف المحفوظة لهذه الدورة."
                replace -> "تم استبدال كشف الدورة بالكامل (${rows.size} صفاً)."
                else -> "تم إدراج ${rows.size} صفاً وحفظها في قاعدة البيانات."
            }, onDone
        ) { OnlineApi.saveSheetRowsBulk(rows.map { it.copy(courseId = courseId) }, if (replace) courseId else null) }

    /** Reads a sheet from a photo on the server; every row is marked "needs checking", as on the web. */
    fun extractTableFromImage(uri: android.net.Uri, onDone: (List<SheetRowDto>?) -> Unit) {
        viewModelScope.launch {
            try {
                val resolver = getApplication<Application>().contentResolver
                val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { resolver.openInputStream(uri)?.use { it.readBytes() } }
                    ?: throw OnlineApi.ApiFailure("تعذر قراءة الصورة", null)
                val mime = resolver.getType(uri) ?: "image/jpeg"
                val base64 = "data:$mime;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val extracted = OnlineApi.extractTable(base64, mime)
                fun num(v: Any?) = (v as? Number)?.toInt() ?: v?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                fun bool(v: Any?) = v == true || v?.toString() == "true"
                val rows = extracted.map { r ->
                    SheetRowDto(
                        id = "", number = num(r["number"]), sheikh_name = r["sheikh_name"]?.toString() ?: "",
                        place = r["place"]?.toString() ?: "", book = r["book"]?.toString() ?: "", students_count = num(r["students_count"]),
                        delivered = bool(r["delivered"]), corrected = bool(r["corrected"]), reviewed = bool(r["reviewed"]),
                        announced = bool(r["announced"]), ceremony = bool(r["ceremony"]), uncertain = true, notes = r["notes"]?.toString() ?: ""
                    )
                }
                if (rows.isEmpty()) showMessage("لم يتم العثور على صفوف في الصورة")
                onDone(rows.ifEmpty { null })
            } catch (e: OnlineApi.ApiFailure) {
                showMessage(e.message ?: "تعذر استخراج الجدول"); onDone(null)
            } catch (e: Exception) {
                showMessage("تعذر استخراج الجدول: ${e.message}"); onDone(null)
            }
        }
    }

    /** Creates the committee for a sheet row and opens it (web: handleOpenRowDetails / add row). */
    fun createCommitteeFromRow(committee: Committee) {
        viewModelScope.launch {
            val result = repository.saveCommittee(committee)
            report(result, "تم تسجيل اللجنة رقم ${committee.committeeNumber}")
            if (result !is WriteResult.Rejected) openCommitteeDetail(committee.id)
        }
    }

    // ---------------- committee detail extras

    fun updateQuestionDetails(committeeId: String, questionId: String, completed: Boolean, scorers: List<String>, score: Double?, notes: String?) =
        write("تم حفظ بيانات السؤال ✓") { repository.updateQuestionDetails(committeeId, questionId, completed, scorers, score, notes) }

    /** The imported grade sheet completes the sheet stage with a note; an empty student count is filled in (web). */
    fun recordSheetImport(committee: Committee, notes: String, studentsCount: Int?) {
        viewModelScope.launch {
            if (studentsCount != null && com.example.data.remote.Permissions.canOn("committees.update", committee.id)) {
                repository.saveCommittee(committee.copy(studentsCount = studentsCount))
            }
            report(repository.completeStageWithNotes(committee.id, "sheet", notes), "تم استيراد كشف الإكسيل بنجاح")
        }
    }

    fun saveExcelUrl(committee: Committee, url: String) =
        write("تم حفظ رابط ملف الإكسيل") { repository.saveCommitteeWithExcelUrl(committee, url) }
}
