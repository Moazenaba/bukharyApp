package com.example.ui.screens.committees

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AssignmentNote
import com.example.data.model.Committee
import com.example.data.model.CommitteeAssignment
import com.example.data.remote.AssignableUserDto
import com.example.data.remote.OnlineApi
import com.example.data.remote.Permissions
import com.example.domain.AssignmentText
import com.example.ui.components.UiMessages
import com.example.ui.screens.assistants.roleLabel
import com.example.ui.theme.Primary900
import com.example.ui.theme.TextMuted
import com.example.ui.theme.tBg
import com.example.ui.theme.tEdge
import com.example.ui.theme.tFg
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// «مراقب»: an extra job on top of a person's role. They stay a sheikh and are sent to another committee for
// its exam or its ceremony. The server enforces everything; these sections only show what it accepts.

private fun failure(e: Exception, fallback: String): String = (e as? OnlineApi.ApiFailure)?.message ?: fallback

@Composable
private fun AssignmentCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = tBg(Color.White)),
        border = BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0))), modifier = Modifier.fillMaxWidth()
    ) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
}

/** Committee page, for whoever may assign: the people sent to observe this committee, and a way to add one. */
@Composable
fun CommitteeObserversSection(committee: Committee) {
    if (!Permissions.canOn("assignments.manage", committee.id)) return
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<CommitteeAssignment>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf<Pair<CommitteeAssignment, String>?>(null) }

    LaunchedEffect(committee.id, reload) {
        list = try {
            OnlineApi.assignments(committeeId = committee.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiMessages.show(failure(e, "تعذر تحميل المراقبين"))
            emptyList()
        }
    }

    AssignmentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("المراقبون المكلّفون بهذه اللجنة", fontWeight = FontWeight.Bold, color = tFg(Primary900), modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { adding = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("تكليف مراقب") }
        }
        val rows = list
        when {
            rows == null -> CircularProgressIndicator(Modifier.padding(4.dp))
            rows.isEmpty() -> Text("لا يوجد مراقبون مكلّفون بهذه اللجنة.", style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
            else -> rows.forEach { a ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(a.userName, fontWeight = FontWeight.Bold)
                    Text(
                        "${AssignmentText.task(a.task)} · ${AssignmentText.dates(a.startsOn, a.endsOn)} · أسنده ${a.assignedBy}",
                        style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(AssignmentText.status(a.status), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        if (a.status == "active") {
                            TextButton(onClick = { finishing = a to "end" }, modifier = Modifier.heightIn(min = 48.dp)) { Text("إنهاء") }
                            TextButton(onClick = { finishing = a to "cancel" }, modifier = Modifier.heightIn(min = 48.dp)) { Text("إلغاء") }
                        }
                    }
                }
            }
        }
    }

    finishing?.let { (a, how) ->
        AlertDialog(
            onDismissRequest = { finishing = null },
            title = { Text(if (how == "end") "إنهاء التكليف؟" else "إلغاء التكليف؟") },
            text = { Text("${a.userName}: ${AssignmentText.task(a.task)}. تنتهي صلاحيات المراقبة فورًا، ويبقى سجل التكليف.") },
            confirmButton = {
                Button(onClick = {
                    finishing = null
                    scope.launch {
                        try {
                            OnlineApi.finishAssignment(a.id, how)
                            reload++
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            UiMessages.show(failure(e, "تعذر تنفيذ العملية"))
                        }
                    }
                }) { Text(if (how == "end") "إنهاء" else "إلغاء التكليف") }
            },
            dismissButton = { TextButton(onClick = { finishing = null }) { Text("رجوع") } }
        )
    }

    if (adding) {
        var people by remember { mutableStateOf<List<AssignableUserDto>?>(null) }
        var picked by remember { mutableStateOf<String?>(null) }
        var task by remember { mutableStateOf("exam") }
        var endsOn by remember { mutableStateOf("") }
        var saving by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            people = try {
                OnlineApi.ceremonyCandidates()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiMessages.show(failure(e, "تعذر تحميل قائمة الأشخاص"))
                emptyList()
            }
        }
        val onCommittee = committee.staff.map { it.id }.toSet()
        val options = people.orEmpty().filter { (it.role == "sheikh" || it.role == "committee_head") && it.id !in onCommittee }
        AlertDialog(
            onDismissRequest = { if (!saving) adding = false },
            title = { Text("تكليف مراقب: لجنة ${committee.committeeNumber}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = task == "exam", onClick = { task = "exam" }, label = { Text(AssignmentText.task("exam")) })
                        FilterChip(selected = task == "ceremony", onClick = { task = "ceremony" }, label = { Text(AssignmentText.task("ceremony")) })
                    }
                    OutlinedTextField(
                        endsOn, { endsOn = it.take(10) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        label = { Text("ينتهي في (اختياري) YYYY-MM-DD") }
                    )
                    if (people == null) {
                        CircularProgressIndicator(Modifier.padding(4.dp))
                    } else if (options.isEmpty()) {
                        Text("لا يوجد أشخاص متاحون للتكليف.", color = tFg(TextMuted))
                    } else {
                        options.forEach { p ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { picked = p.id },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = picked == p.id, onClick = { picked = p.id })
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold)
                                    Text(roleLabel(p.role), style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
                                }
                            }
                        }
                    }
                    Text(
                        "يبقى الشخص على رتبته ولجنته، ويرى هذه اللجنة للقراءة فقط (وورقة امتحانها في تكليف الامتحان) حتى ينتهي التكليف.",
                        style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted)
                    )
                }
            },
            confirmButton = {
                Button(enabled = picked != null && !saving, onClick = {
                    val who = picked ?: return@Button
                    saving = true
                    scope.launch {
                        try {
                            OnlineApi.createAssignment(who, committee.id, task, endsOn.trim().ifEmpty { null })
                            adding = false
                            reload++
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            UiMessages.show(failure(e, "تعذر إنشاء التكليف"))
                        } finally {
                            saving = false
                        }
                    }
                }) { Text("تكليف") }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { adding = false }) { Text("إلغاء") } }
        )
    }
}

