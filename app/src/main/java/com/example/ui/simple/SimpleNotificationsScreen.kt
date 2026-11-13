package com.example.ui.simple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.NotificationItem
import com.example.ui.motion.staggerIn
import com.example.ui.theme.AppBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.tBg
import com.example.ui.theme.tFg

/** «الإشعارات»: one list. Tapping opens the linked committee and marks the item read. */
@Composable
fun SimpleNotificationsScreen(
    items: List<NotificationItem>,
    onOpen: (NotificationItem) -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unread = items.count { !it.read }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(tBg(AppBackground)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "head") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (unread > 0) "الإشعارات ($unread جديد)" else "الإشعارات", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                if (unread > 0) TextButton(onClick = onMarkAllRead) { Text("تعليم الكل كمقروء") }
            }
        }
        if (items.isEmpty()) item(key = "empty") {
            EmptyState(Icons.Default.NotificationsNone, "مفيش إشعارات", "أي جديد يخص لجانك هيوصلك هنا.")
        }
        itemsIndexed(items, key = { _, n -> n.id }) { index, n ->
            SimpleCard(modifier = Modifier.animateItem().staggerIn(index), onClick = { onOpen(n) }, onClickLabel = n.title) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(n.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = if (n.read) FontWeight.Medium else FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!n.read) StatusPill("جديد", Tone.INFO)
                }
                if (n.description.isNotBlank()) Text(n.description, style = MaterialTheme.typography.bodySmall, color = tFg(TextMuted), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
