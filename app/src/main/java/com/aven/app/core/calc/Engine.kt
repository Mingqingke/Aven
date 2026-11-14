package com.aven.app.core.calc

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 成绩计算引擎：纯 Kotlin，不依赖 Android，也不调用任何大模型。
 * 界面上所有“进步 / 退步 / 最佳 / 差距”都从这里得出。
 */
data class QScore(
    val number: String,
    val score: Double,
    val full: Double,
    val type: String = "",
    val knowledge: String = "",
    val wrong: Boolean = false,
    val reason: String = "",
) {
    val rate: Double get() = if (full > 0) (score / full).coerceIn(0.0, 1.0) else 0.0
}

data class SubjScore(
    val subject: String,
    val raw: Double,
    val full: Double,
    val converted: Double? = null,
    val rank: Int? = null,
    val total: Int? = null,
    val classAvg: Double? = null,
    val questions: List<QScore> = emptyList(),
) {
    val rate: Double get() = if (full > 0) (raw / full).coerceIn(0.0, 1.5) else 0.0
    val effective: Double get() = converted ?: raw
    val hasConverted: Boolean get() = converted != null
}

data class ExamSnap(
    val id: Long,
    val title: String,
    val date: Long,
    val type: String,
    val stage: String,
    val subjects: List<SubjScore>,
) {
    val rawTotal: Double get() = subjects.sumOf { it.raw }
    val fullTotal: Double get() = subjects.sumOf { it.full }
    val convertedTotal: Double get() = subjects.sumOf { it.effective }
    val rate: Double get() = if (fullTotal > 0) rawTotal / fullTotal else 0.0
    fun subject(name: String): SubjScore? = subjects.firstOrNull { it.subject == name }
    val hasConverted: Boolean get() = subjects.any { it.hasConverted }
}

enum class Progress { IMPROVED, DECLINED, FLAT, INSUFFICIENT }

enum class Confidence { HIGH, MEDIUM, LOW }

data class CompareResult(
    val state: Progress,
    val lastId: Long? = null,
    val prevId: Long? = null,
    val prevTitle: String = "",
    val prevDate: Long = 0L,
    val deltaRatePp: Double = 0.0,
    val deltaRaw: Double? = null,
    val basis: String = "",
    val confidence: Confidence = Confidence.LOW,
    val confidenceText: String = "仅供参考",
    val reasons: List<String> = emptyList(),
    val commonSubjects: List<String> = emptyList(),
    val daysApart: Int = 0,
) {
    val hasComparison: Boolean get() = state != Progress.INSUFFICIENT
}

object ScoreEngine {

    const val NOTE_RATE_LIMIT = "得分率能减小满分差异，但无法消除试卷难度差异。"

    private fun pct(v: Double) = v * 100.0

    /** 按时间倒序（新 → 旧）。 */
    fun sorted(exams: List<ExamSnap>): List<ExamSnap> = exams.sortedByDescending { it.date }

