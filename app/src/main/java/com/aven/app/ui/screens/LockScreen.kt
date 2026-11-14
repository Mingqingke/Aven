package com.aven.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aven.app.core.Prefs
import com.aven.app.core.lock.AppLock
import com.aven.app.core.lock.LockPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text

/**
 * 应用锁 密码独立界面
 * 六位密码输入 满六位自动验证 成功后圆环收敛并逐笔绘制对勾
 */
@Composable
fun AppLockScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val reduce by Prefs.reduceMotion.collectAsState()
    val scope = rememberCoroutineScope()

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }

    val ring = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    fun complete() {
        if (success) return
        success = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            ring.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            check.animateTo(1f, tween(430, easing = FastOutSlowInEasing))
            scale.animateTo(1.04f, tween(160))
            scale.animateTo(1f, tween(220))
            delay(640)
            AppLock.unlock()
        }
    }

    fun submit(value: String) {
        if (busy || success) return
        busy = true
        error = ""
        scope.launch {
            val result = withContext(Dispatchers.Default) { AppLock.verifyPassword(context, value) }
            busy = false
            when (result) {
                is AppLock.VerifyResult.Ok -> complete()
                is AppLock.VerifyResult.Wrong -> {
                    error = "密码错误 还可尝试 ${result.remaining} 次"
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    shake.snapTo(0f)
                    shake.animateTo(1f, tween(320))
                    pin = ""
                }
                is AppLock.VerifyResult.Blocked -> {
                    error = "尝试过多 请 ${result.seconds} 秒后再试"
                    pin = ""
                }
            }
        }
    }

    LaunchedEffect(pin) {
        if (pin.length == 6) submit(pin)
    }

    val shakeX = if (reduce) 0f else kotlin.math.sin(shake.value * Math.PI * 5f).toFloat() * 7f * (1f - shake.value)

    Box(Modifier.fillMaxSize().background(Color(0xFF050607))) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(42.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("Aven", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.55f))
            }
            Spacer(Modifier.weight(0.55f))

            if (!success) {
                Text("输入密码", fontSize = 23.sp, fontWeight = FontWeight.Medium, color = Color.White)
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.graphicsLayer { translationX = shakeX },
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    repeat(6) { index ->
                        val filled = index < pin.length
                        Box(
                            Modifier.size(12.dp).clip(CircleShape)
                                .background(if (filled) Color.White else Color.White.copy(alpha = 0.18f)),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    when {
                        busy -> "正在验证"
                        error.isNotBlank() -> error
                        else -> "请输入 6 位密码"
                    },
                    fontSize = 11.5.sp,
                    color = if (error.isNotBlank()) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.55f),
                )
                Spacer(Modifier.height(34.dp))
                Keypad(
                    onDigit = {
                        if (!busy && pin.length < 6) {
                            pin += it
                            error = ""
                        }
                    },
                    onDelete = { if (!busy && pin.isNotEmpty()) pin = pin.dropLast(1) },
                )
                Spacer(Modifier.weight(0.7f))
                if (!LockPrefs.hasPassword(context)) {
                    Text("尚未设置密码 请先在设置中开启应用锁", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.4f))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "打开 Aven",
                        fontSize = 13.sp,
                        color = Color(0xFF6EA8FF),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .noRipple { AppLock.unlock() }
                            .padding(horizontal = 22.dp, vertical = 10.dp),
                    )
                }
                Spacer(Modifier.height(26.dp))
            } else {
                Canvas(Modifier.size(260.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val radius = size.minDimension / 2 - 8.dp.toPx()
                    val successColor = Color(0xFF4CC38A)
                    val spacing = 360f / 64f
                    val sweep = spacing * (0.58f + 0.42f * ring.value)
                    for (i in 0 until 64) {
                        drawArc(
                            color = successColor.copy(alpha = 0.9f),
                            startAngle = i * spacing - 90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(cx - radius, cy - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    if (check.value > 0f) {
                        val path = Path().apply {
                            moveTo(cx - radius * 0.26f, cy + radius * 0.02f)
                            lineTo(cx - radius * 0.07f, cy + radius * 0.2f)
                            lineTo(cx + radius * 0.28f, cy - radius * 0.18f)
                        }
                        val measure = PathMeasure()
                        measure.setPath(path, false)
                        val len = measure.length
                        val partial = Path()
                        measure.getSegment(0f, len * check.value, partial, true)
                        drawPath(
                            partial,
                            color = successColor,
                            style = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("已解锁", fontSize = 23.sp, fontWeight = FontWeight.Medium, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("欢迎回来", fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.weight(0.8f))
            }
        }
    }
}

@Composable
private fun Keypad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫"),
    )
    Column(
        Modifier.widthIn(max = 300.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier.weight(1f).height(64.dp).clip(CircleShape)
                            .background(if (key.isBlank()) Color.Transparent else Color.White.copy(alpha = 0.07f))
                            .then(
                                if (key.isBlank()) Modifier
                                else Modifier.noRipple { if (key == "⌫") onDelete() else onDigit(key) }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key.isNotBlank()) {
                            Text(
                                key,
                                fontSize = if (key == "⌫") 18.sp else 25.sp,
                                fontWeight = if (key == "⌫") FontWeight.Normal else FontWeight.Medium,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.noRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = MutableInteractionSource(),
            indication = null,
            onClick = onClick,
        )
    )
