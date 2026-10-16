package com.example.ui.screens.assistants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUser
import com.example.data.model.Committee
import com.example.data.remote.Permissions
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.model.CustomPermission
import com.example.data.model.SystemPermissions
import com.example.data.model.UserRole
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.Primary100
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.Primary900
import com.example.ui.theme.SurfacePure

@Composable
fun AssistantsScreen(
    currentUser: AppUser?,
    users: List<AppUser>,
    committees: List<Committee>,
    /** Null arguments are left unchanged on the server (MOB-09). */
    onApproveUser: (user: AppUser, committeeIds: List<String>?, permissions: List<String>?, role: UserRole?) -> Unit,
    onRejectUser: (user: AppUser, reason: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var approvingUser by remember { mutableStateOf<AppUser?>(null) }
    var rejectingUser by remember { mutableStateOf<AppUser?>(null) }

    val pendingUsers = users.filter { it.status == "pending" || it.status == "interview" }
    val approvedUsers = users.filter { it.status == "approved" }

    // What the server allows: super admins grant roles and permissions; managers approve assistants.
    val canGrant = Permissions.can("users.grant")
    val canManage = Permissions.can("users.manage")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FD))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Primary800)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "👑 لوحة التحكم بالصلاحيات والمساعدين (Super Admin RBAC)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "تحديد الصلاحيات المخصصة بدقة، اعتماد المساعدين، وإنشاء أذونات جديدة",
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary100
                        )
                    }
                }
            }
        }

        // Section 1: Pending Requests
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏳ طلبات الانضمام المعلقة (${pendingUsers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (pendingUsers.isNotEmpty()) Color(0xFFB45309) else Primary900
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (pendingUsers.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "لا توجد طلبات انضمام معلقة حالياً ✓",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF166534),
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    pendingUsers.forEach { user ->
                        PendingUserCard(
                            user = user,
                            onApprove = { approvingUser = user },
                            onReject = { rejectingUser = user },
                            canManage = canManage
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Section 2: Approved Staff & Assistants
        item {
            Column {
                Text(
                    text = "👥 المشرفون والمعاونون المعتمدون (${approvedUsers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary900
                )
                Spacer(modifier = Modifier.height(8.dp))

                approvedUsers.forEach { user ->
                    ApprovedUserCard(
                        user = user,
                        onEditPermissions = if (canGrant || (canManage && user.role == UserRole.ASSISTANT && user.id != currentUser?.id)) { { approvingUser = user } } else null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Approve / Edit User Permissions Dialog
    approvingUser?.let { user ->
        ApproveUserDialog(
            user = user,
            committees = committees,
            canGrant = canGrant,
            onDismiss = { approvingUser = null },
            onConfirm = { committeeIds, perms, role ->
                onApproveUser(user, committeeIds, perms, role)
                approvingUser = null
            }
        )
    }

    // Rejection keeps the account and records the reason, as on the web.
    rejectingUser?.let { user ->
        var reason by remember(user.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rejectingUser = null },
            title = { Text("الاعتذار عن طلب ${user.name}", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب الاعتذار (يظهر في الملف)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRejectUser(user, reason.ifBlank { null })
                    rejectingUser = null
                }) { Text("تأكيد الاعتذار") }
            },
            dismissButton = { TextButton(onClick = { rejectingUser = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
fun PendingUserCard(
    user: AppUser,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    canManage: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_user_${user.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(user.role.emoji, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Primary900
                        )
                        Text(
                            text = "الهاتف: ${user.phone} • الدور المطلوب: ${user.role.titleAr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB45309)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "طلب جديد",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB45309),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (user.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "التزكية/الملاحظات: ${user.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            if (canManage) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onReject) {
                        Text("رفض الطلب", color = Color(0xFFEF4444))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary700),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("اعتماد وتحديد الصلاحيات 🔑")
                    }
                }
            }
        }
    }
}

@Composable
fun ApprovedUserCard(
    user: AppUser,
    onEditPermissions: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("approved_user_${user.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePure),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Primary100),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(user.role.emoji, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Primary900
                        )
                        Text(
                            text = "${user.role.titleAr} • ${user.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onEditPermissions != null) {
                        TextButton(onClick = onEditPermissions) {
                            Text("تعديل الصلاحيات ✏️", style = MaterialTheme.typography.labelSmall, color = Primary700)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "معتمد ✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (user.assignedCommittees.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "اللجان المسندة: ${user.assignedCommittees.joinToString(", ") { "لجنة $it" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary700,
                    fontWeight = FontWeight.Bold
                )
            }

            if (user.permissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الصلاحيات الممنوحة: ${user.permissions.joinToString(" • ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ApproveUserDialog(
    user: AppUser,
    committees: List<Committee>,
    canGrant: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (committeeIds: List<String>?, permissions: List<String>?, role: UserRole?) -> Unit
) {
    // Nothing extra is pre-selected (MOB-09): the current settings are the starting point.
    var selectedRole by remember { mutableStateOf(user.role) }
    var selectedCommittees by remember { mutableStateOf(user.assignedCommitteeIds.toSet()) }
    var selectedPerms by remember { mutableStateOf(user.permissions.filter { id -> SystemPermissions.ALL_PERMISSIONS.any { it.first == id } }.toSet()) }
    // Managers who are not super admins may assign only their own committees.
    val choices = committees.filter { canGrant || Permissions.inScope(it.id) }
        .sortedWith(compareBy({ it.courseName }, { it.committeeNumber }))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (user.status == "approved") "تعديل: ${user.name}" else "اعتماد: ${user.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Primary900
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canGrant) {
                    Text("الدور:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Primary800)
                    listOf(UserRole.ASSISTANT, UserRole.TEACHER, UserRole.COMMITTEE_HEAD, UserRole.AUDITOR).forEach { role ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedRole = role },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(selected = selectedRole == role, onClick = { selectedRole = role })
                            Text(role.titleAr, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    Text(
                        text = "يُعتمد الحساب بدور «${user.role.titleAr}». الأدوار والصلاحيات يحددها المشرف العام؛ يمكنك إسناد لجانك فقط.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }

                Text("اللجان المسندة:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Primary800)
                if (choices.isEmpty()) Text("لا توجد لجان متاحة للإسناد", style = MaterialTheme.typography.bodySmall)
                choices.forEach { c ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedCommittees = if (c.id in selectedCommittees) selectedCommittees - c.id else selectedCommittees + c.id
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = c.id in selectedCommittees,
                            onCheckedChange = { checked -> selectedCommittees = if (checked) selectedCommittees + c.id else selectedCommittees - c.id }
                        )
                        Text(
                            text = "#${c.committeeNumber} ${c.committeeName}${if (c.courseName.isNotBlank()) " — ${c.courseName}" else ""}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (canGrant) {
                    Text("الصلاحيات (لا شيء محدد افتراضياً):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Primary800)
                    SystemPermissions.ALL_PERMISSIONS.forEach { (permId, label) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = permId in selectedPerms,
                                onCheckedChange = { checked -> selectedPerms = if (checked) selectedPerms + permId else selectedPerms - permId }
                            )
                            Text(text = label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val committeeIds = selectedCommittees.toList().takeIf { it.toSet() != user.assignedCommitteeIds.toSet() }
                    val perms = if (canGrant) selectedPerms.toList().takeIf { it.toSet() != user.permissions.toSet() } else null
                    val role = if (canGrant) selectedRole.takeIf { it != user.role } else null
                    onConfirm(committeeIds, perms, role)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary700)
            ) {
                Text(if (user.status == "approved") "حفظ ✓" else "اعتماد الحساب ✓")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
