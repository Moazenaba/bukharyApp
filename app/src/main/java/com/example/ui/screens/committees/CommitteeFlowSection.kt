package com.example.ui.screens.committees

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Checkbox
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
import com.example.data.model.Committee
import com.example.data.remote.OnlineApi
import com.example.data.remote.Permissions
import com.example.ui.components.UiMessages
import com.example.ui.theme.Primary900
import com.example.ui.theme.TextMuted
import com.example.ui.theme.tBg
import com.example.ui.theme.tEdge
import com.example.ui.theme.tFg
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

// The committee's flow (the server's docs/PLAN_ROLES_AND_FLOW.md), on the committee page: its step and the next one
// for whoever takes it, the person's own job with «اعتذار», the owner («شيخ اللجنة») and the driver, the observers,
// the ceremony and its sheikhs. Each person sees only what the server sends them; buttons follow the server's rights.

/** The steps, in order, with who takes each one. */
object FlowSteps {
    data class Step(val key: String, val label: String, val by: String?)

    val all = listOf(
        Step("draft", "إنشاء", null),
        Step("sent_to_manager", "إرسال للمدير", "admin"),
        Step("observers_open", "السماح بالمراقبين", "admin"),
        Step("active", "تفعيل", "manager"),
        Step("observed", "تمت المراقبة", "manager"),
        Step("accredited", "الاعتماد", "admin"),
        Step("ceremony_done", "تمت الحفلة", "manager"),
    )

    fun index(key: String?): Int = all.indexOfFirst { it.key == (key ?: "active") }.coerceAtLeast(0)

    val roleLabel = mapOf("owner" to "شيخ اللجنة", "observer" to "مراقب", "ceremony_sheikh" to "شيخ الحفلة")
}

