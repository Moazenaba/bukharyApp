package com.example.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.remote.SessionManager
import com.example.data.repository.BukhariRepository
import java.util.concurrent.TimeUnit

/**
 * Sends queued offline writes once the phone is online, even when the app is closed.
 * Only one run is pending at a time. A run that still has writes waiting is retried with backoff.
 */
class QueueFlushWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // A fresh process has no session loaded yet; the requests need the saved tokens.
        SessionManager.get(applicationContext)
        if (runAttemptCount >= MAX_ATTEMPTS) return Result.failure()
        val report = BukhariRepository(AppDatabase.getInstance(applicationContext)).flushPendingOps()
        return if (report.waiting == 0) Result.success() else Result.retry()
    }

    companion object {
        private const val UNIQUE_NAME = "bukhari-queue-flush"
        private const val MAX_ATTEMPTS = 10

        /** Asks WorkManager to flush the queue as soon as a network connection is available. */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<QueueFlushWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
