package com.example.ui.v2

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import com.example.ui.components.UiMessages
import kotlinx.coroutines.delay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Groups
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.data.model.Committee
import com.example.data.remote.AppSettings
import com.example.data.remote.Permissions
import com.example.data.remote.StaffUserDto
import com.example.domain.NoticeAudience
import com.example.domain.NoticeTo
import com.example.domain.DayText
import com.example.domain.FeedKind
import com.example.domain.Logistics
import com.example.domain.NextStep
import com.example.domain.StepKind
import com.example.domain.TodayFeedBuilder
import com.example.domain.v2.V2Stages
import com.example.domain.v2.V2Tool
import com.example.ui.AppNavScreen
import com.example.ui.components.Overdue

/** Every V2 page scrolls the same way: one column, 16dp sides, 14dp between blocks. */
@Composable
fun V2Scroll(content: LazyListScope.() -> Unit) {
    // On a tablet the column stays phone-wide and centred, instead of stretching rows across the screen.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.fillMaxHeight().widthIn(max = 640.dp).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp),
            content = { Spaced(this).content() }
        )
    }
}

/**
 * Each block of a screen (`item`) sits [V2Gap] below the one before; rows of a long list (`items`, see [v2Rows])
 * touch, so together they read as one sheet.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private class Spaced(private val inner: LazyListScope) : LazyListScope by inner {
    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        inner.item(key, contentType) {
            val scope = this
            Box(Modifier.padding(top = V2Gap)) { scope.content() }
        }
    }
}

private fun canOn(action: String, committeeId: String) = Permissions.canOn(action, committeeId)

private fun isLate(c: Committee, late: Set<String>) = c.id in late

private fun lateIds(data: V2Data): Map<String, Overdue.Item> =
    Overdue.committees(data.committees, AppSettings.current.overdueDays, data.tasks, data.today).associateBy { it.committee.id }

/** The row of a committee in any list: number, name, place and its stage. */
@Composable
fun V2CommitteeRow(c: Committee, late: Overdue.Item?, onOpen: () -> Unit, showSteps: Boolean = false, today: String = "") {
    val observer = Permissions.observingOn(c.id).isNotEmpty() && !Permissions.inScope(c.id)
    val place = Logistics.placeOf(c)
    val exam = c.examDate?.take(10)?.takeIf { it.isNotBlank() && !c.stages.getStage("exam").completed }
    val sub = listOfNotNull(exam?.let { "الامتحان ${DayText.relative(it, today)}" }, place.ifBlank { null }).joinToString(" · ")
    V2Row(
        title = c.committeeName,
        subtitle = sub,
        mark = { V2Mark(c.committeeNumber.toString(), late = late != null) },
        // The state sits under the text, so a place name is never broken to make room for it.
        onClick = onOpen,
        under = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Spacer(Modifier.height(2.dp))
                when {
                    observer -> V2Tag("مراقب", V2Tone.WATCH)
                    late != null -> V2Tag("متأخرة", V2Tone.LATE)
                    V2Stages.current(c) == null -> V2Tag("اكتملت", V2Tone.DONE)
                    else -> V2Tag(V2Stages.label(c), V2Tone.WAIT)
                }
                if (showSteps) V2Steps(com.example.domain.STAGE_ORDER.count { c.stages.getStage(it).completed })
            }
        }
    )
}

// ------------------------------------------------------------------------------------------- الآن

