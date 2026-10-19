package com.example.ui.screens.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SheetRules
import com.example.data.SimpleXlsx
import com.example.data.model.Committee
import com.example.data.model.Course
import com.example.data.remote.SheetRowDto
import com.example.ui.components.Dates
import com.example.ui.components.XLSX_MIME
import com.example.ui.components.currentStageKey
import com.example.ui.components.escapeHtml
import com.example.ui.components.printHtml
import com.example.ui.components.shareFile
import com.example.ui.theme.StageColorMap
import com.example.ui.theme.TextMuted

private val STAGE_KEYS = listOf("registration", "exam", "scoring", "aggregation", "review", "sheet", "accreditation", "announcement", "ceremony")

/** The web's ExportModal plus the sheet export: a full committees report as CSV or print, and the course sheet as Excel/CSV. */
@Composable
fun ExportDialog(
    committees: List<Committee>,
    courses: List<Course>,
    sheetRows: List<SheetRowDto>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val header = listOf(
        "رقم اللجنة", "اسم اللجنة", "التصنيف النسائي", "الشيخ المرسل إليه", "هاتف الشيخ", "بلد الشيخ", "عدد الطلاب",
        "الكتاب المقرر / مسار المسابقة", "الفرع", "اسم الجمعية", "أعضاء اللجنة", "الحالة الحالية",
        "تاريخ ووقت التسجيل", "تاريخ ووقت الامتحان", "تاريخ ووقت التصحيح", "تاريخ ووقت التجميع", "تاريخ ووقت المراجعة والتدقيق",
        "تاريخ ووقت كتابة الكشف", "تاريخ ووقت الاعتمادية", "تاريخ ووقت إعلان النتيجة", "تاريخ ووقت الحفل", "الملاحظات العامة"
    )
    fun rows() = committees.sortedBy { it.committeeNumber }.map { c ->
        listOf<Any?>(
            "#${c.committeeNumber}", c.committeeName, if (c.isWomenCommittee) "لجنة نسائية" else "-",
            c.sheikhName.ifBlank { "-" }, c.sheikhPhone.ifBlank { "-" }, c.sheikhCountry.ifBlank { "-" }, c.studentsCount,
            c.competitionBook, c.branch, c.associationName.ifBlank { "-" }, c.members.joinToString(" - "),
            StageColorMap[currentStageKey(c.stages)]?.activeLabel ?: currentStageKey(c.stages)
        ) + STAGE_KEYS.map { c.stages.getStage(it).completedAt ?: "-" } + listOf(c.generalNotes.ifBlank { "-" })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تصدير كشوفات لجان مسابقات مسجد البخاري", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "تصدير كشف شامل لكافة لجان المسابقة (${committees.size} لجنة) بجميع مراحلها وملاحظاتها لطباعتها أو فتحها في Excel و Google Sheets.",
                    style = MaterialTheme.typography.bodySmall, color = TextMuted
                )
                Button(onClick = {
                    shareFile(context, SimpleXlsx.csv(header, rows()), "كشوفات_مسابقات_مسجد_البخاري_${Dates.today()}.csv", "text/csv")
                }, modifier = Modifier.fillMaxWidth()) { Text("كشف اللجان CSV (يدعم العربية)") }
                OutlinedButton(onClick = {
                    shareFile(context, SimpleXlsx.write("كشف اللجان", header, rows()), "كشوفات_مسابقات_مسجد_البخاري_${Dates.today()}.xlsx", XLSX_MIME)
                }, modifier = Modifier.fillMaxWidth()) { Text("كشف اللجان Excel") }
                OutlinedButton(onClick = {
                    val body = StringBuilder("<h2 class='center'>كشف لجان مسابقات مسجد الإمام البخاري</h2><p class='center'>${Dates.today()} — ${committees.size} لجنة</p><table><tr>")
                    listOf("رقم", "اللجنة", "الشيخ", "الكتاب", "الطلاب", "الحالة").forEach { body.append("<th>$it</th>") }
                    body.append("</tr>")
                    committees.sortedBy { it.committeeNumber }.forEach { c ->
                        body.append("<tr><td>#${c.committeeNumber}</td><td>${escapeHtml(c.committeeName)}${if (c.isWomenCommittee) " 🧕" else ""}</td><td>${escapeHtml(c.sheikhName.ifBlank { "-" })}</td><td>${escapeHtml(c.competitionBook)}</td><td>${c.studentsCount}</td><td>${escapeHtml(StageColorMap[currentStageKey(c.stages)]?.activeLabel ?: "")}</td></tr>")
                    }
                    body.append("</table><p style='margin-top:40px'>توقيع إدارة المسابقة: ____________________</p>")
                    printHtml(context, "كشف اللجان", body.toString())
                }, modifier = Modifier.fillMaxWidth()) { Text("طباعة / PDF (تقرير ورقي للاعتماد)") }
                if (sheetRows.isNotEmpty() || committees.isNotEmpty()) {
                    val merged = SheetRules.merge(sheetRows, committees).map { it.row }
                    Text("كشف متابعة الدورة (${merged.size} صف)", fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = {
                        shareFile(context, SimpleXlsx.write("كشف الدورة العلمية", SheetRules.EXPORT_HEADER, SheetRules.exportRows(merged, true), SheetRules.EXPORT_WIDTHS),
                            "متابعة_كشف_الدورة_${Dates.today()}.xlsx", XLSX_MIME)
                    }, modifier = Modifier.fillMaxWidth()) { Text("كشف الدورة Excel") }
                    OutlinedButton(onClick = {
                        shareFile(context, SimpleXlsx.csv(SheetRules.EXPORT_HEADER, SheetRules.exportRows(merged, false)), "متابعة_كشف_الدورة_${Dates.today()}.csv", "text/csv")
                    }, modifier = Modifier.fillMaxWidth()) { Text("كشف الدورة CSV") }
                }
                if (courses.isEmpty()) Text("لا توجد دورات بعد", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}
