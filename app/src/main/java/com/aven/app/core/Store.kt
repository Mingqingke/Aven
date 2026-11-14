package com.aven.app.core

import android.content.Context
import android.net.Uri
import com.aven.app.core.calc.ExamSnap
import com.aven.app.core.calc.QScore
import com.aven.app.core.calc.SubjScore
import com.aven.app.core.db.AppDatabase
import com.aven.app.core.db.ChatEntity
import com.aven.app.core.db.ExamEntity
import com.aven.app.core.db.GoalEntity
import com.aven.app.core.db.PlanEntity
import com.aven.app.core.db.QuestionEntity
import com.aven.app.core.db.ReportEntity
import com.aven.app.core.db.SubjectScoreEntity
import com.aven.app.core.db.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

data class SubjectDraft(
    val subject: String,
    val raw: Double,
    val full: Double,
    val converted: Double? = null,
    val rank: Int? = null,
    val total: Int? = null,
    val classAvg: Double? = null,
)

data class ExamDraft(
    val id: Long = 0,
    val title: String,
    val date: Long,
    val type: String,
    val stage: String,
    val term: String = "",
    val note: String = "",
    val images: List<String> = emptyList(),
    val source: String = "manual",
    val subjects: List<SubjectDraft> = emptyList(),
)

/**
 * 唯一的数据入口：界面只通过这里读写，保证成绩、计划、报告都用同一份本地数据库。
 */
object Store {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private var appContext: Context? = null

    val ready = MutableStateFlow(false)

    fun init(context: Context) {
        appContext = context.applicationContext
        db = AppDatabase.get(context)
        ready.value = true
    }