    /**
     * 主比较：优先同类考试、共同科目、同计分体系；条件不足时如实返回“数据不足”。
     */
    fun compare(exams: List<ExamSnap>, bandPp: Double = 2.0, rule: Int = 0): CompareResult {
        val list = sorted(exams)
        if (list.size < 2) return CompareResult(Progress.INSUFFICIENT, lastId = list.firstOrNull()?.id)
        val last = list.first()
        val others = list.drop(1)

        fun overlap(e: ExamSnap): List<String> {
            val mine = last.subjects.map { it.subject }.toSet()
            return e.subjects.map { it.subject }.filter { it in mine }
        }

        val prev = when (rule) {
            1 -> others.first()
            2 -> others.maxByOrNull { overlap(it).size * 1000 + (it.date / 86_400_000L) }
            else -> others.firstOrNull { it.type == last.type && overlap(it).isNotEmpty() }
                ?: others.firstOrNull { it.stage == last.stage && overlap(it).isNotEmpty() }
                ?: others.firstOrNull { overlap(it).isNotEmpty() }
        } ?: return CompareResult(
            Progress.INSUFFICIENT,
            lastId = last.id,
            prevId = others.firstOrNull()?.id,
            prevTitle = others.firstOrNull()?.title ?: "",
            prevDate = others.firstOrNull()?.date ?: 0L,
            reasons = listOf("与上一场考试没有共同科目 无法判断"),
        )

        val common = overlap(prev)
        if (common.isEmpty()) {
            return CompareResult(
                Progress.INSUFFICIENT, lastId = last.id, prevId = prev.id, prevTitle = prev.title,
                prevDate = prev.date,
                reasons = listOf("两场考试没有共同科目 无法判断")
            )
        }

        var weightSum = 0.0
        var weighted = 0.0
        var sameFull = true
        var rawDelta = 0.0
        var rankCross = false
        for (name in common) {
            val a = last.subject(name) ?: continue
            val b = prev.subject(name) ?: continue
            val w = if (a.full > 0) a.full else 1.0
            weightSum += w
            weighted += w * (pct(a.rate) - pct(b.rate))
            if (abs(a.full - b.full) > 0.01) sameFull = false
            rawDelta += a.raw - b.raw
            if (a.rank != null && b.rank != null) rankCross = true
        }
        val deltaPp = if (weightSum > 0) weighted / weightSum else 0.0

        val state = when {
            deltaPp > bandPp -> Progress.IMPROVED
            deltaPp < -bandPp -> Progress.DECLINED
            else -> Progress.FLAT
        }

        val days = ((last.date - prev.date).coerceAtLeast(0L) / 86_400_000L).toInt()

        var level = 2
        val reasons = mutableListOf<String>()
        if (common.size >= 3) {
            reasons += "共同科目 ${common.size} 门 按满分加权比较得分率"
        } else {
            reasons += "仅有 ${common.size} 门共同科目"
            level -= 1
        }
        if (sameFull) {
            reasons += "共同科目满分一致 原始分同步对照"
        } else {
            reasons += "满分不一致 已改用得分率"
            level -= 1
        }
        if (prev.type != last.type) {
            reasons += "考试类型不同 难度可能不同"
            level -= 1
        }
        if (prev.stage != last.stage) {
            reasons += "学段发生变化"
            level -= 1
        }
        if (common.size < last.subjects.size * 0.6) {
            reasons += "本次 ${last.subjects.size} 门科目中只有 ${common.size} 门可比"
            level -= 1
        }
        if (days > 200) {
            reasons += "相隔 $days 天 跨度较大"
            level -= 1
        }
        if (rankCross) {
            reasons += "两场考试均有排名 可交叉参考"
            level += 1
        } else {
            reasons += "缺少排名或平均分"
        }

        val conf = when {
            level >= 2 -> Confidence.HIGH
            level == 1 -> Confidence.MEDIUM
            else -> Confidence.LOW
        }
        val confText = when (conf) {
            Confidence.HIGH -> "比较可信"
            Confidence.MEDIUM -> "参考性比较"
            Confidence.LOW -> "仅供参照"
        }

        return CompareResult(
            state = state,
            lastId = last.id,
            prevId = prev.id,
            prevTitle = prev.title,
            prevDate = prev.date,
            deltaRatePp = deltaPp,
            deltaRaw = if (sameFull) rawDelta else null,
            basis = if (sameFull) "同科目同满分 按原始分对照" else "满分不同 按得分率折算",
            confidence = conf,
            confidenceText = confText,
            reasons = reasons,
            commonSubjects = common,
            daysApart = days,
        )
    }

    /** 指定考试与它之前最近一场可比考试的比较（详情页用）。 */
    fun compareWith(exams: List<ExamSnap>, examId: Long, bandPp: Double = 2.0): CompareResult {
        val list = sorted(exams)
        val idx = list.indexOfFirst { it.id == examId }
        if (idx < 0) return CompareResult(Progress.INSUFFICIENT)
        val target = list[idx]
        val before = list.drop(idx + 1)
        val mine = target.subjects.map { it.subject }.toSet()
        val prev = before.firstOrNull { it.subjects.any { s -> s.subject in mine } }
            ?: return CompareResult(Progress.INSUFFICIENT, lastId = target.id)
        val trimmed = listOf(target, prev)
        return compare(trimmed, bandPp, 1).copy(lastId = target.id, prevId = prev.id)
    }

