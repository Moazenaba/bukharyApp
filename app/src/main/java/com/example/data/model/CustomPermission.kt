package com.example.data.model

/** A permission label defined by the super admin (shared with the web app via /api/custom-permissions). */
data class CustomPermission(
    val id: String,
    val name: String,
    val description: String = ""
)
