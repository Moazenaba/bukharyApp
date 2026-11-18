package com.example.ui.v2

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
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

private fun canOn(action: String, committeeId: String) = Permissions.canOn(action, committeeId)

private fun isLate(c: Committee, late: Set<String>) = c.id in late

private fun lateIds(data: V2Data): Map<String, Overdue.Item> =
    Overdue.committees(data.committees, AppSettings.current.overdueDays, data.tasks, data.today).associateBy { it.committee.id }

/** The row of a committee in any list: number, name, place and its stage. */
@Composable
fun V2CommitteeRow(c: Committee, late: Overdue.Item?, onOpen: () -> Unit, showSteps: Boolean = false) {
    val observer = Permissions.observingOn(c.id).isNotEmpty() && !Permissions.inScope(c.id)
    val place = Logistics.placeOf(c)
    val sub = listOfNotNull(place.ifBlank { null }, Logistics.studentsText(c.studentsCount)).joinToString(" · ")
    V2Row(
        title = c.committeeName,
        subtitle = sub,
        mark = { V2Mark(c.committeeNumber.toString(), late = late != null) },
        trailing = {
            when {
                observer -> V2Tag("مراقب", V2Tone.WATCH)
                late != null -> V2Tag("متأخرة", V2Tone.LATE)
                V2Stages.current(c) == null -> V2Tag("اكتملت", V2Tone.DONE)
                else -> V2Tag(V2Stages.label(c), V2Tone.WAIT)
            }
        },
        onClick = onOpen,
        under = if (showSteps) {
            { V2Steps(com.example.domain.STAGE_ORDER.count { c.stages.getStage(it).completed }) }
        } else null
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
    V2Scroll {
        item(key = "now") {
            val now = feed.now
            if (now == null) {
                V2Empty("مفيش حاجة مطلوبة منك دلوقتي", "أول ما يتسند لك امتحان أو حفل أو مهمة هتلاقيه هنا.")
            } else {
                val committee = now.committeeId?.let { id -> data.committees.firstOrNull { it.id == id } }
                val step = committee?.let { NextStep.of(it) { a -> canOn(a, it.id) } }
                val whenText = listOf(DayText.relative(now.date, data.today), now.time).filter { it.isNotBlank() }.joinToString(" · ")
                V2NowCard(meta = whenText.ifBlank { "مطلوب منك" }, title = now.title) {
                    if (now.place.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Place, null, tint = LocalV2.current.onHero)
                            Text(now.place, style = V2Type.bodyStrong, color = LocalV2.current.onHero)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            now.kind == FeedKind.TASK && now.taskId != null ->
                                V2Button("تمت المهمة", { actions.toggleTask(now.taskId) }, Modifier.weight(1f), kind = V2ButtonKind.CTA, icon = Icons.Default.CheckCircle)
                            step != null && committee != null && step.canDo && step.oneTap && now.kind == FeedKind.ACTION ->
                                V2Button(step.title, { actions.oneTap(committee, step) }, Modifier.weight(1f), kind = V2ButtonKind.CTA)
                            committee != null -> V2Button("افتح اللجنة", { onOpen(committee.id) }, Modifier.weight(1f), kind = V2ButtonKind.CTA)
                        }
                        Logistics.mapsUrl(now.place)?.let { url ->
                            V2Button("الخرائط", { V2Intents.open(context, url) }, Modifier.weight(1f), kind = V2ButtonKind.OUTLINE, icon = Icons.Default.Place, contentColor = LocalV2.current.onHero)
                        }
                    }
                    if (now.headPhone.isNotBlank()) {
                        V2Button("اتصال برئيس اللجنة ${now.headName}".trim(), { V2Intents.call(context, now.headPhone) }, Modifier.fillMaxWidth(), kind = V2ButtonKind.OUTLINE, icon = Icons.Default.Phone, contentColor = LocalV2.current.onHero)
                    }
                }
            }
        }
        if (feed.later.isNotEmpty()) {
            item(key = "later-title") { V2Section("قادم") }
            item(key = "later") {
                V2Group {
                    feed.later.forEachIndexed { i, f ->
                        if (i > 0) V2Divider()
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
                                    FeedKind.ACTION -> V2Tag("مطلوب منك", V2Tone.GO)
                                    FeedKind.TASK -> if (f.overdue) V2Tag("متأخرة", V2Tone.LATE) else Unit
                                }
                            },
                            onClick = c?.let { { onOpen(it.id) } }
                        )
                    }
                }
            }
        }
        val mine = data.committees.filter { Permissions.inScope(it.id) || Permissions.observingOn(it.id).isNotEmpty() }
        if (mine.isNotEmpty() && data.user.role.roleKey == "committee_head") {
            item(key = "mine-title") { V2Section("لجانك") }
            item(key = "mine") {
                V2Group {
                    mine.sortedBy { it.committeeNumber }.forEachIndexed { i, c ->
                        if (i > 0) V2Divider()
                        V2CommitteeRow(c, late[c.id], { onOpen(c.id) }, showSteps = true)
                    }
                }
            }
        }
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
                V2NowCard("مهلة كل مرحلة ${AppSettings.current.overdueDays} أيام", "${late.size} ${if (late.size in 3..10) "لجان متأخرة" else "لجنة متأخرة"}") {
                    V2Button("عرض المتأخر", { onShow(V2Filter.LATE) }, Modifier.fillMaxWidth(), kind = V2ButtonKind.CTA)
                }
            } else {
                V2NowCard(if (examsToday > 0) "$examsToday امتحان النهارده" else "النهارده", "مفيش لجان متأخرة") {
                    V2Button("كل اللجان", { onShow(V2Filter.ALL) }, Modifier.fillMaxWidth(), kind = V2ButtonKind.CTA)
                }
            }
        }
        item(key = "stats") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V2Stat("متأخرة", late.size, late.isNotEmpty(), Modifier.weight(1f)) { onShow(V2Filter.LATE) }
                V2Stat("امتحان النهارده", examsToday, false, Modifier.weight(1f)) { onShow(V2Filter.EXAM) }
                V2Stat("طلب انضمام", pending, pending > 0, Modifier.weight(1f), onPeople)
            }
        }
        item(key = "stages-title") { V2Section("المراحل") }
        item(key = "stages") {
            V2Group {
                V2Stages.Group.values().forEachIndexed { i, g ->
                    if (i > 0) V2Divider()
                    val f = V2Filter.values().first { it.group() == g }
                    V2Row(title = g.label, mark = { V2Mark((counts[g] ?: 0).toString()) }, onClick = { onShow(f) })
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

@Composable
private fun V2Stat(label: String, value: Int, alert: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalV2.current
    V2Group(modifier) {
        Column(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = label, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value.toString(), style = V2Type.bigNumber, color = if (alert) p.brick else p.ink)
            Text(label, style = V2Type.small, color = p.muted, maxLines = 1)
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
    val shown = base.filter { c ->
        V2Stages.matches(c, query) && when (filter) {
            V2Filter.ALL -> true
            V2Filter.LATE -> c.id in late
            else -> V2Stages.groupOf(c) == filter.group()
        }
    }.sortedWith(compareByDescending<Committee> { it.id in late }.thenBy { it.committeeNumber })
    V2Scroll {
        if (base.size > 6) {
            item(key = "search") {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("رقم اللجنة أو اسمها أو الشيخ", style = V2Type.body) },
                    shape = RoundedCornerShape(14.dp), textStyle = V2Type.body, modifier = Modifier.fillMaxWidth()
                )
            }
            item(key = "chips") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
        item(key = "list") {
            if (shown.isEmpty()) {
                V2Empty(
                    if (base.isEmpty()) (if (mineOnly) "لسه مفيش لجنة متسندة لك" else "مفيش لجان في الدورة دي") else "مفيش لجنة بالبحث ده",
                    if (base.isEmpty()) "أول ما تتسند لك لجنة هيوصلك إشعار وتظهر هنا." else "جرّب رقم اللجنة أو امسح التصفية."
                )
            } else {
                V2Group {
                    shown.forEachIndexed { i, c ->
                        if (i > 0) V2Divider()
                        V2CommitteeRow(c, late[c.id], { onOpen(c.id) })
                    }
                }
            }
        }
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
            if (list.isEmpty()) V2Empty("مفيش حفلات ليك لسه", "لما تتدعى لحفل يوصلك إشعار ويظهر هنا بموعده ومكانه.")
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
        if (mine.isEmpty()) item(key = "empty") { V2Empty("مفيش لجان ترأسها", "لما الإدارة تسند لك لجنة تقدر تضيف شيوخها من هنا.") }
        mine.forEach { c ->
            item(key = "t-${c.id}") { V2Section("لجنة ${c.committeeNumber} · ${c.committeeName}") }
            item(key = "g-${c.id}") {
                V2Group {
                    if (c.staff.isEmpty()) V2Row(title = "لسه مفيش شيوخ في اللجنة")
                    c.staff.forEachIndexed { i, s ->
                        if (i > 0) V2Divider()
                        V2Row(title = s.name, subtitle = if (s.role == "committee_head") "رئيس اللجنة" else "شيخ", mark = { V2Avatar(s.name) })
                    }
                    V2Divider()
                    Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        V2Button("إضافة / إزالة شيوخ", { actions.loadCandidates(); editing = c }, Modifier.weight(1f), kind = V2ButtonKind.QUIET)
                        V2Button("المراقبون", { onOpen(c.id) }, Modifier.weight(1f), kind = V2ButtonKind.QUIET)
                    }
                }
            }
        }
    }
    editing?.let { c -> V2StaffDialog(c, data, onDismiss = { editing = null }) { ids -> actions.saveStaff(c, ids); editing = null } }
}

@Composable
fun V2StaffDialog(c: Committee, data: V2Data, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    val p = LocalV2.current
    var picked by remember(c.id) { mutableStateOf(c.staff.map { it.id }.toSet()) }
    val people = data.candidates?.filter { (it.role == "sheikh" || it.role == "committee_head") && it.id != data.user.id }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("شيوخ لجنة ${c.committeeNumber}", style = V2Type.heading) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                if (people == null) item { Text("جارٍ تحميل الأسماء…", style = V2Type.body, color = p.muted) }
                items(people.orEmpty(), key = { it.id }) { person ->
                    val on = person.id in picked
                    V2Row(
                        title = person.name,
                        subtitle = if (person.role == "committee_head") "رئيس لجنة" else "شيخ",
                        mark = { Icon(if (on) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (on) p.leaf else p.muted) },
                        onClick = { picked = if (on) picked - person.id else picked + person.id }
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
    val requests = data.staff.filter { it.status == "pending" && it.role != "super_admin" }
    val approved = data.staff.filter { it.status == "approved" }
        .filter { query.isBlank() || it.name.contains(query.trim()) || it.phone.contains(query.trim()) }
        .sortedWith(compareBy<StaffUserDto> { roleOrder(it.role) }.thenBy { it.name })
    V2Scroll {
        if (requests.isNotEmpty()) {
            item(key = "req-title") { V2Section("طلبات انضمام (${requests.size})") }
            item(key = "req") {
                V2Group {
                    requests.forEachIndexed { i, u ->
                        if (i > 0) V2Divider()
                        V2Row(title = u.name, subtitle = listOf(roleName(u.role), u.phone).filter { it.isNotBlank() }.joinToString(" · "), mark = { V2Avatar(u.name) })
                        if (V2Tool.JOIN_REQUESTS in tools) {
                            Row(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                V2Button("قبول", { actions.approve(u) }, Modifier.weight(1f))
                                V2Button("اعتذار", { rejecting = u }, Modifier.weight(1f), kind = V2ButtonKind.QUIET)
                            }
                        }
                    }
                }
            }
        }
        item(key = "search") {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("الاسم أو رقم الموبايل", style = V2Type.body) },
                shape = RoundedCornerShape(14.dp), textStyle = V2Type.body, modifier = Modifier.fillMaxWidth()
            )
        }
        item(key = "users-title") { V2Section(if (data.staffLoading && data.staff.isEmpty()) "جارٍ التحميل…" else "المستخدمون (${approved.size})") }
        item(key = "users") {
            if (approved.isNotEmpty()) V2Group {
                approved.forEachIndexed { i, u ->
                    if (i > 0) V2Divider()
                    val committees = if (u.allCommitteesAllowed) "كل اللجان" else if (u.assignedCommitteeIds.isEmpty()) "بدون لجان" else "${u.assignedCommitteeIds.size} لجان"
                    V2Row(
                        title = u.name,
                        subtitle = "${roleName(u.role)} · $committees",
                        mark = { V2Avatar(u.name) },
                        trailing = { if (u.phone.isNotBlank()) Icon(Icons.Default.Phone, "اتصال بـ ${u.name}", tint = LocalV2.current.muted) },
                        onClick = if (u.phone.isNotBlank()) ({ V2Intents.call(context, u.phone) }) else null
                    )
                }
            }
        }
        item(key = "note") {
            Text("تعديل الأدوار والصلاحيات الدقيقة بيفتح في الواجهة الكاملة من «المزيد».", style = V2Type.small, color = LocalV2.current.muted)
        }
    }
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

private fun roleOrder(role: String) = when (role) { "super_admin" -> 0; "admin" -> 1; "committee_head" -> 2; else -> 3 }
fun roleName(role: String) = when (role) { "super_admin" -> "مشرف عام"; "admin" -> "أدمن"; "committee_head" -> "رئيس لجنة"; else -> "شيخ" }

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
                V2Row("الإشعارات", if (unread > 0) "$unread جديد" else "لا جديد", mark = { V2Mark(icon = Icons.Default.Notifications) }, onClick = { onPage(V2Page.INBOX) })
                if (V2Tool.EXAMS in tools || V2Tool.EXAM_PAPER in tools) {
                    V2Divider()
                    V2Row("الامتحانات", "${data.exams.size} ورقة", mark = { V2Mark(icon = Icons.Default.Description) }, onClick = { onPage(V2Page.EXAMS) })
                }
                V2Divider()
                V2Row("التقويم", if (todays > 0) "$todays أعمال النهارده" else "المواعيد والمهام", mark = { V2Mark(icon = Icons.Default.CalendarMonth) }, onClick = { onPage(V2Page.CALENDAR) })
                if (V2Tool.SEND_NOTICE in tools) {
                    V2Divider()
                    V2Row("إرسال تنبيه", "يوصل لكل المستخدمين", mark = { V2Mark(icon = Icons.Default.Campaign) }, onClick = { sending = true })
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
    if (sending) V2NoticeDialog(onDismiss = { sending = false }) { t, b -> actions.sendNotice(t, b) { ok -> if (ok) sending = false } }
    if (choosingCourse) {
        AlertDialog(
            onDismissRequest = { choosingCourse = false },
            title = { Text("الدورة المعروضة", style = V2Type.heading) },
            text = {
                Column {
                    data.courses.forEach { c ->
                        V2Row(c.name, if (c.isDefault) "الحالية" else null, onClick = { actions.selectCourse(c.id); choosingCourse = false },
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
    V2Group {
        V2Row(data.user.name, roleName(data.user.role.roleKey), mark = { V2Avatar(data.user.name) })
        V2Divider()
        val theme = com.example.domain.v2.V2Themes.ALL.firstOrNull { it.key == data.themeKey }
        V2Row("الألوان", theme?.let { "${it.name}: ${it.story}" } ?: "تلقائي (حسب الموبايل)", mark = { V2Mark(icon = Icons.Default.DarkMode) },
            trailing = { theme?.let { V2Swatch(it) } }, onClick = { choosingTheme = true })
        V2Divider()
        V2Row("مزامنة الآن", if (data.isOnline) "متصل" else "بدون اتصال", mark = { V2Mark(icon = Icons.Default.Sync) }, onClick = actions.sync)
        V2Divider()
        V2Row("تغيير كلمة المرور", mark = { V2Mark(icon = Icons.Default.Lock) }, onClick = actions.changePassword)
        V2Divider()
        V2Row("الرجوع للواجهة القديمة", "تقدر ترجع للواجهات 2 من القائمة", mark = { V2Mark(icon = Icons.Default.SwapHoriz) }, onClick = actions.leaveV2)
        V2Divider()
        V2Row("تسجيل الخروج", mark = { V2Mark(icon = Icons.AutoMirrored.Filled.Logout, late = true) }, onClick = actions.logout)
    }
    if (choosingTheme) {
        AlertDialog(
            onDismissRequest = { choosingTheme = false },
            title = { Text("الألوان", style = V2Type.heading) },
            text = {
                LazyColumn {
                    item {
                        V2Row("تلقائي", "فاتح أو غامق حسب الموبايل", onClick = { actions.v2Theme(com.example.domain.v2.V2Themes.AUTO); choosingTheme = false },
                            mark = { Icon(if (data.themeKey == com.example.domain.v2.V2Themes.AUTO) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null) })
                    }
                    items(com.example.domain.v2.V2Themes.ALL, key = { it.key }) { t ->
                        V2Row(t.name, t.story, onClick = { actions.v2Theme(t.key); choosingTheme = false },
                            mark = { Icon(if (t.key == data.themeKey) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null) },
                            trailing = { V2Swatch(t) })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingTheme = false }) { Text("إغلاق", style = V2Type.body) } }
        )
    }
}

@Composable
private fun V2NoticeDialog(onDismiss: () -> Unit, onSend: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إرسال تنبيه للجميع", style = V2Type.heading) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("العنوان") }, singleLine = true, textStyle = V2Type.body, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(body, { body = it }, label = { Text("التفاصيل") }, minLines = 3, textStyle = V2Type.body, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSend(title.trim(), body.trim()) }) { Text("إرسال", style = V2Type.bodyStrong) } },
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
        item(key = "list") {
            if (data.notifications.isEmpty()) V2Empty("مفيش إشعارات", "أي إسناد أو دعوة أو تغيير في موعد هيوصلك هنا.")
            else V2Group {
                data.notifications.forEachIndexed { i, n ->
                    if (i > 0) V2Divider()
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
    }
}

@Composable
fun V2ExamsScreen(data: V2Data, actions: V2Actions) {
    val p = LocalV2.current
    V2Scroll {
        item(key = "list") {
            if (data.exams.isEmpty()) V2Empty("مفيش أوراق متاحة لك", "الورقة بتظهر هنا لما الإدارة تتيحها لك أو تتكلف بمراقبة امتحان.")
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
    val upcoming = data.tasks.filter { !it.completed && it.date.take(10) >= data.today }.sortedBy { it.date + TodayFeedBuilder.minutesOf(it.time).toString().padStart(4, '0') }
    val late = data.tasks.filter { !it.completed && it.date.isNotBlank() && it.date.take(10) < data.today }
    V2Scroll {
        if (late.isNotEmpty()) {
            item(key = "late-t") { V2Section("متأخرة") }
            item(key = "late") { V2TaskGroup(late, data, actions, onOpen) }
        }
        item(key = "up-t") { V2Section("القادم") }
        item(key = "up") {
            if (upcoming.isEmpty()) V2Empty("مفيش مواعيد قادمة", "المهام اللي تتسجل في التقويم بتظهر هنا يوم بيوم.")
            else V2TaskGroup(upcoming.take(40), data, actions, onOpen)
        }
    }
}

@Composable
private fun V2TaskGroup(list: List<com.example.data.model.ScheduleTask>, data: V2Data, actions: V2Actions, onOpen: (String) -> Unit) {
    val p = LocalV2.current
    V2Group {
        list.forEachIndexed { i, t ->
            if (i > 0) V2Divider()
            V2Row(
                title = t.title,
                subtitle = listOfNotNull(DayText.relative(t.date.take(10), data.today), t.time.ifBlank { null }, t.committeeNumber?.let { "لجنة $it" }).joinToString(" · "),
                mark = {
                    androidx.compose.foundation.layout.Box(
                        Modifier.padding(2.dp)
                    ) {
                        Icon(
                            if (t.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "تحديد «${t.title}» كمنجزة",
                            tint = if (t.completed) p.leaf else p.muted,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                },
                trailing = { if (t.priority == "urgent") V2Tag("عاجل", V2Tone.LATE) },
                onClick = { actions.toggleTask(t.id) }
            )
        }
    }
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
        FeedKind.TASK -> f.title
    }
}
