package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Session tokens and the server-evaluated permissions, stored encrypted.
 * If encrypted storage is unavailable (e.g. a broken keystore after a restore), the
 * session is kept in memory only: the user signs in again next launch instead of the
 * tokens being written in plain text.
 */
class SessionManager private constructor(context: Context) : ApiClient.TokenStore {

    private val prefs: SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context,
            "bukhari_secure_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w("SessionManager", "Encrypted storage unavailable; session will not be persisted", e)
        null
    }
    private val memory = mutableMapOf<String, String>()

    private fun get(key: String): String? = prefs?.getString(key, null) ?: memory[key]
    private fun put(values: Map<String, String?>) {
        if (prefs != null) {
            prefs.edit().apply { values.forEach { (k, v) -> if (v == null) remove(k) else putString(k, v) } }.apply()
        } else {
            values.forEach { (k, v) -> if (v == null) memory.remove(k) else memory[k] = v }
        }
    }

    override val accessToken: String? get() = get(KEY_ACCESS)
    override val refreshToken: String? get() = get(KEY_REFRESH)

    @Synchronized
    override fun saveTokens(access: String, refresh: String) = put(mapOf(KEY_ACCESS to access, KEY_REFRESH to refresh))

    fun saveUserId(id: String) = put(mapOf(KEY_USER to id))
    fun getUserId(): String? = get(KEY_USER)
    override val sessionUserId: String? get() = getUserId()

    /** JSON of GET /api/auth/me's permissions (actions and scope), so they work offline after a restart. */
    fun savePermissionsJson(json: String) = put(mapOf(KEY_PERMISSIONS to json))
    fun getPermissionsJson(): String? = get(KEY_PERMISSIONS)

    @Synchronized
    override fun clear() = put(mapOf(KEY_ACCESS to null, KEY_REFRESH to null, KEY_USER to null, KEY_PERMISSIONS to null))

    companion object {
        private const val KEY_ACCESS = "auth_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_USER = "user_id"
        private const val KEY_PERMISSIONS = "permissions_json"

        @Volatile
        private var instance: SessionManager? = null

        fun get(context: Context): SessionManager =
            instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also {
                    instance = it
                    ApiClient.tokenStore = it
                }
            }
    }
}
