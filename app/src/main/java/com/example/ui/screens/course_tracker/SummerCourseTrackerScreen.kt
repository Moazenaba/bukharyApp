package com.example.ui.screens.course_tracker

import com.example.ui.components.ChipFlow
import com.example.ui.theme.tEdge
import com.example.ui.theme.tBg
import com.example.ui.theme.tFg
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.ROW_STAGE
import com.example.data.SheetRow
import com.example.data.SheetRules
import com.example.data.SimpleXlsx
import com.example.data.model.Committee
import com.example.data.model.Course
import com.example.data.model.StagesMap
import com.example.data.remote.Permissions
import com.example.data.remote.SheetRowDto
import com.example.ui.components.Dates
import com.example.ui.components.ScreenBanner
import com.example.ui.components.StatTile
import com.example.ui.screens.courses.CourseFormDialog
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.TextMuted

private val FLAGS = listOf("delivered" to "الامتحان", "corrected" to "التصحيح", "reviewed" to "المراجعة", "announced" to "الإعلان", "ceremony" to "الحفل")

private fun SheetRowDto.flag(key: String) = when (key) {
    "delivered" -> delivered; "corrected" -> corrected; "reviewed" -> reviewed; "announced" -> announced; else -> ceremony
}

private fun SheetRowDto.withFlag(key: String, v: Boolean) = when (key) {
    "delivered" -> copy(delivered = v); "corrected" -> copy(corrected = v); "reviewed" -> copy(reviewed = v)
    "announced" -> copy(announced = v); else -> copy(ceremony = v)
}

/**
 * Web: SummerCourseTrackerView. The course sheet merges saved rows with committees; managers edit
 * rows, import Excel/CSV or a photo (OCR), export, add rows and courses. Linked rows follow the
 * committee's stages, so ticking one changes the stage itself.
 */
