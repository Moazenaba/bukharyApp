package com.example.ui.screens.committees

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
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
import com.example.data.model.AggregationDetails
import com.example.data.model.AppUser
import com.example.data.model.CeremonyDetails
import com.example.data.model.Committee
import com.example.data.model.ScoringDetails
import com.example.data.model.SystemPermissions
import com.example.data.remote.Permissions
import com.example.ui.components.InteractiveStageStepper
import com.example.ui.components.StageBadge
import com.example.ui.theme.Primary100
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.Primary900
import com.example.ui.theme.SurfacePure

@Composable
fun CommitteeDetailModal(
    committee: Committee,
    currentUser: AppUser?,
    onClose: () -> Unit,
    onToggleStage: (stageKey: String, currentStatus: Boolean) -> Unit,
    /** The server records who received/approved from the logged-in session. */
    onAssistantReception: (phone: String) -> Unit,
    onApproveExam: () -> Unit,
    onUpdateScoring: (ScoringDetails) -> Unit,
    onUpdateAggregation: (AggregationDetails) -> Unit,
    onUpdateCeremony: (CeremonyDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0: Stepper, 1: Assistant & Exam, 2: Scoring & Aggregation, 3: Ceremony

    // Assistant handover state
    var asstPhone by remember { mutableStateOf(committee.receivedByAssistant?.phone ?: currentUser?.phone ?: "") }


    // Scoring state
    var examinerName by remember { mutableStateOf(committee.scoringDetails.examinerName) }
    var reviewerName by remember { mutableStateOf(committee.scoringDetails.reviewerName) }
    var questionsState by remember {
        mutableStateOf(committee.scoringDetails.questionsStatus.toMutableMap())
    }
    var scoringNotes by remember { mutableStateOf(committee.scoringDetails.notes) }

    // Aggregation state
    var aggregatorName by remember { mutableStateOf(committee.aggregationDetails.aggregatorName) }
    var aggReviewerName by remember { mutableStateOf(committee.aggregationDetails.reviewerName) }
    var confirmedMatch by remember { mutableStateOf(committee.aggregationDetails.isConfirmedMatch) }

    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val todayHijriOrDateStr = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date()) }

    // Ceremony state
    var ceremonyDate by remember { mutableStateOf(committee.ceremonyDetails.date) }
    var ceremonyTime by remember { mutableStateOf(committee.ceremonyDetails.time) }
    var ceremonyVenue by remember { mutableStateOf(committee.ceremonyDetails.venue) }
    var honoreesCount by remember { mutableIntStateOf(if (committee.ceremonyDetails.honoreesCount > 0) committee.ceremonyDetails.honoreesCount else committee.studentsCount) }
    var sheikhsInput by remember { mutableStateOf(committee.ceremonyDetails.attendingSheikhs.joinToString("، ")) }

    // What the server will accept from this user for this committee.
    val inScope = Permissions.inScope(committee.committeeNumber)
    val canReception = inScope && Permissions.can("committees.reception")
    val canScoring = inScope && Permissions.can("committees.scoring")
    val canFinalize = inScope && Permissions.can("committees.finalize")
    val stageAllowed: (String) -> Boolean = { key ->
        when (key) {
            "registration", "exam" -> canReception
            "scoring", "aggregation", "review" -> canScoring
            else -> canFinalize
        }
    }
    var newQuestionLabel by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("committee_detail_modal"),
        color = Color(0xFFF8F9FD)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Top Modal Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Primary700),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${committee.committeeNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = committee.committeeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary900
                        )
                        Text(
                            text = "الدورة: ${committee.courseName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFE2E8F0))
                        .testTag("btn_close_detail_modal")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Primary900)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sheikh & Meta Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePure),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📖 الكتاب: ${committee.competitionBook}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F766E)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الشيخ: ${committee.sheikhName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${committee.studentsCount} طالباً",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }
                    Text(
                        text = "المكان: ${committee.sheikhCountry} • ${if (committee.isWomenCommittee) "مصلى الأخوات" else "مصلى الرجال"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Detail Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE2E8F0))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    0 to "المراحل التسع 📌",
                    1 to "الاستلام والجاهزية 🤝",
                    2 to "التصحيح والتجميع ✏️",
                    3 to "حفل التكريم 🎓"
                ).forEach { (idx, title) ->
                    val isSel = activeTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) Primary700 else Color.Transparent)
                            .clickable { activeTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab 0: Interactive 9 Stages Stepper
            if (activeTab == 0) {
                InteractiveStageStepper(
                    stages = committee.stages,
                    onToggleStage = onToggleStage,
                    canEditStage = stageAllowed
                )
            }

            // Tab 1: Assistant Handover & Exam Readiness
            if (activeTab == 1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfacePure),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🤝 توثيق استلام المعاون الميداني للجنة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary800
                        )

                        if (committee.receivedByAssistant != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0FDF4),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "✓ تم توثيق استلام: ${committee.receivedByAssistant.receivedByName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        text = "التوقيت: ${committee.receivedByAssistant.receivedAt} • هاتف: ${committee.receivedByAssistant.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = asstPhone,
                            onValueChange = { asstPhone = it },
                            label = { Text("هاتف المعاون") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (canReception) Button(
                            onClick = { onAssistantReception(asstPhone) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary700),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("توثيق واعتماد الاستلام الميداني 📥")
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "📝 اعتماد جاهزية وتوزيع الامتحان",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary800
                        )

                        if (committee.examApprovedBy != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "✓ تم اعتماد جاهزية القاعة والأسئلة بواسطة: ${committee.examApprovedBy.approvedByName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8)
                                    )
                                    Text(
                                        text = "التوقيت: ${committee.examApprovedBy.approvedAt}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF1E40AF)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (canReception) Button(
                            onClick = { onApproveExam() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("اعتماد بدء وتوزيع الامتحان 🟦")
                        }
                    }
                }
            }

            // Tab 2: Scoring & Aggregation
            if (activeTab == 2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfacePure),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✏️ تفاصيل مرحلة التصحيح ورصد الأسئلة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary800
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = examinerName,
                            onValueChange = { examinerName = it },
                            label = { Text("اسم المصحح الرئيسي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = reviewerName,
                            onValueChange = { reviewerName = it },
                            label = { Text("اسم مراجع التصحيح") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "قائمة إنجاز تصحيح الأسئلة:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary900
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        questionsState.forEach { (qKey, done) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (done) Color(0xFFFEF3C7) else Color(0xFFF8FAFC))
                                    .clickable {
                                        val newMap = questionsState.toMutableMap()
                                        newMap[qKey] = !done
                                        questionsState = newMap
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = done,
                                        onCheckedChange = {
                                            val newMap = questionsState.toMutableMap()
                                            newMap[qKey] = it
                                            questionsState = newMap
                                        }
                                    )
                                    Text(
                                        text = "تصحيح $qKey ورصد درجات الطلاب",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (done) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                Text(
                                    text = if (done) "مصحح ✓" else "قيد التصحيح",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (done) Color(0xFFB45309) else Color(0xFF94A3B8)
                                )
                            }
                        }

                        if (questionsState.isEmpty()) {
                            Text(
                                text = "لم تُضف أسئلة لهذه اللجنة بعد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        if (canScoring) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newQuestionLabel,
                                    onValueChange = { newQuestionLabel = it },
                                    label = { Text("إضافة سؤال (مثال: السؤال الأول)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        val label = newQuestionLabel.trim().ifBlank { "السؤال ${questionsState.size + 1}" }
                                        if (label !in questionsState) {
                                            questionsState = (questionsState + (label to false)).toMutableMap()
                                        }
                                        newQuestionLabel = ""
                                    }
                                ) { Text("إضافة") }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (canScoring) Button(
                            onClick = {
                                onUpdateScoring(
                                    committee.scoringDetails.copy(
                                        examinerName = examinerName,
                                        reviewerName = reviewerName,
                                        notes = scoringNotes
                                    ).withQuestionsStatus(questionsState)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("حفظ وتحديث درجات التصحيح 🟠")
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "🔵 تجميع ومطابقة الدرجات الكلية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary800
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = aggregatorName,
                            onValueChange = { aggregatorName = it },
                            label = { Text("اسم مجمع الدرجات") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = aggReviewerName,
                            onValueChange = { aggReviewerName = it },
                            label = { Text("اسم مراجع التجميع") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEEF2FF))
                                .clickable { confirmedMatch = !confirmedMatch }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = confirmedMatch,
                                onCheckedChange = { confirmedMatch = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تأكيد مطابقة التجميع النهائي لكافة الكشوفات",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4338CA)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (canScoring) Button(
                            onClick = {
                                // copy() keeps fields edited on the web (notes, scoring reviewer).
                                onUpdateAggregation(
                                    committee.aggregationDetails.copy(
                                        aggregatorName = aggregatorName,
                                        reviewerName = aggReviewerName,
                                        isConfirmedMatch = confirmedMatch,
                                        date = todayHijriOrDateStr
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("توثيق واعتماد تجميع الدرجات 🔵")
                        }
                    }
                }
            }

            // Tab 3: Ceremony Details
            if (activeTab == 3) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfacePure),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎓 تفاصيل حفل التكريم والختام",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary800
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = ceremonyDate,
                            onValueChange = { ceremonyDate = it },
                            label = { Text("تاريخ الحفل (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = ceremonyTime,
                            onValueChange = { ceremonyTime = it },
                            label = { Text("توقيت الحفل (مثال: 04:30 عصراً)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = ceremonyVenue,
                            onValueChange = { ceremonyVenue = it },
                            label = { Text("مكان وقاعة الحفل") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = honoreesCount.toString(),
                            onValueChange = { honoreesCount = it.toIntOrNull() ?: 0 },
                            label = { Text("عدد الطلاب المكرمين") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = sheikhsInput,
                            onValueChange = { sheikhsInput = it },
                            label = { Text("أسماء الشيوخ الحاضرين (مفصولة بفاصلة)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (canFinalize) Button(
                            onClick = {
                                val sheikhsList = sheikhsInput.split("،", ",").map { it.trim() }.filter { it.isNotBlank() }
                                onUpdateCeremony(
                                    committee.ceremonyDetails.copy(
                                        date = ceremonyDate.trim(),
                                        time = ceremonyTime,
                                        venue = ceremonyVenue,
                                        honoreesCount = honoreesCount,
                                        attendingSheikhs = sheikhsList
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("حفظ وجدولة حفل التكريم بالتقويم 🎓")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
