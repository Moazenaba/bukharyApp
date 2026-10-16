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
    val notes: String = ""
)
