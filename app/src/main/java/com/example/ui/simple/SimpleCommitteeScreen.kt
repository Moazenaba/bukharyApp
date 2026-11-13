package com.example.ui.simple

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.AssignableUserDto
import com.example.data.model.Committee
import com.example.domain.DayText
import com.example.domain.NextStep
import com.example.domain.STAGE_ORDER
import com.example.domain.StepKind
import com.example.ui.components.WhatsApp
import com.example.ui.components.currentStageKey
import com.example.ui.motion.MotionSpec
import com.example.ui.motion.rememberReducedMotion
import com.example.ui.screens.committees.CommitteeStaffSection
import com.example.ui.theme.AppBackground
import com.example.ui.theme.Primary100
import com.example.ui.theme.Primary600
import com.example.ui.theme.StageColorMap
import com.example.ui.theme.TextMuted
import com.example.ui.theme.tBg
import com.example.ui.theme.tFg

/** Which legacy form tab does a step that needs a form open on (the screens that already exist). */
fun legacyTabFor(kind: StepKind): Int = when (kind) {
    StepKind.SCORING, StepKind.AGGREGATION -> 2
    StepKind.SHEET -> 5
    StepKind.CEREMONY -> 3
    else -> 0
}

const val LEGACY_EDIT_TAB = 4

/**
 * One committee, top to bottom, each fact once: status line, the next step, info, team, all stages.
 * Steps that need a form open the existing full form at the right tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleCommitteeScreen(
    committee: Committee,
    step: NextStep,
    today: String,
    myId: String?,
    candidates: List<AssignableUserDto>?,
    onLoadCandidates: () -> Unit,
    onSaveStaff: (List<String>) -> Unit,
    onBack: () -> Unit,
    onDoStep: () -> Unit,
    onOpenForm: (tab: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var confirm by rememberSaveable { mutableStateOf(false) }
    var allStages by rememberSaveable { mutableStateOf(false) }
    val stage = StageColorMap[currentStageKey(committee.stages)]

    Column(modifier.fillMaxSize().background(tBg(AppBackground))) {
        TopAppBar(
            title = { Text("لجنة ${committee.committeeNumber}", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            }
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Status line
            SimpleCard {
                Text(committee.committeeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = tFg(Color(0xFF0F172A)))
                Text(stage?.activeLabel.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = tFg(TextMuted))
                val reduced = rememberReducedMotion()
                val progress by androidx.compose.animation.core.animateFloatAsState(
                    step.done.toFloat() / step.total,
                    androidx.compose.animation.core.tween(if (reduced) 0 else MotionSpec.LONG), label = "p"
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 6.dp, max = 6.dp),
                    color = tFg(Primary600), trackColor = tBg(Primary100)
                )
                Text("أنجزنا ${step.done} من ${step.total} مراحل", style = MaterialTheme.typography.labelMedium, color = tFg(TextMuted))
            }

            // 2. Next step
            if (step.kind != StepKind.DONE) {
                if (step.canDo) {
                    Column(
                        Modifier.fillMaxWidth().background(tBg(Primary100), RoundedCornerShape(20.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("الخطوة الجاية", style = MaterialTheme.typography.labelLarge, color = tFg(Primary600))
                        Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = tFg(Color(0xFF0F172A)))
                        Button(
                            onClick = { if (step.oneTap) confirm = true else onDoStep() },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { Text(if (step.oneTap) step.title else "افتح النموذج", fontWeight = FontWeight.Bold) }
                    }
                } else {
                    SimpleCard {
                        Text("اللجنة منتظرة", style = MaterialTheme.typography.labelLarge, color = tFg(TextMuted))
                        Text(step.waiting, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = tFg(Color(0xFF0F172A)))
                    }
                }
            } else {
                StatusPill("اكتملت كل مراحل اللجنة", Tone.SUCCESS)
            }

            // 3. Info (each fact once)
            SimpleCard {
                SectionTitle("معلومات اللجنة")
                InfoRow("الشيخ", committee.sheikhName)
                InfoRow("المكان", committee.associationName.ifBlank { committee.branch })
                InfoRow("الكتاب", committee.competitionBook)
                InfoRow("عدد الطلاب", committee.studentsCount.takeIf { it > 0 }?.toString().orEmpty())
                InfoRow("ميعاد الامتحان", committee.examDate?.takeIf { it.isNotBlank() }?.let { DayText.relative(it, today) }.orEmpty())
                if (committee.sheikhPhone.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { callPhone(context, committee.sheikhPhone) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icon(Icons.Default.Phone, null); Text("  اتصال")
                        }
                        WhatsApp.link(committee.sheikhPhone, "السلام عليكم ورحمة الله")?.let { link ->
                            OutlinedButton(onClick = { openLink(context, link) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("واتساب") }
                        }
                    }
                }
                TextButton(onClick = { onOpenForm(LEGACY_EDIT_TAB) }) { Text("تعديل بيانات اللجنة") }
            }

            // 4. Team
            CommitteeStaffSection(committee, myId, candidates, onLoadCandidates, onSaveStaff)

            // 5. All stages (collapsed, read only)
            SimpleCard(onClick = { allStages = !allStages }, onClickLabel = if (allStages) "إخفاء كل المراحل" else "عرض كل المراحل") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("كل المراحل", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Icon(if (allStages) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
                AnimatedVisibility(
                    visible = allStages,
                    enter = expandVertically(spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        STAGE_ORDER.forEach { key ->
                            val s = committee.stages.getStage(key)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    if (s.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null,
                                    tint = if (s.completed) tFg(Primary600) else tFg(TextMuted)
                                )
                                Column {
                                    Text(StageColorMap[key]?.name ?: key, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    val by = s.completedBy.orEmpty()
                                    if (s.completed && by.isNotBlank()) Text(by, style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(step.title) },
            text = { Text("لجنة ${committee.committeeNumber} — تأكيد الخطوة؟") },
            confirmButton = { TextButton(onClick = { confirm = false; onDoStep() }) { Text("تأكيد") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = tFg(TextMuted))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = tFg(Color(0xFF0F172A)))
    }
}
