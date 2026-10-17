package com.example.data.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.AppUserEntity
import com.example.data.local.entities.CommitteeEntity
import com.example.data.local.entities.CourseEntity
import com.example.data.local.entities.ExamEntity
import com.example.data.local.entities.NotificationEntity
import com.example.data.local.entities.ScheduleTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Courses (الدورات العلمية)
 */
@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY isDefault DESC, createdAt DESC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    suspend fun getCourseById(id: String): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteCourseById(id: String)

    @Query("DELETE FROM courses")
    suspend fun clearAll()
}

/**
 * Data Access Object for Committees (اللجان الميدانية والمراحل)
 */
@Dao
interface CommitteeDao {
    @Query("UPDATE committees SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun setUpdatedAt(id: String, updatedAt: String?)

    @Query("SELECT * FROM committees ORDER BY committeeNumber ASC")
    fun getAllCommittees(): Flow<List<CommitteeEntity>>

    @Query("SELECT * FROM committees WHERE courseId = :courseId ORDER BY committeeNumber ASC")
    fun getCommitteesByCourse(courseId: String): Flow<List<CommitteeEntity>>

    @Query("SELECT * FROM committees WHERE id = :id LIMIT 1")
    suspend fun getCommitteeById(id: String): CommitteeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommittee(committee: CommitteeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommittees(committees: List<CommitteeEntity>)

    @Update
    suspend fun updateCommittee(committee: CommitteeEntity)

    @Delete
    suspend fun deleteCommittee(committee: CommitteeEntity)

    @Query("DELETE FROM committees WHERE id = :id")
    suspend fun deleteCommitteeById(id: String)

    @Query("DELETE FROM committees")
    suspend fun clearAll()
}

/**
 * Data Access Object for Competitions & Exams (بنك المسابقات والامتحانات)
 */
@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY uploadedAt DESC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    suspend fun getExamById(id: String): ExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamEntity>)

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExamById(id: String)

    @Query("DELETE FROM exams")
    suspend fun clearAll()
}

/**
 * Data Access Object for Users & Assistants (المستخدمون والمعاونون)
 */
@Dao
interface AppUserDao {
    @Query("UPDATE app_users SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun setUpdatedAt(id: String, updatedAt: String?)

    @Query("SELECT * FROM app_users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<AppUserEntity>>

    @Query("SELECT * FROM app_users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): AppUserEntity?

    @Query("SELECT * FROM app_users WHERE phone = :phone OR email = :phone LIMIT 1")
    suspend fun getUserByCredentials(phone: String): AppUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: AppUserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<AppUserEntity>)

    @Update
    suspend fun updateUser(user: AppUserEntity)

    @Delete
    suspend fun deleteUser(user: AppUserEntity)

    @Query("DELETE FROM app_users WHERE id = :id")
    suspend fun deleteUserById(id: String)

    @Query("DELETE FROM app_users")
    suspend fun clearAll()
}

/**
 * Data Access Object for Tasks & Calendar Schedule (التقويم وجدول المهام)
 */
@Dao
interface ScheduleTaskDao {
    @Query("SELECT * FROM schedule_tasks ORDER BY date ASC, time ASC")
    fun getAllTasks(): Flow<List<ScheduleTaskEntity>>

    @Query("SELECT * FROM schedule_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): ScheduleTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ScheduleTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<ScheduleTaskEntity>)

    @Update
    suspend fun updateTask(task: ScheduleTaskEntity)

    @Delete
    suspend fun deleteTask(task: ScheduleTaskEntity)

    @Query("DELETE FROM schedule_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM schedule_tasks")
    suspend fun clearAll()
}

/**
 * Data Access Object for Notifications (التنبيهات الميدانية)
 */
@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET `read` = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: String)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}

/**
 * Data Access Object for Custom Dynamic Permissions (الصلاحيات المخصصة)
 */
@Dao
interface CustomPermissionDao {
    @Query("SELECT * FROM custom_permissions")
    fun getAllCustomPermissions(): Flow<List<com.example.data.local.entities.CustomPermissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomPermission(permission: com.example.data.local.entities.CustomPermissionEntity)

