package com.savr.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Grafik garis halus + area gradien. Ketuk grafik untuk memilih titik. */
@Composable
fun LineChart(values: List<Long>, labels: List<String>, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val n = values.size
    var sel by remember(values) { mutableIntStateOf(n - 1) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(values) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }
    Column(modifier) {
        if (n > 0) {
            val i = sel.coerceIn(0, n - 1)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GText(labels[i], 12.sp, FontWeight.Medium, p.textDim, modifier = Modifier.weight(1f))
                GText(Fmt.rupiah(values[i]), 14.sp, FontWeight.Bold, p.text, maxLines = 1)
            }
        }
        Gap(10.dp)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .pointerInput(values) {
                    detectTapGestures { o ->
                        if (n > 1) sel = ((o.x / size.width) * (n - 1)).roundToInt().coerceIn(0, n - 1)
                    }
                }
        ) {
            val padX = 6.dp.toPx()
            val padTop = 10.dp.toPx()
            val padBottom = 8.dp.toPx()
            val w = size.width - padX * 2f
            val h = size.height - padTop - padBottom
            for (g in 0..3) {
                val y = padTop + h * g / 3f
                drawLine(p.text.copy(alpha = 0.07f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
            if (n == 0) return@Canvas
            val lo = min(values.min(), 0L)
            val hi = max(values.max(), 1L)
            val span = (hi - lo).toFloat()
            val k = reveal.value.coerceIn(0f, 1f)
            val base = padTop + h
            fun px(i: Int): Float = if (n == 1) padX + w / 2f else padX + w * i / (n - 1)
            fun py(v: Long): Float {
                val real = base - h * ((v - lo).toFloat() / span)
                return base + (real - base) * k
            }
            val line = Path()
            for (i in 0 until n) {
                val x = px(i)
                val y = py(values[i])
                if (i == 0) line.moveTo(x, y) else {
                    val cx = (px(i - 1) + x) / 2f
                    line.cubicTo(cx, py(values[i - 1]), cx, y, x, y)
                }
            }
            val area = Path()
            area.addPath(line)
            area.lineTo(px(n - 1), base)
            area.lineTo(px(0), base)
            area.close()
            drawPath(area, Brush.verticalGradient(listOf(p.cyan.copy(alpha = 0.30f), p.accent.copy(alpha = 0f)), startY = padTop, endY = base))
            val stroke = Brush.horizontalGradient(listOf(p.accent, p.cyan, p.violet))
            drawPath(line, stroke, alpha = 0.18f, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
            drawPath(line, stroke, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            val i = sel.coerceIn(0, n - 1)
            val sx = px(i)
            val sy = py(values[i])
            drawLine(
                p.text.copy(alpha = 0.25f), Offset(sx, padTop), Offset(sx, base), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            drawCircle(p.cyan.copy(alpha = 0.30f), radius = 9.dp.toPx(), center = Offset(sx, sy))
            drawCircle(p.text, radius = 4.5.dp.toPx(), center = Offset(sx, sy))
        }
        if (n > 0) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0, n / 2, n - 1).distinct().forEach { GText(labels[it], 10.sp, FontWeight.Normal, p.textDim, maxLines = 1) }
            }
        }
    }
}

/** Batang pemasukan vs pengeluaran per bulan. Ketuk batang untuk melihat rincian bulan itu. */
@Composable
fun BarChart(months: List<MonthStat>, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val n = months.size
    var sel by remember(months) { mutableIntStateOf(n - 1) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(months) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 120f))
    }
    val maxV = max(1L, months.maxOfOrNull { max(it.income, it.expense) } ?: 1L)
    Column(modifier) {
        if (n > 0) {
            val m = months[sel.coerceIn(0, n - 1)]
            GText(Fmt.month(m.month), 12.sp, FontWeight.Medium, p.textDim)
            Gap(4.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GText("Masuk " + Fmt.compact(m.income), 12.sp, FontWeight.SemiBold, p.income, maxLines = 1)
                GText("Keluar " + Fmt.compact(m.expense), 12.sp, FontWeight.SemiBold, p.expense, maxLines = 1)
                GText("Bersih " + Fmt.compact(m.net), 12.sp, FontWeight.SemiBold, p.text, maxLines = 1)
            }
        }
        Gap(10.dp)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .pointerInput(months) {
                    detectTapGestures { o ->
                        if (n > 0) sel = (o.x / (size.width.toFloat() / n)).toInt().coerceIn(0, n - 1)
                    }
                }
        ) {
            if (n == 0) return@Canvas
            val slot = size.width / n
            val barW = slot * 0.26f
            val base = size.height - 4.dp.toPx()
            val top = 6.dp.toPx()
            val k = reveal.value
            val cur = sel.coerceIn(0, n - 1)
            for (i in 0 until n) {
                val cx = slot * i + slot / 2f
                if (i == cur) {
                    drawRoundRect(
                        p.text.copy(alpha = 0.08f), Offset(slot * i + 2.dp.toPx(), 0f),
                        Size(slot - 4.dp.toPx(), size.height), CornerRadius(14.dp.toPx())
                    )
                }
                val hIn = max((base - top) * (months[i].income.toFloat() / maxV) * k, 2.dp.toPx())
                val hOut = max((base - top) * (months[i].expense.toFloat() / maxV) * k, 2.dp.toPx())
                drawRoundRect(
                    Brush.verticalGradient(listOf(p.income, p.accent), startY = base - hIn, endY = base),
                    Offset(cx - barW - 2.dp.toPx(), base - hIn), Size(barW, hIn), CornerRadius(barW / 2f)
                )
                drawRoundRect(
                    Brush.verticalGradient(listOf(p.expense, p.accent2), startY = base - hOut, endY = base),
                    Offset(cx + 2.dp.toPx(), base - hOut), Size(barW, hOut), CornerRadius(barW / 2f)
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            months.forEach {
                GText(Fmt.monthShort(it.month), 10.sp, FontWeight.Normal, p.textDim, modifier = Modifier.weight(1f), align = TextAlign.Center, maxLines = 1)
            }
        }
    }
}
