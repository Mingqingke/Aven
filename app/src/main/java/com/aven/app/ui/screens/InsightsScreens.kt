package com.aven.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aven.app.core.Dates
import com.aven.app.core.Prefs
import com.aven.app.core.Store
import com.aven.app.core.ai.Ai
import com.aven.app.core.ai.AiReport
import com.aven.app.core.ai.AiTasks
import com.aven.app.core.calc.Progress
import com.aven.app.core.calc.ScoreEngine
import com.aven.app.core.db.ReportEntity
import com.aven.app.ui.AppState
import com.aven.app.ui.components.Badge
import com.aven.app.ui.components.BoxCard
import com.aven.app.ui.components.Divider
import com.aven.app.ui.components.EmptyState
import com.aven.app.ui.components.EntryRow
import com.aven.app.ui.components.PrimaryButton
import com.aven.app.ui.components.RateBar
import com.aven.app.ui.components.ScreenTitle
import com.aven.app.ui.components.SectionLabel
import com.aven.app.ui.components.SoftButton
import com.aven.app.ui.components.StatTile
import com.aven.app.ui.components.SubjectBars
import com.aven.app.ui.components.TrendChart
import com.aven.app.ui.theme.LocalAven
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Answer
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.MindMap
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun InsightsScreen(
    onOpenSubject: (String) -> Unit,
    onOpenReport: (Long) -> Unit,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExam: (Long) -> Unit,
) {
    val c = LocalAven.current
    val exams by Store.exams.collectAsState()
    val reports by Store.reports.collectAsState()
    val tasks by Store.tasks.collectAsState()
    val goals by Store.goals.collectAsState()
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("总览", "学科", "知识点", "最佳", "AI")

    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("学业分析", "所有结论都来自本地计算与你的记录")
        TabRow(
            tabs = tabs,
            selectedTabIndex = tab,
            onTabSelected = { tab = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.weight(1f),
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(100)) },
            label = "insightTab",
        ) { current ->
            when (current) {
                0 -> OverviewTab(exams, tasks, goals, onOpenExam)
                1 -> SubjectTab(exams, onOpenSubject)
                2 -> KnowledgeTab(exams)
                3 -> BestTab(exams)
                else -> AiTab(reports, onOpenReport, onOpenChat, onOpenSettings)
            }
        }
    }
}

