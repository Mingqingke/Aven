package com.aven.app.ui.screens

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aven.app.BuildConfig
import com.aven.app.ui.Route
import com.aven.app.ui.components.Badge
import com.aven.app.ui.components.BoxCard
import com.aven.app.ui.components.Divider
import com.aven.app.ui.components.EntryRow
import com.aven.app.ui.components.ScreenTitle
import com.aven.app.ui.components.SectionLabel
import com.aven.app.ui.theme.LocalAven
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Store

@Composable
fun AboutScreen(onBack: () -> Unit, onOpen: (Route) -> Unit) {
    val c = LocalAven.current
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("关于", "Aven ${BuildConfig.VERSION_NAME}", onBack = onBack)
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Brush.linearGradient(listOf(c.blue, c.violet))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            androidx.compose.ui.res.painterResource(com.aven.app.R.drawable.ic_launcher_foreground),
                            null,
                            Modifier.size(104.dp),
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    BasicText(
                        "Aven",
                        style = TextStyle(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            brush = Brush.horizontalGradient(listOf(c.ink, c.blue, c.positive)),
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("AI ACADEMIC GROWTH", fontSize = 9.5.sp, letterSpacing = 2.4.sp, color = c.secondary)
                    Spacer(Modifier.height(10.dp))
                    Badge("${BuildConfig.VERSION_NAME} 正式版", c.blue, c.blueBg)
                }
            }

            item {
                BoxCard(container = c.blueBg, insidePadding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).clip(CircleShape).background(c.blue),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("青", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("青", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                            Spacer(Modifier.height(2.dp))
                            Text("设计与开发", fontSize = 10.5.sp, color = c.secondary)
                        }
                        Text(
                            "记录每一次考试\n理解每一次变化",
                            fontSize = 9.5.sp, color = c.blue, lineHeight = 15.sp,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }

            item { SectionLabel("联系作者") }
            item {
                val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                BoxCard(insidePadding = 0.dp) {
                    EntryRow(MiuixIcons.Send, "🛰️ M_qk1127", "点击复制", c.blue, onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString("M_qk1127"))
                        com.aven.app.ui.AppState.say("已复制")
                    })
                    Divider(startPadding = 64.dp)
                    EntryRow(MiuixIcons.Messages, "🐧 1908976218", "点击复制", c.positive, onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString("1908976218"))
                        com.aven.app.ui.AppState.say("已复制")
                    })
                    Divider(startPadding = 64.dp)
                    EntryRow(MiuixIcons.Share, "dy Ence11", "点击复制", c.violet, onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString("Ence11"))
                        com.aven.app.ui.AppState.say("已复制")
                    })
                }
            }

            item { SectionLabel("版本") }
            item {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                BoxCard(insidePadding = 0.dp) {
                    EntryRow(
                        MiuixIcons.Refresh,
                        "检查更新",
                        "当前 ${BuildConfig.VERSION_NAME}",
                        c.blue,
                        onClick = { com.aven.app.core.update.UpdateRunner.check(ctx) },
                    )
                    Divider(startPadding = 64.dp)
                    EntryRow(MiuixIcons.File, "更新日志", "版本 ${BuildConfig.VERSION_NAME} 调整记录", c.blue, onClick = {
                        onOpen(Route.AboutUpdate)
                    })
                }
            }

            item { SectionLabel("开源") }
            item {
                BoxCard(insidePadding = 0.dp) {
                    EntryRow(MiuixIcons.Store, "源代码仓库", "github.com/Mingqingke/Aven", c.blue, onClick = {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(com.aven.app.core.Project.REPO_URL),
                                )
                            )
                        }
                    })
                    Divider(startPadding = 64.dp)
                    EntryRow(MiuixIcons.Info, "问题反馈", "github.com/Mingqingke/Aven/issues", c.secondary, onClick = {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(com.aven.app.core.Project.ISSUES_URL),
                                )
                            )
                        }
                    })
                    Divider(startPadding = 64.dp)
                    EntryRow(MiuixIcons.Layers, "开源组件", "MIUIX AndroidX Room OkHttp", c.violet, onClick = {
                        onOpen(Route.AboutComponents)
                    })
                    Divider(startPadding = 64.dp)
                    EntryRow(MiuixIcons.File, "开源许可证", "MIT 与第三方许可", c.positive, onClick = {
                        onOpen(Route.AboutComponents)
                    })
                }
            }

            item { SectionLabel("说明") }
            item {
                BoxCard(insidePadding = 0.dp) {
                    EntryRow(MiuixIcons.Lock, "隐私与数据", "本地优先 按需授权", c.positive, onClick = {
                        onOpen(Route.AboutTerms)
                    })
                    Divider(startPadding = 64.dp)
                    EntryRow(MiuixIcons.File, "免责声明", "用途与边界", c.amber, onClick = {
                        onOpen(Route.AboutTerms)
                    })
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Aven 为学生自建学业档案\n成绩数据保存在本机",
                    fontSize = 9.5.sp, color = c.secondary.copy(alpha = 0.75f),
                    lineHeight = 15.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun AboutUpdateScreen(onBack: () -> Unit) {
    val c = LocalAven.current
    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("版本与更新", "当前 ${BuildConfig.VERSION_NAME}", onBack = onBack)
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        ) {
            item {
                BoxCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(c.blueBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                androidx.compose.ui.res.painterResource(com.aven.app.R.drawable.ic_launcher_foreground),
                                null, Modifier.size(64.dp), tint = c.blue,
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Aven", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                            Text("版本 ${BuildConfig.VERSION_NAME}", fontSize = 10.5.sp, color = c.secondary)
                        }
                        Badge("正式版", c.blue, c.blueBg)
                    }
                }
            }

            item { SectionLabel("构建信息") }
            item {
                BoxCard(insidePadding = 0.dp) {
                    InfoLine("版本号", BuildConfig.VERSION_NAME)
                    Divider(startPadding = 16.dp)
                    InfoLine("构建号", BuildConfig.VERSION_CODE.toString())
                    Divider(startPadding = 16.dp)
                    InfoLine("最低系统", "Android 8.0")
                    Divider(startPadding = 16.dp)
                    InfoLine("目标系统", "Android 17")
                    Divider(startPadding = 16.dp)
                    InfoLine("数据位置", "本机")
                }
            }

            item { SectionLabel("本版调整") }
            item {
                BoxCard {
                    listOf(
                        "新增面容识别与应用锁 密码保护",
                        "折线图支持滑动查看数据点",
                        "首页去掉重复信息 大卡收敛",
                        "作者联系方式与开源分组",
                        "考试日期不能晚于今天",
                        "页面切换与返回统一动效",
                    ).forEach { line ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Box(
                                Modifier.padding(top = 7.dp).size(4.dp).clip(CircleShape).background(c.blue)
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(line, fontSize = 12.sp, color = c.ink, lineHeight = 18.sp)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "检查更新在设置 更新与版本",
                    fontSize = 9.5.sp, color = c.secondary, lineHeight = 14.sp,
                )
            }
        }
    }
}

