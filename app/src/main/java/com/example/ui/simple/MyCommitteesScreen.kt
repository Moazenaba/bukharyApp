package com.example.ui.simple

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Committee
import com.example.domain.DayText
import com.example.domain.NextStep
import com.example.ui.components.currentStageKey
import com.example.ui.motion.MotionSpec
import com.example.ui.motion.rememberReducedMotion
import com.example.ui.motion.staggerIn
import com.example.ui.theme.AppBackground
import com.example.ui.theme.Primary100
import com.example.ui.theme.Primary600
import com.example.ui.theme.StageColorMap
import com.example.ui.theme.TextMuted
import com.example.ui.theme.tBg
import com.example.ui.theme.tFg

/** «لجاني»: one card per committee, one sentence of status, one pill. No filters, no export. */
@Composable
fun MyCommitteesScreen(
    committees: List<Committee>,
    today: String,
    canOn: (action: String, committeeId: String) -> Boolean,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val rows = remember(committees, today) {
        committees.map { c -> c to NextStep.of(c) { a -> canOn(a, c.id) } }
            .sortedWith(
                compareByDescending<Pair<Committee, NextStep>> { it.second.canDo && it.second.kind != com.example.domain.StepKind.DONE }
                    .thenBy { it.first.examDate.orEmpty().ifBlank { "9999-99-99" } }
                    .thenBy { it.first.committeeNumber }
            )
    }
    val shown = remember(rows, query) {
        val q = query.trim()
        if (q.isEmpty()) rows else rows.filter { (c, _) ->
            c.committeeName.contains(q) || c.committeeNumber.toString() == q || c.sheikhName.contains(q)
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(tBg(AppBackground)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "title") { SectionTitle(if (rows.size > 1) "لجاني (${rows.size})" else "لجنتي") }
        if (rows.size > 8) {
            item(key = "search") {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    label = { Text("ابحث بالرقم أو الاسم") }, modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (shown.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Default.Groups,
                    title = if (rows.isEmpty()) "لسه مفيش لجنة متسجلة ليك" else "مفيش نتيجة للبحث",
                    body = if (rows.isEmpty()) "أول ما تتعيّن على لجنة هتظهر هنا." else "جرّب رقم اللجنة أو جزء من الاسم."
                )
            }
        }
        itemsIndexed(shown, key = { _, r -> r.first.id }) { index, (c, step) ->
            CommitteeSummaryCard(c, step, today, { onOpen(c.id) }, Modifier.animateItem().staggerIn(index))
        }
    }
}

@Composable
fun CommitteeSummaryCard(
    committee: Committee,
    step: NextStep,
    today: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = StageColorMap[currentStageKey(committee.stages)]
    val status = stage?.activeLabel.orEmpty()
    val place = committee.associationName.ifBlank { committee.branch }
    val exam = committee.examDate?.takeIf { it.isNotBlank() }?.let { DayText.relative(it, today) }.orEmpty()
    val line = listOf(status, exam, place).filter { it.isNotBlank() }.joinToString(" · ")
    SimpleCard(modifier = modifier, onClick = onClick, onClickLabel = "فتح لجنة ${committee.committeeNumber}") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                committee.committeeNumber.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = tFg(Primary600),
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(tBg(Primary100)).padding(horizontal = 12.dp, vertical = 10.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(committee.committeeName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = tFg(Color(0xFF0F172A)))
                if (line.isNotBlank()) Text(line, style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        val reduced = rememberReducedMotion()
        val target = if (step.total == 0) 0f else step.done.toFloat() / step.total
        val progress by animateFloatAsState(target, tween(if (reduced) 0 else MotionSpec.LONG), label = "progress")
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
            color = tFg(Primary600), trackColor = tBg(Primary100)
        )
        when {
            step.kind == com.example.domain.StepKind.DONE -> StatusPill("اكتملت كل المراحل", Tone.SUCCESS)
            step.canDo -> StatusPill("الخطوة الجاية: ${step.title}", Tone.INFO)
            else -> StatusPill(step.waiting, Tone.NEUTRAL)
        }
    }
}
