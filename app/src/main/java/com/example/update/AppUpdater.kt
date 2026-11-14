package com.example.update

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import com.example.data.remote.ApiClient
import com.example.push.PushNotifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Keeps the app up to date without the Play Store. The server publishes `app/android.json` and the
 * APK beside the web app (scripts/publish-android.mjs in the web repo). The app downloads a newer
 * APK, checks it is this app signed with the same key and a higher version (so Android never
 * refuses it as a conflicting package), and installs it through PackageInstaller.
 *
 * Android 12+ installs it without asking once the user allowed this app to install apps; older
 * versions show the system's install screen. The install runs only while the app is not on
 * screen, so it never closes the app in the middle of someone's work (see [UpdateWorker]).
 */
object AppUpdater {
    private const val TAG = "AppUpdater"
    private const val MANIFEST_PATH = "app/android.json"
    private const val NOTIFICATION_ID = 7301
    private const val PREFS = "app_update"
    private const val KEY_ASKED_PERMISSION = "asked_install_permission"
    private const val KEY_ASKED_AT = "asked_install_permission_at"

    data class UpdateInfo(
        val versionCode: Int = 0,
        val versionName: String = "",
        /** Path of the APK relative to the server's base URL. */
        val apk: String = "",
        val sha256: String = "",
        val size: Long = 0,
        val notes: String = "",
    )

    /** What the activity shows: nothing, a one-time request to allow installs, or (Android 11 and older) an update ready to install. */
    sealed interface UiState {
        data object Idle : UiState
        data object NeedsPermission : UiState
        data class ReadyToInstall(val versionName: String) : UiState
    }

    private val _ui = MutableStateFlow<UiState>(UiState.Idle)
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val lock = Mutex()

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun updatesDir(context: Context) = File(context.cacheDir, "updates").apply { mkdirs() }

    /** True on Android 12+, where an app updating itself is installed without asking. */
    val installsSilently: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** What a check found: a verified APK to install, or a report saying why there is nothing to install. */
    private sealed interface Outcome {
        data class Ready(val file: File, val info: UpdateInfo) : Outcome
        data class None(val report: UpdateReport) : Outcome
    }

    private val installedName: String get() = BuildConfig.VERSION_NAME

    private val _report = MutableStateFlow<UpdateReport?>(null)

    /** The result of the last check (null before the first one): what the update row and the banner show. */
    val report: StateFlow<UpdateReport?> = _report.asStateFlow()

    /**
     * Fetches the manifest and, if it names a newer version, downloads and checks the APK.
     * Returns the verified APK, or null when there is nothing (valid) to install.
     */
    suspend fun fetchUpdate(context: Context): File? = (fetch(context) as? Outcome.Ready)?.file

    private suspend fun fetch(context: Context): Outcome = lock.withLock {
        withContext(Dispatchers.IO) {
            if (!ApiClient.isConfigured) return@withContext Outcome.None(UpdateReport(UpdateStatus.NOT_CONFIGURED, installedName))
            val info = try {
                fetchInfo()
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed: ${e.message}")
                return@withContext Outcome.None(UpdateReport(UpdateStatus.UNREACHABLE, installedName, detail = e.message.orEmpty()))
            } ?: return@withContext Outcome.None(UpdateReport(UpdateStatus.UNREACHABLE, installedName, detail = "no manifest"))
            val apkUrl = ApiClient.baseUrl + info.apk.trimStart('/')
            if (!UpdateDecision.isNewer(BuildConfig.VERSION_CODE, info.versionCode, info.apk)) {
                clearDownloads(context)
                return@withContext Outcome.None(UpdateReport(UpdateStatus.UP_TO_DATE, installedName, info.versionName))
            }
            fun report(status: UpdateStatus, detail: String = "") = Outcome.None(UpdateReport(status, installedName, info.versionName, apkUrl, detail))
            try {
                val file = File(updatesDir(context), "bukhari-${info.versionCode}.apk")
                if (!(file.exists() && sha256(file).equals(info.sha256, ignoreCase = true))) {
                    clearDownloads(context)
                    download(apkUrl, file)
                    if (info.sha256.isNotBlank() && !sha256(file).equals(info.sha256, ignoreCase = true)) {
                        Log.w(TAG, "Downloaded APK does not match its checksum")
                        file.delete()
                        return@withContext report(UpdateStatus.CHECKSUM_MISMATCH)
                    }
                }
                unsafeReason(context, file)?.let { status ->
                    file.delete()
                    return@withContext report(status)
                }
                Outcome.Ready(file, info)
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed: ${e.message}")
                report(UpdateStatus.DOWNLOAD_FAILED, e.message.orEmpty())
            }
        }
    }

    /**
     * Checks for an update now and says what happened, in a form the screen can show. Does not install.
     */
    suspend fun check(context: Context): UpdateReport {
        val report = when (val outcome = fetch(context)) {
            is Outcome.None -> outcome.report
            is Outcome.Ready -> UpdateReport(
                if (canInstall(context)) UpdateStatus.READY else UpdateStatus.NEEDS_PERMISSION,
                installedName, outcome.info.versionName, ApiClient.baseUrl + outcome.info.apk.trimStart('/')
            )
        }
        _report.value = report
        return report
    }

