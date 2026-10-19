package com.example.data.remote

import com.example.data.local.entities.AppUserEntity
import com.example.data.local.entities.CommitteeEntity
import com.example.data.local.entities.CourseEntity
import com.example.data.local.entities.ExamEntity
import com.example.data.local.entities.NotificationEntity
import com.example.data.local.entities.ScheduleTaskEntity
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class LoginRequest(
    val identifier: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean = false,
    val token: String? = null,
    val refreshToken: String? = null,
    val user: AppUserEntity? = null,
    val mustChangePassword: Boolean = false,
    val message: String? = null
)

/** What the server allows this user to do (server/policy.ts), evaluated by the server. */
data class ServerPermissions(
    val role: String = "",
    val actions: Map<String, Boolean> = emptyMap(),
    /** "all" or a list of committee ids. */
    val committeeScope: Any? = null,
    /** "all" or a list of exam ids. */
    val exams: Any? = null,
    val mustChangePassword: Boolean = false
)

data class MeResponse(
    val success: Boolean = false,
    val user: AppUserEntity? = null,
    val permissions: ServerPermissions = ServerPermissions(),
    val mustChangePassword: Boolean = false
)

data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

/** Someone a task may be assigned to (GET /api/users/assignable). */
data class AssignableUserDto(val id: String, val name: String = "", val role: String = "")
data class AssignableUsersResponse(val success: Boolean = false, val users: List<AssignableUserDto> = emptyList())

data class RegisterResponse(val success: Boolean = false, val requestCode: String? = null, val message: String? = null)

/** Public status of a join request; nothing personal is returned. */
data class JoinStatusResponse(val success: Boolean = false, val status: String? = null, val interviewDate: String? = null)

/** A saved course-sheet row (Web: SummerCourseRow). Read-only on mobile (D-09: sheet editing stays on the web). */
data class SheetRowDto(
    val id: String,
    val number: Int = 0,
    val sheikh_name: String = "",
    val place: String = "",
    val book: String = "",
    val students_count: Int = 0,
    val delivered: Boolean = false,
    val corrected: Boolean = false,
    val reviewed: Boolean = false,
    val announced: Boolean = false,
    val ceremony: Boolean = false,
    val struck_out: Boolean = false,
    val uncertain: Boolean = false,
    val notes: String = "",
    val committeeId: String? = null,
    val courseId: String? = null,
    val courseName: String? = null
)
data class SheetRowsResponse(val success: Boolean = false, val rows: List<SheetRowDto> = emptyList())

data class CustomPermissionDto(val id: String, val label: String = "", val description: String = "")
data class CustomPermissionsResponse(val success: Boolean = false, val permissions: List<CustomPermissionDto> = emptyList())

data class DbTable(val name: String = "", val count: Long = 0)
data class DbMigrations(val total: Int = 0, val pending: List<String> = emptyList())
data class DatabaseStatusResponse(
    val database: String = "",
    val postgresVersion: String = "",
    val latencyMs: Long = 0,
    val tls: String = "",
    val migrations: DbMigrations = DbMigrations(),
    val tables: List<DbTable> = emptyList()
)

data class RegisterRequest(
    val name: String,
    val phone: String,
    val email: String,
    val roleKey: String,
    val password: String,
    val notes: String?,
    val whatsapp: String? = null,
    val nationalId: String? = null,
    val quranLevel: String? = null,
    val qualification: String? = null,
    val experience: String? = null,
    val availability: String? = null,
    val preferredCommittees: List<Int>? = null
)

/** The join request's dossier, the same fields as the web form. */
data class JoinRequestExtras(
    val notes: String = "",
    val whatsapp: String = "",
    val nationalId: String = "",
    val quranLevel: String = "",
    val qualification: String = "",
    val experience: String = "",
    val availability: String = "",
    val preferredCommittees: List<Int> = emptyList()
)

// The server (Web/server.ts) wraps every list in an envelope: { success, <name>: [...] }.
data class CoursesResponse(val success: Boolean = false, val courses: List<CourseEntity> = emptyList())
data class CommitteesResponse(val success: Boolean = false, val committees: List<CommitteeEntity> = emptyList())
data class UsersResponse(val success: Boolean = false, val users: List<AppUserEntity> = emptyList())
data class ExamsResponse(val success: Boolean = false, val exams: List<ExamEntity> = emptyList())
data class TasksResponse(val success: Boolean = false, val tasks: List<ScheduleTaskEntity> = emptyList())
data class NotificationsResponse(val success: Boolean = false, val notifications: List<NotificationEntity> = emptyList())

/** Generic write acknowledgement: { success, message? } */
data class ApiAck(val success: Boolean = false, val message: String? = null)

data class TaskToggleRequest(val completed: Boolean, val completedAt: String?)

/**
 * Read routes of Web/server.ts. Writes are queued and sent by
 * BukhariRepository.push so they survive being offline.
 */
interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("api/auth/join-status/{code}")
    suspend fun joinStatus(@Path("code") code: String): Response<JoinStatusResponse>

    @GET("api/auth/me")
    suspend fun me(): Response<MeResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Body body: Map<String, String>): Response<ApiAck>

    /** Returns a new session (other sessions of the user are ended by the server). */
    @POST("api/auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<LoginResponse>

    @GET("api/users/assignable")
    suspend fun getAssignableUsers(): Response<AssignableUsersResponse>

    @GET("api/courses")
    suspend fun getCourses(): Response<CoursesResponse>

    @GET("api/committees")
    suspend fun getCommittees(): Response<CommitteesResponse>

    @GET("api/users")
    suspend fun getUsers(): Response<UsersResponse>

    /** Exam metadata only; the PDF is downloaded on demand by allowed roles. */
    @GET("api/exams")
    suspend fun getExams(): Response<ExamsResponse>

    @GET("api/tasks")
    suspend fun getTasks(): Response<TasksResponse>

    @GET("api/notifications")
    suspend fun getNotifications(): Response<NotificationsResponse>

    @GET("api/summer-course-rows")
    suspend fun getSheetRows(): Response<SheetRowsResponse>

    @GET("api/custom-permissions")
    suspend fun getCustomPermissions(): Response<CustomPermissionsResponse>

    @GET("api/database/status")
    suspend fun getDatabaseStatus(): Response<DatabaseStatusResponse>
}
