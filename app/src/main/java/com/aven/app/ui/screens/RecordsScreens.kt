package com.aven.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aven.app.core.Dates
import com.aven.app.core.Prefs
import com.aven.app.core.Store
import com.aven.app.core.calc.ExamSnap
import com.aven.app.core.calc.Progress
import com.aven.app.core.calc.QScore
import com.aven.app.core.calc.ScoreEngine
import com.aven.app.ui.components.Badge
import com.aven.app.ui.components.BoxCard
import com.aven.app.ui.components.Divider
import com.aven.app.ui.components.EmptyState
import com.aven.app.ui.components.LabeledInput
import com.aven.app.ui.components.PrimaryButton
import com.aven.app.ui.components.RateBar
import com.aven.app.ui.components.ScreenTitle
import com.aven.app.ui.components.SectionLabel
import com.aven.app.ui.components.SoftButton
import com.aven.app.ui.components.StatTile
import com.aven.app.ui.components.TrendChart
import com.aven.app.ui.theme.LocalAven
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Import
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun RecordsScreen(
    onOpenExam: (Long) -> Unit,
    onAddExam: () -> Unit,
    onOpenSubject: (String) -> Unit,
    onBatch: (List<String>) -> Unit,
) {
    val c = LocalAven.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val exams by Store.exams.collectAsState()
    var filter by remember { mutableStateOf("全部") }
    val types = remember(exams) { listOf("全部") + exams.map { it.type }.distinct().take(4) }
    val shown = if (filter == "全部") exams else exams.filter { it.type == filter }
    val allSubjects = remember(exams) { exams.flatMap { e -> e.subjects.map { it.subject } }.distinct() }
    val batchPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val paths = uris.mapNotNull { Store.importImage(it) }
                if (paths.isNotEmpty()) onBatch(paths)
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("成绩档案", "${exams.size} 场考试  ${allSubjects.size} 个科目") {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(c.neutralBg)
                    .clickable { batchPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(MiuixIcons.Import, "批量导入", Modifier.size(19.dp), tint = c.ink)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(c.ink).clickable(onClick = onAddExam),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MiuixIcons.Add, "添加考试", Modifier.size(19.dp), tint = c.card)
            }
        }
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
        ) {
            if (exams.isEmpty()) {
                item {
                    EmptyState(
                        icon = MiuixIcons.Scan,
                        title = "还没有考试成绩",
                        desc = "上传成绩单 自动整理成可比较的学业档案",
                        action = "添加第一场考试",
                        onAction = onAddExam,
                    )
                }
            } else {
                item {
                    BoxCard(container = c.blueBg) {
                        Row {
                            StatTile("考试", exams.size.toString(), "场", accent = c.blue, modifier = Modifier.weight(1f))
                            StatTile("科目", allSubjects.size.toString(), "个", accent = c.violet, modifier = Modifier.weight(1f))
                            StatTile(
                                "最近得分率",
                                String.format("%.1f", exams.maxByOrNull { it.date }!!.rate * 100),
                                "%", accent = c.positive, modifier = Modifier.weight(1.2f),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        val latest = exams.maxByOrNull { it.date }!!
                        val best = exams.maxByOrNull { it.rawTotal }!!
                        Text(
                            "历史最高总分 ${ScoreEngine.fmtScore(best.rawTotal)}  ${best.title}",
                            fontSize = 10.5.sp, color = c.secondary,
                        )
                    }
                }
                if (types.size > 1) {
                    item {
                        LazyRow(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(types) { t ->
                                Chip(t, t == filter) { filter = t }
                            }
                        }
                    }
                }
                item { SectionLabel("按时间排序", trailing = "点击查看详情", accent = c.blue) }
                items(shown) { e ->
                    ExamRow(e) { onOpenExam(e.id) }
                    Spacer(Modifier.height(12.dp))
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    SoftButton("添加考试", onAddExam, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    SoftButton(
                        "批量导入历史成绩",
                        { batchPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalAven.current
    if (selected) {
        top.yukonga.miuix.kmp.basic.Button(
            onClick = onClick,
            minWidth = 0.dp,
            minHeight = 34.dp,
            cornerRadius = 11.dp,
            insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.buttonColors(
                color = c.blue,
                contentColor = Color.White,
            ),
        ) {
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
    } else {
        top.yukonga.miuix.kmp.basic.TextButton(
            text = text,
            onClick = onClick,
            minWidth = 0.dp,
            minHeight = 34.dp,
            cornerRadius = 11.dp,
            insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
            colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColors(
                color = c.neutralBg,
                textColor = c.ink,
            ),
        )
    }
}

@Composable
private fun ExamRow(e: ExamSnap, onClick: () -> Unit) {
    val c = LocalAven.current
    BoxCard(onClick = onClick, insidePadding = 0.dp) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.width(46.dp).clip(RoundedCornerShape(14.dp)).background(c.blueBg).padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(Dates.date(e.date).monthValue.toString(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.blue)
                Text("月", fontSize = 9.sp, color = c.secondary)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(e.title, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(
                    "${Dates.fmt(e.date)} ${e.type} ${e.subjects.size} 科",
                    fontSize = 10.5.sp, color = c.secondary,
                )
                Spacer(Modifier.height(8.dp))
                RateBar(e.rate.toFloat(), c.ink, height = 5.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(ScoreEngine.fmtScore(e.rawTotal), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                Text(ScoreEngine.fmtRate(e.rate), fontSize = 9.5.sp, color = c.secondary)
            }
        }
    }
}

@Composable
fun ExamDetailScreen(
    examId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenSubject: (String) -> Unit,
    onDeleted: () -> Unit,
) {
    val c = LocalAven.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val exams by Store.exams.collectAsState()
    val exam = exams.firstOrNull { it.id == examId }
    val band by Prefs.compareBand.collectAsState()
    var showDelete by remember { mutableStateOf(false) }
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var detailSubject by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(examId) { images = Store.examImages(examId) }

    if (exam == null) {
        Column(Modifier.fillMaxWidth()) {
            ScreenTitle("考试详情", onBack = onBack)
            EmptyState(MiuixIcons.ListView, "这场考试已不存在", "可能已被删除", "返回", onBack)
        }
        return
    }

    val compare = remember(exams, examId, band) { ScoreEngine.compareWith(exams, examId, band.toDouble()) }
    val combo = remember(examId, exams) { ScoreEngine.bestCombination(exam, exams) }

    Column(Modifier.fillMaxWidth()) {
        ScreenTitle(exam.title, "${Dates.fmt(exam.date)} ${exam.type}", onBack = onBack) {
            SoftButton("编辑", onEdit)
        }
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
        ) {
            item {
                BoxCard {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(ScoreEngine.fmtScore(exam.rawTotal), fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text(" / ${ScoreEngine.fmtScore(exam.fullTotal)}", fontSize = 13.sp, color = c.secondary, modifier = Modifier.padding(bottom = 8.dp))
                        Spacer(Modifier.weight(1f))
                        Badge("得分率 ${ScoreEngine.fmtRate(exam.rate)}", c.ink, c.neutralBg)
                    }
                    Spacer(Modifier.height(12.dp))
                    RateBar(exam.rate.toFloat(), c.ink, height = 7.dp)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        StatTile("科目", exam.subjects.size.toString(), "科")
                        if (exam.hasConverted) StatTile("赋分总分", ScoreEngine.fmtScore(exam.convertedTotal), modifier = Modifier.weight(1f))
                        StatTile("学段", exam.stage)
                    }
                }
            }

            item {
                SectionLabel("与上一场比较", accent = c.violet)
                BoxCard(container = when (compare.state) {
                    Progress.IMPROVED -> c.positiveBg
                    Progress.DECLINED -> c.negativeBg
                    else -> c.card
                }) {
                    if (!compare.hasComparison) {
                        Text("这场考试没有可比的上一场考试", fontSize = 13.sp, color = c.ink)
                        Spacer(Modifier.height(4.dp))
                        Text(compare.reasons.firstOrNull() ?: "需要同科目历史成绩才能比较", fontSize = 11.sp, color = c.secondary)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (compare.state) {
                                    Progress.IMPROVED -> "进步 😋"
                                    Progress.DECLINED -> "退步 😡"
                                    else -> "基本持平 😑"
                                },
                                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.weight(1f),
                            )
                            Badge(compare.confidenceText, c.ink, c.ink.copy(alpha = 0.08f))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            (compare.deltaRaw?.let { "原始分 ${ScoreEngine.fmtDeltaScore(it)} 分  " } ?: "") +
                                "得分率 ${ScoreEngine.fmtDeltaPp(compare.deltaRatePp)}  对比 ${compare.prevTitle}",
                            fontSize = 11.5.sp, color = c.secondary,
                        )
                        Spacer(Modifier.height(10.dp))
                        compare.reasons.take(4).forEach {
                            Row(Modifier.padding(vertical = 2.dp)) {
                                Box(Modifier.padding(top = 6.dp).size(3.dp).clip(CircleShape).background(c.secondary))
                                Spacer(Modifier.width(7.dp))
                                Text(it, fontSize = 11.sp, color = c.secondary, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }

            item { SectionLabel("单科成绩", trailing = "点击查看学科历史", accent = c.blue) }
            item {
                BoxCard(insidePadding = 0.dp) {
                    exam.subjects.forEachIndexed { index, s ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenSubject(s.subject) }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(s.subject, fontSize = 13.5.sp, color = c.ink, modifier = Modifier.width(52.dp))
                            Box(Modifier.weight(1f)) { RateBar(s.rate.toFloat(), c.ink, height = 6.dp) }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${ScoreEngine.fmtScore(s.raw)}/${ScoreEngine.fmtScore(s.full)}",
                                fontSize = 12.sp, color = c.ink, modifier = Modifier.width(66.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            )
                            if (s.rank != null) {
                                Text(
                                    "${s.rank}${s.total?.let { "/$it" } ?: ""}",
                                    fontSize = 10.sp, color = c.secondary, modifier = Modifier.width(48.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                )
                            } else {
                                Box(Modifier.width(48.dp))
                            }
                            Icon(MiuixIcons.ChevronForward, null, Modifier.size(13.dp), tint = c.secondary.copy(alpha = 0.6f))
                        }
                        if (index < exam.subjects.lastIndex) Divider(startPadding = 16.dp)
                    }
                }
            }

            item {
                SectionLabel("题目与知识点", trailing = "可选", accent = c.amber)
                BoxCard {
                    Text(
                        "补充题号 得分与知识点后 诊断可定位到具体题型",
                        fontSize = 11.sp, color = c.secondary, lineHeight = 16.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(exam.subjects) { s ->
                            val has = s.questions.isNotEmpty()
                            SoftButton(
                                if (has) "${s.subject}  ${s.questions.size} 题" else "${s.subject} 添加题目",
                                { detailSubject = s.subject },
                                color = if (has) c.positive else c.ink,
                            )
                        }
                    }
                }
            }

            item {
                SectionLabel("个人最佳差距", accent = c.positive)
                BoxCard {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(ScoreEngine.fmtScore(combo.gapTotal), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text(" 分", fontSize = 11.sp, color = c.secondary, modifier = Modifier.padding(bottom = 5.dp))
                        Spacer(Modifier.weight(1f))
                        Text("最佳合计 ${ScoreEngine.fmtScore(combo.bestTotal)}", fontSize = 10.5.sp, color = c.secondary)
                    }
                    Spacer(Modifier.height(10.dp))
                    combo.rows.filter { it.gapPoints > 0.05 }.take(4).forEach { r ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(r.subject, fontSize = 12.sp, color = c.ink, modifier = Modifier.width(52.dp))
                            Text(r.bestTitle, fontSize = 10.sp, color = c.secondary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("+${ScoreEngine.fmtScore(r.gapPoints)}", fontSize = 11.5.sp, color = c.secondary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("跨考试拼接 不是一次真实成绩", fontSize = 9.5.sp, color = c.secondary.copy(alpha = 0.8f))
                }
            }

            if (images.isNotEmpty()) {
                item { SectionLabel("原始图片", trailing = "${images.size} 张", accent = c.violet) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(images) { path ->
                            Card(
                                modifier = Modifier.size(width = 132.dp, height = 176.dp),
                                cornerRadius = 18.dp,
                            ) {
                                AsyncImage(
                                    model = java.io.File(path),
                                    contentDescription = "成绩单图片",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(176.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionLabel("操作", accent = c.secondary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoftButton("复制模板", {
                        scope.launch {
                            val id = Store.duplicateExam(examId)
                            if (id > 0) com.aven.app.ui.AppState.say("已复制为新考试")
                        }
                    }, modifier = Modifier.weight(1f))
                    SoftButton("删除", { showDelete = true }, modifier = Modifier.weight(1f), color = c.negative)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showDelete) {
        OverlayDialog(
            show = true,
            title = "删除这场考试",
            summary = "科目成绩与题目记录一并删除",
            onDismissRequest = { showDelete = false },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton("取消", { showDelete = false }, modifier = Modifier.weight(1f))
                PrimaryButton("确认删除", {
                    scope.launch {
                        Store.deleteExam(examId)
                        showDelete = false
                        onDeleted()
                    }
                }, modifier = Modifier.weight(1f))
            }
        }
    }

    detailSubject?.let { subject ->
        QuestionSheet(exam, subject) { detailSubject = null }
    }
}

@Composable
private fun QuestionSheet(exam: ExamSnap, subject: String, onClose: () -> Unit) {
    val c = LocalAven.current
    val existing = remember(exam.id, subject) { exam.subject(subject)?.questions.orEmpty() }
    var items by remember { mutableStateOf(existing) }
    var number by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var full by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var knowledge by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    OverlayDialog(
        show = true,
        title = "$subject  题目记录",
        summary = "录入题目数据后可做知识点诊断",
        onDismissRequest = onClose,
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
            if (items.isNotEmpty()) {
                items.forEach { q ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${q.number}", fontSize = 12.sp, color = c.ink, modifier = Modifier.width(38.dp))
                        Text("${ScoreEngine.fmtScore(q.score)}/${ScoreEngine.fmtScore(q.full)}", fontSize = 11.5.sp, color = c.secondary, modifier = Modifier.width(64.dp))
                        Text(
                            listOf(q.type, q.knowledge).filter { it.isNotBlank() }.joinToString("  ").ifBlank { "未标注" },
                            fontSize = 10.5.sp, color = c.secondary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            MiuixIcons.Delete, "删除", Modifier.size(16.dp).clickable { items = items - q },
                            tint = c.secondary,
                        )
                    }
                }
                Divider()
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallField("题号", number, { number = it }, Modifier.width(74.dp))
                SmallField("得分", score, { score = it }, Modifier.weight(1f))
                SmallField("满分", full, { full = it }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallField("题型", type, { type = it }, Modifier.weight(1f))
                SmallField("知识点", knowledge, { knowledge = it }, Modifier.weight(1f))
            }
            if (error.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(error, fontSize = 10.5.sp, color = c.negative)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton("添加题目", {
                    val s = score.toDoubleOrNull()
                    val f = full.toDoubleOrNull()
                    if (s == null || f == null || s < 0 || f <= 0 || s > f) {
                        error = "得分需在 0 到满分之间"
                    } else {
                        items = items + QScore(
                            number = number.ifBlank { (items.size + 1).toString() },
                            score = s, full = f, type = type, knowledge = knowledge, wrong = s < f,
                        )
                        number = ""; score = ""; full = ""; type = ""; knowledge = ""; error = ""
                    }
                }, modifier = Modifier.weight(1f))
                PrimaryButton("保存", {
                    scope.launch {
                        Store.saveQuestions(exam.id, subject, items)
                        com.aven.app.ui.AppState.say("题目记录已保存")
                        onClose()
                    }
                }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SmallField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    LabeledInput(label = label, value = value, onChange = onChange, modifier = modifier)
}
