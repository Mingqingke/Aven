package com.aven.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
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
import com.aven.app.BuildConfig
import com.aven.app.core.Backup
import com.aven.app.core.Dates
import com.aven.app.core.KeyVault
import com.aven.app.core.Prefs
import com.aven.app.core.Store
import com.aven.app.core.ai.Ai
import com.aven.app.ui.AppState
import com.aven.app.ui.components.BoxCard
import com.aven.app.ui.components.Divider
import com.aven.app.ui.components.EntryRow
import com.aven.app.ui.components.LabeledInput
import com.aven.app.ui.components.PrimaryButton
import com.aven.app.ui.components.ScreenTitle
import com.aven.app.ui.components.SectionLabel
import com.aven.app.ui.components.SoftButton
import com.aven.app.ui.components.ToggleRow
import com.aven.app.ui.components.ValueRow
import com.aven.app.ui.theme.LocalAven
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.MindMap
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.overlay.OverlayDialog

val CARD_OPTIONS = listOf(
    "latest" to "最近一次考试",
    "countdown" to "下一次考试倒计时",
    "gaokao" to "高考倒计时",
    "best" to "历史最高总分",
    "gap" to "个人最佳差距",
    "weak" to "需要关注的学科",
    "target" to "距离目标分数",
    "tasks" to "今日任务完成情况",
    "trend" to "成绩趋势",
)

fun cardLabel(key: String): String = CARD_OPTIONS.firstOrNull { it.first == key }?.second ?: "最近一次考试"

@Composable
private fun SettingsPage(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        ScreenTitle(title, subtitle, onBack = onBack)
        content()
    }
}

@Composable
private fun Page(block: @Composable () -> Unit) {
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
    ) { item { block() } }
}

