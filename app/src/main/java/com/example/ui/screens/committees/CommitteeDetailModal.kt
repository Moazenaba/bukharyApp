package com.example.ui.screens.committees

import com.example.ui.components.ChipFlow
import com.example.ui.theme.tEdge
import com.example.ui.theme.tBg
import com.example.ui.theme.tFg
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.theme.TextMuted
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.selectable

import androidx.compose.material.icons.filled.Delete

import androidx.compose.foundation.horizontalScroll

import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.material.icons.automirrored.filled.FactCheck
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
    /** Tab shown first; [COMMITTEE_DATA_TAB] when opened from an «edit» button. */
    initialTab: Int = 0,
    onClose: () -> Unit,
    onToggleStage: (stageKey: String, currentStatus: Boolean) -> Unit,
    /** The server records who received/approved from the logged-in session; `cancel`/`revoke` undo it. */
    onAssistantReception: (phone: String, cancel: Boolean) -> Unit,
    onApproveExam: (revoke: Boolean) -> Unit,
    /** One question ticked or unticked: only that question is sent. */
    onUpdateQuestion: (questionId: String, completed: Boolean) -> Unit,
    onUpdateScoring: (ScoringDetails) -> Unit,
    onUpdateAggregation: (AggregationDetails) -> Unit,
    onUpdateCeremony: (CeremonyDetails) -> Unit,
    courses: List<com.example.data.model.Course> = emptyList(),
    onSaveBase: (Committee) -> Unit = {},
    onDelete: () -> Unit = {},
    onUpdateQuestionDetails: (String, Boolean, List<String>, Double?, String?) -> Unit = { _, _, _, _, _ -> },
    onSheetImported: (String, Int?) -> Unit = { _, _ -> },
    onSaveExcelUrl: (String) -> Unit = {},
    staffCandidates: List<com.example.data.remote.AssignableUserDto>? = null,
    onLoadStaffCandidates: () -> Unit = {},
    onSaveStaff: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(initialTab) } // 0: Stepper, 1: Assistant & Exam, 2: Scoring & Aggregation, 3: Ceremony

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
    var aggScoringReviewer by remember { mutableStateOf(committee.aggregationDetails.scoringReviewerName.ifBlank { committee.scoringDetails.reviewerName }) }
    var confirmedMatch by remember { mutableStateOf(committee.aggregationDetails.isConfirmedMatch) }

    val todayDateStr = remember { com.example.ui.components.Dates.today() }

    // Ceremony state
    var ceremonyDate by remember { mutableStateOf(committee.ceremonyDetails.date) }
    var ceremonyTime by remember { mutableStateOf(committee.ceremonyDetails.time) }
    var ceremonyVenue by remember { mutableStateOf(committee.ceremonyDetails.venue) }
    var honoreesCount by remember { mutableIntStateOf(if (committee.ceremonyDetails.honoreesCount > 0) committee.ceremonyDetails.honoreesCount else committee.studentsCount) }
    var sheikhsInput by remember { mutableStateOf(committee.ceremonyDetails.attendingSheikhs.joinToString("، ")) }

    // What the server will accept from this user for this committee (scope by committee id).
    val inScope = Permissions.inScope(committee.id)
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
    /** The id of a question the server already has, by its label; null for one added on this screen. */
    val savedQuestionId: (String) -> String? = { label ->
        committee.scoringDetails.questions.firstOrNull { it.label.ifBlank { "السؤال ${it.questionNumber}" } == label }?.id
    }

    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("committee_detail_modal"),
        color = tBg(Color(0xFFF8F9FD))
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
                            .background(tBg(Primary700)),
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
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tFg(Primary900)
                        )
                        Text(
                            text = "الدورة: ${committee.courseName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = tFg(TextMuted),
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tBg(Color(0xFFE2E8F0)))
                        .testTag("btn_close_detail_modal")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = tFg(Primary900))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // What this committee is waiting for, then its facts in two columns (it used to be one tall box).
            com.example.ui.components.CommitteeNowCard(committee, onGoTo = { activeTab = it })
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = tBg(SurfacePure)),
                border = androidx.compose.foundation.BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0)))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    com.example.ui.components.InfoGrid(
                        listOf(
                            "الكتاب" to committee.competitionBook,
                            "الشيخ" to committee.sheikhName,
                            "المكان" to committee.sheikhCountry.ifBlank { committee.branch },
                            "النوع" to (if (committee.isWomenCommittee) "مصلى الأخوات" else "مصلى الرجال"),
                            "عدد الطلاب" to (if (committee.studentsCount > 0) "${committee.studentsCount}" else "لم يُسجَّل بعد")
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Detail Navigation Tabs
            // Wraps onto two lines so every tab is visible (it used to scroll sideways, 2.5 tabs showing),
            // in the order the work happens; editing the committee's data is last.
            ChipFlow(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(tBg(Color(0xFFE2E8F0)))
                    .padding(4.dp),
                spacing = 4.dp
            ) {
                listOf(
                    0 to "المراحل التسع 📌",
                    1 to "الاستلام والجاهزية 🤝",
                    2 to "التصحيح والتجميع ✏️",
                    5 to "كشف الدرجات 🟣",
                    3 to "حفل التكريم 🎓",
                    COMMITTEE_DATA_TAB to "تعديل بيانات اللجنة"
                ).forEach { (idx, title) ->
                    val isSel = activeTab == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) tBg(Primary700) else Color.Transparent)
                            .minimumInteractiveComponentSize()
                            .selectable(selected = isSel, role = androidx.compose.ui.semantics.Role.Tab) { activeTab = idx }
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else tFg(Color(0xFF475569))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (activeTab == COMMITTEE_DATA_TAB) {
                CommitteeBaseDataTab(
                    committee = committee,
                    courses = courses,
                    canEditBase = inScope && Permissions.can("committees.update"),
                    canDelete = inScope && Permissions.can("committees.delete"),
                    onSave = onSaveBase,
                    onDelete = onDelete
                )
            }
            if (activeTab == 5) {
                CommitteeSheetTab(
                    committee = committee,
                    canFinalize = canFinalize,
                    canEditBase = inScope && Permissions.can("committees.update"),
                    onSheetImported = onSheetImported,
                    onSaveExcelUrl = onSaveExcelUrl
                )
            }

            // Tab 0: Interactive 9 Stages Stepper
            if (activeTab == 0) {
                MyObservationSection(committee)
                CommitteeStaffSection(committee, currentUser?.id, staffCandidates, onLoadStaffCandidates, onSaveStaff)
                CommitteeObserversSection(committee)
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
                    colors = CardDefaults.cardColors(containerColor = tBg(SurfacePure)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🤝 توثيق استلام المعاون الميداني للجنة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tFg(Primary800)
                        )

                        if (committee.receivedByAssistant != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = tBg(Color(0xFFF0FDF4)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "✓ تم توثيق استلام: ${committee.receivedByAssistant.receivedByName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = tFg(Color(0xFF15803D))
                                    )
                                    Text(
                                        text = "التوقيت: ${com.example.ui.components.Dates.displayTime(committee.receivedByAssistant.receivedAt)} • هاتف: ${committee.receivedByAssistant.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tFg(Color(0xFF166534))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = asstPhone,
                            onValueChange = { asstPhone = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            label = { Text("هاتف المعاون") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (canReception) Button(
                            onClick = { onAssistantReception(asstPhone, false) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = tBg(Primary700)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (committee.receivedByAssistant != null) "تحديث توثيق الاستلام 📥" else "توثيق واعتماد الاستلام الميداني 📥")
                        }
                        if (canReception && committee.receivedByAssistant != null) TextButton(
                            onClick = { onAssistantReception(asstPhone, true) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("إلغاء توثيق الاستلام") }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "📝 اعتماد جاهزية وتوزيع الامتحان",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tFg(Primary800)
                        )

                        if (committee.examApprovedBy != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = tBg(Color(0xFFEFF6FF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "✓ تم اعتماد جاهزية القاعة والأسئلة بواسطة: ${committee.examApprovedBy.approvedByName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = tFg(Color(0xFF1D4ED8))
                                    )
                                    Text(
                                        text = "التوقيت: ${com.example.ui.components.Dates.displayTime(committee.examApprovedBy.approvedAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tFg(Color(0xFF1E40AF))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (canReception && committee.examApprovedBy == null) Button(
                            onClick = { onApproveExam(false) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = tBg(Color(0xFF2563EB))),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("اعتماد بدء وتوزيع الامتحان 🟦")
                        }
                        if (canReception && committee.examApprovedBy != null) TextButton(
                            onClick = { onApproveExam(true) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("إلغاء اعتماد الجاهزية") }
                    }
                }
            }

            // Tab 2: Scoring & Aggregation
            if (activeTab == 2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = tBg(SurfacePure)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✏️ تفاصيل مرحلة التصحيح ورصد الأسئلة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tFg(Primary800)
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
                            color = tFg(Primary900)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        questionsState.forEach { (qKey, done) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (done) tBg(Color(0xFFFEF3C7)) else tBg(Color(0xFFF8FAFC)))
                                    .clickable(enabled = canScoring) {
                                        val newMap = questionsState.toMutableMap()
                                        newMap[qKey] = !done
                                        questionsState = newMap
                                        savedQuestionId(qKey)?.let { onUpdateQuestion(it, !done) }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = done,
                                        enabled = canScoring,
                                        onCheckedChange = {
                                            val newMap = questionsState.toMutableMap()
                                            newMap[qKey] = it
                                            questionsState = newMap
                                            savedQuestionId(qKey)?.let { id -> onUpdateQuestion(id, it) }
                                        }
                                    )
                                    Text(
                                        text = "تصحيح $qKey ورصد درجات الطلاب",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (done) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (done) "مصحح ✓" else "قيد التصحيح",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (done) tFg(Color(0xFFB45309)) else tFg(TextMuted)
                                    )
                                    if (canScoring && questionsState.size > 1) {
                                        IconButton(onClick = {
                                            questionsState = questionsState.toMutableMap().apply { remove(qKey) }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف $qKey", tint = tFg(Color(0xFFEF4444)))
                                        }
                                    }
                                }
                            }
                        }

                        if (questionsState.isEmpty()) {
                            Text(
                                text = "لم تُضف أسئلة لهذه اللجنة بعد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = tFg(TextMuted)
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
                            colors = ButtonDefaults.buttonColors(containerColor = tBg(Color(0xFFD97706))),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("حفظ أسماء المصححين والأسئلة الجديدة 🟠")
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "🔵 تجميع ومطابقة الدرجات الكلية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tFg(Primary800)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = aggScoringReviewer,
                            onValueChange = { aggScoringReviewer = it },
                            label = { Text("مراجع الدرجات في التصحيح") },
                            singleLine = true,
                            enabled = canScoring,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                                .background(tBg(Color(0xFFEEF2FF)))
                                .clickable { confirmedMatch = !confirmedMatch }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = confirmedMatch,
                                enabled = canScoring,
                                onCheckedChange = { confirmedMatch = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تأكيد مطابقة التجميع النهائي لكافة الكشوفات",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = tFg(Color(0xFF4338CA))
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (canScoring) Button(
                            onClick = {
                                // copy() keeps fields edited on the web (notes, scoring reviewer).
                                onUpdateAggregation(
                                    committee.aggregationDetails.copy(
                                        aggregatorName = aggregatorName,
                                        scoringReviewerName = aggScoringReviewer,
                                        reviewerName = aggReviewerName,
                                        isConfirmedMatch = confirmedMatch,
                                        date = todayDateStr
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = tBg(Color(0xFF4338CA))),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("توثيق واعتماد تجميع الدرجات 🔵")
                        }
                    }
                }
            }

            // Tab 3: Ceremony Details
            if (activeTab == 2) {
                Spacer(modifier = Modifier.height(12.dp))
                QuestionScorersSection(committee = committee, canScore = canScoring, onUpdate = onUpdateQuestionDetails)
            }

            if (activeTab == 3) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = tBg(SurfacePure)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎓 تفاصيل حفل التكريم والختام",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tFg(Primary800)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        com.example.ui.components.DateField(
                            value = ceremonyDate,
                            onValueChange = { ceremonyDate = it },
                            label = "تاريخ الحفل",
                            allowClear = true
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
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
                        val chosenSheikhs = sheikhsInput.split("،", ",").map { it.trim() }.filter { it.isNotBlank() }
                        com.example.ui.components.NameSuggestions("اختيار من قائمة مشايخ الحفلات", com.example.data.remote.AppSettings.current.ceremonySheikhs, chosenSheikhs) { n ->
                            sheikhsInput = (chosenSheikhs + n).joinToString("، ")
                        }

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
                            colors = ButtonDefaults.buttonColors(containerColor = tBg(Color(0xFF16A34A))),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("حفظ وجدولة حفل التكريم بالتقويم 🎓")
                        }
                        if (canFinalize) Text(
                            text = "يظهر الموعد في التقويم تلقائياً. إتمام مرحلة الحفل يتم من تبويب المراحل.",
                            style = MaterialTheme.typography.bodySmall,
                            color = tFg(TextMuted),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

/** The «تعديل بيانات اللجنة» tab of the committee details. */
const val COMMITTEE_DATA_TAB = 4
