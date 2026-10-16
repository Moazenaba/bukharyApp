package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A write that has not reached the server yet (offline or server error).
 * Replayed in insertion order before every sync and whenever the network returns.
 */
@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val method: String,
    val path: String,
    val bodyJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Server errors (5xx) so far; after too many the write is parked as failed. */
    val attempts: Int = 0,
    /** Parked: no longer retried automatically and does not block syncing; the user retries or discards it. */
    val failed: Boolean = false,
    val lastError: String? = null,
    /** "committee:<id>" or "user:<id>": the record whose current server version is sent with this write. */
    val versionOf: String? = null
)
