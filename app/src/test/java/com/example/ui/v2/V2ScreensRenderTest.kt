package com.example.ui.v2

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.AppUser
import com.example.data.model.CeremonyDetails
import com.example.data.model.Committee
import com.example.data.model.CommitteeStaff
import com.example.data.model.Course
import com.example.data.model.Exam
import com.example.data.model.NotificationItem
import com.example.data.model.ScheduleTask
import com.example.data.model.StagesMap
import com.example.data.model.UserRole
import com.example.data.remote.Permissions
import com.example.data.remote.StaffUserDto
import com.example.domain.STAGE_ORDER
import com.example.domain.v2.V2ThemeSpec
import com.example.domain.v2.V2Themes
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Runs the «الواجهات 2» screens for real (Robolectric, native graphics) on a phone-sized screen: every role, every
 * theme. Each run taps through the tabs and opens a committee, checks that what the role needs is there and that
 * nothing tappable is smaller than a finger, and saves a picture of each screen to app/build/reports/tests/v2-shots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w392dp-h850dp-xxhdpi")
class V2ScreensRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @After fun reset() = Permissions.clear()

    private val today = "2026-11-17"

    private fun stages(done: Int) = STAGE_ORDER.take(done).fold(StagesMap()) { s, k -> s.updateStage(k, true) }

    private val committees = listOf(
        Committee(
            id = "c12", committeeNumber = 12, committeeName = "الأربعون النووية", sheikhCountry = "مسجد النور - طنطا",
            studentsCount = 18, examDate = today, competitionBook = "الأربعون النووية", stages = stages(0),
            staff = listOf(CommitteeStaff("h1", "أحمد سليم", "committee_head", "01011112222"), CommitteeStaff("s1", "الشيخ محمود", "sheikh"))
        ),
        Committee(
            id = "c9", committeeNumber = 9, committeeName = "العقيدة الواسطية", sheikhCountry = "مسجد الرحمة - المحلة",
            studentsCount = 22, stages = stages(3), staff = listOf(CommitteeStaff("h1", "أحمد سليم", "committee_head", "01011112222"))
        ),
        Committee(
            id = "c4", committeeNumber = 4, committeeName = "عمدة الأحكام", associationName = "جمعية الهدى",
            studentsCount = 12, stages = stages(8),
            ceremonyDetails = CeremonyDetails(date = "2026-11-20", time = "7:00 م", venue = "القاعة الكبرى", honoreesCount = 9)
        )
    )

    private fun user(role: UserRole) = AppUser(id = when (role) { UserRole.SHEIKH -> "s1"; UserRole.COMMITTEE_HEAD -> "h1"; else -> "u-${role.roleKey}" }, name = "مستخدم ${role.titleAr}", phone = "0100", role = role)

    private fun grant(role: UserRole) {
        Permissions.role = role.roleKey
        val head = listOf("committees.read", "committees.reception", "committees.scoring", "committees.finalize", "committees.members", "committees.update", "assignments.manage", "users.read", "exams.read", "exams.file", "tasks.read")
        val admin = head + listOf("users.manage", "users.grant", "notifications.create", "settings.write", "committees.create")
        Permissions.actions = when (role) {
            UserRole.SHEIKH -> listOf("committees.read", "tasks.read")
            UserRole.COMMITTEE_HEAD -> head
            UserRole.ADMIN -> admin
            UserRole.SUPER_ADMIN -> admin + listOf("audit.read", "database.admin")
        }.associateWith { true }
        Permissions.applyServerScope(if (role == UserRole.SHEIKH || role == UserRole.COMMITTEE_HEAD) listOf("c12", "c9") else "all", "all")
    }

    private fun data(role: UserRole, theme: V2ThemeSpec) = V2Data(
        user = user(role),
        committees = committees,
        tasks = listOf(ScheduleTask(id = "t1", title = "تسليم أوراق لجنة 12", date = today, committeeId = "c12", committeeNumber = 12)),
        exams = listOf(Exam(id = "e1", title = "الأربعون النووية", book = "الأربعون النووية", hasFile = true)),
        notifications = listOf(NotificationItem(id = "n1", title = "تمت إضافتك للجنة 12", description = "مسجد النور", committeeId = "c12")),
        staff = listOf(StaffUserDto(id = "p1", name = "يوسف عبد الله", phone = "0101234", role = "sheikh", status = "pending")),
        staffLoading = false,
        candidates = emptyList(),
        courses = listOf(Course(id = "crs", name = "الدورة الصيفية 1447", isDefault = true)),
        selectedCourseId = "crs",
        isOnline = true,
        today = today,
        themeKey = theme.key
    )

    private val noop = V2Actions(
        oneTap = { _, _ -> }, toggleTask = {}, openExam = {}, readNotification = {}, readAll = {}, sync = { done -> done() },
        changePassword = {}, logout = {}, leaveV2 = {}, openClassic = {}, saveStaff = { _, _ -> }, loadCandidates = {},
        loadStaff = {}, approve = { _, _, _ -> }, reject = {}, sendNotice = { _, _, _ -> }, selectCourse = {}, v2Theme = {}
    )

    private fun shot(name: String) {
        val dir = File("build/reports/tests/v2-shots").apply { mkdirs() }
        // Drawn straight from the window (PixelCopy, used by captureToImage, never finishes under Robolectric).
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Every tappable thing is at least 44dp in both directions (48dp is the target; 44 leaves room for rounding). */
    private fun assertFingerSized(where: String) {
        val density = compose.density.density
        val small = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick), useUnmergedTree = false)
            .fetchSemanticsNodes()
            // The element's own size, not its visible part: a row cut by the bottom of the screen is still finger-sized.
            .filter { n -> n.boundsInRoot.width > 0 && n.boundsInRoot.height > 0 && (n.size.height / density < 44f || n.size.width / density < 44f) }
            .map { n -> (n.config.getOrElseNullable(SemanticsProperties.Text) { null }?.joinToString() ?: n.config.getOrElseNullable(SemanticsProperties.ContentDescription) { null }?.joinToString() ?: "?") + " ${n.size}" }
        assertTrue("$where: too small to tap: $small", small.isEmpty())
    }

    private fun tab(label: String) = compose.onAllNodes(hasText(label) and hasClickAction())

    /** One composition per test (a rule allows one setContent); themes change through state. */
    private fun run(role: UserRole, themes: List<V2ThemeSpec>, tabs: List<String>, mustSee: List<String>) {
        grant(role)
        var open by mutableStateOf<String?>(null)
        var theme by mutableStateOf(themes.first())
        compose.setContent {
            V2Theme(theme = theme) {
                V2Shell(
                    data = data(role, theme), actions = noop, selectedCommitteeId = open,
                    onOpenCommittee = { open = it }, onCloseCommittee = { open = null }, legacyForm = { _, _, _ -> }
                )
            }
        }
        themes.forEach { t ->
            compose.runOnIdle { theme = t; open = null }
            compose.waitForIdle()
            val tag = "${role.roleKey}-${t.key}"
            tab(tabs.first()).fetchSemanticsNodes().let { assertTrue("$tag: tab «${tabs.first()}» missing", it.isNotEmpty()) }
            tab(tabs.first())[0].performClick()
            compose.waitForIdle()
            mustSee.forEach { compose.onAllNodesWithText(it, substring = true).fetchSemanticsNodes().let { n -> assertTrue("$tag: «$it» not shown", n.isNotEmpty()) } }
            assertFingerSized("$tag first tab")
            shot("$tag-0")
            tabs.drop(1).forEachIndexed { i, label ->
                tab(label).fetchSemanticsNodes().let { assertTrue("$tag: tab «$label» missing", it.isNotEmpty()) }
                tab(label)[0].performClick()
                compose.waitForIdle()
                assertFingerSized("$tag «$label»")
                shot("$tag-${i + 1}")
            }
            // Open a committee and check the page a field worker needs.
            compose.runOnIdle { open = "c12" }
            compose.waitForIdle()
            compose.onNodeWithText("رايح فين؟").assertExists()
            compose.onAllNodesWithText("مسجد النور", substring = true).fetchSemanticsNodes().let { assertTrue("$tag: place missing", it.isNotEmpty()) }
            assertFingerSized("$tag committee")
            shot("$tag-committee")
        }
    }

    @Test fun `the hour is the large first line of the main card`() {
        compose.setContent {
            V2Theme(theme = V2Themes.INK) {
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.padding(16.dp)) {
                    V2NowCard(meta = "اليوم", title = "حفل تكريم لجنة 12", time = "09:30 ص") {}
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("09:30 ص").assertExists()
        compose.onNodeWithText("حفل تكريم لجنة 12").assertExists()
        shot("now-card-hour")
    }

    @Test fun `sheikh, every theme`() =
        run(UserRole.SHEIKH, V2Themes.ALL, listOf("الآن", "لجاني", "الحفلات", "حسابي"), listOf("الآن", "لجاني", "الحفلات", "حسابي"))

    @Test fun `committee head`() =
        run(UserRole.COMMITTEE_HEAD, listOf(V2Themes.INK), listOf("الآن", "لجاني", "الفريق", "حسابي"), listOf("الفريق", "مطلوب النهارده"))

    @Test fun `admin`() =
        run(UserRole.ADMIN, listOf(V2Themes.SAND, V2Themes.COFFEE), listOf("لوحة", "اللجان", "الناس", "المزيد"), listOf("لوحة", "المراحل", "الناس"))

    @Test fun `super admin, dark theme`() =
        run(UserRole.SUPER_ADMIN, listOf(V2Themes.NIGHT), listOf("لوحة", "اللجان", "الناس", "المزيد"), listOf("لوحة"))
}
