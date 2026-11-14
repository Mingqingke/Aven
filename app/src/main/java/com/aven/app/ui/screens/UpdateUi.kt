package com.aven.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aven.app.BuildConfig
import com.aven.app.core.Dates
import com.aven.app.core.Prefs
import com.aven.app.core.update.UpdateManager
import com.aven.app.core.update.UpdateState
import com.aven.app.core.update.UpdateRunner
import com.aven.app.ui.components.Badge
import com.aven.app.ui.components.BoxCard
import com.aven.app.ui.components.Divider
import com.aven.app.ui.components.EntryRow
import com.aven.app.ui.components.LabeledInput
import com.aven.app.ui.components.PrimaryButton
import com.aven.app.ui.components.ScreenTitle
import com.aven.app.ui.components.SectionLabel
import com.aven.app.ui.components.SoftButton
import com.aven.app.ui.components.ToggleRow
import com.aven.app.ui.theme.LocalAven
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.overlay.OverlayDialog

fun formatBytes(v: Long): String = when {
    v >= 1024L * 1024 -> String.format("%.1f MB", v / 1024.0 / 1024.0)
    v >= 1024 -> String.format("%.1f KB", v / 1024.0)
    else -> "$v B"
}

@Composable
fun UpdateDialogHost(onOpenUpdate: () -> Unit) {
    val c = LocalAven.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val show by UpdateState.showDialog.collectAsState()
    val manifest by UpdateState.manifest.collectAsState()
    val source by UpdateState.sourceName.collectAsState()
    if (!show || manifest == null) return

    OverlayDialog(
        show = true,
        title = "发现新版本 ${manifest!!.versionName}",
        summary = "当前 ${BuildConfig.VERSION_NAME} · ${manifest!!.releaseDate} · ${formatBytes(manifest!!.apkSize)}",
        onDismissRequest = { UpdateState.showDialog.value = false },
    ) {
        Column {
            manifest!!.changelog.take(4).forEach {
                Row(Modifier.padding(vertical = 3.dp)) {
                    Box(Modifier.padding(top = 7.dp).size(4.dp).clip(CircleShape).background(c.blue))
                    Spacer(Modifier.width(8.dp))
                    Text(it, fontSize = 11.5.sp, color = c.ink, lineHeight = 17.sp)
                }
            }
            if (source.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("来源 $source", fontSize = 9.5.sp, color = c.secondary)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton("稍后", { UpdateState.showDialog.value = false }, modifier = Modifier.weight(1f))
                PrimaryButton("立即更新", {
                    UpdateState.showDialog.value = false
                    onOpenUpdate()
                    UpdateRunner.startDownload(context)
                }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun UpdateScreen(onBack: () -> Unit) {
    val c = LocalAven.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val manifest by UpdateState.manifest.collectAsState()
    val phase by UpdateState.phase.collectAsState()
    val downloaded by UpdateState.downloaded.collectAsState()
    val total by UpdateState.total.collectAsState()
    val speed by UpdateState.speed.collectAsState()
    val message by UpdateState.message.collectAsState()
    val source by UpdateState.sourceName.collectAsState()
    val m = manifest

    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("应用更新", m?.let { "最新 ${it.versionName}" } ?: "检查更新", onBack = onBack)
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        ) {
            if (m == null) {
                item {
                    BoxCard {
                        Text("暂无检测到的新版本", fontSize = 13.sp, color = c.ink)
                        Spacer(Modifier.height(6.dp))
                        Text("可在设置中配置更新源后重新检查", fontSize = 10.5.sp, color = c.secondary)
                    }
                }
                return@LazyColumn
            }
            item {
                BoxCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(c.blueBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                androidx.compose.ui.res.painterResource(com.aven.app.R.drawable.ic_launcher_foreground),
                                null, Modifier.size(64.dp), tint = Color.Unspecified,
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Aven ${m.versionName}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                            Text(
                                "${m.releaseDate} · ${formatBytes(m.apkSize)}",
                                fontSize = 10.5.sp, color = c.secondary,
                            )
                        }
                        if (m.mandatory) Badge("重要", c.negative, c.negativeBg)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Text("当前 ${BuildConfig.VERSION_NAME}", fontSize = 10.5.sp, color = c.secondary)
                        Spacer(Modifier.weight(1f))
                        if (source.isNotBlank()) Text("来源 $source", fontSize = 10.5.sp, color = c.secondary)
                    }
                }
            }
            item { SectionLabel("更新内容", accent = c.blue) }
            item {
                BoxCard {
                    if (m.changelog.isEmpty()) {
                        Text("没有提供更新说明", fontSize = 11.5.sp, color = c.secondary)
                    } else {
                        m.changelog.forEach {
                            Row(Modifier.padding(vertical = 4.dp)) {
                                Box(Modifier.padding(top = 7.dp).size(4.dp).clip(CircleShape).background(c.blue))
                                Spacer(Modifier.width(9.dp))
                                Text(it, fontSize = 12.sp, color = c.ink, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
            item { SectionLabel("下载与校验") }
            item {
                BoxCard {
                    val fraction = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(progress = if (phase == UpdateState.Phase.DOWNLOADING) fraction else null)
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Text(
                            when (phase) {
                                UpdateState.Phase.DOWNLOADING -> "${(fraction * 100).toInt()}%  ${formatBytes(downloaded)} / ${formatBytes(total)}"
                                UpdateState.Phase.VERIFYING -> "正在校验安装包"
                                UpdateState.Phase.READY -> "校验通过 可以安装"
                                UpdateState.Phase.FAILED -> message
                                else -> "等待开始"
                            },
                            fontSize = 11.5.sp,
                            color = if (phase == UpdateState.Phase.FAILED) c.negative else c.ink,
                            modifier = Modifier.weight(1f),
                        )
                        if (phase == UpdateState.Phase.DOWNLOADING && speed > 0) {
                            Text("${formatBytes(speed)}/s", fontSize = 10.5.sp, color = c.secondary)
                        }
                    }
                    if (phase == UpdateState.Phase.FAILED && message.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(message, fontSize = 10.5.sp, color = c.negative, lineHeight = 15.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    when (phase) {
                        UpdateState.Phase.READY -> PrimaryButton("安装更新", {
                            UpdateRunner.install(context)
                        }, modifier = Modifier.fillMaxWidth())
                        UpdateState.Phase.DOWNLOADING, UpdateState.Phase.VERIFYING -> SoftButton(
                            "下载中…", {}, modifier = Modifier.fillMaxWidth(),
                        )
                        UpdateState.Phase.FAILED -> PrimaryButton("重新下载", {
                            UpdateRunner.startDownload(context, force = true)
                        }, modifier = Modifier.fillMaxWidth())
                        else -> PrimaryButton("开始下载", {
                            UpdateRunner.startDownload(context)
                        }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
                Text(
                    "安装包会先校验 SHA-256 包名 版本号与签名 校验通过后才交给系统安装器",
                    fontSize = 9.5.sp, color = c.secondary, lineHeight = 15.sp,
                )
            }
        }
    }
}

@Composable
fun UpdateSettingsScreen(onBack: () -> Unit) {
    val c = LocalAven.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val auto by Prefs.autoUpdate.collectAsState()
    val manifestUrl by Prefs.updateManifestUrl.collectAsState()
    val backupUrl by Prefs.updateBackupUrl.collectAsState()
    val repo by Prefs.updateGithubRepo.collectAsState()
    val lastAt by Prefs.lastCheckAt.collectAsState()
    val lastSummary by Prefs.lastCheckSummary.collectAsState()
    val message by UpdateState.message.collectAsState()
    val phase by UpdateState.phase.collectAsState()

    var manifestInput by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(manifestUrl) }
    var backupInput by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(backupUrl) }
    var repoInput by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(repo) }
    val signature = androidx.compose.runtime.remember { UpdateManager.installedSignerSha256(context).orEmpty() }

    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("更新与版本", "当前 ${BuildConfig.VERSION_NAME}", onBack = onBack)
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        ) {
            item {
                BoxCard(insidePadding = 0.dp) {
                    EntryRow(MiuixIcons.Update, "检查更新", lastSummary.ifBlank { "未检查" }, c.blue, onClick = {
                        UpdateRunner.check(context)
                    })
                    Divider(startPadding = 64.dp)
                    ToggleRow("启动时自动检查", "不阻塞首页加载", auto) { Prefs.setAutoUpdate(it) }
                }
            }

            item { SectionLabel("更新源", accent = c.violet) }
            item {
                BoxCard {
                    Text("按顺序尝试 全部失败才提示失败", fontSize = 10.sp, color = c.secondary)
                    Spacer(Modifier.height(10.dp))
                    LabeledInput("官方版本清单 latest.json", manifestInput, { manifestInput = it })
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SoftButton("保存", {
                            Prefs.setUpdateManifestUrl(manifestInput.trim())
                        }, modifier = Modifier.weight(1f))
                        SoftButton("测试", {
                            Prefs.setUpdateManifestUrl(manifestInput.trim())
                            UpdateRunner.check(context)
                        }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    LabeledInput("备用清单地址", backupInput, { backupInput = it })
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SoftButton("保存", {
                            Prefs.setUpdateBackupUrl(backupInput.trim())
                        }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    LabeledInput("GitHub 仓库 owner/repo", repoInput, { repoInput = it })
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SoftButton("保存", {
                            Prefs.setUpdateGithubRepo(repoInput.trim())
                        }, modifier = Modifier.weight(1f))
                        SoftButton("测试", {
                            Prefs.setUpdateGithubRepo(repoInput.trim())
                            UpdateRunner.check(context)
                        }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("留空表示该来源未配置 不会伪造可用状态", fontSize = 9.5.sp, color = c.secondary)
                }
            }

            item { SectionLabel("签名与版本", accent = c.positive) }
            item {
                BoxCard {
                    InfoRow("当前版本", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    Divider(startPadding = 16.dp)
                    InfoRow("包名", BuildConfig.APPLICATION_ID)
                    Divider(startPadding = 16.dp)
                    InfoRow("签名 SHA-256", signature.take(24) + if (signature.length > 24) "…" else "")
                    Divider(startPadding = 16.dp)
                    InfoRow("最近检查", if (lastAt > 0) Dates.fmt(lastAt) else "未检查")
                }
            }
            if (message.isNotBlank()) {
                item {
                    Spacer(Modifier.height(10.dp))
                    Text(message, fontSize = 10.5.sp, color = if (phase == UpdateState.Phase.FAILED) c.negative else c.secondary)
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
                Text(
                    "正式版本必须使用同一签名与递增的 versionCode 否则无法覆盖安装",
                    fontSize = 9.5.sp, color = c.secondary, lineHeight = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val c = LocalAven.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, fontSize = 12.5.sp, color = c.ink, modifier = Modifier.weight(1f))
        Text(value, fontSize = 11.5.sp, color = c.secondary)
    }
}
