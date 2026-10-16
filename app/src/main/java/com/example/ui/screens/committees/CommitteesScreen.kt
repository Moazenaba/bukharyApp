package com.example.ui.screens.committees

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUser
import com.example.data.model.Committee
import com.example.data.model.Course
import com.example.data.model.StagesMap
import com.example.data.model.SystemPermissions
import com.example.ui.components.OrderedStageKeys
import com.example.ui.components.StageBadge
import com.example.ui.theme.Primary100
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.Primary900
import com.example.ui.theme.StageColorMap
import com.example.ui.theme.SurfacePure

@Composable
fun CommitteesScreen(
    currentUser: AppUser?,
    committees: List<Committee>,
    activeCourses: List<Course>,
    selectedStageFilter: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onStageFilterChange: (String) -> Unit,
    onOpenCommitteeDetail: (String) -> Unit,
    onSaveCommittee: (Committee) -> Unit,
    onDeleteCommittee: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val canAdd = currentUser?.hasPermission(SystemPermissions.ADD_NEW_COMMITTEE) ?: false

    val filterTabs = listOf(
        "all" to "الكل (${committees.size})"
    ) + OrderedStageKeys.map { (key, _) ->
        val count = committees.count { it.stages.getStage(key).completed }
        val emoji = StageColorMap[key]?.emoji ?: ""
        val name = StageColorMap[key]?.name ?: ""
        key to "$emoji $name ($count)"
    }

    Scaffold(
        floatingActionButton = {
            if (canAdd) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Primary700,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_committee")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة لجنة جديدة")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FD))
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("بحث باسم اللجنة، رقمها، اسم الشيخ، أو الكتاب...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Primary700) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("committees_search_bar"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Stage Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterTabs.forEach { (key, label) ->
                    val isSelected = selectedStageFilter == key
                    val stageSet = StageColorMap[key]

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(
                                if (isSelected) Primary700 else Color.White
                            )
                            .border(
                                1.dp,
                                if (isSelected) Primary800 else Color(0xFFE2E8F0),
                                RoundedCornerShape(9999.dp)
                            )
                            .clickable { onStageFilterChange(key) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("filter_tab_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Committees List
            if (committees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🔍",
                            fontSize = 40.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد لجان مطابقة للبحث أو التصفية الحالية",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(committees) { committee ->
                        CommitteeCard(
                            committee = committee,
                            onOpenDetail = { onOpenCommitteeDetail(committee.id) },
                            onDelete = { onDeleteCommittee(committee.id) },
                            canDelete = currentUser?.hasPermission(SystemPermissions.MANAGE_COMMITTEES) ?: false
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCommitteeDialog(
            activeCourses = activeCourses,
            existingCount = committees.size,
            onDismiss = { showAddDialog = false },
            onSave = {
                onSaveCommittee(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CommitteeCard(
    committee: Committee,
    onOpenDetail: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val context = LocalContext.current
    val activeStageKey = committee.stages.getCurrentActiveStage()
    val isCompleted = committee.stages.ceremony.completed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail)
            .testTag("committee_card_${committee.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePure),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: # Badge + Name + Students count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Primary700),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${committee.committeeNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = committee.committeeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary900
                        )
                        Text(
                            text = "${committee.branch} • ${if (committee.isWomenCommittee) "مصلى الأخوات 🧕" else "مصلى الرجال 👨"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${committee.studentsCount} طالباً",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Book & Sheikh
            Text(
                text = "📖 ${committee.competitionBook}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F766E)
            )

            if (committee.sheikhName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الشيخ: ${committee.sheikhName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium
                    )

                    if (committee.sheikhPhone.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            modifier = Modifier.clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${committee.sheikhPhone}"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "اتصال",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF16A34A),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini Progress Dots for 9 Stages
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OrderedStageKeys.forEach { (key, _) ->
                    val done = committee.stages.getStage(key).completed
                    val stageSet = StageColorMap[key]

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (done) stageSet?.dot ?: Primary700 else Color(0xFFCBD5E1)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Current Active Stage Badge + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StageBadge(
                    stageKey = activeStageKey,
                    completed = true,
                    showActiveLabel = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف اللجنة",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onOpenDetail,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary700),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "المراحل والتفاصيل ⬅",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddCommitteeDialog(
    activeCourses: List<Course>,
    existingCount: Int,
    onDismiss: () -> Unit,
    onSave: (Committee) -> Unit
) {
    var number by remember { mutableIntStateOf(existingCount + 1) }
    var name by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var book by remember { mutableStateOf("") }
    var sheikhName by remember { mutableStateOf("") }
    var sheikhPhone by remember { mutableStateOf("") }
    var studentsCount by remember { mutableIntStateOf(0) }
    var isWomen by remember { mutableStateOf(false) }
    var courseId by remember { mutableStateOf(activeCourses.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "➕ إضافة وتوثيق لجنة جديدة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Primary900
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم اللجنة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = book,
                    onValueChange = { book = it },
                    label = { Text("الكتاب المقرر") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // The committee must belong to a course (same rule as the web app and the server).
                Text("الدورة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (activeCourses.isEmpty()) {
                    Text(
                        "لا توجد دورات. أضف دورة من الويب أولاً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color(0xFFB91C1C)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeCourses.forEach { course ->
                            FilterChip(
                                selected = courseId == course.id,
                                onClick = { courseId = course.id },
                                label = { Text(course.name, maxLines = 1) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = sheikhName,
                    onValueChange = { sheikhName = it },
                    label = { Text("اسم الشيخ المسؤول / المقرئ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sheikhPhone,
                    onValueChange = { sheikhPhone = it },
                    label = { Text("هاتف الشيخ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = studentsCount.toString(),
                        onValueChange = { studentsCount = it.toIntOrNull() ?: 0 },
                        label = { Text("عدد الطلاب") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = number.toString(),
                        onValueChange = { number = it.toIntOrNull() ?: 1 },
                        label = { Text("رقم اللجنة") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable { isWomen = !isWomen }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = isWomen,
                        onCheckedChange = { isWomen = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isWomen) "لجنة مصلى الأخوات 🧕" else "لجنة مصلى الرجال 👨",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = courseId.isNotBlank() && number > 0,
                onClick = {
                    val newCommittee = Committee(
                        id = "comm-${System.currentTimeMillis()}",
                        committeeNumber = number,
                        committeeName = name.ifBlank { "لجنة رقم $number" },
                        branch = branch,
                        competitionBook = book,
                        sheikhName = sheikhName,
                        sheikhPhone = sheikhPhone,
                        studentsCount = studentsCount,
                        isWomenCommittee = isWomen,
                        courseId = courseId,
                        stages = StagesMap()
                    )
                    onSave(newCommittee)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary700)
            ) {
                Text("حفظ وتسجيل اللجنة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
