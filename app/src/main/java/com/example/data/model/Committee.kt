package com.example.data.model

import com.squareup.moshi.Json

data class StageItem(
    val completed: Boolean = false,
    val completedAt: String? = null,
    val notes: String? = null,
    /** Who completed it (set by the server). */
    val completedBy: String? = null,
    /** Link to the committee's Excel grade sheet (sheet stage). */
    val excelUrl: String? = null
)

data class StagesMap(
    val registration: StageItem = StageItem(),
    val exam: StageItem = StageItem(),
    val scoring: StageItem = StageItem(),
    val aggregation: StageItem = StageItem(),
    val review: StageItem = StageItem(),
    val sheet: StageItem = StageItem(),
    val accreditation: StageItem = StageItem(),
    val announcement: StageItem = StageItem(),
    val ceremony: StageItem = StageItem()
) {
    fun getStage(key: String): StageItem {
        return when (key) {
            "registration" -> registration
            "exam" -> exam
            "scoring" -> scoring
            "aggregation" -> aggregation
            "review" -> review
            "sheet" -> sheet
            "accreditation" -> accreditation
            "announcement" -> announcement
            "ceremony" -> ceremony
            else -> StageItem()
        }
    }

    /** Changes completion of one stage, keeping its notes. */
    fun updateStage(key: String, completed: Boolean, completedAt: String? = null): StagesMap {
        val updated = getStage(key).copy(completed = completed, completedAt = completedAt)
        return when (key) {
            "registration" -> copy(registration = updated)
            "exam" -> copy(exam = updated)
            "scoring" -> copy(scoring = updated)
            "aggregation" -> copy(aggregation = updated)
            "review" -> copy(review = updated)
            "sheet" -> copy(sheet = updated)
            "accreditation" -> copy(accreditation = updated)
            "announcement" -> copy(announcement = updated)
            "ceremony" -> copy(ceremony = updated)
            else -> this
        }
    }

    fun getCurrentActiveStage(): String {
        val keys = listOf(
            "ceremony", "announcement", "accreditation", "sheet",
            "review", "aggregation", "scoring", "exam", "registration"
        )
        // Find the latest completed stage or the next stage to complete
        for (i in keys.indices.reversed()) {
            val key = keys[i]
            if (!getStage(key).completed) {
                return key
            }
        }
        return "ceremony"
    }

    fun getCompletedCount(): Int {
        var count = 0
        if (registration.completed) count++
        if (exam.completed) count++
        if (scoring.completed) count++
        if (aggregation.completed) count++
        if (review.completed) count++
        if (sheet.completed) count++
        if (accreditation.completed) count++
        if (announcement.completed) count++
        if (ceremony.completed) count++
        return count
    }

    val progressFraction: Float get() = getCompletedCount() / 9f
}

// JSON names below match Web/src/types/index.ts, which is what the web app writes
// to the shared database. Kotlin names are kept so the UI code stays unchanged.

data class QuestionScoringItem(
    val id: String = "",
    val questionNumber: Int = 0,
    val label: String = "",
    val completed: Boolean = false,
    val completedAt: String? = null,
    val scorers: List<String> = emptyList(),
    val score: Double? = null,
    val notes: String? = null
)

data class ScoringDetails(
    @Json(name = "scorerName") val examinerName: String = "",
    @Json(name = "scoringReviewerName") val reviewerName: String = "",
    val questions: List<QuestionScoringItem> = emptyList(),
    val notes: String = ""
) {
    /** Question label -> corrected, in question order. */
    val questionsStatus: Map<String, Boolean>
        get() = questions.sortedBy { it.questionNumber }
            .associate { it.displayLabel() to it.completed }

    /** Applies checkbox changes from the UI while keeping each question's other fields. */
    fun withQuestionsStatus(status: Map<String, Boolean>): ScoringDetails {
        val byLabel = questions.associateBy { it.displayLabel() }
        val updated = status.entries.mapIndexed { index, (label, done) ->
            val existing = byLabel[label] ?: QuestionScoringItem(
                id = "q-${index + 1}",
                questionNumber = index + 1,
                label = label
            )
            existing.copy(completed = done, completedAt = if (done) existing.completedAt else null)
        }
        return copy(questions = updated)
    }

    private fun QuestionScoringItem.displayLabel() = label.ifBlank { "السؤال $questionNumber" }
}

data class AggregationDetails(
    val aggregatorName: String = "",
    @Json(name = "aggregationReviewerName") val reviewerName: String = "",
    val scoringReviewerName: String = "",
    @Json(name = "reviewConfirmed") val isConfirmedMatch: Boolean = false,
    val date: String = "",
    val notes: String = ""
)

data class CeremonyDetails(
    @Json(name = "ceremonyDate") val date: String = "",
    @Json(name = "ceremonyTime") val time: String = "",
    @Json(name = "ceremonyPlace") val venue: String = "",
    @Json(name = "honoredStudentsCount") val honoreesCount: Int = 0,
    val attendingSheikhs: List<String> = emptyList(),
    val notes: String = "",
    /** Accounts invited to the ceremony (the server notifies each one). */
    val attendees: List<CeremonyAttendee> = emptyList()
)

data class CeremonyAttendee(val id: String = "", val name: String = "")

data class AssistantReception(
    val assistantId: String = "",
    @Json(name = "assistantName") val receivedByName: String = "",
    val receivedAt: String = "",
    val status: String = "received",
    val phone: String = "",
    val notes: String = ""
)

data class ExamApproval(
    val assistantId: String = "",
    @Json(name = "assistantName") val approvedByName: String = "",
    val approvedAt: String = "",
    val notes: String = ""
)

data class Committee(
    val id: String,
    val committeeNumber: Int,
    val committeeName: String,
    val associationName: String = "",
    val members: List<String> = emptyList(),
    val branch: String = "",
    val competitionBook: String = "",
    val generalNotes: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val sheikhName: String = "",
    val sheikhPhone: String = "",
    val sheikhCountry: String = "",
    val studentsCount: Int = 0,
    val isWomenCommittee: Boolean = false,
    val stages: StagesMap = StagesMap(),
    val scoringDetails: ScoringDetails = ScoringDetails(),
    val aggregationDetails: AggregationDetails = AggregationDetails(),
    val ceremonyDetails: CeremonyDetails = CeremonyDetails(),
    val receivedByAssistant: AssistantReception? = null,
    val examApprovedBy: ExamApproval? = null,
    val createdDate: String = "",
    val createdAtTime: String = "",
    /** Server version; sent back on save so a stale edit is refused instead of overwriting newer data. */
    val updatedAt: String? = null
)
