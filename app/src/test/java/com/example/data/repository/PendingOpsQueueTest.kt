package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.PendingOpEntity
import com.example.data.remote.ApiClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The offline write queue, tested against an in-memory database. Each server reply is scripted,
 * so the tests cover every branch of delivery without a network.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PendingOpsQueueTest {
    private lateinit var db: AppDatabase
    private lateinit var sender: ScriptedSender
    private lateinit var repository: BukhariRepository
    private var currentUser: String? = "u1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        sender = ScriptedSender()
        repository = BukhariRepository(db, sender = sender, userIdProvider = { currentUser })
    }

    @After
    fun tearDown() {
        db.close()
        ApiClient.onUnauthorized = null
    }

    private fun queue(label: String, path: String = "api/committees/$label", method: String = "POST", userId: String? = "u1") = runBlocking {
        db.pendingOpDao().insert(PendingOpEntity(label = label, method = method, path = path, bodyJson = "{}", userId = userId))
    }

    private fun reply(code: Int?, message: String? = null, body: String? = null) = ApiClient.SendResult(code, message, body)

    @Test
    fun `a delivered write leaves the queue and its server version is stored`() = runBlocking {
        queue("a")
        sender.replies += reply(200, body = """{"success":true,"committee":{"id":"c1","updatedAt":"2026-10-16T10:00:00.000Z"}}""")

        val report = repository.flushPendingOps()

        assertEquals(0, report.waiting)
        assertTrue(report.outcomes.values.single() is WriteResult.Sent)
        assertEquals(0, db.pendingOpDao().getActive("u1").size)
    }

    @Test
    fun `no network stops the flush and keeps the write for later`() = runBlocking {
        queue("a")
        queue("b")
        sender.replies += reply(null)

        val report = repository.flushPendingOps()

        assertEquals(2, report.waiting)
        assertEquals(listOf("POST api/committees/a"), sender.calls)
    }

    @Test
    fun `writes are sent in order and a failure blocks the ones behind it`() = runBlocking {
        queue("a")
        queue("b")
        sender.replies += reply(200)
        sender.replies += reply(null)

        repository.flushPendingOps()

        assertEquals(listOf("POST api/committees/a", "POST api/committees/b"), sender.calls)
        assertEquals(listOf("b"), db.pendingOpDao().getActive("u1").map { it.label })
    }

    @Test
    fun `a write the server keeps failing with 5xx is parked after five tries and no longer blocks the queue`() = runBlocking {
        queue("stuck")
        queue("next")
        // Five flushes: the first four fail and stop the queue; the fifth parks "stuck" and carries on.
        repeat(BukhariRepository.MAX_ATTEMPTS) { sender.replies += reply(500, "server error") }
        sender.replies += reply(200)

        repeat(BukhariRepository.MAX_ATTEMPTS) { repository.flushPendingOps() }

        assertEquals(listOf("stuck"), db.pendingOpDao().getFailed().map { it.label })
        assertTrue(sender.calls.contains("POST api/committees/next"))
        assertEquals(0, db.pendingOpDao().countActive("u1"))
    }

    @Test
    fun `a write the server refuses is kept as refused with its message, not dropped (MOB-03)`() = runBlocking {
        queue("refused")
        queue("next")
        sender.replies += reply(409, "هذه اللجنة عُدّلت من مستخدم آخر")
        sender.replies += reply(200)

        val report = repository.flushPendingOps()

        val outcome = report.outcomes.values.first()
        assertTrue(outcome is WriteResult.Rejected)
        assertEquals("هذه اللجنة عُدّلت من مستخدم آخر", (outcome as WriteResult.Rejected).message)
        // It no longer blocks the queue, but it is still there for the user to decide.
        assertEquals(0, db.pendingOpDao().getActive("u1").size)
        val refused = repository.refusedWrites().single()
        assertEquals("refused", refused.label)
        assertEquals(409, refused.httpCode)
    }

    @Test
    fun `HTTP 426 upgrade required is marked rejected and does not block the queue`() = runBlocking {
        queue("outdated")
        queue("next")
        sender.replies += reply(426, "يرجى تحديث التطبيق")
        sender.replies += reply(200)

        val report = repository.flushPendingOps()

        val outcome = report.outcomes.values.first()
        assertTrue(outcome is WriteResult.Rejected)
        assertEquals(0, db.pendingOpDao().getActive("u1").size)
        val refused = repository.refusedWrites().single()
        assertEquals("outdated", refused.label)
        assertEquals(426, refused.httpCode)
    }

    @Test
    fun `a refused write can be sent again or discarded`() = runBlocking {
        queue("refused")
        sender.replies += reply(409, "conflict")
        repository.flushPendingOps()
        val id = repository.refusedWrites().single().id

        sender.replies += reply(200)
        assertTrue(repository.retryRefusedWrite(id).outcomes[id] is WriteResult.Sent)
        assertEquals(emptyList<RefusedWrite>(), repository.refusedWrites())
    }

    @Test
    fun `a delete of something already gone counts as done`() = runBlocking {
        queue("gone", path = "api/tasks/t1", method = "DELETE")
        sender.replies += reply(404, "المهمة غير موجودة")

        val report = repository.flushPendingOps()

        assertTrue(report.outcomes.values.single() is WriteResult.Sent)
        assertEquals(emptyList<RefusedWrite>(), repository.refusedWrites())
    }

    @Test
    fun `every write carries its own idempotency key (MOB-04)`() = runBlocking {
        queue("a")
        queue("b")
        sender.replies += reply(200)
        sender.replies += reply(200)

        repository.flushPendingOps()

        assertEquals(2, sender.keys.toSet().size)
        assertTrue(sender.keys.all { !it.isNullOrBlank() })
    }

    @Test
    fun `two flushers on one database send each write once (MOB-04)`() = runBlocking {
        queue("a")
        queue("b")
        repeat(4) { sender.replies += reply(200) }
        val worker = BukhariRepository(db, sender = sender, userIdProvider = { "u1" })

        kotlinx.coroutines.coroutineScope {
            launch(kotlinx.coroutines.Dispatchers.IO) { repository.flushPendingOps() }
            launch(kotlinx.coroutines.Dispatchers.IO) { worker.flushPendingOps() }
        }

        assertEquals(listOf("POST api/committees/a", "POST api/committees/b"), sender.calls)
    }

    @Test
    fun `another user's unsent work is never sent under this session (MOB-01)`() = runBlocking {
        queue("mine", userId = "u1")
        queue("theirs", userId = "u2")
        sender.replies += reply(200)

        repository.flushPendingOps()

        assertEquals(listOf("POST api/committees/mine"), sender.calls)
        assertEquals(listOf("u2"), repository.otherUsersWithUnsentWork("u1"))
    }

    @Test
    fun `ending the session keeps unsent work unless the user discards it (MOB-01, MOB-02)`() = runBlocking {
        queue("offline-reception")

        repository.clearAllData(keepQueue = true)
        assertEquals(1, repository.unsentCount())

        repository.clearAllData(keepQueue = false)
        assertEquals(0, repository.unsentCount())
    }

    @Test
    fun `an expired session stops the flush and tells the app`() = runBlocking {
        queue("a")
        var expired = false
        ApiClient.onUnauthorized = { expired = true }
        sender.replies += reply(401)

        repository.flushPendingOps()

        assertTrue(expired)
        // The write is still there, waiting for the user to sign in again (MOB-01).
        assertEquals(1, db.pendingOpDao().countActive("u1"))
    }

    @Test
    fun `retrying parked writes sends them again`() = runBlocking {
        db.pendingOpDao().insert(PendingOpEntity(label = "parked", method = "POST", path = "api/x", bodyJson = "{}", attempts = 5, failed = true))
        sender.replies += reply(200)

        val report = repository.retryFailedWrites()

        assertEquals(0, report.waiting)
        assertEquals(listOf("POST api/x"), sender.calls)
    }
}

/** Replays scripted replies in order and records every request it was asked to send. */
private class ScriptedSender : RequestSender {
    val replies = ArrayDeque<ApiClient.SendResult>()
    val calls = java.util.Collections.synchronizedList(mutableListOf<String>())
    val keys = java.util.Collections.synchronizedList(mutableListOf<String?>())
    val bodies = java.util.Collections.synchronizedList(mutableListOf<String?>())

    @Synchronized
    override fun send(method: String, path: String, bodyJson: String?, idempotencyKey: String?): ApiClient.SendResult {
        calls += "$method $path"
        keys += idempotencyKey
        bodies += bodyJson
        return replies.removeFirst()
    }
}
