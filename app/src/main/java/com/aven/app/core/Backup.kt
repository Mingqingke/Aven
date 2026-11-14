package com.aven.app.core

import android.content.Context
import android.net.Uri
import com.aven.app.core.db.ChatEntity
import com.aven.app.core.db.ExamEntity
import com.aven.app.core.db.GoalEntity
import com.aven.app.core.db.PlanEntity
import com.aven.app.core.db.QuestionEntity
import com.aven.app.core.db.ReportEntity
import com.aven.app.core.db.SubjectScoreEntity
import com.aven.app.core.db.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 备份与导出：JSON 可完整恢复，CSV 只导出成绩表。
 * 导入采用“先完整解析、再写入”，解析失败不会破坏现有数据。
 */
object Backup {

    data class Stats(val exams: Int, val subjects: Int, val tasks: Int)

    suspend fun exportJson(context: Context, uri: Uri): Result<Stats> = withContext(Dispatchers.IO) {
        runCatching {
            val db = Store.db()
            val exams = db.backup().allExams()
            val subjects = db.backup().allSubjects()
            val questions = db.backup().allQuestions()
            val goals = db.backup().allGoals()
            val plans = db.backup().allPlans()
            val tasks = db.backup().allTasks()
            val reports = db.backup().allReports()

            val root = JSONObject()
                .put("app", "aven")
                .put("schema", 1)
                .put("exportedAt", System.currentTimeMillis())
            val examArr = JSONArray()
            exams.forEach { e ->
                val o = JSONObject()
                    .put("title", e.title).put("date", e.date).put("type", e.type)
                    .put("stage", e.stage).put("term", e.term).put("note", e.note)
                    .put("source", e.source).put("createdAt", e.createdAt)
                val subArr = JSONArray()
                subjects.filter { it.examId == e.id }.forEach { s ->
                    val so = JSONObject()
                        .put("subject", s.subject).put("raw", s.raw).put("full", s.full)
                        .put("converted", s.converted ?: JSONObject.NULL)
                        .put("rank", s.rankNo ?: JSONObject.NULL)
                        .put("total", s.totalCount ?: JSONObject.NULL)
                        .put("classAvg", s.classAvg ?: JSONObject.NULL)
                        .put("order", s.orderIndex)
                    val qArr = JSONArray()
                    questions.filter { it.subjectId == s.id }.forEach { q ->
                        qArr.put(
                            JSONObject().put("number", q.number).put("score", q.score).put("full", q.full)
                                .put("type", q.type).put("knowledge", q.knowledge)
                                .put("wrong", q.wrong).put("reason", q.reason)
                        )
                    }
                    so.put("questions", qArr)
                    subArr.put(so)
                }
                o.put("subjects", subArr)
                examArr.put(o)
            }
            root.put("exams", examArr)

            val goalArr = JSONArray()
            goals.forEach { g ->
                goalArr.put(
                    JSONObject().put("subject", g.subject).put("targetScore", g.targetScore)
                        .put("targetFull", g.targetFull).put("targetRate", g.targetRate)
                        .put("examDate", g.examDate).put("note", g.note).put("achieved", g.achieved)
                )
            }
            root.put("goals", goalArr)

            val planArr = JSONArray()
            plans.forEach { p ->
                planArr.put(
                    JSONObject().put("title", p.title).put("subject", p.subject)
                        .put("stage", p.stageName).put("start", p.startDate).put("end", p.endDate)
                        .put("note", p.note)
                )
            }
            root.put("plans", planArr)

            val taskArr = JSONArray()
            tasks.forEach { t ->
                taskArr.put(
                    JSONObject().put("subject", t.subject).put("title", t.title)
                        .put("estimateMin", t.estimateMin).put("priority", t.priority)
                        .put("due", t.dueDate).put("status", t.status).put("doneAt", t.doneAt)
                )
            }
            root.put("tasks", taskArr)

            val reportArr = JSONArray()
            reports.forEach { r ->
                reportArr.put(
                    JSONObject().put("kind", r.kind).put("title", r.title)
                        .put("content", r.content).put("model", r.model).put("createdAt", r.createdAt)
                )
            }
            root.put("reports", reportArr)

            context.contentResolver.openOutputStream(uri)?.use {
                it.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: error("无法写入所选文件")
            Stats(exams.size, subjects.size, tasks.size)
        }
    }

    suspend fun importJson(context: Context, uri: Uri): Result<Stats> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("无法读取所选文件")
            val root = JSONObject(text)
            if (root.optString("app") != "aven") error("这不是 Aven 的备份文件")

            // 先全部解析成内存对象，任何一条不合法就整体失败
            val examArr = root.optJSONArray("exams") ?: JSONArray()
            val parsed = mutableListOf<Pair<ExamEntity, List<Pair<SubjectScoreEntity, List<QuestionEntity>>>>>()
            for (i in 0 until examArr.length()) {
                val o = examArr.getJSONObject(i)
                val exam = ExamEntity(
                    title = o.optString("title", "导入的考试"),
                    date = o.optLong("date", System.currentTimeMillis()),
                    type = o.optString("type", "其他"),
                    stage = o.optString("stage", "高中"),
                    term = o.optString("term", ""),
                    note = o.optString("note", ""),
                    images = "",
                    source = "import",
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                )
                val subArr = o.optJSONArray("subjects") ?: JSONArray()
                val subs = mutableListOf<Pair<SubjectScoreEntity, List<QuestionEntity>>>()
                for (j in 0 until subArr.length()) {
                    val so = subArr.getJSONObject(j)
                    val subject = SubjectScoreEntity(
                        examId = 0,
                        subject = so.optString("subject", "未知科目"),
                        raw = so.optDouble("raw", 0.0),
                        full = so.optDouble("full", 100.0),
                        converted = if (so.isNull("converted")) null else so.optDouble("converted"),
                        rankNo = if (so.isNull("rank")) null else so.optInt("rank"),
                        totalCount = if (so.isNull("total")) null else so.optInt("total"),
                        classAvg = if (so.isNull("classAvg")) null else so.optDouble("classAvg"),
                        orderIndex = so.optInt("order", 0),
                    )
                    val qArr = so.optJSONArray("questions") ?: JSONArray()
                    val qs = mutableListOf<QuestionEntity>()
                    for (k in 0 until qArr.length()) {
                        val q = qArr.getJSONObject(k)
                        qs += QuestionEntity(
                            subjectId = 0,
                            number = q.optString("number", ""),
                            score = q.optDouble("score", 0.0),
                            full = q.optDouble("full", 1.0),
                            type = q.optString("type", ""),
                            knowledge = q.optString("knowledge", ""),
                            wrong = q.optBoolean("wrong", false),
                            reason = q.optString("reason", ""),
                        )
                    }
                    subs += subject to qs
                }
                parsed += exam to subs
            }

            val db = Store.db()
            var subjectCount = 0
            var taskCount = 0
            for ((exam, subs) in parsed) {
                val examId = db.exams().insert(exam)
                for ((subject, questions) in subs) {
                    val sid = db.subjects().insert(subject.copy(examId = examId))
                    subjectCount += 1
                    if (questions.isNotEmpty()) db.questions().insertAll(questions.map { it.copy(subjectId = sid) })
                }
            }

            root.optJSONArray("goals")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.goals().insert(
                        GoalEntity(
                            subject = o.optString("subject", ""),
                            targetScore = o.optDouble("targetScore", 0.0),
                            targetFull = o.optDouble("targetFull", 100.0),
                            targetRate = o.optDouble("targetRate", 0.0),
                            examDate = o.optLong("examDate", 0L),
                            note = o.optString("note", ""),
                            achieved = o.optBoolean("achieved", false),
                        )
                    )
                }
            }

            root.optJSONArray("plans")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.plans().insert(
                        PlanEntity(
                            title = o.optString("title", "导入的计划"),
                            subject = o.optString("subject", ""),
                            stageName = o.optString("stage", ""),
                            startDate = o.optLong("start", 0L),
                            endDate = o.optLong("end", 0L),
                            note = o.optString("note", ""),
                        )
                    )
                }
            }

            root.optJSONArray("tasks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.tasks().insert(
                        TaskEntity(
                            subject = o.optString("subject", ""),
                            title = o.optString("title", "导入的任务"),
                            estimateMin = o.optInt("estimateMin", 0),
                            priority = o.optInt("priority", 1),
                            dueDate = o.optLong("due", 0L),
                            status = o.optInt("status", 0),
                            doneAt = o.optLong("doneAt", 0L),
                        )
                    )
                    taskCount += 1
                }
            }

            root.optJSONArray("reports")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.reports().insert(
                        ReportEntity(
                            kind = o.optString("kind", "exam"),
                            title = o.optString("title", "导入的报告"),
                            content = o.optString("content", ""),
                            model = o.optString("model", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        )
                    )
                }
            }

            Stats(parsed.size, subjectCount, taskCount)
        }
    }

    suspend fun exportCsv(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val db = Store.db()
            val exams = db.backup().allExams().sortedByDescending { it.date }
            val subjects = db.backup().allSubjects()
            val sb = StringBuilder("\uFEFF")
            sb.appendLine("考试,日期,类型,学段,科目,原始分,满分,得分率,赋分,排名,参考人数")
            var rows = 0
            exams.forEach { e ->
                subjects.filter { it.examId == e.id }.forEach { s ->
                    val rate = if (s.full > 0) String.format("%.1f%%", s.raw / s.full * 100) else ""
                    sb.append(
                        listOf(
                            quote(e.title), Dates.fmt(e.date), quote(e.type), quote(e.stage),
                            quote(s.subject), s.raw, s.full, rate,
                            s.converted ?: "", s.rankNo ?: "", s.totalCount ?: "",
                        ).joinToString(",")
                    ).appendLine()
                    rows += 1
                }
            }
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(sb.toString().toByteArray(Charsets.UTF_8))
            } ?: error("无法写入所选文件")
            rows
        }
    }

    private fun quote(v: String): String = "\"" + v.replace("\"", "\"\"") + "\""
}
