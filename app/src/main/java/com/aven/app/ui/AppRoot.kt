package com.aven.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aven.app.core.Prefs
import com.aven.app.core.lock.AppLock
import com.aven.app.core.update.UpdateRunner
import com.aven.app.ui.screens.AboutComponentsScreen
import com.aven.app.ui.screens.AboutScreen
import com.aven.app.ui.screens.AboutTermsScreen
import com.aven.app.ui.screens.AboutUpdateScreen
import com.aven.app.ui.screens.AiChatScreen
import com.aven.app.ui.screens.AiSettingsScreen
import com.aven.app.ui.screens.AppearanceSettingsScreen
import com.aven.app.ui.screens.DataSettingsScreen
import com.aven.app.ui.screens.AppLockScreen
import com.aven.app.ui.screens.ExamDetailScreen
import com.aven.app.ui.screens.LockSettingsScreen
import com.aven.app.ui.screens.ExamEditorScreen
import com.aven.app.ui.screens.ExamSettingsScreen
import com.aven.app.ui.screens.HomeScreen
import com.aven.app.ui.screens.InsightsScreen
import com.aven.app.ui.screens.PlansScreen
import com.aven.app.ui.screens.PrivacySettingsScreen
import com.aven.app.ui.screens.ProfileSettingsScreen
import com.aven.app.ui.screens.RecordsScreen
import com.aven.app.ui.screens.ReportDetailScreen
import com.aven.app.ui.screens.SettingsHomeScreen
import com.aven.app.ui.screens.SubjectDetailScreen
import com.aven.app.ui.screens.UpdateDialogHost
import com.aven.app.ui.screens.UpdateScreen
import com.aven.app.ui.screens.UpdateSettingsScreen
import com.aven.app.ui.theme.LocalAven
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.rememberNavController
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition

private val AvenMotion = NavMotion(
    programmatic = top.yukonga.miuix.kmp.nav.transition.NavSettleSpec.Tween(210, FastOutSlowInEasing),
    commit = top.yukonga.miuix.kmp.nav.transition.NavSettleSpec.Tween(170, FastOutSlowInEasing),
    cancel = top.yukonga.miuix.kmp.nav.transition.NavSettleSpec.Tween(150, FastOutSlowInEasing),
)

/** 轻量淡入位移 推入与返回都有动画 不做整页横向滑动 */
private val AvenTransition = navGraphicsTransition(
    opaqueDepth = 1f,
    motion = AvenMotion,
    scrim = { 0f },
) { scope ->
    val d = scope.relativeDepth
    val px = 22f * scope.density.density
    if (d < 0f) {
        val p = (-d).coerceIn(0f, 1f)
        alpha = 1f - p
        translationX = p * px
    } else {
        alpha = 1f
        translationX = 0f
    }
}

private class TabDef(val label: String, val icon: ImageVector)

