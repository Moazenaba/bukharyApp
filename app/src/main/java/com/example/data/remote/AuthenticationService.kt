package com.example.data.remote

import android.content.Context
import com.example.data.model.AppUser
import com.example.data.model.UserRole
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

sealed class AuthResult {
    data class Success(val user: AppUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthenticationService(
    context: Context,
    private val sessionManager: SessionManager = SessionManager.get(context),
    private val apiService: ApiService = ApiClient.apiService
) {
    private val actionsAdapter = ApiClient.moshi.adapter<Map<String, Boolean>>(
        Types.newParameterizedType(Map::class.java, String::class.java, java.lang.Boolean::class.java)
    )

    init {
        restorePermissions()
    }

    /**
     * Logs in against the shared server only. There is deliberately no offline
     * fallback: the password can only be checked by the server.
     */
    suspend fun login(phoneOrEmail: String, pass: String): AuthResult = withContext(Dispatchers.IO) {
        if (phoneOrEmail.isBlank() || pass.isBlank()) {
            return@withContext AuthResult.Error("يرجى إدخال رقم الهاتف أو البريد الإلكتروني وكلمة المرور")
        }
        if (!ApiClient.isConfigured) return@withContext AuthResult.Error("لم يتم ضبط عنوان الخادم في هذا الإصدار من التطبيق")
        try {
            val response = apiService.login(LoginRequest(identifier = phoneOrEmail.trim(), password = pass))
            val body = response.body()
            if (response.isSuccessful && body?.token != null && body.refreshToken != null && body.user != null) {
                sessionManager.saveTokens(body.token, body.refreshToken)
                val user = body.user.toDomain()
                sessionManager.saveUserId(user.id)
                // The server-evaluated permissions decide what the UI offers. All or nothing: if they
                // cannot be loaded, no half-made session may be restored on the next launch.
                if (refreshMe() == null) {
                    forgetSession()
                    return@withContext AuthResult.Error("تعذر تحميل صلاحيات الحساب، حاول مرة أخرى")
                }
                return@withContext AuthResult.Success(user)
            }
            AuthResult.Error(serverMessage(response) ?: "بيانات الدخول غير صحيحة")
        } catch (e: Exception) {
            AuthResult.Error("تعذر الاتصال بالسيرفر، تأكد من اتصالك بالإنترنت ثم حاول مرة أخرى")
        }
    }

    /** Reloads the user and their permissions from the server. Null when offline or rejected. */
    suspend fun refreshMe(): AppUser? = withContext(Dispatchers.IO) {
        try {
            val res = apiService.me()
            val body = res.body()
            if (!res.isSuccessful || body?.user == null) return@withContext null
            val user = body.user.toDomain()
            applyPermissions(user, body.permissions.actions)
            sessionManager.savePermissionsJson(actionsAdapter.toJson(body.permissions.actions))
            user
        } catch (e: Exception) {
            null
        }
    }

    private fun applyPermissions(user: AppUser, actions: Map<String, Boolean>) {
        Permissions.actions = actions
        Permissions.role = user.role.roleKey
        Permissions.assignedCommittees = user.assignedCommittees
    }

    /** Uses the last known permissions until the next successful /me (e.g. starting offline). */
    private fun restorePermissions() {
        val json = sessionManager.getPermissionsJson() ?: return
        Permissions.actions = try {
            actionsAdapter.fromJson(json) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun restoreUserPermissions(user: AppUser) {
        Permissions.role = user.role.roleKey
        Permissions.assignedCommittees = user.assignedCommittees
    }

    private fun serverMessage(response: Response<*>): String? = ApiClient.serverMessage(response.errorBody()?.string())

    /** Public join request; returns the server-issued request code. */
    suspend fun register(name: String, phone: String, email: String, role: UserRole, pass: String, notes: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.register(
                    RegisterRequest(
                        name = name.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        roleKey = role.roleKey,
                        password = pass,
                        notes = notes.trim().ifBlank { null }
                    )
                )
                val code = response.body()?.requestCode
                if (response.isSuccessful && code != null) Result.success(code)
                else Result.failure(Exception(serverMessage(response) ?: "فشل تسجيل طلب الانضمام"))
            } catch (e: Exception) {
                Result.failure(Exception("تعذر الاتصال بالسيرفر"))
            }
        }

    /** Status of a join request by its code. Success(null) when no request has that code. */
    suspend fun trackJoinRequest(code: String): Result<JoinStatusResponse?> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.joinStatus(code.trim())
            when {
                response.isSuccessful -> Result.success(response.body())
                response.code() == 404 -> Result.success(null)
                else -> Result.failure(Exception(serverMessage(response) ?: "تعذر الاستعلام عن الطلب"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال بالسيرفر"))
        }
    }

    fun hasValidSession(): Boolean = !sessionManager.refreshToken.isNullOrBlank()

    fun getSavedUserId(): String? = sessionManager.getUserId()

    /** Revokes the refresh token on the server (best effort) and forgets the session locally. */
    suspend fun logout() {
        val refresh = sessionManager.refreshToken
        sessionManager.clear()
        Permissions.clear()
        if (refresh != null) withContext(Dispatchers.IO) {
            try {
                apiService.logout(mapOf("refreshToken" to refresh))
            } catch (e: Exception) {
                // offline: the token expires on its own
            }
        }
    }

    /** Local-only cleanup after the server already rejected the session. */
    fun forgetSession() {
        sessionManager.clear()
        Permissions.clear()
    }
}
