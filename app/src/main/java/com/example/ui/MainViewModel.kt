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
import com.example.data.remote.AuthResult
import com.example.data.remote.AuthenticationService
import com.example.data.remote.SheetRowDto
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
    STATISTICS("statistics", "الإحصائيات الشاملة", "bar_chart")
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

    val filteredCommittees: StateFlow<List<Committee>> = combine(
        committees, _searchQuery, _selectedStageFilter, _selectedCourseId
    ) { commList, query, stageFilter, courseId ->
        commList.filter { committee ->
            val matchesCourse = courseId == null || committee.courseId == courseId
            val matchesQuery = query.isBlank() ||
                committee.committeeName.contains(query, ignoreCase = true) ||
                committee.sheikhName.contains(query, ignoreCase = true) ||
                committee.competitionBook.contains(query, ignoreCase = true) ||
                committee.committeeNumber.toString() == query.trim()
            val matchesStage = stageFilter == "all" || committee.stages.getStage(stageFilter).completed
            matchesCourse && matchesQuery && matchesStage
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                repository.clearAllData()
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
            _databaseStatus.value = repository.getDatabaseStatus()
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
                    // Never show a previous user's cached data.
                    repository.clearAllData()
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

    fun logout() {
        viewModelScope.launch { endSession("تم تسجيل الخروج", revoke = true) }
    }

    /** Ends the session and removes this user's data and unsent changes from the device. */
    private suspend fun endSession(message: String, revoke: Boolean) {
        if (!_isLoggedIn.value && _currentUser.value == null) return
        _isLoggedIn.value = false
        _currentUser.value = null
        closeCommitteeDetail()
        if (revoke) authService.logout() else authService.forgetSession()
        repository.clearAllData()
        _databaseStatus.value = null
        showMessage(message)
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

    fun recordAssistantReception(committeeId: String, phone: String) {
        val me = _currentUser.value ?: return
        write("تم توثيق استلام اللجنة ✓") { repository.recordAssistantHandover(committeeId, me, phone) }
    }

    fun approveExamReadiness(committeeId: String) {
        val me = _currentUser.value ?: return
        write("تم اعتماد جاهزية الامتحان ✓") { repository.approveExamReadiness(committeeId, me) }
    }

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
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { file.writeBytes(bytes) }
            onReady(file)
        }
    }

    // ---------------- tasks

    fun saveTask(task: ScheduleTask) = write("تمت جدولة المهمة ✓") { repository.saveTask(task) }

    fun toggleTask(taskId: String) = write("تم تحديث حالة المهمة") { repository.toggleTask(taskId) }

    fun deleteTask(taskId: String) = write("تم حذف المهمة") { repository.deleteTask(taskId) }

    // ---------------- users & permissions

    fun approveUser(user: AppUser, committees: List<Int>, permissions: List<String>, role: UserRole) =
        write("تم اعتماد حساب ${user.name} ✓") { repository.approveUser(user.id, committees, permissions, role) }

    fun rejectUser(user: AppUser, reason: String? = null) =
        write("تم تسجيل الاعتذار عن طلب ${user.name}") { repository.rejectUser(user.id, reason) }

    fun createCustomPermission(name: String, description: String) =
        write("تمت إضافة الصلاحية المخصصة ($name)") { repository.saveCustomPermission(name, description) }

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
}
