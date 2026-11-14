package com.aven.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aven.app.core.Dates
import com.aven.app.core.Prefs
import com.aven.app.core.Store
import com.aven.app.core.SubjectDraft
import com.aven.app.core.ExamDraft
import com.aven.app.core.ai.Ai
import com.aven.app.core.ai.AiTasks
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
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text

private data class BatchDraft(
    val path: String,
    val title: String,
    val dateText: String,
    val subjects: List<SubjectDraft>,
    val error: String = "",
)

/**
 * 批量导入 每张图一场考试
 * 每张单独识别 识别后在列表里核对 再一次性保存
 */
@Composable
fun BatchImportScreen(paths: List<String>, autoSave: Boolean = false, onBack: () -> Unit, onDone: () -> Unit) {
    val c = LocalAven.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var drafts by remember { mutableStateOf<List<BatchDraft>>(emptyList()) }
    var progress by remember { mutableStateOf("准备识别") }
    var saving by remember { mutableStateOf(false) }
    var autoDone by remember { mutableStateOf(false) }
    var autoSaved by remember { mutableStateOf(0) }

    LaunchedEffect(paths) {
        val list = mutableListOf<BatchDraft>()
        for ((index, path) in paths.withIndex()) {
            progress = "识别第 ${index + 1}/${paths.size} 张"
            val result = Ai.vision(context, listOf(path), AiTasks.visionPrompt())
            val draft = when (result) {
                is Ai.Out.Ok -> {
                    val sheet = AiTasks.parseVision(result.text)
                    if (sheet == null) {
                        BatchDraft(path, "", "", emptyList(), "内容无法解析")
                    } else if (sheet.subjects.isEmpty()) {
                        BatchDraft(path, "", "", emptyList(), "未识别到科目")
                    } else {
                        val subjects = sheet.subjects.mapNotNull { v ->
                            val name = v.name.take(12)
                            if (name.isBlank()) return@mapNotNull null
                            SubjectDraft(
                                subject = name,
                                raw = v.score ?: 0.0,
                                full = v.full ?: SheetParser.defaultFull(name),
                                converted = v.converted,
                                rank = v.rank,
                                total = v.total,
                            )
                        }
                        BatchDraft(
                            path = path,
                            title = sheet.title ?: "成绩详情",
                            dateText = sheet.date ?: "",
                            subjects = subjects,
                        )
                    }
                }
                is Ai.Out.Err -> BatchDraft(path, "", "", emptyList(), result.message)
            }
            list += draft
            drafts = list.toList()
        }
        if (autoSave) {
            val sorted = list.sortedBy { parseDay(it.dateText) ?: Long.MAX_VALUE }
            drafts = sorted
            var saved = 0
            val existing = Store.exams.value
            for (d in sorted) {
                if (d.error.isNotBlank() || d.subjects.isEmpty()) continue
                val millis = parseDay(d.dateText) ?: continue
                val title = d.title.ifBlank { "成绩详情" }
                if (existing.any { Dates.fmt(it.date) == Dates.fmt(millis) && it.title == title }) continue
                Store.saveExam(
                    ExamDraft(
                        title = title,
                        date = millis,
                        type = Prefs.defaultExamType.value,
                        stage = if (Prefs.stage.value == 0) "初中" else "高中",
                        images = listOf(d.path),
                        source = "vision",
                        subjects = d.subjects,
                    )
                )
                saved++
            }
            autoSaved = saved
            autoDone = true
            AppState.say("已保存 $saved 场 按日期排序")
        }
        progress = ""
    }

    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("批量导入", if (progress.isBlank()) "${drafts.size} 场考试" else progress, onBack = onBack)
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        ) {
            if (progress.isNotBlank()) {
                item {
                    BoxCard {
                        Text(progress, fontSize = 12.sp, color = c.ink)
                        Spacer(Modifier.height(6.dp))
                        Text("每张图会单独成为一场考试", fontSize = 10.sp, color = c.secondary)
                    }
                }
            }
            item { SectionLabel("识别结果", trailing = "${drafts.size} 张") }
            drafts.forEachIndexed { index, draft ->
                item(key = "draft_$index") {
                    BoxCard(modifier = Modifier.padding(bottom = 12.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("#${index + 1}", fontSize = 11.sp, color = c.secondary, modifier = Modifier.weight(1f))
                            if (draft.error.isNotBlank()) {
                                Badge("识别失败", c.negative, c.negativeBg)
                            } else {
                                Badge("${draft.subjects.size} 科", c.blue, c.blueBg)
                            }
                        }
                        if (draft.error.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(draft.error, fontSize = 11.sp, color = c.negative)
                            Spacer(Modifier.height(8.dp))
                            androidx.compose.foundation.layout.Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SoftButton("移除", {
                                    drafts = drafts.filterIndexed { i, _ -> i != index }
                                })
                            }
                        } else {
                            Spacer(Modifier.height(10.dp))
                            if (autoDone) {
                                Text(draft.title.ifBlank { "成绩详情" }, fontSize = 13.sp, color = c.ink)
                                Spacer(Modifier.height(3.dp))
                                Text(draft.dateText.ifBlank { "日期缺失" }, fontSize = 10.5.sp, color = c.secondary)
                            } else {
                                LabeledInput("考试名称", draft.title, { v ->
                                    drafts = drafts.toMutableList().also {
                                        it[index] = draft.copy(title = v)
                                    }
                                })
                                Spacer(Modifier.height(10.dp))
                                LabeledInput("考试日期", draft.dateText, { v ->
                                    drafts = drafts.toMutableList().also {
                                        it[index] = draft.copy(dateText = v)
                                    }
                                })
                            }
                            Spacer(Modifier.height(10.dp))
                            val total = draft.subjects.sumOf { it.raw }
                            Text(
                                "共 ${draft.subjects.size} 科 总分 ${com.aven.app.core.calc.ScoreEngine.fmtScore(total)}",
                                fontSize = 10.5.sp, color = c.secondary,
                            )
                            Spacer(Modifier.height(8.dp))
                            draft.subjects.forEach { s ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                    Text(s.subject, fontSize = 11.5.sp, color = c.ink, modifier = Modifier.weight(1f))
                                    Text(
                                        buildString {
                                            append(com.aven.app.core.calc.ScoreEngine.fmtScore(s.raw))
                                            if (s.converted != null) append(" 赋分 ").append(com.aven.app.core.calc.ScoreEngine.fmtScore(s.converted))
                                            if (s.rank != null) append(" 校次 ").append(s.rank)
                                        },
                                        fontSize = 10.5.sp, color = c.secondary,
                                    )
                                }
                            }
                            if (!autoDone) {
                                Divider(startPadding = 0.dp)
                                Spacer(Modifier.height(6.dp))
                                SoftButton("移除这一场", {
                                    drafts = drafts.filterIndexed { i, _ -> i != index }
                                }, color = c.negative)
                            }
                        }
                    }
                }
            }

            item {
                if (autoDone) {
                    BoxCard(container = c.positiveBg) {
                        Text("已保存 $autoSaved 场 按日期排序", fontSize = 12.5.sp, color = c.positiveInk)
                    }
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton("完成", onDone, modifier = Modifier.fillMaxWidth())
                } else if (drafts.any { it.error.isBlank() && it.subjects.isNotEmpty() }) {
                    PrimaryButton(
                        if (saving) "正在保存" else "保存全部",
                        {
                            if (saving) return@PrimaryButton
                            saving = true
                            scope.launch {
                                var saved = 0
                                var skipped = 0
                                val existing = Store.exams.value
                                for (d in drafts) {
                                    if (d.error.isNotBlank() || d.subjects.isEmpty()) { skipped++; continue }
                                    val millis = parseDay(d.dateText) ?: run { skipped++; continue }
                                    val title = d.title.ifBlank { "成绩详情" }
                                    val dup = existing.any {
                                        Dates.fmt(it.date) == Dates.fmt(millis) && it.title == title
                                    }
                                    if (dup) { skipped++; continue }
                                    Store.saveExam(
                                        ExamDraft(
                                            title = title,
                                            date = millis,
                                            type = Prefs.defaultExamType.value,
                                            stage = if (Prefs.stage.value == 0) "初中" else "高中",
                                            images = listOf(d.path),
                                            source = "vision",
                                            subjects = d.subjects,
                                        )
                                    )
                                    saved++
                                }
                                saving = false
                                AppState.say("已保存 $saved 场" + if (skipped > 0) " 跳过 $skipped 条" else "")
                                onDone()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !saving,
                    )
                } else {
                    Text("没有可保存的考试", fontSize = 11.sp, color = c.secondary)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "日期缺失或重复的条目会被跳过 保存后可在成绩页逐场修改",
                    fontSize = 9.5.sp, color = c.secondary, lineHeight = 14.sp,
                )
            }
        }
    }
}

private fun parseDay(text: String): Long? {
    val m = Regex("(20\\d{2})\\s*[.\\-/年]\\s*(\\d{1,2})\\s*[.\\-/月]\\s*(\\d{1,2})").find(text.trim()) ?: return null
    return runCatching { Dates.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }.getOrNull()
}
