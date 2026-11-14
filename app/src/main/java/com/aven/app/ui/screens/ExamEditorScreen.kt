package com.aven.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.aven.app.BuildConfig
import com.aven.app.core.Dates
import com.aven.app.core.Prefs
import com.aven.app.core.Store
import com.aven.app.core.SubjectDraft
import com.aven.app.core.ExamDraft
import com.aven.app.core.ai.Ai
import com.aven.app.core.ai.AiTasks
import com.aven.app.core.ocr.Ocr
import com.aven.app.core.ocr.SheetParser
import com.aven.app.ui.AppState
import com.aven.app.ui.components.Badge
import com.aven.app.ui.components.BoxCard
import com.aven.app.ui.components.Divider
import com.aven.app.ui.components.LabeledInput
import com.aven.app.ui.components.PrimaryButton
import com.aven.app.ui.components.ScreenTitle
import com.aven.app.ui.components.SectionLabel
import com.aven.app.ui.components.SoftButton
import com.aven.app.ui.theme.LocalAven
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import java.io.File
import kotlin.math.roundToInt

private data class EditSubject(
    val subject: String = "",
    val score: String = "",
    val full: String = "",
    val converted: String = "",
    val rank: String = "",
    val total: String = "",
    val confident: Boolean = true,
)

@Composable
fun ExamEditorScreen(
    examId: Long,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val c = LocalAven.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exams by Store.exams.collectAsState()
    val stage by Prefs.stage.collectAsState()
    val defaultType by Prefs.defaultExamType.collectAsState()
    val electivesPref by Prefs.electives.collectAsState()

    var title by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(Dates.fmt(System.currentTimeMillis())) }
    var type by remember { mutableStateOf(defaultType) }
    var note by remember { mutableStateOf("") }
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var subjects = remember { mutableListOf<EditSubject>().toMutableStateList() }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showVisionConsent by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(examId == 0L) }
    var extraFor by remember { mutableStateOf(-1) }

    LaunchedEffect(examId) {
        if (examId != 0L) {
            val e = Store.exams.value.firstOrNull { it.id == examId }
            if (e != null) {
                title = e.title
                dateText = Dates.fmt(e.date)
                type = e.type
                note = ""
                images = Store.examImages(examId)
                subjects.clear()
                subjects.addAll(
                    e.subjects.map {
                        EditSubject(
                            subject = it.subject,
                            score = ScoreText.of(it.raw),
                            full = ScoreText.of(it.full),
                            converted = it.converted?.let { v -> ScoreText.of(v) } ?: "",
                            rank = it.rank?.toString() ?: "",
                            total = it.total?.toString() ?: "",
                            confident = true,
                        )
                    }
                )
            }
            loaded = true
        } else if (subjects.isEmpty()) {
            subjects.addAll(defaultSubjectRows())
        }
    }

    fun applyParsed(parsed: com.aven.app.core.ocr.ParsedSheet) {
        if (!parsed.title.isNullOrBlank() && title.isBlank()) title = parsed.title
        parsed.dateMillis?.let { dateText = Dates.fmt(it) }
        if (parsed.subjects.isNotEmpty()) {
            subjects.clear()
            subjects.addAll(
                parsed.subjects.map {
                    EditSubject(
                        subject = it.subject,
                        score = it.score?.let { v -> ScoreText.of(v) } ?: "",
                        full = it.full?.let { v -> ScoreText.of(v) } ?: SheetParser.defaultFull(it.subject).let { f -> ScoreText.of(f) },
                        confident = it.confident,
                    )
                }
            )
            status = "识别 ${parsed.subjects.size} 科 请核对后保存"
        } else {
            status = "未识别到分数 可手动补充"
        }
    }

    suspend fun runOcr(paths: List<String>) {
        busy = true
        status = "本地识别中"
        val (text, ok) = Ocr.recognizeMany(context, paths)
        if (ok == 0) {
            status = "图片读取失败 请重新选择"
            busy = false
            return
        }
        val parsed = SheetParser.parse(text)
        applyParsed(parsed)
        busy = false
    }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                busy = true
                val saved = uris.mapNotNull { Store.importImage(it) }
                images = images + saved
                busy = false
                if (saved.isNotEmpty()) runOcr(saved)
            }
        }
    }

    var cameraTarget by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraTarget
        if (ok && uri != null) {
            scope.launch {
                val path = Store.importImage(uri)
                if (path != null) {
                    images = images + path
                    runOcr(listOf(path))
                }
            }
        }
    }

    fun launchCamera() {
        runCatching {
            val dir = File(context.cacheDir, "capture").apply { mkdirs() }
            val file = File(dir, "cap_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
            cameraTarget = uri
            takePicture.launch(uri)
        }.onFailure { AppState.say("无法启动相机 ${it.message}") }
    }

    fun save() {
        error = ""
        val millis = parseDate(dateText)
        if (millis == null) { error = "日期格式应为 2026.11.05"; return }
        if (millis > Dates.today() + 86_400_000L - 1) { error = "日期晚于今天 请选择已完成的考试"; return }
        if (title.isBlank()) { error = "请填写考试名称"; return }
        val drafts = mutableListOf<SubjectDraft>()
        for (s in subjects) {
            if (s.subject.isBlank()) continue
            val raw = s.score.toDoubleOrNull()
            val full = s.full.toDoubleOrNull()
            if (raw == null || full == null || full <= 0 || raw < 0 || raw > full) {
                error = "${s.subject} 的分数或满分无效"; return
            }
            drafts += SubjectDraft(
                subject = s.subject.trim(),
                raw = raw,
                full = full,
                converted = s.converted.toDoubleOrNull(),
                rank = s.rank.toIntOrNull(),
                total = s.total.toIntOrNull(),
            )
        }
        if (drafts.isEmpty()) { error = "至少填写一科成绩"; return }
        scope.launch {
            busy = true
            val id = Store.saveExam(
                ExamDraft(
                    id = examId,
                    title = title.trim(),
                    date = millis,
                    type = type,
                    stage = if (stage == 0) "初中" else "高中",
                    note = note,
                    images = images,
                    source = if (images.isEmpty()) "manual" else "ocr",
                    subjects = drafts,
                )
            )
            busy = false
            AppState.say("已保存 ${drafts.size} 科成绩")
            onSaved(id)
        }
    }

    fun runVision() {
        if (images.isEmpty()) {
            AppState.say("请先拍照或选择图片")
            return
        }
        if (!Prefs.visionConsent.value) {
            showVisionConsent = true
            return
        }
        scope.launch {
            busy = true
            val merged = linkedMapOf<String, EditSubject>()
            var lastError = ""
            var titleFrom: String? = null
            var dateFrom: String? = null
            var conflicts = 0
            val source = images.take(5)
            for ((index, path) in source.withIndex()) {
                status = "识别第 ${index + 1}/${source.size} 张"
                when (val r = Ai.vision(context, listOf(path), AiTasks.visionPrompt())) {
                    is Ai.Out.Ok -> {
                        val sheet = AiTasks.parseVision(r.text)
                        if (sheet == null) {
                            lastError = "第 ${index + 1} 张内容无法解析"
                            continue
                        }
                        if (sheet.subjects.isEmpty()) {
                            lastError = "第 ${index + 1} 张未识别到科目"
                            continue
                        }
                        titleFrom = titleFrom ?: sheet.title
                        dateFrom = dateFrom ?: sheet.date
                        for (v in sheet.subjects) {
                            val name = v.name.take(12)
                            if (name.isBlank()) continue
                            val full = v.full ?: SheetParser.defaultFull(name)
                            val existing = merged[name]
                            if (existing != null && existing.score.isNotBlank() && v.score != null &&
                                existing.score.toDoubleOrNull() != v.score
                            ) conflicts += 1
                            merged[name] = EditSubject(
                                subject = name,
                                score = existing?.score?.takeIf { it.isNotBlank() }
                                    ?: v.score?.let { ScoreText.of(it) } ?: "",
                                full = existing?.full?.takeIf { it.isNotBlank() && it != "0" }
                                    ?: ScoreText.of(full),
                                converted = existing?.converted?.takeIf { it.isNotBlank() }
                                    ?: v.converted?.let { ScoreText.of(it) } ?: "",
                                rank = existing?.rank?.takeIf { it.isNotBlank() }
                                    ?: v.rank?.toString() ?: "",
                                total = existing?.total?.takeIf { it.isNotBlank() }
                                    ?: v.total?.toString() ?: "",
                                confident = v.score != null && v.full != null,
                            )
                        }
                    }
                    is Ai.Out.Err -> lastError = r.message
                }
            }
            if (merged.isEmpty()) {
                status = lastError.ifBlank { "未识别到内容 可改用手动录入" }
            } else {
                subjects.clear()
                subjects.addAll(merged.values)
                titleFrom?.let { if (title.isBlank()) title = it }
                dateFrom?.let { d ->
                    runCatching {
                        val parts = d.split("-")
                        if (parts.size >= 3) {
                            dateText = Dates.fmt(Dates.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt()))
                        }
                    }
                }
                status = "已填充 ${merged.size} 科 请核对" + if (conflicts > 0) " 有重复科目已保留第一张" else ""
            }
            busy = false
        }
    }

    Column(Modifier.fillMaxWidth()) {
        ScreenTitle(if (examId == 0L) "添加考试" else "编辑考试", "识别结果需要你确认后才保存", onBack = onBack) {
            if (busy) {
                Text("处理中", fontSize = 11.sp, color = c.secondary)
            } else {
                SoftButton("保存", { save() })
            }
        }
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
        ) {
            item {
                BoxCard(container = c.cardSoft) {
                    Text("成绩单识别", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.ink)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "本地识别不上传图片 仅当你点击云端识别时才发送",
                        fontSize = 10.5.sp, color = c.secondary, lineHeight = 15.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SoftButton("拍照", { launchCamera() }, modifier = Modifier.weight(1f))
                        SoftButton("相册", {
                            pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }, modifier = Modifier.weight(1f))
                        SoftButton("云端识别", { runVision() }, modifier = Modifier.weight(1f))
                    }
                    if (images.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(images) { path ->
                                Box {
                                    AsyncImage(
                                        model = File(path),
                                        contentDescription = "已选图片",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(84.dp).clip(RoundedCornerShape(14.dp)),
                                    )
                                    Box(
                                        Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape)
                                            .background(c.ink.copy(alpha = 0.7f))
                                            .clickable { images = images - path },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(MiuixIcons.Close, "移除", Modifier.size(11.dp), tint = c.card)
                                    }
                                }
                            }
                        }
                    }
                    if (status.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(status, fontSize = 11.sp, color = c.secondary, lineHeight = 16.sp)
                    }
                }
            }

            item { SectionLabel("考试信息") }
            item {
                BoxCard {
                    Field("考试名称", title, { title = it }, "考试名称")
                    Spacer(Modifier.height(12.dp))
                    Field("考试日期", dateText, { dateText = it }, "2026.11.05")
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("今天" to 0L, "昨天" to 1L, "上周" to 7L).forEach { (label, daysAgo) ->
                            Text(
                                label, fontSize = 10.5.sp, color = c.neutralInk,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(c.neutralBg)
                                    .clickable { dateText = Dates.fmt(System.currentTimeMillis() - daysAgo * 86_400_000L) }
                                    .padding(horizontal = 9.dp, vertical = 5.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("考试类型", fontSize = 11.sp, color = c.secondary)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("月考", "期中", "期末", "模考", "联考", "单元测", "其他")) { t ->
                            Chip(t, t == type) { type = t }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { SectionLabel("各科成绩", accent = c.blue) }
            item {
                val required = listOf("语文", "数学", "英语")
                val electiveList = electivesPref.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val indexed = subjects.mapIndexed { i, item -> i to item }
                val groups = listOf(
                    Triple("必考", indexed.filter { it.second.subject in required }, c.blue),
                    Triple("选考", indexed.filter { it.second.subject in electiveList }, c.violet),
                    Triple(
                        "其他",
                        indexed.filter { it.second.subject !in required && it.second.subject !in electiveList },
                        c.secondary,
                    ),
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    groups.forEach { (label, list, tint) ->
                        if (list.isNotEmpty()) {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 6.dp, top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(5.dp).clip(RoundedCornerShape(2.dp)).background(tint))
                                Spacer(Modifier.width(7.dp))
                                Text(label, fontSize = 11.sp, color = c.secondary)
                                Spacer(Modifier.width(6.dp))
                                Text("${list.size}", fontSize = 10.sp, color = c.secondary.copy(alpha = 0.7f))
                            }
                            list.forEach { (index, item) ->
                                SubjectEditorCard(
                                    index = index,
                                    item = item,
                                    expanded = extraFor == index,
                                    onToggle = { extraFor = if (extraFor == index) -1 else index },
                                    onChange = { updated -> subjects[index] = updated },
                                    onDelete = { subjects.removeAt(index) },
                                )
                            }
                        }
                    }
                    BoxCard(
                        insidePadding = 0.dp,
                        container = c.cardSoft,
                        onClick = {
                            val used = subjects.map { it.subject }.toSet()
                            val next = listOf("语文", "数学", "英语", "物理", "化学", "生物", "政治", "历史", "地理", "日语")
                                .firstOrNull { it !in used } ?: ""
                            subjects.add(EditSubject(subject = next, full = ScoreText.of(SheetParser.defaultFull(next))))
                        },
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(MiuixIcons.AddCircle, null, Modifier.size(17.dp), tint = c.blue)
                            Spacer(Modifier.width(8.dp))
                            Text("添加科目", fontSize = 13.sp, color = c.blue, modifier = Modifier.weight(1f))
                            Text("自定义", fontSize = 10.sp, color = c.secondary)
                        }
                    }
                    BoxCard(
                        insidePadding = 0.dp,
                        container = c.cardSoft,
                        onClick = {
                            subjects.clear()
                            subjects.addAll(defaultSubjectRows())
                            AppState.say("已按选科生成 ${subjects.size} 科")
                        },
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(MiuixIcons.Refresh, null, Modifier.size(17.dp), tint = c.positive)
                            Spacer(Modifier.width(8.dp))
                            Text("按选科重填", fontSize = 13.sp, color = c.positive, modifier = Modifier.weight(1f))
                            Text("${electiveList.size + 3} 科", fontSize = 10.sp, color = c.secondary)
                        }
                    }
                }
            }

            if (error.isNotBlank()) {
                item {
                    Spacer(Modifier.height(10.dp))
                    Text(error, fontSize = 12.sp, color = c.negative)
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    if (examId == 0L) "保存这场考试" else "保存修改",
                    { save() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "不同满分不做直接相加 保存后由本地引擎计算",
                    fontSize = 10.sp, color = c.secondary, lineHeight = 15.sp,
                )
            }
        }
    }

    if (showVisionConsent) {
        OverlayDialog(
            show = true,
            title = "上传成绩单图片",
            summary = "将 ${images.size} 张图片发送到 DeepSeek 可能包含姓名与班级 仅本次请求使用",
            onDismissRequest = { showVisionConsent = false },
        ) {
            Column {
                Text("本地识别无需联网", fontSize = 11.sp, color = c.secondary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoftButton("取消", { showVisionConsent = false }, modifier = Modifier.weight(1f))
                    PrimaryButton("同意并上传", {
                        Prefs.setVisionConsent(true)
                        showVisionConsent = false
                        runVision()
                    }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SubjectEditorCard(
    index: Int,
    item: EditSubject,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (EditSubject) -> Unit,
    onDelete: () -> Unit,
) {
    val c = LocalAven.current
    BoxCard(insidePadding = 0.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Box(Modifier.weight(1.35f)) {
                    Field("科目", item.subject, { v -> onChange(item.copy(subject = v)) })
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    NumberField("得分", item.score, { v -> onChange(item.copy(score = v)) })
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    NumberField("满分", item.full, { v -> onChange(item.copy(full = v)) })
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.padding(bottom = 9.dp)) {
                    Icon(
                        MiuixIcons.Delete, "删除科目",
                        Modifier.size(16.dp).clickable(onClick = onDelete),
                        tint = c.secondary,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!item.confident && item.score.isNotBlank()) {
                    Badge("需核对", c.amber, c.amberBg)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    "赋分 排名 人数",
                    fontSize = 10.sp,
                    color = if (expanded) c.blue else c.secondary,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (expanded) MiuixIcons.ExpandLess else MiuixIcons.ExpandMore,
                        null, Modifier.size(15.dp),
                        tint = if (expanded) c.blue else c.secondary,
                    )
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        NumberField("赋分", item.converted, { v -> onChange(item.copy(converted = v)) })
                    }
                    Box(Modifier.weight(1f)) {
                        NumberField("排名", item.rank, { v -> onChange(item.copy(rank = v)) })
                    }
                    Box(Modifier.weight(1f)) {
                        NumberField("人数", item.total, { v -> onChange(item.copy(total = v)) })
                    }
                }
            }
        }
    }
}

private fun defaultSubjectRows(): List<EditSubject> {
    val base = listOf("语文", "数学", "英语")
    val electives = Prefs.electives.value.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val extra = if (Prefs.stage.value == 1) {
        electives.ifEmpty { listOf("物理", "化学", "生物") }
    } else {
        listOf("物理", "化学", "生物", "政治", "历史", "地理")
    }
    return (base + extra).distinct().map {
        EditSubject(subject = it, full = ScoreText.of(SheetParser.defaultFull(it)))
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, placeholder: String = "") {
    LabeledInput(label = label, value = value, onChange = onChange, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    LabeledInput(
        label = label.ifBlank { "数值" },
        value = value,
        onChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        numeric = true,
    )
}

private object ScoreText {
    fun of(v: Double): String =
        if (kotlin.math.abs(v - v.roundToInt()) < 0.05) v.toInt().toString()
        else String.format("%.1f", v)
}

private fun parseDate(text: String): Long? {
    val m = Regex("(20\\d{2})\\s*[.\\-/年]\\s*(\\d{1,2})\\s*[.\\-/月]\\s*(\\d{1,2})").find(text.trim()) ?: return null
    return runCatching {
        Dates.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
    }.getOrNull()
}
