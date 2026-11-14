package com.aven.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aven.app.core.Dates
import com.aven.app.core.Prefs
import com.aven.app.core.Store
import com.aven.app.core.ai.Ai
import com.aven.app.core.ai.AiTasks
import com.aven.app.core.calc.ScoreEngine
import com.aven.app.core.calc.ScoreEngine.RangePreset
import com.aven.app.ui.AppState
import com.aven.app.ui.components.Badge
import com.aven.app.ui.components.BoxCard
import com.aven.app.ui.components.Divider
import com.aven.app.ui.components.EmptyState
import com.aven.app.ui.components.PrimaryButton
import com.aven.app.ui.components.RateBar
import com.aven.app.ui.components.ScreenTitle
import com.aven.app.ui.components.SectionLabel
import com.aven.app.ui.components.StatTile
import com.aven.app.ui.components.TrendChart
import com.aven.app.ui.theme.LocalAven
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Send

@Composable
fun SubjectDetailScreen(subject: String, onBack: () -> Unit, onOpenExam: (Long) -> Unit) {
    val c = LocalAven.current
    val exams by Store.exams.collectAsState()
    val series = remember(exams, subject) { ScoreEngine.subjectSeries(exams, subject) }
    val best = remember(exams, subject) { ScoreEngine.subjectBests(exams).firstOrNull { it.subject == subject } }
    var range by remember { mutableStateOf(RangePreset.ALL) }
    var showRate by remember { mutableStateOf(true) }
    val shown = remember(series, range) { ScoreEngine.filterSeries(series, range) }

    Column(Modifier.fillMaxWidth()) {
        ScreenTitle(subject, "${series.size} 次记录", onBack = onBack)
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 60.dp),
        ) {
            item {
                BoxCard {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("最近得分率", fontSize = 11.sp, color = c.secondary)
                            Text(
                                ScoreEngine.fmtRate(series.lastOrNull()?.rate ?: 0.0),
                                fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = c.ink,
                            )
                        }
                        if (best != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("最高 ${ScoreEngine.fmtRate(best.bestRate)}", fontSize = 11.sp, color = c.secondary)
                                Text("平均 ${ScoreEngine.fmtRate(best.avgRate)}", fontSize = 11.sp, color = c.secondary)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val subjectGoal by Store.goals.collectAsState()
                    val g = subjectGoal.firstOrNull { it.subject == subject }
                    if (g != null) {
                        val cur = series.lastOrNull()?.raw
                        Text(
                            "目标 ${ScoreEngine.fmtScore(g.targetScore)}  当前 ${cur?.let { ScoreEngine.fmtScore(it) } ?: "—"}",
                            fontSize = 10.5.sp, color = c.secondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        RateBar(ScoreEngine.goalProgress(cur, g.targetScore), c.positive, height = 5.dp)
                        Spacer(Modifier.height(10.dp))
                    }
                    val stat = remember(exams, subject) { ScoreEngine.subjectStats(exams).firstOrNull { it.subject == subject } }
                    Row {
                        StatTile("波动", String.format("%.1f", stat?.volatilityPp ?: 0.0), "pp")
                        Spacer(Modifier.width(22.dp))
                        StatTile("最近变化", String.format("%+.1f", stat?.deltaPp ?: 0.0), "pp")
                        Spacer(Modifier.weight(1f))
                        if (best != null) StatTile("最高原始分", ScoreEngine.fmtScore(best.bestRaw), "")
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(RangePreset.entries.toList()) { r ->
                            Chip(r.label, r == range) { range = r }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("得分率", showRate) { showRate = true }
                    Chip("原始分", !showRate) { showRate = false }
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
                BoxCard {
                    TrendChart(
                        points = shown,
                        value = { if (showRate) it.rate else it.raw },
                        color = c.ink,
                        onTap = { onOpenExam(it.examId) },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (showRate) "纵轴为得分率 可直接比较不同满分" else "纵轴为原始分 不同满分不可直接比较",
                        fontSize = 9.5.sp, color = c.secondary,
                    )
                }
            }
            item { SectionLabel("历次记录", trailing = "点击查看考试") }
            item {
                BoxCard(insidePadding = 0.dp) {
                    shown.reversed().forEachIndexed { i, p ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenExam(p.examId) }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.title, fontSize = 13.sp, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(Dates.fmt(p.date), fontSize = 10.sp, color = c.secondary)
                            }
                            Text(
                                "${ScoreEngine.fmtScore(p.raw)}/${ScoreEngine.fmtScore(p.full)}",
                                fontSize = 12.sp, color = c.ink,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(ScoreEngine.fmtRate(p.rate), fontSize = 10.5.sp, color = c.secondary)
                            Icon(MiuixIcons.ChevronForward, null, Modifier.size(14.dp), tint = c.secondary)
                        }
                        if (i < shown.lastIndex) Divider(startPadding = 16.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun AiChatScreen(onBack: () -> Unit) {
    val c = LocalAven.current
    val context = LocalContext.current
    val chats by Store.chats.collectAsState()
    val exams by Store.exams.collectAsState()
    val goals by Store.goals.collectAsState()
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chats.size) {
        if (chats.isNotEmpty()) {
            runCatching { listState.animateScrollToItem((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)) }
        }
    }

    fun digest(): String {
        val sb = StringBuilder()
        exams.sortedByDescending { it.date }.take(5).forEach { e ->
            sb.appendLine("${Dates.fmt(e.date)} ${e.title}：总分 ${ScoreEngine.fmtScore(e.rawTotal)}/${ScoreEngine.fmtScore(e.fullTotal)}")
            e.subjects.forEach { s -> sb.appendLine("  - ${s.subject} ${ScoreEngine.fmtScore(s.raw)}/${ScoreEngine.fmtScore(s.full)}") }
        }
        goals.take(2).forEach { g -> sb.appendLine("目标：${g.subject.ifBlank { "总分" }} ${ScoreEngine.fmtScore(g.targetScore)}") }
        return sb.toString()
    }

    fun send() {
        val text = input.trim()
        if (text.isBlank() || busy) return
        if (!Ai.hasKey(context)) { error = "请先在设置中填写 DeepSeek API Key"; return }
        if (!Prefs.aiConsent.value) { error = "请先在 AI 分析页同意发送学业数据"; return }
        error = ""
        input = ""
        busy = true
        scope.launch {
            Store.addChat("user", text)
            val profile = "学段：${if (Prefs.stage.value == 0) "初中" else "高中"}，年级：${Prefs.grade.value}"
            val msgs = mutableListOf("system" to AiTasks.chatSystem(profile, digest()))
            chats.takeLast(8).forEach { msgs += it.role to it.content }
            msgs += "user" to text
            when (val r = Ai.chat(context, msgs, maxTokens = 1200)) {
                is Ai.Out.Ok -> Store.addChat("assistant", cleanReply(r.text))
                is Ai.Out.Err -> error = r.message
            }
            busy = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("和 Aven 聊聊", "只带最近 5 场考试摘要", onBack = onBack) {
            if (chats.isNotEmpty()) {
                Text("清空", fontSize = 12.sp, color = c.secondary, modifier = Modifier.clickable {
                    scope.launch { Store.clearChat() }
                }.padding(8.dp))
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (chats.isEmpty()) {
                item {
                    EmptyState(
                        MiuixIcons.Send, "问点具体的",
                        "问点具体的 例如数学为什么波动",
                    )
                }
            }
            items(chats) { m ->
                val mine = m.role == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.86f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp, topEnd = 18.dp,
                                    bottomStart = if (mine) 18.dp else 6.dp,
                                    bottomEnd = if (mine) 6.dp else 18.dp,
                                )
                            )
                            .background(if (mine) c.ink else c.card)
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Text(
                            m.content,
                            fontSize = 13.sp, lineHeight = 20.sp,
                            color = if (mine) c.card else c.ink,
                        )
                    }
                }
            }
            if (busy) {
                item {
                    Row(Modifier.fillMaxWidth()) {
                        Box(Modifier.clip(RoundedCornerShape(18.dp)).background(c.card).padding(14.dp)) {
                            Text("思考中", fontSize = 12.sp, color = c.secondary)
                        }
                    }
                }
            }
            if (error.isNotBlank()) {
                item {
                    Text(error, fontSize = 11.5.sp, color = c.negative, lineHeight = 17.sp)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().imePadding().navigationBarsPadding()
                .background(c.card)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(Modifier.weight(1f)) {
                TextField(
                    value = TextFieldValue(input, TextRange(input.length)),
                    onValueChange = { input = it.text },
                    modifier = Modifier.fillMaxWidth(),
                    label = "",
                    singleLine = false,
                    insideMargin = androidx.compose.ui.unit.DpSize(14.dp, 10.dp),
                    cornerRadius = 20.dp,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.5.sp),
                    colors = top.yukonga.miuix.kmp.basic.TextFieldDefaults.textFieldColors(
                        backgroundColor = c.neutralBg,
                        labelColor = c.secondary,
                        borderColor = c.neutralBg,
                    ),
                )
                if (input.isBlank()) {
                    Text(
                        "发消息",
                        fontSize = 12.5.sp,
                        color = c.secondary.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 15.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(46.dp).clip(CircleShape)
                    .background(if (busy || input.isBlank()) c.neutralBg else c.blue)
                    .clickable(enabled = !busy) { send() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    MiuixIcons.Send, "发送", Modifier.size(19.dp),
                    tint = if (busy || input.isBlank()) c.secondary else androidx.compose.ui.graphics.Color.White,
                )
            }
        }
    }
}

private fun cleanReply(raw: String): String {
    var t = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    if (t.startsWith("{")) {
        val obj = runCatching { org.json.JSONObject(t) }.getOrNull()
        if (obj != null) {
            listOf("回复", "reply", "content", "answer", "message", "text", "result").forEach { key ->
                if (obj.has(key) && obj.optString(key).isNotBlank()) t = obj.optString(key)
            }
        }
    }
    t = t.removeSurrounding("\"").replace("\\n", "\n")
    if (t.startsWith("{") && t.endsWith("}")) t = t.substring(1, t.length - 1)
    return t.trim()
}