    val exams: StateFlow<List<ExamSnap>> by lazy {
        combine(
            db.exams().observeAll(),
            db.subjects().observeAll(),
            db.questions().observeAll(),
        ) { exams, subjects, questions ->
            val qBySubject = questions.groupBy { it.subjectId }
            exams.map { e ->
                ExamSnap(
                    id = e.id,
                    title = e.title,
                    date = e.date,
                    type = e.type,
                    stage = e.stage,
                    subjects = subjects.filter { it.examId == e.id }.sortedBy { it.orderIndex }.map { s ->
                        SubjScore(
                            subject = s.subject,
                            raw = s.raw,
                            full = s.full,
                            converted = s.converted,
                            rank = s.rankNo,
                            total = s.totalCount,
                            classAvg = s.classAvg,
                            questions = qBySubject[s.id].orEmpty().map { q ->
                                QScore(q.number, q.score, q.full, q.type, q.knowledge, q.wrong, q.reason)
                            },
                        )
                    },
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    }

    val goals: StateFlow<List<GoalEntity>> by lazy {
        db.goals().observeAll().stateIn(scope, SharingStarted.Eagerly, emptyList())
    }
    val plans: StateFlow<List<PlanEntity>> by lazy {
        db.plans().observeAll().stateIn(scope, SharingStarted.Eagerly, emptyList())
    }
    val tasks: StateFlow<List<TaskEntity>> by lazy {
        db.tasks().observeAll().stateIn(scope, SharingStarted.Eagerly, emptyList())
    }
    val reports: StateFlow<List<ReportEntity>> by lazy {
        db.reports().observeAll().stateIn(scope, SharingStarted.Eagerly, emptyList())
    }
    val chats: StateFlow<List<ChatEntity>> by lazy {
        db.chats().observeAll().stateIn(scope, SharingStarted.Eagerly, emptyList())
    }

    fun db(): AppDatabase = db

    // ---------- 考试 ----------

    suspend fun saveExam(draft: ExamDraft): Long = withContext(Dispatchers.IO) {
        val examId: Long
        if (draft.id == 0L) {
            examId = db.exams().insert(
                ExamEntity(
                    title = draft.title,
                    date = draft.date,
                    type = draft.type,
                    stage = draft.stage,
                    term = draft.term,
                    note = draft.note,
                    images = JSONArray(draft.images).toString(),
                    source = draft.source,
                )
            )
        } else {
            examId = draft.id
            val old = db.exams().byId(draft.id)
            // 编辑时先把已有题目按科目暂存，重建科目行后回填，避免用户录入的题目被级联删除
            val keptQuestions = mutableMapOf<String, List<QuestionEntity>>()
            db.subjects().forExam(draft.id).forEach { s ->
                val qs = db.questions().forSubject(s.id)
                if (qs.isNotEmpty()) keptQuestions[s.subject] = qs
            }
            db.exams().update(
                ExamEntity(
                    id = draft.id,
                    title = draft.title,
                    date = draft.date,
                    type = draft.type,
                    stage = draft.stage,
                    term = draft.term,
                    note = draft.note,
                    images = JSONArray(draft.images).toString(),
                    source = old?.source ?: draft.source,
                    createdAt = old?.createdAt ?: System.currentTimeMillis(),
                )
            )
            db.subjects().deleteForExam(draft.id)
            draft.subjects.forEachIndexed { index, s ->
                val sid = db.subjects().insert(
                    SubjectScoreEntity(
                        examId = examId, subject = s.subject, raw = s.raw, full = s.full,
                        converted = s.converted, rankNo = s.rank, totalCount = s.total,
                        classAvg = s.classAvg, orderIndex = index,
                    )
                )
                keptQuestions[s.subject]?.let { qs ->
                    db.questions().insertAll(qs.map { it.copy(id = 0, subjectId = sid) })
                }
            }
            return@withContext examId
        }
        draft.subjects.forEachIndexed { index, s ->
            db.subjects().insert(
                SubjectScoreEntity(
                    examId = examId,
                    subject = s.subject,
                    raw = s.raw,
                    full = s.full,
                    converted = s.converted,
                    rankNo = s.rank,
                    totalCount = s.total,
                    classAvg = s.classAvg,
                    orderIndex = index,
                )
            )
        }
        examId
    }

    suspend fun deleteExam(id: Long) = withContext(Dispatchers.IO) {
        val exam = db.exams().byId(id)
        exam?.let { e ->
            runCatching { JSONArray(e.images) }.getOrNull()?.let { arr ->
                for (i in 0 until arr.length()) {
                    runCatching { File(arr.getString(i)).takeIf { f -> f.exists() }?.delete() }
                }
            }
        }
        db.exams().delete(id)
    }

    suspend fun duplicateExam(id: Long): Long = withContext(Dispatchers.IO) {
        val e = db.exams().byId(id) ?: return@withContext 0L
        val subjects = db.subjects().forExam(id)
        val newId = db.exams().insert(e.copy(id = 0, title = e.title + " 副本", createdAt = System.currentTimeMillis()))
        db.subjects().insertAll(subjects.map { it.copy(id = 0, examId = newId) })
        newId
    }

    suspend fun examImages(id: Long): List<String> = withContext(Dispatchers.IO) {
        val e = db.exams().byId(id) ?: return@withContext emptyList()
        runCatching {
            val arr = JSONArray(e.images)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    suspend fun updateExamImages(id: Long, images: List<String>) = withContext(Dispatchers.IO) {
        val e = db.exams().byId(id) ?: return@withContext
        db.exams().update(e.copy(images = JSONArray(images).toString()))
    }

    // ---------- 题目 ----------

    suspend fun questionsFor(examId: Long, subject: String): List<QScore> = withContext(Dispatchers.IO) {
        val s = db.subjects().forExam(examId).firstOrNull { it.subject == subject }
            ?: return@withContext emptyList()
        db.questions().forSubject(s.id).map {
            QScore(it.number, it.score, it.full, it.type, it.knowledge, it.wrong, it.reason)
        }
    }

    suspend fun saveQuestions(examId: Long, subject: String, items: List<QScore>) = withContext(Dispatchers.IO) {
        val s = db.subjects().forExam(examId).firstOrNull { it.subject == subject } ?: return@withContext
        db.questions().deleteForSubject(s.id)
        db.questions().insertAll(
            items.map {
                QuestionEntity(
                    subjectId = s.id, number = it.number, score = it.score, full = it.full,
                    type = it.type, knowledge = it.knowledge, wrong = it.wrong, reason = it.reason,
                )
            }
        )
    }

    // ---------- 目标 ----------

    suspend fun addGoal(goal: GoalEntity): Long = withContext(Dispatchers.IO) { db.goals().insert(goal) }
    suspend fun updateGoal(goal: GoalEntity) = withContext(Dispatchers.IO) { db.goals().update(goal) }
    suspend fun deleteGoal(id: Long) = withContext(Dispatchers.IO) { db.goals().delete(id) }

    // ---------- 计划 ----------

    suspend fun addPlan(plan: PlanEntity): Long = withContext(Dispatchers.IO) { db.plans().insert(plan) }
    suspend fun updatePlan(plan: PlanEntity) = withContext(Dispatchers.IO) { db.plans().update(plan) }
    suspend fun deletePlan(id: Long) = withContext(Dispatchers.IO) {
        db.tasks().deleteForPlan(id)
        db.plans().delete(id)
    }

    // ---------- 任务 ----------

    suspend fun addTask(task: TaskEntity): Long = withContext(Dispatchers.IO) { db.tasks().insert(task) }
    suspend fun addTasks(tasks: List<TaskEntity>) = withContext(Dispatchers.IO) { db.tasks().insertAll(tasks) }
    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) { db.tasks().update(task) }
    suspend fun deleteTask(id: Long) = withContext(Dispatchers.IO) { db.tasks().delete(id) }

    // ---------- 报告 ----------

    suspend fun addReport(report: ReportEntity): Long = withContext(Dispatchers.IO) { db.reports().insert(report) }
    suspend fun deleteReport(id: Long) = withContext(Dispatchers.IO) { db.reports().delete(id) }

    // ---------- 对话 ----------

    suspend fun addChat(role: String, content: String) = withContext(Dispatchers.IO) {
        db.chats().insert(ChatEntity(role = role, content = content))
    }
    suspend fun clearChat() = withContext(Dispatchers.IO) { db.chats().clear() }

    // ---------- 图片 ----------

    suspend fun importImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext null
        runCatching {
            val dir = File(ctx.filesDir, "exam_images").apply { mkdirs() }
            val file = File(dir, "img_${System.currentTimeMillis()}_${(0..999).random()}.jpg")
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            if (file.length() > 0) file.absolutePath else null
        }.getOrNull()
    }

    suspend fun imageBytes(path: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { File(path).readBytes() }.getOrNull()
    }

    suspend fun totalImageBytes(): Long = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext 0L
        File(ctx.filesDir, "exam_images").walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    suspend fun clearImages() = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext
        File(ctx.filesDir, "exam_images").walkBottomUp().forEach { runCatching { it.delete() } }
    }

    // ---------- 清空 ----------

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        db.backup().allExams().forEach { db.exams().delete(it.id) }
        db.backup().allGoals().forEach { db.goals().delete(it.id) }
        db.backup().allPlans().forEach { db.plans().delete(it.id) }
        db.backup().allTasks().forEach { db.tasks().delete(it.id) }
        db.backup().allReports().forEach { db.reports().delete(it.id) }
        db.chats().clear()
        clearImages()
    }
}
