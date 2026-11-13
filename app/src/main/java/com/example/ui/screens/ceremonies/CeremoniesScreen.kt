package com.example.ui.screens.ceremonies

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
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.example.data.SimpleXlsx
import com.example.data.model.CeremonyDetails
import com.example.data.model.Committee
import com.example.data.model.ScheduleTask
import com.example.data.remote.Permissions
import com.example.ui.components.ARABIC_DAYS
import com.example.ui.components.DateField
import com.example.ui.components.Dates
import com.example.ui.components.ScreenBanner
import com.example.ui.components.XLSX_MIME
import com.example.ui.components.shareFile
import com.example.ui.screens.export.ReportSheets
import com.example.ui.components.StatTile
import com.example.ui.components.arabicDayName
import com.example.ui.components.escapeHtml
import com.example.ui.components.nextDateForDay
import com.example.ui.components.printHtml
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.TextMuted

/** The ceremony's calendar task, kept in step by the server (one per committee id). */
fun linkedCeremonyTask(c: Committee, tasks: List<ScheduleTask>): ScheduleTask? =
    tasks.find { it.id == "task-ceremony-${c.id}" } ?: tasks.find { it.type == "ceremony" && it.committeeId == c.id }

/** Date of the committee's ceremony: its calendar task, else the ceremony details. */
fun ceremonyDate(c: Committee, tasks: List<ScheduleTask>): String =
    linkedCeremonyTask(c, tasks)?.date?.takeIf { it.isNotBlank() } ?: c.ceremonyDetails.date

