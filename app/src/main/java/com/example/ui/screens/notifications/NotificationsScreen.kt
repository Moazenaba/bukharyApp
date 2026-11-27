package com.example.ui.screens.notifications

import com.example.ui.theme.tEdge
import com.example.ui.theme.tBg
import com.example.ui.theme.tFg
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import com.example.data.remote.StaffUserDto
import com.example.domain.NoticeAudience
import com.example.domain.NoticeTo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Committee
import com.example.data.model.NotificationItem
import com.example.data.remote.Permissions
import com.example.ui.components.Dates
import com.example.ui.components.ScreenBanner
import com.example.ui.theme.Primary800
import com.example.ui.theme.TextMuted

/** All notifications (web: the bell menu), mark all read, open the linked committee, and — for super admins — send one. */
@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    committees: List<Committee>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenCommittee: (String) -> Unit,
    onSend: (title: String, description: String, type: String, committeeId: String?, audience: Map<String, Any?>, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    /** People to pick from when a notice goes to chosen people (loaded on demand by [onLoadStaff]). */
    staff: List<StaffUserDto> = emptyList(),
    onLoadStaff: () -> Unit = {}
) {
    var composing by remember { mutableStateOf(false) }
    val canSend = Permissions.can("notifications.create")
    val unread = notifications.count { !it.read }

    LazyColumn(
        modifier.fillMaxSize().background(tBg(Color(0xFFF8F9FD))),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenBanner("الإشعارات والتنبيهات", "آخر التنبيهات الميدانية من المنظومة ($unread غير مقروء)", Icons.Default.Notifications, listOf(Primary800, Color(0xFF1E40AF))) {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (unread > 0) OutlinedButton(onClick = onMarkAllRead) { Text("تحديد الكل كمقروء", color = Color.White) }
                    if (canSend) Button(onClick = { composing = true }) { Text("إرسال تنبيه") }
                }
            }
        }
        if (notifications.isEmpty()) item { Text("لا توجد إشعارات حتى الآن", color = tFg(TextMuted), modifier = Modifier.padding(20.dp)) }
        items(notifications, key = { it.id }) { n ->
            Card(
                Modifier.fillMaxWidth().clickable(role = Role.Button) {
                    if (!n.read) onMarkRead(n.id)
                    n.committeeId?.let { id -> if (committees.any { it.id == id }) onOpenCommittee(id) }
                },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (n.read) tBg(Color.White) else tBg(Color(0xFFEFF6FF))),
                border = BorderStroke(1.dp, when (n.type) { "urgent" -> tEdge(Color(0xFFFCA5A5)); "success" -> tEdge(Color(0xFF86EFAC)); else -> tEdge(Color(0xFFE2E8F0)) })
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        (if (!n.read) "● " else "") + n.title, fontWeight = FontWeight.Bold,
                        color = if (n.type == "urgent") tFg(Color(0xFFB91C1C)) else tFg(Color(0xFF0F172A))
                    )
                    if (n.description.isNotBlank()) Text(n.description, style = MaterialTheme.typography.bodySmall, color = tFg(Color(0xFF475569)))
                    Row {
                        Text(Dates.displayTime(n.createdAt), style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted), modifier = Modifier.weight(1f))
                        n.committeeId?.let { id -> committees.find { it.id == id }?.let { Text("لجنة #${it.committeeNumber} ↗", style = MaterialTheme.typography.labelSmall, color = tFg(Color(0xFF0F766E))) } }
                    }
                }
            }
        }
    }

    if (composing) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("info") }
        var committeeId by remember { mutableStateOf<String?>(null) }
        var sending by remember { mutableStateOf(false) }
        var to by remember { mutableStateOf(NoticeTo.ALL) }
        var groups by remember { mutableStateOf(setOf<String>()) }
        var picked by remember { mutableStateOf(setOf<String>()) }
        var query by remember { mutableStateOf("") }
        // A message to everyone is sent on the second tap.
        var armed by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { if (staff.isEmpty()) onLoadStaff() }
        val audience = NoticeAudience.of(to, groups, picked)
        // Who the choice reaches, asked from the server a moment after the choice settles.
        var reach by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(audience) {
            reach = null
            if (audience == null) return@LaunchedEffect
            kotlinx.coroutines.delay(400)
            reach = try { com.example.data.remote.OnlineApi.previewAudience(audience) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { null }
        }
        val people = staff.filter { it.status == "approved" && (query.isBlank() || it.name.contains(query.trim()) || it.phone.contains(query.trim())) }.sortedBy { it.name }
        AlertDialog(
            onDismissRequest = { composing = false },
            title = { Text(NoticeAudience.heading(to, picked.size)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("العنوان") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(description, { description = it }, label = { Text("نص التنبيه") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Text("إلى مين؟", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NoticeTo.values().forEach { o -> FilterChip(selected = to == o, onClick = { to = o; armed = false }, label = { Text(o.label) }) }
                    }
                    when (to) {
                        NoticeTo.ALL -> Text("يوصل لكل المستخدمين المعتمدين.", style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
                        NoticeTo.GROUP -> NoticeAudience.GROUPS.forEach { (key, label) ->
                            Row(Modifier.fillMaxWidth().clickable { groups = if (key in groups) groups - key else groups + key; armed = false }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = key in groups, onCheckedChange = { groups = if (it) groups + key else groups - key; armed = false })
                                Text(label)
                            }
                        }
                        NoticeTo.PEOPLE -> {
                            OutlinedTextField(query, { query = it }, label = { Text("بحث بالاسم أو الموبايل") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            if (staff.isEmpty()) Text("جارٍ تحميل المستخدمين…", style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted))
                            Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                                people.forEach { u ->
                                    Row(Modifier.fillMaxWidth().clickable { picked = if (u.id in picked) picked - u.id else picked + u.id; armed = false }, verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = u.id in picked, onCheckedChange = { picked = if (it) picked + u.id else picked - u.id; armed = false })
                                        Text(u.name)
                                    }
                                }
                            }
                            if (picked.isNotEmpty()) Text("تم اختيار ${picked.size}", style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted))
                        }
                    }
                    reach?.let { n ->
                        Text(if (n == 0) "لا يوجد مستلمون لهذا الاختيار." else "سيصل إلى $n شخص.", fontWeight = FontWeight.Bold,
                            color = if (n == 0) tFg(Color(0xFFB91C1C)) else tFg(Color(0xFF0F766E)))
                    }
                    Text("النوع", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("info" to "معلومة", "success" to "إنجاز", "urgent" to "عاجل").forEach { (k, l) -> FilterChip(selected = type == k, onClick = { type = k }, label = { Text(l) }) }
                    }
                    Text("لجنة مرتبطة (اختياري)", fontWeight = FontWeight.Bold)
                    Column(Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                        FilterChip(selected = committeeId == null, onClick = { committeeId = null }, label = { Text("بدون") })
                        committees.sortedBy { it.committeeNumber }.forEach { c ->
                            FilterChip(
                                selected = committeeId == c.id, onClick = { committeeId = c.id },
                                label = { Text("#${c.committeeNumber} ${c.committeeName}${if (c.courseName.isNotBlank()) " — ${c.courseName}" else ""}") }
                            )
                        }
                    }
                    Text("يصل التنبيه أيضاً كإشعار على هواتف المستخدمين.", style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted))
                }
            },
            confirmButton = {
                val everyone = to == NoticeTo.ALL
                Button(enabled = title.isNotBlank() && audience != null && reach != 0 && !sending, onClick = {
                    if (everyone && !armed) armed = true
                    else {
                        sending = true
                        onSend(title.trim(), description.trim(), type, committeeId, audience!!) { ok -> sending = false; armed = false; if (ok) composing = false }
                    }
                }) { Text(if (sending) "جارٍ الإرسال..." else if (everyone && armed) "تأكيد الإرسال للجميع" else "إرسال") }
            },
            dismissButton = { TextButton(onClick = { composing = false }) { Text("إلغاء") } }
        )
    }
}

