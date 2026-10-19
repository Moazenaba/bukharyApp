package com.example.ui.screens.committees

import androidx.compose.material3.minimumInteractiveComponentSize
import android.content.Intent
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import android.widget.Toast
import android.content.ActivityNotFoundException
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
    onSaveCourse: (Course) -> Unit = {},
    allCommittees: List<Committee> = committees,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    // Controls follow the server's evaluated actions and committee scope (MOB-10).
    val canAdd = com.example.data.remote.Permissions.can("committees.create")

    // Only stages that have committees are offered as filters; keeps the row short and every chip meaningful.
    val filterTabs = listOf("all" to "الكل (${committees.size})") + OrderedStageKeys.mapNotNull { (key, _) ->
        val count = committees.count { it.stages.getStage(key).completed }
        if (count == 0 && key != selectedStageFilter) null
        else key to "${StageColorMap[key]?.name ?: ""} ($count)"
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
                placeholder = { Text("ابحث باسم اللجنة أو الشيخ أو الكتاب") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Primary700) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("committees_search_bar"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stage filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterTabs.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedStageFilter == key,
                        onClick = { onStageFilterChange(key) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("filter_tab_$key")
                    )
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
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد لجان مطابقة للبحث أو التصفية الحالية",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF64748B)
                        )
                        if (searchQuery.isNotBlank() || selectedStageFilter != "all") {
                            TextButton(onClick = {
                                onSearchChange("")
                                onStageFilterChange("all")
                            }) { Text("مسح البحث والتصفية") }
                        }
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
                            canDelete = com.example.data.remote.Permissions.canOn("committees.delete", committee.id)
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
            allCommittees = allCommittees,
            onDismiss = { showAddDialog = false },
            onSaveCourse = onSaveCourse,
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
    var confirmDelete by remember { mutableStateOf(false) }
    val doneCount = OrderedStageKeys.count { (key, _) -> committee.stages.getStage(key).completed }
    val totalStages = OrderedStageKeys.size
    val activeName = StageColorMap[activeStageKey]?.name ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail)
            .testTag("committee_card_${committee.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePure),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        fontWeight = FontWeight.Bold,
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
                        text = "${committee.branch} • ${if (committee.isWomenCommittee) "مصلى الأخوات" else "مصلى الرجال"} • ${committee.studentsCount} طالباً",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "كتاب المسابقة",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = committee.competitionBook,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary900
                )
            }

            if (committee.sheikhName.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الشيخ: ${committee.sheikhName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155)
                    )
                    if (committee.sheikhPhone.isNotBlank()) {
                        TextButton(onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${committee.sheikhPhone}")))
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "لا يوجد تطبيق اتصال على هذا الجهاز", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اتصال بالشيخ", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress: one readable fact instead of nine unlabeled dots.
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "المرحلة الحالية: $activeName، أنجزت $doneCount من $totalStages مراحل"
                }
            ) {
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
                    Text(
                        text = "$doneCount من $totalStages",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { doneCount / totalStages.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Primary700,
                    trackColor = Color(0xFFE2E8F0)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canDelete) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف اللجنة",
                            tint = Color(0xFFB91C1C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                TextButton(onClick = onOpenDetail) {
                    Text("المراحل والتفاصيل", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف اللجنة رقم ${committee.committeeNumber}؟") },
            text = { Text("سيتم حذف لجنة ${committee.committeeName} ومراحلها ودرجاتها نهائياً، ولا يمكن التراجع.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("حذف", color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
fun AddCommitteeDialog(
    activeCourses: List<Course>,
    /** Every committee (not the filtered list), to suggest the next free number in the chosen course (MOB-11). */
    allCommittees: List<Committee>,
    onDismiss: () -> Unit,
    onSaveCourse: (Course) -> Unit = {},
    onSave: (Committee) -> Unit
) {
    var number by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var book by remember { mutableStateOf("") }
    var sheikhName by remember { mutableStateOf("") }
    var sheikhPhone by remember { mutableStateOf("") }
    var studentsCount by remember { mutableIntStateOf(0) }
    var isWomen by remember { mutableStateOf(false) }
    var courseId by remember { mutableStateOf(activeCourses.firstOrNull()?.id ?: "") }

    // Auto update default courseId when courses list changes
    androidx.compose.runtime.LaunchedEffect(activeCourses) {
        if (courseId.isBlank() && activeCourses.isNotEmpty()) {
            courseId = activeCourses.first().id
        }
    }

    // Next free number within the chosen course (numbers are unique per course).
    androidx.compose.runtime.LaunchedEffect(courseId) {
        number = (allCommittees.filter { it.courseId == courseId }.maxOfOrNull { it.committeeNumber } ?: 0) + 1
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة لجنة جديدة",
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
                Text("الدورة العلمية", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (activeCourses.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF2F2))
                            .padding(10.dp)
                    ) {
                        Text(
                            "لا توجد دورات مسجلة حالياً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                val newCourse = Course(
                                    id = "course-" + java.util.UUID.randomUUID().toString(),
                                    name = "الدورة العلمية الرئيسية 2026",
                                    season = "صيف",
                                    year = "2026",
                                    status = "active",
                                    isDefault = true,
                                    targetCommitteesCount = 50
                                )
                                onSaveCourse(newCourse)
                                courseId = newCourse.id
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary700)
                        ) {
                            Text("➕ إنشاء الدورة الرئيسية 2026 فوراً", fontSize = 12.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
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
                    // Same starting point as the web: registration done, five questions (MOB-07);
                    // a random id cannot collide with another device's (MOB-11).
                    val newCommittee = Committee(
                        id = "comm-${java.util.UUID.randomUUID()}",
                        committeeNumber = number,
                        committeeName = name.ifBlank { "لجنة رقم $number" },
                        branch = branch,
                        competitionBook = book,
                        sheikhName = sheikhName,
                        sheikhPhone = sheikhPhone,
                        studentsCount = studentsCount,
                        isWomenCommittee = isWomen,
                        courseId = courseId,
                        stages = StagesMap().updateStage("registration", true, null),
                        scoringDetails = com.example.data.model.ScoringDetails(
                            questions = (1..5).map { i ->
                                com.example.data.model.QuestionScoringItem(id = "q-$i", questionNumber = i, label = "السؤال $i")
                            }
                        )
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
