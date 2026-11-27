package com.example.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import com.example.data.remote.OnlineApi
import com.example.data.remote.SessionManager
import com.example.data.sync.QueueFlushWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Push notifications through Firebase Cloud Messaging. Firebase is started from the values the
 * build put in BuildConfig (see app/build.gradle.kts); a build without them simply has no push.
 * The server sends the same notifications the app shows in its bell (server/push.ts).
 */
object PushNotifications {
    /** Routine notices: in the tray, quietly. Also app updates. */
    const val CHANNEL_ID = "bukhari_general"
    /** Urgent notices: sound and pop-up. Separate, so people can keep these on and quiet the rest. */
    const val URGENT_CHANNEL_ID = "bukhari_urgent"
    /** The single loud channel of versions up to 1.1.34; removed so it no longer shows in the phone's settings. */
    private const val OLD_CHANNEL_ID = "bukhari_alerts"
    const val EXTRA_OPEN_NOTIFICATIONS = "open_notifications"
    /** The committee a notice is about: tapping it opens that committee. */
    const val EXTRA_COMMITTEE_ID = "committeeId"
    private const val TAG = "PushNotifications"
    private const val PREFS = "push"
    private const val KEY_TOKEN = "fcm_token"

    val isConfigured: Boolean
        get() = BuildConfig.FIREBASE_APP_ID.isNotBlank() && BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() && BuildConfig.FIREBASE_SENDER_ID.isNotBlank()

    /** Starts Firebase and creates the notification channel; called once from the Application/Activity. */
    fun init(context: Context) {
        createChannel(context)
        if (!isConfigured || FirebaseApp.getApps(context).isNotEmpty()) return
        try {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                    .build()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Firebase init failed: ${e.message}")
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        // A channel's importance cannot be changed once created, hence new channels instead of lowering the old one.
        manager.createNotificationChannel(
            NotificationChannel(URGENT_CHANNEL_ID, "تنبيهات عاجلة", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "ما يحتاج تصرفًا الآن: بصوت وتظهر فوق الشاشة"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "تنبيهات عامة", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "المهام والتكليفات ومواعيد الحفلات والتحديثات"
                setSound(null, null)
            }
        )
        manager.deleteNotificationChannel(OLD_CHANNEL_ID)
    }

    /** Android 13+ asks the user before showing notifications. */
    fun needsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    fun savedToken(context: Context): String? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    private fun saveToken(context: Context, token: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TOKEN, token).apply()
    }

    /** Sends this phone's token to the server for the signed-in user. */
    suspend fun register(context: Context) {
        if (!isConfigured) return
        try {
            val token = suspendCancellableCoroutine<String?> { cont ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    cont.resume(if (task.isSuccessful) task.result else null)
                }
            } ?: return
            OnlineApi.registerDevice(token)
            saveToken(context, token)
        } catch (e: Exception) {
            Log.w(TAG, "Push registration failed: ${e.message}")
        }
    }

    /** The token to remove on sign-out (the server deletes it with the session). */
    fun tokenForLogout(context: Context): String? = if (isConfigured) savedToken(context) else null

    fun forget(context: Context) = saveToken(context, null)

    fun show(context: Context, title: String, body: String, type: String, committeeId: String? = null) {
        if (needsPermission(context)) return
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
            .apply { if (!committeeId.isNullOrBlank()) putExtra(EXTRA_COMMITTEE_ID, committeeId) }
        val requestCode = (committeeId ?: "").hashCode()
        val pending = PendingIntent.getActivity(context, requestCode, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val urgent = type == "urgent"
        val notification = NotificationCompat.Builder(context, if (urgent) URGENT_CHANNEL_ID else CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(0xFF005C55.toInt())
            .setCategory(if (urgent) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_MESSAGE)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (urgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification not shown: ${e.message}")
        }
    }

    /** Lets an open app refresh its data when a push arrives. */
    @Volatile
    var onPushReceived: (() -> Unit)? = null
}

/** Receives pushes. In the background Android shows them itself; in the foreground the app does. */
class BukhariMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // Firebase replaced this phone's token: tell the server now, or pushes stop until the next app start.
        // Without a session it is registered at the next sign-in.
        if (SessionManager.get(applicationContext).refreshToken.isNullOrBlank()) return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                OnlineApi.registerDevice(token)
                getSharedPreferences("push", MODE_PRIVATE).edit().putString("fcm_token", token).apply()
            } catch (e: Exception) {
                Log.w("PushNotifications", "Token refresh not sent: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: ""
        PushNotifications.show(this, title, body, message.data["type"] ?: "info", message.data["committeeId"])
        PushNotifications.onPushReceived?.invoke()
        // Bring the new data in even if the app is closed, provided we have an authenticated session.
        if (!SessionManager.get(applicationContext).refreshToken.isNullOrBlank()) {
            QueueFlushWorker.enqueue(applicationContext)
        }
    }
}
