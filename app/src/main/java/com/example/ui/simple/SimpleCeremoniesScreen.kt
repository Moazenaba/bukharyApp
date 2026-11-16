package com.example.ui.simple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Committee
import com.example.data.model.ScheduleTask
import com.example.domain.DayText
import com.example.domain.Logistics
import com.example.ui.components.WhatsApp
import com.example.ui.motion.staggerIn
import com.example.ui.theme.AppBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.tBg
import com.example.ui.theme.tFg

/** «الحفلات»: ceremonies I attend or may run: big date, place, who is with me, call and WhatsApp. */
@Composable
fun SimpleCeremoniesScreen(
    committees: List<Committee>,
    tasks: List<ScheduleTask>,
    myId: String?,
    today: String,
    canFinalize: (String) -> Boolean,
    onOpenCommittee: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mine = committees.filter { c ->
        c.ceremonyDetails.attendees.any { it.id == myId } || canFinalize(c.id)
    }.filter { it.ceremonyDetails.date.isNotBlank() || it.ceremonyDetails.attendees.any { a -> a.id == myId } }
        .sortedBy { it.ceremonyDetails.date.ifBlank { "9999-99-99" } }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(tBg(AppBackground)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "title") { SectionTitle("الحفلات") }
        if (mine.isEmpty()) item(key = "empty") {
            EmptyState(Icons.Default.Celebration, "مفيش حفلات ليك لسه", "لما تتدعى لحفل أو يتحدد حفل للجنتك هيظهر هنا.")
        }
        itemsIndexed(mine, key = { _, c -> c.id }) { index, c ->
            val d = c.ceremonyDetails
            val others = d.attendees.filter { it.id != myId }
            SimpleCard(modifier = Modifier.animateItem().staggerIn(index)) {
                Text(
                    if (d.date.isBlank()) "الموعد لم يحدد بعد" else DayText.relative(d.date, today) + if (d.time.isNotBlank()) " · ${d.time}" else "",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = tFg(Color(0xFF78350F))
                )
                Text("حفل تكريم لجنة ${c.committeeNumber} — ${c.committeeName}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                PlaceRow(Logistics.ceremonyPlaceOf(c))
                if (d.honoreesCount > 0) Text("عدد المكرمين: ${d.honoreesCount}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                val head = Logistics.headOf(c)
                ContactRow("رئيس اللجنة", head?.name.orEmpty(), head?.phone.orEmpty(), "لسه ما اتسندش رئيس لجنة")
                if (others.isNotEmpty()) Text("معك: ${others.joinToString("، ") { it.name }}", style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
                if (d.notes.isNotBlank()) Text(d.notes, style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
                if (c.stages.ceremony.completed) StatusPill("تم الحفل", Tone.SUCCESS)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (c.sheikhPhone.isNotBlank()) {
                        OutlinedButton(onClick = { callPhone(context, c.sheikhPhone) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("اتصال بالشيخ") }
                        WhatsApp.link(c.sheikhPhone, WhatsApp.ceremonyInvitation(c, tasks.find { it.id == "task-ceremony-${c.id}" }))?.let { link ->
                            OutlinedButton(onClick = { openLink(context, link) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("واتساب") }
                        }
                    }
                    TextButton(onClick = { onOpenCommittee(c.id) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("تفاصيل اللجنة") }
                }
            }
        }
    }
}
