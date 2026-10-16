package com.example.data.model

data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val type: String = "info", // urgent, info, success
    val read: Boolean = false,
    val committeeId: String? = null,
    /** ISO timestamp from the server. */
    val createdAt: String = ""
)

data class DatabaseTableCount(
    val name: String,
    val nameAr: String,
    val count: Long
)

/**
 * What the app knows about its connection and data — measured, never assumed.
 * [server] is filled only for super admins (GET /api/database/status).
 */
data class DatabaseStatus(
    val isOnline: Boolean,
    val message: String,
    val serverUrl: String,
    val lastSyncAt: Long?,
    val pendingWrites: Int,
    val failedWrites: List<FailedWrite>,
    /** Writes the server refused (4xx), kept for the user to retry or discard. */
    val refusedWrites: List<com.example.data.repository.RefusedWrite> = emptyList(),
    /** Records skipped during the last sync because they could not be read. */
    val skippedRecords: Int = 0,
    val localTables: List<DatabaseTableCount>,
    val server: ServerDatabaseInfo? = null
)

data class FailedWrite(val id: Long, val label: String, val error: String?)

data class ServerDatabaseInfo(
    val database: String,
    val postgresVersion: String,
    val latencyMs: Long,
    val tls: String,
    val pendingMigrations: List<String>,
    val tables: List<DatabaseTableCount>
)
