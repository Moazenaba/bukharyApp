package com.example.ui.screens.schedule

import com.example.ui.theme.tEdge
import com.example.ui.theme.tBg
import com.example.ui.theme.tFg
import com.example.ui.theme.TextMuted
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ScheduleTask
import com.example.ui.components.ARABIC_DAYS
import com.example.ui.components.Dates
import com.example.ui.theme.Primary700
import java.util.Calendar

private val MONTHS = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")

/** Month grid (web: CalendarScheduleView): days with tasks are marked; ceremonies in gold. */
@Composable
fun MonthCalendar(tasks: List<ScheduleTask>, selectedDate: String?, onSelect: (String) -> Unit) {
    val start = remember { Calendar.getInstance() }
    var year by remember { mutableIntStateOf((selectedDate?.let { Dates.toMillis(it) }?.let { Calendar.getInstance().apply { timeInMillis = it } } ?: start).get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf((selectedDate?.let { Dates.toMillis(it) }?.let { Calendar.getInstance().apply { timeInMillis = it } } ?: start).get(Calendar.MONTH)) }
    val today = Dates.today()
    val first = Calendar.getInstance().apply { clear(); set(year, month, 1) }
    // Weeks start on Saturday, as on the web.
    val offset = first.get(Calendar.DAY_OF_WEEK) % 7
    val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val byDate = tasks.groupBy { it.date }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = tBg(Color.White))) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (month == 0) { month = 11; year-- } else month-- }) { Icon(Icons.Filled.ChevronRight, "الشهر السابق") }
                Text("${MONTHS[month]} $year", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { if (month == 11) { month = 0; year++ } else month++ }) { Icon(Icons.Filled.ChevronLeft, "الشهر التالي") }
            }
            androidx.compose.material3.TextButton(
                onClick = {
                    val now = Calendar.getInstance()
                    year = now.get(Calendar.YEAR); month = now.get(Calendar.MONTH)
                    onSelect(today)
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("الذهاب لليوم الحالي") }
            Row(Modifier.fillMaxWidth()) {
                listOf("سبت", "أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة").forEach { d -> Text(d, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            }
            val cells = offset + days
            val rows = (cells + 6) / 7
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (c in 0 until 7) {
                        val dayNum = r * 7 + c - offset + 1
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(1.dp), contentAlignment = Alignment.Center) {
                            if (dayNum in 1..days) {
                                val iso = "%04d-%02d-%02d".format(java.util.Locale.US, year, month + 1, dayNum)
                                val dayTasks = byDate[iso].orEmpty()
                                val selected = iso == selectedDate
                                Column(
                                    Modifier.fillMaxWidth().aspectRatio(1f)
                                        .background(if (selected) tBg(Primary700) else if (iso == today) tBg(Color(0xFFECFDF5)) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .border(1.dp, if (iso == today) tEdge(Color(0xFF10B981)) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { onSelect(iso) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("$dayNum", style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else tFg(Color(0xFF0F172A)))
                                    if (dayTasks.isNotEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            if (dayTasks.any { it.type == "ceremony" }) Box(Modifier.size(5.dp).background(tBg(Color(0xFFF59E0B)), CircleShape))
                                            if (dayTasks.any { it.type != "ceremony" && !it.completed }) Box(Modifier.size(5.dp).background(if (selected) tBg(Color.White) else tBg(Color(0xFFEF4444)), CircleShape))
                                            if (dayTasks.all { it.completed }) Box(Modifier.size(5.dp).background(tBg(Color(0xFF10B981)), CircleShape))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
