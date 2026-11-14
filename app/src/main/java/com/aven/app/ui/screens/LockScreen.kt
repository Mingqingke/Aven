package com.aven.app.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aven.app.core.Prefs
import com.aven.app.core.lock.AppLock
import com.aven.app.core.lock.LockPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults

/**
 * 应用锁界面
 * 视觉参考 Face ID 的深色沉浸布局 实际认证由系统 BiometricPrompt 完成
 */
@Composable
fun AppLockScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val reduce by Prefs.reduceMotion.collectAsState()
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("正在验证") }
    var success by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val ring = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val face = remember { Animatable(1f) }
    val scale = remember { Animatable(1f) }

    val breath = if (reduce) 0.8f else {
        val t = rememberInfiniteTransition(label = "breath")
        val v by t.animateFloat(
            initialValue = 0.55f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Reverse),
            label = "ringAlpha",
        )
        v
    }

    fun complete() {
        if (success) return
        success = true
        status = "已解锁"
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            face.animateTo(0f, tween(240))
            ring.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            check.animateTo(1f, tween(430, easing = FastOutSlowInEasing))
            scale.animateTo(1.04f, tween(160))
            scale.animateTo(1f, tween(220))
            delay(620)
            AppLock.unlock()
        }
    }

    fun authenticate() {
        val activity = context as? FragmentActivity ?: run {
            status = "无法启动系统认证"
            return
        }
        if (!LockPrefs.canBiometric(context)) {
            status = "设备未录入面容"
            return
        }
        status = "正在验证"
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    complete()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        showPassword = true
                    } else {
                        status = "已取消 可重试"
                    }
                }

                override fun onAuthenticationFailed() {
                    status = "验证未通过"
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("面容识别")
            .setSubtitle("解锁 Aven")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("使用密码")
            .build()
        runCatching { prompt.authenticate(info) }.onFailure { status = "无法启动系统认证" }
    }

    LaunchedEffect(Unit) {
        delay(450)
        if (LockPrefs.biometric(context)) authenticate() else showPassword = true
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF050607))) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
        val ringSize = minOf(300.dp, maxWidth * 0.7f, maxHeight * 0.36f)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Aven", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.55f))
            }
            Spacer(Modifier.weight(0.6f))
            Canvas(Modifier.size(ringSize).graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
                val cx = size.width / 2
                val cy = size.height / 2
                val radius = size.minDimension / 2 - 10.dp.toPx()
                val center = Offset(cx, cy)
                val successColor = Color(0xFF4CC38A)

                val faceAlpha = (1f - face.value).coerceIn(0f, 1f)
                if (faceAlpha > 0.01f) {
                    val headW = radius * 0.62f
                    val headH = radius * 0.8f
                    drawOval(
                        color = Color.White.copy(alpha = 0.3f * faceAlpha),
                        topLeft = Offset(cx - headW / 2, cy - headH / 2 - radius * 0.06f),
                        size = Size(headW, headH),
                        style = Stroke(width = 1.6.dp.toPx()),
                    )
                    drawArc(
                        color = Color.White.copy(alpha = 0.3f * faceAlpha),
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(cx - radius * 0.44f, cy + radius * 0.42f),
                        size = Size(radius * 0.88f, radius * 0.6f),
                        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                val spacing = 360f / 64f
                val sweep = spacing * (0.58f + 0.42f * ring.value)
                val color = if (success) successColor else Color.White
                for (i in 0 until 64) {
                    drawArc(
                        color = color.copy(alpha = if (success) 0.9f else breath * 0.7f),
                        startAngle = i * spacing - 90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                if (ring.value > 0.01f && ring.value < 0.99f) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.85f * (1f - ring.value)),
                        startAngle = -90f + 360f * ring.value,
                        sweepAngle = 26f,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
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
                        style = Stroke(
                            width = 4.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (success) "已解锁" else "面容识别",
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (success) "欢迎回来" else status,
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = if (success) 0.8f else 0.55f),
            )
            Spacer(Modifier.weight(1f))

            if (!success) {
                if (!LockPrefs.hasPassword(context)) {
                    Text(
                        "未设置密码 请前往设置开启应用锁",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.45f),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "打开 Aven",
                        fontSize = 14.sp,
                        color = Color(0xFF6EA8FF),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 22.dp, vertical = 11.dp)
                            .clickableNoRipple { AppLock.unlock() },
                    )
                } else {
                    if (LockPrefs.biometric(context)) {
                        Text(
                            "重试面容",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.clickableNoRipple { authenticate() }.padding(vertical = 6.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        "使用密码",
                        fontSize = 13.5.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickableNoRipple {
                                error = ""
                                showPassword = true
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }
            Spacer(Modifier.height(38.dp))
        }
        }

        if (showPassword && !success) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF111417))
                    .padding(22.dp),
            ) {
                Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("输入密码", fontSize = 14.sp, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    TextField(
                        value = TextFieldValue(password, TextRange(password.length)),
                        onValueChange = {
                            password = it.text.filter { ch -> ch.isDigit() }.take(6)
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = "",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        insideMargin = androidx.compose.ui.unit.DpSize(14.dp, 11.dp),
                        cornerRadius = 14.dp,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, color = Color.White),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color(0xFF1C2126),
                            labelColor = Color(0xFF8E949C),
                            borderColor = Color(0xFF1C2126),
                        ),
                    )
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, fontSize = 11.sp, color = Color(0xFFFF8A80))
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "取消",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .clickableNoRipple { showPassword = false }
                                .padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Text(
                            if (busy) "验证中" else "解锁",
                            fontSize = 13.sp,
                            color = Color(0xFF06121F),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF6EA8FF))
                                .clickableNoRipple {
                                    if (busy || password.length != 6) {
                                        error = if (password.length != 6) "请输入 6 位密码" else error
                                        return@clickableNoRipple
                                    }
                                    busy = true
                                    scope.launch {
                                        val result = withContext(Dispatchers.Default) {
                                            AppLock.verifyPassword(context, password)
                                        }
                                        busy = false
                                        when (result) {
                                            is AppLock.VerifyResult.Ok -> complete()
                                            is AppLock.VerifyResult.Wrong -> {
                                                error = "密码错误 还可尝试 ${result.remaining} 次"
                                                password = ""
                                            }
                                            is AppLock.VerifyResult.Blocked -> {
                                                error = "尝试过多 请 ${result.seconds} 秒后再试"
                                                password = ""
                                            }
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
            indication = null,
            onClick = onClick,
        )
    )
