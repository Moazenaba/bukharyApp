package com.example.ui.simple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DayText
import com.example.domain.FeedItem
import com.example.domain.FeedKind
import com.example.domain.TodayFeed
import com.example.ui.motion.staggerIn
import com.example.ui.theme.AppBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.tBg
import com.example.ui.theme.tFg
import java.util.Calendar

/** «اليوم»: the one thing to look at now, then what is coming. Nothing else. */
@Composable
fun TodayScreen(
    userName: String,
    feed: TodayFeed,
    today: String,
    onOpenCommittee: (String) -> Unit,
    onToggleTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(tBg(AppBackground)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "greeting") { Greeting(userName, today) }
        item(key = "now") {
            val now = feed.now
            if (now == null) {
                EmptyState(
                    icon = Icons.Default.CheckCircle,
                    title = "مفيش حاجة مطلوبة منك دلوقتي",
                    body = "لما يبقى فيه امتحان أو حفل أو مهمة هتلاقيه هنا."
                )
            } else {
                TodayHero(now, today, onOpenCommittee, onToggleTask)
            }
        }
        if (feed.later.isNotEmpty()) {
            item(key = "later-title") { SectionTitle(if (feed.total > feed.later.size + 1) "قادم (${feed.total - 1})" else "قادم") }
            itemsIndexed(feed.later, key = { _, item -> item.key }) { index, item ->
                FeedRow(item, today, onOpenCommittee, onToggleTask, Modifier.animateItem().staggerIn(index))
            }
        }
        item(key = "end") { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun Greeting(userName: String, today: String) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val hello = if (hour < 12) "صباح الخير" else "مساء الخير"
    val name = remember(userName) { userName.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString(" ") }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            if (name.isBlank()) hello else "$hello، $name",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = tFg(Color(0xFF0F172A))
        )
        val day = DayText.full(today)
        if (day.isNotBlank()) Text(day, style = MaterialTheme.typography.bodyMedium, color = tFg(TextMuted))
    }
}

private fun when_(item: FeedItem, today: String): String {
    val rel = DayText.relative(item.date, today)
    val time = item.time.trim()
    return if (rel.isBlank()) "" else if (time.isNotBlank() && !item.overdue) "$rel · $time" else rel
}

/** The focal block: what to look at first. The only strongly colored block on the screen. */
@Composable
private fun TodayHero(item: FeedItem, today: String, onOpen: (String) -> Unit, onToggle: (String) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val isTask = item.kind == FeedKind.TASK
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(FocusStart, FocusEnd)))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("الآن", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
        Text(
            item.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        if (item.subtitle.isNotBlank()) {
            Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (item.place.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Place, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(item.place, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        val whenText = when_(item, today)
        if (whenText.isNotBlank()) {
            Text(
                whenText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(if (item.overdue) Color(0xFFB91C1C) else Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
        val open: (() -> Unit)? = if (!isTask) item.committeeId?.let { id -> { onOpen(id) } } else null
        val toggle: (() -> Unit)? = if (isTask) item.taskId?.let { id -> { onToggle(id) } } else null
        val action = open ?: toggle
        if (action != null) {
            Button(
                onClick = action,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = FocusStart),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(if (isTask) "تمت المهمة" else "افتح", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun FeedRow(
    item: FeedItem,
    today: String,
    onOpen: (String) -> Unit,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isTask = item.kind == FeedKind.TASK
    val open: (() -> Unit)? = if (!isTask) item.committeeId?.let { id -> { onOpen(id) } } else null
    SimpleCard(modifier = modifier, onClick = open, onClickLabel = item.title, padding = if (isTask) 8.dp else 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isTask) {
                IconToggleButton(checked = false, onCheckedChange = { item.taskId?.let(onToggle) }) {
                    Icon(Icons.Default.RadioButtonUnchecked, contentDescription = "تحديد «${item.title}» كمنجز", tint = tFg(TextMuted))
                }
            } else {
                IconDisc(
                    when (item.kind) {
                        FeedKind.CEREMONY -> Icons.Default.Celebration
                        FeedKind.EXAM -> Icons.Default.EditNote
                        else -> Icons.Default.TaskAlt
                    },
                    when (item.kind) {
                        FeedKind.CEREMONY -> Tone.WARN
                        FeedKind.ACTION -> Tone.INFO
                        else -> Tone.NEUTRAL
                    }
                )
            }
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = tFg(Color(0xFF0F172A)))
                if (item.subtitle.isNotBlank()) {
                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = tFg(TextMuted))
                }
            }
            val whenText = when_(item, today)
            if (whenText.isNotBlank()) {
                StatusPill(whenText, if (item.overdue) Tone.DANGER else if (item.today) Tone.WARN else Tone.NEUTRAL)
            }
        }
    }
}
