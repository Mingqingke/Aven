package com.aven.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * Aven 的视觉体系：高级中性色底 + 白色超圆角卡片，
 * 绿色 / 红色只用于成绩状态，不用于普通文字与按钮。
 */
@Immutable
data class AvenColors(
    val dark: Boolean,
    val page: Color,
    val card: Color,
    val cardSoft: Color,
    val ink: Color,
    val secondary: Color,
    val hairline: Color,
    val positive: Color,
    val positiveBg: Color,
    val positiveInk: Color,
    val negative: Color,
    val negativeBg: Color,
    val negativeInk: Color,
    val neutralBg: Color,
    val neutralInk: Color,
    val blue: Color,
    val amber: Color,
    val violet: Color,
    val blueBg: Color,
    val amberBg: Color,
    val violetBg: Color,
    val shadow: Color,
)

val AvenLight = AvenColors(
    dark = false,
    page = Color(0xFFF5F6F8),
    card = Color(0xFFFFFFFF),
    cardSoft = Color(0xFFFAFAFB),
    ink = Color(0xFF0B0C0D),
    secondary = Color(0xFF8A9099),
    hairline = Color(0xFFECEEF1),
    positive = Color(0xFF2F9E6B),
    positiveBg = Color(0xFFE2F3E9),
    positiveInk = Color(0xFF176142),
    negative = Color(0xFFCF4A3C),
    negativeBg = Color(0xFFFBE7E3),
    negativeInk = Color(0xFF8C2A20),
    neutralBg = Color(0xFFEFF1F3),
    neutralInk = Color(0xFF565D66),
    blue = Color(0xFF3482FF),
    amber = Color(0xFFC4861C),
    violet = Color(0xFF7A5AF8),
    blueBg = Color(0xFFEAF1FF),
    amberBg = Color(0xFFFDF3E2),
    violetBg = Color(0xFFF1EDFE),
    shadow = Color(0x14000000),
)

val AvenDark = AvenColors(
    dark = true,
    page = Color(0xFF0C0E10),
    card = Color(0xFF171A1D),
    cardSoft = Color(0xFF1D2125),
    ink = Color(0xFFF2F3F4),
    secondary = Color(0xFF8E949C),
    hairline = Color(0xFF24282C),
    positive = Color(0xFF4CC38A),
    positiveBg = Color(0xFF14301F),
    positiveInk = Color(0xFFA6E7C6),
    negative = Color(0xFFF07A70),
    negativeBg = Color(0xFF3A1D1A),
    negativeInk = Color(0xFFF3B4AA),
    neutralBg = Color(0xFF202428),
    neutralInk = Color(0xFFB7BEC6),
    blue = Color(0xFF5B9BFF),
    amber = Color(0xFFE0B25C),
    violet = Color(0xFF9B85FF),
    blueBg = Color(0xFF14243D),
    amberBg = Color(0xFF33280F),
    violetBg = Color(0xFF241F3A),
    shadow = Color(0x66000000),
)

val LocalAven = staticCompositionLocalOf { AvenLight }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF3482FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF1FF),
    onPrimaryContainer = Color.White,
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF0B0C0D),
    onBackgroundVariant = Color(0xFF8A9099),
    surface = Color(0xFFF5F6F8),
    onSurface = Color(0xFF0B0C0D),
    surfaceVariant = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    onSurfaceContainer = Color(0xFF0B0C0D),
    onSurfaceContainerVariant = Color(0xFF8A9099),
    onSurfaceVariantSummary = Color(0x99000000),
    onSurfaceVariantActions = Color(0x66000000),
    outline = Color(0xFFE4E6EA),
    dividerLine = Color(0xFFECEEF1),
    secondaryContainer = Color(0xFFEFF1F3),
    onSecondaryContainer = Color(0xFF565D66),
    tertiaryContainer = Color(0xFFEAF1FF),
    onTertiaryContainer = Color(0xFF3482FF),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF5B9BFF),
    onPrimary = Color(0xFF06121F),
    primaryContainer = Color(0xFF14243D),
    onPrimaryContainer = Color(0xFFF2F3F4),
    background = Color(0xFF0C0E10),
    onBackground = Color(0xFFF2F3F4),
    onBackgroundVariant = Color(0xFF8E949C),
    surface = Color(0xFF0C0E10),
    onSurface = Color(0xFFF2F3F4),
    surfaceVariant = Color(0xFF171A1D),
    surfaceContainer = Color(0xFF171A1D),
    onSurfaceContainer = Color(0xFFEDEEEF),
    onSurfaceContainerVariant = Color(0xFF8E949C),
    onSurfaceVariantSummary = Color(0x99FFFFFF),
    onSurfaceVariantActions = Color(0x66FFFFFF),
    outline = Color(0xFF2A2E33),
    dividerLine = Color(0xFF24282C),
    secondaryContainer = Color(0xFF202428),
    onSecondaryContainer = Color(0xFFB7BEC6),
)

@Composable
fun AvenTheme(mode: Int, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalAven provides if (dark) AvenDark else AvenLight) {
        MiuixTheme(colors = if (dark) DarkScheme else LightScheme) { content() }
    }
}
