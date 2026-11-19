package com.example.ui.v2

import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import kotlinx.coroutines.launch
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.selection.selectable
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppUser
import com.example.data.model.Committee
import com.example.data.model.Course
import com.example.data.model.Exam
import com.example.data.model.NotificationItem
import com.example.data.model.ScheduleTask
import com.example.data.remote.AssignableUserDto
import com.example.data.remote.Permissions
import com.example.data.remote.StaffUserDto
import com.example.domain.NextStep
import com.example.domain.v2.V2Stages
import com.example.domain.v2.V2Tab
import com.example.domain.v2.V2Tool
import com.example.domain.v2.V2Tools

/** Everything the new screens read. They hold no data of their own. */
data class V2Data(
    val user: AppUser,
    val committees: List<Committee>,
    val tasks: List<ScheduleTask>,
    val exams: List<Exam>,
    val notifications: List<NotificationItem>,
    val staff: List<StaffUserDto>,
    val staffLoading: Boolean,
    val candidates: List<AssignableUserDto>?,
    val courses: List<Course>,
    val selectedCourseId: String?,
    val isOnline: Boolean,
    val today: String,
    /** The chosen colour theme (V2Themes key, or "auto"). */
    val themeKey: String = "auto"
)

/** Everything the new screens can do; each goes through the same view model calls the classic app uses. */
class V2Actions(
    val oneTap: (Committee, NextStep) -> Unit,
    val toggleTask: (String) -> Unit,
    val openExam: (Exam) -> Unit,
    val readNotification: (NotificationItem) -> Unit,
    val readAll: () -> Unit,
    /** Syncs now; the callback runs when it is really done. */
    val sync: (done: () -> Unit) -> Unit,
    val changePassword: () -> Unit,
    val logout: () -> Unit,
    val leaveV2: () -> Unit,
    /** Opens a classic screen (audit log, database) by leaving V2 for the full app. */
    val openClassic: (com.example.ui.AppNavScreen) -> Unit,
    val saveStaff: (Committee, List<String>) -> Unit,
    val loadCandidates: () -> Unit,
    val loadStaff: () -> Unit,
    /** Approves a join request with the role and committees picked by the admin («all» when [committeeIds] is null). */
    val approve: (StaffUserDto, role: String, committeeIds: List<String>?) -> Unit,
    val reject: (StaffUserDto) -> Unit,
    val sendNotice: (title: String, body: String, done: (Boolean) -> Unit) -> Unit,
    val selectCourse: (String?) -> Unit,
    val v2Theme: (String) -> Unit = {}
)

/** Pages that open on top of a tab (with a back arrow). */
enum class V2Page(val title: String) { INBOX("الإشعارات"), EXAMS("الامتحانات"), CALENDAR("التقويم"), ACCOUNT("حسابي") }