private fun Map<String, Any?>.str(key: String): String = (this[key] as? String).orEmpty()

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.obj(key: String): Map<String, Any?>? = this[key] as? Map<String, Any?>

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.objs(key: String): List<Map<String, Any?>> = (this[key] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList()

private fun failure(e: Exception, fallback: String): String = (e as? OnlineApi.ApiFailure)?.message ?: fallback

@Composable
private fun FlowCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = tBg(Color.White)),
        border = BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0))), modifier = Modifier.fillMaxWidth()
    ) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommitteeFlowSection(committee: Committee) {
    val scope = rememberCoroutineScope()
    var data by remember(committee.id) { mutableStateOf<Map<String, Any?>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf<String?>(null) }
    var stepTo by remember { mutableStateOf<FlowSteps.Step?>(null) }
    var excusing by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var removing by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var editingDriver by remember { mutableStateOf(false) }
    var editingCeremony by remember { mutableStateOf(false) }

    LaunchedEffect(committee.id, reload) {
        data = try {
            OnlineApi.committeePeople(committee.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
    val d = data ?: return

    fun act(fallback: String, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
                reload++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiMessages.show(failure(e, fallback))
            }
        }
    }

    val viewer = d.str("viewer")
    val lifecycle = d.str("lifecycle")
    val at = FlowSteps.index(lifecycle)
    val next = FlowSteps.all.getOrNull(at + 1)
    val isAdmin = Permissions.can("lifecycle.admin")
    val canStep = next != null && (if (next.by == "admin") isAdmin else Permissions.can("lifecycle.manager"))
    val full = viewer == "admin" || viewer == "manager"
    val canStaff = full || (viewer == "owner" && d["ownerCanAssign"] == true)
    val observersOpen = isAdmin || at >= FlowSteps.index("observers_open")
    val ceremonyOpen = isAdmin || at >= FlowSteps.index("accredited")

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowCard {
            Text("مرحلة اللجنة: ${d.str("lifecycleLabel")}", fontWeight = FontWeight.Bold, color = tFg(Primary900))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FlowSteps.all.forEachIndexed { i, s ->
                    Text(
                        (if (i < at) "✓ " else "") + s.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (i == at) FontWeight.Bold else FontWeight.Normal,
                        color = if (i == at) tFg(Primary900) else tFg(TextMuted)
                    )
                }
            }
            if (at in 1 until FlowSteps.index("active")) {
                Text("تنبيهات الأعضاء محبوسة حتى يفعّل مدير الحفلات والامتحانات اللجنة.", style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
            }
            if (canStep && next != null) {
                Button(onClick = { stepTo = next }, modifier = Modifier.heightIn(min = 48.dp)) { Text(next.label) }
            }
        }

        d.objs("mine").forEach { m ->
            FlowCard {
                Text("تكليفك في هذه اللجنة: ${FlowSteps.roleLabel[m.str("role")] ?: ""}", fontWeight = FontWeight.Bold)
                if (m.str("note").isNotBlank()) Text("ملاحظة: ${m.str("note")}", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { excusing = m }, modifier = Modifier.heightIn(min = 48.dp)) { Text("اعتذار") }
            }
        }

        FlowCard {
            Text("شيخ اللجنة", fontWeight = FontWeight.Bold, color = tFg(Primary900))
            val owner = d.obj("owner")
            if (owner == null) Text("لم يُحدد بعد", color = tFg(TextMuted))
            else PersonLine(owner.str("name"), owner.str("phone"))
            if (Permissions.can("committees.delegate") && full && owner != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    act("تعذر الحفظ") { OnlineApi.setCanAssign(committee.id, d["ownerCanAssign"] != true) }
                }) {
                    Checkbox(checked = d["ownerCanAssign"] == true, onCheckedChange = { v -> act("تعذر الحفظ") { OnlineApi.setCanAssign(committee.id, v) } })
                    Text("السماح لشيخ اللجنة بالتعيين")
                }
            }
            val driver = d.obj("driver")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("السائق", fontWeight = FontWeight.Bold)
                    if (driver == null) Text("لا يوجد سائق", color = tFg(TextMuted)) else PersonLine(driver.str("name").ifBlank { "محدد" }, driver.str("phone"))
                }
                if (Permissions.can("committees.driver") && full) TextButton(onClick = { editingDriver = true }) { Text("تعديل") }
            }
        }

        if (viewer != "ceremony_sheikh") {
            FlowCard {
                val observers = d.objs("observers")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("المراقبون (${observers.size})", fontWeight = FontWeight.Bold, color = tFg(Primary900), modifier = Modifier.weight(1f))
                    if (canStaff && observersOpen) OutlinedButton(onClick = { adding = "observer" }, modifier = Modifier.heightIn(min = 48.dp)) { Text("إضافة مراقب") }
                }
                if (observers.isEmpty()) Text("لا يوجد مراقبون بعد.", color = tFg(TextMuted), style = MaterialTheme.typography.bodySmall)
                observers.forEach { s -> StaffLine(s, canStaff) { removing = s } }
            }
        }

        if (viewer != "observer") {
            FlowCard {
                val sheikhs = d.objs("ceremonySheikhs")
                val c = committee.ceremonyDetails
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الحفلة ومشايخها (${sheikhs.size})", fontWeight = FontWeight.Bold, color = tFg(Primary900), modifier = Modifier.weight(1f))
                    if (canStaff && ceremonyOpen) OutlinedButton(onClick = { adding = "ceremony_sheikh" }, modifier = Modifier.heightIn(min = 48.dp)) { Text("تعيين شيخ") }
                }
                val date = listOf(c.date, c.time).filter { it.isNotBlank() }.joinToString(" ")
                Text(
                    if (date.isBlank() && c.venue.isBlank()) "لم تُنشأ الحفلة بعد."
                    else "الموعد: ${date.ifBlank { "لم يُحدد" }} · المكان: ${c.venue.ifBlank { "لم يُحدد" }}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (Permissions.can("committees.ceremony") && full) TextButton(onClick = { editingCeremony = true }) { Text(if (date.isBlank()) "إنشاء الحفلة" else "تعديل الحفلة") }
                sheikhs.forEach { s -> StaffLine(s, canStaff) { removing = s } }
            }
        }
    }

    stepTo?.let { s ->
        AlertDialog(
            onDismissRequest = { stepTo = null },
            title = { Text("تأكيد: ${s.label}؟") },
            text = { Text(if (s.key == "active") "سيصل لكل أعضاء اللجنة الآن تنبيه بالتفاصيل." else "تنتقل اللجنة إلى «${s.label}».") },
            confirmButton = { Button(onClick = { stepTo = null; act("تعذر تنفيذ الخطوة") { OnlineApi.lifecycleStep(committee.id, s.key) } }) { Text("تأكيد") } },
            dismissButton = { TextButton(onClick = { stepTo = null }) { Text("رجوع") } }
        )
    }

    excusing?.let { m ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { excusing = null },
            title = { Text("الاعتذار عن التكليف") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يصل اعتذارك لمدير الحفلات والامتحانات ولشيخ اللجنة.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("السبب") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    excusing = null
                    act("تعذر إرسال الاعتذار") { OnlineApi.excuse(committee.id, m.str("id"), reason) }
                }) { Text("إرسال الاعتذار") }
            },
            dismissButton = { TextButton(onClick = { excusing = null }) { Text("رجوع") } }
        )
    }

    removing?.let { s ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("إزالة ${s.str("name")}؟") },
            text = { Text("يصله تنبيه بإلغاء تكليفه إن كان قد أُبلغ به.") },
            confirmButton = { Button(onClick = { removing = null; act("تعذر الإزالة") { OnlineApi.removeStaff(committee.id, s.str("id")) } }) { Text("إزالة") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("رجوع") } }
        )
    }

    adding?.let { role ->
        AddStaffDialog(committee, role, onDone = { adding = null; reload++ }, onDismiss = { adding = null })
    }

    if (editingDriver) {
        val driver = d.obj("driver")
        var has by remember { mutableStateOf(driver != null) }
        var name by remember { mutableStateOf(driver?.str("name").orEmpty()) }
        var phone by remember { mutableStateOf(driver?.str("phone").orEmpty()) }
        AlertDialog(
            onDismissRequest = { editingDriver = false },
            title = { Text("سائق اللجنة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = has, onCheckedChange = { has = it })
                        Text("يوجد سائق")
                    }
                    if (has) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("الرقم") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = { Button(onClick = { editingDriver = false; act("تعذر الحفظ") { OnlineApi.setDriver(committee.id, has, name, phone) } }) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { editingDriver = false }) { Text("رجوع") } }
        )
    }

    if (editingCeremony) {
        val c = committee.ceremonyDetails
        var date by remember { mutableStateOf(c.date) }
        var time by remember { mutableStateOf(c.time) }
        var place by remember { mutableStateOf(c.venue) }
        AlertDialog(
            onDismissRequest = { editingCeremony = false },
            title = { Text("الحفلة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("التاريخ (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("الوقت") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = place, onValueChange = { place = it }, label = { Text("المكان") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { Button(onClick = { editingCeremony = false; act("تعذر حفظ الحفلة") { OnlineApi.saveCeremony(committee.id, date, time, place) } }) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { editingCeremony = false }) { Text("رجوع") } }
        )
    }
}

@Composable
private fun PersonLine(name: String, phone: String) {
    Text(if (phone.isBlank()) name else "$name · $phone", fontWeight = FontWeight.SemiBold)
}

@Composable
private fun StaffLine(s: Map<String, Any?>, canRemove: Boolean, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            PersonLine(s.str("name"), s.str("phone"))
            if (s.str("note").isNotBlank()) Text("ملاحظة: ${s.str("note")}", style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
            if (s["notified"] == false) Text("لم يُبلّغ بعد", style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
        }
        if (canRemove) TextButton(onClick = onRemove, modifier = Modifier.heightIn(min = 48.dp)) { Text("إزالة") }
    }
}

/** Pick a person (with what keeps them busy that day), then add; a same-day conflict asks to confirm, with a note. */
@Composable
private fun AddStaffDialog(committee: Committee, role: String, onDone: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var people by remember { mutableStateOf<List<Triple<String, String, String>>?>(null) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var conflict by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(role) {
        people = try {
            if (Permissions.can("people.availability")) {
                val day = (if (role == "ceremony_sheikh") committee.ceremonyDetails.date else committee.examDate)
                    ?.takeIf { it.length >= 10 }?.take(10)
                    ?: LocalDate.now(ZoneId.of("Africa/Cairo")).toString()
                val a = OnlineApi.availability(day)
                val free = a.objs("available").map { Triple(it.str("id"), it.str("name"), "") }
                val busy = a.objs("busy").map { p ->
                    Triple(p.str("id"), p.str("name"), p.objs("items").joinToString("، ") { it.str("label") })
                }
                (free + busy).sortedBy { it.second }
            } else {
                OnlineApi.ceremonyCandidates().filter { it.role == "sheikh" || it.role == "committee_head" }.map { Triple(it.id, it.name, "") }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiMessages.show(failure(e, "تعذر تحميل الأشخاص"))
            emptyList()
        }
    }

    fun submit(confirm: Boolean) {
        val id = chosen ?: return
        saving = true
        scope.launch {
            try {
                OnlineApi.addStaff(committee.id, id, role, note, confirm)
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val f = e as? OnlineApi.ApiFailure
                if (f?.errorCode == "schedule_conflict") conflict = f.message ?: "هذا الشخص لديه مهمة أخرى في نفس اليوم."
                else UiMessages.show(failure(e, "تعذر التعيين"))
            } finally {
                saving = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (role == "observer") "إضافة مراقب" else "تعيين شيخ للحفلة") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val list = people
                if (list == null) Text("جارٍ التحميل…")
                list?.forEach { (id, name, busy) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { chosen = id; conflict = null }) {
                        RadioButton(selected = chosen == id, onClick = { chosen = id; conflict = null })
                        Column {
                            Text(name)
                            if (busy.isNotBlank()) Text("مشغول: $busy", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB45309))
                        }
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("ملاحظة له (اختياري)") }, modifier = Modifier.fillMaxWidth())
                conflict?.let { Text("تنبيه: $it", color = Color(0xFFB45309), fontWeight = FontWeight.Bold) }
            }
        },
        confirmButton = {
            Button(enabled = chosen != null && !saving, onClick = { submit(conflict != null) }) {
                Text(if (conflict != null) "تأكيد رغم التعارض" else "تعيين")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("رجوع") } }
    )
}
