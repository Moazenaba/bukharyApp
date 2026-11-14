package com.example.ui.screens.exams

import com.example.ui.components.ChipFlow
import com.example.ui.theme.tEdge
import com.example.ui.theme.tBg
import com.example.ui.theme.tFg
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import com.example.ui.theme.TextMuted
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.minimumInteractiveComponentSize
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.data.remote.BookDto
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUser
import com.example.data.model.Exam
import com.example.data.model.SystemPermissions
import com.example.ui.theme.Primary100
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.Primary900
import com.example.ui.theme.SurfacePure

@Composable
fun ExamsScreen(
    currentUser: AppUser?,
    exams: List<Exam>,
    onSaveExam: (Exam, Uri?) -> Unit,
    onDeleteExam: (String) -> Unit,
    onOpenFile: (Exam) -> Unit = {},
    books: List<BookDto> = emptyList(),
    onUploadFile: (String, Uri) -> Unit = { _, _ -> },
    onAddBook: (String) -> Unit = {},
    onDeleteBook: (BookDto) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var bookFilter by remember { mutableStateOf("all") }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddBook by remember { mutableStateOf(false) }
    var previewExam by remember { mutableStateOf<Exam?>(null) }
    var confirmDeleteExam by remember { mutableStateOf<Exam?>(null) }
    var confirmDeleteBook by remember { mutableStateOf<BookDto?>(null) }
    var uploadFor by remember { mutableStateOf<String?>(null) }
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val examId = uploadFor
        uploadFor = null
        if (uri != null && examId != null) onUploadFile(examId, uri)
    }

    // What the server accepts from this user (same rules as the web app).
    val canManage = com.example.data.remote.Permissions.can("exams.write")
    val canEditBooks = com.example.data.remote.Permissions.can("books.write")
    val examScope = com.example.data.remote.Permissions.examScope
    val bookNames = (books.map { it.name } + exams.map { it.book }).filter { it.isNotBlank() }.distinct().sorted()

    val filtered = exams.filter { exam ->
        (bookFilter == "all" || exam.book == bookFilter) && (
            searchQuery.isBlank() ||
                exam.title.contains(searchQuery, ignoreCase = true) ||
                exam.book.contains(searchQuery, ignoreCase = true) ||
                exam.branch.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        floatingActionButton = {
            if (tab == 0 && canManage) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = tBg(Primary700),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_exam")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة امتحان ورفع PDF")
                }
            } else if (tab == 1 && canEditBooks) {
                FloatingActionButton(onClick = { showAddBook = true }, containerColor = tBg(Color(0xFFB45309)), contentColor = Color.White) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة كتاب مقرر")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(tBg(Color(0xFFF8F9FD)))
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
            Column {
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = tBg(Primary800))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("📚 الامتحانات ونماذج PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.White)
                        Text("نماذج الاختبارات المعتمدة والكتب والمتون المقررة", style = MaterialTheme.typography.bodySmall, color = tFg(Primary100))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("نماذج الامتحانات", "${exams.size}", Modifier.weight(1f))
                MiniStat("الكتب المقررة", "${bookNames.size}", Modifier.weight(1f))
                MiniStat("ملفات PDF", "${exams.count { it.hasFile }}", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            TabRow(selectedTabIndex = tab, containerColor = tBg(Color.White)) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("نماذج الامتحانات وPDF") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("الكتب والمتون المقررة") })
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (examScope != null && tab == 0) {
                Text(
                    "نطاق الصلاحية المخصص: تفتح ملفات الامتحانات المصرح لك بها فقط (${examScope.size}).",
                    style = MaterialTheme.typography.labelSmall, color = tFg(Color(0xFF92400E)),
                    modifier = Modifier.fillMaxWidth().background(tBg(Color(0xFFFEF3C7)), RoundedCornerShape(8.dp)).padding(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (tab == 0) "بحث في الامتحانات بالاسم أو الكتاب أو الفرع..." else "بحث في الكتب والمتون المقررة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = tFg(Primary700)) },
                modifier = Modifier.fillMaxWidth().testTag("input_exam_search"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            }
            }
            if (tab == 0) {
                item {
                ChipFlow(Modifier.fillMaxWidth().padding(vertical = 6.dp), 6.dp) {
                    FilterChip(selected = bookFilter == "all", onClick = { bookFilter = "all" }, label = { Text("كافة الكتب (${exams.size})") })
                    bookNames.forEach { b -> FilterChip(selected = bookFilter == b, onClick = { bookFilter = b }, label = { Text("$b (${exams.count { it.book == b }})") }) }
                }
                }
                if (filtered.isEmpty()) {
                    item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد امتحانات مطابقة للبحث", style = MaterialTheme.typography.bodyMedium, color = tFg(TextMuted))
                    }
                    }
                } else {
                    run {
                        items(filtered, key = { it.id }) { exam ->
                            ExamCard(
                                exam = exam,
                                onPreview = { if (exam.hasFile && com.example.data.remote.Permissions.canOpenExam(exam.id)) onOpenFile(exam) else previewExam = exam },
                                onDelete = { confirmDeleteExam = exam },
                                canDelete = com.example.data.remote.Permissions.can("exams.delete"),
                                onUpload = if (canManage) ({ uploadFor = exam.id; pickPdf.launch("application/pdf") }) else null
                            )
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            } else {
                val shownBooks = bookNames.filter { searchQuery.isBlank() || it.contains(searchQuery, ignoreCase = true) }
                run {
                    if (shownBooks.isEmpty()) item { Text("لا توجد كتب مقررة مطابقة", color = tFg(TextMuted), modifier = Modifier.padding(16.dp)) }
                    items(shownBooks, key = { it }) { name ->
                        val dto = books.find { it.name == name }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = tBg(Color(0xFFFFFBEB))),
                            border = androidx.compose.foundation.BorderStroke(1.dp, tEdge(Color(0xFFFDE68A)))
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = tFg(Color(0xFFB45309)))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.Bold, color = tFg(Color(0xFF78350F)))
                                    Text("نماذج الامتحانات المرفوعة: ${exams.count { it.book == name }} • متون علمية معتمدة", style = MaterialTheme.typography.labelSmall, color = tFg(Color(0xFF92400E)))
                                }
                                TextButton(onClick = { tab = 0; bookFilter = name; searchQuery = "" }) { Text("الامتحانات") }
                                if (canEditBooks && dto != null) {
                                    IconButton(onClick = { confirmDeleteBook = dto }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف هذا الكتاب", tint = tFg(Color(0xFFEF4444)))
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExamDialog(
            bookSuggestions = bookNames,
            onDismiss = { showAddDialog = false },
            onSave = { exam, pdf ->
                onSaveExam(exam, pdf)
                showAddDialog = false
            }
        )
    }
    if (showAddBook) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBook = false },
            title = { Text("إضافة كتاب مقرر جديد", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(name, { name = it }, label = { Text("اسم الكتاب / المتن *") }, placeholder = { Text("مثال: كتاب الأصول الثلاثة") }, singleLine = true)
            },
            confirmButton = {
                Button(onClick = { onAddBook(name.trim()); showAddBook = false }, enabled = name.isNotBlank()) { Text("حفظ الكتاب واعتماده") }
            },
            dismissButton = { TextButton(onClick = { showAddBook = false }) { Text("إلغاء") } }
        )
    }
    confirmDeleteExam?.let { e ->
        AlertDialog(
            onDismissRequest = { confirmDeleteExam = null },
            title = { Text("حذف الامتحان") },
            text = { Text("هل تريد حذف «${e.title}» نهائياً؟") },
            confirmButton = { TextButton(onClick = { confirmDeleteExam = null; onDeleteExam(e.id) }) { Text("حذف", color = tFg(Color(0xFFDC2626))) } },
            dismissButton = { TextButton(onClick = { confirmDeleteExam = null }) { Text("إلغاء") } }
        )
    }
    confirmDeleteBook?.let { b ->
        AlertDialog(
            onDismissRequest = { confirmDeleteBook = null },
            title = { Text("حذف الكتاب") },
            text = { Text("هل تريد حذف «${b.name}» من الكتب المقررة؟") },
            confirmButton = { TextButton(onClick = { confirmDeleteBook = null; onDeleteBook(b) }) { Text("حذف", color = tFg(Color(0xFFDC2626))) } },
            dismissButton = { TextButton(onClick = { confirmDeleteBook = null }) { Text("إلغاء") } }
        )
    }

    previewExam?.let { exam ->
        ExamPreviewDialog(exam = exam, onDismiss = { previewExam = null })
    }
}

@Composable
private fun MiniStat(title: String, value: String, modifier: Modifier) {
    Column(modifier.background(tBg(Color.White), RoundedCornerShape(12.dp)).border(1.dp, tEdge(Color(0xFFE2E8F0)), RoundedCornerShape(12.dp)).padding(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = tFg(TextMuted))
        Text(value, fontWeight = FontWeight.Bold, color = tFg(Primary700))
    }
}

@Composable
fun ExamCard(
    exam: Exam,
    onPreview: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean,
    onUpload: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onPreview)
            .testTag("exam_card_${exam.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = tBg(SurfacePure)),
        border = androidx.compose.foundation.BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0))),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(tBg(Color(0xFFFEE2E2))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = tFg(Color(0xFFDC2626)),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = exam.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = tFg(Primary900)
                        )
                        Text(
                            text = exam.book,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = tFg(Color(0xFF0F766E)),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = tFg(Color(0xFFEF4444)), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Web: «مقفول — تُتاح الورقة …» until shortly before the exam.
            ExamLock.text(exam)?.let { (locked, text) ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (locked) tFg(Color(0xFF92400E)) else tFg(Color(0xFF475569)),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (locked) tBg(Color(0xFFFFFBEB)) else tBg(Color(0xFFF8FAFC)))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tBg(Color(0xFFF1F5F9)))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "الدرجة: ${exam.totalMarks}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tFg(Color(0xFF334155))
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tBg(Color(0xFFEFF6FF)))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "المدة: ${exam.durationMinutes} دقيقة",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tFg(Color(0xFF1D4ED8))
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tBg(Color(0xFFFEF3C7)))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${exam.questionsCount} أسئلة",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tFg(Color(0xFFB45309))
                    )
                }
            }

            if (exam.branch.isNotBlank() || exam.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                if (exam.branch.isNotBlank()) Text("الفرع: ${exam.branch}", style = MaterialTheme.typography.labelSmall, color = tFg(Color(0xFF475569)))
                if (exam.notes.isNotBlank()) Text("تعليمات: ${exam.notes}", style = MaterialTheme.typography.labelSmall, color = tFg(Color(0xFF475569)))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (exam.hasFile) "📄 ${exam.pdfFileName.ifBlank { "نموذج_امتحان.pdf" }} ${exam.pdfFileSize}" else "لا يوجد ملف PDF بعد",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = tFg(TextMuted)
                    )
                    if (onUpload != null) {
                        TextButton(onClick = onUpload, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                            Text(if (exam.hasFile) "استبدال ملف PDF" else "⬆ رفع ملف PDF", style = MaterialTheme.typography.labelSmall, color = tFg(Color(0xFF2563EB)))
                        }
                    }
                }

                Button(
                    onClick = onPreview,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tBg(Primary700)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("معاينة الأسئلة 👁️", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ExamPreviewDialog(
    exam: Exam,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = exam.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = tFg(Primary900)
                )
                Text(
                    text = "الكتاب: ${exam.book} • الدرجة الكلية: ${exam.totalMarks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = tFg(Color(0xFF0F766E))
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "📋 بنك الأسئلة المعتمد للنموذج:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = tFg(Primary800)
                )

                if (exam.sampleQuestions.isEmpty()) {
                    Text(
                        text = "النموذج متاح كملف PDF ورقي تم توزيعه على اللجان.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tFg(TextMuted)
                    )
                } else {
                    exam.sampleQuestions.forEach { question ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = tBg(Color(0xFFF8FAFC)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, tEdge(Color(0xFFE2E8F0))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = question,
                                style = MaterialTheme.typography.bodySmall,
                                color = tFg(Color(0xFF1E293B)),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tBg(Color(0xFFDCFCE7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = tFg(Color(0xFF16A34A)), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ملف PDF جاهز للطباعة وتوزيعه على قاعات الاختبار",
                            style = MaterialTheme.typography.labelSmall,
                            color = tFg(Color(0xFF15803D)),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = tBg(Primary700))
            ) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun AddExamDialog(
    onDismiss: () -> Unit,
    onSave: (Exam, Uri?) -> Unit,
    bookSuggestions: List<String> = emptyList()
) {
    var notes by remember { mutableStateOf("") }
    var pdf by remember { mutableStateOf<Uri?>(null) }
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) pdf = uri }
    var title by remember { mutableStateOf("") }
    var book by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var totalMarks by remember { mutableIntStateOf(100) }
    var durationMinutes by remember { mutableIntStateOf(60) }
    var questionsCount by remember { mutableIntStateOf(0) }
    var questionsInput by remember { mutableStateOf("") }

    val branches = listOf("الفرع العام", "فرع المتقدمين", "فرع المتوسطين", "فرع المبتدئين", "فرع البراعم")


    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "➕ رفع وإضافة نموذج امتحان جديد",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tFg(Primary900)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الامتحان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = book,
                    onValueChange = { book = it },
                    label = { Text("الكتاب المقرر") },
                    placeholder = { Text("اختر أو اكتب اسم الكتاب...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (bookSuggestions.isNotEmpty()) {
                    ChipFlow(Modifier.fillMaxWidth(), 6.dp) {
                        bookSuggestions.filter { book.isBlank() || it.contains(book) }.take(12).forEach { b ->
                            FilterChip(selected = book == b, onClick = { book = b }, label = { Text(b) })
                        }
                    }
                }

                Text(
                    text = "الفرع المستهدف:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = tFg(Primary800)
                )

                ChipFlow(Modifier.fillMaxWidth(), 6.dp) {
                    branches.forEach { bName ->
                        val isSel = branch == bName
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) tBg(Primary700) else tBg(Color(0xFFF1F5F9)))
                                .minimumInteractiveComponentSize().clickable { branch = bName }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = bName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else tFg(Color(0xFF475569))
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = totalMarks.toString(),
                        onValueChange = { totalMarks = it.toIntOrNull() ?: 100 },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        label = { Text("الدرجة") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = durationMinutes.toString(),
                        onValueChange = { durationMinutes = it.toIntOrNull() ?: 60 },
                        label = { Text("المدة (د)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = if (questionsCount == 0) "" else questionsCount.toString(),
                    onValueChange = { questionsCount = it.filter(Char::isDigit).toIntOrNull() ?: 0 },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    label = { Text("عدد الأسئلة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // The question PDF is uploaded right after the exam is saved, as on the web.
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = tBg(Color(0xFFEFF6FF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tEdge(Color(0xFFBFDBFE))),
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { pickPdf.launch("application/pdf") }
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = tFg(Color(0xFF2563EB)), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (pdf != null) "تم اختيار ملف PDF ✓ (اضغط للتغيير)" else "اختيار ملف الأسئلة PDF (اختياري)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = tFg(Color(0xFF1D4ED8))
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("تعليمات خاصة للمصححين أو توزيع الدرجات") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = questionsInput,
                    onValueChange = { questionsInput = it },
                    label = { Text("الأسئلة (افصل بينها بسطر جديد)") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val qList = questionsInput.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                        val exam = Exam(
                            id = "exam-${java.util.UUID.randomUUID()}",
                            title = title,
                            book = book,
                            branch = branch,
                            totalMarks = totalMarks,
                            durationMinutes = durationMinutes,
                            questionsCount = if (qList.isNotEmpty()) qList.size else questionsCount,
                            sampleQuestions = qList,
                            hasFile = false,
                            uploadedAt = "",
                            notes = notes.trim()
                        )
                        onSave(exam, pdf)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = tBg(Primary700))
            ) {
                Text("حفظ النموذج")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
