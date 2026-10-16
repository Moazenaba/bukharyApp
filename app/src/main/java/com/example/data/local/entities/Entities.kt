package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.example.data.model.AggregationDetails
import com.example.data.model.AppUser
import com.example.data.model.AssistantReception
import com.example.data.model.CeremonyDetails
import com.example.data.model.Committee
import com.example.data.model.Course
import com.example.data.model.Exam
import com.example.data.model.ExamApproval
import com.example.data.model.NotificationItem
import com.example.data.model.ScheduleTask
import com.example.data.model.ScoringDetails
import com.example.data.model.StagesMap
import com.example.data.model.UserRole

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val season: String = "",
    val year: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "active",
    val isDefault: Boolean = false,
    val targetCommitteesCount: Int = 0,
    val description: String = "",
    val createdAt: String = ""
) {
    fun toDomain() = Course(
        id = id,
        name = name,
        season = season,
        year = year,
        startDate = startDate,
        endDate = endDate,
        status = status,
        isDefault = isDefault,
        targetCommitteesCount = targetCommitteesCount,
        description = description,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(model: Course) = CourseEntity(
            id = model.id,
            name = model.name,
            season = model.season,
            year = model.year,
            startDate = model.startDate,
            endDate = model.endDate,
            status = model.status,
            isDefault = model.isDefault,
            targetCommitteesCount = model.targetCommitteesCount,
            description = model.description,
            createdAt = model.createdAt
        )
    }
}

@Entity(tableName = "committees")
data class CommitteeEntity(
    @PrimaryKey val id: String,
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
    val updatedAt: String? = null
) {
    fun toDomain() = Committee(
        id = id,
        committeeNumber = committeeNumber,
        committeeName = committeeName,
        associationName = associationName,
        members = members,
        branch = branch,
        competitionBook = competitionBook,
        generalNotes = generalNotes,
        courseId = courseId,
        courseName = courseName,
        sheikhName = sheikhName,
        sheikhPhone = sheikhPhone,
        sheikhCountry = sheikhCountry,
        studentsCount = studentsCount,
        isWomenCommittee = isWomenCommittee,
        stages = stages,
        scoringDetails = scoringDetails,
        aggregationDetails = aggregationDetails,
        ceremonyDetails = ceremonyDetails,
        receivedByAssistant = receivedByAssistant,
        examApprovedBy = examApprovedBy,
        createdDate = createdDate,
        createdAtTime = createdAtTime,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(model: Committee) = CommitteeEntity(
            id = model.id,
            committeeNumber = model.committeeNumber,
            committeeName = model.committeeName,
            associationName = model.associationName,
            members = model.members,
            branch = model.branch,
            competitionBook = model.competitionBook,
            generalNotes = model.generalNotes,
            courseId = model.courseId,
            courseName = model.courseName,
            sheikhName = model.sheikhName,
            sheikhPhone = model.sheikhPhone,
            sheikhCountry = model.sheikhCountry,
            studentsCount = model.studentsCount,
            isWomenCommittee = model.isWomenCommittee,
            stages = model.stages,
            scoringDetails = model.scoringDetails,
            aggregationDetails = model.aggregationDetails,
            ceremonyDetails = model.ceremonyDetails,
            receivedByAssistant = model.receivedByAssistant,
            examApprovedBy = model.examApprovedBy,
            createdDate = model.createdDate,
            createdAtTime = model.createdAtTime,
            updatedAt = model.updatedAt
        )
    }
}

