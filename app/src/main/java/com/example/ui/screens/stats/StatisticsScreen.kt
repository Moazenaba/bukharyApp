package com.example.ui.screens.stats

import com.example.ui.theme.tEdge
import com.example.ui.theme.tBg
import com.example.ui.theme.tFg
import androidx.compose.runtime.remember
import com.example.ui.theme.TextMuted
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.ui.components.stageIcon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Committee
import com.example.ui.components.OrderedStageKeys
import com.example.ui.theme.Primary100
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.Primary900
import com.example.ui.theme.StageColorMap
import com.example.ui.theme.SurfacePure

@Composable
fun StatisticsScreen(
    committees: List<Committee>,
    modifier: Modifier = Modifier
) {
    val totalCommittees = committees.size
    val totalStudents = committees.sumOf { it.studentsCount }
    val maleCount = committees.count { !it.isWomenCommittee }
    val womenCount = committees.count { it.isWomenCommittee }

    val totalStagesPossible = totalCommittees * 9
    val completedStagesTotal = committees.sumOf { it.stages.getCompletedCount() }
    val overallPercentage = if (totalStagesPossible > 0) (completedStagesTotal.toFloat() / totalStagesPossible * 100).toInt() else 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(tBg(Color(0xFFF8F9FD)))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = tBg(Primary800))
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
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "📊 الإحصائيات الشاملة ونسب الإنجاز",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "مؤشرات تقدم اللجان عبر المراحل التسع الميدانية",
                            style = MaterialTheme.typography.bodySmall,
                            color = tFg(Primary100)
                        )
                    }
                }
            }
        }

        // Overall Progress Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = tBg(SurfacePure)),
                border = androidx.compose.foundation.BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المعدل العام لاكتمال الدورة",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = tFg(Primary900)
                        )
                        Text(
                            text = "$overallPercentage%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = tFg(Primary700)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { overallPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(9999.dp)),
                        color = tFg(Primary700),
                        trackColor = tBg(Color(0xFFE2E8F0))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "تم إنجاز $completedStagesTotal مرحلة من أصل $totalStagesPossible مرحلة في كافة اللجان.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tFg(TextMuted)
                    )
                }
            }
        }

        // Gender & Branch Breakdown
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = tBg(SurfacePure)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0)))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "مصلى الرجال 👨", style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
                        Text(text = "$maleCount لجان", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = tFg(Primary900))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = tBg(SurfacePure)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0)))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "مصلى الأخوات 🧕", style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
                        Text(text = "$womenCount لجان", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = tFg(Color(0xFF7E22CE)))
                    }
                }
            }
        }

        // Stage by Stage Progress Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = tBg(SurfacePure)),
                border = androidx.compose.foundation.BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "معدل الإنجاز بحسب كل مرحلة من المراحل الـ 9:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = tFg(Primary900)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OrderedStageKeys.forEach { (key, name) ->
                        val count = committees.count { it.stages.getStage(key).completed }
                        val pct = if (totalCommittees > 0) count.toFloat() / totalCommittees else 0f
                        val stageSet = StageColorMap[key]

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = stageIcon(key),
                                        contentDescription = null,
                                        tint = stageSet?.dot?.let { tFg(it) } ?: tFg(TextMuted),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tFg(Color(0xFF334155))
                                    )
                                }
                                Text(
                                    text = "$count / $totalCommittees (${(pct * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = stageSet?.text?.let { tFg(it) } ?: tFg(Primary700)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(9999.dp)),
                                color = stageSet?.dot?.let { tFg(it) } ?: tFg(Primary700),
                                trackColor = tBg(Color(0xFFF1F5F9))
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }

        // Web: StatisticsView — progress by period, question scoring, current stage and books.
        item { WebStatsSections(committees) }
    }
}

@Composable
private fun WebStatsSections(committees: List<Committee>) {
    val total = committees.size
    fun pct(n: Int) = if (total > 0) n * 100 / total else 0
    var period by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("week") }
    val weekly = committees.count { it.stages.ceremony.completed || it.stages.announcement.completed || it.stages.review.completed }
    val monthly = committees.count { it.stages.ceremony.completed || it.stages.announcement.completed }
    val annual = committees.count { it.stages.ceremony.completed }
    val (done, label) = when (period) {
        "month" -> monthly to "اللجان التي أُعلنت نتيجتها"
        "year" -> annual to "لجان أتمت الحفل بالكامل"
        else -> weekly to "اللجان التي بلغت مرحلة المراجعة أو ما بعدها"
    }
    val rate = pct(done)
    val level = when {
        rate >= 90 -> "مستوى التقدم: استثنائي - تجاوز التوقعات"
        rate >= 75 -> "مستوى التقدم: ممتاز - وفق الخطة المقررة"
        rate >= 50 -> "مستوى التقدم: جيد - يتطلب متابعة دورية"
        rate >= 25 -> "مستوى التقدم: مقبول - يتطلب تسريع الوتيرة"
        else -> "مستوى التقدم: في البداية - بانتظار الاعتماد الميداني"
    }
    val questions = remember(committees) { committees.sumOf { it.scoringDetails.questions.size } }
    val scored = remember(committees) { committees.sumOf { c -> c.scoringDetails.questions.count { it.completed } } }
    val byStage = remember(committees) {
        val perStage = committees.groupingBy { com.example.ui.components.currentStageKey(it.stages) }.eachCount()
        OrderedStageKeys.map { (k, n) -> n to (perStage[k] ?: 0) }
    }
    val byBook = remember(committees) { committees.groupingBy { it.competitionBook.trim().ifBlank { "متون غير مصنفة" } }.eachCount().entries.sortedByDescending { it.value } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = tBg(SurfacePure))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("لوحة الإحصائيات ونسب الإنجاز الحية", fontWeight = FontWeight.Bold, color = tFg(Primary900))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("week" to "أسبوعي", "month" to "شهري", "year" to "النسبة الإجمالية (الموسم)").forEach { (k, l) ->
                        androidx.compose.material3.FilterChip(selected = period == k, onClick = { period = k }, label = { Text(l) })
                    }
                }
                Text("$label: $done لجنة من إجمالي $total", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { rate / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = tFg(Primary700), trackColor = tBg(Primary100))
                Text("$rate% — $level", style = MaterialTheme.typography.labelSmall, color = tFg(Primary800))
            }
        }
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = tBg(SurfacePure))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("تصحيح الأسئلة", fontWeight = FontWeight.Bold, color = tFg(Primary900))
                Text("إجمالي الأسئلة المصححة: $scored من $questions", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { if (questions > 0) scored.toFloat() / questions else 0f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = tFg(Color(0xFFD97706)), trackColor = tBg(Color(0xFFFEF3C7)))
            }
        }
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = tBg(SurfacePure))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("توزيع اللجان حسب المرحلة الحالية", fontWeight = FontWeight.Bold, color = tFg(Primary900))
                byStage.forEach { (name, n) ->
                    Row { Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall); Text("$n لجنة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = tBg(SurfacePure))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("اللجان حسب الكتاب المقرر", fontWeight = FontWeight.Bold, color = tFg(Primary900))
                byBook.forEach { (book, n) ->
                    Row { Text(book, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall); Text("$n لجنة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