@Composable
fun AboutComponentsScreen(onBack: () -> Unit) {
    val c = LocalAven.current
    val items = listOf(
        Triple("MIUIX Compose", "Apache-2.0", "HyperOS 组件 图标 导航 弹窗"),
        Triple("AndroidX Compose", "Apache-2.0", "界面 动画 生命周期"),
        Triple("Room", "Apache-2.0", "本地数据库"),
        Triple("Kotlin Coroutines", "Apache-2.0", "异步与流"),
        Triple("OkHttp", "Apache-2.0", "DeepSeek 请求"),
        Triple("Coil", "Apache-2.0", "本地图片加载"),
        Triple("ML Kit Text Recognition", "Google 条款", "离线中文识别"),
    )
    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("开源组件", "只列出直接依赖", onBack = onBack)
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        ) {
            item { SectionLabel("组件与许可") }
            item {
                BoxCard(insidePadding = 0.dp) {
                    items.forEachIndexed { index, (name, license, use) ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.ink)
                                Spacer(Modifier.height(2.dp))
                                Text(use, fontSize = 10.sp, color = c.secondary)
                            }
                            Badge(license, c.secondary, c.neutralBg)
                        }
                        if (index < items.lastIndex) Divider(startPadding = 16.dp)
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "以上项目版权归各自作者所有\n使用遵循对应开源许可",
                    fontSize = 9.5.sp, color = c.secondary, lineHeight = 15.sp,
                )
            }
        }
    }
}

@Composable
fun AboutTermsScreen(onBack: () -> Unit) {
    val c = LocalAven.current
    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("隐私与边界", "说明数据去向与使用范围", onBack = onBack)
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        ) {
            item { SectionLabel("数据") }
            item {
                BoxCard {
                    Paragraph("成绩 图片 计划与报告默认保存在本机应用目录")
                    Paragraph("本地识别使用随应用安装的离线模型 不上传图片")
                    Paragraph("卸载应用会删除本地数据 需要保留请先导出备份")
                }
            }
            item { SectionLabel("AI") }
            item {
                BoxCard {
                    Paragraph("只有你主动生成报告或发起对话时 才通过 HTTPS 发送必要摘要")
                    Paragraph("不发送姓名 学校 图片与原始成绩单")
                    Paragraph("授权可随时在隐私与安全中关闭")
                }
            }
            item { SectionLabel("边界") }
            item {
                BoxCard {
                    Paragraph("Aven 不是官方教育平台 不提供提分承诺")
                    Paragraph("比较结论来自本地计算 附带可信度说明")
                    Paragraph("得分率无法完全消除试卷难度差异 结论仅供参考")
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "使用本应用即表示理解以上范围",
                    fontSize = 9.5.sp, color = c.secondary,
                )
            }
        }
    }
}

@Composable
private fun Paragraph(text: String) {
    val c = LocalAven.current
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Box(Modifier.padding(top = 7.dp).size(4.dp).clip(CircleShape).background(c.blue))
        Spacer(Modifier.width(9.dp))
        Text(text, fontSize = 12.sp, color = c.ink, lineHeight = 18.sp)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    val c = LocalAven.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.5.sp, color = c.ink, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.5.sp, color = c.secondary)
    }
}
