package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.daos.AppUserDao
import com.example.data.local.daos.CommitteeDao
import com.example.data.local.daos.CourseDao
import com.example.data.local.daos.ExamDao
import com.example.data.local.daos.NotificationDao
import com.example.data.local.daos.ScheduleTaskDao
import com.example.data.local.entities.AppUserEntity
import com.example.data.local.entities.CommitteeEntity
import com.example.data.local.entities.CourseEntity
import com.example.data.local.entities.ExamEntity
import com.example.data.local.entities.NotificationEntity
import com.example.data.local.entities.ScheduleTaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CourseEntity::class,
        CommitteeEntity::class,
        AppUserEntity::class,
        ExamEntity::class,
        ScheduleTaskEntity::class,
        NotificationEntity::class,
        com.example.data.local.entities.CustomPermissionEntity::class,
        com.example.data.local.entities.PendingOpEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun committeeDao(): CommitteeDao
    abstract fun appUserDao(): AppUserDao
    abstract fun examDao(): ExamDao
    abstract fun scheduleTaskDao(): ScheduleTaskDao
    abstract fun notificationDao(): NotificationDao
    abstract fun customPermissionDao(): com.example.data.local.daos.CustomPermissionDao
    abstract fun pendingOpDao(): com.example.data.local.daos.PendingOpDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bukhari_competition.db"
                )
                    // No destructive fallback: unsent writes must survive upgrades.
                    .addMigrations(*Migrations.ALL)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
