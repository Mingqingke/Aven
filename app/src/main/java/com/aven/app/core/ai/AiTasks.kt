package com.aven.app.core.ai

import com.aven.app.core.calc.ScoreEngine.BestCombo
import com.aven.app.core.calc.CompareResult
import com.aven.app.core.calc.ExamSnap
import com.aven.app.core.calc.Progress
import com.aven.app.core.calc.ScoreEngine
import org.json.JSONArray
import org.json.JSONObject

/**
 * AI 提示词与结构化报告。客户端只负责提供事实数据，不做任何“替 AI 编结论”的事。
 */
data class AiReport(
    val headline: String = "",
    val summary: String = "",
    val facts: List<String> = emptyList(),
    val trends: List<String> = emptyList(),
    val strengths: List<String> = emptyList(),
    val issues: List<String> = emptyList(),
    val hypotheses: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val next: String = "",
    val raw: String = "",
    val degraded: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toJson(): String = JSONObject()
        .put("headline", headline)
        .put("summary", summary)
        .put("facts", JSONArray(facts))
        .put("trends", JSONArray(trends))
        .put("strengths", JSONArray(strengths))
        .put("issues", JSONArray(issues))
        .put("hypotheses", JSONArray(hypotheses))
        .put("actions", JSONArray(actions))
        .put("next", next)
        .put("raw", raw)
        .put("degraded", degraded)
        .put("createdAt", createdAt)
        .toString()

    companion object {
        fun fromJson(text: String): AiReport? = runCatching {
            val o = JSONObject(text)
            AiReport(
                headline = o.optString("headline"),
                summary = o.optString("summary"),
                facts = o.optJSONArray("facts").toStringList(),
                trends = o.optJSONArray("trends").toStringList(),
                strengths = o.optJSONArray("strengths").toStringList(),
                issues = o.optJSONArray("issues").toStringList(),
                hypotheses = o.optJSONArray("hypotheses").toStringList(),
                actions = o.optJSONArray("actions").toStringList(),
                next = o.optString("next"),
                raw = o.optString("raw"),
                degraded = o.optBoolean("degraded"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }.getOrNull()
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotBlank() } }
}

object AiTasks {

    const val SYSTEM = """你是 Aven 的学业分析助手，服务中国大陆的初中与高中学生。
你只能依据用户给出的数据作答，必须遵守：
1. 陈述“事实”时，必须能对应到具体科目、分数、得分率或名次；没有数据就不要说。
2. 严格区分：已知事实 / 推测（待验证） / 建议。推测不能写进事实或问题部分。
3. 只有科目总分时，可以指出成绩变化，但不能断言“粗心”“基础差”“某个知识点没掌握”。
4. 不得编造排名、平均分、题目难度、知识点得分，也不得自行换算或修改分数。
5. 建议要具体到学科与可执行动作，避免空泛鼓励。
6. 语气平实、克制，不夸张、不承诺提分。
7. 只输出一个 JSON 对象，不要输出 Markdown 代码块或多余解释。"""

    val REPORT_SCHEMA = """请严格按以下 JSON 结构输出：
{
  "headline": "一句话结论（20 字内）",
  "summary": "本次考试的整体总结（80 字内）",
  "facts": ["已知事实，必须带数字"],
  "trends": ["长期趋势"],
  "strengths": ["有数据支撑的优势"],
  "issues": ["有数据支撑、需要关注的问题"],
  "hypotheses": ["尚待验证的可能原因，明确标注需要更多数据"],
  "actions": ["具体可执行的行动"],
  "next": "下一阶段方向（40 字内）"
}"""

    /** 只包含必要数据，避免把全部私人记录一次性发出去。 */
    fun examContext(exam: ExamSnap, compare: CompareResult, combo: BestCombo?, limit: Int = 6): String {
        val sb = StringBuilder()
        sb.appendLine("【本次考试】")
        sb.appendLine("名称：${exam.title}；类型：${exam.type}；日期：${ScoreEngine.fmtScore(exam.date.toDouble())}")
        sb.appendLine("总分：${ScoreEngine.fmtScore(exam.rawTotal)} / ${ScoreEngine.fmtScore(exam.fullTotal)}，得分率 ${ScoreEngine.fmtRate(exam.rate)}")
        exam.subjects.forEach {
            sb.appendLine(
                "- ${it.subject}：${ScoreEngine.fmtScore(it.raw)}/${ScoreEngine.fmtScore(it.full)}" +
                    "（得分率 ${ScoreEngine.fmtRate(it.rate)}）" +
                    (it.converted?.let { c -> "，赋分 ${ScoreEngine.fmtScore(c)}" } ?: "") +
                    (it.rank?.let { r -> "，名次 $r" } ?: "") +
                    (it.total?.let { t -> "/$t" } ?: "")
            )
        }
        sb.appendLine("【与上一场比较】")
        if (compare.hasComparison) {
            val state = when (compare.state) {
                Progress.IMPROVED -> "进步"
                Progress.DECLINED -> "退步"
                Progress.FLAT -> "基本持平"
                Progress.INSUFFICIENT -> "不可比"
            }
            sb.appendLine("结论：$state；对比考试：${compare.prevTitle}；得分率变化 ${ScoreEngine.fmtDeltaPp(compare.deltaRatePp)}")
            sb.appendLine("口径：${compare.basis}；可信度：${compare.confidenceText}")
            sb.appendLine("共同科目：${compare.commonSubjects.joinToString("、")}")
            compare.reasons.forEach { sb.appendLine("- $it") }
        } else {
            sb.appendLine("没有可比的上一场考试，不要判断进步或退步。")
        }
        combo?.let {
            sb.appendLine("【个人最佳组合】跨考试拼接，当前 ${ScoreEngine.fmtScore(it.currentTotal)}，最佳合计 ${ScoreEngine.fmtScore(it.bestTotal)}，差距 ${ScoreEngine.fmtScore(it.gapTotal)} 分")
            it.rows.take(limit).forEach { r ->
                if (r.gapPoints > 0.05) {
                    sb.appendLine("- ${r.subject}：当前 ${ScoreEngine.fmtScore(r.currentRaw)}，最佳 ${ScoreEngine.fmtRate(r.bestRate)}（${r.bestTitle}），差 ${ScoreEngine.fmtScore(r.gapPoints)} 分")
                }
            }
        }
        return sb.toString()
    }

    fun examPrompt(exam: ExamSnap, compare: CompareResult, combo: BestCombo?, historyDigest: String): String =
        "${SYSTEM}\n\n${REPORT_SCHEMA}\n\n以下是用户授权的数据：\n\n" +
            examContext(exam, compare, combo) + "\n【历史摘要】\n" + historyDigest +
            "\n\n请生成这次考试的分析报告，所有结论必须来自上面的数据。"

    fun subjectPrompt(subject: String, digest: String, target: String?): String =
        "${SYSTEM}\n\n${REPORT_SCHEMA}\n\n【单科学业数据】\n" + digest +
            (if (target != null) "\n【目标】\n$target" else "") +
            "\n\n请围绕 $subject 做诊断，说明变化与波动，不要臆测原因。"

    fun planPrompt(goal: String, digest: String): String =
        "${SYSTEM}\n\n${REPORT_SCHEMA}\n\n【成绩摘要】\n$digest\n\n【目标】\n$goal" +
            "\n\n请给出阶段计划与可执行任务，任务要能在两周内开始做。"

    fun reviewPrompt(digest: String): String =
        "${SYSTEM}\n\n${REPORT_SCHEMA}\n\n【近期任务与成绩】\n$digest" +
            "\n\n请复盘执行情况：完成率与成绩变化只能陈述数据，不要归因于意志力。"

    fun chatSystem(profile: String, digest: String): String =
        """你是 Aven 学业助手 面向中国中学生
用中文像朋友一样自然说话 遵守
1 先回应用户 再给一两句有依据的判断 必要时给一个可执行建议
2 最多三句话 不分点 不列功能清单 不说你可以问我
3 不使用括号 引号 编号 星号 不输出 JSON 不输出英文字段名
4 没有数据支撑时直接说这需要更多成绩或题目数据
5 语气平实 简短 不夸张 不承诺提分

学生档案
$profile

已授权成绩
$digest

现在回答学生的问题"""

    fun visionPrompt(): String = """你在识别一张中国中学成绩单。
只输出 JSON 对象，不要解释：
{"title":"考试名称，无法确认填 null","date":"YYYY-MM-DD，无法确认填 null",
 "subjects":[{"name":"科目","score":分数或null,"full":满分或null,"rank":名次或null,"total":参考人数或null}]}
识别规则：
1. 看不清或不确定的数字必须填 null，禁止猜测。
2. 不要把总分、编号、考号当成分数。
3. 科目名保留成绩单原文。
4. 只输出 JSON。"""

    fun parseReport(text: String): AiReport {
        val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = cleaned.substringAfter('{', "").let { if (it.isBlank()) cleaned else "{" + it }
        val parsed = runCatching {
            val o = JSONObject(json)
            AiReport(
                headline = o.optString("headline"),
                summary = o.optString("summary"),
                facts = o.optJSONArray("facts").toStringList(),
                trends = o.optJSONArray("trends").toStringList(),
                strengths = o.optJSONArray("strengths").toStringList(),
                issues = o.optJSONArray("issues").toStringList(),
                hypotheses = o.optJSONArray("hypotheses").toStringList(),
                actions = o.optJSONArray("actions").toStringList(),
                next = o.optString("next"),
                raw = cleaned,
            )
        }.getOrNull()
        return parsed ?: AiReport(
            headline = "模型返回了非结构化内容",
            summary = cleaned.take(600),
            raw = cleaned,
            degraded = true,
        )
    }

    data class VisionSubject(
        val name: String,
        val score: Double?,
        val full: Double?,
        val rank: Int?,
        val total: Int?,
    )

    data class VisionSheet(val title: String?, val date: String?, val subjects: List<VisionSubject>) {
        val parsedCount: Int get() = subjects.count { it.score != null }
    }

    fun parseVision(text: String): VisionSheet? {
        val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = cleaned.substringAfter('{', "").let { if (it.isBlank()) cleaned else "{" + it }
        return runCatching {
            val o = JSONObject(json)
            val arr = o.optJSONArray("subjects")
            val list = mutableListOf<VisionSubject>()
            if (arr != null) for (i in 0 until arr.length()) {
                val s = arr.optJSONObject(i) ?: continue
                val name = s.optString("name").trim()
                if (name.isBlank()) continue
                list += VisionSubject(
                    name = name,
                    score = if (s.isNull("score")) null else s.optDouble("score"),
                    full = if (s.isNull("full")) null else s.optDouble("full"),
                    rank = if (s.isNull("rank")) null else s.optInt("rank"),
                    total = if (s.isNull("total")) null else s.optInt("total"),
                )
            }
            VisionSheet(
                title = if (o.isNull("title")) null else o.optString("title").takeIf { it.isNotBlank() && it != "null" },
                date = if (o.isNull("date")) null else o.optString("date").takeIf { it.isNotBlank() && it != "null" },
                subjects = list,
            )
        }.getOrNull()
    }
}