@Composable
fun V2Shell(
    data: V2Data,
    actions: V2Actions,
    selectedCommitteeId: String?,
    onOpenCommittee: (String) -> Unit,
    onCloseCommittee: () -> Unit,
    /** The long classic forms (scoring, sheet, full edit), opened from inside a V2 committee. */
    legacyForm: @Composable (Committee, Int, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalV2.current
    val role = data.user.role.roleKey
    val tabs = V2Tools.tabs(role)
    val observing = Permissions.observing
    val tools = V2Tools.tools({ Permissions.can(it) }, observesExam = observing.any { it.task == "exam" }, observesAny = observing.isNotEmpty())

    var tab by rememberSaveable { mutableStateOf(tabs.first()) }
    if (tab !in tabs) tab = tabs.first()
    var page by rememberSaveable { mutableStateOf<V2Page?>(null) }
    var filter by rememberSaveable { mutableStateOf(V2Filter.ALL) }
    var legacy by rememberSaveable { mutableStateOf<Pair<String, Int>?>(null) }

    // Admins see the number of join requests on the board, so the people list loads once on entry.
    androidx.compose.runtime.LaunchedEffect(role) { if (V2Tool.USERS in tools && V2Tools.isAdmin(role)) actions.loadStaff() }

    val open = selectedCommitteeId?.let { id -> data.committees.firstOrNull { it.id == id } }
    val legacyCommittee = legacy?.let { (id, _) -> data.committees.firstOrNull { it.id == id } }

    BackHandler(enabled = legacy != null || open != null || page != null || tab != tabs.first()) {
        when {
            legacy != null -> legacy = null
            open != null -> onCloseCommittee()
            page != null -> page = null
            else -> tab = tabs.first()
        }
    }

    if (legacyCommittee != null) {
        Box(modifier.fillMaxSize().systemBarsPadding()) { legacyForm(legacyCommittee, legacy!!.second) { legacy = null } }
        return
    }

    val unread = data.notifications.count { !it.read }
    Column(modifier.fillMaxSize().background(p.paper).statusBarsPadding()) {
        val title = when {
            open != null -> "لجنة ${open.committeeNumber}"
            page != null -> page!!.title
            else -> tab.label
        }
        V2Header(
            // The role is said only where it differs from what the screens already show (admins), not on every screen.
            hello = if (data.user.role.roleKey in setOf("admin", "super_admin")) "${v2TodayText(data.today)} · ${roleName(data.user.role.roleKey)}" else v2TodayText(data.today),
            title = title,
            unread = unread,
            isOnline = data.isOnline,
            onBack = when {
                open != null -> onCloseCommittee
                page != null -> ({ page = null })
                else -> null
            },
            onBell = { page = V2Page.INBOX }
        )
        if (!data.isOnline) V2OfflineStrip(onRetry = { actions.sync {} })
        val context = LocalContext.current
        val reduceMotion = remember {
            android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }
        val snack = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val undo = remember(snack, scope) {
            V2Undo { message, onUndo ->
                scope.launch {
                    snack.currentSnackbarData?.dismiss()
                    if (snack.showSnackbar(message, actionLabel = "تراجع", duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) onUndo()
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().then(if (open != null) Modifier.navigationBarsPadding() else Modifier)) {
          CompositionLocalProvider(LocalV2Undo provides undo, LocalV2Reduce provides reduceMotion) {
            AnimatedContent(
                targetState = Triple(open?.id, page, tab),
                // A short rise and fade: calm, and quick enough that a cheap phone never waits on it.
                transitionSpec = {
                    // «Remove animations» in the phone's settings (scale 0) is respected: the screens just swap.
                    // Moving between tabs is instant (no slide on every tap); only opening or closing a screen animates.
                    val tabOnly = initialState.first == targetState.first && initialState.second == targetState.second
                    if (reduceMotion || tabOnly) EnterTransition.None togetherWith ExitTransition.None
                    else (fadeIn(tween(220, delayMillis = 60)) + slideInVertically(tween(260)) { it / 24 }) togetherWith fadeOut(tween(120))
                },
                label = "v2"
            ) { (openId, pg, t) ->
                val c = openId?.let { id -> data.committees.firstOrNull { it.id == id } }
                when {
                    c != null -> V2CommitteeScreen(c, data, actions, tools, onLegacy = { tabIndex -> legacy = c.id to tabIndex })
                    pg == V2Page.INBOX -> V2InboxScreen(data, actions, onOpenCommittee)
                    pg == V2Page.EXAMS -> V2ExamsScreen(data, actions)
                    pg == V2Page.CALENDAR -> V2CalendarScreen(data, actions, onOpenCommittee)
                    pg == V2Page.ACCOUNT -> V2AccountScreen(data, actions)
                    else -> when (t) {
                        V2Tab.NOW -> V2NowScreen(data, actions, onOpenCommittee)
                        V2Tab.MINE -> V2CommitteeListScreen(data, onOpenCommittee, filter, { filter = it }, mineOnly = true)
                        V2Tab.CEREMONIES -> V2CeremoniesScreen(data, onOpenCommittee)
                        V2Tab.TEAM -> V2TeamScreen(data, actions, onOpenCommittee)
                        V2Tab.ME -> V2MoreScreen(data, actions, tools, onPage = { page = it })
                        V2Tab.BOARD -> V2BoardScreen(data, onOpenCommittee, onShow = { f -> filter = f; tab = V2Tab.COMMITTEES }, onPeople = { tab = V2Tab.PEOPLE })
                        V2Tab.COMMITTEES -> V2CommitteeListScreen(data, onOpenCommittee, filter, { filter = it }, mineOnly = false)
                        V2Tab.PEOPLE -> V2PeopleScreen(data, actions, tools)
                        V2Tab.MORE -> V2MoreScreen(data, actions, tools, onPage = { page = it })
                    }
                }
            }
          }
            // Scrolled content fades into the tab bar instead of ending on a hard edge.
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(24.dp).background(Brush.verticalGradient(listOf(Color.Transparent, p.paper))))
            SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 8.dp)) { d ->
                Snackbar(d, shape = RoundedCornerShape(20.dp), containerColor = p.ink, contentColor = p.paper, actionColor = p.paper)
            }
        }
        if (open == null) {
            V2TabBar(tabs, tab) { chosen ->
                page = null
                if (chosen != tab) filter = V2Filter.ALL // a filter never follows you silently to another tab
                tab = chosen
            }
        }
    }
}

/** «Undo» for an action just done: a message with «تراجع» above the tab bar. */
fun interface V2Undo { fun offer(message: String, onUndo: () -> Unit) }

val LocalV2Undo = staticCompositionLocalOf { V2Undo { _, _ -> } }

/** Offline, said plainly under the header (and to TalkBack), with a way to try again. */
@Composable
private fun V2OfflineStrip(onRetry: () -> Unit) {
    val p = LocalV2.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(p.brick.copy(alpha = 0.12f))
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }.padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CloudOff, null, tint = p.brick, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("مفيش اتصال. اللي بتعمله هيتبعت لما النت يرجع.", style = V2Type.small, color = p.ink, modifier = Modifier.weight(1f))
        TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("إعادة", style = V2Type.smallStrong, color = p.brick) }
    }
}