@Composable
fun AppRoot() {
    val c = LocalAven.current
    val reduceMotion by Prefs.reduceMotion.collectAsState()

    var tab by remember { mutableIntStateOf(0) }
    var settingsOpen by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf("") }

    val homeNav = rememberNavController<Route>(Route.Home)
    val recordsNav = rememberNavController<Route>(Route.Records)
    val insightsNav = rememberNavController<Route>(Route.Insights)
    val plansNav = rememberNavController<Route>(Route.Plans)
    val settingsNav = rememberNavController<Route>(Route.Settings)

    val tabs = listOf(
        TabDef("首页", MiuixIcons.Home),
        TabDef("成绩", MiuixIcons.ListView),
        TabDef("分析", MiuixIcons.Report),
        TabDef("计划", MiuixIcons.Tasks),
    )
    val activeNav = when (tab) {
        1 -> recordsNav
        2 -> insightsNav
        3 -> plansNav
        else -> homeNav
    }
    val locked by AppLock.locked.collectAsState()
    if (locked) {
        AppLockScreen()
        return
    }

    val atRoot = activeNav.backStack.size == 1
    val settingsAtRoot = settingsNav.backStack.size == 1

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        if (Prefs.autoUpdate.value) {
            delay(1200)
            UpdateRunner.check(context)
        }
    }

    val message by AppState.toast.collectAsState()
    LaunchedEffect(message) {
        if (message.isNotBlank()) {
            toast = message
            AppState.toast.value = ""
            delay(1600)
            toast = ""
        }
    }

    BackHandler(enabled = settingsOpen) {
        if (settingsAtRoot) settingsOpen = false else settingsNav.pop()
    }
    BackHandler(enabled = !settingsOpen && !atRoot) { activeNav.pop() }
    BackHandler(enabled = !settingsOpen && tab != 0 && atRoot) { tab = 0 }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = c.page,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
    Box(Modifier.fillMaxSize().background(c.page)) {
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "tabSwitch",
        ) { current ->
            val nav = when (current) {
                1 -> recordsNav
                2 -> insightsNav
                3 -> plansNav
                else -> homeNav
            }
            TabNavHost(
                nav = nav,
                reduceMotion = reduceMotion,
                onOpenSettings = { settingsOpen = true },
                onOpenTab = { tab = it },
            )
        }

        if (atRoot && !settingsOpen) {
            BottomBar(
                tabs = tabs,
                current = tab,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) { index ->
                if (index == tab) {
                    val nav = when (index) {
                        1 -> recordsNav
                        2 -> insightsNav
                        3 -> plansNav
                        else -> homeNav
                    }
                    while (nav.backStack.size > 1) nav.pop()
                } else tab = index
            }
        }

        if (settingsOpen) {
            Box(Modifier.fillMaxSize().background(c.page)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.fillMaxHeight().widthIn(max = 700.dp).fillMaxWidth()) {
                key(reduceMotion) {
                    NavDisplay(
                        navController = settingsNav,
                        modifier = Modifier.fillMaxSize(),
                        transition = AvenTransition,
                    ) {
                        entry<Route.Settings> {
                            PageSurface { SettingsHomeScreen(onOpen = { settingsNav.pushSafely(it) }, onBack = { settingsOpen = false }) }
                        }
                        entry<Route.SettingsProfile> {
                            PageSurface { ProfileSettingsScreen(onBack = { settingsNav.pop() }) }
                        }
                        entry<Route.SettingsExam> {
                            PageSurface { ExamSettingsScreen(onBack = { settingsNav.pop() }) }
                        }
                        entry<Route.SettingsAi> {
                            PageSurface { AiSettingsScreen(onBack = { settingsNav.pop() }) }
                        }
                        entry<Route.SettingsData> {
                            PageSurface { DataSettingsScreen(onBack = { settingsNav.pop() }) }
                        }
                        entry<Route.SettingsAppearance> {
                            PageSurface { AppearanceSettingsScreen(onBack = { settingsNav.pop() }) }
                        }
                        entry<Route.SettingsPrivacy> {
                            PageSurface { PrivacySettingsScreen(onBack = { settingsNav.pop() }) }
                        }
                        entry<Route.SettingsUpdate> {
                            PageSurface { UpdateSettingsScreen(onBack = { settingsNav.pop() }) }
                        }
                        entry<Route.SettingsLock> {
                            PageSurface { LockSettingsScreen(onBack = { settingsNav.pop() }) }
                        }
                        entry<Route.Update> {
                            PageSurface { UpdateScreen(onBack = { settingsNav.pop() }) }
                        }
                        entry<Route.About> {
                            PageSurface { AboutScreen(onBack = { settingsNav.pop() }, onOpen = { settingsNav.pushSafely(it) }) }
                        }
                        entry<Route.AboutComponents> {
                            PageSurface { AboutComponentsScreen(onBack = { settingsNav.pop() }) }
                        }
                        entry<Route.AboutTerms> {
                            PageSurface { AboutTermsScreen(onBack = { settingsNav.pop() }) }
                        }
                        entry<Route.AboutUpdate> {
                            PageSurface { AboutUpdateScreen(onBack = { settingsNav.pop() }) }
                        }
                    }
                }
                }
                }
            }
        }

        UpdateDialogHost(onOpenUpdate = {
            settingsOpen = true
            settingsNav.pushSafely(Route.Update)
        })

        AnimatedVisibility(
            visible = toast.isNotBlank(),
            enter = fadeIn(tween(160)) + slideInVertically { it / 3 } + expandVertically(),
            exit = fadeOut(tween(160)) + slideOutVertically { it / 3 } + shrinkVertically(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                Modifier
                    .padding(bottom = if (atRoot && !settingsOpen) 84.dp else 32.dp)
                    .padding(horizontal = 40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(c.ink.copy(alpha = 0.9f))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text(toast, fontSize = 11.sp, color = c.card, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    }
}

/** 页面必须有自己的不透明背景 否则转场时下一层会透出来 */
@Composable
private fun PageSurface(content: @Composable () -> Unit) {
    val c = LocalAven.current
    Box(Modifier.fillMaxSize().background(c.page).navigationBarsPadding()) { content() }
}

private fun NavController.pushSafely(route: Route) {
    if (backStack.any { it == route }) return
    push(route)
}

@Composable
private fun BottomBar(
    tabs: List<TabDef>,
    current: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    val c = LocalAven.current
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        color = c.card,
        showDivider = true,
        mode = NavigationBarDisplayMode.IconAndText,
    ) {
        tabs.forEachIndexed { index, item ->
            val selected = current == index
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(index) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val scale by animateFloatAsState(if (selected) 1.08f else 1f, tween(180), label = "navScale")
                Icon(
                    item.icon, item.label, Modifier.size(22.dp).graphicsLayer { scaleX = scale; scaleY = scale },
                    tint = if (selected) c.ink else c.secondary.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    item.label, fontSize = 10.5.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = if (selected) c.ink else c.secondary,
                )
            }
        }
    }
}

