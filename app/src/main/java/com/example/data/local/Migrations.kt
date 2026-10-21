package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations. Unsent writes (pending_ops) are kept across upgrades, so a version
 * bump never silently discards changes the server has not received yet. Tables that are
 * only a cache of server data are rebuilt and refilled at the next sync.
 */
object Migrations {

    /** 1 -> 2: adds the offline write queue. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `pending_ops` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`label` TEXT NOT NULL, `method` TEXT NOT NULL, `path` TEXT NOT NULL, `bodyJson` TEXT, " +
                    "`createdAt` INTEGER NOT NULL)"
            )
        }
    }

    /**
     * 2 -> 3: exams keep only "has a file" (the PDF itself stays on the server); the queue
     * gains retry bookkeeping; the unused roles table goes; custom permissions keep only
     * the fields the server defines.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `roles`")

            db.execSQL("ALTER TABLE `exams` RENAME TO `exams_old`")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `exams` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `book` TEXT NOT NULL, " +
                    "`branch` TEXT NOT NULL, `totalMarks` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, " +
                    "`questionsCount` INTEGER NOT NULL, `pdfFileName` TEXT NOT NULL, `pdfFileSize` TEXT NOT NULL, " +
                    "`hasFile` INTEGER NOT NULL, `uploadedAt` TEXT NOT NULL, `sampleQuestions` TEXT NOT NULL, " +
                    "`notes` TEXT NOT NULL, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "INSERT INTO `exams` (`id`, `title`, `book`, `branch`, `totalMarks`, `durationMinutes`, `questionsCount`, " +
                    "`pdfFileName`, `pdfFileSize`, `hasFile`, `uploadedAt`, `sampleQuestions`, `notes`) " +
                    "SELECT `id`, `title`, `book`, `branch`, `totalMarks`, `durationMinutes`, `questionsCount`, " +
                    "`pdfFileName`, `pdfFileSize`, 0, `uploadedAt`, `sampleQuestions`, `notes` FROM `exams_old`"
            )
            db.execSQL("DROP TABLE `exams_old`")

            db.execSQL("ALTER TABLE `custom_permissions` RENAME TO `custom_permissions_old`")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `custom_permissions` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                    "`description` TEXT NOT NULL, PRIMARY KEY(`id`))"
            )
            db.execSQL("INSERT INTO `custom_permissions` (`id`, `name`, `description`) SELECT `id`, `name`, `description` FROM `custom_permissions_old`")
            db.execSQL("DROP TABLE `custom_permissions_old`")

            db.execSQL("ALTER TABLE `pending_ops` RENAME TO `pending_ops_old`")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `pending_ops` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`label` TEXT NOT NULL, `method` TEXT NOT NULL, `path` TEXT NOT NULL, `bodyJson` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, `failed` INTEGER NOT NULL, `lastError` TEXT)"
            )
            db.execSQL(
                "INSERT INTO `pending_ops` (`id`, `label`, `method`, `path`, `bodyJson`, `createdAt`, `attempts`, `failed`, `lastError`) " +
                    "SELECT `id`, `label`, `method`, `path`, `bodyJson`, `createdAt`, 0, 0, NULL FROM `pending_ops_old`"
            )
            db.execSQL("DROP TABLE `pending_ops_old`")
        }
    }

    /**
     * 3 -> 4: records carry the server version (updatedAt) so a stale write is refused
     * rather than overwriting newer data; queued writes remember which record they belong to.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `committees` ADD COLUMN `updatedAt` TEXT")
            db.execSQL("ALTER TABLE `app_users` ADD COLUMN `updatedAt` TEXT")

            db.execSQL("ALTER TABLE `pending_ops` RENAME TO `pending_ops_old`")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `pending_ops` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`label` TEXT NOT NULL, `method` TEXT NOT NULL, `path` TEXT NOT NULL, `bodyJson` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, `failed` INTEGER NOT NULL, " +
                    "`lastError` TEXT, `versionOf` TEXT)"
            )
            db.execSQL(
                "INSERT INTO `pending_ops` (`id`, `label`, `method`, `path`, `bodyJson`, `createdAt`, `attempts`, `failed`, `lastError`, `versionOf`) " +
                    "SELECT `id`, `label`, `method`, `path`, `bodyJson`, `createdAt`, `attempts`, `failed`, `lastError`, NULL FROM `pending_ops_old`"
            )
            db.execSQL("DROP TABLE `pending_ops_old`")
        }
    }

    /**
     * 4 -> 5: queued writes remember who made them and carry an idempotency key; refused writes
     * stay visible instead of being dropped; users carry committee ids (MOB-01, MOB-03, MOB-04).
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `pending_ops` ADD COLUMN `userId` TEXT")
            db.execSQL("ALTER TABLE `pending_ops` ADD COLUMN `idempotencyKey` TEXT NOT NULL DEFAULT ''")
            // Existing writes get their own key; hex(randomblob) is unique enough for one device.
            db.execSQL("UPDATE `pending_ops` SET `idempotencyKey` = lower(hex(randomblob(16))) WHERE `idempotencyKey` = ''")
            db.execSQL("ALTER TABLE `pending_ops` ADD COLUMN `rejected` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `pending_ops` ADD COLUMN `httpCode` INTEGER")
            db.execSQL("ALTER TABLE `pending_ops` ADD COLUMN `claimedAt` INTEGER")
            db.execSQL("ALTER TABLE `app_users` ADD COLUMN `assignedCommitteeIds` TEXT NOT NULL DEFAULT '[]'")
            db.execSQL("ALTER TABLE `app_users` ADD COLUMN `allCommitteesAllowed` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `app_users` ADD COLUMN `mustChangePassword` INTEGER NOT NULL DEFAULT 0")
        }
    }

    /** 5 -> 6: committees list their staff (committee heads and sheikhs), from the server. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `committees` ADD COLUMN `staff` TEXT NOT NULL DEFAULT '[]'")
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
}
