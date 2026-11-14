package com.aven.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aven.app.core.Dates
import com.aven.app.core.Prefs
import com.aven.app.core.Store
import com.aven.app.core.calc.CompareResult
import com.aven.app.core.calc.ExamSnap
import com.aven.app.core.calc.Progress
import com.aven.app.core.calc.ScoreEngine
import com.aven.app.core.db.GoalEntity
import com.aven.app.core.db.TaskEntity
import com.aven.app.ui.components.Badge
import com.aven.app.ui.components.BoxCard
import com.aven.app.ui.components.Divider
import com.aven.app.ui.components.EntryRow
import com.aven.app.ui.components.MiniSparkline
import com.aven.app.ui.components.RateBar
import com.aven.app.ui.components.SectionLabel
import com.aven.app.ui.components.StatTile
import com.aven.app.ui.theme.LocalAven
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.UploadCloud
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 首页：与 MEMORA 同版式
 * 左卡占满高度 右侧两张小卡 间距 12 不做任何静态旋转
 */
@Composable
fun HomeScreen(
    onOpenExam: (Long) -> Unit,
    onOpenSubject: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlans: () -> Unit,
    onAddExam: () -> Unit,
    onOpenInsights: () -> Unit,
) {
    val c = LocalAven.current
    val exams by Store.exams.collectAsState()
    val tasks by Store.tasks.collectAsState()
    val goals by Store.goals.collectAsState()
    val band by Prefs.compareBand.collectAsState()
    val rule by Prefs.compareRule.collectAsState()
    val nickname by Prefs.nickname.collectAsState()
    val cardA by Prefs.cardA.collectAsState()
    val cardB by Prefs.cardB.collectAsState()

    val compare = remember(exams, band, rule) { ScoreEngine.compare(exams, band.toDouble(), rule) }
    val latest = exams.maxByOrNull { it.date }
    val combo = remember(latest, exams) { latest?.let { ScoreEngine.bestCombination(it, exams) } }
    val scope = rememberCoroutineScope()
    val empty = exams.isEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, top = 10.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${Dates.greeting()} $nickname",
                    fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold,
                    color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(Dates.headerDate(), fontSize = 11.5.sp, color = c.secondary)
            }
            Avatar(nickname)
            Spacer(Modifier.width(10.dp))
            HeaderIcon(MiuixIcons.Settings, "设置", onOpenSettings)
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 600.dp
        val rightW = if (wide) 150.dp else 104.dp
        val heroH = if (wide) 254.dp else 220.dp
        Row(
            Modifier.fillMaxWidth().height(heroH),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                ProgressCard(
                    compare = compare,
                    latest = latest,
                    modifier = Modifier.fillMaxSize(),
                    onOpen = { latest?.let { onOpenExam(it.id) } },
                    onAddExam = onAddExam,
                )
            }
            Column(
                Modifier.width(rightW).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    MiniCard(
                        key = cardA, exams = exams, combo = combo, tasks = tasks, goals = goals,
                        latest = latest, modifier = Modifier.fillMaxSize(),
                        onClick = { latest?.let { onOpenExam(it.id) } ?: onAddExam() },
                        onOpenInsights = onOpenInsights, onOpenPlans = onOpenPlans,
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    MiniCard(
                        key = cardB, exams = exams, combo = combo, tasks = tasks, goals = goals,
                        latest = latest, modifier = Modifier.fillMaxSize(),
                        onClick = { latest?.let { onOpenExam(it.id) } ?: onAddExam() },
                        onOpenInsights = onOpenInsights, onOpenPlans = onOpenPlans,
                    )
                }
            }
        }
        }

        if (!empty) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.blueBg)
                    .clickable(onClick = onAddExam)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(MiuixIcons.UploadCloud, null, Modifier.size(18.dp), tint = c.blue)
                Spacer(Modifier.width(9.dp))
                Text("录入下一次考试", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.blue)
                Spacer(Modifier.weight(1f))
                Text("拍照 相册 手动", fontSize = 10.sp, color = c.blue.copy(alpha = 0.65f))
                Spacer(Modifier.width(4.dp))
                Icon(MiuixIcons.ChevronForward, null, Modifier.size(14.dp), tint = c.blue.copy(alpha = 0.7f))
            }
        }

        if (empty) {
            SectionLabel("开始记录", accent = c.blue)
            BoxCard(insidePadding = 0.dp) {
                EntryRow(MiuixIcons.Scan, "上传成绩单", "拍照或从相册选择", c.positive, onAddExam)
                Divider(startPadding = 64.dp)
                EntryRow(MiuixIcons.Edit, "手动添加", "逐科填写分数与满分", c.ink, onAddExam)
                Divider(startPadding = 64.dp)
                EntryRow(MiuixIcons.Settings, "完善档案", "学段 年级 选科", c.secondary, onOpenSettings)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "录入之后显示与上一场考试的比较",
                fontSize = 9.5.sp, color = c.secondary.copy(alpha = 0.75f),
                modifier = Modifier.padding(start = 6.dp),
            )
        } else {
            if (latest != null) {
                SectionLabel("学科趋势", trailing = "${latest!!.subjects.size} 科", accent = c.blue)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(latest!!.subjects) { s ->
                        val stat = remember(exams, s.subject) { ScoreEngine.subjectStats(exams).firstOrNull { it.subject == s.subject } }
                        val spark = remember(exams, s.subject) { ScoreEngine.subjectSeries(exams, s.subject).map { it.rate } }
                        SubjectMiniCard(
                            subject = s.subject,
                            score = s.raw,
                            deltaPp = stat?.deltaPp ?: 0.0,
                            values = spark,
                            onClick = { onOpenSubject(s.subject) },
                        )
                    }
                }
            }

            val today = tasks.filter { it.status == 0 && (it.dueDate == 0L || it.dueDate <= Dates.today() + 86_400_000L - 1) }
            val upcoming = goals.filter { it.examDate > System.currentTimeMillis() && it.subject.isBlank() }
                .sortedBy { it.examDate }.take(2)
            SectionLabel("今日任务", trailing = "计划", accent = c.positive)
            BoxCard(insidePadding = 0.dp) {
                if (today.isEmpty() && upcoming.isEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().clickable(onClick = onOpenPlans).padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(MiuixIcons.AddCircle, null, Modifier.size(17.dp), tint = c.secondary)
                        Spacer(Modifier.width(9.dp))
                        Text("今天没有任务", fontSize = 12.5.sp, color = c.secondary, modifier = Modifier.weight(1f))
                        Text("去计划", fontSize = 10.5.sp, color = c.blue)
                    }
                } else {
                    today.take(2).forEachIndexed { index, task ->
                        TaskRow(task) {
                            scope.launch {
                                Store.updateTask(task.copy(status = 1, doneAt = System.currentTimeMillis()))
                            }
                        }
                        if (index < minOf(today.size, 2) - 1) Divider(startPadding = 52.dp)
                    }
                    if (upcoming.isNotEmpty()) {
                        if (today.isNotEmpty()) Divider(startPadding = 14.dp)
                        upcoming.forEachIndexed { i, g ->
                            Row(
                                Modifier.fillMaxWidth().clickable(onClick = onOpenPlans)
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("考试", fontSize = 9.5.sp, color = c.amber)
                                Spacer(Modifier.width(8.dp))
                                Text(g.note.ifBlank { "考试" }, fontSize = 12.5.sp, color = c.ink, modifier = Modifier.weight(1f))
                                Text("${Dates.daysBetween(System.currentTimeMillis(), g.examDate)} 天", fontSize = 10.5.sp, color = c.secondary)
                            }
                            if (i < upcoming.size - 1) Divider(startPadding = 14.dp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun homeFeedback(): PressFeedbackType {
    val reduce by Prefs.reduceMotion.collectAsState()
    return if (reduce) PressFeedbackType.None else PressFeedbackType.Tilt
}

@Composable
private fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val c = LocalAven.current
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(c.card).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, Modifier.size(19.dp), tint = c.ink) }
}

@Composable
private fun Avatar(name: String) {
    val c = LocalAven.current
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(c.ink),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.take(1).ifBlank { "A" }, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.card)
    }
}

