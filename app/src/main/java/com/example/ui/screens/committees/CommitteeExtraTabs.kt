package com.example.ui.screens.committees

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.SimpleXlsx
import com.example.data.model.Committee
import com.example.data.model.Course
import com.example.ui.components.Dates
import com.example.ui.components.XLSX_MIME
import com.example.ui.components.shareFile
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.TextMuted

/** Base data of a committee (web: the editable fields of CommitteeDetailModal), plus delete. */
@Composable
fun CommitteeBaseDataTab(
    committee: Committee,
    courses: List<Course>,
    canEditBase: Boolean,
    canDelete: Boolean,
    onSave: (Committee) -> Unit,
    onDelete: () -> Unit
) {
    var number by remember { mutableStateOf(committee.committeeNumber.toString()) }
    var name by remember { mutableStateOf(committee.committeeName) }
    var association by remember { mutableStateOf(committee.associationName) }
    var branch by remember { mutableStateOf(committee.branch) }
    var book by remember { mutableStateOf(committee.competitionBook) }
    var sheikhName by remember { mutableStateOf(committee.sheikhName) }
    var sheikhPhone by remember { mutableStateOf(committee.sheikhPhone) }
    var sheikhCountry by remember { mutableStateOf(committee.sheikhCountry) }
    var students by remember { mutableStateOf(committee.studentsCount.toString()) }
    var isWomen by remember { mutableStateOf(committee.isWomenCommittee) }
    var courseId by remember { mutableStateOf(committee.courseId.ifBlank { courses.find { it.isDefault }?.id ?: "" }) }
    var members by remember { mutableStateOf(committee.members) }
    var newMember by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(committee.generalNotes) }
    var confirmDelete by remember { mutableStateOf(false) }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("بيانات اللجنة (كما أُدخلت عند الإضافة)", fontWeight = FontWeight.Bold, color = Primary800)
            OutlinedTextField(
                number, { number = it.filter(Char::isDigit) }, label = { Text("رقم اللجنة") }, enabled = canEditBase, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(name, { name = it }, label = { Text("اسم اللجنة") }, enabled = canEditBase, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(book, { book = it }, label = { Text("الكتاب المقرر / المسابقة") }, enabled = canEditBase, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(branch, { branch = it }, label = { Text("الفرع") }, enabled = canEditBase, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(association, { association = it }, label = { Text("الجمعية (اختياري)") }, enabled = canEditBase, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(sheikhName, { sheikhName = it }, label = { Text("الشيخ المرسل إليه") }, enabled = canEditBase, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                sheikhPhone, { sheikhPhone = it }, label = { Text("هاتف الشيخ") }, enabled = canEditBase, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(sheikhCountry, { sheikhCountry = it }, label = { Text("مكان الشيخ (المسجد / القاعة)") }, enabled = canEditBase, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                students, { students = it.filter(Char::isDigit) }, label = { Text("عدد الطلاب") }, enabled = canEditBase, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("لجنة نسائية 🧕", modifier = Modifier.weight(1f))
                Switch(isWomen, { isWomen = it }, enabled = canEditBase)
            }
            Text("الدورة", fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                courses.forEach { c -> FilterChip(selected = courseId == c.id, onClick = { if (canEditBase) courseId = c.id }, enabled = canEditBase, label = { Text(c.name) }) }
            }
            Text("أعضاء اللجنة", fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                members.forEachIndexed { i, m ->
                    InputChip(selected = false, enabled = canEditBase, onClick = { members = members.filterIndexed { j, _ -> j != i } }, label = { Text(if (canEditBase) "$m ✕" else m) })
                }
            }
            if (canEditBase) Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(newMember, { newMember = it }, label = { Text("إضافة عضو") }, singleLine = true, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val n = newMember.trim()
                    if (n.isNotEmpty() && n !in members) members = members + n
                    newMember = ""
                }) { Text("إضافة") }
            }
            OutlinedTextField(notes, { notes = it }, label = { Text("الملاحظات العامة") }, enabled = canEditBase, minLines = 3, modifier = Modifier.fillMaxWidth())
            if (canEditBase) {
                Button(
                    onClick = {
                        onSave(
                            committee.copy(
                                committeeNumber = number.toIntOrNull() ?: committee.committeeNumber,
                                committeeName = name.trim().ifBlank { committee.committeeName },
                                competitionBook = book.trim(), branch = branch.trim(), associationName = association.trim(),
                                sheikhName = sheikhName.trim(), sheikhPhone = sheikhPhone.trim(), sheikhCountry = sheikhCountry.trim(),
                                studentsCount = students.toIntOrNull() ?: 0, isWomenCommittee = isWomen,
                                courseId = courseId, courseName = courses.find { it.id == courseId }?.name ?: committee.courseName,
                                members = members.map { it.trim() }.filter { it.isNotEmpty() }, generalNotes = notes
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Primary700)
                ) { Text("حفظ بيانات اللجنة") }
            } else Text("تعديل البيانات الأساسية غير متاح لدورك.", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            if (canDelete) OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("حذف اللجنة", color = Color(0xFFB91C1C)) }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف اللجنة رقم ${committee.committeeNumber}؟") },
            text = { Text("سيتم حذف لجنة ${committee.committeeName} ومراحلها ودرجاتها نهائياً، ولا يمكن التراجع.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("حذف", color = Color(0xFFB91C1C)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("إلغاء") } }
        )
    }
}

/** Per-question scorers, score and notes (web: scorer chips under each question). */
@Composable
fun QuestionScorersSection(
    committee: Committee,
    canScore: Boolean,
    onUpdate: (questionId: String, completed: Boolean, scorers: List<String>, score: Double?, notes: String?) -> Unit
) {
    val questions = committee.scoringDetails.questions.sortedBy { it.questionNumber }
    if (questions.isEmpty()) return
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFFDE68A))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("المصححون والدرجة لكل سؤال", fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
            questions.forEach { q ->
                var newScorer by remember(q.id) { mutableStateOf("") }
                var score by remember(q.id, q.score) { mutableStateOf(q.score?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
                var qNotes by remember(q.id, q.notes) { mutableStateOf(q.notes ?: "") }
                Column(Modifier.fillMaxWidth().background(Color(0xFFFFFBEB), RoundedCornerShape(10.dp)).padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(q.completed, { if (canScore) onUpdate(q.id, it, q.scorers, q.score, q.notes) }, enabled = canScore)
                        Text(q.label.ifBlank { "السؤال ${q.questionNumber}" }, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        q.scorers.forEach { s ->
                            InputChip(selected = false, enabled = canScore, onClick = { onUpdate(q.id, q.completed, q.scorers - s, q.score, q.notes) }, label = { Text(if (canScore) "$s ✕" else s) })
                        }
                        if (q.scorers.isEmpty()) Text("لا يوجد مصححون بعد", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    if (canScore) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(newScorer, { newScorer = it }, label = { Text("إضافة مصحح") }, singleLine = true, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                val n = newScorer.trim()
                                if (n.isNotEmpty() && n !in q.scorers) onUpdate(q.id, q.completed, q.scorers + n, q.score, q.notes)
                                newScorer = ""
                            }) { Text("إضافة") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                score, { score = it }, label = { Text("الدرجة") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.width(110.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            OutlinedTextField(qNotes, { qNotes = it }, label = { Text("ملاحظات") }, singleLine = true, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onUpdate(q.id, q.completed, q.scorers, score.toDoubleOrNull(), qNotes.ifBlank { null }) }) { Text("حفظ") }
                        }
                    }
                }
            }
        }
    }
}

/** A student line of the committee's grade sheet. */
data class GradeLine(val number: Int, val name: String, val score: String, val grade: String, val notes: String)

/** Stage 6 «كتابة الكشف»: import the grade sheet from Excel, export it, the template, and the Excel link. */
@Composable
fun CommitteeSheetTab(
    committee: Committee,
    canFinalize: Boolean,
    canEditBase: Boolean,
    onSheetImported: (notes: String, studentsCount: Int?) -> Unit,
    onSaveExcelUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var lines by remember { mutableStateOf<List<GradeLine>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var excelUrl by remember { mutableStateOf(committee.stages.sheet.excelUrl ?: "") }
    val sheet = committee.stages.sheet

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
            var name = "كشف.xlsx"
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0)?.let { name = it } }
            val isZip = bytes.size > 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
            val table = if (isZip) SimpleXlsx.read(bytes) else SimpleXlsx.parseCsv(bytes)
            if (table.size < 2) { error = "الملف المحدد لا يحتوي على صفوف أو بيانات مقروءة."; return@rememberLauncherForActivityResult }
            val header = table.first().map { it.trim() }
            fun col(names: List<String>) = names.firstNotNullOfOrNull { n -> header.indexOf(n).takeIf { it >= 0 } }
            val cNum = col(listOf("م", "الرقم", "رقم", "id", "number"))
            val cName = col(listOf("اسم الطالب", "الطالب", "الاسم", "student_name", "name", "اسم الشيخ", "الشيخ"))
            val cScore = col(listOf("الدرجة", "المجموع", "الدرجة النهائية", "النسبة", "score", "total", "mark"))
            val cGrade = col(listOf("التقدير", "الحالة", "النتيجة", "grade", "status", "result"))
            val cNotes = col(listOf("ملاحظات", "ملاحظة", "notes"))
            val parsed = table.drop(1).mapIndexed { i, r ->
                fun v(c: Int?) = c?.let { r.getOrNull(it)?.trim() } ?: ""
                GradeLine(v(cNum).toIntOrNull()?.takeIf { it > 0 } ?: (i + 1), v(cName).ifBlank { "طالب ${i + 1}" }, v(cScore).ifBlank { "—" }, v(cGrade), v(cNotes))
            }
            lines = parsed
            onSheetImported("كشف إكسيل: $name (${parsed.size} طالب)", if (committee.studentsCount == 0) parsed.size else null)
        } catch (e: Exception) {
            error = "تعذر قراءة ملف الإكسيل، يرجى التأكد من اختيار ملف .xlsx أو .csv صالح."
        }
    }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE9D5FF))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🟣 كتابة الكشف وكشف الدرجات", fontWeight = FontWeight.Bold, color = Color(0xFF6B21A8))
            Text(
                if (sheet.completed) "✓ مكتملة${sheet.completedAt?.let { " — ${Dates.displayTime(it)}" } ?: ""}${sheet.completedBy?.let { " — $it" } ?: ""}" else "لم تكتمل مرحلة كتابة الكشف بعد",
                style = MaterialTheme.typography.bodySmall, color = if (sheet.completed) Color(0xFF047857) else TextMuted
            )
            sheet.notes?.takeIf { it.isNotBlank() }?.let { Text("📎 $it", style = MaterialTheme.typography.bodySmall) }
            if (canFinalize) Button(onClick = { pick.launch("*/*") }, modifier = Modifier.fillMaxWidth()) { Text("استيراد كشف الدرجات من Excel") }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = {
                    val header = listOf("م", "اسم الطالب", "الدرجة", "التقدير", "ملاحظات")
                    val rows = listOf(listOf<Any?>(1, "أحمد محمد علي", 98, "ممتاز مرتفع", "حفظ متقن ومتميز"), listOf<Any?>(2, "عبد الرحمن خالد", 92, "ممتاز", "أداء متقن"), listOf<Any?>(3, "عمر يوسف إبراهيم", 85, "جيد جداً", ""))
                    shareFile(context, SimpleXlsx.write("نموذج كشف درجات", header, rows, listOf(6, 25, 12, 16, 25)), "نموذج_كشف_درجات_لجنة_${committee.committeeNumber}.xlsx", XLSX_MIME)
                }, modifier = Modifier.weight(1f)) { Text("تنزيل النموذج") }
                OutlinedButton(onClick = {
                    val header = listOf("رقم اللجنة", "اسم اللجنة", "الدورة", "الكتاب المقرر", "الفرع", "الشيخ المرسل إليه", "م", "اسم الطالب", "الدرجة", "التقدير", "ملاحظات")
                    val data = lines.ifEmpty { (1..maxOf(1, committee.studentsCount)).map { GradeLine(it, "طالب $it", "—", "—", "") } }
                    val rows = data.map { s ->
                        listOf<Any?>("#${committee.committeeNumber}", committee.committeeName, committee.courseName.ifBlank { "—" }, committee.competitionBook, committee.branch, committee.sheikhName.ifBlank { "—" }, s.number, s.name, s.score, s.grade, s.notes)
                    }
                    shareFile(context, SimpleXlsx.write("كشف درجات اللجنة", header, rows, listOf(12, 22, 18, 24, 16, 20, 6, 25, 12, 14, 24)), "كشف_درجات_لجنة_${committee.committeeNumber}_${Dates.today()}.xlsx", XLSX_MIME)
                }, modifier = Modifier.weight(1f)) { Text("تصدير Excel") }
            }
            if (lines.isNotEmpty()) {
                Text("الطلاب المستوردون (${lines.size})", fontWeight = FontWeight.Bold)
                lines.take(50).forEach { s -> Text("${s.number}. ${s.name} — ${s.score} ${s.grade}".trim(), style = MaterialTheme.typography.bodySmall) }
            }
            Text("رابط ملف الإكسيل / كشف الدرجات", fontWeight = FontWeight.Bold)
            OutlinedTextField(excelUrl, { excelUrl = it }, enabled = canEditBase, singleLine = true, placeholder = { Text("https://...") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (canEditBase) OutlinedButton(onClick = { onSaveExcelUrl(excelUrl.trim()) }) { Text("حفظ الرابط") }
                if (excelUrl.startsWith("http")) OutlinedButton(onClick = { runCatching { uriHandler.openUri(excelUrl.trim()) } }) { Text("فتح الرابط ↗") }
            }
        }
    }
    error?.let { msg -> AlertDialog(onDismissRequest = { error = null }, text = { Text(msg) }, confirmButton = { TextButton(onClick = { error = null }) { Text("حسناً") } }) }
}
