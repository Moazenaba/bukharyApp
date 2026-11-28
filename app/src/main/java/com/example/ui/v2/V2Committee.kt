package com.example.ui.v2

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Chat
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
import com.example.domain.v2.V2Tools
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

/** The server action behind each stage (server/workflow.ts STAGE_ACTION). */
private fun stageRight(stage: String): String = when (stage) {
    "registration", "exam" -> "committees.reception"
    "scoring", "aggregation", "review" -> "committees.scoring"
    else -> "committees.finalize"
}

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
    val confirmed = rememberV2Confirm()
    // An admin changes the committee's stage from here: tap the current stage to complete it, or the last done one to reopen it.
    var stageToChange by remember { mutableStateOf<String?>(null) }
    val head = Logistics.headOf(c)
    val place = Logistics.placeOf(c)
    val mineAsObserver = Permissions.observingOn(c.id)

    V2Scroll {
        item(key = "title") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(c.committeeName, style = V2Type.heading, color = p.ink)
                val about = listOf(c.competitionBook, c.courseName).filter { it.isNotBlank() && it != c.committeeName }.distinct().joinToString(" · ")
                if (about.isNotBlank()) Text(about, style = V2Type.small, color = p.muted)
                Text("المرحلة ${minOf(step.done + 1, step.total)} من ${step.total}", style = V2Type.label, color = p.muted)
                V2Steps(step.done, step.total)
            }
        }

        // 1. What this committee needs now
        item(key = "step") {
            when {
                step.kind == StepKind.DONE -> V2Group { V2Row("اكتملت كل مراحل اللجنة", mark = { V2Mark(icon = Icons.Default.CheckCircle) }) }
                step.canDo -> V2NowCard("الخطوة الجاية", step.title) {
                    V2Button(
                        if (step.oneTap) "تم، أكّد" else "افتح النموذج",
                        { if (step.oneTap) confirm = true else onLegacy(formTabFor(step.kind)) },
                        Modifier.fillMaxWidth(),
                        kind = V2ButtonKind.CTA
                    )
                }
                // Nothing to do here yet: the most useful thing is getting there, so the hero offers the map.
                else -> V2NowCard("", step.waiting.ifBlank { "اللجنة منتظرة" }) {
                    Logistics.mapsUrl(place)?.let { url ->
                        V2Button("افتح في الخرائط", { V2Intents.open(context, url) }, Modifier.fillMaxWidth(), kind = V2ButtonKind.CTA, icon = Icons.Default.Place)
                    }
                }
            }
        }

        // 1b. The ceremony decision of a temporary head (only when such a grant is on this committee)
        item(key = "decision") { V2CeremonyDecision(c) }

        // 2. Where to go and whom to call
        item(key = "where-t") { V2Section("إلى أين؟") }
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
                // A waiting committee already leads with the map in its top card: it is not offered twice.
                Logistics.mapsUrl(place)?.takeIf { step.canDo || step.kind == StepKind.DONE }?.let { url ->
                    Row(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
                        V2Button("افتح في الخرائط", { V2Intents.open(context, url) }, Modifier.fillMaxWidth(), kind = V2ButtonKind.SOLID, icon = Icons.Default.Place)
                    }
                }
                V2Divider()
                V2Row(
                    title = head?.name ?: "لم يُسند شيخ اللجنة بعد",
                    subtitle = if (head == null) null else if (head.phone.isBlank()) "شيخ اللجنة · لم يُسجل له رقم" else "شيخ اللجنة",
                    mark = { V2Avatar(head?.name ?: "؟") }
                )
                if (head != null && head.phone.isNotBlank() && head.id != data.user.id) {
                    Row(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        V2Button("اتصال", { V2Intents.call(context, head.phone) }, Modifier.weight(1f), kind = V2ButtonKind.SOLID, icon = Icons.Default.Phone)
                        WhatsApp.link(head.phone, "السلام عليكم ورحمة الله")?.let { link ->
                            V2Button("واتساب", { V2Intents.open(context, link) }, Modifier.weight(1f), kind = V2ButtonKind.SOLID, icon = Icons.AutoMirrored.Filled.Chat)
                        }
                    }
                }
                if (c.sheikhName.isNotBlank()) {
                    V2Divider()
                    V2Row(c.sheikhName, listOf("الشيخ الميداني", v2Phone(c.sheikhPhone)).filter { it.isNotBlank() }.joinToString(" · "), mark = { V2Avatar(c.sheikhName) },
                        trailing = { if (c.sheikhPhone.isNotBlank()) V2CallButton(c.sheikhName) { V2Intents.call(context, c.sheikhPhone) } })
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

        // 5. The team: one section for everyone on the committee, each with their job, and one button to add a person
        //    (the server sends each person only what they may see). Until it answers, or without a connection, the
        //    committee's people the phone already has are listed.
        item(key = "team") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                V2Section("فريق اللجنة")
                com.example.ui.screens.committees.CommitteeFlowSection(c, fallback = {
                    V2Group {
                        val team = c.staff
                        if (team.isEmpty()) V2Row("لا يوجد أعضاء ظاهرون بعد")
                        team.forEachIndexed { i, m ->
                            if (i > 0) V2Divider()
                            V2Row(m.name, if (m.role == "committee_head") "شيخ اللجنة" else "مراقب", mark = { V2Avatar(m.name) })
                        }
                    }
                })
            }
        }

        // 7. The nine stages
        item(key = "stages-t") { V2Section("مراحل العمل (من الامتحان للحفل)") }
        item(key = "stages") {
            V2Group {
                val current = STAGE_ORDER.firstOrNull { !c.stages.getStage(it).completed }
                STAGE_ORDER.forEachIndexed { i, key ->
                    if (i > 0) V2Divider(inset = 56.dp)
                    val s = c.stages.getStage(key)
                    val now = key == current
                    val lastDone = s.completed && STAGE_ORDER.drop(i + 1).none { c.stages.getStage(it).completed }
                    val mayChange = V2Tools.isAdmin(data.user.role.roleKey) && Permissions.canOn(stageRight(key), c.id) && (now || lastDone)
                    V2Row(
                        V2Stages.LABEL[key] ?: key,
                        when {
                            s.completed -> listOfNotNull("تمت", s.completedBy?.takeIf { it.isNotBlank() }).joinToString(" · ")
                            now -> "المرحلة الحالية"
                            else -> null
                        },
                        onClick = if (mayChange) ({ stageToChange = key }) else null,
                        actionLabel = if (s.completed) "إعادة فتح المرحلة" else "إتمام المرحلة",
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
                V2Button("تعديل بيانات اللجنة (الواجهة الكاملة)", { onLegacy(EDIT_TAB) }, Modifier.fillMaxWidth(), kind = V2ButtonKind.SOLID, icon = Icons.Default.EditNote)
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(step.title, style = V2Type.heading) },
            text = { Text("لجنة ${c.committeeNumber}: تأكيد الخطوة؟", style = V2Type.body) },
            confirmButton = { TextButton(onClick = { confirm = false; actions.oneTap(c, step); confirmed(); UiMessages.show("تم تسجيل: ${step.title}") }) { Text("تأكيد", style = V2Type.bodyStrong) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("إلغاء", style = V2Type.body) } }
        )
    }
    stageToChange?.let { key ->
        val completed = c.stages.getStage(key).completed
        val label = V2Stages.LABEL[key] ?: key
        AlertDialog(
            onDismissRequest = { stageToChange = null },
            title = { Text(if (completed) "إعادة فتح «$label»؟" else "إتمام «$label»؟", style = V2Type.heading) },
            text = { Text(if (completed) "ترجع اللجنة لهذه المرحلة، ويتسجّل التغيير باسمك." else "تنتقل اللجنة للمرحلة التي بعدها، ويتسجّل التغيير باسمك.", style = V2Type.body) },
            confirmButton = {
                TextButton(onClick = { stageToChange = null; actions.setStage(c, key, !completed) }) {
                    Text(if (completed) "إعادة فتح" else "إتمام", style = V2Type.bodyStrong)
                }
            },
            dismissButton = { TextButton(onClick = { stageToChange = null }) { Text("إلغاء", style = V2Type.body) } }
        )
    }
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
            notes!!.isEmpty() -> V2Row("لا توجد ملاحظات بعد", "اكتب أول ملاحظة أدناه.")
            else -> notes!!.forEachIndexed { i, n ->
                if (i > 0) V2Divider()
                V2Row(n.note, v2OpensText(n.at).removeSuffix(" (بتوقيت القاهرة)"))
            }
        }
        V2Divider()
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                text, { text = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, textStyle = V2Type.body,
                shape = RoundedCornerShape(16.dp), colors = v2FieldColors(),
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
            }, Modifier.fillMaxWidth(), enabled = text.isNotBlank() && !saving, busy = saving)
        }
    }
}

