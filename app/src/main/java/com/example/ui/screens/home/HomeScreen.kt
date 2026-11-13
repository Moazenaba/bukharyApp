package com.example.ui.screens.home

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import com.example.ui.theme.TextMuted
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.data.model.SystemPermissions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUser
import com.example.data.model.Committee
import com.example.data.model.Course
import com.example.data.model.ScheduleTask
import com.example.ui.AppNavScreen
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.Primary100
import com.example.ui.theme.Primary200
import com.example.ui.theme.Primary500
import com.example.ui.theme.Primary700
import com.example.ui.theme.Primary800
import com.example.ui.theme.Primary900
import com.example.ui.theme.PriorityHighBg
import com.example.ui.theme.PriorityHighDot
import com.example.ui.theme.PriorityHighText
import com.example.ui.theme.PriorityNormalBg
import com.example.ui.theme.PriorityNormalDot
import com.example.ui.theme.PriorityNormalText
import com.example.ui.theme.PriorityUrgentBg
import com.example.ui.theme.PriorityUrgentDot
import com.example.ui.theme.PriorityUrgentText
import com.example.ui.theme.SurfacePure

@Composable
fun HomeScreen(
    currentUser: AppUser?,
    activeCourse: Course?,
    committees: List<Committee>,
    tasks: List<ScheduleTask>,
    onNavigate: (AppNavScreen) -> Unit,
    onOpenCommitteeDetail: (String) -> Unit,
    onToggleTask: (String) -> Unit,
    onOpenAddCommittee: () -> Unit,
    /** Opens the committees list filtered by a stage ("all" for none), like the web's dashboard tiles. */
    onNavigateCommittees: (String) -> Unit = { onNavigate(AppNavScreen.COMMITTEES) },
    /** The signed-in user's own ceremonies («حفلاتي») shown at the top. */
    myCeremonies: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalCommittees = committees.size
    // Web: committees waiting for their exam or whose exam is done and not yet scored.
    val activeCommittees = remember(committees) { committees.count { com.example.ui.components.currentStageKey(it.stages) in setOf("registration", "exam") } }
    val completedCommittees = remember(committees) { committees.count { it.stages.ceremony.completed } }
    val totalStudents = remember(committees) { committees.sumOf { it.studentsCount } }

    val urgentTasks = remember(tasks) {
        tasks.filter { !it.completed }
            .sortedWith(
                compareByDescending<ScheduleTask> { it.priority == "urgent" }
                    .thenByDescending { it.priority == "high" }
                    .thenBy { it.date }
            )
    }

    val upcomingCeremonies = remember(committees) {
        committees.filter { it.ceremonyDetails.date.isNotBlank() }
            .sortedBy { it.ceremonyDetails.date }
    }

    val canAddCommittee = com.example.data.remote.Permissions.can("committees.create")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FD))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { myCeremonies() }
        item { com.example.ui.components.OverduePanel(committees, tasks, onOpenCommitteeDetail) }
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Hero Greeting Card
            HeroGreetingCard(
                user = currentUser,
                activeCourse = activeCourse,
                onViewTracker = { onNavigate(AppNavScreen.COURSE_TRACKER) }
            )
        }

        // KPI Counters Grid
        item {
            Column {
                Text(
                    text = "📊 إحصائيات المؤشرات الميدانية (Live KPIs)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary900
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "إجمالي اللجان",
                        value = "$totalCommittees",
                        sub = "لجنة ميدانية",
                        icon = Icons.Default.Groups,
                        color = Color(0xFF0F766E),
                        bgColor = Color(0xFFF0FDFA),
                        modifier = Modifier.weight(1f).clickable(role = Role.Button) { onNavigateCommittees("all") }
                    )
                    KpiCard(
                        title = "الامتحان (منتظر/تم)",
                        value = "$activeCommittees",
                        sub = "لجنة نشطة",
                        icon = Icons.Default.School,
                        color = Color(0xFF2563EB),
                        bgColor = Color(0xFFEFF6FF),
                        modifier = Modifier.weight(1f).clickable(role = Role.Button) { onNavigateCommittees("registration") }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "اكتمل حفلها",
                        value = "$completedCommittees",
                        sub = "لجنة مكرّمة",
                        icon = Icons.Default.WorkspacePremium,
                        color = Color(0xFF15803D),
                        bgColor = Color(0xFFF0FDF4),
                        modifier = Modifier.weight(1f).clickable(role = Role.Button) { onNavigateCommittees("ceremony") }
                    )
                    KpiCard(
                        title = "إجمالي الطلاب",
                        value = "$totalStudents",
                        sub = "طالباً وطالبة",
                        icon = Icons.Default.Person,
                        color = Color(0xFFB45309),
                        bgColor = Color(0xFFFFFBEB),
                        modifier = Modifier.weight(1f).clickable(role = Role.Button) { onNavigateCommittees("all") }
                    )
                }
            }
        }

        // Quick Actions
        item {
            Column {
                Text(
                    text = "⚡ الإجراءات والوصول السريع",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary900
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (canAddCommittee) {
                        QuickActionButton(
                            title = "+ إضافة لجنة",
                            subtitle = "تسجيل لجنة جديدة",
                            icon = Icons.Default.Add,
                            onClick = onOpenAddCommittee,
                            containerColor = Primary700,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    QuickActionButton(
                        title = "كشف الدورات",
                        subtitle = "مصفوفة المتابعة",
                        icon = Icons.Default.TableChart,
                        onClick = { onNavigate(AppNavScreen.COURSE_TRACKER) },
                        containerColor = Color.White,
                        contentColor = Primary800,
                        modifier = Modifier.weight(if (canAddCommittee) 1f else 2f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "بنك الامتحانات",
                        subtitle = "نماذج الـ PDF والأسئلة",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = { onNavigate(AppNavScreen.EXAMS) },
                        containerColor = Color.White,
                        contentColor = Primary800,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        title = "جدول المواعيد",
                        subtitle = "المهام والتسليم",
                        icon = Icons.Default.CalendarMonth,
                        onClick = { onNavigate(AppNavScreen.SCHEDULE) },
                        containerColor = Color.White,
                        contentColor = Primary800,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Today's Urgent Tasks Alert Bar
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مهام التسليم والاستلام الميداني لليوم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary900
                        )
                    }
                    Text(
                        text = "عرض الكل (${urgentTasks.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary700,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(role = Role.Button) { onNavigate(AppNavScreen.SCHEDULE) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (urgentTasks.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تم إنجاز كافة المهام الميدانية المقررة لليوم بنجاح! 🎉",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF166534),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    urgentTasks.take(3).forEach { task ->
                        HomeTaskItem(task = task, onToggle = { onToggleTask(task.id) })
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Upcoming Ceremonies Carousel
        item {
            Column {
                Text(
                    text = "🎓 حفلات التكريم والختام المقررة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary900
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (upcomingCeremonies.isEmpty()) {
                    Text(
                        text = "لم يتم جدولة مواعيد حفلات قادمة بعد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        upcomingCeremonies.forEach { committee ->
                            CeremonyCard(
                                committee = committee,
                                onClick = { onOpenCommitteeDetail(committee.id) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun HeroGreetingCard(
    user: AppUser?,
    activeCourse: Course?,
    onViewTracker: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_greeting_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Primary800),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Primary800, Primary700, Color(0xFF0F766E))
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "مرحباً بك،",
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary200
                        )
                        Text(
                            text = user?.name ?: "المشرف العام",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    // Role Badge
                    user?.let {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${it.role.emoji} ${it.role.titleAr}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Course Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .clickable(role = Role.Button) { onViewTracker() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "الدورة النشطة الحالية 🌟",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeCourse?.name ?: "لا توجد دورة نشطة حالياً",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "فتح الكشف ⬅",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary100,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    sub: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("kpi_card_$title"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePure),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Primary900
            )

            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("btn_quick_action_$title"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (containerColor == Color.White) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (containerColor == Color.White) Primary100 else Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (containerColor == Color.White) Primary700 else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = if (containerColor == Color.White) TextMuted else Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun HomeTaskItem(
    task: ScheduleTask,
    onToggle: () -> Unit
) {
    val (badgeBg, badgeText, badgeDot) = when (task.priority) {
        "urgent" -> Triple(PriorityUrgentBg, PriorityUrgentText, PriorityUrgentDot)
        "high" -> Triple(PriorityHighBg, PriorityHighText, PriorityHighDot)
        else -> Triple(PriorityNormalBg, PriorityNormalText, PriorityNormalDot)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePure),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.IconToggleButton(checked = task.completed, onCheckedChange = { onToggle() }) {
                Icon(
                    imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (task.completed) "إلغاء تحديد «${task.title}» كمنجز" else "تحديد «${task.title}» كمنجز",
                    tint = if (task.completed) Color(0xFF15803D) else TextMuted
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (task.priority == "urgent") "🔴 عاجل" else if (task.priority == "high") "🟡 هام" else "🟢 عادي",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    task.committeeNumber?.let {
                        Text(
                            text = "لجنة ($it)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary700,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary900
                )

                if (task.deliverable.isNotBlank()) {
                    Text(
                        text = "التسليم: ${task.deliverable}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            Text(
                text = task.time,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CeremonyCard(
    committee: Committee,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("ceremony_card_${committee.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لجنة (${committee.committeeNumber})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15803D)
                )
                Text(
                    text = "🎓 حفل تكريم",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF166534),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = committee.committeeName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Primary900,
                maxLines = 1
            )

            Text(
                text = "الموعد: ${committee.ceremonyDetails.date} • ${committee.ceremonyDetails.time}",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = Color(0xFF15803D),
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "المكان: ${committee.ceremonyDetails.venue}",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = Color(0xFF475569),
                maxLines = 1
            )
        }
    }
}
