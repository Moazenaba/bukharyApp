package com.example.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Upgrading a real version-4 database (built from the exported schema) to the current version
 * (MOB-18). Room validates every table against the entities after migrating, so a migration that
 * forgets a column fails here; unsent writes must survive the upgrade.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    private data class Field(val columnName: String)
    private data class Index(val createSql: String)
    private data class EntitySchema(val tableName: String, val createSql: String, val indices: List<Index>? = null)
    private data class Database(val version: Int, val entities: List<EntitySchema>, val setupQueries: List<String>)
    private data class Schema(val database: Database)

    private fun schema(version: Int): Database {
        val file = File("schemas/com.example.data.local.AppDatabase/$version.json")
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        return moshi.adapter(Schema::class.java).fromJson(file.readText())!!.database
    }

    @Test
    fun `version 4 upgrades to the current schema and keeps unsent writes`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val v4 = schema(4)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, null).use { raw ->
            v4.entities.forEach { e ->
                raw.execSQL(e.createSql.replace("\${TABLE_NAME}", e.tableName))
                e.indices?.forEach { raw.execSQL(it.createSql.replace("\${TABLE_NAME}", e.tableName)) }
            }
            v4.setupQueries.forEach { raw.execSQL(it) }
            raw.execSQL(
                "INSERT INTO pending_ops (label, method, path, bodyJson, createdAt, attempts, failed, lastError, versionOf) " +
                    "VALUES ('offline-reception', 'PATCH', 'api/committees/c1', '{}', 1, 0, 0, NULL, NULL)"
            )
            raw.execSQL(
                "INSERT INTO app_users (id, name, phone, email, passportNumber, roleId, roleKey, status, assignedCommittees, permissions, createdAt, approvedAt, approvedBy, notes, updatedAt) " +
                    "VALUES ('u1', 'معاون', '05', '', '', 'assistant', 'assistant', 'approved', '[1]', '[]', '', NULL, NULL, '', NULL)"
            )
            raw.version = 4
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val ops = db.pendingOpDao().getAllActive()
            assertEquals(listOf("offline-reception"), ops.map { it.label })
            assertTrue("existing writes get an idempotency key", ops.single().idempotencyKey.isNotBlank())
            val user = db.appUserDao().getUserById("u1")!!
            assertEquals(emptyList<String>(), user.assignedCommitteeIds)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