@Composable
fun V2NowScreen(data: V2Data, actions: V2Actions, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val feed = remember(data.committees, data.tasks, data.today) {
        TodayFeedBuilder.build(data.committees, data.tasks, data.user.id, ::canOn, data.today)
    }
    val late = remember(data.committees, data.tasks, data.today) { lateIds(data) }
    // The hero is what needs me most among what is due (late, today, or a step with no day): a step I can do,
    // then an exam, then a ceremony, then a task — late before on time. Everything else is «قادم».
    val (hero, rest) = remember(feed) {
        val all = listOfNotNull(feed.now) + feed.later
        val weight = mapOf(FeedKind.ACTION to 0, FeedKind.EXAM to 1, FeedKind.CEREMONY to 2, FeedKind.TASK to 3)
        val due = all.filter { it.overdue || it.today || it.date.isBlank() }
        val pick = due.minWithOrNull(compareBy<com.example.domain.FeedItem>({ weight[it.kind] }, { if (it.overdue) 0 else 1 })) ?: all.firstOrNull()
        pick to all.filter { it !== pick }
    }
    var confirmStep by remember { mutableStateOf(false) }
    val undo = LocalV2Undo.current
    val confirmed = rememberV2Confirm()
    // A first-time tip, shown once and remembered on the phone: what the big card is, and where the rest is.
    val prefs = remember { context.getSharedPreferences("bukhari_ui", android.content.Context.MODE_PRIVATE) }
    var tipSeen by remember { mutableStateOf(runCatching { prefs.getBoolean("v2_tip_now", false) }.getOrDefault(false)) }
    V2Scroll {
        if (!tipSeen && hero != null) item(key = "tip") {
            V2Group {
                V2Row(
                    "أول مرة هنا؟", "الكارت الكبير فيه الذي مطلوب منك الآن، وزرار واحد ملوّن لتنفيذه. باقي المواعيد تحته.",
                    mark = { V2Mark(icon = Icons.Default.Info) },
                    trailing = {
                        TextButton(onClick = { tipSeen = true; runCatching { prefs.edit().putBoolean("v2_tip_now", true).apply() } }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("تمام", style = V2Type.smallStrong, color = LocalV2.current.ink)
                        }
                    }
                )
            }
        }
        item(key = "now") {
            val now = hero
            if (now == null) {
                V2Empty("لا يوجد ما هو مطلوب منك الآن", "أول ما يُسند إليك امتحان أو حفل أو مهمة ستجهذا هنا.")
            } else {
                val committee = now.committeeId?.let { id -> data.committees.firstOrNull { it.id == id } }
                val step = committee?.let { NextStep.of(it) { a -> canOn(a, it.id) } }
                val whenText = listOf(DayText.relative(now.date, data.today)).filter { it.isNotBlank() }.joinToString(" · ")
                // Title says what to do; the line under it says which committee and when (no «#», no repeats).
                val title = when {
                    committee == null -> now.title
                    now.kind == FeedKind.ACTION -> now.title.replace(Regex("^لجنة #\\d+:\\s*"), "")
                    else -> now.title.replace("لجنة #", "لجنة ")
                }
                val meta = listOfNotNull(
                    committee?.takeIf { now.kind == FeedKind.ACTION }?.let { "لجنة ${it.committeeNumber}" },
                    committee?.committeeName?.takeIf { it.isNotBlank() && now.kind != FeedKind.ACTION },
                    whenText.ifBlank { null }
                ).joinToString(" · ")
                V2NowCard(meta = meta.ifBlank { "مطلوب منك" }, title = title, time = now.time) {
                    if (now.place.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Place, null, tint = LocalV2.current.onHero)
                            Text(now.place, style = V2Type.bodyStrong, color = LocalV2.current.onHero)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            now.kind == FeedKind.TASK && now.taskId != null ->
                                V2Button("تمت المهمة", {
                                    actions.toggleTask(now.taskId)
                                    confirmed()
                                    undo.offer("اتعلّمت «${now.title}» كمنجزة") { actions.toggleTask(now.taskId) }
                                }, Modifier.weight(1f), kind = V2ButtonKind.CTA, icon = Icons.Default.CheckCircle)
                            step != null && committee != null && step.canDo && step.oneTap && now.kind == FeedKind.ACTION ->
                                V2Button("تم، أكّد", { confirmStep = true }, Modifier.weight(1f), kind = V2ButtonKind.CTA, icon = Icons.Default.CheckCircle)
                            committee != null -> V2Button("افتح اللجنة", { onOpen(committee.id) }, Modifier.weight(1f), kind = V2ButtonKind.CTA)
                        }
                        Logistics.mapsUrl(now.place)?.let { url ->
                            V2Button("الخرائط", { V2Intents.open(context, url) }, Modifier.weight(1f), kind = V2ButtonKind.ON_HERO, icon = Icons.Default.Place)
                        }
                    }
                    // Never offer to call yourself: the head sees this card too.
                    if (now.headPhone.isNotBlank() && committee?.let { Logistics.headOf(it)?.id } != data.user.id) {
                        V2Button("اتصال برئيس اللجنة ${now.headName}".trim(), { V2Intents.call(context, now.headPhone) }, Modifier.fillMaxWidth(), kind = V2ButtonKind.ON_HERO, icon = Icons.Default.Phone)
                    }
                }
            }
        }
        // Due today (or already late) is said apart from what is coming, so «later» never hides «now».
        val (dueNow, later) = rest.partition { it.overdue || it.today || it.date.isBlank() }
        if (dueNow.isNotEmpty()) {
            item(key = "due-title") { V2Section("مطلوب اليوم") }
            v2Rows(dueNow, key = { "due-${it.key}" }) { f -> V2FeedRow(f, data, onOpen) }
        }
        if (later.isNotEmpty()) {
            item(key = "later-title") { V2Section("قادم") }
            v2Rows(later, key = { "later-${it.key}" }) { f -> V2FeedRow(f, data, onOpen) }
        }
        if (hero != null && rest.size < 4) item(key = "all-done") {
            Text("هذا كل الذي عليك الآن.", style = V2Type.small, color = LocalV2.current.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        }
        val shownAbove = (listOfNotNull(hero) + rest).mapNotNull { it.committeeId }.toSet()
        val mine = data.committees.filter { (Permissions.inScope(it.id) || Permissions.observingOn(it.id).isNotEmpty()) && it.id !in shownAbove }
        if (mine.isNotEmpty() && data.user.role.roleKey == "committee_head") {
            item(key = "mine-title") { V2Section("لجانك") }
            item(key = "mine") {
                V2Group {
                    mine.sortedBy { it.committeeNumber }.forEachIndexed { i, c ->
                        if (i > 0) V2Divider()
                        V2CommitteeRow(c, late[c.id], { onOpen(c.id) }, showSteps = true, today = data.today)
                    }
                }
            }
        }
    }
    // A stage step can't be taken back from here, so the hero asks first — the same as the committee screen.
    val stepCommittee = hero?.committeeId?.let { id -> data.committees.firstOrNull { it.id == id } }
    val heroStep = stepCommittee?.let { c -> NextStep.of(c) { a -> canOn(a, c.id) } }
    if (confirmStep && stepCommittee != null && heroStep != null) {
        AlertDialog(
            onDismissRequest = { confirmStep = false },
            title = { Text(heroStep.title, style = V2Type.heading) },
            text = { Text("لجنة ${stepCommittee.committeeNumber}. بعد التأكيد مش هتقدر ترجعها من هنا.", style = V2Type.body) },
            confirmButton = { TextButton(onClick = { confirmStep = false; actions.oneTap(stepCommittee, heroStep); confirmed(); UiMessages.show("تم تسجيل: ${heroStep.title}") }) { Text("تأكيد", style = V2Type.bodyStrong) } },
            dismissButton = { TextButton(onClick = { confirmStep = false }) { Text("إلغاء", style = V2Type.body) } }
        )
    }
}

// ------------------------------------------------------------------------------------------- لوحة الإدارة

