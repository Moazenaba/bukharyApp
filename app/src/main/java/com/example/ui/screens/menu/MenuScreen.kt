package com.example.ui.screens.menu

import com.example.ui.theme.tEdge
import com.example.ui.theme.tBg
import com.example.ui.theme.tFg
import com.example.ui.theme.ThemeMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppUser
import com.example.data.model.Committee
import com.example.data.model.Course
import com.example.data.model.Exam
import com.example.data.model.ScheduleTask
import com.example.data.model.UserRole
import com.example.data.remote.Permissions
import com.example.ui.AppNavScreen
import com.example.ui.components.Dates
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.TextMuted

private data class MenuEntry(val screen: AppNavScreen, val label: String, val icon: ImageVector, val badge: String?)

/** The web's sidebar: every screen this user may open, in the same sections and order, with the same counts. */
@Composable
fun MenuScreen(
    currentUser: AppUser?,
    committees: List<Committee>,
    exams: List<Exam>,
    courses: List<Course>,
    tasks: List<ScheduleTask>,
    pendingRequests: Int,
    unreadNotifications: Int,
    onNavigate: (AppNavScreen) -> Unit,
    onAddCommittee: () -> Unit,
    onExport: () -> Unit,
    onChangePassword: () -> Unit,
    /** Switch to the simple look (sheikhs and committee heads); null hides the row. */
    onTrySimple: (() -> Unit)? = null,
    /** Open «الواجهات 2» (the new screens); null hides the row. */
    onTryV2: (() -> Unit)? = null,
    onLogout: () -> Unit,
    /** The look the person chose (light until they pick another). */
    themeMode: ThemeMode = ThemeMode.LIGHT,
    onThemeMode: (ThemeMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val user = currentUser ?: return
    val today = Dates.today()
    val todays = tasks.count { it.date == today && !it.completed }
    val ceremonies = committees.count { it.stages.ceremony.completed }
    val myCommittees = if (user.role == UserRole.SUPER_ADMIN || user.allCommitteesAllowed) committees.size else user.assignedCommitteeIds.size
    val sections = listOf(
        "العمليات الميدانية والكشوفات" to listOf(
            MenuEntry(AppNavScreen.HOME, "الرئيسية ونظرة عامة", Icons.Default.Home, null),
            MenuEntry(AppNavScreen.COURSE_TRACKER, "قاعدة البيانات والكشوفات", Icons.Default.TableChart, "الكشوفات"),
            MenuEntry(AppNavScreen.COMMITTEES, "إدارة اللجان الميدانية", Icons.Default.Groups, "$myCommittees"),
            MenuEntry(AppNavScreen.EXAMS, "الامتحانات ونماذج PDF", Icons.AutoMirrored.Filled.MenuBook, "${exams.size}")
        ),
        "المواعيد والتنظيم" to listOf(
            MenuEntry(AppNavScreen.CEREMONIES, "الحفلات والتكريم", Icons.Default.Celebration, if (ceremonies > 0) "$ceremonies حفل" else null),
            MenuEntry(AppNavScreen.SCHEDULE, "الجدول وتقويم المواعيد", Icons.Default.CalendarMonth, tasks.count { !it.completed }.takeIf { it > 0 }?.toString()),
            MenuEntry(AppNavScreen.COURSES, "إضافة وإدارة الدورات", Icons.Default.EventRepeat, "${courses.size}")
        ),
        "الكادر والإحصائيات" to listOf(
            MenuEntry(AppNavScreen.ASSISTANTS, "الشيوخ والصلاحيات", Icons.Default.Badge, if (pendingRequests > 0) "$pendingRequests معلق" else null),
            MenuEntry(AppNavScreen.STATISTICS, "حساب ونسبة الإنجاز", Icons.Default.DonutLarge, null),
            MenuEntry(AppNavScreen.DAILY_ALERTS, "التنبيهات والمهام اليومية", Icons.Default.NotificationsActive, if (todays > 0) "$todays اليوم" else null),
            MenuEntry(AppNavScreen.AUDIT, "سجل التدقيق", Icons.Default.History, null)
        ),
        "الحساب والنظام" to listOf(
            MenuEntry(AppNavScreen.NOTIFICATIONS, "الإشعارات", Icons.Default.Notifications, if (unreadNotifications > 0) "$unreadNotifications جديد" else null),
            MenuEntry(AppNavScreen.DATABASE_INSPECTOR, if (Permissions.can("database.admin")) "قاعدة البيانات وحالة المزامنة" else "حالة البيانات والمزامنة", Icons.Default.Storage, null)
        )
    )

    LazyColumn(
        modifier.fillMaxSize().background(tBg(Color(0xFFF8F9FD))),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = tBg(Primary800)), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(user.name, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${user.role.emoji} ${user.role.titleAr}", color = tFg(Color(0xFFCCFBF1)), style = MaterialTheme.typography.bodySmall)
                    Text("📞 ${user.phone}", color = tFg(Color(0xFFCCFBF1)), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        val quick = buildList {
            if (Permissions.can("committees.create")) add(MenuEntry(AppNavScreen.MENU, "إضافة لجنة", Icons.Default.AddCircle, null))
            add(MenuEntry(AppNavScreen.MENU, "تصدير الكشوف", Icons.Default.FileDownload, null))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quick.forEach { q ->
                    Card(
                        Modifier.weight(1f).clickable(role = Role.Button) { if (q.label == "إضافة لجنة") onAddCommittee() else onExport() },
                        shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = tBg(Color.White)),
                        border = BorderStroke(1.dp, tEdge(Color(0xFFA7F3D0)))
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(q.icon, null, tint = tFg(Primary700))
                            Spacer(Modifier.width(6.dp))
                            Text(q.label, fontWeight = FontWeight.Bold, color = tFg(Primary800))
                        }
                    }
                }
            }
        }
        // For sheikhs and committee heads: the simple look is the first thing in the menu, not buried at the bottom.
        onTryV2?.let { go -> item { MenuRow("الواجهات 2 (التصميم الجديد)", Icons.Default.AutoAwesome, "جديد", onClick = go) } }
        onTrySimple?.let { go -> item { MenuRow("جرّب الواجهة المبسطة (أسهل وأوضح)", Icons.Default.AutoAwesome, null, onClick = go) } }
        sections.forEach { (title, entries) ->
            val visible = entries.filter { user.canAccessScreen(it.screen) }
            if (visible.isNotEmpty()) {
                item { Text(title, style = MaterialTheme.typography.labelMedium, color = tFg(TextMuted), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                visible.forEach { e ->
                    item(key = e.screen.route) { MenuRow(e.label, e.icon, e.badge) { onNavigate(e.screen) } }
                }
            }
        }
        item { Text("الحساب", style = MaterialTheme.typography.labelMedium, color = tFg(TextMuted), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        item { MenuRow("تغيير كلمة المرور", Icons.Default.Key, null, onClick = onChangePassword) }
        item { com.example.ui.components.UpdateRow() }
        item { ThemeChooser(themeMode, onThemeMode) }
        item { MenuRow("تسجيل الخروج", Icons.AutoMirrored.Filled.Logout, null, tint = tFg(Color(0xFFDC2626)), onClick = onLogout) }
        item { Spacer(Modifier.padding(16.dp)) }
    }
}

/** «المظهر»: follow the phone, always light, or always dark. */
@Composable
private fun ThemeChooser(mode: ThemeMode, onChange: (ThemeMode) -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = tBg(Color.White)),
        border = BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0)))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("المظهر", fontWeight = FontWeight.SemiBold, color = tFg(Color(0xFF0F172A)))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.values().forEach { m ->
                    androidx.compose.material3.FilterChip(selected = mode == m, onClick = { onChange(m) }, label = { Text(m.label) })
                }
            }
        }
    }
}

@Composable
private fun MenuRow(label: String, icon: ImageVector, badge: String?, tint: Color = tFg(Primary700), onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = tBg(Color.White)),
        border = BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0)))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), color = if (tint == tFg(Primary700)) tFg(Color(0xFF0F172A)) else tint)
            badge?.let {
                Text(
                    it, style = MaterialTheme.typography.labelSmall, color = tFg(Primary800), fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(tBg(Color(0xFFD1FAE5)), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