    data class SubjectBest(
        val subject: String,
        val bestRaw: Double,
        val bestRawFull: Double,
        val bestRawExamId: Long,
        val bestRawTitle: String,
        val bestRawDate: Long,
        val bestRate: Double,
        val bestRateExamId: Long,
        val bestRateTitle: String,
        val bestRateDate: Long,
        val count: Int,
        val avgRate: Double,
        val latestRate: Double,
        val volatilityPp: Double,
    )

    fun subjectBests(exams: List<ExamSnap>): List<SubjectBest> {
        val map = linkedMapOf<String, MutableList<Pair<ExamSnap, SubjScore>>>()
        for (e in sorted(exams)) {
            for (s in e.subjects) {
                map.getOrPut(s.subject) { mutableListOf() }.add(e to s)
            }
        }
        return map.map { (subject, items) ->
            val bestRaw = items.maxByOrNull { it.second.raw }!!
            val bestRate = items.maxByOrNull { it.second.rate }!!
            val rates = items.map { it.second.rate }
            val avg = rates.average()
            val sd = if (rates.size > 1) sqrt(rates.map { (it - avg) * (it - avg) }.average()) else 0.0
            SubjectBest(
                subject = subject,
                bestRaw = bestRaw.second.raw,
                bestRawFull = bestRaw.second.full,
                bestRawExamId = bestRaw.first.id,
                bestRawTitle = bestRaw.first.title,
                bestRawDate = bestRaw.first.date,
                bestRate = bestRate.second.rate,
                bestRateExamId = bestRate.first.id,
                bestRateTitle = bestRate.first.title,
                bestRateDate = bestRate.first.date,
                count = items.size,
                avgRate = avg,
                latestRate = items.first().second.rate,
                volatilityPp = sd * 100.0,
            )
        }
    }

    data class ComboRow(
        val subject: String,
        val bestRate: Double,
        val bestTitle: String,
        val bestDate: Long,
        val currentRaw: Double,
        val currentFull: Double,
        val currentRate: Double,
        val gapPoints: Double,
        val gapRatePp: Double,
    )

    data class BestCombo(
        val rows: List<ComboRow>,
        val currentTotal: Double,
        val bestTotal: Double,
        val gapTotal: Double,
        val gapRatePp: Double,
        val currentFull: Double,
    )

    /** 个人最佳组合：各科历史最高得分率折算到本次满分，跨考试拼接，不是一次真实成绩。 */
    fun bestCombination(current: ExamSnap, all: List<ExamSnap>): BestCombo {
        val bests = subjectBests(all).associateBy { it.subject }
        val rows = current.subjects.map { s ->
            val b = bests[s.subject]
            val bestRate = maxOf(b?.bestRate ?: s.rate, s.rate)
            val bestPoints = bestRate * s.full
            ComboRow(
                subject = s.subject,
                bestRate = bestRate,
                bestTitle = if ((b?.bestRate ?: 0.0) >= s.rate) b?.bestRateTitle ?: current.title else current.title,
                bestDate = if ((b?.bestRate ?: 0.0) >= s.rate) b?.bestRateDate ?: current.date else current.date,
                currentRaw = s.raw,
                currentFull = s.full,
                currentRate = s.rate,
                gapPoints = (bestPoints - s.raw).coerceAtLeast(0.0),
                gapRatePp = (bestRate - s.rate) * 100.0,
            )
        }.sortedByDescending { it.gapPoints }
        val currentTotal = current.rawTotal
        val bestTotal = rows.sumOf { it.currentRaw + it.gapPoints }
        val full = current.fullTotal
        return BestCombo(
            rows = rows,
            currentTotal = currentTotal,
            bestTotal = bestTotal,
            gapTotal = (bestTotal - currentTotal).coerceAtLeast(0.0),
            gapRatePp = if (full > 0) (bestTotal - currentTotal) / full * 100.0 else 0.0,
            currentFull = full,
        )
    }

    data class TrendPoint(
        val examId: Long,
        val title: String,
        val date: Long,
        val raw: Double,
        val full: Double,
        val rate: Double,
        val rank: Int? = null,
        val total: Int? = null,
    )

