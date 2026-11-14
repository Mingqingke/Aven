package com.aven.app.core.ocr

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 成绩单文字解析：纯 Kotlin，可单元测试。
 * 只做“猜字段”，最终结果必须由用户在核对界面确认后才入库。
 */
data class ParsedSubject(
    val subject: String,
    val score: Double?,
    val full: Double?,
    val rank: Int? = null,
    val total: Int? = null,
    val rawLine: String = "",
    val confident: Boolean = false,
)

data class ParsedSheet(
    val title: String? = null,
    val dateMillis: Long? = null,
    val dateText: String? = null,
    val subjects: List<ParsedSubject> = emptyList(),
    val rawText: String = "",
)

object SheetParser {

    private val SUBJECTS = listOf(
        "语文", "数学", "英语", "物理", "化学", "生物",
        "思想政治", "道德与法治", "政治", "历史", "地理",
        "科学", "信息技术", "通用技术", "日语", "俄语", "体育", "音乐", "美术",
        "理综", "文综", "综合",
    ).sortedByDescending { it.length }

    private val TITLE_KEYS = listOf(
        "期末", "期中", "一模", "二模", "三模", "月考", "联考", "统考",
        "考试", "检测", "测验", "调研", "诊断", "学业水平", "单元",
    )

    private val FULL_CANDIDATES = setOf(
        50.0, 60.0, 70.0, 75.0, 80.0, 90.0, 100.0, 105.0, 110.0,
        120.0, 130.0, 135.0, 150.0, 200.0, 250.0, 300.0, 750.0,
    )

    private val DATE_FULL = Regex("(20\\d{2})\\s*[-/年.]\\s*(\\d{1,2})\\s*[-/月.]\\s*(\\d{1,2})")
    private val DATE_SHORT = Regex("(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日")
    private val NUM = Regex("(\\d{1,3}(?:\\.\\d{1,2})?)")
    private val RANK = Regex("(?:排名|名次|级名|班名)\\D{0,3}(\\d{1,4})\\s*(?:/\\s*(\\d{1,5}))?")

    fun defaultFull(subject: String): Double = when (subject) {
        "语文", "数学", "英语" -> 150.0
        "理综", "文综", "综合" -> 300.0
        else -> 100.0
    }

    fun parse(text: String, nowMillis: Long = System.currentTimeMillis()): ParsedSheet {
        val lines = text.split('\n', '\r')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val title = detectTitle(lines)
        val (dateMillis, dateText) = detectDate(lines, nowMillis)
        val subjects = linkedMapOf<String, ParsedSubject>()

        for (raw in lines) {
            val line = raw.replace('　', ' ')
            val subject = findSubject(line) ?: continue
            if (subjects.containsKey(subject)) continue

            val rankMatch = RANK.find(line)
            val rank = rankMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
            val total = rankMatch?.groupValues?.getOrNull(2)?.toIntOrNull()?.takeIf { it > 0 }

            // 去掉日期与排名的数字，避免把日期当成分数
            var work = line
            DATE_FULL.findAll(line).forEach { work = work.replace(it.value, " ") }
            DATE_SHORT.findAll(line).forEach { work = work.replace(it.value, " ") }
            rankMatch?.let { work = work.replace(it.value, " ") }
            work = work.replace(subject, " ")

            val slash = Regex("(\\d{1,3}(?:\\.\\d{1,2})?)\\s*/\\s*(\\d{1,3}(?:\\.\\d{1,2})?)").find(work)
            var score: Double? = null
            var full: Double? = null
            var confident = false
            if (slash != null) {
                score = slash.groupValues[1].toDoubleOrNull()
                full = slash.groupValues[2].toDoubleOrNull()
                confident = valid(score, full)
            } else {
                val nums = NUM.findAll(work).mapNotNull { it.value.toDoubleOrNull() }.toList()
                when {
                    nums.size >= 2 -> {
                        val a = nums[0]
                        val b = nums[1]
                        if (a in FULL_CANDIDATES && b < a) {
                            // 出现 “150 112” 这种满分在前的写法
                            full = a; score = b
                        } else {
                            score = a; full = b
                        }
                        confident = valid(score, full)
                    }
                    nums.size == 1 -> {
                        score = nums[0]
                        full = defaultFull(subject)
                        confident = false
                    }
                }
            }
            // “满分 150” 单独出现时的修正
            Regex("满分\\D{0,2}(\\d{1,3}(?:\\.\\d{1,2})?)").find(line)?.let {
                it.groupValues[1].toDoubleOrNull()?.let { f -> full = f }
            }
            if (score != null && full != null && score!! > full!!) {
                val t = score; score = full; full = t
            }
            if (score != null) {
                subjects[subject] = ParsedSubject(
                    subject = subject,
                    score = score,
                    full = full,
                    rank = rank,
                    total = total,
                    rawLine = raw,
                    confident = confident,
                )
            }
        }

        return ParsedSheet(
            title = title,
            dateMillis = dateMillis,
            dateText = dateText,
            subjects = subjects.values.toList(),
            rawText = text,
        )
    }

    private fun valid(score: Double?, full: Double?): Boolean {
        if (score == null || full == null) return false
        if (full <= 0) return false
        if (score < 0 || score > full * 1.05) return false
        return full in FULL_CANDIDATES || score <= full
    }

    private fun findSubject(line: String): String? {
        for (s in SUBJECTS) {
            var idx = line.indexOf(s)
            while (idx >= 0) {
                // “物理”不能被“物理化学生物”这类合称误判成单科
                val chained = line.length > idx + s.length &&
                    SUBJECTS.any { o -> o != s && line.startsWith(o, idx + s.length) }
                if (!chained) return s
                idx = line.indexOf(s, idx + 1)
            }
        }
        return null
    }

    private fun detectTitle(lines: List<String>): String? {
        val candidates = lines.filter { l ->
            l.length in 4..32 && TITLE_KEYS.any { l.contains(it) } &&
                !l.contains("序号") && !l.contains("姓名") && !l.contains("准考证") &&
                findSubject(l) == null
        }
        return candidates.minByOrNull { line ->
            val keyScore = TITLE_KEYS.indexOfFirst { line.contains(it) }.let { if (it < 0) 99 else it }
            keyScore * 10 + line.length / 10
        }?.take(32)
    }

    private fun detectDate(lines: List<String>, nowMillis: Long): Pair<Long?, String?> {
        for (l in lines) {
            DATE_FULL.find(l)?.let { m ->
                val d = runCatching {
                    LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
                }.getOrNull()
                if (d != null) return toMillis(d) to m.value
            }
            DATE_SHORT.find(l)?.let { m ->
                val year = LocalDate.now().year
                val d = runCatching {
                    LocalDate.of(year, m.groupValues[1].toInt(), m.groupValues[2].toInt())
                }.getOrNull()
                if (d != null) return toMillis(d) to m.value
            }
        }
        return null to null
    }

    private fun toMillis(d: LocalDate): Long =
        d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun formatDate(millis: Long): String =
        java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
            .toLocalDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
}
