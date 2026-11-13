package com.example.ui.screens.committees

import com.example.ui.theme.tEdge
import com.example.ui.theme.tBg
import com.example.ui.theme.tFg
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.Committee
import com.example.data.remote.AssignableUserDto
import com.example.data.remote.Permissions
import com.example.ui.screens.assistants.roleLabel
import com.example.ui.theme.Primary900
import com.example.ui.theme.TextMuted

// Who goes where (same rules as the web): admins place committee heads and sheikhs on committees;
// committee heads place sheikhs on the committees they head. Everyone added or removed is notified.

private fun isStaffRole(role: String) = role == "sheikh" || role == "committee_head"

private fun committeeTitle(c: Committee) = "لجنة ${c.committeeNumber} — ${c.committeeName}"

/** Committee page: its heads and sheikhs, with add / remove for those who may staff it. */
@Composable
fun CommitteeStaffSection(
    committee: Committee,
    myId: String?,
    candidates: List<AssignableUserDto>?,
    onLoadCandidates: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val canStaff = Permissions.canOn("committees.members", committee.id)
    var editing by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = tBg(Color.White)),
        border = BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0))), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("شيوخ اللجنة (${committee.staff.size})", fontWeight = FontWeight.Bold, color = tFg(Primary900), modifier = Modifier.weight(1f))
                if (canStaff) OutlinedButton(onClick = { editing = true }) { Text("إضافة / إزالة") }
            }
            if (committee.staff.isEmpty()) {
                Text("لم يُضف أحد لهذه اللجنة بعد.", style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    committee.staff.forEach { s ->
                        AssistChip(onClick = {}, label = { Text(if (s.role == "committee_head") "${s.name} · رئيس لجنة" else s.name) })
                    }
                }
            }
        }
    }
    if (editing) {
        LaunchedEffect(Unit) { if (candidates == null) onLoadCandidates() }
        var selected by remember { mutableStateOf(committee.staff.map { it.id }.toSet()) }
        var query by remember { mutableStateOf("") }
        val placeable = candidates.orEmpty().filter { isStaffRole(it.role) && it.id != myId }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("شيوخ ${committeeTitle(committee)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (candidates == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.padding(4.dp)); Text("جارٍ تحميل قائمة الشيوخ…") }
                    } else {
                        OutlinedTextField(query, { query = it }, placeholder = { Text("بحث بالاسم…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        CheckList(
                            items = placeable.filter { query.isBlank() || it.name.contains(query.trim()) },
                            checked = { it.id in selected },
                            onToggle = { p -> selected = if (p.id in selected) selected - p.id else selected + p.id },
                            title = { it.name },
                            subtitle = { roleLabel(it.role) },
                            empty = "لا يوجد أشخاص مطابقون"
                        )
                        Text("يصل لكل من تضيفه أو تزيله تنبيه بذلك.", style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
                    }
                }
            },
            confirmButton = {
                Button(enabled = candidates != null, onClick = {
                    // Only people the caller may place are sent; the server keeps everyone else as they are.
                    val ids = placeable.map { it.id }.toSet()
                    onSave(selected.filter { it in ids })
                    editing = false
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("إلغاء") } }
        )
    }
}

/**
 * A person's page: the committees they are on and the ceremonies they are invited to, changed in
 * one place. [onSave] gets the new member list per changed committee and (add, remove) per ceremony.
 */
@Composable
fun PersonAssignmentsDialog(
    userId: String,
    userName: String,
    userRole: String,
    committees: List<Committee>,
    onDismiss: () -> Unit,
    onSave: (members: Map<String, List<String>>, ceremonies: Map<String, Pair<List<String>, List<String>>>) -> Unit
) {
    val memberNow = committees.filter { c -> c.staff.any { it.id == userId } }.map { it.id }.toSet()
    val invitedNow = committees.filter { c -> c.ceremonyDetails.attendees.any { it.id == userId } }.map { it.id }.toSet()
    var memberOf by remember { mutableStateOf(memberNow) }
    var invitedTo by remember { mutableStateOf(invitedNow) }
    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }

    // Admins place committee heads and sheikhs; committee heads place sheikhs only.
    val canPlace = userRole == "sheikh" || Permissions.can("users.grant")
    val staffable = committees.filter { canPlace && Permissions.canOn("committees.members", it.id) }
    val withCeremony = committees.filter {
        Permissions.canOn("committees.finalize", it.id) && (it.ceremonyDetails.date.isNotBlank() || it.id in invitedNow)
    }
    val match: (Committee) -> Boolean = { query.isBlank() || committeeTitle(it).contains(query.trim()) || it.courseName.contains(query.trim()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("لجان وحفلات: $userName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("اللجان (${memberOf.size})") })
                    FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("الحفلات (${invitedTo.size})") })
                }
                OutlinedTextField(query, { query = it }, placeholder = { Text("بحث برقم اللجنة أو اسمها…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (tab == 0) {
                    CheckList(
                        items = staffable.filter(match),
                        checked = { it.id in memberOf },
                        onToggle = { c -> memberOf = if (c.id in memberOf) memberOf - c.id else memberOf + c.id },
                        title = { committeeTitle(it) },
                        subtitle = { it.courseName },
                        empty = "لا توجد لجان يمكنك إضافته إليها"
                    )
                } else {
                    CheckList(
                        items = withCeremony.filter(match),
                        checked = { it.id in invitedTo },
                        onToggle = { c -> invitedTo = if (c.id in invitedTo) invitedTo - c.id else invitedTo + c.id },
                        title = { committeeTitle(it) },
                        subtitle = { c ->
                            listOf(c.ceremonyDetails.date, c.ceremonyDetails.time, c.ceremonyDetails.venue).filter { it.isNotBlank() }
                                .joinToString(" — ").ifBlank { "لم يحدد الموعد" }
                        },
                        empty = "لا توجد حفلات محددة الموعد يمكنك دعوته إليها"
                    )
                }
                Text("يصله تنبيه عند إضافته أو إزالته، ويرى في حسابه اللجان والحفلات المسندة إليه فقط.", style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
            }
        },
        confirmButton = {
            Button(onClick = {
                val members = staffable.filter { (it.id in memberNow) != (it.id in memberOf) }.associate { c ->
                    val others = c.staff.map { it.id }.filter { it != userId }
                    c.id to if (c.id in memberOf) others + userId else others
                }
                val ceremonies = withCeremony.filter { (it.id in invitedNow) != (it.id in invitedTo) }.associate { c ->
                    val change: Pair<List<String>, List<String>> = if (c.id in invitedTo) Pair(listOf(userId), emptyList()) else Pair(emptyList(), listOf(userId))
                    c.id to change
                }
                if (members.isNotEmpty() || ceremonies.isNotEmpty()) onSave(members, ceremonies)
                onDismiss()
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun <T> CheckList(
    items: List<T>,
    checked: (T) -> Boolean,
    onToggle: (T) -> Unit,
    title: (T) -> String,
    subtitle: (T) -> String,
    empty: String
) {
    Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
        if (items.isEmpty()) Text(empty, color = tFg(TextMuted))
        items.forEach { item ->
            val isChecked = checked(item)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = isChecked, role = Role.Checkbox) { onToggle(item) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(isChecked, null)
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(title(item), fontWeight = FontWeight.SemiBold)
                    val sub = subtitle(item)
                    if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted))
                }
            }
        }
    }
}