/** Web: CeremoniesView — schedule, complete and print honour rolls for every committee's ceremony. */
@Composable
fun CeremoniesScreen(
    committees: List<Committee>,
    tasks: List<ScheduleTask>,
    onScheduleCeremony: (Committee, CeremonyDetails) -> Unit,
    onToggleCeremonyDone: (Committee) -> Unit,
    onOpenCommittee: (String) -> Unit,
    onNavigateDailyAlerts: () -> Unit,
    onNavigateCalendar: () -> Unit,
    currentUserId: String? = null,
    ceremonyCandidates: List<com.example.data.remote.AssignableUserDto>? = null,
    onLoadCandidates: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }
    var book by rememberSaveable { mutableStateOf("all") }
    var scheduling by remember { mutableStateOf<Committee?>(null) }
    var honorRoll by remember { mutableStateOf<Committee?>(null) }
    val today = Dates.today()
    val books = committees.map { it.competitionBook }.filter { it.isNotBlank() }.distinct()

    val done = committees.count { it.stages.ceremony.completed }
    val upcoming = committees.count { !it.stages.ceremony.completed && ceremonyDate(it, tasks).isNotBlank() }
    val todayCount = committees.count { ceremonyDate(it, tasks) == today }

    val shown = committees.filter { c ->
        val isDone = c.stages.ceremony.completed
        val date = ceremonyDate(c, tasks)
        val scheduled = date.isNotBlank()
        val passes = when (filter) {
            "today" -> date == today
            "scheduled" -> scheduled && !isDone
            "completed" -> isDone
            "unscheduled" -> !scheduled && !isDone
            else -> true
        }
        val q = query.trim().lowercase()
        passes && (book == "all" || c.competitionBook == book) && (
            q.isEmpty() || c.committeeName.lowercase().contains(q) || c.committeeNumber.toString().contains(q) ||
                c.sheikhName.lowercase().contains(q) || c.competitionBook.lowercase().contains(q) || c.associationName.lowercase().contains(q))
    }

    LazyColumn(
        modifier.fillMaxSize().background(Color(0xFFF8F9FD)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenBanner(
                "إدارة وجدولة حفلات التكريم والختام",
                "مربوطة ببيانات اللجان: أي ميعاد حفل تحدده يظهر في التنبيهات اليومية وتقويم المواعيد.",
                Icons.Default.Celebration, listOf(Color(0xFF78350F), Primary800)
            ) {
                Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onNavigateDailyAlerts) { Text("التنبيهات اليومية ($todayCount حفل اليوم) ↗", color = Color.White) }
                    OutlinedButton(onClick = onNavigateCalendar) { Text("عرض التقويم العام ↗", color = Color.White) }
                }
            }
        }
        item { MyCeremoniesSection(committees, tasks, currentUserId, onOpenCommittee) }
        item { CeremoniesExportCard(committees, tasks) }
        item { CeremonyStatusReportCard(committees, tasks) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("إجمالي اللجان", "${committees.size}", Modifier.weight(1f))
                StatTile("حفلات اليوم", "$todayCount", Modifier.weight(1f), tint = Color(0xFFB45309))
            }
            Spacer(Modifier.padding(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("مجدولة قادمة", "$upcoming", Modifier.weight(1f), tint = Color(0xFF1D4ED8))
                StatTile("تم الحفل (${if (committees.isEmpty()) 0 else done * 100 / committees.size}%)", "$done", Modifier.weight(1f), tint = Color(0xFF047857))
            }
        }
        item {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("بحث برقم اللجنة، الشيخ، الكتاب، المسجد...") }, shape = RoundedCornerShape(14.dp)
            )
            Row(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "all" to "الكل (${committees.size})", "today" to "حفلات اليوم ($todayCount)", "scheduled" to "المجدولة ($upcoming)",
                    "completed" to "المكتملة ($done)", "unscheduled" to "بدون موعد"
                ).forEach { (k, l) -> FilterChip(selected = filter == k, onClick = { filter = k }, label = { Text(l) }) }
            }
            if (books.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = book == "all", onClick = { book = "all" }, label = { Text("كافة المتون والكتب") })
                books.forEach { b -> FilterChip(selected = book == b, onClick = { book = b }, label = { Text(b) }) }
            }
        }
        if (shown.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("لا توجد حفلات مطابقة للبحث أو التصفية الحالية", fontWeight = FontWeight.Bold)
                Text("اختر تصفية أخرى أو جدول موعد حفل لأي لجنة.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        items(shown, key = { it.id }) { c ->
            val isDone = c.stages.ceremony.completed
            val task = linkedCeremonyTask(c, tasks)
            val date = ceremonyDate(c, tasks)
            val isToday = date == today
            val canFinalize = Permissions.canOn("committees.finalize", c.id)
            Card(
                shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(
                    if (isDone || isToday) 2.dp else 1.dp,
                    when { isDone -> Color(0xFF6EE7B7); isToday -> Color(0xFFFBBF24); date.isNotBlank() -> Color(0xFFBFDBFE); else -> Color(0xFFE2E8F0) }
                )
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "#${c.committeeNumber}", color = Color.White, fontWeight = FontWeight.Black,
                            modifier = Modifier.background(Color(0xFF047857), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.branch.ifBlank { "—" }, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(c.committeeName, fontWeight = FontWeight.Black)
                        }
                    }
                    Text(
                        when {
                            isDone -> "✓ تم الحفل والتكريم"
                            isToday -> "حفل اليوم (${arabicDayName(date)})"
                            date.isNotBlank() -> "مجدول: يوم ${arabicDayName(date)}"
                            else -> "بانتظار تحديد اليوم والموعد"
                        },
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                        color = when { isDone -> Color(0xFF047857); isToday -> Color(0xFF92400E); date.isNotBlank() -> Color(0xFF1D4ED8); else -> TextMuted }
                    )
                    Text("الكتاب: ${c.competitionBook} • الشيخ: ${c.sheikhName.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall)
                    Text("عدد الطلاب المكرمين: ${c.ceremonyDetails.honoreesCount.takeIf { it > 0 } ?: c.studentsCount} طالب", style = MaterialTheme.typography.bodySmall)
                    if (c.ceremonyDetails.attendees.isNotEmpty()) {
                        Text("المدعوون: ${c.ceremonyDetails.attendees.joinToString("، ") { it.name }}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    if (date.isNotBlank()) {
                        Text(
                            "الموعد: يوم ${arabicDayName(date)} ($date) — ${(task?.time ?: c.ceremonyDetails.time).ifBlank { "لم يحدد الوقت" }}",
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E3A8A),
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)).padding(6.dp)
                        )
                        val place = c.ceremonyDetails.venue.ifBlank { task?.notes ?: "" }
                        if (place.isNotBlank()) Text("📍 $place", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (canFinalize) {
                            OutlinedButton(onClick = { scheduling = c }) { Text(if (date.isNotBlank()) "تعديل الميعاد واليوم" else "جدولة الميعاد واليوم") }
                            Button(
                                onClick = { onToggleCeremonyDone(c) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDone) Color(0xFFD1FAE5) else Primary700, contentColor = if (isDone) Color(0xFF065F46) else Color.White)
                            ) { Text(if (isDone) "إلغاء التمام" else "إتمام الحفل الآن") }
                        }
                        OutlinedButton(onClick = { honorRoll = c }) { Text("كشف التكريم والشهادات") }
                        TextButton(onClick = { onOpenCommittee(c.id) }) { Text("تفاصيل اللجنة ↗") }
                    }
                }
            }
        }
        item { Spacer(Modifier.padding(24.dp)) }
    }

    scheduling?.let { c ->
        val task = linkedCeremonyTask(c, tasks)
        var date by remember { mutableStateOf(ceremonyDate(c, tasks).ifBlank { Dates.today() }) }
        var time by remember { mutableStateOf((task?.time ?: c.ceremonyDetails.time).ifBlank { "08:00 م (بعد صلاة العشاء)" }) }
        var place by remember { mutableStateOf(c.ceremonyDetails.venue) }
        var honored by remember { mutableStateOf((c.ceremonyDetails.honoreesCount.takeIf { it > 0 } ?: c.studentsCount).toString()) }
        var notes by remember { mutableStateOf(c.ceremonyDetails.notes) }
        var attendees by remember { mutableStateOf(c.ceremonyDetails.attendees) }
        AlertDialog(
            onDismissRequest = { scheduling = null },
            title = { Text("جدولة حفل تكريم لجنة #${c.committeeNumber}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${c.committeeName} — ${c.competitionBook}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Text("اختر يوم الحفل *", fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ARABIC_DAYS.forEach { d -> FilterChip(selected = arabicDayName(date) == d, onClick = { date = nextDateForDay(d) }, label = { Text(d) }) }
                    }
                    Text("✓ اليوم المحدد حالياً: يوم ${arabicDayName(date)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF047857))
                    DateField(date, { date = it }, "تاريخ الحفل *")
                    OutlinedTextField(time, { time = it }, label = { Text("وقت وتوقيت الحفل *") }, placeholder = { Text("مثال: 08:00 م (بعد صلاة العشاء)") }, singleLine = true)
                    OutlinedTextField(place, { place = it }, label = { Text("مكان إقامة الحفل *") }, placeholder = { Text("مثال: القاعة الكبرى - مسجد الإمام البخاري") }, singleLine = true)
                    OutlinedTextField(
                        honored, { honored = it.filter(Char::isDigit) }, label = { Text("عدد الطلاب المكرمين والشهادات") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(c.sheikhName, {}, readOnly = true, label = { Text("الشيخ المكرم") })
                    AttendeePicker(ceremonyCandidates, onLoadCandidates, attendees) { attendees = it }
                    OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات وترتيبات الحفل (اختياري)") }, minLines = 2)
                    Text("يظهر الموعد تلقائياً في التنبيهات اليومية وتقويم المواعيد ليوم ($date).", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            },
            confirmButton = {
                Button(enabled = date.isNotBlank() && time.isNotBlank() && place.isNotBlank(), onClick = {
                    onScheduleCeremony(
                        c, c.ceremonyDetails.copy(date = date, time = time.trim(), venue = place.trim(), honoreesCount = honored.toIntOrNull() ?: 0, notes = notes.trim(), attendees = attendees)
                    )
                    scheduling = null
                }) { Text("حفظ وتأكيد الميعاد") }
            },
            dismissButton = { TextButton(onClick = { scheduling = null }) { Text("إلغاء") } }
        )
    }

    honorRoll?.let { c ->
        AlertDialog(
            onDismissRequest = { honorRoll = null },
            title = { Text("كشف شرف وتكريم لجنة #${c.committeeNumber}") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("بِسْمِ اللَّـهِ الرَّحْمَـٰنِ الرَّحِيمِ", fontWeight = FontWeight.Bold)
                    Text("مسجد الإمام البخاري — الدورة العلمية المكثفة", style = MaterialTheme.typography.labelSmall)
                    Text("شهادة تكريم وتقدير واجتياز", fontWeight = FontWeight.Black, color = Color(0xFF78350F))
                    Text(
                        "يُشهد بأن طلاب لجنة (#${c.committeeNumber} - ${c.committeeName}) بإشراف فضيلة الشيخ ${c.sheikhName.ifBlank { "—" }} قد أتموا بنجاح اختبار كتاب:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("📖 ${c.competitionBook}", fontWeight = FontWeight.Bold)
                    Text("عدد الطلاب المكرمين: ${c.studentsCount} طالب وطالبة", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    printHtml(
                        context, "شهادة تكريم لجنة ${c.committeeNumber}",
                        """<div class="center"><p>بِسْمِ اللَّـهِ الرَّحْمَـٰنِ الرَّحِيمِ</p><p>مسجد الإمام البخاري — الدورة العلمية المكثفة</p>
                        <h2>شهادة تكريم وتقدير واجتياز</h2>
                        <p>يُشهد بأن طلاب لجنة <b>(#${c.committeeNumber} - ${escapeHtml(c.committeeName)})</b> بإشراف فضيلة الشيخ <b>${escapeHtml(c.sheikhName.ifBlank { "—" })}</b> قد أتموا بنجاح اختبار كتاب:</p>
                        <h3>📖 ${escapeHtml(c.competitionBook)}</h3><p>عدد الطلاب المكرمين: ${c.studentsCount} طالب وطالبة</p></div>"""
                    )
                }) { Text("طباعة شهادات التكريم") }
            },
            dismissButton = { TextButton(onClick = { honorRoll = null }) { Text("إغلاق") } }
        )
    }
}

/** Web: the ceremonies sheet export — a daily, weekly or monthly Excel of every ceremony and its details. */
@Composable
private fun CeremoniesExportCard(committees: List<Committee>, tasks: List<ScheduleTask>) {
    val context = LocalContext.current
    var period by rememberSaveable { mutableStateOf("day") }
    var anchor by rememberSaveable { mutableStateOf(Dates.today()) }
    val rows = ReportSheets.ceremonyRows(committees, tasks, period, anchor)
    val (from, to) = ReportSheets.periodRange(period, anchor)
    Card(
        shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("تصدير ومشاركة كشف الحفلات (Excel / PDF)", fontWeight = FontWeight.Black)
            Text(
                "الموعد والمكان واسم الشيخ ومكانه وعدد الطلاب المكرمين والمشايخ والمدعوين ومراقبي اللجنة — " +
                    if (from == to) from else "من $from إلى $to",
                style = MaterialTheme.typography.bodySmall, color = TextMuted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ReportSheets.PERIOD_LABELS.forEach { (k, l) -> FilterChip(selected = period == k, onClick = { period = k }, label = { Text(l) }) }
            }
            DateField(
                anchor, { anchor = it },
                when (period) { "day" -> "اليوم"; "week" -> "أي يوم في الأسبوع"; else -> "أي يوم في الشهر" }
            )
            Button(
                onClick = {
                    shareFile(
                        context,
                        SimpleXlsx.write("الحفلات (${ReportSheets.PERIOD_LABELS[period]})", ReportSheets.CEREMONY_HEADER, rows, ReportSheets.CEREMONY_WIDTHS),
                        ReportSheets.ceremonyFileName(period, anchor), XLSX_MIME
                    )
                },
                enabled = rows.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (rows.isEmpty()) "لا توجد حفلات في هذه الفترة" else "Excel (${rows.size} حفل)") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                // «تصدير PDF»: the print screen, where the sheet is saved as a PDF file or printed.
                OutlinedButton(
                    onClick = { printHtml(context, "كشف الحفلات", ReportSheets.ceremonyPrintBody(rows, period, anchor)) },
                    enabled = rows.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("تصدير PDF") }
                // «مشاركة PDF»: a ready PDF file sent through the share sheet (WhatsApp, Telegram, e-mail...).
                Button(
                    onClick = {
                        try {
                            val pdf = com.example.data.PdfTable.write(
                                ReportSheets.ceremonyTitle(period), ReportSheets.ceremonySubtitle(period, anchor, rows.size),
                                ReportSheets.CEREMONY_PDF_HEADER, ReportSheets.ceremonyPdfRows(rows), ReportSheets.CEREMONY_PDF_WEIGHTS
                            )
                            shareFile(context, pdf, ReportSheets.ceremonyPdfFileName(period, anchor), "application/pdf")
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "تعذر إنشاء ملف PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = rows.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("مشاركة PDF") }
            }
        }
    }
}

/**
 * «لم يتم الحفل» / «تم الحفل» reports (web: the same section on the ceremonies screen), laid out like
 * the sheets: «تصدير PDF» opens the print screen (save as PDF), «مشاركة» sends a PDF file.
 */
@Composable
fun CeremonyStatusReportCard(committees: List<Committee>, tasks: List<ScheduleTask>) {
    val context = LocalContext.current
    val courseName = committees.firstOrNull()?.courseName
    Card(
        shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFFDE68A))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("كشف «لم يتم الحفل» و«تم الحفل» (PDF)", fontWeight = FontWeight.Black)
            Text(
                "بنفس بيانات الكشوفات: اسم الشيخ، المكان، الكتاب، عدد الطلاب، وموعد ومكان الحفل، مع الإجمالي.",
                style = MaterialTheme.typography.bodySmall, color = TextMuted
            )
            listOf("not_held", "held").forEach { status ->
                val rows = ReportSheets.statusRows(committees, tasks, status)
                val count = ReportSheets.statusCommittees(committees, status).size
                val label = ReportSheets.STATUS_LABEL.getValue(status)
                val today = Dates.today()
                val subtitle = ReportSheets.statusSubtitle(rows, courseName, today)
                Text("$label ($count لجنة)", fontWeight = FontWeight.Bold, color = if (status == "held") Color(0xFF047857) else Color(0xFFC2410C))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { printHtml(context, ReportSheets.statusTitle(status), ReportSheets.statusPrintBody(rows, status, subtitle)) },
                        enabled = count > 0,
                        modifier = Modifier.weight(1f)
                    ) { Text("تصدير PDF") }
                    Button(
                        onClick = {
                            try {
                                val pdf = com.example.data.PdfTable.write(
                                    ReportSheets.statusTitle(status), subtitle, ReportSheets.statusHeader(status), rows, ReportSheets.STATUS_WEIGHTS
                                )
                                shareFile(context, pdf, ReportSheets.statusFileName(status, today), "application/pdf")
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "تعذر إنشاء ملف PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = count > 0,
                        modifier = Modifier.weight(1f)
                    ) { Text("مشاركة") }
                }
            }
        }
    }
}

