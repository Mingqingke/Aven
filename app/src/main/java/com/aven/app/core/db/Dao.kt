package com.aven.app.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY date DESC")
    fun observeAll(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun byId(id: Long): ExamEntity?

    @Insert
    suspend fun insert(exam: ExamEntity): Long

    @Update
    suspend fun update(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM exams")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM exams")
    suspend fun count(): Int
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subject_scores ORDER BY examId, orderIndex")
    fun observeAll(): Flow<List<SubjectScoreEntity>>

    @Query("SELECT * FROM subject_scores WHERE examId = :examId ORDER BY orderIndex")
    suspend fun forExam(examId: Long): List<SubjectScoreEntity>

    @Insert
    suspend fun insertAll(items: List<SubjectScoreEntity>): List<Long>

    @Insert
    suspend fun insert(item: SubjectScoreEntity): Long

    @Update
    suspend fun update(item: SubjectScoreEntity)

    @Query("DELETE FROM subject_scores WHERE examId = :examId")
    suspend fun deleteForExam(examId: Long)

    @Query("DELETE FROM subject_scores WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions")
    fun observeAll(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE subjectId = :subjectId ORDER BY id")
    suspend fun forSubject(subjectId: Long): List<QuestionEntity>

    @Insert
    suspend fun insertAll(items: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE subjectId = :subjectId")
    suspend fun deleteForSubject(subjectId: Long)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlanEntity>>

    @Insert
    suspend fun insert(plan: PlanEntity): Long

    @Update
    suspend fun update(plan: PlanEntity)

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY status ASC, dueDate ASC, createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Insert
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM tasks WHERE planId = :planId")
    suspend fun deleteForPlan(planId: Long)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ReportEntity>>

    @Insert
    suspend fun insert(report: ReportEntity): Long

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY id ASC")
    fun observeAll(): Flow<List<ChatEntity>>

    @Insert
    suspend fun insert(chat: ChatEntity): Long

    @Query("DELETE FROM chats")
    suspend fun clear()
}

@Dao
interface BackupDao {
    @Transaction
    @Query("SELECT * FROM exams")
    suspend fun allExams(): List<ExamEntity>

    @Transaction
    @Query("SELECT * FROM subject_scores")
    suspend fun allSubjects(): List<SubjectScoreEntity>

    @Transaction
    @Query("SELECT * FROM questions")
    suspend fun allQuestions(): List<QuestionEntity>

    @Transaction
    @Query("SELECT * FROM goals")
    suspend fun allGoals(): List<GoalEntity>

    @Transaction
    @Query("SELECT * FROM plans")
    suspend fun allPlans(): List<PlanEntity>

    @Transaction
    @Query("SELECT * FROM tasks")
    suspend fun allTasks(): List<TaskEntity>

    @Transaction
    @Query("SELECT * FROM reports")
    suspend fun allReports(): List<ReportEntity>
}
