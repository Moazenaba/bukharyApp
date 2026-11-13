package com.example.data.model

data class Exam(
    val id: String,
    val title: String,
    val book: String,
    val branch: String = "",
    val totalMarks: Int = 100,
    val durationMinutes: Int = 60,
    val questionsCount: Int = 0,
    /** A question PDF is stored on the server; only super admins and committee heads may open it. */
    val hasFile: Boolean = false,
    val pdfFileName: String = "",
    val pdfFileSize: String = "",
    val uploadedAt: String = "",
    val sampleQuestions: List<String> = emptyList(),
    val notes: String = "",
    /** When the paper may be downloaded (ISO time, set on the website); null = as soon as uploaded. */
    val opensAt: String? = null,
    /** Set by the server while the paper is still closed for this user: the time it opens (ISO). */
    val lockedUntil: String? = null
)
