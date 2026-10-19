package com.example.data.remote

import android.content.Context
import com.example.data.model.AppUser
import com.example.data.model.UserRole
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
    private val permissionsAdapter = ApiClient.moshi.adapter(ServerPermissions::class.java)

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
                val user = body.user.toDomain().copy(mustChangePassword = body.mustChangePassword || body.user.mustChangePassword)
                sessionManager.saveUserId(user.id)
                // The server-evaluated permissions decide what the UI offers. All or nothing: if they
                // cannot be loaded, no half-made session may be restored on the next launch.
                val me = refreshMe()
                if (me == null) {
                    forgetSession()
                    return@withContext AuthResult.Error("تعذر تحميل صلاحيات الحساب، حاول مرة أخرى")
                }
                return@withContext AuthResult.Success(me)
            }
            AuthResult.Error(serverMessage(response) ?: "بيانات الدخول غير صحيحة")
        } catch (e: Exception) {
            AuthResult.Error("تعذر الاتصال بالسيرفر، تأكد من اتصالك بالإنترنت ثم حاول مرة أخرى")
        }
    }

    /**
     * Reloads the user and their permissions from the server. Null when offline or rejected.
     * An expired access token is refreshed for this call too (MOB-05).
     */
    suspend fun refreshMe(): AppUser? = withContext(Dispatchers.IO) {
        try {
            val res = apiService.me()
            val body = res.body()
            if (!res.isSuccessful || body?.user == null) return@withContext null
            val must = body.mustChangePassword || body.permissions.mustChangePassword || body.user.mustChangePassword
            val user = body.user.toDomain().copy(mustChangePassword = must)
            applyPermissions(user, body.permissions)
            sessionManager.savePermissionsJson(permissionsAdapter.toJson(body.permissions))
            user
        } catch (e: Exception) {
            null
        }
    }

    private fun applyPermissions(user: AppUser, permissions: ServerPermissions) {
        Permissions.actions = permissions.actions
        Permissions.role = user.role.roleKey
        Permissions.applyServerScope(permissions.committeeScope, permissions.exams)
    }

    /** Uses the last known permissions until the next successful /me (e.g. starting offline). */
    private fun restorePermissions() {
        val json = sessionManager.getPermissionsJson() ?: return
        val saved = try {
            permissionsAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        } ?: return
        Permissions.actions = saved.actions
        Permissions.applyServerScope(saved.committeeScope, saved.exams)
    }

    fun restoreUserPermissions(user: AppUser) {
        Permissions.role = user.role.roleKey
    }

    /**
     * Changes the own password (MOB-08). The server ends the other sessions and returns a new one,
     * which replaces the current tokens.
     */
    suspend fun changePassword(current: String, next: String): Result<AppUser> = withContext(Dispatchers.IO) {
        try {
            val res = apiService.changePassword(ChangePasswordRequest(current, next))
            val body = res.body()
            if (!res.isSuccessful || body?.token == null || body.refreshToken == null) {
                return@withContext Result.failure(Exception(serverMessage(res) ?: "تعذر تغيير كلمة المرور"))
            }
            sessionManager.saveTokens(body.token, body.refreshToken)
            val me = refreshMe() ?: return@withContext Result.failure(Exception("تم تغيير كلمة المرور، لكن تعذر تحميل الحساب"))
            Result.success(me)
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال بالسيرفر"))
        }
    }

    /** People the user may assign tasks to (id and name), limited by the server to their committees. */
    suspend fun assignableUsers(): List<AssignableUserDto> = withContext(Dispatchers.IO) {
        try {
            apiService.getAssignableUsers().body()?.users ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serverMessage(response: Response<*>): String? = ApiClient.serverMessage(response.errorBody()?.string())

    /** Public join request; returns the server-issued request code. */
    suspend fun register(name: String, phone: String, email: String, role: UserRole, pass: String, extras: JoinRequestExtras): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.register(
                    RegisterRequest(
                        name = name.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        roleKey = role.roleKey,
                        password = pass,
                        notes = extras.notes.trim().ifBlank { null },
                        whatsapp = extras.whatsapp.trim().ifBlank { null },
                        nationalId = extras.nationalId.trim().ifBlank { null },
                        quranLevel = extras.quranLevel.trim().ifBlank { null },
                        qualification = extras.qualification.trim().ifBlank { null },
                        experience = extras.experience.trim().ifBlank { null },
                        availability = extras.availability.trim().ifBlank { null },
                        preferredCommittees = extras.preferredCommittees.ifEmpty { null }
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

    /** Ends the session on the server (best effort; its access token stops working) and forgets it locally. */
    suspend fun logout() {
        val refresh = sessionManager.refreshToken
        withContext(Dispatchers.IO) {
            try {
                // Sent while the access token is still stored, so the server ends this session.
                if (refresh != null) apiService.logout(mapOf("refreshToken" to refresh))
            } catch (e: Exception) {
                // offline: the refresh token expires on its own
            }
        }
        sessionManager.clear()
        Permissions.clear()
    }

    /** Local-only cleanup after the server already rejected the session. */
    fun forgetSession() {
        sessionManager.clear()
        Permissions.clear()
    }
}
