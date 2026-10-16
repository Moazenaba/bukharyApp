package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.PendingOpEntity
import com.example.data.remote.ApiClient
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

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        sender = ScriptedSender()
        repository = BukhariRepository(db, sender = sender)
    }

    @After
    fun tearDown() {
        db.close()
        ApiClient.onUnauthorized = null
    }

    private fun queue(label: String, path: String = "api/committees/$label") = runBlocking {
        db.pendingOpDao().insert(PendingOpEntity(label = label, method = "POST", path = path, bodyJson = "{}"))
    }

    private fun reply(code: Int?, message: String? = null, body: String? = null) = ApiClient.SendResult(code, message, body)

    @Test
    fun `a delivered write leaves the queue and its server version is stored`() = runBlocking {
        queue("a")
        sender.replies += reply(200, body = """{"success":true,"committee":{"id":"c1","updatedAt":"2026-10-16T10:00:00.000Z"}}""")

        val report = repository.flushPendingOps()

        assertEquals(0, report.waiting)
        assertTrue(report.outcomes.values.single() is WriteResult.Sent)
        assertEquals(0, db.pendingOpDao().getActive().size)
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
        assertEquals(listOf("b"), db.pendingOpDao().getActive().map { it.label })
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
        assertEquals(0, db.pendingOpDao().countActive())
    }

    @Test
    fun `a write the server refuses is dropped and reported, not retried`() = runBlocking {
        queue("refused")
        sender.replies += reply(409, "هذه اللجنة عُدّلت من مستخدم آخر")

        val report = repository.flushPendingOps()

        val outcome = report.outcomes.values.single()
        assertTrue(outcome is WriteResult.Rejected)
        assertEquals("هذه اللجنة عُدّلت من مستخدم آخر", (outcome as WriteResult.Rejected).message)
        assertEquals(0, db.pendingOpDao().getActive().size)
    }

    @Test
    fun `an expired session stops the flush and tells the app`() = runBlocking {
        queue("a")
        var expired = false
        ApiClient.onUnauthorized = { expired = true }
        sender.replies += reply(401)

        repository.flushPendingOps()

        assertTrue(expired)
        assertEquals(1, db.pendingOpDao().countActive())
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
    val calls = mutableListOf<String>()

    override fun send(method: String, path: String, bodyJson: String?): ApiClient.SendResult {
        calls += "$method $path"
        return replies.removeFirst()
    }
}
