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
    customPermissions: List<CustomPermission>,
    onApproveUser: (user: AppUser, committees: List<Int>, permissions: List<String>, role: UserRole) -> Unit,
    onRejectUser: (user: AppUser) -> Unit,
    onCreateCustomPermission: (name: String, description: String, feature: String, target: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var approvingUser by remember { mutableStateOf<AppUser?>(null) }
    var showCreatePermissionDialog by remember { mutableStateOf(false) }

    val pendingUsers = users.filter { it.status == "pending" }
    val approvedUsers = users.filter { it.status == "approved" }

    val isSuperAdmin = currentUser?.role == UserRole.SUPER_ADMIN
    val canManage = isSuperAdmin || (currentUser?.hasPermission(SystemPermissions.MANAGE_ASSISTANTS) == true)

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

        // Super Admin Dynamic Permission Creator Section
        if (isSuperAdmin) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إنشاء وتعيين صلاحية مخصصة جديدة",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF92400E)
                                )
                            }

                            Button(
                                onClick = { showCreatePermissionDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_create_custom_permission")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("➕ صلاحية جديدة", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "بصفتك السوبر أدمن، يمكنك بضغطة زر إنشاء أذن وصلاحية مخصصة بالاسم والتفاصيل التي تحددها (مثل: دخول امتحان كتاب التوحيد فقط، أو التحكم بلجنة محددة)، وتعيينها فورياً لأي مساعد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF78350F),
                            fontSize = 11.sp
                        )

                        if (customPermissions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "الصلاحيات المخصصة المنشأة حالياً (${customPermissions.size}):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            customPermissions.forEach { perm ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCD34D)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "🔑 ${perm.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                            if (perm.description.isNotBlank()) {
                                                Text(
                                                    text = perm.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF78350F),
                                                    fontSize = 11.sp
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
                                                text = "مخصصة",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GoldPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
                            onReject = { onRejectUser(user) },
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
                        onEditPermissions = if (canManage) { { approvingUser = user } } else null
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
            customPermissions = customPermissions,
            onDismiss = { approvingUser = null },
            onConfirm = { committees, perms, role ->
                onApproveUser(user, committees, perms, role)
                approvingUser = null
            }
        )
    }

    // Create Custom Permission Dialog
    if (showCreatePermissionDialog) {
        CreateCustomPermissionDialog(
            onDismiss = { showCreatePermissionDialog = false },
            onCreate = { name, desc, feature, target ->
                onCreateCustomPermission(name, desc, feature, target)
                showCreatePermissionDialog = false
            }
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
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ApproveUserDialog(
    user: AppUser,
    customPermissions: List<CustomPermission>,
    onDismiss: () -> Unit,
    onConfirm: (committees: List<Int>, permissions: List<String>, role: UserRole) -> Unit
) {
    var selectedRole by remember { mutableStateOf(user.role) }
    var commInput by remember { mutableStateOf(user.assignedCommittees.joinToString(",")) }
    var selectedPerms by remember {
        mutableStateOf(
            if (user.permissions.isNotEmpty()) user.permissions.toMutableSet()
            else mutableSetOf(SystemPermissions.DATABASE_SHEETS, SystemPermissions.CALENDAR_SCHEDULE)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "اعتماد/تعديل صلاحيات: ${user.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Primary900
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commInput,
                    onValueChange = { commInput = it },
                    label = { Text("أرقام اللجان المسندة (مثال: 1, 2, 5)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "الصلاحيات القياسية المتاحة:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary800
                )

                SystemPermissions.ALL_PERMISSIONS.forEach { (permId, label) ->
                    val isChecked = selectedPerms.contains(permId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                val newSet = selectedPerms.toMutableSet()
                                if (checked) newSet.add(permId) else newSet.remove(permId)
                                selectedPerms = newSet
                            }
                        )
                        Text(text = label, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (customPermissions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🔑 الصلاحيات المخصصة المنشأة بواسطة السوبر أدمن:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )

                    customPermissions.forEach { cPerm ->
                        val isChecked = selectedPerms.contains(cPerm.id) || selectedPerms.contains(cPerm.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val newSet = selectedPerms.toMutableSet()
                                    if (checked) {
                                        newSet.add(cPerm.id)
                                        newSet.add(cPerm.name)
                                    } else {
                                        newSet.remove(cPerm.id)
                                        newSet.remove(cPerm.name)
                                    }
                                    selectedPerms = newSet
                                }
                            )
                            Column {
                                Text(text = "🔑 ${cPerm.name}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                if (cPerm.description.isNotBlank()) {
                                    Text(text = cPerm.description, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val commList = commInput.split(",", "،").mapNotNull { it.trim().toIntOrNull() }
                    onConfirm(commList, selectedPerms.toList(), selectedRole)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary700)
            ) {
                Text("تفعيل وتحديث الصلاحيات ✓")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun CreateCustomPermissionDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, feature: String, target: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var feature by remember { mutableStateOf("exam_details") }
    var target by remember { mutableStateOf("كتاب التوحيد") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🔑 إنشاء صلاحية مخصصة جديدة (السوبر أدمن)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Primary900
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الصلاحية المخصصة (مثال: دخول تفاصيل امتحان التوحيد)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_custom_permission_name")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف والتفاصيل الدقيقة للصلاحية") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("الامتحان أو اللجنة أو المكون المحدد (Scope Target)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, description, feature, target)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
            ) {
                Text("حفظ وإنشاء الصلاحية 🔑")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
