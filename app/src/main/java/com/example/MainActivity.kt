package com.example

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.platform.LocalConfiguration
import com.example.ui.components.BukhariNavigationRail
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppNavScreen
import com.example.ui.MainViewModel
import com.example.ui.components.BukhariBottomNavBar
import com.example.ui.components.BukhariTopBar
import com.example.ui.screens.committees.CommitteeDetailModal
import com.example.ui.screens.committees.CommitteesScreen
import com.example.ui.screens.course_tracker.SummerCourseTrackerScreen
import com.example.ui.screens.database.DatabaseInspectorScreen
import com.example.ui.screens.exams.ExamsScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.login.CentralLoginPortal
import com.example.ui.screens.schedule.ScheduleScreen
import com.example.ui.screens.stats.StatisticsScreen
import com.example.ui.theme.BukhariTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** Opens a downloaded exam PDF in the user's PDF viewer. */
    private fun openPdf(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            viewModel.showMessage("لا يوجد تطبيق لعرض ملفات PDF على هذا الجهاز")
        }
    }

    private val askNotifications = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(com.example.push.PushNotifications.EXTRA_OPEN_NOTIFICATIONS, false)) {
            viewModel.navigateTo(com.example.ui.AppNavScreen.NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.push.PushNotifications.init(this)
        val openNotifications = intent?.getBooleanExtra(com.example.push.PushNotifications.EXTRA_OPEN_NOTIFICATIONS, false) == true

        setContent {
            BukhariTheme(lightBackgroundBehindStatusBar = !viewModel.isLoggedIn.collectAsStateWithLifecycle().value) {
                val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
                val isRestoring by viewModel.isRestoringSession.collectAsStateWithLifecycle()
                val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                val currentScreen by viewModel.currentNavScreen.collectAsStateWithLifecycle()
                val selectedCommitteeId by viewModel.selectedCommitteeIdForDetail.collectAsStateWithLifecycle()

                val courses by viewModel.courses.collectAsStateWithLifecycle()
                val selectedCourseId by viewModel.selectedCourseId.collectAsStateWithLifecycle()
                val committees by viewModel.committees.collectAsStateWithLifecycle()
                val filteredCommittees by viewModel.filteredCommittees.collectAsStateWithLifecycle()
                val tasks by viewModel.tasks.collectAsStateWithLifecycle()
                val exams by viewModel.exams.collectAsStateWithLifecycle()
                val users by viewModel.users.collectAsStateWithLifecycle()
                val notifications by viewModel.notifications.collectAsStateWithLifecycle()
                val unsentCount by viewModel.unsentCount.collectAsStateWithLifecycle()
                val logoutConfirmation by viewModel.logoutConfirmation.collectAsStateWithLifecycle()
                val foreignUnsentWork by viewModel.foreignUnsentWork.collectAsStateWithLifecycle()
                val assignableUsers by viewModel.assignableUsers.collectAsStateWithLifecycle()
                var showChangePassword by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
                val sheetRows by viewModel.sheetRows.collectAsStateWithLifecycle()
                val databaseStatus by viewModel.databaseStatus.collectAsStateWithLifecycle()
                val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
                val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsStateWithLifecycle()
                val lastSyncOk by viewModel.lastSyncOk.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                val selectedStageFilter by viewModel.selectedStageFilter.collectAsStateWithLifecycle()
                val snackbarMessage by viewModel.snackBarMessage.collectAsStateWithLifecycle()
                val books by viewModel.books.collectAsStateWithLifecycle()
                val staffUsers by viewModel.staffUsers.collectAsStateWithLifecycle()
                val profiles by viewModel.profiles.collectAsStateWithLifecycle()
                val staffLoading by viewModel.staffLoading.collectAsStateWithLifecycle()
                val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
                val auditLoading by viewModel.auditLoading.collectAsStateWithLifecycle()
                val auditHasMore by viewModel.auditHasMore.collectAsStateWithLifecycle()
                val committeeFilters by viewModel.committeeFilters.collectAsStateWithLifecycle()
                val confirmRequest by viewModel.confirmRequest.collectAsStateWithLifecycle()
                var openAddCommittee by remember { androidx.compose.runtime.mutableStateOf(false) }
                var showExport by remember { androidx.compose.runtime.mutableStateOf(false) }

                val isWide = LocalConfiguration.current.screenWidthDp >= 600
                val onScreenSelected: (AppNavScreen) -> Unit = { screen ->
                    if (currentUser?.canAccessScreen(screen) == true) viewModel.navigateTo(screen)
                    else viewModel.showMessage("هذه الشاشة غير متاحة لدورك")
                }
                val snackbarHostState = remember { SnackbarHostState() }
                val defaultCourse = courses.find { it.isDefault } ?: courses.find { it.status == "active" }

                // Once signed in: ask for notification permission (Android 13+) and honour a tapped push.
                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        if (com.example.push.PushNotifications.isConfigured && com.example.push.PushNotifications.needsPermission(this@MainActivity) &&
                            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                        ) askNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        if (openNotifications) viewModel.navigateTo(AppNavScreen.NOTIFICATIONS)
                    }
                }

                val dialogError by viewModel.dialogError.collectAsStateWithLifecycle()
                LaunchedEffect(dialogError) {
                    dialogError?.let {
                        android.widget.Toast.makeText(this@MainActivity, it, android.widget.Toast.LENGTH_LONG).show()
                        viewModel.clearDialogError()
                    }
                }

                LaunchedEffect(snackbarMessage) {
                    snackbarMessage?.let { msg ->
                        viewModel.clearMessage()
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                // Logout with unsent writes: the user chooses; nothing is deleted without consent (MOB-02).
                logoutConfirmation?.let { count ->
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { viewModel.cancelLogout() },
                        title = { Text("تعديلات لم تُرسل بعد") },
                        text = {
                            Text("لديك $count من التعديلات لم تصل إلى الخادم. يمكنك إرسالها الآن، أو الخروج مع إبقائها على الجهاز لتُرسل بعد دخولك مرة أخرى، أو حذفها.")
                        },
                        confirmButton = {
                            Column(horizontalAlignment = Alignment.End) {
                                androidx.compose.material3.TextButton(onClick = { viewModel.syncThenLogout() }) { Text("إرسال ثم الخروج") }
                                androidx.compose.material3.TextButton(onClick = { viewModel.logout(discardUnsent = false) }) { Text("الخروج مع الإبقاء عليها") }
                                androidx.compose.material3.TextButton(onClick = { viewModel.logout(discardUnsent = true) }) {
                                    Text("حذفها والخروج", color = androidx.compose.ui.graphics.Color(0xFFB91C1C))
                                }
                            }
                        },
                        dismissButton = { androidx.compose.material3.TextButton(onClick = { viewModel.cancelLogout() }) { Text("إلغاء") } }
                    )
                }

                confirmRequest?.let { req ->
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { viewModel.dismissConfirm() },
                        title = { Text(req.title) },
                        text = { Text(req.message) },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { viewModel.dismissConfirm(); req.onConfirm() }) {
                                Text(req.confirmLabel, color = androidx.compose.ui.graphics.Color(0xFFB91C1C))
                            }
                        },
                        dismissButton = { androidx.compose.material3.TextButton(onClick = { viewModel.dismissConfirm() }) { Text("إلغاء") } }
                    )
                }

                if (showExport) {
                    com.example.ui.screens.export.ExportDialog(
                        committees = committees,
                        courses = courses,
                        sheetRows = sheetRows,
                        onDismiss = { showExport = false }
                    )
                }

                // Another account left unsent work on this device: it is never sent as this user (MOB-01).
                if (foreignUnsentWork) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { viewModel.keepForeignUnsentWork() },
                        title = { Text("تعديلات حساب آخر") },
                        text = { Text("على هذا الجهاز تعديلات لم تُرسل من حساب آخر. لن تُرسل باسمك؛ تبقى حتى يدخل صاحبها، أو يمكنك حذفها.") },
                        confirmButton = { androidx.compose.material3.TextButton(onClick = { viewModel.keepForeignUnsentWork() }) { Text("إبقاؤها") } },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { viewModel.discardForeignUnsentWork() }) {
                                Text("حذفها", color = androidx.compose.ui.graphics.Color(0xFFB91C1C))
                            }
                        }
                    )
                }

                BackHandler(enabled = isLoggedIn && (selectedCommitteeId != null || currentScreen != AppNavScreen.HOME)) {
                    if (selectedCommitteeId != null) viewModel.closeCommitteeDetail()
                    else viewModel.navigateBack()
                }

                // One Scaffold for every state, so messages (e.g. login errors) are always visible.
                Scaffold(
                    topBar = {
                        if (isLoggedIn) {
                            BukhariTopBar(
                                currentUser = currentUser,
                                notifications = notifications,
                                isSyncing = isSyncing,
                                isOnline = isNetworkAvailable && lastSyncOk,
                                activeCourseName = defaultCourse?.name,
                                onSync = { viewModel.syncNow() },
                                onNavigate = { screen -> viewModel.navigateTo(screen) },
                                onMarkNotificationRead = { id -> viewModel.markNotificationRead(id) },
                                onLogout = { viewModel.requestLogout() },
                                onChangePassword = { showChangePassword = true }
                            )
                        }
                    },
                    bottomBar = {
                        if (isLoggedIn && !isWide) {
                            BukhariBottomNavBar(
                                currentUser = currentUser,
                                currentScreen = currentScreen,
                                onScreenSelected = { screen ->
                                    if (currentUser?.canAccessScreen(screen) == true) viewModel.navigateTo(screen)
                                    else viewModel.showMessage("هذه الشاشة غير متاحة لدورك")
                                }
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.fillMaxSize().imePadding()
                ) { innerPadding ->
                    when {
                        isRestoring -> Column(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Text("جارٍ التحقق من الجلسة...")
                        }

                        !isLoggedIn -> CentralLoginPortal(
                            onLogin = { ident, pass -> viewModel.login(ident, pass) },
                            onRegisterRequest = { name, phone, email, role, pass, notes, onDone ->
                                viewModel.registerJoinRequest(name, phone, email, role, pass, notes, onDone)
                            },
                            onTrackRequest = { code, onResult -> viewModel.trackJoinRequest(code, onResult) },
                            isSubmitting = isSubmitting,
                            unsentCount = unsentCount,
                            loginError = viewModel.loginError.collectAsStateWithLifecycle().value,
                            modifier = Modifier.padding(innerPadding)
                        )

                        // After an admin reset nothing else opens until a new password is chosen (WEB-02, MOB-08).
                        currentUser?.mustChangePassword == true || showChangePassword -> com.example.ui.screens.account.ChangePasswordScreen(
                            forced = currentUser?.mustChangePassword == true,
                            isSubmitting = isSubmitting,
                            onSubmit = { current, next, onDone ->
                                viewModel.changePassword(current, next) { ok ->
                                    if (ok) showChangePassword = false
                                    onDone(ok)
                                }
                            },
                            onCancel = {
                                if (currentUser?.mustChangePassword == true) viewModel.requestLogout() else showChangePassword = false
                            },
                            modifier = Modifier.padding(innerPadding)
                        )

                        else -> Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            if (isWide) {
                                BukhariNavigationRail(
                                    currentUser = currentUser,
                                    currentScreen = currentScreen,
                                    onScreenSelected = onScreenSelected
                                )
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "ScreenTransition"
                            ) { screen ->
                                when (screen) {
                                    AppNavScreen.HOME -> HomeScreen(
                                        currentUser = currentUser,
                                        activeCourse = courses.find { it.id == selectedCourseId } ?: defaultCourse,
                                        committees = committees,
                                        tasks = tasks,
                                        onNavigate = { viewModel.navigateTo(it) },
                                        onOpenCommitteeDetail = { viewModel.openCommitteeDetail(it) },
                                        onToggleTask = { viewModel.toggleTask(it) },
                                        onNavigateCommittees = { stage ->
                                            viewModel.setSelectedStageFilter(stage)
                                            viewModel.navigateTo(AppNavScreen.COMMITTEES)
                                        },
                                        onOpenAddCommittee = {
                                            openAddCommittee = true
                                            viewModel.navigateTo(AppNavScreen.COMMITTEES)
                                        }
                                    )
                                    AppNavScreen.COURSE_TRACKER -> SummerCourseTrackerScreen(
                                        courses = courses,
                                        selectedCourseId = selectedCourseId,
                                        committees = committees,
                                        sheetRows = sheetRows,
                                        onSelectCourse = { viewModel.setSelectedCourseId(it) },
                                        onSaveRow = { row, done -> viewModel.saveSheetRow(row, done) },
                                        onDeleteRow = { viewModel.deleteSheetRow(it) },
                                        onImportRows = { rows, courseId, replace, done -> viewModel.importSheetRows(rows, courseId, replace, done) },
                                        onExtractFromImage = { uri, done -> viewModel.extractTableFromImage(uri, done) },
                                        onAddCourse = { course, done -> viewModel.saveCourse(course, done) },
                                        onCreateCommitteeFromRow = { viewModel.createCommitteeFromRow(it) },
                                        onExport = { showExport = true },
                                        onToggleStage = { commId, stageKey, curr -> viewModel.toggleStage(commId, stageKey, curr) },
                                        onOpenCommitteeDetail = { viewModel.openCommitteeDetail(it) }
                                    )
                                    AppNavScreen.COMMITTEES -> CommitteesScreen(
                                        currentUser = currentUser,
                                        committees = filteredCommittees,
                                        allCommittees = committees,
                                        activeCourses = courses,
                                        selectedStageFilter = selectedStageFilter,
                                        searchQuery = searchQuery,
                                        onSearchChange = { viewModel.setSearchQuery(it) },
                                        onStageFilterChange = { viewModel.setSelectedStageFilter(it) },
                                        onOpenCommitteeDetail = { viewModel.openCommitteeDetail(it) },
                                        onSaveCommittee = { viewModel.saveCommittee(it) },
                                        onDeleteCommittee = { viewModel.deleteCommittee(it) },
                                        selectedCourseId = selectedCourseId,
                                        onCourseFilterChange = { viewModel.setSelectedCourseId(it) },
                                        filters = committeeFilters,
                                        onFiltersChange = { viewModel.setCommitteeFilters(it) },
                                        onReceive = { id, cancel -> viewModel.recordAssistantReception(id, "", cancel) },
                                        onApproveReadiness = { id, revoke -> viewModel.approveExamReadiness(id, revoke) },
                                        openAddRequested = openAddCommittee,
                                        onCreateCourse = if (com.example.data.remote.Permissions.can("courses.write")) ({ c -> viewModel.saveCourse(c) }) else null,
                                        onAddRequestConsumed = { openAddCommittee = false }
                                    )
                                    AppNavScreen.EXAMS -> ExamsScreen(
                                        currentUser = currentUser,
                                        exams = exams,
                                        onSaveExam = { exam, pdf -> viewModel.saveExamWithFile(exam, pdf) },
                                        onDeleteExam = { viewModel.deleteExam(it) },
                                        onOpenFile = { exam -> viewModel.openExamFile(exam) { openPdf(it) } },
                                        books = books,
                                        onUploadFile = { id, uri -> viewModel.uploadExamFile(id, uri) },
                                        onAddBook = { viewModel.addBook(it) },
                                        onDeleteBook = { viewModel.deleteBook(it) }
                                    )
                                    AppNavScreen.SCHEDULE -> ScheduleScreen(
                                        currentUser = currentUser,
                                        tasks = tasks,
                                        committees = committees,
                                        assignableUsers = assignableUsers,
                                        onLoadAssignableUsers = { viewModel.loadAssignableUsers() },
                                        onToggleTask = { viewModel.toggleTask(it) },
                                        onSaveTask = { viewModel.saveTask(it) },
                                        onDeleteTask = { viewModel.deleteTask(it) },
                                        onScheduleCeremony = { c, d -> viewModel.updateCeremony(c.id, d) },
                                        onOpenCommittee = { viewModel.openCommitteeDetail(it) }
                                    )
                                    AppNavScreen.ASSISTANTS -> com.example.ui.screens.assistants.UsersManagementScreen(
                                        currentUser = currentUser,
                                        users = staffUsers,
                                        profiles = profiles,
                                        committees = committees,
                                        exams = exams,
                                        isLoading = staffLoading,
                                        onReload = { viewModel.loadStaff() },
                                        onSaveUser = { _, body, msg, done -> viewModel.saveStaffUser(body, msg, done) },
                                        onDeleteUser = { viewModel.deleteStaffUser(it) },
                                        onResetPassword = { u, pw, done -> viewModel.resetStaffPassword(u, pw, done) },
                                        onSaveProfile = { p, done -> viewModel.saveProfile(p, done) },
                                        onDeleteProfile = { viewModel.deleteProfile(it) },
                                        onOpenCommittee = { viewModel.openCommitteeDetail(it) }
                                    )
                                    AppNavScreen.DATABASE_INSPECTOR -> DatabaseInspectorScreen(
                                        databaseStatus = databaseStatus,
                                        isSyncing = isSyncing,
                                        isNetworkAvailable = isNetworkAvailable,
                                        onRefresh = { viewModel.syncNow() },
                                        onLoadStatus = { viewModel.refreshDatabaseStatus() },
                                        onRetryFailed = { viewModel.retryFailedWrites() },
                                        onDiscardFailed = { viewModel.discardFailedWrites() },
                                        onRetryRefused = { viewModel.retryRefusedWrite(it) },
                                        onDiscardRefused = { viewModel.discardRefusedWrite(it) }
                                    )
                                    AppNavScreen.STATISTICS -> StatisticsScreen(committees = committees)
                                    AppNavScreen.COURSES -> com.example.ui.screens.courses.CoursesScreen(
                                        courses = courses,
                                        committees = committees,
                                        onSaveCourse = { c, done -> viewModel.saveCourse(c, done) },
                                        onDeleteCourse = { viewModel.deleteCourse(it) },
                                        onSetDefaultCourse = { viewModel.setDefaultCourse(it) },
                                        onShowCourseCommittees = {
                                            viewModel.setSelectedCourseId(it)
                                            viewModel.navigateTo(AppNavScreen.COMMITTEES)
                                        }
                                    )
                                    AppNavScreen.CEREMONIES -> com.example.ui.screens.ceremonies.CeremoniesScreen(
                                        committees = committees,
                                        tasks = tasks,
                                        onScheduleCeremony = { c, d -> viewModel.updateCeremony(c.id, d) },
                                        onToggleCeremonyDone = { viewModel.toggleCeremonyDone(it) },
                                        onOpenCommittee = { viewModel.openCommitteeDetail(it) },
                                        onNavigateDailyAlerts = { viewModel.navigateTo(AppNavScreen.DAILY_ALERTS) },
                                        onNavigateCalendar = { viewModel.navigateTo(AppNavScreen.SCHEDULE) }
                                    )
                                    AppNavScreen.DAILY_ALERTS -> com.example.ui.screens.alerts.DailyAlertsScreen(
                                        currentUser = currentUser,
                                        tasks = tasks,
                                        committees = committees,
                                        assignableUsers = assignableUsers,
                                        onLoadAssignableUsers = { viewModel.loadAssignableUsers() },
                                        onToggleTask = { viewModel.toggleTask(it) },
                                        onSaveTask = { viewModel.saveTask(it) },
                                        onDeleteTask = { viewModel.deleteTask(it) },
                                        onScheduleCeremony = { c, d -> viewModel.updateCeremony(c.id, d) },
                                        onNavigateCalendar = { viewModel.navigateTo(AppNavScreen.SCHEDULE) },
                                        onNavigateCeremonies = { viewModel.navigateTo(AppNavScreen.CEREMONIES) }
                                    )
                                    AppNavScreen.AUDIT -> com.example.ui.screens.audit.AuditLogScreen(
                                        logs = auditLogs,
                                        isLoading = auditLoading,
                                        hasMore = auditHasMore,
                                        onLoad = { entity, reset -> viewModel.loadAuditLogs(entity, reset) }
                                    )
                                    AppNavScreen.NOTIFICATIONS -> com.example.ui.screens.notifications.NotificationsScreen(
                                        notifications = notifications,
                                        committees = committees,
                                        onMarkRead = { viewModel.markNotificationRead(it) },
                                        onMarkAllRead = { viewModel.markAllNotificationsRead() },
                                        onOpenCommittee = { viewModel.openCommitteeDetail(it) },
                                        onSend = { t, d, type, cId, done -> viewModel.sendNotification(t, d, type, cId, done) }
                                    )
                                    AppNavScreen.MENU -> com.example.ui.screens.menu.MenuScreen(
                                        currentUser = currentUser,
                                        committees = committees,
                                        exams = exams,
                                        courses = courses,
                                        tasks = tasks,
                                        pendingRequests = staffUsers.count { it.status == "pending" },
                                        unreadNotifications = notifications.count { !it.read },
                                        onNavigate = { viewModel.navigateTo(it) },
                                        onAddCommittee = {
                                            openAddCommittee = true
                                            viewModel.navigateTo(AppNavScreen.COMMITTEES)
                                        },
                                        onExport = { showExport = true },
                                        onChangePassword = { showChangePassword = true },
                                        onLogout = { viewModel.requestLogout() }
                                    )
                                }
                            }

                            selectedCommitteeId?.let { commId ->
                                committees.find { it.id == commId }?.let { selectedCommittee ->
                                    // Fresh form state for each committee (no values carried over from the previous one).
                                    key(selectedCommittee.id) { CommitteeDetailModal(
                                        committee = selectedCommittee,
                                        currentUser = currentUser,
                                        onClose = { viewModel.closeCommitteeDetail() },
                                        onToggleStage = { stageKey, curr -> viewModel.toggleStage(selectedCommittee.id, stageKey, curr) },
                                        onAssistantReception = { phone, cancel -> viewModel.recordAssistantReception(selectedCommittee.id, phone, cancel) },
                                        onApproveExam = { revoke -> viewModel.approveExamReadiness(selectedCommittee.id, revoke) },
                                        onUpdateQuestion = { qId, done -> viewModel.updateQuestion(selectedCommittee.id, qId, done) },
                                        onUpdateScoring = { viewModel.updateScoring(selectedCommittee.id, it) },
                                        onUpdateAggregation = { viewModel.updateAggregation(selectedCommittee.id, it) },
                                        onUpdateCeremony = { viewModel.updateCeremony(selectedCommittee.id, it) },
                                        courses = courses,
                                        onSaveBase = { viewModel.saveCommittee(it) },
                                        onDelete = { viewModel.deleteCommittee(selectedCommittee.id) },
                                        onUpdateQuestionDetails = { qId, done, scorers, score, notes -> viewModel.updateQuestionDetails(selectedCommittee.id, qId, done, scorers, score, notes) },
                                        onSheetImported = { notes, count -> viewModel.recordSheetImport(selectedCommittee, notes, count) },
                                        onSaveExcelUrl = { url -> viewModel.saveExcelUrl(selectedCommittee, url) }
                                    ) }
                                }
                            }
                            }
                        }
                    }
                }
            }
        }
    }
}
