package com.example.ui.screens.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.AuditLogDto
import com.example.ui.components.Dates
import com.example.ui.components.ScreenBanner
import com.example.ui.screens.assistants.roleLabel
import com.example.ui.theme.Primary800
import com.example.ui.theme.TextMuted

private val ENTITIES = listOf(
    "" to "الكل", "auth" to "الدخول والجلسات", "user" to "المستخدمون", "committee" to "اللجان", "exam" to "الامتحانات",
    "task" to "المهام", "course" to "الدورات", "summer_course_row" to "كشف الدورة", "permission_profile" to "بروفايلات الصلاحيات"
)

/** Web: AuditLogView — every change with who made it and when, newest first, paged. */
@Composable
fun AuditLogScreen(
    logs: List<AuditLogDto>,
    isLoading: Boolean,
    hasMore: Boolean,
    onLoad: (entity: String, reset: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var entity by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(entity) { onLoad(entity, true) }

    LazyColumn(
        modifier.fillMaxSize().background(Color(0xFFF8F9FD)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenBanner("سجل التدقيق", "كل تعديل يُسجَّل مع اسم من قام به ووقته، في نفس عملية الحفظ.", Icons.Default.History, listOf(Color(0xFF334155), Primary800))
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ENTITIES.forEach { (k, l) -> FilterChip(selected = entity == k, onClick = { entity = k }, label = { Text(l) }) }
            }
        }
        if (logs.isEmpty() && !isLoading) item { Text("لا توجد سجلات", color = TextMuted, modifier = Modifier.padding(20.dp)) }
        items(logs, key = { it.id }) { l ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(10.dp).fillMaxWidth()) {
                    Row {
                        Text(l.action, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(Dates.displayTime(l.timestamp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Text("${l.user ?: "—"}${l.role?.let { " (${roleLabel(it)})" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF0F766E))
                    if (!l.entity.isNullOrBlank()) Text("العنصر: ${l.entity}${l.entityId?.let { " • $it" } ?: ""}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    if (l.details.isNotBlank()) Text(l.details, style = MaterialTheme.typography.labelSmall, maxLines = 4)
                    if (l.status.isNotBlank() && l.status != "success") Text(l.status, style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626))
                }
            }
        }
        item {
            when {
                isLoading -> CircularProgressIndicator(Modifier.padding(12.dp))
                hasMore && logs.isNotEmpty() -> Button(onClick = { onLoad(entity, false) }, modifier = Modifier.fillMaxWidth()) { Text("تحميل المزيد") }
            }
        }
    }
}
