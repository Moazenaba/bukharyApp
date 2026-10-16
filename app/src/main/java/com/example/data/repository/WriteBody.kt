package com.example.data.repository

import com.example.data.remote.ApiClient
import com.squareup.moshi.Types

/** JSON helpers for the write queue: attach the record version and read the version the server returns. */
object WriteBody {
    private val mapAdapter = ApiClient.moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    /** A record the server returned, with its server version. */
    data class ServerVersion(val kind: String, val id: String, val updatedAt: String)

    /**
     * Adds `expectedUpdatedAt` to a JSON object body. The server refuses the write (409) when the
     * record changed since this version was seen. Returns the body unchanged if it is not an object.
     */
    fun withExpectedUpdatedAt(json: String?, updatedAt: String): String? {
        if (json == null) return null
        val map = try {
            mapAdapter.fromJson(json)
        } catch (e: Exception) {
            return json
        } ?: return json
        @Suppress("UNCHECKED_CAST")
        val out = (normalize(map) as Map<String, Any?>).toMutableMap()
        out["expectedUpdatedAt"] = updatedAt
        return mapAdapter.toJson(out)
    }

    /** The record (committee or user) the server returned after a successful write, if any. */
    fun serverVersion(responseBody: String?): ServerVersion? {
        if (responseBody.isNullOrBlank()) return null
        val map = try {
            mapAdapter.fromJson(responseBody)
        } catch (e: Exception) {
            return null
        } ?: return null
        for (kind in listOf("committee", "user")) {
            val record = map[kind] as? Map<*, *> ?: continue
            val id = record["id"] as? String ?: continue
            val updatedAt = record["updatedAt"] as? String ?: continue
            return ServerVersion(kind, id, updatedAt)
        }
        return null
    }

    /** Moshi reads every JSON number as Double; whole numbers go back out as integers. */
    private fun normalize(value: Any?): Any? = when (value) {
        is Double -> if (value % 1.0 == 0.0 && value.isFinite()) value.toLong() else value
        is Map<*, *> -> value.entries.associate { (k, v) -> k.toString() to normalize(v) }
        is List<*> -> value.map { normalize(it) }
        else -> value
    }
}
