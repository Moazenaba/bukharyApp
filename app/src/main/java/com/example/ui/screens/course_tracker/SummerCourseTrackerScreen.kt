package com.example.ui.screens.course_tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Committee
import com.example.data.model.Course
import com.example.ui.theme.Primary100
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.Primary900
import com.example.ui.theme.SurfacePure

@Composable
fun SummerCourseTrackerScreen(
    courses: List<Course>,
    selectedCourseId: String?,
    committees: List<Committee>,
    /** Saved sheet rows from the server; rows not linked to a committee are shown read-only. */
    sheetRows: List<com.example.data.remote.SheetRowDto> = emptyList(),
    onSelectCourse: (String?) -> Unit,
    onToggleStage: (committeeId: String, stageKey: String, currentStatus: Boolean) -> Unit,
    onOpenCommitteeDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var courseMenuExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val activeCourse = courses.find { it.id == selectedCourseId } ?: courses.firstOrNull()

    val filteredList = committees.filter { comm ->
        (selectedCourseId == null || comm.courseId == selectedCourseId) &&
                (searchQuery.isBlank() ||
                        comm.committeeName.contains(searchQuery, ignoreCase = true) ||
                        comm.sheikhName.contains(searchQuery, ignoreCase = true) ||
                        comm.committeeNumber.toString() == searchQuery.trim())
    }

    val unlinkedRows = sheetRows.filter { row ->
        row.committeeId == null &&
            (selectedCourseId == null || row.courseId == selectedCourseId) &&
            (searchQuery.isBlank() || row.sheikh_name.contains(searchQuery, ignoreCase = true) || row.number.toString() == searchQuery.trim())
    }

    val totalStudents = filteredList.sumOf { it.studentsCount } + unlinkedRows.sumOf { it.students_count }
    val deliveredCount = filteredList.count { it.stages.exam.completed }
    val correctedCount = filteredList.count { it.stages.scoring.completed }
    val reviewedCount = filteredList.count { it.stages.review.completed }
    val announcedCount = filteredList.count { it.stages.announcement.completed }
    val ceremonyCount = filteredList.count { it.stages.ceremony.completed }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FD))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Course Selector Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("course_selector_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Primary800)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "كشف ومصفوفة متابعة الدورة الصيفية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dropdown trigger
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { courseMenuExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeCourse?.name ?: "كافة الدورات العلمية",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = courseMenuExpanded,
                            onDismissRequest = { courseMenuExpanded = false }
                        ) {
                            courses.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text(course.name) },
                                    onClick = {
                                        onSelectCourse(course.id)
                                        courseMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary KPI Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePure),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryPill(title = "اللجان", value = "${filteredList.size}", color = Primary700)
                    SummaryPill(title = "الطلاب", value = "$totalStudents", color = Color(0xFFD97706))
                    SummaryPill(title = "مستلم 📦", value = "$deliveredCount", color = Color(0xFF2563EB))
                    SummaryPill(title = "مصحح ✏️", value = "$correctedCount", color = Color(0xFFF59E0B))
                    SummaryPill(title = "مكتمل 🟢", value = "$ceremonyCount", color = Color(0xFF16A34A))
                }
            }
        }

        // Search in tracker
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث برقم اللجنة، الشيخ، أو الكتاب المقرر...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tracker_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Committee Matrix Rows
        if (filteredList.isEmpty()) {
            item {
                Text(
                    text = "لا توجد لجان مسجلة تطابق التصفية الحالية.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(unlinkedRows, key = { "sheet-${it.id}" }) { row -> SheetRowCard(row) }

            items(filteredList, key = { it.id }) { committee ->
                TrackerRowCard(
                    committee = committee,
                    onToggleStage = onToggleStage,
                    onOpenDetail = { onOpenCommitteeDetail(committee.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SummaryPill(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 12.sp,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
fun TrackerRowCard(
    committee: Committee,
    onToggleStage: (committeeId: String, stageKey: String, currentStatus: Boolean) -> Unit,
    onOpenDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tracker_row_${committee.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePure),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: #, Name, Students count, Info button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Primary100),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${committee.committeeNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = Primary800
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = committee.committeeName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Primary900
                        )
                        Text(
                            text = "الشيخ: ${committee.sheikhName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${committee.studentsCount} طلاب",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onOpenDetail,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "التفاصيل والمراحل",
                            tint = Primary700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "📖 ${committee.competitionBook}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF0F766E),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Stage Switches Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickSwitchItem(
                    label = "مستلم 📦",
                    checked = committee.stages.exam.completed,
                    onToggle = { onToggleStage(committee.id, "exam", committee.stages.exam.completed) },
                    activeColor = Color(0xFF2563EB),
                    enabled = com.example.data.remote.Permissions.canOn("committees.reception", committee.id)
                )
                QuickSwitchItem(
                    label = "مصحح ✏️",
                    checked = committee.stages.scoring.completed,
                    onToggle = { onToggleStage(committee.id, "scoring", committee.stages.scoring.completed) },
                    activeColor = Color(0xFFD97706),
                    enabled = com.example.data.remote.Permissions.canOn("committees.scoring", committee.id)
                )
                QuickSwitchItem(
                    label = "مراجع 🔍",
                    checked = committee.stages.review.completed,
                    onToggle = { onToggleStage(committee.id, "review", committee.stages.review.completed) },
                    activeColor = Color(0xFFCA8A04),
                    enabled = com.example.data.remote.Permissions.canOn("committees.scoring", committee.id)
                )
                QuickSwitchItem(
                    label = "معلن 📢",
                    checked = committee.stages.announcement.completed,
                    onToggle = { onToggleStage(committee.id, "announcement", committee.stages.announcement.completed) },
                    activeColor = Color(0xFF0891B2),
                    enabled = com.example.data.remote.Permissions.canOn("committees.finalize", committee.id)
                )
                QuickSwitchItem(
                    label = "حفل 🎓",
                    checked = committee.stages.ceremony.completed,
                    onToggle = { onToggleStage(committee.id, "ceremony", committee.stages.ceremony.completed) },
                    activeColor = Color(0xFF16A34A),
                    enabled = com.example.data.remote.Permissions.canOn("committees.finalize", committee.id)
                )
            }
        }
    }
}

@Composable
fun QuickSwitchItem(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    activeColor: Color,
    /** Offered only when the server would accept the change (MOB-10). */
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (checked) activeColor.copy(alpha = 0.15f) else Color.White)
            .border(
                1.dp,
                if (checked) activeColor else Color(0xFFCBD5E1),
                RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (checked) activeColor else Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                color = if (checked) activeColor else Color(0xFF64748B)
            )
        }
    }
}

/** A sheet row that is not linked to a committee: read-only, edited on the web app. */
@Composable
fun SheetRowCard(row: com.example.data.remote.SheetRowDto) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("sheet_row_${row.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePure),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "#${row.number} • ${row.sheikh_name.ifBlank { "بدون اسم شيخ" }}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Primary900
            )
            Text(
                text = listOf(row.place, row.book).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "—" },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "الطلاب: ${row.students_count}" +
                    listOfNotNull(
                        "امتحان ✓".takeIf { row.delivered },
                        "تصحيح ✓".takeIf { row.corrected },
                        "مراجعة ✓".takeIf { row.reviewed },
                        "إعلان ✓".takeIf { row.announced },
                        "حفل ✓".takeIf { row.ceremony }
                    ).joinToString("") { "  $it" },
                style = MaterialTheme.typography.bodySmall
            )
            if (row.uncertain) {
                Text(
                    text = "⚠ يحتاج مراجعة يدوية قبل الاعتماد",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB45309)
                )
            }
        }
    }
}