@Composable
fun V2BoardScreen(data: V2Data, onOpen: (String) -> Unit, onShow: (V2Filter) -> Unit, onPeople: () -> Unit) {
    val p = LocalV2.current
    val late = remember(data.committees, data.tasks, data.today) { lateIds(data) }
    val counts = remember(data.committees) { V2Stages.counts(data.committees) }
    val examsToday = data.committees.count { it.examDate?.take(10) == data.today && !it.stages.getStage("exam").completed }
    val pending = data.staff.count { it.status == "pending" && it.role != "super_admin" }
    V2Scroll {
        item(key = "hero") {
            if (late.isNotEmpty()) {
                V2NowCard("مهلة كل مرحلة ${AppSettings.current.overdueDays} أيام", v2Count(late.size, "لجنة متأخرة", "لجنتين متأخرتين", "لجان متأخرة", "لجنة متأخرة")) {
                    V2Button("عرض المتأخر", { onShow(V2Filter.LATE) }, Modifier.fillMaxWidth(), kind = V2ButtonKind.CTA)
                }
            } else if (pending > 0) {
                // Nothing is late: the next thing that waits on the admin is people asking to join.
                V2NowCard("راجعهم واقبل أو اعتذر", v2Count(pending, "طلب انضمام مستني", "طلبين انضمام مستنيين", "طلبات انضمام مستنية", "طلب انضمام مستني")) {
                    V2Button("راجع الطلبات", onPeople, Modifier.fillMaxWidth(), kind = V2ButtonKind.CTA)
                }
            } else if (examsToday > 0) {
                V2NowCard("لا توجد لجان متأخرة", v2Count(examsToday, "امتحان اليوم", "امتحانان اليوم", "امتحانات اليوم", "امتحان اليوم")) {
                    V2Button("عرض امتحانات اليوم", { onShow(V2Filter.EXAM) }, Modifier.fillMaxWidth(), kind = V2ButtonKind.CTA)
                }
            } else {
                V2NowCard("كل اللجان تسير في مواعيدها", "لا يوجد ما ينتظرك")
            }
        }
        // Only what has something in it: a «0» row says nothing and opens nothing. The join request is not repeated
        // while the main card above already says «طلب انضمام مستني».
        val rows = buildList<Triple<String, Int, () -> Unit>> {
            if (late.isNotEmpty()) add(Triple("لجان متأخرة", late.size, { onShow(V2Filter.LATE) }))
            if (examsToday > 0) add(Triple("امتحانات اليوم", examsToday, { onShow(V2Filter.EXAM) }))
            if (pending > 0 && late.isNotEmpty()) add(Triple("طلبات انضمام", pending, { onPeople() }))
        }
        if (rows.isNotEmpty()) item(key = "stats") {
            V2Group {
                rows.forEachIndexed { i, (label, n, open) ->
                    if (i > 0) V2Divider(inset = 16.dp)
                    V2Row(label, onClick = open, actionLabel = "عرض",
                        trailing = { Text(n.toString(), style = V2Type.number, color = if (label == "لجان متأخرة") p.brick else p.muted) })
                }
            }
        }
        item(key = "stages-title") { V2Section("المراحل") }
        item(key = "stages") {
            V2Group {
                V2Stages.Group.values().filter { (counts[it] ?: 0) > 0 }.forEachIndexed { i, g ->
                    if (i > 0) V2Divider(inset = 16.dp)
                    val f = V2Filter.values().first { it.group() == g }
                    V2Row(
                        title = g.label, onClick = { onShow(f) }, actionLabel = "عرض اللجان",
                        trailing = { Text((counts[g] ?: 0).toString(), style = V2Type.number, color = p.muted) }
                    )
                }
            }
        }
        if (late.isNotEmpty()) {
            item(key = "late-title") { V2Section("الأكثر تأخرًا") }
            item(key = "late") {
                V2Group {
                    late.values.sortedByDescending { it.days }.take(5).forEachIndexed { i, item ->
                        if (i > 0) V2Divider()
                        V2Row(
                            title = item.committee.committeeName,
                            subtitle = item.reason,
                            mark = { V2Mark(item.committee.committeeNumber.toString(), late = true) },
                            trailing = { Text(DayText.daysWord(item.days), style = V2Type.smallStrong, color = p.brick) },
                            onClick = { onOpen(item.committee.id) }
                        )
                    }
                }
            }
        }
    }
}


// ------------------------------------------------------------------------------------------- القوائم

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun V2CommitteeListScreen(data: V2Data, onOpen: (String) -> Unit, filter: V2Filter, onFilter: (V2Filter) -> Unit, mineOnly: Boolean) {
    var query by rememberSaveable { mutableStateOf("") }
    val late = remember(data.committees, data.tasks, data.today) { lateIds(data) }
    val base = data.committees
    val shown = remember(base, query, filter, late, mineOnly, data.today) { base.filter { c ->
        V2Stages.matches(c, query) && when (filter) {
            V2Filter.ALL -> true
            V2Filter.LATE -> c.id in late
            else -> V2Stages.groupOf(c) == filter.group()
        }
    }.sortedWith(
        // A sheikh's own list: the nearest exam first. The full list: late first, then by number.
        if (mineOnly) compareBy<Committee> { it.examDate?.take(10)?.takeIf { d -> d.isNotBlank() && d >= data.today } ?: "9999" }.thenBy { it.committeeNumber }
        else compareByDescending<Committee> { it.id in late }.thenBy { it.committeeNumber }
    ) }
    V2Scroll {
        // Search and filters show for a long list — and always while a filter is on, so it can be seen and cleared.
        if (base.size > 6 || filter != V2Filter.ALL) {
            item(key = "search") {
                V2SearchField(query, { query = it }, "رقم اللجنة أو اسمها أو الشيخ")
            }
            item(key = "chips") {
                FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    V2Filter.values().forEach { f ->
                        val n = when (f) {
                            V2Filter.ALL -> base.size
                            V2Filter.LATE -> late.size
                            else -> base.count { V2Stages.groupOf(it) == f.group() }
                        }
                        if (f == V2Filter.ALL || n > 0) V2Chip("${f.label} $n", f == filter) { onFilter(f) }
                    }
                }
            }
        }
        if (shown.isEmpty()) item(key = "empty") {
            V2Empty(
                if (base.isEmpty()) (if (mineOnly) "لم تُسند إليك لجنة بعد" else "لا توجد لجان في هذه الدورة") else "لا توجد لجنة مطابقة للبحث",
                if (base.isEmpty()) "أول ما تتسند لك لجنة هيوصلك إشعار وتظهر هنا." else "جرّب رقم اللجنة أو امسح التصفية."
            )
        } else v2Rows(shown, key = { "c-${it.id}" }) { c -> V2CommitteeRow(c, late[c.id], { onOpen(c.id) }, today = data.today) }
    }
}