@Entity(tableName = "app_users")
data class AppUserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val email: String = "",
    val passportNumber: String = "",
    val roleId: String = "assistant", // Reference to Role ID
    // The server and web app call this field "role".
    @Json(name = "role") val roleKey: String = "assistant",
    val status: String = "approved",
    val assignedCommittees: List<Int> = emptyList(),
    val permissions: List<String> = emptyList(),
    val createdAt: String = "",
    val approvedAt: String? = null,
    val approvedBy: String? = null,
    val notes: String = "",
    val updatedAt: String? = null
) {
    fun toDomain() = AppUser(
        id = id,
        name = name,
        phone = phone,
        email = email,
        passportNumber = passportNumber,
        role = UserRole.fromKey(roleKey),
        status = status,
        assignedCommittees = assignedCommittees,
        permissions = permissions,
        createdAt = createdAt,
        approvedAt = approvedAt,
        approvedBy = approvedBy,
        notes = notes,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(model: AppUser) = AppUserEntity(
            id = model.id,
            name = model.name,
            phone = model.phone,
            email = model.email,
            passportNumber = model.passportNumber,
            roleId = model.role.roleKey,
            roleKey = model.role.roleKey,
            status = model.status,
            assignedCommittees = model.assignedCommittees,
            permissions = model.permissions,
            createdAt = model.createdAt,
            approvedAt = model.approvedAt,
            approvedBy = model.approvedBy,
            notes = model.notes,
            updatedAt = model.updatedAt
        )
    }
}

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String,
    val title: String,
    val book: String,
    val branch: String = "",
    val totalMarks: Int = 100,
    val durationMinutes: Int = 60,
    val questionsCount: Int = 0,
    val pdfFileName: String = "",
    val pdfFileSize: String = "",
    val hasFile: Boolean = false,
    val uploadedAt: String = "",
    val sampleQuestions: List<String> = emptyList(),
    val notes: String = ""
) {
    fun toDomain() = Exam(
        id = id,
        title = title,
        book = book,
        branch = branch,
        totalMarks = totalMarks,
        durationMinutes = durationMinutes,
        questionsCount = questionsCount,
        pdfFileName = pdfFileName,
        pdfFileSize = pdfFileSize,
        hasFile = hasFile,
        uploadedAt = uploadedAt,
        sampleQuestions = sampleQuestions,
        notes = notes
    )

    companion object {
        fun fromDomain(model: Exam) = ExamEntity(
            id = model.id,
            title = model.title,
            book = model.book,
            branch = model.branch,
            totalMarks = model.totalMarks,
            durationMinutes = model.durationMinutes,
            questionsCount = model.questionsCount,
            pdfFileName = model.pdfFileName,
            pdfFileSize = model.pdfFileSize,
            hasFile = model.hasFile,
            uploadedAt = model.uploadedAt,
            sampleQuestions = model.sampleQuestions,
            notes = model.notes
        )
    }
}

@Entity(tableName = "schedule_tasks")
data class ScheduleTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: String,
    val time: String = "09:30 ص",
    val committeeId: String? = null,
    val committeeNumber: Int? = null,
    val assignedAssistantId: String? = null,
    val assignedAssistantName: String? = null,
    val type: String = "general",
    val priority: String = "normal",
    val completed: Boolean = false,
    val completedAt: String? = null,
    val deliverable: String = "",
    val notes: String = ""
) {
    fun toDomain() = ScheduleTask(
        id = id,
        title = title,
        date = date,
        time = time,
        committeeId = committeeId,
        committeeNumber = committeeNumber,
        assignedAssistantId = assignedAssistantId,
        assignedAssistantName = assignedAssistantName,
        type = type,
        priority = priority,
        completed = completed,
        completedAt = completedAt,
        deliverable = deliverable,
        notes = notes
    )

    companion object {
        fun fromDomain(model: ScheduleTask) = ScheduleTaskEntity(
            id = model.id,
            title = model.title,
            date = model.date,
            time = model.time,
            committeeId = model.committeeId,
            committeeNumber = model.committeeNumber,
            assignedAssistantId = model.assignedAssistantId,
            assignedAssistantName = model.assignedAssistantName,
            type = model.type,
            priority = model.priority,
            completed = model.completed,
            completedAt = model.completedAt,
            deliverable = model.deliverable,
            notes = model.notes
        )
    }
}

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val type: String = "info",
    val read: Boolean = false,
    val committeeId: String? = null,
    // The server sends the creation timestamp as "time".
    @Json(name = "time") val createdAt: String = "منذ قليل"
) {
    fun toDomain() = NotificationItem(
        id = id,
        title = title,
        description = description,
        type = type,
        read = read,
        committeeId = committeeId,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(model: NotificationItem) = NotificationEntity(
            id = model.id,
            title = model.title,
            description = model.description,
            type = model.type,
            read = model.read,
            committeeId = model.committeeId,
            createdAt = model.createdAt
        )
    }
}

@Entity(tableName = "custom_permissions")
data class CustomPermissionEntity(
    @PrimaryKey val id: String,
    @Json(name = "label") val name: String,
    val description: String = ""
) {
    fun toDomain() = com.example.data.model.CustomPermission(id = id, name = name, description = description)

    companion object {
        fun fromDomain(model: com.example.data.model.CustomPermission) =
            CustomPermissionEntity(id = model.id, name = model.name, description = model.description)
    }
}
