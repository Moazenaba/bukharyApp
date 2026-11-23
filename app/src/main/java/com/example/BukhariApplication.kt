package com.example

import android.app.Application
import com.example.push.PushNotifications
import com.example.update.UpdateWorker

class BukhariApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PushNotifications.init(this)
        // On phones WorkManager starts itself (App Startup) before this runs. Where it is not set up (unit tests under
        // Robolectric) scheduling would throw and take the whole app down: the update check is then simply skipped.
        try {
            UpdateWorker.schedule(this)
        } catch (e: IllegalStateException) {
            android.util.Log.w("BukhariApplication", "WorkManager not initialised; update checks not scheduled", e)
        }
    }
}
