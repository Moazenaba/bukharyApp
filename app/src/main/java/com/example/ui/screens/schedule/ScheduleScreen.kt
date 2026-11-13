package com.example.ui.screens.schedule

import androidx.compose.foundation.selection.selectable

import androidx.compose.material3.minimumInteractiveComponentSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.ScheduleTask
import com.example.data.remote.AssignableUserDto
import com.example.data.remote.Permissions
import com.example.ui.components.DateField
import com.example.ui.components.Dates
import com.example.ui.theme.Primary100
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.Primary900
import com.example.ui.theme.PriorityHighBg
import com.example.ui.theme.PriorityHighDot
import com.example.ui.theme.PriorityHighText
import com.example.ui.theme.PriorityNormalBg
import com.example.ui.theme.PriorityNormalDot
import com.example.ui.theme.PriorityNormalText
import com.example.ui.theme.PriorityUrgentBg
import com.example.ui.theme.PriorityUrgentDot
import com.example.ui.theme.PriorityUrgentText
import com.example.ui.theme.SurfacePure

@Composable
fun ScheduleScreen(
    currentUser: AppUser?,
    tasks: List<ScheduleTask>,
    onToggleTask: (String) -> Unit,
    onSaveTask: (ScheduleTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    committees: List<Committee> = emptyList(),
    assignableUsers: List<AssignableUserDto> = emptyList(),
    onLoadAssignableUsers: () -> Unit = {},
    onScheduleCeremony: ((Committee, com.example.data.model.CeremonyDetails) -> Unit)? = null,
    onOpenCommittee: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("all") } // all, pending, completed, urgent, mine, deliverables
    var selectedDate by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(Dates.today()) }
    var showCalendar by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
    var addingCeremony by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Controls follow the server's evaluated actions (MOB-10).
    val canManage = Permissions.can("tasks.write")
    val canDelete = Permissions.can("tasks.delete")

    val dayTasks = selectedDate?.let { d -> tasks.filter { it.date == d } } ?: tasks
    val filteredTasks = when (selectedFilter) {
        "pending" -> dayTasks.filter { !it.completed }
        "completed" -> dayTasks.filter { it.completed }
        "urgent" -> dayTasks.filter { it.priority == "urgent" }
        // Web: assigned to me, or linked to a committee assigned to me.
        "mine" -> dayTasks.filter { t ->
            t.assignedAssistantId == currentUser?.id || (t.committeeId != null && Permissions.committeeScope?.contains(t.committeeId) == true)
        }
        "deliverables" -> dayTasks.filter { it.deliverable.isNotBlank() }
        else -> dayTasks
    }

    val pendingCount = tasks.count { !it.completed }
    val completedCount = tasks.count { it.completed }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Primary700,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_task")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "جدولة مهمة جديدة")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FD))
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
            Column {
            Spacer(modifier = Modifier.height(12.dp))

            // Calendar Header Card
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
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "📅 تقويم وجدول المهام والتسليم الميداني",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "$pendingCount مهمة قيد التنفيذ • $completedCount مهمة مكتملة",
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary100
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.FilterChip(selected = showCalendar, onClick = { showCalendar = !showCalendar }, label = { Text(if (showCalendar) "إخفاء التقويم" else "عرض التقويم الشهري") })
                androidx.compose.material3.FilterChip(selected = selectedDate == null, onClick = { selectedDate = if (selectedDate == null) Dates.today() else null }, label = { Text("كل التواريخ") })
                if (onScheduleCeremony != null && Permissions.can("committees.finalize")) {
                    androidx.compose.material3.FilterChip(selected = false, onClick = { addingCeremony = true }, label = { Text("🎉 جدولة حفل") })
                }
            }
            if (showCalendar) {
                Spacer(modifier = Modifier.height(6.dp))
                MonthCalendar(tasks = tasks, selectedDate = selectedDate, onSelect = { selectedDate = it })
            }
            selectedDate?.let {
                Text(
                    "مهام يوم ${com.example.ui.components.arabicDayName(it)} ($it): ${dayTasks.size}",
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Primary800,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "all" to "كافة المهام (${dayTasks.size})",
                    "pending" to "قيد التنفيذ (${dayTasks.count { !it.completed }})",
                    "urgent" to "🔴 عاجل",
                    "mine" to "مهامي",
                    "deliverables" to "التسليمات",
                    "completed" to "المكتملة (${dayTasks.count { it.completed }})"
                ).forEach { (key, label) ->
                    val isSel = selectedFilter == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) Primary700 else SurfacePure)
                            .border(1.dp, if (isSel) Primary800 else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .minimumInteractiveComponentSize()
                            .selectable(selected = isSel, role = androidx.compose.ui.semantics.Role.Tab) { selectedFilter = key }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            }
            }
            if (filteredTasks.isEmpty()) {
                item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مهام مطابقة للتصفية المحددة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }
            }
            } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        ScheduleTaskCard(
                            task = task,
                            onToggle = { onToggleTask(task.id) },
                            onDelete = { onDeleteTask(task.id) },
                            canDelete = canDelete && !task.id.startsWith("task-ceremony-") // a ceremony task follows its committee
                        )
                        task.committeeId?.let { id ->
                            if (committees.any { it.id == id }) TextButton(onClick = { onOpenCommittee(id) }) { Text("تفاصيل اللجنة المرتبطة ↗", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
            }
        }
    }

    if (addingCeremony) {
        com.example.ui.screens.alerts.CeremonyQuickDialog(
            committees = committees.filter { Permissions.canOn("committees.finalize", it.id) },
            initialDate = selectedDate ?: Dates.today(),
            onDismiss = { addingCeremony = false },
            onSave = { c, d -> onScheduleCeremony?.invoke(c, d); addingCeremony = false }
        )
    }

    if (showAddDialog) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onLoadAssignableUsers() }
        AddTaskDialog(
            committees = committees,
            assignableUsers = assignableUsers,
            currentUserId = currentUser?.id,
            initialDate = selectedDate ?: Dates.today(),
            onDismiss = { showAddDialog = false },
            onSave = {
                onSaveTask(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ScheduleTaskCard(
    task: ScheduleTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val (badgeBg, badgeText, badgeDot) = when (task.priority) {
        "urgent" -> Triple(PriorityUrgentBg, PriorityUrgentText, PriorityUrgentDot)
        "high" -> Triple(PriorityHighBg, PriorityHighText, PriorityHighDot)
        else -> Triple(PriorityNormalBg, PriorityNormalText, PriorityNormalDot)
    }

    val typeLabel = when (task.type) {
        "delivery" -> "تسليم أوراق 📦"
        "receive" -> "استلام إجابات 📥"
        "exam" -> "انعقاد امتحان 🟦"
        "scoring" -> "تصحيح ورصد ✏️"
        "review" -> "تدقيق ومراجعة 🔍"
        "ceremony" -> "حفل وتكريم 🎓"
        else -> "مهمة عامة 📌"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_task_${task.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) Color(0xFFF8FAFC) else SurfacePure
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.completed) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "تبديل الإنجاز",
                    tint = if (task.completed) Color(0xFF16A34A) else Color(0xFF94A3B8),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (task.priority == "urgent") "عاجل" else if (task.priority == "high") "هام" else "عادي",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeText
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0F766E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        task.committeeNumber?.let {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• لجنة ($it)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary700,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (canDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (task.completed) Color(0xFF64748B) else Primary900
                )

                if (task.deliverable.isNotBlank()) {
                    Text(
                        text = "📦 المطلوب تسليمه: ${task.deliverable}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155),
                        fontSize = 12.sp
                    )
                }

                if (!task.assignedAssistantName.isNullOrBlank()) {
                    Text(
                        text = "المعاون المسؤول: ${task.assignedAssistantName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 ${task.date} • ⏰ ${task.time}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Bold
                    )

                    if (task.completed && !task.completedAt.isNullOrBlank()) {
                        Text(
                            text = "اكتمل في: ${task.completedAt}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF16A34A),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (ScheduleTask) -> Unit,
    committees: List<Committee> = emptyList(),
    assignableUsers: List<AssignableUserDto> = emptyList(),
    currentUserId: String? = null,
    initialDate: String = Dates.today()
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf("") }
    // Committee and assignee are chosen by id, like on the web; the server fills number and name (MOB-06).
    var committeeId by remember { mutableStateOf<String?>(null) }
    var assigneeId by remember { mutableStateOf(currentUserId) }
    var deliverable by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("normal") }
    var type by remember { mutableStateOf("general") }
    val committeeChoices = committees.filter { Permissions.inScope(it.id) }.sortedWith(compareBy({ it.courseName }, { it.committeeNumber }))

    val priorities = listOf("urgent" to "🔴 عاجل", "high" to "🟡 هام", "normal" to "🟢 عادي")
    val types = listOf(
        "delivery" to "تسليم 📦",
        "receive" to "استلام 📥",
        "exam" to "امتحان 🟦",
        "scoring" to "تصحيح ✏️",
        "review" to "تدقيق 🔍",
        "ceremony" to "حفل 🎓",
        "general" to "عام 📌"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "➕ جدولة مهمة ميدانية جديدة",
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
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان المهمة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateField(
                        value = date,
                        onValueChange = { date = it },
                        label = "التاريخ",
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("التوقيت") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                ChoiceField(
                    label = "اللجنة (اختياري)",
                    selected = committeeChoices.firstOrNull { it.id == committeeId }?.let { "#${it.committeeNumber} ${it.committeeName}" } ?: "بدون لجنة",
                    options = listOf<Pair<String?, String>>(null to "بدون لجنة") + committeeChoices.map {
                        it.id to "#${it.committeeNumber} ${it.committeeName}${if (it.courseName.isNotBlank()) " — ${it.courseName}" else ""}"
                    },
                    onSelect = { committeeId = it }
                )
                ChoiceField(
                    label = "المكلف",
                    selected = assignableUsers.firstOrNull { it.id == assigneeId }?.name ?: "بدون تكليف",
                    options = listOf<Pair<String?, String>>(null to "بدون تكليف") + assignableUsers.map { it.id to it.name },
                    onSelect = { assigneeId = it }
                )

                OutlinedTextField(
                    value = deliverable,
                    onValueChange = { deliverable = it },
                    label = { Text("المطلوب تسليمه / استلامه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "أولوية المهمة:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary800
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    priorities.forEach { (key, label) ->
                        val isSel = priority == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Primary100 else Color(0xFFF1F5F9))
                                .border(1.dp, if (isSel) Primary700 else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                .minimumInteractiveComponentSize().clickable { priority = key }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Primary800 else Color(0xFF475569)
                            )
                        }
                    }
                }

                Text(
                    text = "نوع المهمة:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary800
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { (key, label) ->
                        val isSel = type == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Primary700 else Color(0xFFF1F5F9))
                                .minimumInteractiveComponentSize().clickable { type = key }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && Dates.isRealDate(date)) {
                        val task = ScheduleTask(
                            id = "task-${java.util.UUID.randomUUID()}",
                            title = title,
                            date = date,
                            time = time,
                            committeeId = committeeId,
                            assignedAssistantId = assigneeId,
                            deliverable = deliverable,
                            priority = priority,
                            type = type
                        )
                        onSave(task)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary700)
            ) {
                Text("جدولة المهمة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}


/** A read-only field that opens a list of choices (committee, assignee). */
@Composable
private fun <T> ChoiceField(label: String, selected: String, options: List<Pair<T, String>>, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Box(modifier = Modifier.matchParentSize().clickable { open = true })
        androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (value, text) ->
                androidx.compose.material3.DropdownMenuItem(text = { Text(text) }, onClick = {
                    onSelect(value)
                    open = false
                })
            }
        }
    }
}
