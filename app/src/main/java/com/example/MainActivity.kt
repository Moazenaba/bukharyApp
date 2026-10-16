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
import com.example.ui.screens.assistants.AssistantsScreen
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BukhariTheme {
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
                val customPermissions by viewModel.customPermissions.collectAsStateWithLifecycle()
                val sheetRows by viewModel.sheetRows.collectAsStateWithLifecycle()
                val databaseStatus by viewModel.databaseStatus.collectAsStateWithLifecycle()
                val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
                val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsStateWithLifecycle()
                val lastSyncOk by viewModel.lastSyncOk.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                val selectedStageFilter by viewModel.selectedStageFilter.collectAsStateWithLifecycle()
                val snackbarMessage by viewModel.snackBarMessage.collectAsStateWithLifecycle()

                val isWide = LocalConfiguration.current.screenWidthDp >= 600
                val onScreenSelected: (AppNavScreen) -> Unit = { screen ->
                    if (currentUser?.canAccessScreen(screen) == true) viewModel.navigateTo(screen)
                    else viewModel.showMessage("هذه الشاشة غير متاحة لدورك")
                }
                val snackbarHostState = remember { SnackbarHostState() }
                val defaultCourse = courses.find { it.isDefault } ?: courses.find { it.status == "active" }

                LaunchedEffect(snackbarMessage) {
                    snackbarMessage?.let { msg ->
                        viewModel.clearMessage()
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                BackHandler(enabled = isLoggedIn && (selectedCommitteeId != null || currentScreen != AppNavScreen.HOME)) {
                    if (selectedCommitteeId != null) viewModel.closeCommitteeDetail()
                    else viewModel.navigateTo(AppNavScreen.HOME)
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
                                onLogout = { viewModel.logout() }
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
                                        onOpenAddCommittee = { viewModel.navigateTo(AppNavScreen.COMMITTEES) }
                                    )
                                    AppNavScreen.COURSE_TRACKER -> SummerCourseTrackerScreen(
                                        courses = courses,
                                        selectedCourseId = selectedCourseId,
                                        committees = committees,
                                        sheetRows = sheetRows,
                                        onSelectCourse = { viewModel.setSelectedCourseId(it) },
                                        onToggleStage = { commId, stageKey, curr -> viewModel.toggleStage(commId, stageKey, curr) },
                                        onOpenCommitteeDetail = { viewModel.openCommitteeDetail(it) }
                                    )
                                    AppNavScreen.COMMITTEES -> CommitteesScreen(
                                        currentUser = currentUser,
                                        committees = filteredCommittees,
                                        activeCourses = courses,
                                        selectedStageFilter = selectedStageFilter,
                                        searchQuery = searchQuery,
                                        onSearchChange = { viewModel.setSearchQuery(it) },
                                        onStageFilterChange = { viewModel.setSelectedStageFilter(it) },
                                        onOpenCommitteeDetail = { viewModel.openCommitteeDetail(it) },
                                        onSaveCommittee = { viewModel.saveCommittee(it) },
                                        onDeleteCommittee = { viewModel.deleteCommittee(it) }
                                    )
                                    AppNavScreen.EXAMS -> ExamsScreen(
                                        currentUser = currentUser,
                                        exams = exams,
                                        onSaveExam = { viewModel.saveExam(it) },
                                        onDeleteExam = { viewModel.deleteExam(it) },
                                        onOpenFile = { exam -> viewModel.openExamFile(exam) { openPdf(it) } }
                                    )
                                    AppNavScreen.SCHEDULE -> ScheduleScreen(
                                        currentUser = currentUser,
                                        tasks = tasks,
                                        onToggleTask = { viewModel.toggleTask(it) },
                                        onSaveTask = { viewModel.saveTask(it) },
                                        onDeleteTask = { viewModel.deleteTask(it) }
                                    )
                                    AppNavScreen.ASSISTANTS -> AssistantsScreen(
                                        currentUser = currentUser,
                                        users = users,
                                        customPermissions = customPermissions,
                                        onApproveUser = { user, comms, perms, role -> viewModel.approveUser(user, comms, perms, role) },
                                        onRejectUser = { user -> viewModel.rejectUser(user) },
                                        onCreateCustomPermission = { name, desc, _, _ -> viewModel.createCustomPermission(name, desc) }
                                    )
                                    AppNavScreen.DATABASE_INSPECTOR -> DatabaseInspectorScreen(
                                        databaseStatus = databaseStatus,
                                        isSyncing = isSyncing,
                                        isNetworkAvailable = isNetworkAvailable,
                                        onRefresh = { viewModel.syncNow() },
                                        onLoadStatus = { viewModel.refreshDatabaseStatus() },
                                        onRetryFailed = { viewModel.retryFailedWrites() },
                                        onDiscardFailed = { viewModel.discardFailedWrites() }
                                    )
                                    AppNavScreen.STATISTICS -> StatisticsScreen(committees = committees)
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
                                        onAssistantReception = { phone -> viewModel.recordAssistantReception(selectedCommittee.id, phone) },
                                        onApproveExam = { viewModel.approveExamReadiness(selectedCommittee.id) },
                                        onUpdateScoring = { viewModel.updateScoring(selectedCommittee.id, it) },
                                        onUpdateAggregation = { viewModel.updateAggregation(selectedCommittee.id, it) },
                                        onUpdateCeremony = { viewModel.updateCeremony(selectedCommittee.id, it) }
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
