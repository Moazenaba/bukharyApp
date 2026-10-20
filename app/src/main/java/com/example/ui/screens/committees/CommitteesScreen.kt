package com.example.ui.screens.committees

import androidx.compose.foundation.verticalScroll

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
import androidx.compose.material3.OutlinedButton
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
    allCommittees: List<Committee> = committees,
    selectedCourseId: String? = null,
    onCourseFilterChange: (String?) -> Unit = {},
    filters: com.example.ui.MainViewModel.CommitteeFilters = com.example.ui.MainViewModel.CommitteeFilters(),
    onFiltersChange: (com.example.ui.MainViewModel.CommitteeFilters) -> Unit = {},
    onReceive: (String, Boolean) -> Unit = { _, _ -> },
    onApproveReadiness: (String, Boolean) -> Unit = { _, _ -> },
    openAddRequested: Boolean = false,
    onAddRequestConsumed: () -> Unit = {},
    onCreateCourse: ((Course) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(openAddRequested) {
        if (openAddRequested) { showAddDialog = true; onAddRequestConsumed() }
    }
    var page by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(1) }
    androidx.compose.runtime.LaunchedEffect(searchQuery, selectedStageFilter, selectedCourseId, filters) { page = 1 }

    // Controls follow the server's evaluated actions and committee scope (MOB-10).
    val canAdd = com.example.data.remote.Permissions.can("committees.create")

    // Only stages that have committees are offered as filters; keeps the row short and every chip meaningful.
    val filterTabs = listOf("all" to "الكل (${allCommittees.size})") + OrderedStageKeys.mapNotNull { (key, _) ->
        val count = allCommittees.count { com.example.ui.components.currentStageKey(it.stages) == key }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FD))
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
            Column {
            Spacer(modifier = Modifier.height(12.dp))

            // Assistant bar (web): assigned committees, my committees only, unscored questions.
            if (currentUser?.role == com.example.data.model.UserRole.ASSISTANT) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFECFDF5)).padding(10.dp)
                ) {
                    Text(
                        "اللجان المسندة إليك: " + if (currentUser.allCommitteesAllowed) "كل اللجان"
                        else currentUser.assignedCommittees.joinToString("، ") { "#$it" }.ifBlank { "لم تُسند إليك لجان بعد" },
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF065F46)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = filters.mineOnly, onClick = { onFiltersChange(filters.copy(mineOnly = !filters.mineOnly, needsScoring = false)) }, label = { Text("لجاني فقط") })
                        FilterChip(selected = filters.needsScoring, onClick = { onFiltersChange(filters.copy(needsScoring = !filters.needsScoring, mineOnly = false)) }, label = { Text("أسئلة لم تُصحح") })
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Hero stats (web: HeroStats)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.example.ui.components.StatTile("إجمالي اللجان", "${allCommittees.size}", Modifier.weight(1f))
                com.example.ui.components.StatTile("لجان نشطة", "${allCommittees.count { !it.stages.ceremony.completed }}", Modifier.weight(1f), tint = Color(0xFF1D4ED8))
                com.example.ui.components.StatTile("تم الحفل", "${allCommittees.count { it.stages.ceremony.completed }}", Modifier.weight(1f), tint = Color(0xFF047857))
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("بحث برقم اللجنة أو الاسم أو الشيخ", maxLines = 1) },
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

            // Web: the «جاري الامتحان الآن» sheet (sheikh, country, students and proctors of each committee)
            val examCount = com.example.ui.screens.export.ReportSheets.examInProgress(allCommittees).size
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = {
                    com.example.ui.components.shareFile(
                        context,
                        com.example.data.SimpleXlsx.write(
                            "جاري الامتحان الآن", com.example.ui.screens.export.ReportSheets.EXAM_HEADER,
                            com.example.ui.screens.export.ReportSheets.examRows(allCommittees), com.example.ui.screens.export.ReportSheets.EXAM_WIDTHS
                        ),
                        "كشف_جاري_الامتحان_الآن_${com.example.ui.components.Dates.today()}.xlsx", com.example.ui.components.XLSX_MIME
                    )
                },
                enabled = examCount > 0,
                modifier = Modifier.weight(1f)
            ) { Text("كشف «جاري الامتحان الآن» Excel ($examCount)") }
            OutlinedButton(
                onClick = {
                    com.example.ui.components.printHtml(
                        context, "كشف جاري الامتحان الآن",
                        com.example.ui.screens.export.ReportSheets.examPrintBody(allCommittees, com.example.ui.components.Dates.today())
                    )
                },
                enabled = examCount > 0
            ) { Text("طباعة / PDF") }
            }

            // Course, gender and sort (web: SearchAndFilters)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(selected = selectedCourseId == null, onClick = { onCourseFilterChange(null) }, label = { Text("كل الدورات") })
                activeCourses.forEach { c ->
                    FilterChip(selected = selectedCourseId == c.id, onClick = { onCourseFilterChange(c.id) }, label = { Text(c.name) })
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("all" to "الكل", "men" to "رجال", "women" to "نسائية").forEach { (k, l) ->
                    FilterChip(selected = filters.gender == k, onClick = { onFiltersChange(filters.copy(gender = k)) }, label = { Text(l) })
                }
                Text("|", color = Color(0xFFCBD5E1), modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
                listOf("newest" to "الأحدث", "oldest" to "الأقدم", "committeeNumber" to "رقم اللجنة", "book" to "الكتاب").forEach { (k, l) ->
                    FilterChip(selected = filters.sortBy == k, onClick = { onFiltersChange(filters.copy(sortBy = k)) }, label = { Text(l) })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            }
            }
            // Committees List
            if (committees.isEmpty()) {
                item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
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
            }
            } else {
                    val pageSize = 6
                    val pages = (committees.size + pageSize - 1) / pageSize
                    val current = page.coerceIn(1, maxOf(1, pages))
                    items(committees.drop((current - 1) * pageSize).take(pageSize), key = { it.id }) { committee ->
                        CommitteeCard(
                            committee = committee,
                            onOpenDetail = { onOpenCommitteeDetail(committee.id) },
                            onDelete = { onDeleteCommittee(committee.id) },
                            canDelete = com.example.data.remote.Permissions.canOn("committees.delete", committee.id),
                            onReceive = if (com.example.data.remote.Permissions.canOn("committees.reception", committee.id)) ({ onReceive(committee.id, committee.receivedByAssistant != null) }) else null,
                            onApproveReadiness = if (com.example.data.remote.Permissions.canOn("committees.reception", committee.id)) ({ onApproveReadiness(committee.id, committee.examApprovedBy != null) }) else null
                        )
                    }
                    item {
                        // Web: 6 per page with numbered pages.
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "عرض ${if (committees.isEmpty()) 0 else (current - 1) * pageSize + 1} إلى ${minOf(current * pageSize, committees.size)} من إجمالي ${committees.size} لجنة مسجلة",
                                style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B)
                            )
                            if (pages > 1) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { page = current - 1 }, enabled = current > 1) { Text("السابق") }
                                (1..pages).forEach { n ->
                                    FilterChip(selected = n == current, onClick = { page = n }, label = { Text("$n") })
                                }
                                TextButton(onClick = { page = current + 1 }, enabled = current < pages) { Text("التالي") }
                            }
                        }
                        Spacer(modifier = Modifier.height(60.dp))
                    }
            }
        }
    }

    if (showAddDialog) {
        AddCommitteeDialog(
            activeCourses = activeCourses,
            allCommittees = allCommittees,
            onDismiss = { showAddDialog = false },
            onCreateCourse = onCreateCourse,
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
    canDelete: Boolean,
    onReceive: (() -> Unit)? = null,
    onApproveReadiness: (() -> Unit)? = null
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

            // Web: reception and exam readiness, each with who and when, straight from the card.
            if (onReceive != null || onApproveReadiness != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onReceive != null) {
                        OutlinedButton(onClick = onReceive, modifier = Modifier.weight(1f)) {
                            Text(if (committee.receivedByAssistant != null) "إلغاء الاستلام" else "📥 توثيق الاستلام", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (onApproveReadiness != null) {
                        OutlinedButton(onClick = onApproveReadiness, modifier = Modifier.weight(1f)) {
                            Text(if (committee.examApprovedBy != null) "إلغاء الجاهزية" else "✅ اعتماد الجاهزية", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                committee.receivedByAssistant?.let { Text("استلمها: ${it.receivedByName} — ${com.example.ui.components.Dates.displayTime(it.receivedAt)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF047857)) }
                committee.examApprovedBy?.let { Text("اعتمد الجاهزية: ${it.approvedByName} — ${com.example.ui.components.Dates.displayTime(it.approvedAt)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1D4ED8)) }
            }

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
    onSave: (Committee) -> Unit,
    /** Creates a course from inside the form (web: «➕ إضافة دورة جديدة»). */
    onCreateCourse: ((Course) -> Unit)? = null
) {
    var number by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }
    var association by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var book by remember { mutableStateOf("") }
    var sheikhName by remember { mutableStateOf("") }
    var sheikhPhone by remember { mutableStateOf("") }
    var sheikhCountry by remember { mutableStateOf("") }
    var studentsCount by remember { mutableStateOf("") }
    var questionsCount by remember { mutableIntStateOf(5) }
    var isWomen by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var members by remember { mutableStateOf(listOf("")) }
    var initialStage by remember { mutableStateOf("exam") }
    var newCourseName by remember { mutableStateOf("") }
    var courseId by remember {
        mutableStateOf(activeCourses.find { it.isDefault }?.id ?: activeCourses.find { it.status == "active" }?.id ?: activeCourses.firstOrNull()?.id ?: "")
    }
    androidx.compose.runtime.LaunchedEffect(activeCourses) {
        if (courseId.isBlank() || activeCourses.none { it.id == courseId }) courseId = activeCourses.find { it.isDefault }?.id ?: activeCourses.firstOrNull()?.id ?: ""
    }
    // Next free number within the chosen course (numbers are unique per course).
    androidx.compose.runtime.LaunchedEffect(courseId) {
        number = (allCommittees.filter { it.courseId == courseId }.maxOfOrNull { it.committeeNumber } ?: 0) + 1
    }
    val initialStages = listOf(
        "exam" to "🟦 جاري الامتحان الآن", "scoring" to "🟠 في التصحيح", "aggregation" to "🔵 في التجميع",
        "sheet" to "🟣 كتابة الكشف", "review" to "🟡 في المراجعة", "announcement" to "📢 إعلان النتيجة", "ceremony" to "🟢 تم الحفل"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة لجنة جديدة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary900) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // The committee must belong to a course (same rule as the web app and the server).
                Text("الدورة *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    activeCourses.forEach { course ->
                        FilterChip(selected = courseId == course.id, onClick = { courseId = course.id }, label = { Text(course.name, maxLines = 1) })
                    }
                }
                if (onCreateCourse != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(newCourseName, { newCourseName = it }, label = { Text("➕ إضافة دورة جديدة") }, singleLine = true, modifier = Modifier.weight(1f))
                        TextButton(enabled = newCourseName.isNotBlank(), onClick = {
                            val c = Course(id = "course-${java.util.UUID.randomUUID()}", name = newCourseName.trim(), startDate = com.example.ui.components.Dates.today(), status = "active")
                            onCreateCourse(c)
                            courseId = c.id
                            newCourseName = ""
                        }) { Text("إنشاء") }
                    }
                } else if (activeCourses.isEmpty()) {
                    Text("لا توجد دورات. أضف دورة أولاً.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB91C1C))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = number.toString(), onValueChange = { number = it.filter(Char::isDigit).toIntOrNull() ?: 0 },
                        label = { Text("رقم اللجنة") }, singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(name, { name = it }, label = { Text("اسم اللجنة *") }, singleLine = true, modifier = Modifier.weight(2f))
                }
                Text("المرحلة الحالية للجنة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    initialStages.forEach { (k, l) -> FilterChip(selected = initialStage == k, onClick = { initialStage = k }, label = { Text(l) }) }
                }
                OutlinedTextField(book, { book = it }, label = { Text("الكتاب المقرر / مسار المسابقة") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(branch, { branch = it }, label = { Text("الفرع") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(association, { association = it }, label = { Text("اسم الجمعية (إن وجد)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(sheikhName, { sheikhName = it }, label = { Text("الشيخ المرسل إليه") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(sheikhPhone, { sheikhPhone = it }, label = { Text("هاتف الشيخ") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(sheikhCountry, { sheikhCountry = it }, label = { Text("بلد الشيخ") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(studentsCount, { studentsCount = it.filter(Char::isDigit) }, label = { Text("عدد الطلاب") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        questionsCount.toString(), { questionsCount = it.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 30) ?: 1 },
                        label = { Text("عدد الأسئلة") }, singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
                Text("أعضاء اللجنة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                members.forEachIndexed { i, m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(m, { v -> members = members.mapIndexed { j, x -> if (j == i) v else x } }, label = { Text("عضو ${i + 1}") }, singleLine = true, modifier = Modifier.weight(1f))
                        TextButton(onClick = { members = if (members.size == 1) listOf("") else members.filterIndexed { j, _ -> j != i } }) { Text("✕") }
                    }
                }
                TextButton(onClick = { members = members + "" }) { Text("+ إضافة عضو") }
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F5F9)).clickable { isWomen = !isWomen }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Checkbox(checked = isWomen, onCheckedChange = { isWomen = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isWomen) "لجنة نسائية 🧕" else "لجنة رجال 👨", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات عامة") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                enabled = courseId.isNotBlank() && number > 0 && name.isNotBlank(),
                onClick = {
                    val finalMembers = members.map { it.trim() }.filter { it.isNotEmpty() }
                    // Stages before the chosen current stage are completed, as on the web; the server stamps the time.
                    val order = listOf("exam", "scoring", "aggregation", "review", "sheet", "accreditation", "announcement", "ceremony")
                    val upto = when (initialStage) {
                        "exam" -> 0; "scoring" -> 1; "aggregation" -> 2; "review" -> 3; "sheet" -> 4
                        "announcement" -> 6; "ceremony" -> 8; else -> 0
                    }
                    var stages = StagesMap().updateStage("registration", true, null)
                    order.take(upto).forEach { stages = stages.updateStage(it, true, null) }
                    val newCommittee = Committee(
                        id = "comm-${java.util.UUID.randomUUID()}",
                        committeeNumber = number,
                        committeeName = name.trim(),
                        associationName = association.trim(),
                        members = finalMembers,
                        branch = branch.trim(),
                        competitionBook = book.trim(),
                        generalNotes = notes.trim(),
                        sheikhName = sheikhName.trim(),
                        sheikhPhone = sheikhPhone.trim(),
                        sheikhCountry = sheikhCountry.trim(),
                        studentsCount = studentsCount.toIntOrNull() ?: 0,
                        isWomenCommittee = isWomen,
                        courseId = courseId,
                        courseName = activeCourses.find { it.id == courseId }?.name ?: "",
                        stages = stages,
                        createdDate = com.example.ui.components.Dates.today(),
                        scoringDetails = com.example.data.model.ScoringDetails(
                            examinerName = finalMembers.getOrNull(0) ?: "",
                            reviewerName = finalMembers.getOrNull(1) ?: "",
                            questions = (1..questionsCount).map { i ->
                                com.example.data.model.QuestionScoringItem(id = "q-$i", questionNumber = i, label = "السؤال $i")
                            }
                        ),
                        aggregationDetails = com.example.data.model.AggregationDetails(
                            aggregatorName = finalMembers.getOrNull(1) ?: finalMembers.getOrNull(0) ?: "",
                            scoringReviewerName = finalMembers.getOrNull(0) ?: "",
                            reviewerName = finalMembers.getOrNull(2) ?: ""
                        )
                    )
                    onSave(newCommittee)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary700)
            ) { Text("حفظ وتسجيل اللجنة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
