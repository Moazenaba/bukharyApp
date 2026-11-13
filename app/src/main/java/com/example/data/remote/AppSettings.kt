package com.example.data.remote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The website's «إعدادات النظام» (GET /api/settings); the defaults are the server's. */
data class AppSettings(
    val observers: List<String> = emptyList(),
    val ceremonySheikhs: List<String> = emptyList(),
    val overdueDays: Int = 3,
    val examOpenHoursBefore: Int = 24
) {
    companion object {
        /** The last settings loaded from the server (Compose state: screens update when it changes). */
        var current by mutableStateOf(AppSettings())

        private fun names(v: Any?) = (v as? List<*>)?.mapNotNull { (it as? String)?.trim()?.ifBlank { null } }?.distinct() ?: emptyList()
        private fun int(v: Any?, def: Int) = (v as? Number)?.toInt() ?: (v as? String)?.toIntOrNull() ?: def

        fun from(raw: Map<*, *>) = AppSettings(
            observers = names(raw["observers"]),
            ceremonySheikhs = names(raw["ceremony_sheikhs"]),
            overdueDays = int(raw["overdue_days"], 3).coerceIn(1, 90),
            examOpenHoursBefore = int(raw["exam_open_hours_before"], 24)
        )
    }
}
