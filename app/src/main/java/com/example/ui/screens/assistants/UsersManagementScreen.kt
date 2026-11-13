package com.example.ui.screens.assistants

import com.example.ui.theme.tEdge
import com.example.ui.theme.tBg
import com.example.ui.theme.tFg
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppUser
import com.example.data.model.Committee
import com.example.data.model.Exam
import com.example.data.model.SystemPermissions
import com.example.data.model.UserRole
import com.example.data.remote.Permissions
import com.example.data.remote.PermissionProfileDto
import com.example.data.remote.StaffUserDto
import com.example.ui.components.DateField
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.Primary900
import com.example.ui.theme.TextMuted

private val Danger = Color(0xFFDC2626)
private const val DEFAULT_INTERVIEW_NOTES = "يرجى الحضور لمقر إدارة لجان المسجد لإجراء اختبار حفظ القرآن والمقابلة."
private const val DEFAULT_REJECTION = "نعتذر عن قبول الطلب حالياً لاكتمال النصاب المطلوب في لجان هذه الدورة، مع تمنياتنا بالتوفيق."

/** Role label, as on the web (AuthService.getRoleLabel). */
fun roleLabel(role: String): String = UserRole.entries.find { it.roleKey == role }?.titleAr ?: role

private fun statusLabel(s: String) = when (s) {
    "pending" -> "قيد المراجعة"
    "interview" -> "مقابلة مجدولة"
    "rejected" -> "معتذر عنه"
    "approved" -> "معتمد"
    else -> s
}

/**
 * The web's «الشيوخ والصلاحيات» (AssistantsView): approved accounts, join requests with
 * interviews and dossiers, and permission profiles. Every change is confirmed by the server first.
 */
