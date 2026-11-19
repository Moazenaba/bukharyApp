package com.example.ui.v2

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.AssignmentNote
import com.example.data.model.Committee
import com.example.data.model.CommitteeAssignment
import com.example.data.remote.OnlineApi
import com.example.data.remote.Permissions
import com.example.domain.AssignmentText
import com.example.domain.DayText
import com.example.domain.Logistics
import com.example.domain.NextStep
import com.example.domain.STAGE_ORDER
import com.example.domain.StepKind
import com.example.domain.v2.V2Stages
import com.example.domain.v2.V2Tool
import com.example.ui.components.UiMessages
import com.example.ui.components.WhatsApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Which classic form tab a step needs (the long forms stay there in this first version). */
private fun formTabFor(kind: StepKind): Int = when (kind) {
    StepKind.SCORING, StepKind.AGGREGATION -> 2
    StepKind.SHEET -> 5
    StepKind.CEREMONY -> 3
    else -> 0
}

private const val EDIT_TAB = 4

private fun failure(e: Exception, fallback: String) = (e as? OnlineApi.ApiFailure)?.message ?: fallback

/**
 * One committee, top to bottom: what to do now, where to go and whom to call, the paper, the observer's notes,
 * the team, the observers, and the nine stages.
 */
@Composable
fun V2CommitteeScreen(c: Committee, data: V2Data, actions: V2Actions, tools: Set<V2Tool>, onLegacy: (Int) -> Unit) {
    val p = LocalV2.current
    val context = LocalContext.current
    val step = NextStep.of(c) { a -> Permissions.canOn(a, c.id) }
    var confirm by remember { mutableStateOf(false) }
    var editingStaff by remember { mutableStateOf(false) }
    val head = Logistics.headOf(c)
    val place = Logistics.placeOf(c)
    val mineAsObserver = Permissions.observingOn(c.id)

    V2Scroll {
        item(key = "title") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(c.committeeName, style = V2Type.heading, color = p.ink)
                val about = listOf(c.competitionBook, c.courseName).filter { it.isNotBlank() && it != c.committeeName }.distinct().joinToString(" · ")
                if (about.isNotBlank()) Text(about, style = V2Type.small, color = p.muted)
                V2Steps(step.done, step.total)
            }
        }

        // 1. What this committee needs now
        item(key = "step") {
            when {
                step.kind == StepKind.DONE -> V2Group { V2Row("اكتملت كل مراحل اللجنة", mark = { V2Mark(icon = Icons.Default.CheckCircle) }) }
                step.canDo -> V2NowCard("الخطوة الجاية في اللجنة", step.title) {
                    V2Button(
                        if (step.oneTap) step.title else "افتح النموذج",
                        { if (step.oneTap) confirm = true else onLegacy(formTabFor(step.kind)) },
                        Modifier.fillMaxWidth(),
                        kind = V2ButtonKind.CTA
                    )
                }
                else -> V2Group { V2Row("اللجنة منتظرة", step.waiting, mark = { V2Mark(icon = Icons.Default.HourglassEmpty) }) }
            }
        }

        // 2. Where to go and whom to call
        item(key = "where-t") { V2Section("رايح فين؟") }
        item(key = "where") {
            V2Group {
                V2Row(
                    title = place.ifBlank { "المكان لم يُحدد بعد" },
                    subtitle = listOfNotNull(
                        c.examDate?.takeIf { it.isNotBlank() }?.let { "الامتحان ${DayText.relative(it.take(10), data.today)}" },
                        Logistics.studentsText(c.studentsCount)
                    ).joinToString(" · "),
                    mark = { V2Mark(icon = Icons.Default.Place) }
                )
                Logistics.mapsUrl(place)?.let { url ->
                    Row(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
                        V2Button("افتح في الخرائط", { V2Intents.open(context, url) }, Modifier.fillMaxWidth(), kind = V2ButtonKind.QUIET, icon = Icons.Default.Place)
                    }
                }
                V2Divider()
                V2Row(
                    title = head?.name ?: "لسه ما اتسندش رئيس لجنة",
                    subtitle = if (head == null) null else if (head.phone.isBlank()) "رئيس اللجنة · لم يُسجل له رقم" else "رئيس اللجنة",
                    mark = { V2Avatar(head?.name ?: "؟") }
                )
                if (head != null && head.phone.isNotBlank() && head.id != data.user.id) {
                    Row(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        V2Button("اتصال", { V2Intents.call(context, head.phone) }, Modifier.weight(1f), kind = V2ButtonKind.SOLID, icon = Icons.Default.Phone)
                        WhatsApp.link(head.phone, "السلام عليكم ورحمة الله")?.let { link ->
                            V2Button("واتساب", { V2Intents.open(context, link) }, Modifier.weight(1f), kind = V2ButtonKind.QUIET, icon = Icons.Default.Chat)
                        }
                    }
                }
                if (c.sheikhName.isNotBlank()) {
                    V2Divider()
                    V2Row(c.sheikhName, listOf("الشيخ الميداني", c.sheikhPhone).filter { it.isNotBlank() }.joinToString(" · "), mark = { V2Avatar(c.sheikhName) },
                        onClick = if (c.sheikhPhone.isNotBlank()) ({ V2Intents.call(context, c.sheikhPhone) }) else null)
                }
            }
        }

        // 3. The exam paper of this committee's book
        val paper = data.exams.firstOrNull { it.hasFile && it.book.trim() == c.competitionBook.trim() }
        if (V2Tool.EXAM_PAPER in tools && paper != null) {
            item(key = "paper") {
                val locked = !paper.lockedUntil.isNullOrBlank()
                V2Group {
                    V2Row(
                        "ورقة الامتحان",
                        if (locked) "تُتاح ${v2OpensText(paper.lockedUntil!!)}" else paper.pdfFileName.ifBlank { paper.title },
                        mark = { V2Mark(icon = Icons.Default.Description) },
                        trailing = { if (locked) V2Tag("مقفولة", V2Tone.WAIT) else V2Tag("فتح", V2Tone.GO) },
                        onClick = if (locked) null else ({ actions.openExam(paper) })
                    )
                }
            }
        }

        // 4. Sent here as an observer
        if (mineAsObserver.isNotEmpty()) {
            item(key = "obs") { V2ObserverNotes(c, mineAsObserver.map { it.id to it.task }) }
        }

        // 5. Team
        item(key = "team-t") { V2Section("فريق اللجنة") }
        item(key = "team") {
            V2Group {
                if (c.staff.isEmpty()) V2Row("لسه مفيش حد في اللجنة")
                c.staff.forEachIndexed { i, s ->
                    if (i > 0) V2Divider()
                    V2Row(s.name, if (s.role == "committee_head") "رئيس اللجنة" else "شيخ", mark = { V2Avatar(s.name) })
                }
                if (V2Tool.STAFF in tools && Permissions.canOn("committees.members", c.id)) {
                    V2Divider()
                    Row(Modifier.padding(10.dp)) {
                        V2Button("إضافة / إزالة شيوخ", { actions.loadCandidates(); editingStaff = true }, Modifier.fillMaxWidth(), kind = V2ButtonKind.QUIET)
                    }
                }
            }
        }

        // 6. Observers sent to this committee
        if (V2Tool.OBSERVERS in tools && Permissions.canOn("assignments.manage", c.id)) {
            item(key = "observers") { V2ObserversSection(c, data, actions) }
        }

        // 7. The nine stages
        item(key = "stages-t") { V2Section("المراحل") }
        item(key = "stages") {
            V2Group {
                val current = STAGE_ORDER.firstOrNull { !c.stages.getStage(it).completed }
                STAGE_ORDER.forEachIndexed { i, key ->
                    if (i > 0) V2Divider(inset = 56.dp)
                    val s = c.stages.getStage(key)
                    val now = key == current
                    V2Row(
                        V2Stages.LABEL[key] ?: key,
                        when {
                            s.completed -> listOfNotNull("تمت", s.completedBy?.takeIf { it.isNotBlank() }).joinToString(" · ")
                            now -> "المرحلة الحالية"
                            else -> null
                        },
                        mark = {
                            // Done: a green check. Now: an accent dot. Ahead: a faint ring. The words above say the same.
                            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                when {
                                    s.completed -> Icon(Icons.Default.CheckCircle, null, tint = p.leaf, modifier = Modifier.size(24.dp))
                                    now -> Box(Modifier.size(14.dp).clip(CircleShape).background(p.cta))
                                    else -> Box(Modifier.size(14.dp).clip(CircleShape).border(2.dp, p.mist, CircleShape))
                                }
                            }
                        }
                    )
                }
            }
        }

        if (Permissions.canOn("committees.update", c.id)) {
            item(key = "full") {
                V2Button("تعديل بيانات اللجنة (الواجهة الكاملة)", { onLegacy(EDIT_TAB) }, Modifier.fillMaxWidth(), kind = V2ButtonKind.QUIET, icon = Icons.Default.EditNote)
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(step.title, style = V2Type.heading) },
            text = { Text("لجنة ${c.committeeNumber}: تأكيد الخطوة؟", style = V2Type.body) },
            confirmButton = { TextButton(onClick = { confirm = false; actions.oneTap(c, step) }) { Text("تأكيد", style = V2Type.bodyStrong) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("إلغاء", style = V2Type.body) } }
        )
    }
    if (editingStaff) V2StaffDialog(c, data, onDismiss = { editingStaff = false }) { ids -> actions.saveStaff(c, ids); editingStaff = false }
}