@Composable
fun SummerCourseTrackerScreen(
    courses: List<Course>,
    selectedCourseId: String?,
    committees: List<Committee>,
    sheetRows: List<SheetRowDto> = emptyList(),
    onSelectCourse: (String?) -> Unit,
    onToggleStage: (committeeId: String, stageKey: String, currentStatus: Boolean) -> Unit,
    onOpenCommitteeDetail: (String) -> Unit,
    onSaveRow: (SheetRowDto, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onDeleteRow: (SheetRowDto) -> Unit = {},
    onImportRows: (List<SheetRowDto>, String, Boolean, (Boolean) -> Unit) -> Unit = { _, _, _, _ -> },
    onExtractFromImage: (ByteArray, (List<SheetRowDto>?) -> Unit) -> Unit = { _, _ -> },
    onAddCourse: (Course, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onCreateCommitteeFromRow: (Committee) -> Unit = {},
    /** Saves sheikh name, place, book and students on the committee itself (rows linked to a committee). */
    onSaveCommittee: (Committee) -> Unit = {},
    onExport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val canEdit = Permissions.can("summerRows.write")
    val canCreateCommittees = Permissions.can("committees.create")
    val canWriteCourses = Permissions.can("courses.write")
    val canOcr = Permissions.can("ocr.use")
    val defaultCourse = courses.find { it.isDefault } ?: courses.find { it.status == "active" }

    var query by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("all") }
    var book by rememberSaveable { mutableStateOf("all") }
    var place by rememberSaveable { mutableStateOf("all") }
    var editing by remember { mutableStateOf<SheetRow?>(null) }
    var deleting by remember { mutableStateOf<SheetRowDto?>(null) }
    var addingRow by remember { mutableStateOf(false) }
    var addingCourse by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<List<SheetRowDto>?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var extracting by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }

    val rows = remember(sheetRows, committees) { SheetRules.merge(sheetRows, committees) }
    val importCourseId = selectedCourseId ?: defaultCourse?.id

    fun handleExtracted(extracted: List<SheetRowDto>) {
        if (extracted.isEmpty()) { importError = "لم يتم العثور على أي صفوف"; return }
        val courseId = importCourseId ?: run { importError = "اختر الدورة التي تريد استيراد الكشف إليها أولاً"; return }
        if (sheetRows.isEmpty()) onImportRows(extracted.map { it.copy(id = "row-${java.util.UUID.randomUUID()}") }, courseId, false) {}
        else pendingImport = extracted
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
            val isZip = bytes.size > 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
            val table = if (isZip) SimpleXlsx.read(bytes) else SimpleXlsx.parseCsv(bytes)
            handleExtracted(SheetRules.parseImported(table))
        } catch (e: Exception) {
            importError = "تعذر قراءة ملف الإكسيل. يرجى التأكد من صيغة الملف."
        }
    }
    var workbenchUri by remember { mutableStateOf<Uri?>(null) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> workbenchUri = uri }
    workbenchUri?.let { uri ->
        ImageWorkbenchDialog(uri, onDismiss = { workbenchUri = null }) { bytes ->
            workbenchUri = null
            extracting = true
            onExtractFromImage(bytes) { result -> extracting = false; result?.let { handleExtracted(it) } }
        }
    }

    val books = rows.map { it.row.book.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    val places = rows.map { it.row.place.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    val q = query.trim().lowercase()
    val shown = rows.filter { sr ->
        val r = sr.row
        val matchesQuery = q.isEmpty() || r.sheikh_name.lowercase().contains(q) || r.place.lowercase().contains(q) ||
            r.book.lowercase().contains(q) || r.notes.lowercase().contains(q) || r.number.toString().contains(q)
        val matchesStatus = when (status) {
            "not_delivered" -> !r.delivered; "delivered" -> r.delivered; "corrected" -> r.corrected; "reviewed" -> r.reviewed
            "announced" -> r.announced; "no_ceremony" -> !r.ceremony; "ceremony" -> r.ceremony; "uncertain" -> r.uncertain; "struck_out" -> r.struck_out
            else -> true
        }
        val matchesCourse = selectedCourseId == null || (r.courseId ?: defaultCourse?.id) == selectedCourseId
        matchesQuery && matchesStatus && (book == "all" || r.book.trim() == book) && (place == "all" || r.place.trim() == place) && matchesCourse
    }
    val inCourse = rows.filter { selectedCourseId == null || (it.row.courseId ?: defaultCourse?.id) == selectedCourseId }.map { it.row }

    LazyColumn(
        modifier.fillMaxSize().background(tBg(Color(0xFFF8F9FD))),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenBanner(
                "كشف ومصفوفة متابعة الدورة",
                "نظام المتابعة الإدارية للكشوفات ورصد درجات المتون العلمية، مربوط بلجان المسابقة.",
                Icons.Default.TableChart, listOf(Color(0xFF003B36), Primary800)
            ) {
                ChipFlow(Modifier.padding(top = 8.dp), 6.dp) {
                    if (canWriteCourses) OutlinedButton(onClick = { addingCourse = true }) { Text("+ دورة جديدة", color = Color.White) }
                    if (canCreateCommittees) OutlinedButton(onClick = { addingRow = true }) { Text("+ إضافة صف ولجنة", color = Color.White) }
                    if (canEdit) OutlinedButton(onClick = { pickFile.launch("*/*") }) { Text("استيراد Excel/CSV", color = Color.White) }
                    if (canOcr) OutlinedButton(onClick = { pickImage.launch("image/*") }) { Text("استخراج من صورة", color = Color.White) }
                    OutlinedButton(onClick = onExport) { Text("تصدير", color = Color.White) }
                    if (canEdit && sheetRows.isNotEmpty()) OutlinedButton(onClick = { confirmClear = true }) { Text("تفريغ الكشف", color = tFg(Color(0xFFFECACA))) }
                }
            }
        }
        if (extracting) item {
            Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(); Spacer(Modifier.width(8.dp)); Text("جارٍ قراءة الجدول من الصورة...") }
        }
        item {
            ChipFlow(Modifier, 6.dp) {
                FilterChip(selected = selectedCourseId == null, onClick = { onSelectCourse(null) }, label = { Text("كافة الدورات (${rows.size})") })
                courses.forEach { c ->
                    FilterChip(
                        selected = selectedCourseId == c.id, onClick = { onSelectCourse(c.id) },
                        label = { Text("${c.name}${if (c.isDefault) " ★" else ""} (${rows.count { (it.row.courseId ?: defaultCourse?.id) == c.id }})") }
                    )
                }
            }
        }
        item {
            val active = inCourse.count { !it.struck_out }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("إجمالي الصفوف", "${inCourse.size}", Modifier.weight(1f))
                    StatTile("الفعّالة", "$active", Modifier.weight(1f))
                    StatTile("الطلاب", "${inCourse.sumOf { it.students_count }}", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("الامتحان", "${inCourse.count { it.delivered }}", Modifier.weight(1f), tint = tFg(Color(0xFF1D4ED8)))
                    StatTile("التصحيح", "${inCourse.count { it.corrected }}", Modifier.weight(1f), tint = tFg(Color(0xFFB45309)))
                    StatTile("المراجعة", "${inCourse.count { it.reviewed }}", Modifier.weight(1f), tint = tFg(Color(0xFF0F766E)))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("الإعلان", "${inCourse.count { it.announced }}", Modifier.weight(1f), tint = tFg(Color(0xFF0E7490)))
                    StatTile("الحفل", "${inCourse.count { it.ceremony }}", Modifier.weight(1f), tint = tFg(Color(0xFF047857)))
                    StatTile("يحتاج تدقيق", "${inCourse.count { it.uncertain }}", Modifier.weight(1f), tint = tFg(Color(0xFFB91C1C)))
                }
            }
        }
        item {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("بحث بالرقم، الشيخ، المكان، الكتاب، الملاحظات...") }, shape = RoundedCornerShape(14.dp)
            )
            ChipFlow(Modifier.padding(top = 6.dp), 6.dp) {
                listOf(
                    "all" to "الكل", "not_delivered" to "في انتظار الامتحان", "delivered" to "تم الامتحان", "corrected" to "تم التصحيح",
                    "reviewed" to "تمت المراجعة", "announced" to "تم الإعلان", "no_ceremony" to "لم يتم الحفل", "ceremony" to "تم الحفل", "uncertain" to "يحتاج تدقيق", "struck_out" to "مشطوب"
                ).forEach { (k, l) -> FilterChip(selected = status == k, onClick = { status = k }, label = { Text(l) }) }
            }
            // PDF of the rows on screen: pick «لم يتم الحفل» or «تم الحفل» first to get that list.
            val statusLabel = STATUS_LABELS[status].orEmpty()
            Row(Modifier.padding(top = 6.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fun pdfParts(): Triple<String, String, List<List<Any?>>> {
                    val list = shown.map { it.row }
                    val title = "كشف الدورة${if (statusLabel.isNotEmpty() && status != "all") " — $statusLabel" else ""} — مسابقات مسجد الإمام البخاري"
                    val total = list.sumOf { it.students_count }
                    val subtitle = listOfNotNull(defaultCourse?.name, Dates.today(), "${list.size} لجنة", "$total طالب").joinToString(" — ")
                    fun mark(v: Boolean) = if (v) "✓" else "—"
                    val rows = list.map { r ->
                        listOf<Any?>(r.number, r.sheikh_name.ifBlank { "-" }, r.place.ifBlank { "-" }, r.book.ifBlank { "-" }, r.students_count,
                            mark(r.delivered), mark(r.corrected), mark(r.reviewed), mark(r.announced), mark(r.ceremony))
                    } + listOf(listOf<Any?>("", "الإجمالي: ${list.size} لجنة", "", "", total, "", "", "", "", ""))
                    return Triple(title, subtitle, rows)
                }
                OutlinedButton(
                    onClick = {
                        val (title, subtitle, rows) = pdfParts()
                        val body = StringBuilder("<h2 class='center'>${com.example.ui.components.escapeHtml(title)}</h2><p class='center'>${com.example.ui.components.escapeHtml(subtitle)}</p><table><tr>")
                        SHEET_PDF_HEADER.forEach { body.append("<th>$it</th>") }
                        body.append("</tr>")
                        rows.forEachIndexed { i, r ->
                            body.append(if (i == rows.lastIndex) "<tr style='font-weight:bold;background:#f1f5f9'>" else "<tr>")
                            r.forEach { body.append("<td>${com.example.ui.components.escapeHtml(it?.toString() ?: "")}</td>") }
                            body.append("</tr>")
                        }
                        com.example.ui.components.printHtml(context, title, body.append("</table><style>@page{size:A4 landscape}</style>").toString())
                    },
                    enabled = shown.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("تصدير PDF") }
                Button(
                    onClick = {
                        try {
                            val (title, subtitle, rows) = pdfParts()
                            val pdf = com.example.data.PdfTable.write(title, subtitle, SHEET_PDF_HEADER, rows, listOf(4, 16, 16, 18, 7, 7, 7, 7, 7, 7))
                            com.example.ui.components.shareFile(context, pdf, "Bukhari-sheet-${Dates.today()}.pdf", "application/pdf")
                        } catch (e: Exception) {
                            importError = "تعذر إنشاء ملف PDF: ${e.message}"
                        }
                    },
                    enabled = shown.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("مشاركة PDF") }
            }
            if (books.isNotEmpty()) ChipFlow(Modifier, 6.dp) {
                FilterChip(selected = book == "all", onClick = { book = "all" }, label = { Text("كل الكتب") })
                books.forEach { b -> FilterChip(selected = book == b, onClick = { book = b }, label = { Text(b) }) }
            }
            if (places.isNotEmpty()) ChipFlow(Modifier, 6.dp) {
                FilterChip(selected = place == "all", onClick = { place = "all" }, label = { Text("كل الأماكن") })
                places.forEach { p -> FilterChip(selected = place == p, onClick = { place = p }, label = { Text(p) }) }
            }
            Text("عرض ${shown.size} من ${rows.size} صف", style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted))
        }
        if (rows.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("الكشف فارغ", fontWeight = FontWeight.Bold)
                Text("أضف صفاً أو استورد ملف Excel لبدء المتابعة.", color = tFg(TextMuted), style = MaterialTheme.typography.bodySmall)
            }
        }
        items(shown, key = { it.row.id }) { sr ->
            val r = sr.row
            val committee = r.committeeId?.let { id -> committees.find { it.id == id } }
            val canToggle = if (committee != null) Permissions.can("committees.update") || Permissions.canOn("committees.reception", committee.id) ||
                Permissions.canOn("committees.scoring", committee.id) || Permissions.canOn("committees.finalize", committee.id) else canEdit
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (r.struck_out) tBg(Color(0xFFF1F5F9)) else tBg(Color.White)),
                border = BorderStroke(1.dp, if (r.uncertain) tEdge(Color(0xFFFCA5A5)) else tEdge(Color(0xFFE2E8F0)))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${r.number}", color = Color.White, fontWeight = FontWeight.Black,
                            modifier = Modifier.background(if (sr.virtual) tBg(TextMuted) else tBg(Primary700), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                r.sheikh_name.ifBlank { "—" }, fontWeight = FontWeight.Bold,
                                textDecoration = if (r.struck_out) TextDecoration.LineThrough else null
                            )
                            Text(
                                listOf(r.place, r.book, "${r.students_count} طالب", r.courseName ?: "").filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted)
                            )
                        }
                        // A committee's data (sheikh, place, book, students) can be edited from its row too.
                        if ((canEdit && !sr.virtual) || (committee != null && Permissions.can("committees.update"))) IconButton(onClick = { editing = sr }) { Icon(Icons.Default.Edit, "تعديل الصف", tint = tFg(Primary700)) }
                        if (canEdit && !sr.virtual) IconButton(onClick = { deleting = r }) { Icon(Icons.Default.Delete, "حذف الصف", tint = tFg(Color(0xFFEF4444))) }
                    }
                    ChipFlow(Modifier, 4.dp) {
                        FLAGS.forEach { (key, label) ->
                            FilterChip(
                                selected = r.flag(key), enabled = canToggle,
                                onClick = {
                                    val newValue = !r.flag(key)
                                    if (committee != null) {
                                        val stage = ROW_STAGE.getValue(key)
                                        val stageDone = committee.stages.getStage(stage).completed
                                        if (!newValue && !stageDone) {
                                            importError = "هذه الخانة مكتملة لأن مرحلة لاحقة في اللجنة مكتملة؛ ألغِ المرحلة اللاحقة أولاً من تفاصيل اللجنة"
                                        } else onToggleStage(committee.id, stage, !newValue)
                                    } else onSaveRow(r.withFlag(key, newValue)) {}
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (r.uncertain) Text("⚠️ قراءة غير مؤكدة تحتاج تدقيق", style = MaterialTheme.typography.labelSmall, color = tFg(Color(0xFFB91C1C)))
                    if (r.notes.isNotBlank()) Text("💬 ${r.notes}", style = MaterialTheme.typography.labelSmall)
                    Row {
                        if (committee != null) TextButton(onClick = { onOpenCommitteeDetail(committee.id) }) { Text("تفاصيل اللجنة ↗") }
                        else if (canCreateCommittees) TextButton(onClick = {
                            val courseId = r.courseId ?: defaultCourse?.id
                            if (courseId == null) importError = "حدد الدورة التي يتبعها هذا الصف قبل فتح تفاصيل اللجنة"
                            else onCreateCommitteeFromRow(committeeFromRow(r, courseId, courses.find { it.id == courseId }?.name ?: ""))
                        }) { Text("إنشاء لجنة من هذا الصف") }
                    }
                }
            }
        }
        item { Spacer(Modifier.padding(24.dp)) }
    }

    editing?.let { sr ->
        RowEditDialog(sr.row, courses, canEditCommitteeData = sr.row.committeeId == null || Permissions.can("committees.update"), onDismiss = { editing = null }) { updated ->
            val committee = updated.committeeId?.let { id -> committees.find { it.id == id } }
            if (committee != null && Permissions.can("committees.update")) {
                // Linked row: the edit goes to the committee, so it shows everywhere (web, details, ceremonies).
                val changed = committee.copy(
                    sheikhName = updated.sheikh_name, sheikhCountry = updated.place,
                    competitionBook = updated.book, studentsCount = updated.students_count
                )
                if (changed != committee) onSaveCommittee(changed)
                val old = sr.row
                val rowOnlyChanged = updated.notes != old.notes || updated.struck_out != old.struck_out || updated.uncertain != old.uncertain
                if (rowOnlyChanged) {
                    onSaveRow(updated) { ok -> if (ok) editing = null }
                } else {
                    editing = null
                }
            } else {
                onSaveRow(updated) { ok -> if (ok) editing = null }
            }
        }
    }
    deleting?.let { r ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف الصف") },
            text = { Text("هل أنت متأكد من حذف الصف رقم ${r.number}؟") },
            confirmButton = { TextButton(onClick = { deleting = null; onDeleteRow(r) }) { Text("حذف", color = tFg(Color(0xFFDC2626))) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("إلغاء") } }
        )
    }
    if (addingCourse) {
        CourseFormDialog(course = null, onDismiss = { addingCourse = false }) { c -> onAddCourse(c) { ok -> if (ok) { addingCourse = false; onSelectCourse(c.id) } } }
    }
    if (addingRow) {
        AddRowDialog(
            courses = courses, initialCourseId = importCourseId,
            nextNumber = { courseId -> (committees.filter { it.courseId == courseId }.maxOfOrNull { it.committeeNumber } ?: 0) + 1 },
            onDismiss = { addingRow = false },
            onSave = { c -> onCreateCommitteeFromRow(c); addingRow = false }
        )
    }
    pendingImport?.let { incoming ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("الكشف يحتوي على بيانات") },
            text = { Text("تم العثور على ${incoming.size} صفاً. هل تريد دمجها مع الصفوف الحالية، أم استبدال كشف الدورة بالكامل؟") },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        val existing = rows.map { it.row.number }.toMutableSet()
                        var next = (rows.maxOfOrNull { it.row.number } ?: 0) + 1
                        val formatted = incoming.map { r ->
                            var n = r.number
                            if (n == 0 || n in existing) n = next++
                            existing.add(n)
                            r.copy(id = "row-${java.util.UUID.randomUUID()}", number = n)
                        }
                        importCourseId?.let { onImportRows(formatted, it, false) {} }
                        pendingImport = null
                    }) { Text("دمج مع الكشف الحالي") }
                    TextButton(onClick = {
                        var next = 1
                        val formatted = incoming.map { r -> r.copy(id = "row-${java.util.UUID.randomUUID()}", number = if (r.number > 0) r.number else next++) }
                        importCourseId?.let { onImportRows(formatted, it, true) {} }
                        pendingImport = null
                    }) { Text("استبدال كشف الدورة بالكامل", color = tFg(Color(0xFFB91C1C))) }
                }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("إلغاء") } }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("تفريغ الكشف") },
            text = { Text("سيتم حذف صفوف الكشف المحفوظة لهذه الدورة (اللجان نفسها لن تُحذف). هل أنت متأكد؟") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    importCourseId?.let { onImportRows(emptyList(), it, true) {} } ?: run { importError = "اختر الدورة التي تريد تفريغ كشفها أولاً" }
                }) { Text("تفريغ", color = tFg(Color(0xFFDC2626))) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("إلغاء") } }
        )
    }
    importError?.let { msg ->
        AlertDialog(onDismissRequest = { importError = null }, text = { Text(msg) }, confirmButton = { TextButton(onClick = { importError = null }) { Text("حسناً") } })
    }
}