@Composable
fun UsersManagementScreen(
    currentUser: AppUser?,
    users: List<StaffUserDto>,
    profiles: List<PermissionProfileDto>,
    committees: List<Committee>,
    exams: List<Exam>,
    isLoading: Boolean,
    onReload: () -> Unit,
    /** Sends a user save (identity fields + only what changed); calls back true once saved. */
    onSaveUser: (StaffUserDto?, Map<String, Any?>, String, (Boolean) -> Unit) -> Unit,
    onDeleteUser: (StaffUserDto) -> Unit,
    onResetPassword: (StaffUserDto, String, (Boolean) -> Unit) -> Unit,
    onSaveProfile: (PermissionProfileDto, (Boolean) -> Unit) -> Unit,
    onDeleteProfile: (PermissionProfileDto) -> Unit,
    onOpenCommittee: (String) -> Unit,
    /** New member lists per committee and (add, remove) per ceremony, for one person. */
    onSaveAssignments: (Map<String, List<String>>, Map<String, Pair<List<String>, List<String>>>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { onReload() }
    val me = currentUser ?: return
    val canGrant = Permissions.can("users.grant")
    val canManage = Permissions.can("users.manage")
    val canDelete = Permissions.can("users.delete")
    val canReadProfiles = Permissions.can("profiles.read")
    val canWriteProfiles = Permissions.can("profiles.write")
    val canAssign: (Committee) -> Boolean = { canGrant || Permissions.inScope(it.id) }
    fun committeeLabel(ids: List<String>) = committees.filter { it.id in ids }
        .joinToString("، ") { "#${it.committeeNumber}${if (it.courseName.isNotBlank()) " (${it.courseName})" else ""}" }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var requestsFilter by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StaffUserDto?>(null) }
    var deleting by remember { mutableStateOf<StaffUserDto?>(null) }
    var dossier by remember { mutableStateOf<StaffUserDto?>(null) }
    var interviewing by remember { mutableStateOf<StaffUserDto?>(null) }
    var rejecting by remember { mutableStateOf<StaffUserDto?>(null) }
    var approving by remember { mutableStateOf<StaffUserDto?>(null) }
    var assigningProfile by remember { mutableStateOf<StaffUserDto?>(null) }
    var demoting by remember { mutableStateOf<StaffUserDto?>(null) }
    var placing by remember { mutableStateOf<StaffUserDto?>(null) }
    val isSuperAdmin = Permissions.role == "super_admin"
    var profileEditing by remember { mutableStateOf<PermissionProfileDto?>(null) }
    var profileCreating by remember { mutableStateOf(false) }
    var profileDeleting by remember { mutableStateOf<PermissionProfileDto?>(null) }

    val approved = users.filter { it.status == "approved" }
    val requests = users.filter { it.role != "super_admin" && it.status != "approved" }
    val q = query.trim().lowercase()
    val shownApproved = approved.filter { q.isEmpty() || it.name.lowercase().contains(q) || it.phone.contains(q) || it.email.lowercase().contains(q) }
    val shownRequests = requests.filter {
        (requestsFilter == "all" || it.status == requestsFilter) &&
            (q.isEmpty() || it.name.lowercase().contains(q) || it.phone.contains(q) ||
                (it.requestCode?.lowercase()?.contains(q) == true) || (it.quranLevel?.lowercase()?.contains(q) == true))
    }

    fun identity(u: StaffUserDto): MutableMap<String, Any?> =
        mutableMapOf("id" to u.id, "name" to u.name, "phone" to u.phone, "email" to u.email.ifBlank { null }, "expectedUpdatedAt" to u.updatedAt)

    LazyColumn(
        modifier = modifier.fillMaxSize().background(tBg(Color(0xFFF8F9FD))),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(tBg(Primary800), tBg(Color(0xFF065F46)))), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text("الشيوخ والصلاحيات", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(
                    "إدارة المستخدمين المعتمدين، طلبات الانضمام والمقابلات، وبناء بروفايلات الصلاحيات.",
                    color = tFg(Color(0xFFD1FAE5)), style = MaterialTheme.typography.bodySmall
                )
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canManage) {
                        Button(
                            onClick = { adding = true },
                            colors = ButtonDefaults.buttonColors(containerColor = tBg(Color.White), contentColor = tFg(Primary800)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, null)
                            Spacer(Modifier.width(4.dp))
                            Text("إضافة مستخدم مباشر", fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(onClick = onReload, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                }
            }
        }
        item {
            TabRow(selectedTabIndex = tab, containerColor = tBg(Color.White)) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("المستخدمون (${approved.size})") })
                if (canReadProfiles) Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("البروفايلات (${profiles.size})") })
                Tab(
                    selected = tab == 2, onClick = { tab = 2 },
                    text = { Text("الطلبات (${requests.count { it.status == "pending" }} معلق)") }
                )
            }
        }
        item {
            OutlinedTextField(
                query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text(if (tab == 2) "بحث بالاسم أو الهاتف أو كود الطلب..." else "بحث بالاسم أو الهاتف أو البريد...") },
                shape = RoundedCornerShape(14.dp)
            )
            if (isLoading) Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.padding(end = 8.dp))
                Text("جارٍ تحميل المستخدمين من الخادم...", color = tFg(TextMuted))
            }
        }

        when (tab) {
            0 -> {
                if (shownApproved.isEmpty() && !isLoading) item { Text("لا يوجد مستخدمون مطابقون", color = tFg(TextMuted)) }
                items(shownApproved, key = { it.id }) { u ->
                    val profile = profiles.find { it.id == u.profileId }
                    val canEditUser = canGrant || (u.role == "sheikh" && canManage) || u.id == me.id
                    val placedOn = committees.count { c -> c.staff.any { it.id == u.id } }
                    val invitedTo = committees.filter { c -> c.ceremonyDetails.attendees.any { it.id == u.id } }
                    val canPlace = (u.role == "sheikh" || (u.role == "committee_head" && canGrant)) &&
                        (Permissions.can("committees.members") || Permissions.can("committees.finalize"))
                    Card(
                        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = tBg(Color.White)),
                        border = BorderStroke(1.dp, if (u.role == "super_admin") tEdge(Color(0xFFFCD34D)) else tEdge(Color(0xFFE2E8F0)))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(u.name, fontWeight = FontWeight.Bold, color = tFg(Primary900), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (u.role == "super_admin") Text("  👑 سوبر أدمن", style = MaterialTheme.typography.labelSmall, color = tFg(Color(0xFF92400E)))
                                    }
                                    Text("📞 ${u.phone} • ${roleLabel(u.role)}", style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
                                    if (u.mustChangePassword) Text("بانتظار تغيير كلمة المرور المؤقتة", style = MaterialTheme.typography.labelSmall, color = tFg(Color(0xFFB45309)))
                                }
                                if (canEditUser) IconButton(onClick = { editing = u }) { Icon(Icons.Default.Edit, "تعديل ${u.name}", tint = tFg(Primary700)) }
                                if (u.role != "super_admin" && u.id != me.id && canDelete) IconButton(onClick = { deleting = u }) { Icon(Icons.Default.Delete, "حذف الحساب", tint = tFg(Danger)) }
                            }
                            InfoLine(
                                "نظام الصلاحية المطبق:",
                                when {
                                    u.role == "super_admin" -> "كامل الصلاحيات (سوبر أدمن)"
                                    profile != null -> "آخر بروفايل طُبّق: ${profile.name}"
                                    else -> "صلاحيات محددة يدوياً"
                                }
                            )
                            if (u.role != "super_admin" && u.permissions.isNotEmpty()) {
                                Text(
                                    u.permissions.joinToString(" • ") { p -> SystemPermissions.ALL_PERMISSIONS.find { it.first == p }?.second ?: p },
                                    style = MaterialTheme.typography.labelSmall, color = tFg(Color(0xFF0F766E))
                                )
                            }
                            InfoLine(
                                "اللجان المسموح بها:",
                                when {
                                    u.role == "super_admin" || u.role == "admin" || u.allCommitteesAllowed -> "كل اللجان ✓"
                                    else -> committeeLabel(u.assignedCommitteeIds).ifBlank { "لا توجد لجان مسندة" }
                                }
                            )
                            InfoLine(
                                "الامتحانات المصرح بها:",
                                when {
                                    u.role == "super_admin" || u.role == "admin" || u.allExamsAllowed -> "كل الامتحانات ✓"
                                    u.allowedExamIds.isNotEmpty() -> "${u.allowedExamIds.size} امتحان محدد"
                                    else -> "لا يفتح ملفات الامتحانات"
                                }
                            )
                            if (u.role == "sheikh" || u.role == "committee_head") {
                                InfoLine(
                                    "اللجان والحفلات:",
                                    "$placedOn لجنة · ${invitedTo.size} حفل" +
                                        if (invitedTo.isEmpty()) "" else " (" + invitedTo.joinToString("، ") { "لجنة ${it.committeeNumber}" } + ")"
                                )
                            }
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (canPlace) {
                                    Button(onClick = { placing = u }) { Text("اللجان والحفلات") }
                                }
                                if (canGrant && u.role != "super_admin" && profiles.isNotEmpty()) {
                                    OutlinedButton(onClick = { assigningProfile = u }) { Text("تغيير نظام الصلاحية") }
                                }
                                if (isSuperAdmin && u.id != me.id) {
                                    if (u.role == "super_admin") {
                                        OutlinedButton(onClick = { demoting = u }) { Text("تنزيل من سوبر أدمن", color = tFg(Danger)) }
                                    } else {
                                        OutlinedButton(onClick = {
                                            onSaveUser(u, identity(u).apply { put("role", "super_admin") }, "تم تحديث الرتبة بنجاح.") {}
                                        }) { Text("ترقية إلى سوبر أدمن", color = tFg(Color(0xFF92400E))) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("مركز بناء بروفايلات الصلاحيات", fontWeight = FontWeight.Bold, color = tFg(Color(0xFF134E4A)), modifier = Modifier.weight(1f))
                        if (canWriteProfiles) Button(onClick = { profileCreating = true }, colors = ButtonDefaults.buttonColors(containerColor = tBg(Color(0xFF0F766E)))) { Text("+ بروفايل جديد") }
                    }
                }
                if (profiles.isEmpty() && !isLoading) item { Text("لا توجد بروفايلات بعد", color = tFg(TextMuted)) }
                items(profiles, key = { it.id }) { p ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = tBg(Color.White)), border = BorderStroke(1.dp, tEdge(Color(0xFF99F6E4)))) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.name, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                                if (p.isDefault) Text("افتراضي", style = MaterialTheme.typography.labelSmall, color = tFg(Color(0xFF0F766E)))
                            }
                            if (p.description.isNotBlank()) Text(p.description, style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
                            InfoLine("الصلاحيات:", p.permissions.joinToString("، ") { id -> SystemPermissions.ALL_PERMISSIONS.find { it.first == id }?.second ?: id }.ifBlank { "بدون" })
                            InfoLine("اللجان:", if (p.allCommitteesAllowed) "كل اللجان" else committeeLabel(p.allowedCommitteeIds).ifBlank { "لا توجد" })
                            InfoLine("الامتحانات:", if (p.allExamsAllowed) "كل الامتحانات" else "${p.allowedExamIds.size} امتحان")
                            Text("المستخدمون المطبق عليهم: ${users.count { it.profileId == p.id }}", style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted))
                            if (canWriteProfiles) Row {
                                TextButton(onClick = { profileEditing = p }) { Text("تعديل") }
                                if (!p.isDefault) TextButton(onClick = { profileDeleting = p }) { Text("حذف", color = tFg(Danger)) }
                            }
                        }
                    }
                }
            }
            else -> {
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "all" to "الكل (${requests.size})",
                            "pending" to "قيد المراجعة (${requests.count { it.status == "pending" }})",
                            "interview" to "المقابلات (${requests.count { it.status == "interview" }})",
                            "rejected" to "المعتذر عنهم (${requests.count { it.status == "rejected" }})"
                        ).forEach { (k, l) -> FilterChip(selected = requestsFilter == k, onClick = { requestsFilter = k }, label = { Text(l) }) }
                    }
                }
                if (shownRequests.isEmpty() && !isLoading) item { Text("لا توجد طلبات في هذا التصنيف", color = tFg(TextMuted)) }
                items(shownRequests, key = { it.id }) { u ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = tBg(Color.White)), border = BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0)))) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(u.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(statusLabel(u.status), style = MaterialTheme.typography.labelSmall, color = if (u.status == "rejected") tFg(Danger) else tFg(Color(0xFF1D4ED8)))
                            }
                            Text("📞 ${u.phone} • الدور المطلوب: ${roleLabel(u.role)}", style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
                            u.requestCode?.let { Text("كود الطلب: $it", style = MaterialTheme.typography.labelSmall) }
                            u.quranLevel?.let { InfoLine("الحفظ:", it) }
                            if (u.status == "interview") u.interviewDate?.let { InfoLine("موعد المقابلة:", it) }
                            if (u.status == "rejected") u.rejectionReason?.let { InfoLine("سبب الاعتذار:", it) }
                            if (canManage) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { approving = u }, colors = ButtonDefaults.buttonColors(containerColor = tBg(Color(0xFF059669)))) { Text("اعتماد") }
                                OutlinedButton(onClick = { interviewing = u }) { Text("تحديد مقابلة") }
                                OutlinedButton(onClick = { dossier = u }) { Text("الملف") }
                                if (u.status != "rejected") OutlinedButton(onClick = { rejecting = u }) { Text("اعتذار", color = tFg(Danger)) }
                            } else Row { OutlinedButton(onClick = { dossier = u }) { Text("الملف") } }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.padding(24.dp)) }
    }

    // ------------------------------------------------------------ dialogs

    if (adding) {
        UserAccessDialog(
            title = "إضافة وتفعيل مستخدم مباشر",
            user = null, me = me, canGrant = canGrant, profiles = profiles, committees = committees, exams = exams,
            canAssign = canAssign,
            onDismiss = { adding = false },
            onSubmit = { body, done -> onSaveUser(null, body, "تم إنشاء الحساب، وسيُطلب منه تغيير كلمة المرور عند أول دخول") { ok -> done(ok); if (ok) adding = false } },
            onResetPassword = null
        )
    }
    editing?.let { u ->
        UserAccessDialog(
            title = "تعديل بيانات وصلاحيات: ${u.name}",
            user = u, me = me, canGrant = canGrant, profiles = profiles, committees = committees, exams = exams,
            canAssign = canAssign,
            onDismiss = { editing = null },
            onSubmit = { body, done -> onSaveUser(u, body, "تم حفظ بيانات المستخدم") { ok -> done(ok); if (ok) editing = null } },
            onResetPassword = if (canGrant && u.id != me.id) ({ pw, done -> onResetPassword(u, pw, done) }) else null
        )
    }
    approving?.let { u ->
        ApproveDialog(
            user = u, canGrant = canGrant, profiles = profiles, committees = committees, canAssign = canAssign,
            onDismiss = { approving = null },
            onApprove = { access, done ->
                onSaveUser(u, identity(u).apply { put("status", "approved"); putAll(access) }, "تم اعتماد الحساب") { ok -> done(ok); if (ok) approving = null }
            }
        )
    }
    interviewing?.let { u ->
        var date by remember { mutableStateOf(u.interviewDate ?: "") }
        var notes by remember { mutableStateOf(DEFAULT_INTERVIEW_NOTES) }
        AlertDialog(
            onDismissRequest = { interviewing = null },
            title = { Text("تحديد موعد مقابلة: ${u.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField(date, { date = it }, "تاريخ المقابلة")
                    OutlinedTextField(notes, { notes = it }, label = { Text("تعليمات المقابلة") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveUser(u, identity(u).apply { put("status", "interview"); put("interviewDate", date); put("interviewNotes", notes) }, "تم تحديد موعد المقابلة") { ok -> if (ok) interviewing = null }
                }, enabled = date.isNotBlank()) { Text("تأكيد الموعد") }
            },
            dismissButton = { TextButton(onClick = { interviewing = null }) { Text("إلغاء") } }
        )
    }
    rejecting?.let { u ->
        var reason by remember { mutableStateOf(DEFAULT_REJECTION) }
        AlertDialog(
            onDismissRequest = { rejecting = null },
            title = { Text("الاعتذار عن طلب ${u.name}") },
            text = { OutlinedTextField(reason, { reason = it }, label = { Text("سبب الاعتذار (يظهر في الملف)") }, minLines = 3) },
            confirmButton = {
                Button(onClick = {
                    onSaveUser(u, identity(u).apply { put("status", "rejected"); put("rejectionReason", reason) }, "تم تسجيل الاعتذار عن الطلب") { ok -> if (ok) rejecting = null }
                }, colors = ButtonDefaults.buttonColors(containerColor = tBg(Danger))) { Text("تأكيد الاعتذار") }
            },
            dismissButton = { TextButton(onClick = { rejecting = null }) { Text("إلغاء") } }
        )
    }
    dossier?.let { u ->
        AlertDialog(
            onDismissRequest = { dossier = null },
            title = { Text("ملف وسيرة المتقدم: ${u.name}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    InfoLine("كود الطلب:", u.requestCode ?: "—")
                    InfoLine("تاريخ التقديم:", u.createdAt)
                    InfoLine("الحالة:", statusLabel(u.status))
                    InfoLine("الدور المطلوب:", roleLabel(u.role))
                    InfoLine("الهاتف:", u.phone)
                    InfoLine("واتساب:", u.whatsapp ?: u.phone)
                    if (u.email.isNotBlank()) InfoLine("البريد:", u.email)
                    InfoLine("مقدار الحفظ:", u.quranLevel ?: "—")
                    InfoLine("المؤهل العلمي:", u.qualification ?: "—")
                    u.experience?.let { InfoLine("الخبرات:", it) }
                    u.availability?.let { InfoLine("أوقات الإتاحة:", it) }
                    u.preferredCommittees?.takeIf { it.isNotEmpty() }?.let { InfoLine("اللجان المفضلة:", it.joinToString("، ") { n -> "#$n" }) }
                    u.notes?.let { InfoLine("ملاحظات:", it) }
                    u.interviewDate?.let { InfoLine("موعد المقابلة:", it) }
                    u.interviewNotes?.let { InfoLine("تعليمات المقابلة:", it) }
                    u.rejectionReason?.let { InfoLine("سبب الاعتذار:", it) }
                }
            },
            confirmButton = {
                if (canManage && u.status != "approved") Button(onClick = { dossier = null; approving = u }) { Text("اعتماد الطلب") }
            },
            dismissButton = { TextButton(onClick = { dossier = null }) { Text("إغلاق") } }
        )
    }
    deleting?.let { u ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("تأكيد حذف الحساب") },
            text = { Text("سيتم حذف حساب «${u.name}» نهائياً وإنهاء جلساته. هل أنت متأكد؟") },
            confirmButton = { TextButton(onClick = { deleting = null; onDeleteUser(u) }) { Text("حذف نهائي", color = tFg(Danger)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("إلغاء") } }
        )
    }
    placing?.let { u ->
        com.example.ui.screens.committees.PersonAssignmentsDialog(
            userId = u.id, userName = u.name, userRole = u.role, committees = committees,
            onDismiss = { placing = null },
            onSave = { members, ceremonies -> onSaveAssignments(members, ceremonies) }
        )
    }
    demoting?.let { u ->
        var to by remember { mutableStateOf("admin") }
        AlertDialog(
            onDismissRequest = { demoting = null },
            title = { Text("تنزيل ${u.name} من المشرف العام") },
            text = {
                Column {
                    Text("اختر الدور الجديد:")
                    listOf("admin", "committee_head", "sheikh").forEach { r ->
                        Row(Modifier.fillMaxWidth().clickable { to = r }, verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.RadioButton(selected = to == r, onClick = { to = r })
                            Text(roleLabel(r))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveUser(u, identity(u).apply { put("role", to) }, "تم تحديث الرتبة بنجاح.") { ok -> if (ok) demoting = null }
                }) { Text("تأكيد التنزيل", color = tFg(Danger)) }
            },
            dismissButton = { TextButton(onClick = { demoting = null }) { Text("إلغاء") } }
        )
    }
    assigningProfile?.let { u ->
        AlertDialog(
            onDismissRequest = { assigningProfile = null },
            title = { Text("تطبيق بروفايل على ${u.name}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    profiles.forEach { p ->
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                onSaveUser(u, identity(u).apply { put("profileId", p.id) }, "تم تطبيق البروفايل") { ok -> if (ok) assigningProfile = null }
                            },
                            border = BorderStroke(1.dp, if (u.profileId == p.id) tEdge(Color(0xFF0F766E)) else tEdge(Color(0xFFE2E8F0)))
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text(p.name, fontWeight = FontWeight.Bold)
                                if (p.description.isNotBlank()) Text(p.description, style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { assigningProfile = null }) { Text("إغلاق") } }
        )
    }
    if (profileCreating || profileEditing != null) {
        ProfileDialog(
            profile = profileEditing, me = me, committees = committees, exams = exams,
            onDismiss = { profileCreating = false; profileEditing = null },
            onSave = { p, done -> onSaveProfile(p) { ok -> done(ok); if (ok) { profileCreating = false; profileEditing = null } } }
        )
    }
    profileDeleting?.let { p ->
        AlertDialog(
            onDismissRequest = { profileDeleting = null },
            title = { Text("حذف البروفايل") },
            text = { Text("حذف بروفايل «${p.name}»؟ لا يغيّر صلاحيات من طُبّق عليهم.") },
            confirmButton = { TextButton(onClick = { profileDeleting = null; onDeleteProfile(p) }) { Text("حذف", color = tFg(Danger)) } },
            dismissButton = { TextButton(onClick = { profileDeleting = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row {
        Text(label, style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted), modifier = Modifier.padding(end = 4.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

/** System permissions as checkboxes (only system permissions grant anything, D-01). */
@Composable
fun PermissionChecklist(selected: List<String>, onChange: (List<String>) -> Unit, enabled: Boolean = true) {
    Column {
        SystemPermissions.ALL_PERMISSIONS.forEach { (id, label) ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .toggleable(value = id in selected, enabled = enabled, role = Role.Checkbox) { onChange(if (id in selected) selected - id else selected + id) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = id in selected, onCheckedChange = null, enabled = enabled)
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Multi-select committees by id, grouped with their course; only assignable ones are offered. */
@Composable
fun CommitteeMultiPicker(
    label: String,
    committees: List<Committee>,
    selectedIds: List<String>,
    canAssign: (Committee) -> Boolean,
    onChange: (List<String>) -> Unit
) {
    var filter by remember { mutableStateOf("") }
    val options = committees.filter(canAssign).sortedWith(compareBy({ it.courseName }, { it.committeeNumber }))
        .filter { filter.isBlank() || it.committeeNumber.toString() == filter.trim() || it.committeeName.contains(filter) || it.sheikhName.contains(filter) }
    Column {
        Text("$label (${selectedIds.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(filter, { filter = it }, placeholder = { Text("بحث برقم اللجنة أو الاسم") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (options.isEmpty()) Text("لا توجد لجان متاحة للإسناد", style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted))
        Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
            options.forEach { c ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .toggleable(value = c.id in selectedIds, role = Role.Checkbox) { onChange(if (c.id in selectedIds) selectedIds - c.id else selectedIds + c.id) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = c.id in selectedIds, onCheckedChange = null)
                    Spacer(Modifier.width(6.dp))
                    Text("#${c.committeeNumber} ${c.committeeName}${if (c.courseName.isNotBlank()) " — ${c.courseName}" else ""}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ExamMultiPicker(exams: List<Exam>, selected: List<String>, onChange: (List<String>) -> Unit) {
    Column(Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
        exams.forEach { e ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = e.id in selected, role = Role.Checkbox) { onChange(if (e.id in selected) selected - e.id else selected + e.id) }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = e.id in selected, onCheckedChange = null)
                Spacer(Modifier.width(6.dp))
                Text("${e.title} (${e.book})", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RolePicker(role: String, enabled: Boolean, onChange: (String) -> Unit, includeSuper: Boolean) {
    // Only roles the server lets this user give (admins never give admin roles).
    val roles = Permissions.grantableRoles.filter { includeSuper || it != "super_admin" }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        roles.forEach { r -> FilterChip(selected = role == r, onClick = { if (enabled) onChange(r) }, enabled = enabled, label = { Text(roleLabel(r)) }) }
    }
}

@Composable
private fun ProfilePicker(profiles: List<PermissionProfileDto>, selected: String, onChange: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = selected.isBlank(), onClick = { onChange("") }, label = { Text("بدون بروفايل (حدد أدناه)") })
        profiles.forEach { p -> FilterChip(selected = selected == p.id, onClick = { onChange(p.id) }, label = { Text(p.name) }) }
    }
}

/** Temporary password with letters and digits, as the web generates. */
private fun temporaryPassword(): String {
    val letters = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz"
    val digits = "23456789"
    val r = java.security.SecureRandom()
    val chars = (1..6).map { letters[r.nextInt(letters.length)] } + (1..4).map { digits[r.nextInt(digits.length)] }
    return chars.shuffled(r).joinToString("")
}

/**
 * Add (user == null) or edit a user. Only fields that changed are sent, and access fields only by
 * those allowed to grant them, as on the web (WEB-03, WEB-05, WEB-14).
 */
@Composable
private fun UserAccessDialog(
    title: String,
    user: StaffUserDto?,
    me: AppUser,
    canGrant: Boolean,
    profiles: List<PermissionProfileDto>,
    committees: List<Committee>,
    exams: List<Exam>,
    canAssign: (Committee) -> Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Map<String, Any?>, (Boolean) -> Unit) -> Unit,
    onResetPassword: ((String, (Boolean) -> Unit) -> Unit)?
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(user?.notes ?: "") }
    var role by remember { mutableStateOf(user?.role ?: "sheikh") }
    var profileId by remember { mutableStateOf("") }
    var perms by remember { mutableStateOf(user?.permissions ?: emptyList()) }
    var allCommittees by remember { mutableStateOf(user?.allCommitteesAllowed ?: false) }
    var committeeIds by remember { mutableStateOf(user?.assignedCommitteeIds ?: emptyList()) }
    var allExams by remember { mutableStateOf(user?.allExamsAllowed ?: false) }
    var examIds by remember { mutableStateOf(user?.allowedExamIds ?: emptyList()) }
    var reset by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val isSelfEdit = user?.id == me.id

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                error?.let { Text(it, color = tFg(Danger)) }
                OutlinedTextField(name, { name = it }, label = { Text("الاسم الكامل *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    phone, { phone = it }, label = { Text("رقم الهاتف (اسم الدخول) *") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(email, { email = it }, label = { Text("البريد الإلكتروني") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (user == null) {
                    OutlinedTextField(password, { password = it }, label = { Text("كلمة المرور المؤقتة *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { password = temporaryPassword() }) { Text("توليد كلمة مرور مؤقتة") }
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth())
                if (!isSelfEdit || canGrant) {
                    HorizontalDivider()
                    if (canGrant) {
                        Text("الدور في المنظومة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        RolePicker(role, enabled = profileId.isBlank(), onChange = { role = it }, includeSuper = false)
                        if (profiles.isNotEmpty()) {
                            Text("تطبيق بروفايل", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            ProfilePicker(profiles, profileId) { profileId = it }
                        }
                    }
                    if (profileId.isBlank()) {
                        if (canGrant) {
                            Text("الصلاحيات", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            PermissionChecklist(perms, { perms = it })
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = allCommittees, role = Role.Checkbox) { allCommittees = it }, verticalAlignment = Alignment.CenterVertically) { Checkbox(allCommittees, null); Text("كل اللجان في كل الدورات")
                            }
                        }
                        if (!allCommittees) {
                            CommitteeMultiPicker("اللجان المسندة:", committees, committeeIds, canAssign) { committeeIds = it }
                        }
                        if (canGrant && (role == "committee_head" || "exams_pdf" in perms)) {
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = allExams, role = Role.Checkbox) { allExams = it }, verticalAlignment = Alignment.CenterVertically) { Checkbox(allExams, null); Text("فتح كل ملفات الامتحانات")
                            }
                            if (!allExams) ExamMultiPicker(exams, examIds) { examIds = it }
                        }
                    }
                }
                if (onResetPassword != null) {
                    HorizontalDivider()
                    Text("إعادة تعيين كلمة المرور", fontWeight = FontWeight.Bold, color = tFg(Color(0xFF92400E)))
                    OutlinedTextField(reset, { reset = it }, label = { Text("كلمة المرور المؤقتة") }, placeholder = { Text("8 أحرف على الأقل، حروف وأرقام") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row {
                        TextButton(onClick = { reset = temporaryPassword() }) { Text("توليد") }
                        TextButton(onClick = { onResetPassword(reset) { ok -> if (ok) reset = "" } }, enabled = reset.length >= 8) { Text("تعيين كلمة المرور المؤقتة") }
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = !saving, onClick = {
                if (name.isBlank() || phone.isBlank() || (user == null && password.length < 8)) {
                    error = if (user == null && password.length < 8) "كلمة المرور يجب ألا تقل عن 8 أحرف" else "الاسم ورقم الهاتف مطلوبان"
                    return@Button
                }
                val body = mutableMapOf<String, Any?>(
                    "id" to (user?.id ?: "usr_${java.util.UUID.randomUUID()}"),
                    "name" to name.trim(), "phone" to phone.trim(), "email" to email.trim().ifBlank { null },
                    "notes" to notes.trim().ifBlank { null }
                )
                if (user == null) {
                    body["password"] = password
                    body["status"] = "approved"
                    body["role"] = if (canGrant) role else "sheikh"
                    if (canGrant && profileId.isNotBlank()) body["profileId"] = profileId
                    else {
                        body["permissions"] = if (canGrant) perms else emptyList<String>()
                        if (canGrant && allCommittees) body["allCommitteesAllowed"] = true else body["assignedCommitteeIds"] = committeeIds
                    }
                } else {
                    body["expectedUpdatedAt"] = user.updatedAt
                    if (profileId.isNotBlank()) body["profileId"] = profileId
                    else {
                        if (canGrant && role != user.role) body["role"] = role
                        if (canGrant && perms.toSet() != user.permissions.toSet()) body["permissions"] = perms
                        if (canGrant && allCommittees != user.allCommitteesAllowed) body["allCommitteesAllowed"] = allCommittees
                        if (!allCommittees && committeeIds.toSet() != user.assignedCommitteeIds.toSet()) body["assignedCommitteeIds"] = committeeIds
                        if (canGrant && allExams != user.allExamsAllowed) body["allExamsAllowed"] = allExams
                        if (canGrant && !allExams && examIds.toSet() != user.allowedExamIds.toSet()) body["allowedExamIds"] = examIds
                    }
                }
                saving = true
                onSubmit(body) { saving = false }
            }) { Text(if (saving) "جارٍ الحفظ..." else "حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun ApproveDialog(
    user: StaffUserDto,
    canGrant: Boolean,
    profiles: List<PermissionProfileDto>,
    committees: List<Committee>,
    canAssign: (Committee) -> Boolean,
    onDismiss: () -> Unit,
    onApprove: (Map<String, Any?>, (Boolean) -> Unit) -> Unit
) {
    var role by remember { mutableStateOf(user.role) }
    var profileId by remember { mutableStateOf("") }
    var perms by remember { mutableStateOf(emptyList<String>()) }
    var allCommittees by remember { mutableStateOf(false) }
    // Preferred committees are numbers; offered only where the number is unambiguous (as on the web).
    val preferred = committees.filter { (user.preferredCommittees ?: emptyList()).contains(it.committeeNumber) && canAssign(it) }
    var committeeIds by remember {
        mutableStateOf<List<String>>(if (preferred.size == preferred.map { it.committeeNumber }.toSet().size) preferred.map { it.id } else emptyList())
    }
    var saving by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اعتماد ${user.name} وتحديد الصلاحيات") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canGrant) {
                    Text("الدور", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    RolePicker(role, enabled = true, onChange = { role = it }, includeSuper = false)
                    if (profiles.isNotEmpty()) {
                        Text("بروفايل الصلاحيات", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        ProfilePicker(profiles, profileId) { profileId = it }
                    }
                    if (profileId.isBlank()) {
                        PermissionChecklist(perms, { perms = it })
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = allCommittees, role = Role.Checkbox) { allCommittees = it }, verticalAlignment = Alignment.CenterVertically) { Checkbox(allCommittees, null); Text("كل اللجان")
                        }
                    }
                }
                if (profileId.isBlank() && !allCommittees) {
                    CommitteeMultiPicker("إسناد اللجان:", committees, committeeIds, canAssign) { committeeIds = it }
                }
            }
        },
        confirmButton = {
            Button(enabled = !saving, onClick = {
                val access = mutableMapOf<String, Any?>()
                if (canGrant) {
                    if (role != user.role) access["role"] = role
                    if (profileId.isNotBlank()) access["profileId"] = profileId
                    else {
                        access["permissions"] = perms
                        if (allCommittees) access["allCommitteesAllowed"] = true else access["assignedCommitteeIds"] = committeeIds
                    }
                } else access["assignedCommitteeIds"] = committeeIds
                saving = true
                onApprove(access) { saving = false }
            }, colors = ButtonDefaults.buttonColors(containerColor = tBg(Color(0xFF059669)))) { Text("اعتماد الحساب") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun ProfileDialog(
    profile: PermissionProfileDto?,
    me: AppUser,
    committees: List<Committee>,
    exams: List<Exam>,
    onDismiss: () -> Unit,
    onSave: (PermissionProfileDto, (Boolean) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var description by remember { mutableStateOf(profile?.description ?: "") }
    var color by remember { mutableStateOf(profile?.badgeColor ?: "emerald") }
    var perms by remember { mutableStateOf(profile?.permissions ?: emptyList()) }
    var allCommittees by remember { mutableStateOf(profile?.allCommitteesAllowed ?: false) }
    var committeeIds by remember { mutableStateOf(profile?.allowedCommitteeIds ?: emptyList()) }
    var allExams by remember { mutableStateOf(profile?.allExamsAllowed ?: true) }
    var examIds by remember { mutableStateOf(profile?.allowedExamIds ?: emptyList()) }
    var saving by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (profile == null) "بناء بروفايل صلاحيات جديد" else "تعديل البروفايل: ${profile.name}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("اسم البروفايل *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("الوصف") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("emerald", "blue", "amber", "purple", "rose", "teal").forEach { c -> FilterChip(selected = color == c, onClick = { color = c }, label = { Text(c) }) }
                }
                Text("الصلاحيات", fontWeight = FontWeight.Bold)
                PermissionChecklist(perms, { perms = it })
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = allCommittees, role = Role.Checkbox) { allCommittees = it }, verticalAlignment = Alignment.CenterVertically) { Checkbox(allCommittees, null); Text("كل اللجان") }
                if (!allCommittees) CommitteeMultiPicker("اللجان المسموح بها:", committees, committeeIds, { true }) { committeeIds = it }
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = allExams, role = Role.Checkbox) { allExams = it }, verticalAlignment = Alignment.CenterVertically) { Checkbox(allExams, null); Text("كل الامتحانات") }
                if (!allExams) ExamMultiPicker(exams, examIds) { examIds = it }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank() && !saving, onClick = {
                val ids = if (allCommittees) emptyList() else committeeIds
                saving = true
                onSave(
                    PermissionProfileDto(
                        id = profile?.id ?: "profile_${java.util.UUID.randomUUID()}",
                        name = name.trim(), description = description.trim(), badgeColor = color,
                        allowedCommitteeIds = ids,
                        allowedCommittees = committees.filter { it.id in ids }.map { it.committeeNumber },
                        allCommitteesAllowed = allCommittees,
                        allowedExamIds = if (allExams) emptyList() else examIds,
                        allExamsAllowed = allExams,
                        permissions = perms,
                        isDefault = profile?.isDefault ?: false,
                        createdBy = me.name,
                        createdAt = profile?.createdAt
                    )
                ) { saving = false }
            }) { Text("حفظ البروفايل") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
