package com.example.data.model

data class Course(
    val id: String,
    val name: String,
    val season: String = "",
    val year: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "active", // active, completed, upcoming
    val isDefault: Boolean = false,
    val targetCommitteesCount: Int = 0,
    val description: String = "",
    val createdAt: String = ""
)
