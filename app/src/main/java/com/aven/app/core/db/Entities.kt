package com.aven.app.core.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: Long,
    val type: String,
    val stage: String,
    val term: String = "",
    val note: String = "",
    val images: String = "",       // JSON 数组，保存导入的原始图片路径
    val source: String = "manual", // manual / ocr / vision / import
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "subject_scores",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("examId")],
)
data class SubjectScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val subject: String,
    val raw: Double,
    val full: Double,
    val converted: Double? = null,
    val rankNo: Int? = null,
    val totalCount: Int? = null,
    val classAvg: Double? = null,
    val orderIndex: Int = 0,
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectScoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("subjectId")],
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val number: String,
    val score: Double,
    val full: Double,
    val type: String = "",
    val knowledge: String = "",
    val wrong: Boolean = false,
    val reason: String = "",
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String = "",      // 空字符串表示总分目标
    val targetScore: Double,
    val targetFull: Double,
    val targetRate: Double,
    val examDate: Long = 0L,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val achieved: Boolean = false,
)

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String = "",
    val stageName: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val goalId: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long? = null,
    val subject: String = "",
    val title: String,
    val estimateMin: Int = 0,
    val priority: Int = 1,          // 0 低 / 1 中 / 2 高
    val dueDate: Long = 0L,
    val status: Int = 0,            // 0 待完成 / 1 完成 / 2 跳过 / 3 延期
    val doneAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long? = null,
    val kind: String,               // exam / subject / goal / plan
    val title: String,
    val content: String,            // 结构化 JSON 或纯文本
    val model: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,               // user / assistant
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)
