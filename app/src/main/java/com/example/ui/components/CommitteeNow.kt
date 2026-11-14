package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Committee
import com.example.data.remote.AppSettings
import com.example.data.remote.Permissions
import com.example.domain.NextStep
import com.example.domain.StepKind
import com.example.ui.theme.TextMuted
import com.example.ui.theme.tBg
import com.example.ui.theme.tEdge
import com.example.ui.theme.tFg

/** Which tab of the committee screen holds the form for this step. */
fun tabForStep(kind: StepKind): Int = when (kind) {
    StepKind.RECEIVE, StepKind.EXAM_READY -> 1
    StepKind.SCORING, StepKind.AGGREGATION -> 2
    StepKind.SHEET -> 5
    StepKind.CEREMONY -> 3
    else -> 0
}

/**
 * The first thing on the committee screen: what this committee is waiting for, in red when it is
 * late (with how long), and a button that goes to the right place. Whoever may not do the step is
 * told what the committee waits for instead.
 */
@Composable
fun CommitteeNowCard(committee: Committee, onGoTo: (tab: Int) -> Unit, modifier: Modifier = Modifier) {
    val step = NextStep.of(committee) { Permissions.canOn(it, committee.id) }
    val late = Overdue.committees(listOf(committee), AppSettings.current.overdueDays).firstOrNull()
    val done = step.kind == StepKind.DONE
    val shape = RoundedCornerShape(16.dp)
    val bg = when { done -> Color(0xFFECFDF5); late != null -> Color(0xFFFFF1F2); else -> Color(0xFFFFFBEB) }
    val edge = when { done -> Color(0xFFA7F3D0); late != null -> Color(0xFFFECDD3); else -> Color(0xFFFDE68A) }
    val ink = when { done -> Color(0xFF065F46); late != null -> Color(0xFFB91C1C); else -> Color(0xFF92400E) }
    Column(
        modifier.fillMaxWidth().background(tBg(bg), shape).border(1.dp, tEdge(edge), shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            if (done) "اكتملت كل المراحل" else if (late != null) "متأخرة — المطلوب الآن" else "المطلوب الآن",
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = tFg(ink)
        )
        if (!done) {
            Text(step.waiting, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = tFg(Color(0xFF0F172A)))
            if (late != null) Text(late.reason, style = MaterialTheme.typography.bodySmall, color = tFg(ink))
            if (step.canDo) {
                Button(
                    onClick = { onGoTo(tabForStep(step.kind)) },
                    colors = ButtonDefaults.buttonColors(containerColor = tBg(if (late != null) Color(0xFFE11D48) else Color(0xFF0F766E)), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text(step.title, fontWeight = FontWeight.Bold) }
            } else {
                Text("لا تملك صلاحية تنفيذ هذه الخطوة.", style = MaterialTheme.typography.labelMedium, color = tFg(TextMuted))
            }
        }
    }
}

/** Label / value pairs, two to a row, instead of one long line per fact; empty values are left out. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InfoGrid(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val shown = items.filter { it.second.isNotBlank() }
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 2
    ) {
        shown.forEach { (label, value) ->
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted))
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = tFg(Color(0xFF0F172A)), maxLines = 2)
            }
        }
    }
}