/** For the person sent here as «مراقب»: what they were sent to do, their notes, and a box for a new one. */
@Composable
private fun V2ObserverNotes(c: Committee, mine: List<Pair<String, String>>) {
    val p = LocalV2.current
    val scope = rememberCoroutineScope()
    val assignmentId = mine.first().first
    var notes by remember(assignmentId) { mutableStateOf<List<AssignmentNote>?>(null) }
    var failed by remember(assignmentId) { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(assignmentId, reload) {
        failed = false
        notes = try { OnlineApi.observations(assignmentId) } catch (e: CancellationException) { throw e } catch (e: Exception) { failed = true; null }
    }
    V2Group {
        V2Row(AssignmentText.mine(mine.map { it.second }), "صلاحيتك هنا للقراءة وكتابة الملاحظات، وتنتهي بانتهاء التكليف.", mark = { V2Mark(icon = Icons.Default.Visibility) })
        V2Divider()
        when {
            failed -> V2Retry("الملاحظات") { reload++ }
            notes == null -> V2Row("جارٍ تحميل الملاحظات…")
            notes!!.isEmpty() -> V2Row("لسه مفيش ملاحظات", "اكتب أول ملاحظة تحت.")
            else -> notes!!.forEachIndexed { i, n ->
                if (i > 0) V2Divider()
                V2Row(n.note, v2OpensText(n.at).removeSuffix(" (بتوقيت القاهرة)"))
            }
        }
        V2Divider()
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                text, { text = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, textStyle = V2Type.body,
                label = { Text("ملاحظة المراقبة") }
            )
            V2Button("حفظ الملاحظة", {
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
            }, Modifier.fillMaxWidth(), enabled = text.isNotBlank() && !saving)
        }
    }
}

