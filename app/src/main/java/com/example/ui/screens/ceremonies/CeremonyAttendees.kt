package com.example.ui.screens.ceremonies

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CeremonyAttendee
import com.example.data.model.Committee
import com.example.data.model.ScheduleTask
import com.example.data.remote.AssignableUserDto
import com.example.ui.components.arabicDayName
import com.example.ui.screens.assistants.roleLabel
import com.example.ui.theme.TextMuted

/** Picks the accounts invited to a ceremony; the server notifies each newly added person. */
@Composable
fun AttendeePicker(
    candidates: List<AssignableUserDto>?,
    onLoad: () -> Unit,
    selected: List<CeremonyAttendee>,
    onChange: (List<CeremonyAttendee>) -> Unit
) {
    LaunchedEffect(Unit) { if (candidates == null) onLoad() }
    var query by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("المدعوون للحفل (${selected.size}) — يصل لكل من تضيفه إشعار بالموعد", fontWeight = FontWeight.Bold)
        if (candidates == null) {
            Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.padding(4.dp)); Text("جارٍ تحميل قائمة الأشخاص…") }
            return@Column
        }
        OutlinedTextField(query, { query = it }, placeholder = { Text("بحث بالاسم…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
            val shown = candidates.filter { query.isBlank() || it.name.contains(query.trim()) }
            if (shown.isEmpty()) Text("لا يوجد أشخاص مطابقون", color = TextMuted)
            shown.forEach { p ->
                val checked = selected.any { it.id == p.id }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = checked, role = Role.Checkbox) {
                        onChange(if (checked) selected.filterNot { it.id == p.id } else selected + CeremonyAttendee(p.id, p.name))
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked, null)
                    Spacer(Modifier.width(6.dp))
                    Text(p.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(roleLabel(p.role), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }
    }
}

/** «حفلاتي»: ceremonies the signed-in user is invited to, with their details (same as the web). */
@Composable
fun MyCeremoniesSection(committees: List<Committee>, tasks: List<ScheduleTask>, myId: String?, onOpenCommittee: (String) -> Unit) {
    val mine = committees.filter { c -> myId != null && c.ceremonyDetails.attendees.any { it.id == myId } }
        .sortedBy { it.ceremonyDetails.date }
    if (mine.isEmpty()) return
    Card(
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        border = BorderStroke(2.dp, Color(0xFFFCD34D))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🎉 حفلاتي (${mine.size})", fontWeight = FontWeight.Black, color = Color(0xFF78350F), style = MaterialTheme.typography.titleMedium)
            mine.forEach { c ->
                val d = c.ceremonyDetails
                val date = d.date.ifBlank { tasks.find { it.id == "task-ceremony-${c.id}" }?.date ?: "" }
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFFDE68A))) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("حفل تكريم لجنة #${c.committeeNumber} — ${c.committeeName}", fontWeight = FontWeight.Black)
                        Text(
                            "الموعد: " + if (date.isBlank()) "لم يحدد بعد" else "يوم ${arabicDayName(date)} $date${if (d.time.isNotBlank()) " — ${d.time}" else ""}",
                            fontWeight = FontWeight.Bold
                        )
                        if (d.venue.isNotBlank()) Text("📍 ${d.venue}")
                        Text("الكتاب: ${c.competitionBook} • الشيخ: ${c.sheikhName.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall)
                        if (d.honoreesCount > 0) Text("عدد المكرمين: ${d.honoreesCount} طالب", style = MaterialTheme.typography.bodySmall)
                        val others = d.attendees.filter { it.id != myId }
                        if (others.isNotEmpty()) Text("معك في الحفل: ${others.joinToString("، ") { it.name }}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        if (d.notes.isNotBlank()) Text("📝 ${d.notes}", style = MaterialTheme.typography.bodySmall)
                        if (c.stages.ceremony.completed) Text("✓ تم الحفل", color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                        TextButton(onClick = { onOpenCommittee(c.id) }) { Text("تفاصيل اللجنة ↗") }
                    }
                }
            }
        }
    }
}