/**
 * «القرار في الحفل»: a temporary head (or the committee's head, or an admin) says whether a ceremony happens. The
 * server decides who may answer and never lets a lower decision overwrite a higher one; this only offers the choices.
 */
@Composable
private fun V2CeremonyDecision(c: Committee) {
    val p = LocalV2.current
    val scope = rememberCoroutineScope()
    var grants by remember(c.id) { mutableStateOf<List<com.example.data.model.HeadGrant>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(c.id, reload) {
        // A grant that cannot be read simply shows nothing here; the committee itself still works offline.
        grants = try {
            OnlineApi.headGrants().filter { it.status == "active" && c.id in it.committeeIds }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }
    if (grants.isEmpty()) return
    val label = mapOf("yes" to "سيكون هناك حفل", "no" to "لن يكون هناك حفل", "undecided" to "لم يُحدد بعد")
    V2Section("القرار في الحفل")
    V2Group {
        grants.forEachIndexed { i, g ->
            if (i > 0) V2Divider()
            V2Row(
                g.userName,
                "شيخ اللجنة من ${g.startsOn} حتى ${g.endsOn} · ${label[g.ceremonyDecision] ?: ""}",
                mark = { V2Avatar(g.userName) }
            )
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("yes", "no", "undecided").forEach { d ->
                    val chosen = g.ceremonyDecision == d
                    V2Button(
                        label[d].orEmpty(),
                        {
                            scope.launch {
                                try {
                                    OnlineApi.decideHeadGrant(g.id, d)
                                    reload++
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    UiMessages.show("تعذر تسجيل القرار. حاول مرة أخرى.")
                                }
                            }
                        },
                        Modifier.fillMaxWidth(),
                        kind = if (chosen) V2ButtonKind.CTA else V2ButtonKind.SOLID,
                        icon = if (chosen) Icons.Default.CheckCircle else null
                    )
                }
            }
        }
    }
}

