package com.example.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Checks for an app update and installs it while the app is not on screen: every few hours, and
 * shortly after the user leaves the app. Android 12+ installs it without asking; older versions
 * get a notification to tap.
 */
class UpdateWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val apk = AppUpdater.fetchUpdate(applicationContext) ?: return Result.success()
        // Never close the app under someone's hands; the next run (or leaving the app) installs it.
        if (AppUpdater.isInForeground()) return Result.success()
        if (!AppUpdater.canInstall(applicationContext)) return Result.success()
        if (AppUpdater.installsSilently) {
            runCatching { AppUpdater.install(applicationContext, apk) }
        } else {
            AppUpdater.notifyReady(applicationContext, null)
        }
        return Result.success()
    }

    companion object {
        private const val PERIODIC_NAME = "bukhari-app-update"
        private const val AFTER_LEAVING_NAME = "bukhari-app-update-now"

        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Checks every 6 hours while the phone is online. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateWorker>(6, TimeUnit.HOURS).setConstraints(online).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Runs a check shortly after the app leaves the screen, so a ready update goes in right away. */
        fun runAfterLeaving(context: Context) {
            val request = OneTimeWorkRequestBuilder<UpdateWorker>()
                .setInitialDelay(20, TimeUnit.SECONDS)
                .setConstraints(online)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(AFTER_LEAVING_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
