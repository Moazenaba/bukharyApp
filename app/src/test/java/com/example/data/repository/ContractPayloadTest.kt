package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AppUserEntity
import com.example.data.local.entities.CommitteeEntity
import com.example.data.model.AggregationDetails
import com.example.data.model.AppUser
import com.example.data.model.CeremonyDetails
import com.example.data.model.Committee
import com.example.data.model.QuestionScoringItem
import com.example.data.model.ScheduleTask
import com.example.data.model.ScoringDetails
import com.example.data.model.StagesMap
import com.example.data.model.UserRole
import com.example.data.remote.ApiClient
import com.squareup.moshi.Types
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Records the exact requests the app sends for each kind of write (XP-01). The file it writes,
 * build/contract-payloads/android.json, is replayed against the server's validation by the web
 * repository's contract test (Web/tests/contract), so a field the server would drop or refuse
 * fails a build instead of a user's offline work.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContractPayloadTest {
    private lateinit var db: AppDatabase
    private val sent = mutableListOf<Map<String, Any?>>()
    private lateinit var repository: BukhariRepository
    private val mapAdapter = ApiClient.moshi.adapter<Any>(Any::class.java)

    private val committee = Committee(
        id = "contract-committee",
        committeeNumber = 7,
        committeeName = "لجنة العقد",
        courseId = "contract-course",
        stages = StagesMap().updateStage("registration", true, null),
        scoringDetails = ScoringDetails(questions = listOf(QuestionScoringItem(id = "q-1", questionNumber = 1, label = "السؤال 1"))),
        updatedAt = "2026-10-01T00:00:00.000Z"
    )

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        db.committeeDao().insertCommittee(CommitteeEntity.fromDomain(committee))
        db.appUserDao().insertUser(
            AppUserEntity(
                id = "contract-applicant", name = "متقدم", phone = "0512345678", email = "", passportNumber = "",
                roleKey = "assistant", status = "pending", updatedAt = "2026-10-01T00:00:00.000Z"
            )
        )
        repository = BukhariRepository(
            db,
            sender = { method, path, body, _ ->
                sent += mapOf("method" to method, "path" to path, "body" to body?.let { mapAdapter.fromJson(it) })
                ApiClient.SendResult(200, null, null)
            },
            userIdProvider = { "contract-user" }
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `records every kind of write the app sends`() = runBlocking {
        val me = AppUser(id = "contract-user", name = "معاون", phone = "0500000000")
        repository.saveCommittee(committee.copy(id = "contract-new", committeeNumber = 8, updatedAt = null))
        repository.updateStage(committee.id, "exam", true)
        repository.recordAssistantHandover(committee.id, me, "0500000000")
        repository.approveExamReadiness(committee.id, me)
        repository.updateQuestion(committee.id, "q-1", true)
        repository.updateScoring(committee.id, committee.scoringDetails.copy(examinerName = "مصحح"))
        repository.updateAggregation(committee.id, AggregationDetails(aggregatorName = "مجمع", isConfirmedMatch = true, date = "2026-10-16"))
        repository.updateCeremony(committee.id, CeremonyDetails(date = "2026-11-01", time = "7 مساءً", venue = "القاعة", honoreesCount = 10, attendingSheikhs = listOf("الشيخ")))
        repository.saveTask(ScheduleTask(id = "contract-task", title = "تسليم", date = "2026-10-20", committeeId = committee.id, assignedAssistantId = "contract-user"))
        repository.approveUser("contract-applicant", listOf(committee.id), null, null)
        repository.rejectUser("contract-applicant", "اكتمل العدد")

        val out = File("build/contract-payloads").apply { mkdirs() }.resolve("android.json")
        out.writeText(ApiClient.moshi.adapter<List<Map<String, Any?>>>(
            Types.newParameterizedType(List::class.java, Map::class.java)
        ).indent("  ").toJson(sent))

        // Invariants the server relies on.
        val bodies = sent.mapNotNull { it["body"] as? Map<*, *> }
        val task = bodies.first { it["id"] == "contract-task" }
        assertEquals(committee.id, task["committeeId"])
        assertEquals("contract-user", task["assignedAssistantId"])
        val approval = sent.first { it["path"] == "api/users" }["body"] as Map<*, *>
        assertFalse("approval must not resend the passport", approval.containsKey("passportNumber"))
        assertFalse("approval must not send permissions it did not change", approval.containsKey("permissions"))
        assertEquals(listOf(committee.id), approval["assignedCommitteeIds"])
        assertTrue(sent.any { it["path"] == "api/committees/contract-committee/actions" })
        val stage = sent.first { it["path"] == "api/committees/contract-committee/actions" }["body"] as Map<*, *>
        assertEquals(mapOf("type" to "completeStage", "stage" to "exam"), stage["action"])
    }
}