/** Committee page, for the observer: what they were sent to do here, and a place for their notes. */
@Composable
fun MyObservationSection(committee: Committee) {
    val mine = Permissions.observingOn(committee.id)
    if (mine.isEmpty()) return
    val assignmentId = mine.first().id
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf<List<AssignmentNote>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(assignmentId, reload) {
        notes = try {
            OnlineApi.observations(assignmentId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiMessages.show(failure(e, "تعذر تحميل الملاحظات"))
            emptyList()
        }
    }
    Card(
        shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = tBg(Color(0xFFFFFBEB))),
        border = BorderStroke(1.dp, tEdge(Color(0xFFFDE68A))), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(AssignmentText.mine(mine.map { it.task }), fontWeight = FontWeight.Bold, color = tFg(Color(0xFF78350F)))
            Text("صلاحيتك هنا للقراءة وكتابة الملاحظات فقط، وتنتهي بانتهاء التكليف.", style = MaterialTheme.typography.bodySmall, color = tFg(Color(0xFF78350F)))
            notes.forEach { n -> Text("• ${n.note}", style = MaterialTheme.typography.bodyMedium) }
            OutlinedTextField(
                text, { text = it }, modifier = Modifier.fillMaxWidth(), minLines = 3,
                label = { Text("ملاحظة المراقبة") }, placeholder = { Text("اكتب ملاحظاتك عن سير الامتحان أو الحفل…") }
            )
            Button(
                enabled = text.isNotBlank() && !saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                onClick = {
                    saving = true
                    scope.launch {
                        try {
                            OnlineApi.addObservation(assignmentId, text.trim())
                            text = ""
                            reload++
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            UiMessages.show(failure(e, "تعذر حفظ الملاحظة"))
                        } finally {
                            saving = false
                        }
                    }
                }
            ) { Text("حفظ الملاحظة") }
        }
    }
}