/** List filters, shared by the board (which opens the list already filtered) and the list itself. */
enum class V2Filter(val label: String) {
    ALL("الكل"), LATE("متأخرة"), EXAM("الامتحان"), SCORING("التصحيح"), SHEET("الكشف"), CEREMONY("الحفل");

    fun group(): V2Stages.Group? = when (this) {
        EXAM -> V2Stages.Group.EXAM
        SCORING -> V2Stages.Group.SCORING
        SHEET -> V2Stages.Group.SHEET
        CEREMONY -> V2Stages.Group.CEREMONY
        else -> null
    }
}

@Composable
private fun V2Header(hello: String, title: String, unread: Int, isOnline: Boolean, onBack: (() -> Unit)?, onBell: () -> Unit) {
    val p = LocalV2.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
    Row(
        Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (onBack != null) {
            Box(
                Modifier.size(48.dp).v2Sheet(CircleShape).clickable(role = Role.Button, onClickLabel = "رجوع", onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = p.ink) }
        }
        Column(Modifier.weight(1f)) {
            Text(
                hello, style = V2Type.small, color = p.muted, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(title, style = V2Type.title, color = p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
        }
        Box(
            Modifier.size(48.dp).v2Sheet(CircleShape)
                .clickable(role = Role.Button, onClick = onBell)
                .semantics { contentDescription = if (unread > 0) "الإشعارات، $unread جديد" else "الإشعارات" },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Notifications, null, tint = p.ink)
            if (unread > 0) Box(Modifier.align(Alignment.TopStart).padding(11.dp).size(9.dp).clip(CircleShape).background(p.ink))
        }
    }
    }
}

private fun iconFor(tab: V2Tab): ImageVector = when (tab) {
    V2Tab.NOW -> Icons.Default.Schedule
    V2Tab.MINE -> Icons.Default.AssignmentInd
    V2Tab.COMMITTEES -> Icons.AutoMirrored.Filled.ViewList
    V2Tab.CEREMONIES -> Icons.Default.Celebration
    V2Tab.TEAM -> Icons.Default.Groups
    V2Tab.ME -> Icons.Default.Person
    V2Tab.BOARD -> Icons.Default.Dashboard
    V2Tab.PEOPLE -> Icons.Default.PersonSearch
    V2Tab.MORE -> Icons.Default.MoreHoriz
}

/** A floating capsule over the paper: four tabs, the current one in the accent colour. */
@Composable
private fun V2TabBar(tabs: List<V2Tab>, current: V2Tab, onSelect: (V2Tab) -> Unit) {
    val p = LocalV2.current
    Box(Modifier.fillMaxWidth().background(p.paper).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp), contentAlignment = Alignment.Center) {
        // At very large text the labels of the tabs you are not on are dropped (the icon + TalkBack name remain),
        // so four tabs still fit a phone's width.
        val big = LocalDensity.current.fontScale >= 1.3f
        Row(Modifier.widthIn(max = 640.dp).fillMaxWidth().v2Sheet(RoundedCornerShape(50)).selectableGroup().padding(horizontal = 6.dp, vertical = 6.dp)) {
            tabs.forEach { t ->
                val on = t == current
                val tint by animateColorAsState(if (on) p.cta else p.muted, label = "tab")
                val pill by animateColorAsState(if (on) p.cta.copy(alpha = 0.10f) else Color.Transparent, label = "tab-pill")
                Column(
                    Modifier.weight(1f).heightIn(min = 60.dp).clip(RoundedCornerShape(50))
                        .background(pill)
                        .selectable(selected = on, role = Role.Tab, onClick = { onSelect(t) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
                ) {
                    Icon(iconFor(t), if (big && !on) t.label else null, tint = tint, modifier = Modifier.size(24.dp))
                    if (on || !big) Text(t.label, style = V2Type.smallStrong, color = if (on) p.cta else p.muted, maxLines = 1)
                }
            }
        }
    }
}
