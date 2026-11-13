package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Committee
import com.example.data.model.ScheduleTask
import com.example.data.remote.AppSettings

/** «لجان متأخرة» card (web: OverduePanel), with the overdue days from «إعدادات النظام». */
@Composable
fun OverduePanel(committees: List<Committee>, tasks: List<ScheduleTask>, onOpen: (String) -> Unit, limit: Int = 6) {
    val days = AppSettings.current.overdueDays
    val items = Overdue.committees(committees, days, tasks)
    val shape = RoundedCornerShape(16.dp)
    if (items.isEmpty()) {
        Text(
            "✅ لا توجد لجان متأخرة (المهلة $days أيام لكل مرحلة).",
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF065F46),
            modifier = Modifier.fillMaxWidth().background(Color(0xFFECFDF5), shape).border(1.dp, Color(0xFFA7F3D0), shape).padding(12.dp)
        )
        return
    }
    Column(
        Modifier.fillMaxWidth().background(Color(0xFFFFF1F2), shape).border(1.dp, Color(0xFFFECDD3), shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("⏰ لجان متأخرة (${items.size})", fontWeight = FontWeight.Black, color = Color(0xFF881337))
        items.take(limit).forEach { item ->
            Row(
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).clickable { onOpen(item.committee.id) }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(32.dp).background(Color(0xFFE11D48), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text("${item.committee.committeeNumber}", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        item.committee.committeeName + (item.committee.sheikhName.takeIf { s -> s.isNotBlank() }?.let { s -> " — $s" } ?: ""),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color(0xFF0F172A)
                    )
                    Text(item.reason, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9F1239))
                }
            }
        }
        if (items.size > limit) Text("و ${items.size - limit} لجنة أخرى…", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9F1239))
    }
}