    @Delete
    suspend fun deleteCustomPermission(permission: com.example.data.local.entities.CustomPermissionEntity)

    @Query("DELETE FROM custom_permissions WHERE id = :id")
    suspend fun deleteCustomPermissionById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(permissions: List<com.example.data.local.entities.CustomPermissionEntity>)

    @Query("DELETE FROM custom_permissions")
    suspend fun clearAll()
}




/**
 * Data Access Object for writes waiting to be sent to the server
 */
@Dao
interface PendingOpDao {
    @Insert
    suspend fun insert(op: com.example.data.local.entities.PendingOpEntity): Long

    /** Writes waiting to be sent for this user (or made before writes were tagged with a user). */
    @Query("SELECT * FROM pending_ops WHERE failed = 0 AND rejected = 0 AND (userId IS NULL OR userId = :userId) ORDER BY id ASC")
    suspend fun getActive(userId: String?): List<com.example.data.local.entities.PendingOpEntity>

    @Query("SELECT * FROM pending_ops WHERE failed = 0 AND rejected = 0 ORDER BY id ASC")
    suspend fun getAllActive(): List<com.example.data.local.entities.PendingOpEntity>

    @Query("SELECT * FROM pending_ops WHERE failed = 1 ORDER BY id ASC")
    suspend fun getFailed(): List<com.example.data.local.entities.PendingOpEntity>

    @Query("SELECT * FROM pending_ops WHERE rejected = 1 ORDER BY id ASC")
    suspend fun getRejected(): List<com.example.data.local.entities.PendingOpEntity>

    @Query("SELECT COUNT(*) FROM pending_ops WHERE failed = 0 AND rejected = 0 AND (userId IS NULL OR userId = :userId)")
    suspend fun countActive(userId: String?): Int

    /** Writes of any user that have not reached the server (shown before logout and sign-in). */
    @Query("SELECT COUNT(*) FROM pending_ops WHERE rejected = 0")
    suspend fun countUnsent(): Int

    @Query("SELECT DISTINCT userId FROM pending_ops WHERE userId IS NOT NULL AND rejected = 0")
    suspend fun unsentUserIds(): List<String>

    /** Takes a write for sending; 0 rows means another flusher has it (MOB-04). */
    @Query("UPDATE pending_ops SET claimedAt = :now WHERE id = :id AND (claimedAt IS NULL OR claimedAt < :staleBefore)")
    suspend fun claim(id: Long, now: Long, staleBefore: Long): Int

    @Query("UPDATE pending_ops SET claimedAt = NULL WHERE id = :id")
    suspend fun release(id: Long)

    @Query("UPDATE pending_ops SET attempts = :attempts, failed = :failed, lastError = :error, claimedAt = NULL WHERE id = :id")
    suspend fun recordFailure(id: Long, attempts: Int, failed: Boolean, error: String?)

    @Query("UPDATE pending_ops SET rejected = 1, httpCode = :code, lastError = :error, claimedAt = NULL WHERE id = :id")
    suspend fun markRejected(id: Long, code: Int, error: String?)

    @Query("UPDATE pending_ops SET failed = 0, attempts = 0, lastError = NULL WHERE failed = 1")
    suspend fun unparkAll()

    @Query("UPDATE pending_ops SET rejected = 0, httpCode = NULL, lastError = NULL, attempts = 0 WHERE id = :id")
    suspend fun unreject(id: Long)

    @Query("DELETE FROM pending_ops WHERE failed = 1")
    suspend fun deleteFailed()

    @Query("DELETE FROM pending_ops WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_ops WHERE userId IS NOT NULL AND userId <> :userId")
    suspend fun deleteOtherUsers(userId: String)

    @Query("DELETE FROM pending_ops")
    suspend fun clearAll()
}
