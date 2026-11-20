package com.example

import android.app.Application
import com.example.push.PushNotifications
import com.example.update.UpdateWorker

class BukhariApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PushNotifications.init(this)
        UpdateWorker.schedule(this)
    }
}
