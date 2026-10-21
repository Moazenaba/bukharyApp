package com.example.ui.screens.alerts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.AppUser
import com.example.data.model.CeremonyDetails
import com.example.data.model.Committee
import com.example.data.model.ScheduleTask
import com.example.data.model.UserRole
import com.example.data.remote.AssignableUserDto
import com.example.data.remote.Permissions
import com.example.ui.components.DateField
import com.example.ui.components.Dates
import com.example.ui.components.ScreenBanner
import com.example.ui.components.StatTile
import com.example.ui.components.arabicDayName
import com.example.ui.screens.schedule.AddTaskDialog
import com.example.ui.theme.Primary800
import com.example.ui.theme.TextMuted
import java.util.Calendar

private val MONTHS = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")

private fun longArabicDate(iso: String): String {
    val millis = Dates.toMillis(iso) ?: return iso
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return "${arabicDayName(iso)} ${c.get(Calendar.DAY_OF_MONTH)} ${MONTHS[c.get(Calendar.MONTH)]} ${c.get(Calendar.YEAR)}م"
}

private fun typeLabel(type: String) = when (type) {
    "ceremony" -> "🎉 حفل وتكريم"
    "delivery" -> "📦 تسليم"
    "receive" -> "📥 استلام"
    "exam" -> "📝 امتحان"
    "scoring" -> "✍️ تصحيح"
    "review" -> "🔍 مراجعة"
    else -> "📌 عمل مجدول"
}