    fun subjectSeries(exams: List<ExamSnap>, subject: String): List<TrendPoint> =
        sorted(exams).mapNotNull { e ->
            e.subject(subject)?.let {
                TrendPoint(e.id, e.title, e.date, it.raw, it.full, it.rate, it.rank, it.total)
            }
        }.sortedBy { it.date }

    fun overallSeries(exams: List<ExamSnap>): List<TrendPoint> =
        sorted(exams).map {
            TrendPoint(it.id, it.title, it.date, it.rawTotal, it.fullTotal, it.rate)
        }.sortedBy { it.date }

    enum class RangePreset(val label: String) {
        LAST3("最近三次"), LAST5("最近五次"), TERM("本学期"), YEAR("本学年"), ALL("全部")
    }

    fun filterSeries(points: List<TrendPoint>, preset: RangePreset): List<TrendPoint> {
        if (points.isEmpty()) return points
        val lastDate = points.last().date
        return when (preset) {
            RangePreset.LAST3 -> points.takeLast(3)
            RangePreset.LAST5 -> points.takeLast(5)
            RangePreset.TERM -> points.filter { lastDate - it.date <= 190L * 86_400_000L }
            RangePreset.YEAR -> points.filter { lastDate - it.date <= 370L * 86_400_000L }
            RangePreset.ALL -> points
        }
    }

    data class SubjectStat(
        val subject: String,
        val bestRate: Double,
        val avgRate: Double,
        val latestRate: Double,
        val deltaPp: Double,
        val volatilityPp: Double,
        val count: Int,
        val latestRaw: Double,
        val latestFull: Double,
        val trendUp: Boolean,
    )

    fun subjectStats(exams: List<ExamSnap>): List<SubjectStat> {
        val bests = subjectBests(exams).associateBy { it.subject }
        val list = sorted(exams)
        return bests.map { (subject, b) ->
            val series = subjectSeries(list, subject)
            val prev = series.dropLast(1).lastOrNull()
            val latest = series.lastOrNull()
            val delta = if (prev != null && latest != null) (latest.rate - prev.rate) * 100.0 else 0.0
            SubjectStat(
                subject = subject,
                bestRate = b.bestRate,
                avgRate = b.avgRate,
                latestRate = b.latestRate,
                deltaPp = delta,
                volatilityPp = b.volatilityPp,
                count = b.count,
                latestRaw = latest?.raw ?: 0.0,
                latestFull = latest?.full ?: 0.0,
                trendUp = delta > 0.5,
            )
        }.sortedBy { it.latestRate }
    }

    data class KnowledgeStat(
        val knowledge: String,
        val earned: Double,
        val full: Double,
        val count: Int,
        val wrongCount: Int,
        val subjects: List<String>,
    ) {
        val rate: Double get() = if (full > 0) earned / full else 0.0
    }

    fun knowledgeStats(exams: List<ExamSnap>): List<KnowledgeStat> {
        val map = linkedMapOf<String, MutableList<Pair<String, QScore>>>()
        for (e in exams) for (s in e.subjects) for (q in s.questions) {
            val key = q.knowledge.ifBlank { "未标注知识点" }
            map.getOrPut(key) { mutableListOf() }.add(s.subject to q)
        }
        return map.map { (k, items) ->
            KnowledgeStat(
                knowledge = k,
                earned = items.sumOf { it.second.score },
                full = items.sumOf { it.second.full },
                count = items.size,
                wrongCount = items.count { it.second.wrong },
                subjects = items.map { it.first }.distinct(),
            )
        }.sortedBy { it.rate }
    }

    /** 目标达成度：返回 0..1 的进度，超额也按 1 处理。 */
    fun goalProgress(current: Double?, target: Double?): Float {
        if (current == null || target == null || target <= 0) return 0f
        return (current / target).toFloat().coerceIn(0f, 1f)
    }

    fun fmtScore(v: Double): String =
        if (abs(v - v.roundToInt()) < 0.05) v.roundToInt().toString() else String.format("%.1f", v)

    fun fmtRate(r: Double): String = String.format("%.1f%%", r * 100.0)

    fun fmtDeltaPp(v: Double): String = String.format("%+.1f pp", v)

    fun fmtDeltaScore(v: Double): String = String.format("%+.1f", v)
}