@Composable
private fun TabNavHost(
    nav: NavController,
    reduceMotion: Boolean,
    onOpenSettings: () -> Unit,
    onOpenTab: (Int) -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.fillMaxHeight().widthIn(max = 700.dp).fillMaxWidth()) {
            NavDisplay(
                navController = nav,
                modifier = Modifier.fillMaxSize(),
                transition = AvenTransition,
            ) {
                entry<Route.Home> {
                    PageSurface {
                        HomeScreen(
                            onOpenExam = { nav.pushSafely(Route.ExamDetail(it)) },
                            onOpenSubject = { nav.pushSafely(Route.SubjectDetail(it)) },
                            onOpenSettings = onOpenSettings,
                            onOpenPlans = { onOpenTab(3) },
                            onAddExam = { nav.pushSafely(Route.AddExam) },
                            onOpenInsights = { onOpenTab(2) },
                        )
                    }
                }
                entry<Route.Records> {
                    PageSurface {
                        RecordsScreen(
                            onOpenExam = { nav.pushSafely(Route.ExamDetail(it)) },
                            onAddExam = { nav.pushSafely(Route.AddExam) },
                            onOpenSubject = { nav.pushSafely(Route.SubjectDetail(it)) },
                        )
                    }
                }
                entry<Route.Insights> {
                    PageSurface {
                        InsightsScreen(
                            onOpenSubject = { nav.pushSafely(Route.SubjectDetail(it)) },
                            onOpenReport = { nav.pushSafely(Route.ReportDetail(it)) },
                            onOpenChat = { nav.pushSafely(Route.AiChat) },
                            onOpenSettings = onOpenSettings,
                            onOpenExam = { nav.pushSafely(Route.ExamDetail(it)) },
                        )
                    }
                }
                entry<Route.Plans> {
                    PageSurface { PlansScreen() }
                }
                entry<Route.ExamDetail> { r ->
                    PageSurface {
                        ExamDetailScreen(
                            examId = r.id,
                            onBack = { nav.pop() },
                            onEdit = { nav.pushSafely(Route.EditExam(r.id)) },
                            onOpenSubject = { nav.pushSafely(Route.SubjectDetail(it)) },
                            onDeleted = { nav.pop() },
                        )
                    }
                }
                entry<Route.SubjectDetail> { r ->
                    PageSurface {
                        SubjectDetailScreen(r.subject, onBack = { nav.pop() }, onOpenExam = { nav.pushSafely(Route.ExamDetail(it)) })
                    }
                }
                entry<Route.AddExam> {
                    PageSurface {
                        ExamEditorScreen(0L, onBack = { nav.pop() }, onSaved = { nav.pop() })
                    }
                }
                entry<Route.EditExam> { r ->
                    PageSurface {
                        ExamEditorScreen(r.id, onBack = { nav.pop() }, onSaved = { nav.pop() })
                    }
                }
                entry<Route.ReportDetail> { r ->
                    PageSurface {
                        ReportDetailScreen(r.id, onBack = { nav.pop() }, onDeleted = { nav.pop() })
                    }
                }
                entry<Route.AiChat> {
                    PageSurface {
                        AiChatScreen(onBack = { nav.pop() })
                    }
                }
            }
        }
    }
}
