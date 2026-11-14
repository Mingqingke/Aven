package com.aven.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aven.app.ui.theme.LocalAven
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun avenCardColors(color: Color? = null): top.yukonga.miuix.kmp.basic.CardColors {
    val c = LocalAven.current
    return CardDefaults.defaultColors(color = color ?: c.card, contentColor = c.ink)
}

@Composable
fun BoxCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    container: Color? = null,
    onClick: (() -> Unit)? = null,
    interactive: Boolean = false,
    insidePadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduceMotion by com.aven.app.core.Prefs.reduceMotion.collectAsState()
    Card(
        modifier = modifier,
        cornerRadius = corner,
        colors = avenCardColors(container),
        pressFeedbackType = if (interactive && !reduceMotion) PressFeedbackType.Tilt else PressFeedbackType.None,
        onClick = onClick,
    ) {
        Column(Modifier.padding(insidePadding), content = content)
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier, startPadding: Dp = 0.dp) {
    val c = LocalAven.current
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = startPadding)
            .height(0.7.dp)
            .background(c.hairline)
    )
}

@Composable
fun ScreenTitle(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = LocalAven.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = if (onBack != null) 8.dp else 22.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MiuixIcons.ChevronBackward, "返回", Modifier.size(20.dp), tint = c.ink)
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = if (onBack != null) 20.sp else 28.sp,
                lineHeight = if (onBack != null) 24.sp else 32.sp,
                fontWeight = FontWeight.Bold,
                color = c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = c.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        actions()
    }
}

@Composable
fun SectionLabel(
    text: String,
    trailing: String? = null,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    val c = LocalAven.current
    Row(
        modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 22.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (accent != null) {
            Box(Modifier.size(5.dp).clip(RoundedCornerShape(2.dp)).background(accent))
            Spacer(Modifier.width(7.dp))
        }
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = c.secondary, modifier = Modifier.weight(1f))
        if (!trailing.isNullOrBlank()) Text(trailing, fontSize = 11.sp, color = c.secondary.copy(alpha = 0.75f))
    }
}

@Composable
fun IconChip(icon: ImageVector, tint: Color, bg: Color, size: Dp = 36.dp, iconSize: Dp = 18.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size / 2.8f)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(iconSize), tint = tint)
    }
}

@Composable
fun EntryRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = LocalAven.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconChip(icon, tint ?: c.ink, (tint ?: c.ink).copy(alpha = 0.10f))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!summary.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(summary, fontSize = 11.5.sp, color = c.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) trailing() else if (onClick != null) {
            Icon(MiuixIcons.ChevronForward, null, Modifier.size(16.dp), tint = c.secondary.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun ValueRow(title: String, value: String, onClick: (() -> Unit)? = null, accent: Color? = null) {
    val c = LocalAven.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 14.5.sp, color = c.ink, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = accent ?: c.secondary, fontWeight = FontWeight.Medium)
        if (onClick != null) {
            Spacer(Modifier.width(4.dp))
            Icon(MiuixIcons.ChevronForward, null, Modifier.size(15.dp), tint = c.secondary.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun ToggleRow(title: String, summary: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = LocalAven.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.5.sp, color = c.ink)
            if (!summary.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(summary, fontSize = 11.5.sp, color = c.secondary)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun Badge(text: String, color: Color, background: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(8.dp)).background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = LocalAven.current
    top.yukonga.miuix.kmp.basic.Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        cornerRadius = 16.dp,
        minWidth = 0.dp,
        minHeight = 46.dp,
        colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.buttonColors(
            color = c.ink,
            disabledColor = c.neutralBg,
            contentColor = c.card,
            disabledContentColor = c.secondary,
        ),
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val c = LocalAven.current
    val fg = color ?: c.ink
    top.yukonga.miuix.kmp.basic.TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        cornerRadius = 16.dp,
        minWidth = 0.dp,
        minHeight = 46.dp,
        colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColors(
            color = fg.copy(alpha = 0.08f),
            textColor = fg,
        ),
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    desc: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val c = LocalAven.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(c.neutralBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(28.dp), tint = c.secondary)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = c.ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            desc, fontSize = 12.5.sp, color = c.secondary, lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (action != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            PrimaryButton(action, onAction)
        }
    }
}

@Composable
fun StatTile(label: String, value: String, unit: String? = null, accent: Color? = null, modifier: Modifier = Modifier) {
    val c = LocalAven.current
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = accent ?: c.ink, maxLines = 1)
            if (unit != null) {
                Spacer(Modifier.width(2.dp))
                Text(unit, fontSize = 11.sp, color = c.secondary, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 11.sp, color = c.secondary, maxLines = 1)
    }
}

@Composable
fun AnimatedCardContainer(content: @Composable () -> Unit) {
    Box(Modifier.animateContentSize(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow))) {
        content()
    }
}

@Composable
fun PagePadding(): PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)


/** 统一的输入行 标签在上 输入框在下 避免大字占位符 */
@Composable
fun LabeledInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    enabled: Boolean = true,
) {
    val c = LocalAven.current
    Column(modifier) {
        Text(label, fontSize = 9.5.sp, color = c.secondary, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        top.yukonga.miuix.kmp.basic.TextField(
            value = androidx.compose.ui.text.input.TextFieldValue(
                value,
                androidx.compose.ui.text.TextRange(value.length),
            ),
            onValueChange = { input ->
                onChange(if (numeric) input.text.filter { ch -> ch.isDigit() || ch == '.' } else input.text)
            },
            modifier = Modifier.fillMaxWidth(),
            label = "",
            enabled = enabled,
            singleLine = true,
            insideMargin = androidx.compose.ui.unit.DpSize(10.dp, 6.dp),
            cornerRadius = 10.dp,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.5.sp),
            colors = top.yukonga.miuix.kmp.basic.TextFieldDefaults.textFieldColors(
                backgroundColor = c.neutralBg,
                labelColor = c.secondary,
                borderColor = c.ink,
            ),
        )
    }
}

@Composable
fun progressFraction(target: Float): Float {
    val v by animateFloatAsState(target, spring(dampingRatio = 0.85f, stiffness = 380f), label = "frac")
    return v
}