/** Web: DailyAlertsView — the day's tasks and ceremonies, by day or for the whole schedule. */
@Composable
fun DailyAlertsScreen(
    currentUser: AppUser?,
    tasks: List<ScheduleTask>,
    committees: List<Committee>,
    assignableUsers: List<AssignableUserDto>,
    onLoadAssignableUsers: () -> Unit,
    onToggleTask: (String) -> Unit,
    onSaveTask: (ScheduleTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onScheduleCeremony: (Committee, CeremonyDetails) -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateCeremonies: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = Dates.today()
    var selectedDate by rememberSaveable { mutableStateOf(today) }
    var allSchedule by rememberSaveable { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }
    var addingTask by remember { mutableStateOf(false) }
    var addingCeremony by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ScheduleTask?>(null) }
    val canWriteTasks = Permissions.can("tasks.write")
    val canDeleteTasks = Permissions.can("tasks.delete")
    val canScheduleCeremonies = Permissions.can("committees.finalize")

    val quickDays = remember(today) {
        (0 until 7).map { i ->
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, i) }
            val iso = Dates.fromMillis(c.timeInMillis)
            val label = "${arabicDayName(iso)} ${c.get(Calendar.DAY_OF_MONTH)}/${c.get(Calendar.MONTH) + 1}"
            iso to if (i == 0) "اليوم ($label)" else label
        }
    }
    val viewTasks = if (allSchedule) tasks else tasks.filter { it.date == selectedDate }
    val done = viewTasks.count { it.completed }
    val pending = viewTasks.size - done
    val ceremoniesToday = tasks.filter { it.type == "ceremony" && it.date == selectedDate }
    val q = query.trim().lowercase()
    val shown = viewTasks.filter { t ->
        (status == "all" || (status == "pending" && !t.completed) || (status == "completed" && t.completed)) && (
            q.isEmpty() || t.title.lowercase().contains(q) || (t.assignedAssistantName ?: "").lowercase().contains(q) ||
                t.deliverable.lowercase().contains(q) || t.notes.lowercase().contains(q) ||
                (t.committeeNumber?.toString()?.contains(q) == true) || t.date.contains(q))
    }.sortedWith(compareBy({ it.completed }, { it.date }, { it.time }))

    LazyColumn(
        modifier.fillMaxSize().background(Color(0xFFF8F9FD)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenBanner(
                "التنبيهات والمهام اليومية",
                "📅 ${if (allSchedule) "كافة المواعيد المجدولة" else longArabicDate(selectedDate)} — مربوطة بجدول تقويم المواعيد.",
                Icons.Default.NotificationsActive, listOf(Primary800, Color(0xFF0F766E))
            ) {
                Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (canScheduleCeremonies) OutlinedButton(onClick = { addingCeremony = true }) { Text("+ إضافة وجدولة حفلة", color = Color.White) }
                    if (canWriteTasks) OutlinedButton(onClick = { onLoadAssignableUsers(); addingTask = true }) { Text("+ إضافة عمل / تنبيه لليوم", color = Color.White) }
                    OutlinedButton(onClick = onNavigateCalendar) { Text("عرض جدول التقويم الكامل ↗", color = Color.White) }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = !allSchedule, onClick = { allSchedule = false }, label = { Text("اليوم المحدد (${tasks.count { it.date == selectedDate }})") })
                FilterChip(selected = allSchedule, onClick = { allSchedule = true }, label = { Text("كافة المواعيد (${tasks.size})") })
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                quickDays.forEach { (iso, label) ->
                    val n = tasks.count { it.date == iso }
                    FilterChip(
                        selected = !allSchedule && selectedDate == iso,
                        onClick = { selectedDate = iso; allSchedule = false },
                        label = { Text(if (n > 0) "$label ($n)" else label) }
                    )
                }
            }
            DateField(selectedDate, { selectedDate = it; allSchedule = false }, "اختر يوماً من التقويم")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(if (allSchedule) "إجمالي المهام" else "مهام اليوم المحدد", "${viewTasks.size}", Modifier.weight(1f))
                StatTile("قيد المتابعة", "$pending", Modifier.weight(1f), tint = Color(0xFFD97706))
                StatTile("المنجز (${if (viewTasks.isEmpty()) 100 else done * 100 / viewTasks.size}%)", "$done", Modifier.weight(1f), tint = Color(0xFF059669))
            }
        }
        if (ceremoniesToday.isNotEmpty()) item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFBBF24)), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("🎉 تنبيه حفل وتكريم اليوم", fontWeight = FontWeight.Black)
                    Text("اليوم موعد إقامة حفل تكريم وتوزيع جوائز (${ceremoniesToday.size}) لجنة!", fontWeight = FontWeight.Bold)
                    Text(ceremoniesToday.joinToString(" • ") { "لجنة #${it.committeeNumber ?: ""} (${it.title})" }, style = MaterialTheme.typography.bodySmall)
                    Text("📍 يرجى تجهيز شهادات التقدير والدروع التذكارية وكشوف التكريم.", style = MaterialTheme.typography.labelSmall)
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val rows = com.example.ui.screens.export.ReportSheets.ceremonyRows(committees, tasks, "day", selectedDate)
                        TextButton(
                            onClick = {
                                com.example.ui.components.shareFile(
                                    context,
                                    com.example.data.SimpleXlsx.write(
                                        "الحفلات (يومي)", com.example.ui.screens.export.ReportSheets.CEREMONY_HEADER, rows,
                                        com.example.ui.screens.export.ReportSheets.CEREMONY_WIDTHS
                                    ),
                                    com.example.ui.screens.export.ReportSheets.ceremonyFileName("day", selectedDate), com.example.ui.components.XLSX_MIME
                                )
                            },
                            enabled = rows.isNotEmpty()
                        ) { Text("كشف حفلات اليوم Excel", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold) }
                        TextButton(onClick = onNavigateCeremonies) { Text("فتح تاب الحفلات والتكريم ↗", color = Color(0xFF0F172A)) }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("بحث في المهام، المكلف، التاريخ، اللجنة...") }, shape = RoundedCornerShape(14.dp)
            )
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = status == "all", onClick = { status = "all" }, label = { Text("الكل (${viewTasks.size})") })
                FilterChip(selected = status == "pending", onClick = { status = "pending" }, label = { Text("المتبقي ($pending)") })
                FilterChip(selected = status == "completed", onClick = { status = "completed" }, label = { Text("المنجز ($done)") })
            }
        }
        if (shown.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (viewTasks.isEmpty()) "لا توجد مهام مسجلة ليوم (${longArabicDate(selectedDate)})" else "لا توجد نتائج تطابق معايير التصفية الحالية",
                    fontWeight = FontWeight.Bold
                )
                if (canWriteTasks) TextButton(onClick = { onLoadAssignableUsers(); addingTask = true }) { Text("+ إضافة عمل ليوم ${arabicDayName(selectedDate)}") }
            }
        }
        items(shown, key = { it.id }) { t ->
            val canToggle = (canWriteTasks || t.assignedAssistantId == currentUser?.id)
            Card(
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(
                    if (t.type == "ceremony" && !t.completed) 2.dp else 1.dp,
                    when { t.completed -> Color(0xFFA7F3D0); t.type == "ceremony" -> Color(0xFFFBBF24); t.priority == "urgent" -> Color(0xFFFDA4AF); else -> Color(0xFFE2E8F0) }
                )
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    androidx.compose.material3.IconToggleButton(checked = t.completed, onCheckedChange = { onToggleTask(t.id) }, enabled = canToggle) {
                        Icon(
                            if (t.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (t.completed) "إلغاء تحديد «${t.title}» كمنجز" else "تحديد «${t.title}» كمنجز",
                            tint = if (t.completed) Color(0xFF059669) else Color(0xFF94A3B8)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(typeLabel(t.type), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("📅 ${t.date}", style = MaterialTheme.typography.labelSmall)
                            t.committeeNumber?.let { Text("لجنة #$it", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0F766E)) }
                            Text(t.time.ifBlank { "بدون وقت محدد" }, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            if (t.priority == "urgent" && !t.completed) Text("⚠️ عاجل", style = MaterialTheme.typography.labelSmall, color = Color(0xFFBE123C))
                        }
                        Text(
                            t.title, fontWeight = FontWeight.Bold,
                            textDecoration = if (t.completed) TextDecoration.LineThrough else null,
                            color = if (t.completed) Color(0xFF94A3B8) else Color(0xFF0F172A)
                        )
                        if (t.deliverable.isNotBlank()) Text("📋 المطلوب: ${t.deliverable}", style = MaterialTheme.typography.bodySmall)
                        if (t.notes.isNotBlank()) Text("💬 ${t.notes}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("👤 المكلف: ${t.assignedAssistantName ?: "غير محدد"}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    if (canDeleteTasks) IconButton(onClick = { deleting = t }) { Icon(Icons.Default.Delete, "حذف هذا التنبيه", tint = Color(0xFFEF4444)) }
                }
            }
        }
        item { Spacer(Modifier.padding(24.dp)) }
    }

    if (addingTask) {
        AddTaskDialog(
            onDismiss = { addingTask = false },
            onSave = { onSaveTask(it); addingTask = false },
            committees = committees,
            assignableUsers = assignableUsers,
            currentUserId = currentUser?.id,
            initialDate = selectedDate
        )
    }
    deleting?.let { t ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف التنبيه") },
            text = { Text("حذف «${t.title}»؟") },
            confirmButton = { TextButton(onClick = { deleting = null; onDeleteTask(t.id) }) { Text("حذف", color = Color(0xFFDC2626)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("إلغاء") } }
        )
    }
    if (addingCeremony) {
        CeremonyQuickDialog(
            committees = committees.filter { Permissions.canOn("committees.finalize", it.id) },
            initialDate = selectedDate,
            onDismiss = { addingCeremony = false },
            onSave = { c, details -> onScheduleCeremony(c, details); addingCeremony = false }
        )
    }
}

/** «إضافة وجدولة حفلة»: pick a committee, then the day, time, place, honourees and attending sheikhs. */
@Composable
fun CeremonyQuickDialog(
    committees: List<Committee>,
    initialDate: String,
    onDismiss: () -> Unit,
    onSave: (Committee, CeremonyDetails) -> Unit
) {
    var committeeId by remember { mutableStateOf(committees.firstOrNull()?.id ?: "") }
    val committee = committees.find { it.id == committeeId }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var honored by remember(committeeId) { mutableStateOf((committee?.studentsCount ?: 0).toString()) }
    var sheikhs by remember(committeeId) { mutableStateOf(listOfNotNull(committee?.sheikhName?.takeIf { it.isNotBlank() })) }
    var newSheikh by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة وجدولة حفلة تكريم") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(filter, { filter = it }, label = { Text("بحث عن اللجنة") }, singleLine = true)
                Column {
                    committees.filter { filter.isBlank() || it.committeeNumber.toString() == filter.trim() || it.committeeName.contains(filter) }.take(30).forEach { c ->
                        FilterChip(
                            selected = committeeId == c.id, onClick = { committeeId = c.id },
                            label = { Text("#${c.committeeNumber} ${c.committeeName}${if (c.courseName.isNotBlank()) " — ${c.courseName}" else ""}") }
                        )
                    }
                }
                DateField(date, { date = it }, "تاريخ الحفل (يوم ${arabicDayName(date)})")
                OutlinedTextField(time, { time = it }, label = { Text("وقت الحفل") }, placeholder = { Text("مثال: 4 العصر") }, singleLine = true)
                OutlinedTextField(place, { place = it }, label = { Text("مكان الحفل") }, singleLine = true)
                OutlinedTextField(
                    honored, { honored = it.filter(Char::isDigit) }, label = { Text("عدد الطلاب المكرمين") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text("المشايخ الحاضرون", fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    sheikhs.forEachIndexed { i, s ->
                        InputChip(selected = false, onClick = { sheikhs = sheikhs.filterIndexed { j, _ -> j != i } }, label = { Text("$s ✕") })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(newSheikh, { newSheikh = it }, label = { Text("إضافة شيخ") }, singleLine = true, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val n = newSheikh.trim()
                        if (n.isNotEmpty() && n !in sheikhs) sheikhs = sheikhs + n
                        newSheikh = ""
                    }) { Text("إضافة") }
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات وترتيبات") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(enabled = committee != null && date.isNotBlank(), onClick = {
                val c = committee ?: return@Button
                onSave(c, c.ceremonyDetails.copy(date = date, time = time.trim(), venue = place.trim(), honoreesCount = honored.toIntOrNull() ?: 0, attendingSheikhs = sheikhs, notes = notes.trim()))
            }) { Text("حفظ وجدولة الحفل") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