    /** The «حدّث الآن» button: checks, and installs while the app is on screen, so Android's own confirm dialog opens at once. */
    suspend fun updateNow(context: Context): UpdateReport {
        val report = check(context)
        if (report.status != UpdateStatus.READY) return report
        val apk = fetchUpdate(context) ?: return report
        return try {
            install(context, apk)
            report
        } catch (e: Exception) {
            Log.w(TAG, "Install failed to start: ${e.message}")
            report.copy(status = UpdateStatus.DOWNLOAD_FAILED, detail = e.message.orEmpty())
        }
    }

    private fun fetchInfo(): UpdateInfo? {
        val request = Request.Builder().url(ApiClient.baseUrl + MANIFEST_PATH).header("Cache-Control", "no-cache").build()
        client.newCall(request).execute().use { res ->
            if (!res.isSuccessful) return null
            val body = res.body?.string() ?: return null
            return ApiClient.moshi.adapter(UpdateInfo::class.java).fromJson(body)
        }
    }

    private fun download(url: String, target: File) {
        val part = File(target.path + ".part")
        client.newCall(Request.Builder().url(url).build()).execute().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("HTTP ${res.code}")
            val body = res.body ?: throw IllegalStateException("Empty body")
            part.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
        if (!part.renameTo(target)) throw IllegalStateException("Could not save the update")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Same package, higher version and the same signing key as the installed app: Android will accept it as an update. Null when it will; else why not. */
    private fun unsafeReason(context: Context, apk: File): UpdateStatus? {
        val pm = context.packageManager
        val archive = archiveInfo(pm, apk.path) ?: return UpdateStatus.DOWNLOAD_FAILED.also { Log.w(TAG, "Downloaded file is not an APK") }
        if (archive.packageName != context.packageName) return UpdateStatus.WRONG_APP.also { Log.w(TAG, "APK is for another app: ${archive.packageName}") }
        @Suppress("DEPRECATION")
        val archiveCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) archive.longVersionCode else archive.versionCode.toLong()
        if (archiveCode <= BuildConfig.VERSION_CODE) return UpdateStatus.NOT_NEWER.also { Log.w(TAG, "APK is not newer ($archiveCode)") }
        val installed = installedInfo(pm, context.packageName)
        val apkSigners = signers(archive)
        val ownSigners = signers(installed)
        // Without readable signatures the installer still rejects a different key, quietly (no dialog).
        if (apkSigners.isNotEmpty() && ownSigners.isNotEmpty() && apkSigners != ownSigners) {
            Log.w(TAG, "APK is signed with a different key; skipped")
            return UpdateStatus.WRONG_SIGNATURE
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun archiveInfo(pm: PackageManager, path: String): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            pm.getPackageArchiveInfo(path, flags)
        }
    }

    @Suppress("DEPRECATION")
    private fun installedInfo(pm: PackageManager, packageName: String): PackageInfo {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            pm.getPackageInfo(packageName, flags)
        }
    }

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.let { if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory }
        } else {
            info.signatures
        }
        return sigs.orEmpty().map { it.toCharsString() }.toSet()
    }

    private fun clearDownloads(context: Context) {
        updatesDir(context).listFiles()?.forEach { it.delete() }
    }

    /** Hands the APK to Android's installer; the result arrives in [UpdateInstallReceiver]. */
    fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val callback = PendingIntent.getBroadcast(
                context, sessionId, Intent(context, UpdateInstallReceiver::class.java).setPackage(context.packageName), flags
            )
            session.commit(callback.intentSender)
        }
    }

    fun isInForeground(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    /**
     * Called when the app opens: fetches any update in the background of the screen, asks once for
     * permission to install apps, and on Android 11 and older offers the downloaded update.
     */
    suspend fun onAppOpened(context: Context) {
        val report = check(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        when (report.status) {
            UpdateStatus.NEEDS_PERMISSION -> {
                val lastAsked = prefs.getLong(KEY_ASKED_AT, if (prefs.getBoolean(KEY_ASKED_PERMISSION, false)) 1L else 0L)
                if (UpdateDecision.shouldAskPermission(lastAsked, System.currentTimeMillis())) _ui.value = UiState.NeedsPermission
            }
            UpdateStatus.READY -> if (!installsSilently) {
                val apk = fetchUpdate(context)
                if (apk != null) _ui.value = UiState.ReadyToInstall(versionNameOf(context, apk))
            }
            // Android 12+: installed quietly once the app leaves the screen (MainActivity.onStop), or at once from the update row.
            else -> Unit
        }
    }

    private fun versionNameOf(context: Context, apk: File): String = archiveInfo(context.packageManager, apk.path)?.versionName.orEmpty()

    /** The settings screen where the user allows this app to install updates. */
    fun permissionSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    fun dismissPermissionRequest(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ASKED_PERMISSION, true).putLong(KEY_ASKED_AT, System.currentTimeMillis()).apply()
        _ui.value = UiState.Idle
    }

    fun dismiss() {
        _ui.value = UiState.Idle
    }

    /** Installs the downloaded update now (the user tapped «تحديث», or the app is not on screen). */
    suspend fun installNow(context: Context): Boolean {
        _ui.value = UiState.Idle
        val apk = fetchUpdate(context) ?: return false
        if (!canInstall(context)) return false
        return try {
            install(context, apk)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Install failed to start: ${e.message}")
            false
        }
    }

    /** Shown when Android needs the user to confirm but the app is not on screen (Android 11 and older, mostly). */
    fun notifyReady(context: Context, confirm: Intent?) {
        val intent = confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, PushNotifications.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("تحديث جديد للتطبيق")
            .setContentText("اضغط لتثبيت التحديث")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notifications are not allowed")
        }
    }

    fun cancelNotification(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }
}
