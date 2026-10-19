package com.example.ui.screens.courses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Committee
import com.example.data.model.Course
import com.example.data.remote.Permissions
import com.example.ui.components.DateField
import com.example.ui.components.Dates
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.Primary900
import com.example.ui.theme.TextMuted

private val STATUS_LABELS = mapOf("active" to "نشطة حالياً", "upcoming" to "قادمة (مستقبلية)", "completed" to "مكتملة ومؤرشفة")
private val SEASONS = listOf("صيفية" to "دورة صيفية", "شتوية" to "دورة شتوية", "رمضانية" to "دورة رمضانية", "مكثفة" to "دورة مكثفة", "سنوية" to "مسابقة سنوية")

/** Courses (web: CoursesView): list by status, add, edit, delete, set the current course. */
@Composable
fun CoursesScreen(
    courses: List<Course>,
    committees: List<Committee>,
    onSaveCourse: (Course, (Boolean) -> Unit) -> Unit,
    onDeleteCourse: (Course) -> Unit,
    onSetDefaultCourse: (Course) -> Unit,
    onShowCourseCommittees: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val canWrite = Permissions.can("courses.write")
    var filter by rememberSaveable { mutableStateOf("all") }
    var editing by remember { mutableStateOf<Course?>(null) }
    var adding by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Course?>(null) }
    val shown = courses.filter { filter == "all" || it.status == filter }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Color(0xFFF8F9FD)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF115E59), Primary800)))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EventRepeat, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("إدارة الدورات والمسابقات الموسمية", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "تنظيم مسابقات مسجد البخاري على مستوى الدورات. تُربط اللجان بكل دورة، وتُحفظ الدورات المكتملة بأرشيفها.",
                        color = Color(0xFFCCFBF1), style = MaterialTheme.typography.bodySmall
                    )
                    if (canWrite) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { adding = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Primary800),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(4.dp))
                            Text("إضافة دورة جديدة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "all" to "جميع الدورات (${courses.size})",
                    "active" to "النشطة (${courses.count { it.status == "active" }})",
                    "completed" to "المكتملة والمؤرشفة (${courses.count { it.status == "completed" }})",
                    "upcoming" to "القادمة (${courses.count { it.status == "upcoming" }})"
                ).forEach { (key, label) ->
                    FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label) })
                }
            }
            Text("إجمالي اللجان المسجلة بالمنظومة: ${committees.size} لجنة", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        if (shown.isEmpty()) {
            item { Text("لا توجد دورات في هذا التصنيف", color = TextMuted, modifier = Modifier.padding(24.dp)) }
        }
        items(shown, key = { it.id }) { course ->
            val comms = committees.filter { it.courseId == course.id }
            val done = comms.count { it.stages.ceremony.completed }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(if (course.isDefault) 2.dp else 1.dp, if (course.isDefault) Color(0xFF10B981) else Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${course.season} • ${course.year}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(course.name, fontWeight = FontWeight.Bold, color = Primary900)
                        }
                        if (course.isDefault) {
                            Text(
                                "الدورة الحالية", color = Color.White, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.background(Color(0xFF059669), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        } else {
                            Text(STATUS_LABELS[course.status] ?: course.status, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                    if (course.description.isNotBlank()) {
                        Text(course.description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp)).padding(8.dp).fillMaxWidth())
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox("اللجان المرتبطة", "${comms.size} لجنة", "(الهدف: ${course.targetCommitteesCount.takeIf { it > 0 } ?: 30})", Modifier.weight(1f))
                        StatBox(
                            "المراحل المكتملة", "$done من ${comms.size}",
                            "${if (comms.isNotEmpty()) done * 100 / comms.size else 0}% إنجاز", Modifier.weight(1f)
                        )
                    }
                    Text(
                        "الفترة: ${course.startDate}${if (course.endDate.isNotBlank()) " إلى ${course.endDate}" else " (مفتوحة)"}",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted
                    )
                    Button(
                        onClick = { onShowCourseCommittees(course.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary700),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Groups, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("عرض لجان هذه الدورة (${comms.size})")
                    }
                    if (canWrite) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!course.isDefault) TextButton(onClick = { onSetDefaultCourse(course) }) { Text("✓ تعيين كدورة نشطة", fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { editing = course }) { Text("تعديل", color = Color(0xFF92400E)) }
                            if (courses.size > 1) TextButton(onClick = { confirmDelete = course }) { Text("حذف", color = Color(0xFFDC2626)) }
                        }
                    }
                }
            }
        }
    }

    if (adding || editing != null) {
        CourseFormDialog(
            course = editing,
            onDismiss = { adding = false; editing = null },
            onSave = { c -> onSaveCourse(c) { ok -> if (ok) { adding = false; editing = null } } }
        )
    }
    confirmDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("حذف الدورة") },
            text = { Text("هل تريد حذف «${c.name}»؟ لا يمكن حذف دورة مرتبطة بلجان.") },
            confirmButton = { TextButton(onClick = { confirmDelete = null; onDeleteCourse(c) }) { Text("حذف", color = Color(0xFFDC2626)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun StatBox(title: String, value: String, sub: String, modifier: Modifier) {
    Column(modifier.background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp)).padding(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, fontWeight = FontWeight.Bold, color = Primary700)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

/** Add / edit form with the web's fields; a new active course becomes the current one, as on the web. */
@Composable
fun CourseFormDialog(course: Course?, onDismiss: () -> Unit, onSave: (Course) -> Unit) {
    var name by remember { mutableStateOf(course?.name ?: "") }
    var season by remember { mutableStateOf(course?.season ?: "صيفية") }
    var year by remember { mutableStateOf(course?.year ?: "") }
    var startDate by remember { mutableStateOf(course?.startDate ?: Dates.today()) }
    var endDate by remember { mutableStateOf(course?.endDate ?: "") }
    var status by remember { mutableStateOf(course?.status ?: "active") }
    var target by remember { mutableStateOf((course?.targetCommitteesCount?.takeIf { it > 0 } ?: 30).toString()) }
    var description by remember { mutableStateOf(course?.description ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (course == null) "إضافة دورة مسابقات جديدة" else "تعديل بيانات الدورة", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                error?.let { Text(it, color = Color(0xFFDC2626)) }
                OutlinedTextField(name, { name = it; error = null }, label = { Text("اسم الدورة *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("الموسم / النوع", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val options = if (SEASONS.none { it.first == season }) SEASONS + (season to season) else SEASONS
                    options.forEach { (key, label) -> FilterChip(selected = season == key, onClick = { season = key }, label = { Text(label) }) }
                }
                OutlinedTextField(year, { year = it }, label = { Text("العام / السنة") }, placeholder = { Text("1448هـ / 2026م") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                DateField(startDate, { startDate = it }, "تاريخ البداية")
                DateField(endDate, { endDate = it }, "تاريخ النهاية المتوقع", allowClear = true)
                Text("حالة الدورة", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    STATUS_LABELS.forEach { (key, label) -> FilterChip(selected = status == key, onClick = { status = key }, label = { Text(label) }) }
                }
                OutlinedTextField(
                    target, { target = it.filter(Char::isDigit) }, label = { Text("عدد اللجان المستهدف") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(description, { description = it }, label = { Text("وصف الدورة والأهداف") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) { error = "اسم الدورة مطلوب"; return@Button }
                onSave(
                    Course(
                        id = course?.id ?: "course-${java.util.UUID.randomUUID()}",
                        name = name.trim(),
                        season = season,
                        year = year.trim(),
                        startDate = startDate.ifBlank { Dates.today() },
                        endDate = endDate,
                        status = status,
                        // Same rule as the web: a new active course becomes the current one; editing keeps it.
                        isDefault = course?.isDefault ?: (status == "active"),
                        targetCommitteesCount = target.toIntOrNull() ?: 30,
                        description = description.trim(),
                        createdAt = course?.createdAt ?: Dates.today()
                    )
                )
            }, colors = ButtonDefaults.buttonColors(containerColor = Primary700)) { Text("حفظ الدورة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