/** For whoever may assign: the people sent to observe this committee, add one, end or cancel. */
@Composable
private fun V2ObserversSection(c: Committee, data: V2Data, actions: V2Actions) {
    val p = LocalV2.current
    val scope = rememberCoroutineScope()
    var list by remember(c.id) { mutableStateOf<List<CommitteeAssignment>?>(null) }
    var failed by remember(c.id) { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf<Pair<CommitteeAssignment, String>?>(null) }
    LaunchedEffect(c.id, reload) {
        failed = false
        list = try { OnlineApi.assignments(committeeId = c.id) } catch (e: CancellationException) { throw e } catch (e: Exception) { failed = true; null }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        V2Section("المراقبون")
        V2Group {
            val rows = list
            when {
                failed -> V2Retry("المراقبين") { reload++ }
                rows == null -> V2Row("جارٍ التحميل…")
                rows.isEmpty() -> V2Row("لا يوجد مراقبون مكلّفون", "المراقب يفضل على رتبته ويشوف اللجنة للقراءة فقط.")
                else -> rows.forEachIndexed { i, a ->
                    if (i > 0) V2Divider()
                    V2Row(
                        a.userName,
                        "${AssignmentText.task(a.task)} · ${AssignmentText.dates(a.startsOn, a.endsOn)} · أسنده ${a.assignedBy}",
                        mark = { V2Avatar(a.userName) },
                        trailing = { V2Tag(AssignmentText.status(a.status), if (a.status == "active") V2Tone.WATCH else V2Tone.WAIT) },
                        onClick = if (a.status == "active") ({ finishing = a to "end" }) else null
                    )
                }
            }
            V2Divider()
            Row(Modifier.padding(10.dp)) {
                V2Button("تكليف مراقب", { actions.loadCandidates(); adding = true }, Modifier.fillMaxWidth(), kind = V2ButtonKind.QUIET, icon = Icons.Default.Visibility)
            }
        }
    }

    finishing?.let { (a, _) ->
        AlertDialog(
            onDismissRequest = { finishing = null },
            title = { Text("تكليف ${a.userName}", style = V2Type.heading) },
            text = { Text("${AssignmentText.task(a.task)}. الإنهاء أو الإلغاء يوقف صلاحيات المراقبة فورًا ويبقى السجل.", style = V2Type.body) },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        finishing = null
                        scope.launch {
                            try { OnlineApi.finishAssignment(a.id, "end"); reload++ } catch (e: CancellationException) { throw e } catch (e: Exception) { UiMessages.show(failure(e, "تعذر إنهاء التكليف")) }
                        }
                    }) { Text("إنهاء", style = V2Type.bodyStrong) }
                    TextButton(onClick = {
                        finishing = null
                        scope.launch {
                            try { OnlineApi.finishAssignment(a.id, "cancel"); reload++ } catch (e: CancellationException) { throw e } catch (e: Exception) { UiMessages.show(failure(e, "تعذر إلغاء التكليف")) }
                        }
                    }) { Text("إلغاء التكليف", style = V2Type.bodyStrong, color = p.brick) }
                }
            },
            dismissButton = { TextButton(onClick = { finishing = null }) { Text("رجوع", style = V2Type.body) } }
        )
    }

    if (adding) {
        var picked by remember { mutableStateOf<String?>(null) }
        var task by remember { mutableStateOf("exam") }
        var endsOn by remember { mutableStateOf("") }
        var search by remember { mutableStateOf("") }
        var saving by remember { mutableStateOf(false) }
        val onCommittee = c.staff.map { it.id }.toSet()
        val people = data.candidates?.filter { (it.role == "sheikh" || it.role == "committee_head") && it.id !in onCommittee && it.id != data.user.id }
        AlertDialog(
            onDismissRequest = { if (!saving) adding = false },
            title = { Text("تكليف مراقب: لجنة ${c.committeeNumber}", style = V2Type.heading) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            V2Chip(AssignmentText.task("exam"), task == "exam") { task = "exam" }
                            V2Chip(AssignmentText.task("ceremony"), task == "ceremony") { task = "ceremony" }
                        }
                    }
                    item {
                        // When the observing ends: picked, never typed (no date format to get wrong).
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("ينتهي", style = V2Type.smallStrong, color = p.muted)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("" to "مفتوح", v2PlusDays(data.today, 7) to "أسبوع", v2PlusDays(data.today, 30) to "شهر").forEach { (d, label) ->
                                    V2Chip(label, endsOn == d) { endsOn = d }
                                }
                            }
                        }
                    }
                    if ((people?.size ?: 0) > 8) item {
                        OutlinedTextField(search, { search = it }, singleLine = true, textStyle = V2Type.body, shape = RoundedCornerShape(50),
                            colors = v2FieldColors(), placeholder = { Text("ابحث بالاسم", style = V2Type.body) }, modifier = Modifier.fillMaxWidth())
                    }
                    if (people == null) item { Text("جارٍ تحميل الأسماء…", style = V2Type.body, color = p.muted) }
                    items(people.orEmpty().filter { search.isBlank() || it.name.contains(search.trim()) }, key = { it.id }) { person ->
                        val on = picked == person.id
                        V2Row(person.name, roleName(person.role),
                            mark = { Icon(if (on) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (on) p.leaf else p.muted) },
                            onClick = { picked = person.id })
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = picked != null && !saving, onClick = {
                    val who = picked ?: return@TextButton
                    saving = true
                    scope.launch {
                        try {
                            OnlineApi.createAssignment(who, c.id, task, endsOn.trim().ifEmpty { null })
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
                }) { Text("تكليف", style = V2Type.bodyStrong) }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { adding = false }) { Text("إلغاء", style = V2Type.body) } }
        )
    }
}
