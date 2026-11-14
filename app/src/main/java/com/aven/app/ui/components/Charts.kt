package com.aven.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aven.app.core.calc.ScoreEngine
import com.aven.app.core.calc.ScoreEngine.TrendPoint
import com.aven.app.ui.theme.LocalAven
import top.yukonga.miuix.kmp.basic.Text

/**
 * 折线图：自绘 Canvas，支持平滑曲线、渐变填充、逐点绘制动画与点击查看。
 * 不引入第三方图表库，避免包体积与兼容性风险。
 */
@Composable
fun TrendChart(
    points: List<TrendPoint>,
    value: (TrendPoint) -> Double,
    color: Color,
    modifier: Modifier = Modifier,
    showFill: Boolean = true,
    onTap: ((TrendPoint) -> Unit)? = null,
) {
    val c = LocalAven.current
    if (points.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("还没有可绘制的数据", fontSize = 12.sp, color = c.secondary)
        }
        return
    }
    var started by remember(points) { mutableStateOf(false) }
    LaunchedEffect(points) { started = true }
    val anim by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(680, easing = LinearEasing),
        label = "trend",
    )
    val values = points.map(value)
    val minV = values.minOrNull() ?: 0.0
    val maxV = values.maxOrNull() ?: 0.0
    val span = (maxV - minV).takeIf { it > 1e-6 } ?: 1.0
    val lo = minV - span * 0.18
    val hi = maxV + span * 0.18

    Box(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(196.dp)
                .pointerInput(points) {
                    if (onTap != null) {
                        detectTapGestures { off ->
                            val padX = 14.dp.toPx()
                            val usable = size.width - padX * 2
                            val step = if (points.size > 1) usable / (points.size - 1) else 0f
                            val idx = if (points.size == 1) 0 else ((off.x - padX) / step).toInt().coerceIn(0, points.lastIndex)
                            onTap(points[idx])
                        }
                    }
                }
        ) {
            val padX = 14.dp.toPx()
            val padTop = 20.dp.toPx()
            val padBottom = 16.dp.toPx()
            val w = size.width - padX * 2
            val h = size.height - padTop - padBottom
            fun x(i: Int) = padX + if (points.size > 1) w * i / (points.size - 1) else w / 2
            fun y(v: Double) = padTop + (h * (1f - ((v - lo) / (hi - lo)).toFloat()))

            // 底线
            drawLine(
                color = c.hairline,
                start = Offset(padX, padTop + h),
                end = Offset(padX + w, padTop + h),
                strokeWidth = 1.dp.toPx(),
            )

            val line = Path()
            val fill = Path()
            for (i in points.indices) {
                val px = x(i)
                val py = y(values[i])
                if (i == 0) {
                    line.moveTo(px, py)
                    fill.moveTo(px, py)
                } else {
                    val pxPrev = x(i - 1)
                    val pyPrev = y(values[i - 1])
                    val mid = (pxPrev + px) / 2f
                    line.cubicTo(mid, pyPrev, mid, py, px, py)
                    fill.cubicTo(mid, pyPrev, mid, py, px, py)
                }
            }
            fill.lineTo(x(points.lastIndex), padTop + h)
            fill.lineTo(x(0), padTop + h)
            fill.close()

            clipRect(right = size.width * anim) {
                if (showFill) {
                    drawPath(
                        fill,
                        brush = Brush.verticalGradient(
                            listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.02f)),
                            startY = padTop,
                            endY = padTop + h,
                        ),
                    )
                }
                drawPath(line, color, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))
                points.forEachIndexed { i, _ ->
                    val px = x(i)
                    val py = y(values[i])
                    val last = i == points.lastIndex
                    drawCircle(c.getCardColor(), radius = (if (last) 5.2f else 3.6f).dp.toPx(), center = Offset(px, py))
                    drawCircle(color, radius = (if (last) 4.0f else 2.6f).dp.toPx(), center = Offset(px, py))
                }
            }
        }
        Row(Modifier.fillMaxWidth().align(Alignment.TopStart).padding(start = 2.dp, end = 2.dp)) {
            Text(ScoreEngine.fmtScore(maxV), fontSize = 9.5.sp, color = c.secondary.copy(alpha = 0.8f))
            Spacer(Modifier.weight(1f))
            Text(ScoreEngine.fmtScore(minV), fontSize = 9.5.sp, color = c.secondary.copy(alpha = 0.8f))
        }
    }
}

private fun com.aven.app.ui.theme.AvenColors.getCardColor(): Color = card

@Composable
fun MiniSparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 34.dp,
) {
    if (values.isEmpty()) {
        Box(modifier.height(height))
        return
    }
    val minV = values.minOrNull() ?: 0.0
    val maxV = values.maxOrNull() ?: 0.0
    val span = (maxV - minV).takeIf { it > 1e-6 } ?: 1.0
    Canvas(modifier.fillMaxWidth().height(height)) {
        val pad = 3.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        fun x(i: Int) = pad + if (values.size > 1) w * i / (values.size - 1) else w / 2
        fun y(v: Double) = pad + h * (1f - ((v - (minV - span * 0.2)) / (span * 1.4)).toFloat())
        val path = Path()
        for (i in values.indices) {
            val px = x(i)
            val py = y(values[i])
            if (i == 0) path.moveTo(px, py) else {
                val mid = (x(i - 1) + px) / 2f
                path.cubicTo(mid, y(values[i - 1]), mid, py, px, py)
            }
        }
        drawPath(path, color, style = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color, 2.4.dp.toPx(), Offset(x(values.lastIndex), y(values.last())))
    }
}

@Composable
fun RateBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    track: Color? = null,
    height: Dp = 8.dp,
    animate: Boolean = true,
) {
    val c = LocalAven.current
    val target = fraction.coerceIn(0f, 1f)
    val f by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(if (animate) 560 else 0),
        label = "bar",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
            .background(track ?: c.neutralBg)
    ) {
        Box(
            Modifier
                .fillMaxWidth(f)
                .height(height)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                .background(color)
        )
    }
}

@Composable
fun RingProgress(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    stroke: Dp = 9.dp,
    track: Color? = null,
    content: @Composable () -> Unit = {},
) {
    val c = LocalAven.current
    val f by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(620),
        label = "ring",
    )
    Box(modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(96.dp)) {
            val sw = stroke.toPx()
            val inset = sw / 2
            drawArc(
                color = track ?: c.neutralBg,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f, sweepAngle = 360f * f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

/** 周完成率柱状图，用于学习复盘。 */
@Composable
fun WeekBars(
    values: List<Float>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val c = LocalAven.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            val f by animateFloatAsState(v.coerceIn(0f, 1f), tween(520), label = "week$i")
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier
                        .width(15.dp)
                        .height(72.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .background(c.neutralBg),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((72 * f).dp.coerceAtLeast(if (v > 0f) 4.dp else 0.dp))
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                            .background(if (v >= 0.999f) c.positive else color)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(labels.getOrElse(i) { "" }, fontSize = 9.5.sp, color = c.secondary)
            }
        }
    }
}

@Composable
fun SubjectBars(
    items: List<Pair<String, Float>>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val c = LocalAven.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        items.forEach { (label, f) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 11.sp, color = c.secondary, modifier = Modifier.width(46.dp))
                Box(Modifier.weight(1f)) { RateBar(f, color, height = 7.dp) }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${(f * 100).toInt()}%", fontSize = 10.5.sp, color = c.secondary,
                    fontWeight = FontWeight.Medium, modifier = Modifier.width(34.dp),
                )
            }
        }
    }
}