@Composable
private fun ProgressCard(
    compare: CompareResult,
    latest: ExamSnap?,
    modifier: Modifier,
    onOpen: () -> Unit,
    onAddExam: () -> Unit,
) {
    val c = LocalAven.current
    val targetBg = when (compare.state) {
        Progress.IMPROVED -> c.positiveBg
        Progress.DECLINED -> c.negativeBg
        else -> c.neutralBg
    }
    val targetFg = when (compare.state) {
        Progress.IMPROVED -> c.positiveInk
        Progress.DECLINED -> c.negativeInk
        else -> c.ink
    }
    val bg by animateColorAsState(targetBg, tween(320), label = "statusBg")
    val fg by animateColorAsState(targetFg, tween(320), label = "statusFg")
    val title = when (compare.state) {
        Progress.IMPROVED -> "考试进步"
        Progress.DECLINED -> "考试退步"
        Progress.FLAT -> "基本持平"
        Progress.INSUFFICIENT -> "开始记录你的成长"
    }
    val emoji = when (compare.state) {
        Progress.IMPROVED -> "😋"
        Progress.DECLINED -> "😡"
        Progress.FLAT -> "😑"
        Progress.INSUFFICIENT -> ""
    }
    Card(
        modifier = modifier,
        cornerRadius = 26.dp,
        colors = com.aven.app.ui.components.avenCardColors(bg),
        pressFeedbackType = homeFeedback(),
        showIndication = true,
        onClick = if (latest != null) onOpen else onAddExam,
    ) {
        Column(Modifier.fillMaxSize().padding(start = 18.dp, top = 18.dp, end = 16.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "学业进度", fontSize = 10.sp, fontWeight = FontWeight.Medium,
                    color = fg.copy(alpha = 0.55f), modifier = Modifier.weight(1f),
                )
                if (compare.hasComparison) {
                    Text(compare.confidenceText, fontSize = 9.sp, color = fg.copy(alpha = 0.45f))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title, fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold,
                    color = fg, maxLines = 2,
                )
                if (emoji.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(emoji, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (latest != null) {
                Text(
                    latest.title, fontSize = 12.5.sp, fontWeight = FontWeight.Medium,
                    color = fg.copy(alpha = 0.88f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text("${Dates.fmt(latest.date)} ${latest.type}", fontSize = 10.sp, color = fg.copy(alpha = 0.52f))
            } else {
                Text("上传第一张成绩单", fontSize = 11.sp, color = fg.copy(alpha = 0.65f))
            }
            Spacer(Modifier.height(12.dp))
            if (compare.hasComparison) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        compare.deltaRaw?.let { ScoreEngine.fmtDeltaScore(it) + " 分" }
                            ?: ScoreEngine.fmtDeltaPp(compare.deltaRatePp),
                        fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = fg,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "较 ${compare.prevTitle}", fontSize = 9.5.sp, color = fg.copy(alpha = 0.55f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(compare.basis, fontSize = 9.sp, color = fg.copy(alpha = 0.45f), maxLines = 1)
                Spacer(Modifier.weight(1f))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (latest != null) "录入下一次考试即可比较" else "添加考试",
                        fontSize = 11.sp, color = fg.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(MiuixIcons.ChevronForward, null, Modifier.size(14.dp), tint = fg.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun MiniCard(
    key: String,
    exams: List<ExamSnap>,
    combo: ScoreEngine.BestCombo?,
    tasks: List<TaskEntity>,
    goals: List<GoalEntity>,
    latest: ExamSnap?,
    modifier: Modifier,
    onClick: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenPlans: () -> Unit,
) {
    val c = LocalAven.current
    val stats = remember(exams) { ScoreEngine.subjectStats(exams) }
    val series = remember(exams) { ScoreEngine.overallSeries(exams) }
    val title = when (key) {
        "countdown" -> "下一次考试"
        "gaokao" -> "高考倒计时"
        "best" -> "最高总分"
        "gap" -> "距个人最佳"
        "weak" -> "需要关注"
        "tasks" -> "今日任务"
        "trend" -> "成绩趋势"
        "target" -> "距目标"
        else -> "最近考试"
    }
    val open = when (key) {
        "gap", "best", "weak", "trend" -> onOpenInsights
        "countdown", "tasks", "target" -> onOpenPlans
        else -> onClick
    }
    val tint = when (key) {
        "countdown", "target" -> c.blueBg
        "gaokao" -> c.violetBg
        "tasks" -> c.positiveBg
        "weak" -> c.amberBg
        else -> c.card
    }
    Card(
        modifier = modifier,
        cornerRadius = 22.dp,
        colors = com.aven.app.ui.components.avenCardColors(tint),
        pressFeedbackType = homeFeedback(),
        showIndication = true,
        onClick = open,
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(title, fontSize = 9.5.sp, fontWeight = FontWeight.Medium, color = c.secondary, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            when (key) {
                "countdown" -> {
                    val next = goals.filter { it.examDate > System.currentTimeMillis() && it.subject.isBlank() }
                        .minByOrNull { it.examDate }
                    if (next == null) {
                        Text("未设定", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text("计划中设定", fontSize = 9.sp, color = c.secondary)
                    } else {
                        Text("${Dates.daysBetween(System.currentTimeMillis(), next.examDate)}", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text("天后", fontSize = 9.sp, color = c.secondary)
                    }
                }
                "gaokao" -> {
                    Text("${Dates.daysBetween(System.currentTimeMillis(), Dates.gaokaoDate())}", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                    Text("天", fontSize = 9.sp, color = c.secondary)
                }
                "best" -> {
                    val best = exams.maxByOrNull { it.rawTotal }
                    Text(best?.let { ScoreEngine.fmtScore(it.rawTotal) } ?: "—", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                    Text(best?.type ?: "暂无", fontSize = 9.sp, color = c.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                "gap" -> {
                    if (combo == null) {
                        Text("—", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text("暂无", fontSize = 9.sp, color = c.secondary)
                    } else {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(ScoreEngine.fmtScore(combo.gapTotal), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                            Text(" 分", fontSize = 9.sp, color = c.secondary, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Text("最佳 ${ScoreEngine.fmtScore(combo.bestTotal)}", fontSize = 9.sp, color = c.secondary)
                    }
                }
                "weak" -> {
                    val weak = stats.firstOrNull()
                    Text(weak?.subject ?: "—", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                    Text(weak?.let { ScoreEngine.fmtRate(it.latestRate) } ?: "暂无", fontSize = 9.sp, color = c.secondary)
                }
                "tasks" -> {
                    val done = tasks.count { it.status == 1 && Dates.fmt(it.doneAt) == Dates.fmt(System.currentTimeMillis()) }
                    val pending = tasks.count { it.status == 0 }
                    Text("$done/${done + pending}", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                    Text("已完成", fontSize = 9.sp, color = c.secondary)
                }
                "trend" -> {
                    Text(
                        series.lastOrNull()?.let { ScoreEngine.fmtRate(it.rate) } ?: "—",
                        fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = c.ink,
                    )
                    MiniSparkline(series.map { it.rate }, c.positive, height = 24.dp)
                }
                "target" -> {
                    val g = goals.filter { it.subject.isBlank() }.minByOrNull { it.examDate }
                    val cur = latest?.rawTotal
                    if (g == null || cur == null) {
                        Text("—", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text("未设目标", fontSize = 9.sp, color = c.secondary)
                    } else {
                        val gap = g.targetScore - cur
                        Text(
                            if (gap > 0) ScoreEngine.fmtScore(gap) else "达成",
                            fontSize = if (gap > 0) 24.sp else 17.sp,
                            fontWeight = FontWeight.SemiBold, color = c.ink,
                        )
                        Text(if (gap > 0) "分差" else "已达标", fontSize = 9.sp, color = c.secondary)
                    }
                }
                else -> {
                    if (latest == null) {
                        Text("—", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text("暂无", fontSize = 9.sp, color = c.secondary)
                    } else {
                        Text(ScoreEngine.fmtScore(latest.rawTotal), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text(ScoreEngine.fmtRate(latest.rate), fontSize = 9.sp, color = c.secondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectMiniCard(
    subject: String,
    score: Double,
    deltaPp: Double,
    values: List<Double>,
    onClick: () -> Unit,
) {
    val c = LocalAven.current
    val tint = when {
        deltaPp > 0.5 -> c.positive
        deltaPp < -0.5 -> c.negative
        else -> c.secondary
    }
    BoxCard(modifier = Modifier.width(128.dp), corner = 20.dp, onClick = onClick, insidePadding = 13.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(subject, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = c.ink, modifier = Modifier.weight(1f))
            if (deltaPp != 0.0) Text(String.format("%+.0f", deltaPp), fontSize = 9.sp, color = tint)
        }
        Spacer(Modifier.height(4.dp))
        Text(ScoreEngine.fmtScore(score), fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
        MiniSparkline(values, tint, height = 26.dp)
    }
}

@Composable
private fun TaskRow(task: TaskEntity, onToggle: () -> Unit) {
    val c = LocalAven.current
    val done = task.status == 1
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(if (done) c.positive else c.neutralBg),
            contentAlignment = Alignment.Center,
        ) { if (done) Icon(MiuixIcons.Ok, null, Modifier.size(13.dp), tint = c.card) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                task.title, fontSize = 13.5.sp, color = if (done) c.secondary else c.ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (done) TextDecoration.LineThrough else null,
            )
            if (task.subject.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(task.subject, fontSize = 10.sp, color = c.secondary)
            }
        }
        if (task.estimateMin > 0) Text("${task.estimateMin} 分钟", fontSize = 10.sp, color = c.secondary)
    }
}