@Composable
private fun OverviewTab(
    exams: List<com.aven.app.core.calc.ExamSnap>,
    tasks: List<com.aven.app.core.db.TaskEntity>,
    goals: List<com.aven.app.core.db.GoalEntity>,
    onOpenExam: (Long) -> Unit,
) {
    val c = LocalAven.current
    val series = remember(exams) { ScoreEngine.overallSeries(exams) }
    val compare = remember(exams) { ScoreEngine.compare(exams, Prefs.compareBand.value.toDouble()) }
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
    ) {
        if (exams.isEmpty()) {
            item {
                EmptyState(
                    MiuixIcons.MindMap, "还没有可分析的学业数据",
                    "录入至少两场考试后显示趋势与比较",
                )
            }
        } else {
            item {
                BoxCard {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("总得分率趋势", fontSize = 12.sp, color = c.blue)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                ScoreEngine.fmtRate(series.lastOrNull()?.rate ?: 0.0),
                                fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = c.ink,
                            )
                        }
                        if (compare.hasComparison) {
                            val tint = when (compare.state) {
                                Progress.IMPROVED -> c.positive
                                Progress.DECLINED -> c.negative
                                else -> c.secondary
                            }
                            Badge(ScoreEngine.fmtDeltaPp(compare.deltaRatePp), tint, tint.copy(alpha = 0.12f))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    TrendChart(series, { it.rate }, c.ink, modifier = Modifier.fillMaxWidth(), onTap = { onOpenExam(it.examId) })
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(series.firstOrNull()?.let { Dates.fmtShort(it.date) } ?: "", fontSize = 9.5.sp, color = c.secondary)
                        Spacer(Modifier.weight(1f))
                        Text(series.lastOrNull()?.let { Dates.fmtShort(it.date) } ?: "", fontSize = 9.5.sp, color = c.secondary)
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BoxCard(modifier = Modifier.weight(1f)) {
                        StatTile("考试总数", exams.size.toString(), "场")
                        Spacer(Modifier.height(10.dp))
                        StatTile("覆盖科目", exams.flatMap { e -> e.subjects.map { it.subject } }.distinct().size.toString(), "科")
                    }
                    BoxCard(modifier = Modifier.weight(1f)) {
                        val best = exams.maxByOrNull { it.rawTotal }
                        StatTile("最高总分", best?.let { ScoreEngine.fmtScore(it.rawTotal) } ?: "—", "分")
                        Spacer(Modifier.height(10.dp))
                        StatTile("完成率", completionText(tasks), "")
                    }
                }
            }

            item {
                SectionLabel("学科分布", trailing = "按最近得分率", accent = c.blue)
                BoxCard {
                    SubjectBars(
                        exams.maxByOrNull { it.date }!!.subjects
                            .sortedByDescending { it.rate }
                            .map { it.subject to it.rate.toFloat() },
                        c.ink,
                    )
                }
            }

            if (goals.isNotEmpty()) {
                item {
                    SectionLabel("目标进度", accent = c.positive)
                    BoxCard {
                        goals.take(3).forEachIndexed { i, g ->
                            val current = if (g.subject.isBlank()) {
                                exams.maxByOrNull { it.date }?.rawTotal to exams.maxByOrNull { it.date }?.fullTotal
                            } else {
                                val s = exams.maxByOrNull { it.date }?.subject(g.subject)
                                s?.raw to s?.full
                            }
                            val p = ScoreEngine.goalProgress(current.first, g.targetScore)
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(g.subject.ifBlank { "总分" }, fontSize = 12.sp, color = c.ink, modifier = Modifier.width(52.dp))
                                Box(Modifier.weight(1f)) { RateBar(p, c.positive, height = 6.dp) }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "${current.first?.let { ScoreEngine.fmtScore(it) } ?: "—"}/${ScoreEngine.fmtScore(g.targetScore)}",
                                    fontSize = 10.5.sp, color = c.secondary,
                                )
                            }
                            if (i < minOf(goals.size, 3) - 1) Divider()
                        }
                    }
                }
            }
        }
    }
}

private fun completionText(tasks: List<com.aven.app.core.db.TaskEntity>): String {
    if (tasks.isEmpty()) return "—"
    val done = tasks.count { it.status == 1 }
    return "${done * 100 / tasks.size}%"
}