/** A new committee built from a sheet row (web: handleOpenRowDetails / add row). */
private fun committeeFromRow(r: SheetRowDto, courseId: String, courseName: String): Committee {
    val now = Dates.today()
    return Committee(
        id = r.committeeId ?: "comm-${java.util.UUID.randomUUID()}",
        committeeNumber = r.number,
        committeeName = if (r.sheikh_name.isNotBlank()) "لجنة ${r.sheikh_name}" else "لجنة #${r.number}",
        sheikhName = r.sheikh_name, studentsCount = r.students_count, branch = r.place, competitionBook = r.book,
        courseId = courseId, courseName = courseName, generalNotes = r.notes, stages = StagesMap(), createdDate = now
    )
}

@Composable
private fun RowEditDialog(row: SheetRowDto, courses: List<Course>, canEditCommitteeData: Boolean = true, onDismiss: () -> Unit, onSave: (SheetRowDto) -> Unit) {
    var number by remember { mutableStateOf(row.number.toString()) }
    var sheikh by remember { mutableStateOf(row.sheikh_name) }
    var place by remember { mutableStateOf(row.place) }
    var book by remember { mutableStateOf(row.book) }
    var students by remember { mutableStateOf(row.students_count.toString()) }
    var notes by remember { mutableStateOf(row.notes) }
    var struck by remember { mutableStateOf(row.struck_out) }
    var uncertain by remember { mutableStateOf(row.uncertain) }
    var courseId by remember { mutableStateOf(row.courseId) }
    val linked = row.committeeId != null
    // A linked row shows its committee's sheikh, place, book and students (one place for them);
    // without the right to edit committees they are read-only here.
    val dataEditable = canEditCommitteeData
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل الصف رقم ${row.number}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!linked) OutlinedTextField(number, { number = it.filter(Char::isDigit) }, label = { Text("م") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(sheikh, { sheikh = it }, label = { Text("اسم الشيخ") }, singleLine = true, enabled = dataEditable)
                OutlinedTextField(place, { place = it }, label = { Text("المكان (مكان الشيخ)") }, singleLine = true, enabled = dataEditable)
                OutlinedTextField(book, { book = it }, label = { Text("الكتاب") }, singleLine = true, enabled = dataEditable)
                OutlinedTextField(students, { students = it.filter(Char::isDigit) }, label = { Text("عدد الطلاب") }, singleLine = true, enabled = dataEditable, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                if (linked && !dataEditable) Text(
                    "الشيخ والمكان والكتاب وعدد الطلاب بيانات اللجنة نفسها، وتعديلها من تفاصيل اللجنة لمن له صلاحية تعديل اللجان.",
                    style = MaterialTheme.typography.labelSmall, color = tFg(Color(0xFFB45309))
                )
                OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات") })
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(struck, { struck = it }); Text("مشطوب (ملغي)") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(uncertain, { uncertain = it }); Text("قراءة غير مؤكدة تحتاج تدقيق") }
                if (!linked) {
                    Text("الدورة", fontWeight = FontWeight.Bold)
                    ChipFlow(Modifier, 6.dp) {
                        courses.forEach { c -> FilterChip(selected = courseId == c.id, onClick = { courseId = c.id }, label = { Text(c.name) }) }
                    }
                } else Text("الصف مربوط بلجنة؛ رقمها ودورتها ومراحلها تُعدّل من تفاصيل اللجنة.", style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted))
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    row.copy(
                        number = if (linked) row.number else number.toIntOrNull() ?: row.number,
                        sheikh_name = sheikh.trim(), place = place.trim(), book = book.trim(),
                        students_count = students.toIntOrNull() ?: 0, notes = notes.trim(), struck_out = struck, uncertain = uncertain,
                        courseId = courseId, courseName = courses.find { it.id == courseId }?.name ?: row.courseName
                    )
                )
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun AddRowDialog(courses: List<Course>, initialCourseId: String?, nextNumber: (String) -> Int, onDismiss: () -> Unit, onSave: (Committee) -> Unit) {
    var courseId by remember { mutableStateOf(initialCourseId ?: courses.firstOrNull()?.id ?: "") }
    var number by remember(courseId) { mutableStateOf(if (courseId.isNotBlank()) nextNumber(courseId).toString() else "1") }
    var sheikh by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var book by remember { mutableStateOf("") }
    var students by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    var flags by remember { mutableStateOf(setOf<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة صف ولجنة جديدة إلى الكشف") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("الدورة *", fontWeight = FontWeight.Bold)
                ChipFlow(Modifier, 6.dp) {
                    courses.forEach { c -> FilterChip(selected = courseId == c.id, onClick = { courseId = c.id }, label = { Text(c.name) }) }
                }
                OutlinedTextField(number, { number = it.filter(Char::isDigit) }, label = { Text("رقم اللجنة") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(sheikh, { sheikh = it }, label = { Text("اسم الشيخ") }, singleLine = true)
                OutlinedTextField(place, { place = it }, label = { Text("المكان / الفرع") }, singleLine = true)
                OutlinedTextField(book, { book = it }, label = { Text("الكتاب") }, singleLine = true)
                OutlinedTextField(students, { students = it.filter(Char::isDigit) }, label = { Text("عدد الطلاب") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                ChipFlow(Modifier, 4.dp) {
                    FLAGS.forEach { (k, l) -> FilterChip(selected = k in flags, onClick = { flags = if (k in flags) flags - k else flags + k }, label = { Text(l) }) }
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات") })
            }
        },
        confirmButton = {
            Button(enabled = courseId.isNotBlank(), onClick = {
                val n = number.toIntOrNull() ?: nextNumber(courseId)
                val now = Dates.today()
                fun st(k: String) = com.example.data.model.StageItem(completed = k in flags)
                onSave(
                    Committee(
                        id = "comm-${java.util.UUID.randomUUID()}",
                        committeeNumber = n,
                        committeeName = if (sheikh.isNotBlank()) "لجنة ${sheikh.trim()}" else "لجنة #$n",
                        sheikhName = sheikh.trim(), branch = place.trim(), competitionBook = book.trim(),
                        studentsCount = students.toIntOrNull() ?: 0, courseId = courseId,
                        courseName = courses.find { it.id == courseId }?.name ?: "", generalNotes = notes.trim(),
                        stages = StagesMap(
                            registration = com.example.data.model.StageItem(completed = true),
                            exam = st("delivered"), scoring = st("corrected"), review = st("reviewed"),
                            announcement = st("announced"), ceremony = st("ceremony")
                        ),
                        createdDate = now
                    )
                )
            }, colors = ButtonDefaults.buttonColors(containerColor = tBg(Primary700), contentColor = Color.White)) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

private val STATUS_LABELS = mapOf(
    "all" to "الكل", "not_delivered" to "في انتظار الامتحان", "delivered" to "تم الامتحان", "corrected" to "تم التصحيح",
    "reviewed" to "تمت المراجعة", "announced" to "تم الإعلان", "no_ceremony" to "لم يتم الحفل", "ceremony" to "تم الحفل",
    "uncertain" to "يحتاج تدقيق", "struck_out" to "مشطوب"
)
private val SHEET_PDF_HEADER = listOf("م", "اسم الشيخ", "المكان", "الكتاب المقرر", "عدد الطلاب", "تم الامتحان", "تم التصحيح", "تمت المراجعة", "تم الإعلان", "تم الحفل")