@Composable
fun V2CeremoniesScreen(data: V2Data, onOpen: (String) -> Unit) {
    val p = LocalV2.current
    val list = data.committees.filter { c ->
        c.ceremonyDetails.attendees.any { it.id == data.user.id } || Permissions.canOn("committees.finalize", c.id) ||
            Permissions.observingOn(c.id).any { it.task == "ceremony" }
    }.filter { it.ceremonyDetails.date.isNotBlank() || !it.stages.ceremony.completed }
        .sortedBy { it.ceremonyDetails.date.ifBlank { "9999" } }
    V2Scroll {
        item(key = "list") {
            if (list.isEmpty()) V2Empty("لا توجد حفلات لك بعد", "عند دعوتك لحفل سيصلك إشعار يظهر هنا بموعهذا ومكانه.")
            else V2Group {
                list.forEachIndexed { i, c ->
                    if (i > 0) V2Divider()
                    val d = c.ceremonyDetails
                    V2Row(
                        title = "حفل تكريم لجنة ${c.committeeNumber}",
                        subtitle = listOf(
                            if (d.date.isBlank()) "لم يحدد الموعد بعد" else DayText.relative(d.date, data.today),
                            d.time, Logistics.ceremonyPlaceOf(c)
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        mark = { V2Mark(icon = Icons.Default.Celebration) },
                        trailing = { if (c.stages.ceremony.completed) V2Tag("تم", V2Tone.DONE) else if (d.honoreesCount > 0) Text("${d.honoreesCount} مكرّم", style = V2Type.small, color = p.muted) else Unit },
                        onClick = { onOpen(c.id) }
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------- الفريق (رئيس اللجنة)

@Composable
fun V2TeamScreen(data: V2Data, actions: V2Actions, onOpen: (String) -> Unit) {
    val mine = data.committees.filter { Permissions.canOn("committees.members", it.id) }.sortedBy { it.committeeNumber }
    var editing by remember { mutableStateOf<Committee?>(null) }
    V2Scroll {
        if (mine.isEmpty()) item(key = "empty") { V2Empty("لا توجد لجان ترأسها", "عندما تُسند إليك الإدارة لجنة يمكنك إضافة شيوخها من هنا.") }
        mine.forEach { c ->
            item(key = "t-${c.id}") { V2Section("لجنة ${c.committeeNumber} · ${c.committeeName}") }
            item(key = "g-${c.id}") {
                V2Group {
                    if (c.staff.none { it.id != data.user.id }) V2Row(title = "لم يُضَف شيوخ إلى اللجنة بعد")
                    // The head is the reader: list the sheikhs only, then two plain rows for what can be done.
                    c.staff.filter { it.id != data.user.id }.forEachIndexed { i, s ->
                        if (i > 0) V2Divider()
                        V2Row(title = s.name, subtitle = if (s.role == "committee_head") "رئيس اللجنة" else "شيخ", mark = { V2Avatar(s.name) })
                    }
                    V2Divider()
                    V2Row("إضافة أو إزالة شيوخ", mark = { V2Mark(icon = Icons.Default.Groups) }, onClick = { actions.loadCandidates(); editing = c }, actionLabel = "تعديل الشيوخ")
                    V2Divider()
                    V2Row("المراقبون ومراحل اللجنة", mark = { V2Mark(icon = Icons.Default.Visibility) }, onClick = { onOpen(c.id) })
                }
            }
        }
    }
    editing?.let { c -> V2StaffDialog(c, data, onRetry = actions.loadCandidates, onDismiss = { editing = null }) { ids -> actions.saveStaff(c, ids); editing = null } }
}

@Composable
fun V2StaffDialog(c: Committee, data: V2Data, onRetry: () -> Unit, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    val p = LocalV2.current
    var picked by remember(c.id) { mutableStateOf(c.staff.map { it.id }.toSet()) }
    val people = data.candidates?.filter { (it.role == "sheikh" || it.role == "committee_head") && it.id != data.user.id }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("شيوخ لجنة ${c.committeeNumber}", style = V2Type.heading) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                if (people == null) item { V2LoadingNames(onRetry) }
                items(people.orEmpty(), key = { it.id }) { person ->
                    val on = person.id in picked
                    V2Row(
                        title = person.name,
                        subtitle = if (person.role == "committee_head") "رئيس لجنة" else "شيخ",
                        mark = { Icon(if (on) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (on) p.leaf else p.muted) },
                        onClick = { picked = if (on) picked - person.id else picked + person.id },
                        selected = on, multiple = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = people != null, onClick = {
                val allowed = people.orEmpty().map { it.id }.toSet()
                onSave(picked.filter { it in allowed })
            }) { Text("حفظ", style = V2Type.bodyStrong) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", style = V2Type.body) } }
    )
}

// ------------------------------------------------------------------------------------------- الناس (الإدارة)

@Composable
fun V2PeopleScreen(data: V2Data, actions: V2Actions, tools: Set<V2Tool>) {
    val context = LocalContext.current
    // Admins load the list once on entering V2 (V2Shell); a committee head loads it here.
    LaunchedEffect(Unit) { if (!com.example.domain.v2.V2Tools.isAdmin(data.user.role.roleKey)) actions.loadStaff() }
    var query by rememberSaveable { mutableStateOf("") }
    var rejecting by remember { mutableStateOf<StaffUserDto?>(null) }
    var approving by remember { mutableStateOf<StaffUserDto?>(null) }
    var approvingId by remember { mutableStateOf<String?>(null) }
    val requests = data.staff.filter { it.status == "pending" && it.role != "super_admin" }
    // Everyone the server sends, except people still waiting (they are above): approved, and also those who were declined or paused.
    val approved = data.staff.filter { it.status != "pending" }
        .filter { query.isBlank() || it.name.contains(query.trim()) || it.phone.contains(query.trim()) }
        .sortedWith(compareBy<StaffUserDto> { roleOrder(it.role) }.thenBy { it.name })
    V2Scroll {
        if (requests.isNotEmpty()) {
            item(key = "req-title") { V2Section("طلبات انضمام (${requests.size})") }
            item(key = "req") {
                V2Group {
                    requests.forEachIndexed { i, u ->
                        if (i > 0) V2Divider()
                        V2Row(title = u.name, subtitle = listOf(roleName(u.role), v2Phone(u.phone)).filter { it.isNotBlank() }.joinToString(" · "), mark = { V2Avatar(u.name) })
                        if (V2Tool.JOIN_REQUESTS in tools) {
                            Row(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                V2Button("قبول", { approving = u }, Modifier.weight(1f), kind = V2ButtonKind.CTA, icon = Icons.Default.CheckCircle, busy = approvingId == u.id)
                                V2Button("اعتذار", { rejecting = u }, Modifier.weight(1f), kind = V2ButtonKind.QUIET)
                            }
                        }
                    }
                }
            }
        }
        item(key = "search") {
            V2SearchField(query, { query = it }, "الاسم أو رقم الموبايل")
        }
        item(key = "users-title") { V2Section(if (data.staffLoading && data.staff.isEmpty()) "جارٍ التحميل…" else if (approved.isEmpty()) "المستخدمون" else "المستخدمون (${approved.size})") }
        if (approved.isEmpty() && !data.staffLoading) item(key = "users-empty") {
            V2Empty(if (query.isBlank()) "لا يوجد مستخدمون بعد" else "لا يوجد نتيجة للبحث", if (query.isBlank()) "أول من يُقبل سيظهر هنا." else "جرّب جزءاً من الاسم أو آخر أرقام الهاتف.")
        }
        run {
            v2Rows(approved, key = { "u-${it.id}" }) { u ->
                    val committees = if (u.allCommitteesAllowed) "كل اللجان" else if (u.assignedCommitteeIds.isEmpty()) "بدون لجان" else v2Count(u.assignedCommitteeIds.size, "لجنة واحدة", "لجنتين", "لجان", "لجنة")
                    V2Row(
                        title = u.name,
                        subtitle = listOfNotNull(roleName(u.role), committees, if (u.status == "approved") null else "غير مفعّل").joinToString(" · "),
                        mark = { V2Avatar(u.name) },
                        trailing = { if (u.phone.isNotBlank()) V2CallButton(u.name) { V2Intents.call(context, u.phone) } }
                    )
            }
        }
        item(key = "note") {
            V2Group {
                V2Row("تعديل الأدوار والصلاحيات", "بيفتح في الواجهة القديمة", mark = { V2Mark(icon = Icons.Default.Settings) }, onClick = actions.leaveV2, actionLabel = "فتح الواجهة القديمة")
            }
        }
    }
    // After approving, the button shows a spinner until the person leaves the list (or a few seconds pass).
    LaunchedEffect(approvingId) { if (approvingId != null) { delay(8000); approvingId = null } }
    approving?.let { u -> V2ApproveDialog(u, data, onDismiss = { approving = null }) { role, ids -> actions.approve(u, role, ids); approvingId = u.id; approving = null } }
    rejecting?.let { u ->
        AlertDialog(
            onDismissRequest = { rejecting = null },
            title = { Text("اعتذار عن طلب ${u.name}؟", style = V2Type.heading) },
            text = { Text("هيتسجّل الاعتذار ويقدر يقدّم طلب جديد بعدين.", style = V2Type.body) },
            confirmButton = { TextButton(onClick = { actions.reject(u); rejecting = null }) { Text("اعتذار", style = V2Type.bodyStrong) } },
            dismissButton = { TextButton(onClick = { rejecting = null }) { Text("رجوع", style = V2Type.body) } }
        )
    }
}

private fun roleOrder(role: String) = when (role) { "super_admin" -> 0; "admin" -> 1; "exams_manager" -> 2; "committee_head" -> 3; else -> 4 }
fun roleName(role: String) = when (role) { "super_admin" -> "مشرف عام"; "admin" -> "أدمن"; "exams_manager" -> "مدير الحفلات والامتحانات"; "committee_head" -> "رئيس لجنة"; else -> "شيخ" }

// ------------------------------------------------------------------------------------------- المزيد / حسابي

@Composable
fun V2MoreScreen(data: V2Data, actions: V2Actions, tools: Set<V2Tool>, onPage: (V2Page) -> Unit) {
    var sending by remember { mutableStateOf(false) }
    var choosingCourse by remember { mutableStateOf(false) }
    val unread = data.notifications.count { !it.read }
    val todays = data.tasks.count { it.date.take(10) == data.today && !it.completed }
    val course = data.courses.firstOrNull { it.id == data.selectedCourseId } ?: data.courses.firstOrNull { it.isDefault }
    V2Scroll {
        item(key = "work") {
            V2Group {
                V2Row("الإشعارات", if (unread > 0) v2Count(unread, "إشعار جديد", "إشعارين جداد", "إشعارات جديدة", "إشعار جديد") else "لا جديد", mark = { V2Mark(icon = Icons.Default.Notifications) }, onClick = { onPage(V2Page.INBOX) })
                if (V2Tool.EXAMS in tools || V2Tool.EXAM_PAPER in tools) {
                    V2Divider()
                    V2Row("الامتحانات", v2Count(data.exams.size, "ورقة واحدة", "ورقتين", "ورقات", "ورقة"), mark = { V2Mark(icon = Icons.Default.Description) }, onClick = { onPage(V2Page.EXAMS) })
                }
                V2Divider()
                V2Row("التقويم", if (todays > 0) v2Count(todays, "مهمة اليوم", "مهمتين اليوم", "مهام اليوم", "مهمة اليوم") else "المواعيد والمهام", mark = { V2Mark(icon = Icons.Default.CalendarMonth) }, onClick = { onPage(V2Page.CALENDAR) })
                if (V2Tool.SEND_NOTICE in tools) {
                    V2Divider()
                    V2Row("إرسال تنبيه", "للجميع، أو لفئة، أو لأشخاص محددين", mark = { V2Mark(icon = Icons.Default.Campaign) }, onClick = { sending = true })
                }
                if (data.courses.size > 1) {
                    V2Divider()
                    V2Row("الدورة المعروضة", course?.name.orEmpty(), mark = { V2Mark(icon = Icons.Default.EventRepeat) }, onClick = { choosingCourse = true })
                }
            }
        }
        if (V2Tool.AUDIT in tools || V2Tool.DATABASE in tools) {
            item(key = "admin") {
                V2Group {
                    if (V2Tool.AUDIT in tools) V2Row("سجل التدقيق", "يفتح في الواجهة الكاملة", mark = { V2Mark(icon = Icons.Default.History) }, onClick = { actions.openClassic(AppNavScreen.AUDIT) })
                    if (V2Tool.DATABASE in tools) {
                        if (V2Tool.AUDIT in tools) V2Divider()
                        V2Row("حالة قاعدة البيانات", "يفتح في الواجهة الكاملة", mark = { V2Mark(icon = Icons.Default.Storage) }, onClick = { actions.openClassic(AppNavScreen.DATABASE_INSPECTOR) })
                    }
                }
            }
        }
        item(key = "account") { V2AccountRows(data, actions) }
    }
    if (sending) V2NoticeDialog(data.staff, actions.loadStaff, onDismiss = { sending = false }) { t, b, audience, done -> actions.sendNoticeTo(t, b, audience) { ok -> done(ok); if (ok) sending = false } }
    if (choosingCourse) {
        AlertDialog(
            onDismissRequest = { choosingCourse = false },
            title = { Text("الدورة المعروضة", style = V2Type.heading) },
            text = {
                Column {
                    data.courses.forEach { c ->
                        V2Row(c.name, if (c.isDefault) "الحالية" else null, onClick = { actions.selectCourse(c.id); choosingCourse = false }, selected = c.id == course?.id,
                            mark = { Icon(if (c.id == course?.id) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null) })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingCourse = false }) { Text("إغلاق", style = V2Type.body) } }
        )
    }
}

@Composable
fun V2AccountScreen(data: V2Data, actions: V2Actions) {
    V2Scroll { item(key = "account") { V2AccountRows(data, actions) } }
}

@Composable
private fun V2AccountRows(data: V2Data, actions: V2Actions) {
    var choosingTheme by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    // A tap on sync shows a spinner for a moment, so it never looks like nothing happened.
    var syncing by remember { mutableStateOf(false) }
    // The spinner stops when the sync really ends; 15 seconds is only a safety net if the phone never answers.
    LaunchedEffect(syncing) { if (syncing) { delay(15000); syncing = false } }
    Column(verticalArrangement = Arrangement.spacedBy(V2Gap)) {
    V2Group {
        if (data.user.name.isBlank() || data.user.name.startsWith("مستخدم ")) V2Row(roleName(data.user.role.roleKey), v2Phone(data.user.phone).ifBlank { null }, mark = { V2Mark(icon = Icons.Default.Person) })
        else V2Row(data.user.name, listOf(roleName(data.user.role.roleKey), v2Phone(data.user.phone)).filter { it.isNotBlank() }.joinToString(" · "), mark = { V2Avatar(data.user.name) })
    }
    V2Group {
        val theme = com.example.domain.v2.V2Themes.ALL.firstOrNull { it.key == data.themeKey }
        V2Row("الألوان", theme?.name ?: "تلقائي (حسب الموبايل)", mark = { V2Mark(icon = Icons.Default.DarkMode) },
            trailing = { theme?.let { V2Swatch(it) } }, onClick = { choosingTheme = true })
        V2Divider()
        V2Row("مزامنة الآن", if (data.isOnline) null else "بدون اتصال", mark = { V2Mark(icon = Icons.Default.Sync) },
            trailing = { if (syncing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalV2.current.muted) },
            onClick = { if (!syncing) { syncing = true; actions.sync { syncing = false } } })
        V2Divider()
        V2Row("تغيير كلمة المرور", mark = { V2Mark(icon = Icons.Default.Lock) }, onClick = actions.changePassword)
        V2Divider()
        V2Row("الرجوع للواجهة القديمة", "وترجع هنا تاني من «الواجهات 2» في القائمة", mark = { V2Mark(icon = Icons.Default.SwapHoriz) }, onClick = actions.leaveV2)
    }
    V2Group {
        V2Row("تسجيل الخروج", mark = { V2Mark(icon = Icons.AutoMirrored.Filled.Logout, late = true) }, onClick = { leaving = true })
    }
    }
    if (leaving) {
        AlertDialog(
            onDismissRequest = { leaving = false },
            title = { Text("تسجيل الخروج؟", style = V2Type.heading) },
            text = { Text("هتحتاج رقم الموبايل وكلمة المرور لكي تدخل تاني.", style = V2Type.body) },
            confirmButton = { TextButton(onClick = { leaving = false; actions.logout() }) { Text("خروج", style = V2Type.bodyStrong, color = LocalV2.current.brick) } },
            dismissButton = { TextButton(onClick = { leaving = false }) { Text("رجوع", style = V2Type.body) } }
        )
    }
    if (choosingTheme) {
        AlertDialog(
            onDismissRequest = { choosingTheme = false },
            title = { Text("الألوان", style = V2Type.heading) },
            text = {
                LazyColumn {
                    item {
                        V2Row("تلقائي", "فاتح أو غامق حسب الموبايل", onClick = { actions.v2Theme(com.example.domain.v2.V2Themes.AUTO); choosingTheme = false }, selected = data.themeKey == com.example.domain.v2.V2Themes.AUTO,
                            mark = { Icon(if (data.themeKey == com.example.domain.v2.V2Themes.AUTO) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null) })
                    }
                    items(com.example.domain.v2.V2Themes.ALL, key = { it.key }) { t ->
                        V2Row(t.name, t.story, onClick = { actions.v2Theme(t.key); choosingTheme = false }, selected = t.key == data.themeKey,
                            mark = { Icon(if (t.key == data.themeKey) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null) },
                            trailing = { V2Swatch(t) })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingTheme = false }) { Text("إغلاق", style = V2Type.body) } }
        )
    }
}

/**
 * «إرسال تنبيه»: to everyone, to a category (observers, ceremony sheikhs, committee sheikhs), or to people picked
 * from the system (one or many). A message to everyone is sent on the second tap.
 */
@Composable
private fun V2NoticeDialog(
    staff: List<StaffUserDto>,
    onLoadStaff: () -> Unit,
    onDismiss: () -> Unit,
    onSend: (String, String, Map<String, Any?>, (Boolean) -> Unit) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var to by remember { mutableStateOf(NoticeTo.ALL) }
    var groups by remember { mutableStateOf(setOf<String>()) }
    var picked by remember { mutableStateOf(setOf<String>()) }
    var query by remember { mutableStateOf("") }
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (staff.isEmpty()) onLoadStaff() }
    val people = staff.filter { it.status == "approved" && (query.isBlank() || it.name.contains(query.trim()) || it.phone.contains(query.trim())) }.sortedBy { it.name }
    val audience = NoticeAudience.of(to, groups, picked)
    val heading = NoticeAudience.heading(to, picked.size)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading, style = V2Type.heading) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it; armed = false }, label = { Text("العنوان") }, singleLine = true, textStyle = V2Type.body, shape = RoundedCornerShape(16.dp), colors = v2FieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(body, { body = it; armed = false }, label = { Text("التفاصيل") }, minLines = 3, textStyle = V2Type.body, shape = RoundedCornerShape(16.dp), colors = v2FieldColors(), modifier = Modifier.fillMaxWidth())
                Text("إلى مين؟", style = V2Type.label, color = LocalV2.current.muted)
                V2Group {
                    NoticeTo.values().forEachIndexed { i, o ->
                        if (i > 0) V2Divider()
                        V2Row(o.label, onClick = { to = o; armed = false }, selected = to == o,
                            mark = { Icon(if (to == o) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null) })
                    }
                }
                when (to) {
                    NoticeTo.ALL -> Text("يوصل لكل المستخدمين المعتمدين.", style = V2Type.small, color = LocalV2.current.muted)
                    NoticeTo.GROUP -> V2Group {
                        NoticeAudience.GROUPS.forEachIndexed { i, (key, label) ->
                            if (i > 0) V2Divider()
                            V2Row(label, onClick = { groups = if (key in groups) groups - key else groups + key; armed = false }, selected = key in groups, multiple = true,
                                mark = { Icon(if (key in groups) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null) })
                        }
                    }
                    NoticeTo.PEOPLE -> {
                        V2SearchField(query, { query = it }, "الاسم أو رقم الموبايل")
                        if (staff.isEmpty()) Text("جارٍ تحميل المستخدمين…", style = V2Type.small, color = LocalV2.current.muted)
                        else if (people.isEmpty()) Text("لا يوجد نتيجة للبحث.", style = V2Type.small, color = LocalV2.current.muted)
                        else V2Group {
                            people.forEachIndexed { i, u ->
                                if (i > 0) V2Divider()
                                V2Row(u.name, roleName(u.role), onClick = { picked = if (u.id in picked) picked - u.id else picked + u.id; armed = false }, selected = u.id in picked, multiple = true,
                                    mark = { Icon(if (u.id in picked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null) })
                            }
                        }
                        if (picked.isNotEmpty()) Text("تم اختيار ${picked.size}", style = V2Type.small, color = LocalV2.current.muted)
                    }
                }
            }
        },
        confirmButton = {
            val everyone = to == NoticeTo.ALL
            TextButton(enabled = title.isNotBlank() && audience != null && !sent, onClick = {
                if (everyone && !armed) armed = true
                else { sent = true; onSend(title.trim(), body.trim(), audience!!) { ok -> if (!ok) { sent = false; armed = false } } }
            }) { Text(if (everyone && armed) "تأكيد الإرسال للجميع" else "إرسال", style = V2Type.bodyStrong, color = if (everyone && armed) LocalV2.current.brick else LocalV2.current.ink) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", style = V2Type.body) } }
    )
}

// ------------------------------------------------------------------------------------------- صفحات

@Composable
fun V2InboxScreen(data: V2Data, actions: V2Actions, onOpen: (String) -> Unit) {
    val p = LocalV2.current
    V2Scroll {
        if (data.notifications.any { !it.read }) item(key = "all") {
            V2Button("تعليم الكل كمقروء", actions.readAll, Modifier.fillMaxWidth(), kind = V2ButtonKind.QUIET)
        }
        if (data.notifications.isEmpty()) item(key = "empty") { V2Empty("لا توجد إشعارات", "أي إسناد أو دعوة أو تغيير في موعد سيصلك هنا.") }
        else v2Rows(data.notifications, key = { "n-${it.id}" }) { n ->
                    V2Row(
                        title = n.title,
                        subtitle = n.description,
                        mark = { V2Mark(icon = Icons.Default.Notifications, late = n.type == "urgent") },
                        trailing = { if (!n.read) V2Tag("جديد", V2Tone.WATCH) },
                        onClick = {
                            actions.readNotification(n)
                            n.committeeId?.let { id -> if (data.committees.any { it.id == id }) onOpen(id) }
                        }
                    )
        }
    }
}

@Composable
fun V2ExamsScreen(data: V2Data, actions: V2Actions) {
    val p = LocalV2.current
    V2Scroll {
        item(key = "list") {
            if (data.exams.isEmpty()) V2Empty("لا توجد أوراق متاحة لك", "تظهر الورقة هنا عندما تتيحها الإدارة لك أو تُكلَّف بمراقبة امتحان.")
            else V2Group {
                data.exams.forEachIndexed { i, e ->
                    if (i > 0) V2Divider()
                    val locked = !e.lockedUntil.isNullOrBlank()
                    V2Row(
                        title = e.title.ifBlank { e.book },
                        subtitle = when {
                            !e.hasFile -> "لم يُرفع الملف بعد"
                            locked -> "تُتاح ${v2OpensText(e.lockedUntil!!)}"
                            else -> e.book
                        },
                        mark = { V2Mark(icon = Icons.Default.Description) },
                        trailing = { if (e.hasFile && !locked) V2Tag("فتح", V2Tone.GO) else if (locked) V2Tag("مقفولة", V2Tone.WAIT) else Unit },
                        onClick = if (e.hasFile && !locked) ({ actions.openExam(e) }) else null
                    )
                }
            }
        }
    }
}

@Composable
fun V2CalendarScreen(data: V2Data, actions: V2Actions, onOpen: (String) -> Unit) {
    val upcoming = data.tasks.filter { (!it.completed || it.date.take(10) == data.today) && it.date.take(10) >= data.today }.sortedBy { it.date + TodayFeedBuilder.minutesOf(it.time).toString().padStart(4, '0') }
    val late = data.tasks.filter { !it.completed && it.date.isNotBlank() && it.date.take(10) < data.today }
    V2Scroll {
        if (late.isNotEmpty()) {
            item(key = "late-t") { V2Section("متأخرة") }
            v2Rows(late, key = { "late-${it.id}" }) { t -> V2TaskRow(t, data, actions, onOpen) }
        }
        item(key = "up-t") { V2Section("القادم") }
        if (upcoming.isEmpty()) item(key = "up-empty") { V2Empty("لا توجد مواعيد قادمة", "المهام المسجلة في التقويم تظهر هنا يوماً بيوم.") }
        else v2Rows(upcoming.take(60), key = { "up-${it.id}" }) { t -> V2TaskRow(t, data, actions, onOpen) }
    }
}

/**
 * One calendar task. Tapping the row opens its committee; marking it done is its own 48dp check on the start side
 * (never the whole row), and the message that follows offers «تراجع».
 */
@Composable
private fun V2TaskRow(t: com.example.data.model.ScheduleTask, data: V2Data, actions: V2Actions, onOpen: (String) -> Unit) {
    val p = LocalV2.current
    val undo = LocalV2Undo.current
    val committee = t.committeeId?.takeIf { id -> data.committees.any { it.id == id } }
    V2Row(
        title = t.title,
        subtitle = listOfNotNull(DayText.relative(t.date.take(10), data.today), t.time.ifBlank { null }, t.committeeNumber?.let { "لجنة $it" }).joinToString(" · "),
        mark = {
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .toggleable(value = t.completed, role = Role.Checkbox, onValueChange = {
                        actions.toggleTask(t.id)
                        if (!t.completed) undo.offer("اتعلّمت «${t.title}» كمنجزة") { actions.toggleTask(t.id) }
                    })
                    .semantics { contentDescription = "«${t.title}» منجزة" },
                contentAlignment = Alignment.Center
            ) {
                Icon(if (t.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (t.completed) p.leaf else p.muted)
            }
        },
        trailing = { if (t.priority == "urgent") V2Tag("عاجل", V2Tone.LATE) },
        onClick = committee?.let { { onOpen(it) } },
        actionLabel = "فتح اللجنة"
    )
}

@Composable
private fun V2FeedRow(f: com.example.domain.FeedItem, data: V2Data, onOpen: (String) -> Unit) {
    val c = f.committeeId?.let { id -> data.committees.firstOrNull { it.id == id } }
    V2Row(
        title = v2FeedTitle(f, c),
        subtitle = listOf(DayText.relative(f.date, data.today), f.place).filter { it.isNotBlank() }.joinToString(" · "),
        mark = {
            if (c != null) V2Mark(c.committeeNumber.toString(), late = f.overdue)
            else V2Mark(icon = Icons.Default.CalendarMonth, late = f.overdue)
        },
        trailing = {
            when (f.kind) {
                FeedKind.CEREMONY -> V2Tag("حفل", V2Tone.WATCH)
                FeedKind.EXAM -> V2Tag("امتحان", V2Tone.GO)
                FeedKind.ACTION -> V2Tag("مطلوب منك", V2Tone.ACT)
                FeedKind.TASK -> if (f.overdue) V2Tag("متأخرة", V2Tone.LATE) else Unit
            }
        },
        onClick = c?.let { { onOpen(it.id) } }
    )
}

/**
 * The row's badge already shows the committee number, so the title drops the "لجنة #N" the shared
 * feed puts in front, and the kind (exam, ceremony) is said once, by the tag.
 */
internal fun v2FeedTitle(f: com.example.domain.FeedItem, c: com.example.data.model.Committee?): String {
    if (c == null) return f.title
    return when (f.kind) {
        FeedKind.EXAM, FeedKind.CEREMONY -> c.committeeName.ifBlank { f.title }
        FeedKind.ACTION -> f.title.replace(Regex("^لجنة #\\d+:\\s*"), "").ifBlank { f.title }
        // The row's mark already says which committee; a task title ending «لجنة 12» doesn't say it again.
        FeedKind.TASK -> f.title.replace(Regex("\\s*[-–:]?\\s*لجنة\\s*#?\\d+\\s*$"), "").ifBlank { f.title }
    }
}

/**
 * Approving a join request says what the person will be: a role (when the admin may grant one) and their
 * committees, or all of them. Nobody is approved with nothing to do.
 */
@Composable
private fun V2ApproveDialog(u: StaffUserDto, data: V2Data, onDismiss: () -> Unit, onApprove: (role: String, committeeIds: List<String>?) -> Unit) {
    val canGrant = Permissions.can("users.grant")
    var role by remember { mutableStateOf(if (u.role == "committee_head" && canGrant) "committee_head" else "sheikh") }
    var all by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(setOf<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("قبول ${u.name}", style = V2Type.heading) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (canGrant) item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        V2Chip("شيخ", role == "sheikh") { role = "sheikh" }
                        V2Chip("رئيس لجنة", role == "committee_head") { role = "committee_head" }
                    }
                }
                item { V2Row("كل اللجان", "يشوف كل لجان الدورة", onClick = { all = !all }, selected = all, multiple = true) }
                if (!all) items(data.committees.sortedBy { it.committeeNumber }, key = { it.id }) { c ->
                    val on = c.id in picked
                    V2Row("لجنة ${c.committeeNumber}", c.committeeName, onClick = { picked = if (on) picked - c.id else picked + c.id }, selected = on, multiple = true)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = all || picked.isNotEmpty(), onClick = { onApprove(role, if (all) null else picked.toList()) }) {
                Text("قبول", style = V2Type.bodyStrong)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("رجوع", style = V2Type.body) } }
    )
}

/** A round phone button for a person's row (the row itself never dials). */
@Composable
fun V2CallButton(name: String, onCall: () -> Unit) {
    val p = LocalV2.current
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(p.tonal).clickable(role = Role.Button, onClick = onCall)
            .semantics { contentDescription = "اتصال بـ $name" },
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Default.Phone, null, tint = p.ink, modifier = Modifier.size(20.dp)) }
}