@Composable
private fun SubjectTab(exams: List<com.aven.app.core.calc.ExamSnap>, onOpenSubject: (String) -> Unit) {
    val c = LocalAven.current
    val stats = remember(exams) { ScoreEngine.subjectStats(exams) }
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
    ) {
        item {
            BoxCard(container = c.cardSoft) {
                Text("学科诊断", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.ink)
                Spacer(Modifier.height(3.dp))
                Text(
                    "按最近得分率升序 波动为历次得分率标准差",
                    fontSize = 10.5.sp, color = c.secondary, lineHeight = 15.sp,
                )
            }
        }
        items(stats) { s ->
            Spacer(Modifier.height(10.dp))
            BoxCard(onClick = { onOpenSubject(s.subject) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.subject, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.ink)
                        Spacer(Modifier.height(3.dp))
                        Text("${s.count} 次记录  正确率波动 ${String.format("%.1f", s.volatilityPp)} pp", fontSize = 10.5.sp, color = c.secondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(ScoreEngine.fmtRate(s.latestRate), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text(
                            if (s.deltaPp == 0.0) "持平" else String.format("%+.1f pp", s.deltaPp),
                            fontSize = 10.sp,
                            color = when {
                                s.deltaPp > 0.5 -> c.positive
                                s.deltaPp < -0.5 -> c.negative
                                else -> c.secondary
                            },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                RateBar(s.latestRate.toFloat(), c.ink, height = 6.dp)
                Spacer(Modifier.height(10.dp))
                Row {
                    MiniStat("最高", ScoreEngine.fmtRate(s.bestRate))
                    Spacer(Modifier.width(18.dp))
                    MiniStat("平均", ScoreEngine.fmtRate(s.avgRate))
                    Spacer(Modifier.weight(1f))
                    Icon(MiuixIcons.ChevronForward, null, Modifier.size(15.dp), tint = c.secondary)
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    val c = LocalAven.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 10.sp, color = c.secondary)
        Spacer(Modifier.width(4.dp))
        Text(value, fontSize = 10.5.sp, color = c.ink, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun KnowledgeTab(exams: List<com.aven.app.core.calc.ExamSnap>) {
    val c = LocalAven.current
    val stats = remember(exams) { ScoreEngine.knowledgeStats(exams) }
    val questionCount = exams.sumOf { e -> e.subjects.sumOf { it.questions.size } }
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
    ) {
        if (questionCount == 0) {
            item {
                EmptyState(
                    MiuixIcons.Answer, "还没有题目级数据",
                    "需要题号 得分与知识点 只有总分时不做猜测",
                )
            }
            item {
                BoxCard(container = c.cardSoft) {
                    Text("怎样补录", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "成绩档案 打开考试 题目与知识点 逐题填写",
                        fontSize = 11.sp, color = c.secondary, lineHeight = 16.sp,
                    )
                }
            }
        } else {
            item {
                BoxCard {
                    Text("已录入 $questionCount 题 ${stats.size} 个知识点", fontSize = 12.5.sp, color = c.ink)
                    Spacer(Modifier.height(4.dp))
                    Text("按得分率升序 优先关注长期偏低的考点", fontSize = 10.sp, color = c.secondary)
                }
            }
            items(stats) { s ->
                Spacer(Modifier.height(10.dp))
                BoxCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.knowledge, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.ink)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "${s.count} 题 错 ${s.wrongCount} 题 ${s.subjects.joinToString(" ")}",
                                fontSize = 10.5.sp, color = c.secondary,
                            )
                        }
                        Text(ScoreEngine.fmtRate(s.rate), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                    }
                    Spacer(Modifier.height(10.dp))
                    RateBar(s.rate.toFloat(), if (s.rate < 0.6) c.negative else c.ink, height = 6.dp)
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "来自你录入的题目数据 样本较少时仅作观察",
                    fontSize = 9.5.sp, color = c.secondary, lineHeight = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun BestTab(exams: List<com.aven.app.core.calc.ExamSnap>) {
    val c = LocalAven.current
    val bests = remember(exams) { ScoreEngine.subjectBests(exams) }
    val latest = exams.maxByOrNull { it.date }
    val combo = remember(latest, exams) { latest?.let { ScoreEngine.bestCombination(it, exams) } }
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
    ) {
        if (latest == null) {
            item { EmptyState(MiuixIcons.Report, "还没有成绩", "录入成绩后计算历史最佳") }
        } else {
            item {
                BoxCard {
                    Text("个人最佳组合", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.ink)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(combo?.let { ScoreEngine.fmtScore(it.bestTotal) } ?: "—", fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text(" / ${ScoreEngine.fmtScore(latest.fullTotal)}", fontSize = 12.sp, color = c.secondary, modifier = Modifier.padding(bottom = 6.dp))
                        Spacer(Modifier.weight(1f))
                        combo?.let { Badge("距最佳 ${ScoreEngine.fmtScore(it.gapTotal)} 分", c.ink, c.neutralBg) }
                    }
                    Spacer(Modifier.height(12.dp))
                    combo?.rows?.forEach { r ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(r.subject, fontSize = 11.5.sp, color = c.ink, modifier = Modifier.width(52.dp))
                            Box(Modifier.weight(1f)) { RateBar(r.bestRate.toFloat(), c.positive, height = 5.dp) }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (r.gapPoints > 0.05) "+${ScoreEngine.fmtScore(r.gapPoints)}" else "已达成",
                                fontSize = 10.sp, color = if (r.gapPoints > 0.05) c.secondary else c.positive,
                                modifier = Modifier.width(52.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "各科最好表现跨考试拼接 不是一次真实成绩",
                        fontSize = 9.5.sp, color = c.secondary, lineHeight = 14.sp,
                    )
                }
            }

            item { SectionLabel("各科最佳记录", trailing = "原始分与得分率分别统计") }
            item {
                BoxCard(insidePadding = 0.dp) {
                    bests.forEachIndexed { i, b ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.width(52.dp)) {
                                Text(b.subject, fontSize = 13.sp, color = c.ink)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "最高原始分 ${ScoreEngine.fmtScore(b.bestRaw)} / ${ScoreEngine.fmtScore(b.bestRawFull)}",
                                    fontSize = 11.sp, color = c.ink,
                                )
                                Text(b.bestRawTitle, fontSize = 9.5.sp, color = c.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(ScoreEngine.fmtRate(b.bestRate), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = c.ink)
                                Text("最高得分率", fontSize = 9.sp, color = c.secondary)
                            }
                        }
                        if (i < bests.lastIndex) Divider(startPadding = 16.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiTab(
    reports: List<ReportEntity>,
    onOpenReport: (Long) -> Unit,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = LocalAven.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val exams by Store.exams.collectAsState()
    val goals by Store.goals.collectAsState()
    val tasks by Store.tasks.collectAsState()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var showConsent by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val hasKey = Ai.hasKey(context)

    fun digest(): String {
        val sb = StringBuilder()
        exams.sortedByDescending { it.date }.take(6).forEach { e ->
            sb.appendLine("${Dates.fmt(e.date)} ${e.title}（${e.type}）：总分 ${ScoreEngine.fmtScore(e.rawTotal)}/${ScoreEngine.fmtScore(e.fullTotal)}，得分率 ${ScoreEngine.fmtRate(e.rate)}")
            e.subjects.forEach { sb.appendLine("  - ${it.subject} ${ScoreEngine.fmtScore(it.raw)}/${ScoreEngine.fmtScore(it.full)}") }
        }
        if (goals.isNotEmpty()) {
            sb.appendLine("目标：")
            goals.take(3).forEach { sb.appendLine("- ${it.subject.ifBlank { "总分" }} 目标 ${ScoreEngine.fmtScore(it.targetScore)}") }
        }
        if (tasks.isNotEmpty()) {
            sb.appendLine("任务：共 ${tasks.size} 个，已完成 ${tasks.count { it.status == 1 }} 个")
        }
        return sb.toString()
    }

    fun run(kind: String) {
        error = ""
        val latest = exams.maxByOrNull { it.date }
        if (latest == null) { error = "先录入至少一场考试"; return }
        scope.launch {
            busy = when (kind) { "exam" -> "生成考试诊断"; "subject" -> "生成学科诊断"; "goal" -> "生成目标计划"; else -> "复盘近期执行" }
            val band = Prefs.compareBand.value.toDouble()
            val prompt = when (kind) {
                "exam" -> AiTasks.examPrompt(
                    latest,
                    ScoreEngine.compare(exams, band, Prefs.compareRule.value),
                    ScoreEngine.bestCombination(latest, exams),
                    digest(),
                )
                "subject" -> {
                    val stat = ScoreEngine.subjectStats(exams).firstOrNull()
                    AiTasks.subjectPrompt(stat?.subject ?: latest.subjects.first().subject, digest(), goals.firstOrNull()?.let { "目标 ${ScoreEngine.fmtScore(it.targetScore)}" })
                }
                "goal" -> AiTasks.planPrompt(goals.firstOrNull()?.let { "总分目标 ${ScoreEngine.fmtScore(it.targetScore)}" } ?: "尚未设定目标", digest())
                else -> AiTasks.reviewPrompt(digest())
            }
            val model = Prefs.aiModel.value
            when (val r = Ai.chat(context, listOf("system" to AiTasks.SYSTEM, "user" to prompt), jsonMode = true, modelOverride = model)) {
                is Ai.Out.Ok -> {
                    val report = AiTasks.parseReport(r.text)
                    Store.addReport(
                        ReportEntity(
                            examId = if (kind == "exam") latest.id else null,
                            kind = kind,
                            title = report.headline.ifBlank { "AI 分析" },
                            content = report.toJson(),
                            model = model,
                        )
                    )
                    AppState.say("报告已生成")
                }
                is Ai.Out.Err -> error = r.message
            }
            busy = ""
        }
    }

    fun guarded(action: () -> Unit) {
        if (!Ai.hasKey(context)) { error = "请先在设置中填写 DeepSeek API Key"; return }
        if (!Prefs.aiConsent.value) { pendingAction = action; showConsent = true } else action()
    }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
    ) {
        if (!hasKey) {
            item {
                BoxCard(container = c.cardSoft) {
                    Text("尚未配置 DeepSeek", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = c.ink)
                    Spacer(Modifier.height(4.dp))
                    Text("成绩比较仍可使用 AI 报告需要你的 Key", fontSize = 11.sp, color = c.secondary, lineHeight = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    SoftButton("去配置", onOpenSettings)
                }
            }
        }
        item {
            SectionLabel("分析工作台", accent = c.violet)
            BoxCard(insidePadding = 0.dp) {
                EntryRow(MiuixIcons.Report, "考试诊断", "最近一场变化 强项与问题", c.ink, { guarded { run("exam") } })
                Divider(startPadding = 64.dp)
                EntryRow(MiuixIcons.MindMap, "学科诊断", "单科趋势与波动", c.ink, { guarded { run("subject") } })
                Divider(startPadding = 64.dp)
                EntryRow(MiuixIcons.Tasks, "目标规划", "把目标拆成阶段计划", c.ink, { guarded { run("goal") } })
                Divider(startPadding = 64.dp)
                EntryRow(MiuixIcons.Answer, "计划复盘", "对照任务完成情况", c.ink, { guarded { run("review") } })
            }
        }
        if (busy.isNotBlank()) {
            item {
                BoxCard(modifier = Modifier.padding(top = 12.dp), container = c.cardSoft) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        top.yukonga.miuix.kmp.basic.CircularProgressIndicator(size = 18.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(busy, fontSize = 12.sp, color = c.secondary)
                    }
                }
            }
        }
        if (error.isNotBlank()) {
            item {
                BoxCard(modifier = Modifier.padding(top = 12.dp)) {
                    Text(error, fontSize = 12.sp, color = c.negative, lineHeight = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    SoftButton("重试或检查设置", onOpenSettings)
                }
            }
        }
        item {
            SectionLabel("继续追问", accent = c.positive)
            BoxCard(onClick = onOpenChat, container = c.ink) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("和 Aven 聊聊你的成绩", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.card)
                        Spacer(Modifier.height(3.dp))
                        Text("只发送你授权的成绩摘要", fontSize = 10.5.sp, color = c.card.copy(alpha = 0.65f))
                    }
                    Icon(MiuixIcons.ChevronForward, null, Modifier.size(18.dp), tint = c.card.copy(alpha = 0.8f))
                }
            }
        }
        item { SectionLabel("已保存的报告", trailing = "${reports.size} 份", accent = c.blue) }
        if (reports.isEmpty()) {
            item {
                BoxCard {
                    Text("报告保存在这里 可随时查看", fontSize = 11.5.sp, color = c.secondary)
                }
            }
        } else {
            items(reports) { r ->
                BoxCard(modifier = Modifier.padding(bottom = 10.dp), onClick = { onOpenReport(r.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(c.neutralBg),
                            contentAlignment = Alignment.Center,
                        ) { Icon(MiuixIcons.Report, null, Modifier.size(16.dp), tint = c.ink) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.title, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${Dates.fmt(r.createdAt)} ${r.model}", fontSize = 9.5.sp, color = c.secondary)
                        }
                        Icon(MiuixIcons.ChevronForward, null, Modifier.size(15.dp), tint = c.secondary)
                    }
                }
            }
        }
    }

    if (showConsent) {
        OverlayDialog(
            show = true,
            title = "允许发送学业数据",
            summary = "发送分数 得分率 目标与任务数 不发送姓名 学校与图片",
            onDismissRequest = { showConsent = false },
        ) {
            Column {
                Text("可随时在隐私与安全中关闭", fontSize = 11.sp, color = c.secondary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoftButton("取消", { showConsent = false; pendingAction = null }, modifier = Modifier.weight(1f))
                    PrimaryButton("同意", {
                        Prefs.setAiConsent(true)
                        showConsent = false
                        pendingAction?.invoke()
                        pendingAction = null
                    }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ReportDetailScreen(reportId: Long, onBack: () -> Unit, onDeleted: () -> Unit) {
    val c = LocalAven.current
    val reports by Store.reports.collectAsState()
    val entity = reports.firstOrNull { it.id == reportId }
    val report = remember(entity) { entity?.let { AiReport.fromJson(it.content) } }
    var showDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("分析报告", entity?.let { "${Dates.fmt(it.createdAt)}  ${it.model}" } ?: "", onBack = onBack) {
            SoftButton("删除", { showDelete = true }, color = c.negative)
        }
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
        ) {
            if (entity == null) {
                item { EmptyState(MiuixIcons.Report, "报告不存在", "可能已被删除", "返回", onBack) }
            } else if (report == null) {
                item {
                    BoxCard {
                        Text("这份报告是纯文本记录", fontSize = 12.sp, color = c.secondary)
                        Spacer(Modifier.height(8.dp))
                        Text(entity.content, fontSize = 13.sp, color = c.ink, lineHeight = 20.sp)
                    }
                }
            } else {
                item {
                    BoxCard {
                        if (report.headline.isNotBlank()) {
                            Text(report.headline, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = c.ink, lineHeight = 27.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(report.summary, fontSize = 12.5.sp, color = c.secondary, lineHeight = 19.sp)
                        if (report.degraded) {
                            Spacer(Modifier.height(8.dp))
                            Text("模型返回非结构化内容 已保留原文", fontSize = 10.sp, color = c.amber)
                        }
                    }
                }
                reportSection("已知事实", report.facts, c.ink, c.ink)
                reportSection("长期趋势", report.trends, c.ink, c.ink)
                reportSection("优势", report.strengths, c.positive, c.ink)
                reportSection("需要关注", report.issues, c.negative, c.ink)
                reportSection("待验证的推测", report.hypotheses, c.amber, c.ink)
                reportSection("行动建议", report.actions, c.blue, c.ink)
                if (report.next.isNotBlank()) {
                    item {
                        SectionLabel("下一阶段")
                        BoxCard(container = c.cardSoft) {
                            Text(report.next, fontSize = 13.sp, color = c.ink, lineHeight = 20.sp)
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "报告由 ${entity.model} 生成 推测部分需更多数据验证",
                        fontSize = 9.5.sp, color = c.secondary.copy(alpha = 0.8f), lineHeight = 14.sp,
                    )
                }
            }
        }
    }

    if (showDelete) {
        OverlayDialog(
            show = true, title = "删除这份报告", summary = "不影响成绩数据",
            onDismissRequest = { showDelete = false },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton("取消", { showDelete = false }, modifier = Modifier.weight(1f))
                PrimaryButton("删除", { scope.launch { Store.deleteReport(reportId); showDelete = false; onDeleted() } }, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.reportSection(
    title: String,
    items: List<String>,
    color: androidx.compose.ui.graphics.Color,
    ink: androidx.compose.ui.graphics.Color,
) {
    if (items.isEmpty()) return
    item {
        SectionLabel(title)
        BoxCard {
            items.forEachIndexed { i, s ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Box(Modifier.padding(top = 6.dp).size(4.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(8.dp))
                    Text(s, fontSize = 12.5.sp, color = ink, lineHeight = 19.sp)
                }
            }
        }
    }
}