@Composable
fun SettingsHomeScreen(onOpen: (com.aven.app.ui.Route) -> Unit, onBack: (() -> Unit)? = null) {
    val c = LocalAven.current
    val nickname by Prefs.nickname.collectAsState()
    val grade by Prefs.grade.collectAsState()
    SettingsPage("设置", "Aven ${BuildConfig.VERSION_NAME}", onBack) {
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        ) {
            item {
                BoxCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(50.dp).clip(RoundedCornerShape(18.dp)).background(c.ink),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(nickname.take(1).ifBlank { "A" }, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = c.card)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(nickname, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                            Text(grade + "  " + (if (Prefs.stage.value == 0) "初中" else "高中"), fontSize = 11.sp, color = c.secondary)
                        }
                        Text("编辑", fontSize = 12.sp, color = c.blue, modifier = Modifier.clickable { onOpen(com.aven.app.ui.Route.SettingsProfile) })
                    }
                }
            }
            item { SectionLabel("档案与考试") }
            item {
                BoxCard(insidePadding = 0.dp) {
                    EntryRow(MiuixIcons.Store, "个人与学业档案", "昵称 学段 年级 地区 选科", c.positive, { onOpen(com.aven.app.ui.Route.SettingsProfile) })
                    Divider(startPadding = 64.dp)
                    EntryRow(MiuixIcons.Tasks, "考试设置", "类型 排名 比较规则 波动区间", c.blue, { onOpen(com.aven.app.ui.Route.SettingsExam) })
                }
            }
            item { SectionLabel("智能与数据") }
            item {
                BoxCard(insidePadding = 0.dp) {
                    EntryRow(MiuixIcons.MindMap, "AI 配置", "DeepSeek Key 模型 连通测试", c.violet, { onOpen(com.aven.app.ui.Route.SettingsAi) })
                    Divider(startPadding = 64.dp)
                    EntryRow(MiuixIcons.Folder, "数据管理", "备份 恢复 导出 清理", c.amber, { onOpen(com.aven.app.ui.Route.SettingsData) })
                    Divider(startPadding = 64.dp)
                    EntryRow(MiuixIcons.Lock, "隐私与安全", "密钥 授权 删除", c.negative, { onOpen(com.aven.app.ui.Route.SettingsPrivacy) })
                }
            }
            item { SectionLabel("外观") }
            item {
                BoxCard(insidePadding = 0.dp) {
                    EntryRow(MiuixIcons.Theme, "外观与卡片", "主题 动效 首页卡片", c.blue, { onOpen(com.aven.app.ui.Route.SettingsAppearance) })
                    Divider(startPadding = 64.dp)
                    EntryRow(MiuixIcons.Update, "更新与版本", "检查更新 更新源 签名", c.blue, { onOpen(com.aven.app.ui.Route.SettingsUpdate) })
                    Divider(startPadding = 64.dp)
                    EntryRow(MiuixIcons.Info, "关于 Aven", "版本 作者 开源组件", c.violet, { onOpen(com.aven.app.ui.Route.About) })
                }
            }
            item {
                Spacer(Modifier.height(18.dp))
                Text(
                    "全部数据保存在本机 卸载前请备份",
                    fontSize = 10.sp, color = c.secondary, modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

@Composable
fun ProfileSettingsScreen(onBack: () -> Unit) {
    val c = LocalAven.current
    val nickname by Prefs.nickname.collectAsState()
    val stage by Prefs.stage.collectAsState()
    val grade by Prefs.grade.collectAsState()
    val term by Prefs.term.collectAsState()
    val province by Prefs.province.collectAsState()
    val city by Prefs.city.collectAsState()
    val school by Prefs.school.collectAsState()
    val electives by Prefs.electives.collectAsState()

    SettingsPage("个人与学业档案", "用于换算与建议 不做上传", onBack) {
        Page {
            BoxCard {
                Column {
                    Text("昵称", fontSize = 11.sp, color = c.secondary)
                    Spacer(Modifier.height(4.dp))
                    Field(nickname, { Prefs.setNickname(it.take(16)) }, "你的名字")
                }
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                Text("学段", fontSize = 11.sp, color = c.secondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("初中", stage == 0) {
                        Prefs.setStage(0)
                        if (grade !in listOf("初一", "初二", "初三")) Prefs.setGrade("初三")
                    }
                    Chip("高中", stage == 1) {
                        Prefs.setStage(1)
                        if (grade !in listOf("高一", "高二", "高三")) Prefs.setGrade("高二")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("年级", fontSize = 11.sp, color = c.secondary)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(if (stage == 0) listOf("初一", "初二", "初三") else listOf("高一", "高二", "高三")) { g ->
                        Chip(g, grade == g) { Prefs.setGrade(g) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("学期", fontSize = 11.sp, color = c.secondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("上学期", "下学期").forEach { t -> Chip(t, term == t) { Prefs.setTerm(t) } }
                }
            }

            SectionLabel("地区与学校 可选")
            BoxCard {
                Field(province, { Prefs.setProvince(it) }, "省份 例如 陕西省")
                Spacer(Modifier.height(8.dp))
                Field(city, { Prefs.setCity(it) }, "城市 例如 汉中市")
                Spacer(Modifier.height(8.dp))
                Field(school, { Prefs.setSchool(it) }, "学校 仅保存在本机")
                Spacer(Modifier.height(8.dp))
                Text("赋分按你的记录 不套用固定公式", fontSize = 9.5.sp, color = c.secondary)
            }

            SectionLabel("选科")
            BoxCard {
                val selected = electives.split(",").filter { it.isNotBlank() }
                Text("已选 ${selected.joinToString(" ").ifBlank { "未设置" }}", fontSize = 11.sp, color = c.secondary)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("物理", "历史", "化学", "生物", "政治", "地理")) { s ->
                        Chip(s, s in selected) {
                            val next = if (s in selected) selected - s else selected + s
                            Prefs.setElectives(next.joinToString(","))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("通常选考 3 门", fontSize = 9.5.sp, color = c.secondary)
            }
        }
    }
}

@Composable
fun ExamSettingsScreen(onBack: () -> Unit) {
    val c = LocalAven.current
    val type by Prefs.defaultExamType.collectAsState()
    val rank by Prefs.rankReference.collectAsState()
    val rule by Prefs.compareRule.collectAsState()
    val band by Prefs.compareBand.collectAsState()
    val converted by Prefs.convertedEnabled.collectAsState()

    SettingsPage("考试设置", "决定比较口径", onBack) {
        Page {
            BoxCard {
                Text("默认考试类型", fontSize = 11.sp, color = c.secondary)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("月考", "期中", "期末", "模考", "联考", "单元测")) { t ->
                        Chip(t, type == t) { Prefs.setDefaultExamType(t) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("排名参考", fontSize = 11.sp, color = c.secondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("年级", "班级", "全省").forEachIndexed { i, t ->
                        Chip(t, rank == i) { Prefs.setRankReference(i) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("比较规则", fontSize = 11.sp, color = c.secondary)
                Spacer(Modifier.height(8.dp))
                listOf("优先同类考试", "始终比较最近两次", "综合排序选择").forEachIndexed { i, t ->
                    Row(Modifier.fillMaxWidth().clickable { Prefs.setCompareRule(i) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(18.dp).clip(CircleShape)
                                .background(if (rule == i) c.ink else c.neutralBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (rule == i) Box(Modifier.size(6.dp).clip(CircleShape).background(c.card))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(t, fontSize = 13.sp, color = c.ink)
                    }
                }
            }

            SectionLabel("中性波动区间")
            BoxCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("± ${String.format("%.1f", band)} 个百分点", fontSize = 14.sp, color = c.ink, modifier = Modifier.weight(1f))
                    Text("变化在此区间内视为基本持平", fontSize = 9.5.sp, color = c.secondary)
                }
                Slider(
                    value = band,
                    onValueChange = { Prefs.setCompareBand(it) },
                    valueRange = 0f..5f,
                    steps = 9,
                )
                Text("变化小于该区间视为持平", fontSize = 9.5.sp, color = c.secondary)
            }

            SectionLabel("计分")
            BoxCard {
                ToggleRow("记录赋分成绩", "选考科目可同时保存原始分与赋分", converted) { Prefs.setConvertedEnabled(it) }
                Divider()
                ValueRow("赋分规则", "按成绩单录入")
                Divider()
                Text(
                    "赋分按录入值展示",
                    fontSize = 9.5.sp, color = c.secondary,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
fun AiSettingsScreen(onBack: () -> Unit) {
    val c = LocalAven.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val base by Prefs.apiBase.collectAsState()
    val model by Prefs.aiModel.collectAsState()
    val consent by Prefs.aiConsent.collectAsState()
    var keyInput by remember { mutableStateOf("") }
    var masked by remember { mutableStateOf(KeyVault.masked(context)) }
    var testResult by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }

    SettingsPage("AI 配置", "使用你自己的 DeepSeek API Key", onBack) {
        Page {
            BoxCard {
                Text("API Key", fontSize = 11.sp, color = c.secondary)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (masked.isBlank()) "尚未保存" else "已保存 $masked",
                    fontSize = 13.sp, color = if (masked.isBlank()) c.secondary else c.ink,
                )
                Spacer(Modifier.height(10.dp))
                Field(keyInput, { keyInput = it }, "粘贴新的 Key")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("保存密钥", {
                        if (keyInput.isBlank()) AppState.say("请输入 Key") else {
                            val ok = KeyVault.save(context, keyInput.trim())
                            keyInput = ""
                            masked = KeyVault.masked(context)
                            AppState.say(if (ok) "密钥已加密保存" else "保存失败 系统密钥库不可用")
                        }
                    }, modifier = Modifier.weight(1f))
                    SoftButton("删除", {
                        KeyVault.clear(context); masked = ""; AppState.say("密钥已删除")
                    }, modifier = Modifier.weight(1f), color = c.negative)
                }
                Spacer(Modifier.height(8.dp))
                Text("密钥加密保存在本机", fontSize = 9.5.sp, color = c.secondary)
            }

            SectionLabel("接口与模型")
            BoxCard {
                Text("API 地址", fontSize = 11.sp, color = c.secondary)
                Spacer(Modifier.height(6.dp))
                Field(base, { Prefs.setApiBase(it) }, "https://api.deepseek.com")
                Spacer(Modifier.height(12.dp))
                Text("模型", fontSize = 11.sp, color = c.secondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("deepseek-flash", model == "deepseek-flash") { Prefs.setAiModel("deepseek-flash") }
                    Chip("deepseek-v4-pro", model == "deepseek-v4-pro") { Prefs.setAiModel("deepseek-v4-pro") }
                }
                Spacer(Modifier.height(8.dp))
                Text("flash 支持图片 v4-pro 仅文本", fontSize = 9.5.sp, color = c.secondary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoftButton(if (testing) "测试中" else "测试连接", {
                        if (testing) return@SoftButton
                        scope.launch {
                            testing = true
                            testResult = ""
                            testResult = when (val r = Ai.listModels(context)) {
                                is Ai.Out.Ok -> "连接成功 ${r.text}"
                                is Ai.Out.Err -> "连接失败 ${r.message}"
                            }
                            testing = false
                        }
                    }, modifier = Modifier.weight(1f))
                }
                if (testResult.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(testResult, fontSize = 10.5.sp, color = if (testResult.startsWith("连接成功")) c.positive else c.negative, lineHeight = 15.sp)
                }
            }

            SectionLabel("数据授权")
            BoxCard {
                ToggleRow("允许发送学业摘要", "分数 得分率 目标与任务数", consent) { Prefs.setAiConsent(it) }
                Divider()
                Text(
                    "默认关闭 只发送上述摘要",
                    fontSize = 9.5.sp, color = c.secondary,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
fun DataSettingsScreen(onBack: () -> Unit) {
    val c = LocalAven.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var imageBytes by remember { mutableStateOf(0L) }
    var confirmClearImages by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { imageBytes = Store.totalImageBytes() }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            Backup.exportJson(context, uri)
                .onSuccess { message = "已导出 ${it.exams} 场考试 ${it.subjects} 条记录" }
                .onFailure { message = "导出失败 ${it.message}" }
            busy = false
        }
    }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            Backup.importJson(context, uri)
                .onSuccess { message = "已导入 ${it.exams} 场考试 原有数据未改动" }
                .onFailure { message = "导入失败 ${it.message}" }
            busy = false
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            Backup.exportCsv(context, uri)
                .onSuccess { message = "已导出 $it 行成绩" }
                .onFailure { message = "导出失败 ${it.message}" }
            busy = false
        }
    }

    SettingsPage("数据管理", "所有数据默认只在本机", onBack) {
        Page {
            BoxCard(insidePadding = 0.dp) {
                EntryRow(MiuixIcons.Folder, "导出完整备份", "JSON 含成绩 题目 目标与任务", c.ink, {
                    exportJson.launch("aven-backup-${Dates.fmt(System.currentTimeMillis()).replace(".", "")}.json")
                })
                Divider(startPadding = 64.dp)
                EntryRow(MiuixIcons.Tasks, "从备份恢复", "只追加 不覆盖", c.ink, {
                    importJson.launch(arrayOf("application/json", "*/*"))
                })
                Divider(startPadding = 64.dp)
                EntryRow(MiuixIcons.Store, "导出成绩 CSV", "可用 Excel 打开", c.ink, {
                    exportCsv.launch("aven-scores-${Dates.fmt(System.currentTimeMillis()).replace(".", "")}.csv")
                })
            }
            if (message.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                BoxCard(container = c.cardSoft) { Text(message, fontSize = 11.5.sp, color = c.ink, lineHeight = 17.sp) }
            }
            if (busy) Text("处理中", fontSize = 11.sp, color = c.secondary, modifier = Modifier.padding(8.dp))

            SectionLabel("原始图片")
            BoxCard {
                ValueRow("已占用", String.format("%.1f MB", imageBytes / 1024.0 / 1024.0))
                Divider()
                Row(Modifier.fillMaxWidth().clickable { confirmClearImages = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("清理全部成绩单图片", fontSize = 14.sp, color = c.negative, modifier = Modifier.weight(1f))
                }
                Text(
                    "只删图片 成绩保留",
                    fontSize = 9.5.sp, color = c.secondary, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp),
                )
            }

            SectionLabel("危险操作")
            BoxCard {
                Row(Modifier.fillMaxWidth().clickable { confirmClearAll = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("清空全部数据", fontSize = 14.sp, color = c.negative, modifier = Modifier.weight(1f))
                    Text("不可恢复", fontSize = 10.5.sp, color = c.secondary)
                }
            }
        }
    }

    if (confirmClearImages) {
        OverlayDialog(show = true, title = "清理成绩单图片", summary = "成绩记录保留", onDismissRequest = { confirmClearImages = false }) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton("取消", { confirmClearImages = false }, modifier = Modifier.weight(1f))
                PrimaryButton("清理", {
                    scope.launch {
                        Store.clearImages()
                        imageBytes = Store.totalImageBytes()
                        confirmClearImages = false
                        message = "图片已清理"
                    }
                }, modifier = Modifier.weight(1f))
            }
        }
    }

    if (confirmClearAll) {
        OverlayDialog(
            show = true, title = "清空全部数据",
            summary = "全部删除 无法恢复",
            onDismissRequest = { confirmClearAll = false },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton("取消", { confirmClearAll = false }, modifier = Modifier.weight(1f))
                PrimaryButton("确认清空", {
                    scope.launch {
                        Store.clearAll()
                        imageBytes = 0
                        confirmClearAll = false
                        message = "全部数据已清空"
                        AppState.say("数据已清空")
                    }
                }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AppearanceSettingsScreen(onBack: () -> Unit) {
    val c = LocalAven.current
    val mode by Prefs.themeMode.collectAsState()
    val reduce by Prefs.reduceMotion.collectAsState()
    val cardA by Prefs.cardA.collectAsState()
    val cardB by Prefs.cardB.collectAsState()
    var editCard by remember { mutableStateOf<String?>(null) }

    SettingsPage("外观与卡片", "浅色 深色 首页配置", onBack) {
        Page {
            BoxCard {
                Text("主题", fontSize = 11.sp, color = c.secondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("跟随系统", "浅色", "深色").forEachIndexed { i, t ->
                        Chip(t, mode == i) { Prefs.setThemeMode(i) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Divider()
                ToggleRow("减少动态效果", "关闭卡片形变与部分过渡", reduce) { Prefs.setReduceMotion(it) }
            }

            SectionLabel("首页右侧卡片")
            BoxCard(insidePadding = 0.dp) {
                EntryRow(MiuixIcons.Settings, "上方卡片", cardLabel(cardA), c.ink, { editCard = "A" })
                Divider(startPadding = 64.dp)
                EntryRow(MiuixIcons.Settings, "下方卡片", cardLabel(cardB), c.ink, { editCard = "B" })
                Divider(startPadding = 64.dp)
                Row(Modifier.fillMaxWidth().clickable {
                    Prefs.setCardA(cardB); Prefs.setCardB(cardA)
                }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("交换位置", fontSize = 13.5.sp, color = c.blue)
                }
            }

        }
    }

    editCard?.let { slot ->
        val current = if (slot == "A") cardA else cardB
        OverlayDialog(show = true, title = "选择${if (slot == "A") "上方" else "下方"}卡片", onDismissRequest = { editCard = null }) {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                CARD_OPTIONS.forEach { (key, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (slot == "A") Prefs.setCardA(key) else Prefs.setCardB(key)
                            editCard = null
                        }.padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(18.dp).clip(CircleShape).background(if (current == key) c.ink else c.neutralBg),
                            contentAlignment = Alignment.Center,
                        ) { if (current == key) Box(Modifier.size(6.dp).clip(CircleShape).background(c.card)) }
                        Spacer(Modifier.width(10.dp))
                        Text(label, fontSize = 13.sp, color = c.ink)
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacySettingsScreen(onBack: () -> Unit) {
    val c = LocalAven.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val consent by Prefs.aiConsent.collectAsState()
    val vision by Prefs.visionConsent.collectAsState()
    var showPolicy by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    SettingsPage("隐私与安全", "本地优先 按需授权", onBack) {
        Page {
            BoxCard {
                ToggleRow("允许发送学业摘要给 AI", "分数 得分率 目标与任务", consent) { Prefs.setAiConsent(it) }
                Divider()
                ToggleRow("允许上传成绩单图片", "仅在手动点击云端识别时发生", vision) { Prefs.setVisionConsent(it) }
                Divider()
                Row(Modifier.fillMaxWidth().clickable {
                    KeyVault.clear(context); AppState.say("API 密钥已删除")
                }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("删除已保存的 API Key", fontSize = 14.sp, color = c.ink, modifier = Modifier.weight(1f))
                }
            }

            SectionLabel("数据")
            BoxCard(insidePadding = 0.dp) {
                EntryRow(MiuixIcons.Lock, "隐私说明", "数据流向与保存位置", c.ink, { showPolicy = true })
                Divider(startPadding = 64.dp)
                EntryRow(MiuixIcons.Delete, "删除全部数据", "成绩 图片 报告与密钥", c.negative, { showDelete = true })
            }

            SectionLabel("面向未成年人")
            BoxCard {
                Text(
                    "面向中学生 默认不联网 不上传成绩 可随时关闭授权",
                    fontSize = 10.5.sp, color = c.secondary, lineHeight = 16.sp,
                )
            }
        }
    }

    if (showPolicy) {
        OverlayDialog(show = true, title = "隐私说明", onDismissRequest = { showPolicy = false }) {
            Column {
                Text(
                    "1 成绩与图片保存在本机\n" +
                        "2 本地识别使用离线模型\n" +
                        "3 仅在你主动调用时通过 HTTPS 发送必要内容\n" +
                        "4 API Key 经系统 Keystore 加密\n" +
                        "5 卸载会删除本地数据 请先导出备份",
                    fontSize = 11.5.sp, color = c.ink, lineHeight = 18.sp,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton("知道了", { showPolicy = false }, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showDelete) {
        OverlayDialog(show = true, title = "删除全部数据", summary = "包含成绩 图片 报告与密钥", onDismissRequest = { showDelete = false }) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton("取消", { showDelete = false }, modifier = Modifier.weight(1f))
                PrimaryButton("确认删除", {
                    scope.launch {
                        Store.clearAll()
                        KeyVault.clear(context)
                        showDelete = false
                        AppState.say("全部数据已删除")
                    }
                }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, placeholder: String) {
    LabeledInput(label = placeholder, value = value, onChange = onChange, modifier = Modifier.fillMaxWidth())
}
