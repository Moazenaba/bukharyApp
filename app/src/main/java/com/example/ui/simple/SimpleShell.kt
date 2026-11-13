package com.example.ui.simple

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppUser
import com.example.data.model.Committee
import com.example.data.model.NotificationItem
import com.example.data.model.ScheduleTask
import com.example.data.remote.AssignableUserDto
import com.example.data.remote.Permissions
import com.example.domain.NextStep
import com.example.domain.SimpleTab
import com.example.domain.TodayFeedBuilder
import com.example.ui.motion.MotionSpec
import com.example.ui.motion.rememberReducedMotion
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.tBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun iconOf(tab: SimpleTab): ImageVector = when (tab) {
    SimpleTab.TODAY -> Icons.Default.Today
    SimpleTab.COMMITTEES -> Icons.Default.Groups
    SimpleTab.CEREMONIES -> Icons.Default.Celebration
    SimpleTab.NOTIFICATIONS -> Icons.Default.Notifications
}

/**
 * The simple look for sheikhs and committee heads: four tabs, one account menu. The committee page
 * opens over the tabs; forms that already exist open through [legacyForm].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleShell(
    user: AppUser,
    committees: List<Committee>,
    tasks: List<ScheduleTask>,
    notifications: List<NotificationItem>,
    selectedCommitteeId: String?,
    candidates: List<AssignableUserDto>?,
    isOnline: Boolean,
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    onOpenCommittee: (String) -> Unit,
    onCloseCommittee: () -> Unit,
    onToggleTask: (String) -> Unit,
    onOneTap: (Committee, NextStep) -> Unit,
    onSaveStaff: (Committee, List<String>) -> Unit,
    onLoadCandidates: () -> Unit,
    onOpenNotification: (NotificationItem) -> Unit,
    onMarkAllRead: () -> Unit,
    onSync: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onFullView: () -> Unit,
    legacyForm: @Composable (Committee, Int, onClose: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var tabIndex by rememberSaveable { mutableStateOf(0) }
    var legacyTab by rememberSaveable(selectedCommitteeId) { mutableStateOf<Int?>(null) }
    val tab = SimpleTab.entries[tabIndex]
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val canOn: (String, String) -> Boolean = { a, id -> Permissions.canOn(a, id) }
    val feed = remember(committees, tasks, user.id, today) {
        TodayFeedBuilder.build(committees, tasks, user.id, canOn, today)
    }
    val unread = notifications.count { !it.read }
    val reduced = rememberReducedMotion()
    val selected = selectedCommitteeId?.let { id -> committees.find { it.id == id } }

    BackHandler(enabled = selected == null && tab != SimpleTab.TODAY) { tabIndex = 0 }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SimpleTopBar(user, isOnline, themeMode, onThemeMode, onSync, onChangePassword, onLogout, onFullView)
            Box(Modifier.weight(1f)) {
                AnimatedContent(targetState = tab, transitionSpec = { MotionSpec.fadeThrough(reduced) }, label = "simpleTab") { t ->
                    when (t) {
                        SimpleTab.TODAY -> TodayScreen(user.name, feed, today, onOpenCommittee, onToggleTask)
                        SimpleTab.COMMITTEES -> MyCommitteesScreen(committees, today, canOn, onOpenCommittee)
                        SimpleTab.CEREMONIES -> SimpleCeremoniesScreen(
                            committees, tasks, user.id, today, { Permissions.canOn("committees.finalize", it) }, onOpenCommittee
                        )
                        SimpleTab.NOTIFICATIONS -> SimpleNotificationsScreen(notifications, onOpenNotification, onMarkAllRead)
                    }
                }
            }
            NavigationBar {
                SimpleTab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = i == tabIndex,
                        onClick = { tabIndex = i },
                        icon = {
                            if (t == SimpleTab.NOTIFICATIONS && unread > 0) {
                                BadgedBox(badge = { Badge { Text(if (unread > 9) "9+" else unread.toString()) } }) { Icon(iconOf(t), null) }
                            } else Icon(iconOf(t), null)
                        },
                        label = { Text(t.label) }
                    )
                }
            }
        }

        if (selected != null) {
            val step = NextStep.of(selected) { a -> Permissions.canOn(a, selected.id) }
            val form = legacyTab
            Box(Modifier.fillMaxSize().padding(0.dp)) {
                if (form == null) {
                    SimpleCommitteeScreen(
                        committee = selected, step = step, today = today, myId = user.id,
                        candidates = candidates, onLoadCandidates = onLoadCandidates,
                        onSaveStaff = { ids -> onSaveStaff(selected, ids) },
                        onBack = onCloseCommittee,
                        onDoStep = { if (step.oneTap) onOneTap(selected, step) else legacyTab = legacyTabFor(step.kind) },
                        onOpenForm = { legacyTab = it },
                        modifier = Modifier.fillMaxSize().let { it }
                    )
                } else {
                    legacyForm(selected, form) { legacyTab = null }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTopBar(
    user: AppUser,
    isOnline: Boolean,
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    onSync: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onFullView: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text("مسابقات الإمام البخاري", fontWeight = FontWeight.Bold) },
        actions = {
            Box {
                IconButton(onClick = { open = true }, modifier = Modifier.padding(0.dp)) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "حسابي")
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    DropdownMenuItem(
                        text = { Text("${user.name} — ${user.role.titleAr}", fontWeight = FontWeight.Bold) },
                        onClick = { open = false }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isOnline) "متصل · مزامنة الآن" else "غير متصل · محاولة المزامنة") },
                        leadingIcon = { Icon(Icons.Default.Sync, null) },
                        onClick = { open = false; onSync() }
                    )
                    DropdownMenuItem(
                        text = { Text("المظهر: ${themeMode.label}") },
                        onClick = { onThemeMode(ThemeMode.entries[(themeMode.ordinal + 1) % ThemeMode.entries.size]) }
                    )
                    DropdownMenuItem(text = { Text("تغيير كلمة السر") }, onClick = { open = false; onChangePassword() })
                    DropdownMenuItem(text = { Text("العرض الكامل") }, onClick = { open = false; onFullView() })
                    DropdownMenuItem(text = { Text("تسجيل الخروج") }, onClick = { open = false; onLogout() })
                }
            }
        }
    )
}
