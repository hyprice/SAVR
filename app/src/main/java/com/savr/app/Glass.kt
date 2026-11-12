package com.savr.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// =============================================================== Palet (adaptif terang/gelap)

@Immutable
class Palette(
    val dark: Boolean,
    val bg0: Color, val bg1: Color, val bg2: Color,
    val blobA: Color, val blobB: Color, val blobC: Color,
    val waveA: Color, val waveB: Color,
    val glassHi: Color, val glassLo: Color, val sheen: Float, val depth: Color,
    val rim: Color, val rimCyan: Color, val rimViolet: Color,
    val text: Color, val textDim: Color,
    val accent: Color, val accent2: Color, val cyan: Color, val violet: Color,
    val income: Color, val expense: Color, val danger: Color, val track: Color
)

val DarkPalette = Palette(
    dark = true,
    bg0 = Color(0xFF050B1F), bg1 = Color(0xFF0A1B4D), bg2 = Color(0xFF140B3A),
    blobA = Color(0x992F6BFF), blobB = Color(0x6638D8F5), blobC = Color(0x779B6BFF),
    waveA = Color(0x3338D8F5), waveB = Color(0x2E8B5CF6),
    glassHi = Color(0x2BFFFFFF), glassLo = Color(0x0DFFFFFF), sheen = 0.12f, depth = Color(0x40000018),
    rim = Color(0x73FFFFFF), rimCyan = Color(0xAA4DE3FF), rimViolet = Color(0x889B6BFF),
    text = Color(0xFFF4F8FF), textDim = Color(0xFFA9B9D9),
    accent = Color(0xFF2F6BFF), accent2 = Color(0xFF7A5CFF), cyan = Color(0xFF4DE3FF), violet = Color(0xFFB69BFF),
    income = Color(0xFF55E6F2), expense = Color(0xFFB69BFF), danger = Color(0xFFFF8FA8), track = Color(0x24FFFFFF)
)

val LightPalette = Palette(
    dark = false,
    bg0 = Color(0xFFEEF4FF), bg1 = Color(0xFFDDE9FF), bg2 = Color(0xFFEFE9FF),
    blobA = Color(0x662F6BFF), blobB = Color(0x6638C8E8), blobC = Color(0x55A98BFF),
    waveA = Color(0x332F6BFF), waveB = Color(0x2A8B5CF6),
    glassHi = Color(0xB3FFFFFF), glassLo = Color(0x61FFFFFF), sheen = 0.35f, depth = Color(0x142F4B9A),
    rim = Color(0xE6FFFFFF), rimCyan = Color(0x6638C8E8), rimViolet = Color(0x558B5CF6),
    text = Color(0xFF0A1740), textDim = Color(0xFF3B4C78),
    accent = Color(0xFF1F5BFF), accent2 = Color(0xFF6A4DFF), cyan = Color(0xFF0090B0), violet = Color(0xFF6B45E0),
    income = Color(0xFF007C99), expense = Color(0xFF6A3FD6), danger = Color(0xFFC62850), track = Color(0x1F0A1740)
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }
val LocalHaptics = staticCompositionLocalOf { true }
val LocalPhase = staticCompositionLocalOf<State<Float>> { mutableStateOf(0.12f) }
val LocalBgSize = compositionLocalOf { Size.Zero }

/** Warna teks/ikon otomatis agar tetap terbaca di atas warna latar apa pun. */
fun readableOn(bg: Color): Color = if (bg.luminance() > 0.40f) Color(0xFF0A1740) else Color.White

private fun Color.clear(): Color = copy(alpha = 0f)

// =============================================================== Background cair

internal fun DrawScope.drawBlobs(p: Palette, t: Float, w: Float, h: Float, mul: Float) {
    val a = t * 2f * PI.toFloat()
    fun blob(c: Color, cx: Float, cy: Float, r: Float) {
        val col = c.copy(alpha = (c.alpha * mul).coerceAtMost(1f))
        drawCircle(
            brush = Brush.radialGradient(listOf(col, col.clear()), center = Offset(cx, cy), radius = r),
            radius = r,
            center = Offset(cx, cy)
        )
    }
    blob(p.blobA, w * (0.15f + 0.10f * sin(a)), h * (0.12f + 0.05f * cos(a)), w * 0.80f)
    blob(p.blobB, w * (0.90f - 0.10f * cos(a)), h * (0.45f + 0.06f * sin(a)), w * 0.72f)
    blob(p.blobC, w * (0.30f + 0.08f * cos(a)), h * (0.88f - 0.05f * sin(a)), w * 0.85f)
}

private fun DrawScope.waveLayer(w: Float, h: Float, a: Float, baseY: Float, amp: Float, cycles: Float, k: Int, c: Color, mul: Float) {
    val steps = 36
    val top = Path()
    for (i in 0..steps) {
        val x = w * i / steps
        val y = baseY + amp * sin(cycles * 2f * PI.toFloat() * i / steps + k * a)
        if (i == 0) top.moveTo(x, y) else top.lineTo(x, y)
    }
    val fill = Path()
    fill.addPath(top)
    fill.lineTo(w, h)
    fill.lineTo(0f, h)
    fill.close()
    val col = c.copy(alpha = (c.alpha * mul).coerceAtMost(1f))
    drawPath(fill, Brush.verticalGradient(listOf(col, col.clear()), startY = baseY - amp, endY = h))
    drawPath(top, Color.White.copy(alpha = (0.10f * mul).coerceAtMost(0.5f)), style = Stroke(width = 1.2f * density))
}

internal fun DrawScope.drawWaves(p: Palette, t: Float, w: Float, h: Float, mul: Float) {
    val a = t * 2f * PI.toFloat() // k bilangan bulat -> animasi berulang mulus
    waveLayer(w, h, a, h * 0.58f, h * 0.030f, 1.2f, 1, p.waveA, mul)
    waveLayer(w, h, a, h * 0.70f, h * 0.040f, 0.9f, -2, p.waveB, mul)
    waveLayer(w, h, a, h * 0.82f, h * 0.030f, 1.6f, 3, p.waveA, mul)
}

@Composable
fun LiquidBackground() {
    val p = LocalPalette.current
    val phase = LocalPhase.current
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(p.bg0, p.bg1, p.bg2)))
        drawBlobs(p, phase.value, size.width, size.height, 1f)
        drawWaves(p, phase.value, size.width, size.height, 1f)
    }
}

// =============================================================== Panel kaca

private class CoordHolder { var c: LayoutCoordinates? = null }

internal fun DrawScope.drawGlass(p: Palette, cr: Float, tint: Color?) {
    val r = CornerRadius(cr)
    val end = Offset(size.width, size.height)
    drawRoundRect(Brush.linearGradient(listOf(p.glassHi, p.glassLo), Offset.Zero, end), cornerRadius = r)
    if (tint != null) {
        drawRoundRect(
            Brush.radialGradient(
                listOf(tint.copy(alpha = 0.30f), tint.copy(alpha = 0f)),
                center = Offset(size.width * 0.9f, size.height * 0.1f),
                radius = size.maxDimension * 0.85f
            ),
            cornerRadius = r
        )
    }
    // kilau atas (specular) + kedalaman bawah
    drawRoundRect(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = p.sheen), Color.White.copy(alpha = 0f)), startY = 0f, endY = size.height * 0.45f),
        cornerRadius = r
    )
    drawRoundRect(
        Brush.verticalGradient(listOf(p.depth.copy(alpha = 0f), p.depth), startY = size.height * 0.55f, endY = size.height),
        cornerRadius = r
    )
    // tepi: dispersi cyan -> violet + garis putih
    val sw = 1.2f * density
    val inset = sw / 2f
    val rs = Size(size.width - sw, size.height - sw)
    val rr = CornerRadius((cr - inset).coerceAtLeast(0f))
    drawRoundRect(
        Brush.linearGradient(listOf(p.rimCyan, p.rimCyan.clear(), p.rimViolet), Offset.Zero, end),
        Offset(inset, inset), rs, rr, style = Stroke(sw * 1.6f)
    )
    drawRoundRect(
        Brush.linearGradient(listOf(p.rim, p.rim.clear(), p.rim.copy(alpha = p.rim.alpha * 0.35f)), Offset.Zero, end),
        Offset(inset, inset), rs, rr, style = Stroke(sw)
    )
}

private fun DrawScope.drawFloatShadow(p: Palette, cr: Float) {
    val outline = Path()
    outline.addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(cr)))
    clipPath(outline, ClipOp.Difference) {
        val c = if (p.dark) Color(0xFF000010) else Color(0xFF2F4B9A)
        val a = if (p.dark) 0.10f else 0.05f
        for (i in 1..5) {
            val e = i * 4f * density
            drawRoundRect(
                c.copy(alpha = a),
                Offset(-e * 0.5f, e * 0.7f),
                Size(size.width + e, size.height + e * 0.6f),
                CornerRadius(cr + e / 2f)
            )
        }
    }
}

/** Refraksi: isi background digambar ulang membesar di dalam panel, sehingga kaca "membelokkan" cahaya. */
private fun DrawScope.drawRefraction(p: Palette, t: Float, bg: Size, c: LayoutCoordinates?) {
    if (c == null || !c.isAttached || bg.width <= 0f) return
    val pos = c.positionInWindow()
    withTransform({
        translate(-pos.x, -pos.y)
        scale(1.10f, 1.10f, Offset(pos.x + size.width / 2f, pos.y + size.height / 2f))
    }) {
        drawBlobs(p, t, bg.width, bg.height, 0.9f)
        drawWaves(p, t, bg.width, bg.height, 1.5f)
    }
}

@Composable
fun Glass(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    floating: Boolean = false,
    refract: Boolean = false,
    tint: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val p = LocalPalette.current
    val phase = LocalPhase.current
    val bg = LocalBgSize.current
    val holder = remember { CoordHolder() }
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .then(if (floating) Modifier.drawBehind { drawFloatShadow(p, corner.toPx()) } else Modifier)
            .then(if (refract) Modifier.onGloballyPositioned { holder.c = it } else Modifier)
            .clip(shape)
            .drawBehind {
                if (refract) drawRefraction(p, phase.value, bg, holder.c)
                drawGlass(p, corner.toPx(), tint)
            },
        content = content
    )
}

// =============================================================== Teks, ikon, spacer

@Composable
fun GText(
    text: String,
    size: TextUnit = 14.sp,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign = TextAlign.Unspecified,
    spacing: TextUnit = TextUnit.Unspecified
) {
    val c = if (color == Color.Unspecified) LocalPalette.current.text else color
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(color = c, fontSize = size, fontWeight = weight, textAlign = align, letterSpacing = spacing),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable fun Gap(h: Dp) = Spacer(Modifier.height(h))
@Composable fun GapW(w: Dp) = Spacer(Modifier.width(w))

@Composable
fun Ico(v: ImageVector, tint: Color, dim: Dp = 20.dp, modifier: Modifier = Modifier) {
    Image(imageVector = v, contentDescription = null, modifier = modifier.size(dim), colorFilter = ColorFilter.tint(tint))
}

enum class Gl { Analytics, History, In, Out, Filter, Export, Import, Target }

private fun DrawScope.ln(c: Color, x1: Float, y1: Float, x2: Float, y2: Float, s: Float, w: Float) =
    drawLine(c, Offset(x1 * s, y1 * s), Offset(x2 * s, y2 * s), w, StrokeCap.Round)

/** Ikon garis kecil (menggantikan material-icons-extended yang berat). */
@Composable
fun Glyph(g: Gl, tint: Color, dim: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val s = size.minDimension
        val w = s * 0.09f
        when (g) {
            Gl.Analytics -> {
                ln(tint, 0.24f, 0.82f, 0.24f, 0.50f, s, w * 1.6f)
                ln(tint, 0.50f, 0.82f, 0.50f, 0.22f, s, w * 1.6f)
                ln(tint, 0.76f, 0.82f, 0.76f, 0.38f, s, w * 1.6f)
            }
            Gl.History -> {
                drawCircle(tint, radius = s * 0.38f, center = center, style = Stroke(w, cap = StrokeCap.Round))
                ln(tint, 0.5f, 0.5f, 0.5f, 0.30f, s, w)
                ln(tint, 0.5f, 0.5f, 0.65f, 0.58f, s, w)
            }
            Gl.In -> {
                ln(tint, 0.75f, 0.25f, 0.25f, 0.75f, s, w)
                ln(tint, 0.25f, 0.75f, 0.25f, 0.45f, s, w)
                ln(tint, 0.25f, 0.75f, 0.55f, 0.75f, s, w)
            }
            Gl.Out -> {
                ln(tint, 0.25f, 0.75f, 0.75f, 0.25f, s, w)
                ln(tint, 0.75f, 0.25f, 0.75f, 0.55f, s, w)
                ln(tint, 0.75f, 0.25f, 0.45f, 0.25f, s, w)
            }
            Gl.Filter -> {
                ln(tint, 0.15f, 0.30f, 0.85f, 0.30f, s, w)
                ln(tint, 0.30f, 0.50f, 0.70f, 0.50f, s, w)
                ln(tint, 0.42f, 0.70f, 0.58f, 0.70f, s, w)
            }
            Gl.Export -> {
                ln(tint, 0.5f, 0.70f, 0.5f, 0.20f, s, w)
                ln(tint, 0.5f, 0.20f, 0.32f, 0.38f, s, w)
                ln(tint, 0.5f, 0.20f, 0.68f, 0.38f, s, w)
                ln(tint, 0.20f, 0.86f, 0.80f, 0.86f, s, w)
            }
            Gl.Import -> {
                ln(tint, 0.5f, 0.20f, 0.5f, 0.70f, s, w)
                ln(tint, 0.5f, 0.70f, 0.32f, 0.52f, s, w)
                ln(tint, 0.5f, 0.70f, 0.68f, 0.52f, s, w)
                ln(tint, 0.20f, 0.86f, 0.80f, 0.86f, s, w)
            }
            Gl.Target -> {
                drawCircle(tint, radius = s * 0.40f, center = center, style = Stroke(w))
                drawCircle(tint, radius = s * 0.20f, center = center, style = Stroke(w))
                drawCircle(tint, radius = s * 0.05f, center = center)
            }
        }
    }
}

// =============================================================== Interaksi bouncy

fun Modifier.bounceClick(enabled: Boolean = true, pressedScale: Float = 0.92f, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 600f),
        label = "bounce"
    )
    val haptic = LocalHapticFeedback.current
    val hapticsOn = LocalHaptics.current
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button) {
            if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

// =============================================================== Komponen

@Composable
fun GlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    danger: Boolean = false,
    enabled: Boolean = true,
    leading: (@Composable (Color) -> Unit)? = null
) {
    val p = LocalPalette.current
    val fillBrush = if (danger) Brush.linearGradient(listOf(Color(0xFFD1365B), Color(0xFFA4287F)))
    else Brush.linearGradient(listOf(p.accent, p.accent2))
    val filled = primary || danger
    val fg = if (filled) Color.White else p.text
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.5f)
            .bounceClick(enabled, 0.93f, onClick)
            .clip(shape)
            .drawBehind {
                val cr = 20.dp.toPx()
                if (filled) {
                    drawRoundRect(fillBrush, cornerRadius = CornerRadius(cr))
                    drawRoundRect(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0f)), endY = size.height * 0.5f),
                        cornerRadius = CornerRadius(cr)
                    )
                } else {
                    drawGlass(p, cr, null)
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            leading?.invoke(fg)
            GText(label, 14.sp, FontWeight.SemiBold, fg, maxLines = 1)
        }
    }
}

@Composable
fun GChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val fill by animateColorAsState(if (selected) p.accent else p.accent.copy(alpha = 0f), label = "chip")
    val fg = if (selected) readableOn(p.accent) else p.text
    Box(
        modifier
            .bounceClick(true, 0.9f, onClick)
            .clip(CircleShape)
            .drawBehind { drawGlass(p, size.height / 2f, null) }
            .background(fill)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        GText(label, 13.sp, if (selected) FontWeight.SemiBold else FontWeight.Medium, fg, maxLines = 1)
    }
}

/** Menampilkan angka dengan pemisah ribuan "." sambil menjaga posisi kursor. */
object ThousandsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val n = digits.length
        val sb = StringBuilder()
        val o2t = IntArray(n + 1)
        for (i in 0 until n) {
            if (i > 0 && (n - i) % 3 == 0) sb.append('.')
            o2t[i] = sb.length
            sb.append(digits[i])
        }
        o2t[n] = sb.length
        val t2o = IntArray(sb.length + 1)
        var cnt = 0
        for (ti in 0..sb.length) {
            t2o[ti] = cnt
            if (ti < sb.length && sb[ti] != '.') cnt++
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = o2t[offset.coerceIn(0, n)]
            override fun transformedToOriginal(offset: Int): Int = t2o[offset.coerceIn(0, sb.length)]
        }
        return TransformedText(AnnotatedString(sb.toString()), mapping)
    }
}

@Composable
fun GField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    prefix: String? = null,
    placeholder: String = "",
    visual: VisualTransformation = VisualTransformation.None,
    leading: (@Composable () -> Unit)? = null
) {
    val p = LocalPalette.current
    val focus = LocalFocusManager.current
    Column(modifier) {
        if (label.isNotEmpty()) {
            GText(label, 11.sp, FontWeight.Medium, p.textDim, spacing = 0.5.sp)
            Gap(6.dp)
        }
        Glass(Modifier.fillMaxWidth(), corner = 16.dp) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                leading?.invoke()
                if (prefix != null) {
                    GText(prefix, 15.sp, FontWeight.SemiBold, p.textDim)
                    GapW(8.dp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(color = p.text, fontSize = 16.sp),
                    cursorBrush = SolidColor(p.cyan),
                    visualTransformation = visual,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty() && placeholder.isNotEmpty()) {
                                GText(placeholder, 16.sp, FontWeight.Normal, p.textDim.copy(alpha = 0.7f), maxLines = 1)
                            }
                            inner()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GSwitch(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val p = LocalPalette.current
    val pos by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "switch")
    val haptic = LocalHapticFeedback.current
    val hapticsOn = LocalHaptics.current
    Box(
        Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .size(width = 54.dp, height = 32.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch) {
                if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onChange(it)
            }
            .clip(CircleShape)
            .drawBehind {
                drawGlass(p, size.height / 2f, null)
                if (pos > 0f) drawRoundRect(Brush.horizontalGradient(listOf(p.accent, p.accent2)), cornerRadius = CornerRadius(size.height / 2f), alpha = pos.coerceIn(0f, 1f))
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.height * 0.36f
            val x = size.height / 2f + (size.width - size.height) * pos
            drawCircle(Color.White, radius = r, center = Offset(x, size.height / 2f))
        }
    }
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        GText(title, 17.sp, FontWeight.Bold, p.text, modifier = Modifier.weight(1f), maxLines = 1)
        if (action != null) {
            GText(action, 13.sp, FontWeight.SemiBold, p.cyan, modifier = Modifier.bounceClick(true, 0.9f, onAction).padding(8.dp), maxLines = 1)
        }
    }
}

// =============================================================== Animasi cair

@Composable
fun LiquidBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    val p = LocalPalette.current
    val phase = LocalPhase.current
    val prog by animateFloatAsState(progress, spring(dampingRatio = 0.7f, stiffness = 90f), label = "bar")
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = size.height / 2f
        drawRoundRect(p.track, cornerRadius = CornerRadius(r))
        val w = size.width * prog.coerceIn(0f, 1f)
        if (w > 0.5f) {
            val clip = Path()
            clip.addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r)))
            clipPath(clip) {
                val a = phase.value * 2f * PI.toFloat()
                val amp = if (prog < 0.995f) size.height * 0.20f else 0f
                val path = Path()
                path.moveTo(0f, 0f)
                val steps = 8
                for (i in 0..steps) {
                    path.lineTo(w + amp * sin(i * 0.9f + 3f * a), size.height * i / steps)
                }
                path.lineTo(0f, size.height)
                path.close()
                drawPath(path, Brush.horizontalGradient(listOf(p.cyan, p.accent, p.violet), startX = 0f, endX = max(w, 1f)))
                drawRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0f)), endY = size.height * 0.6f),
                    size = Size(w, size.height)
                )
            }
        }
    }
}

@Composable
fun ProgressRing(progress: Float, dim: Dp, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val prog by animateFloatAsState(progress, spring(dampingRatio = 0.7f, stiffness = 90f), label = "ring")
    Box(modifier.size(dim), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = size.minDimension * 0.12f
            val d = size.minDimension - sw
            val tl = Offset(sw / 2f, sw / 2f)
            drawArc(p.track, 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
            rotate(-90f) {
                drawArc(
                    Brush.sweepGradient(listOf(p.cyan, p.accent, p.violet, p.cyan)),
                    0f, 360f * prog.coerceIn(0f, 1f), false, tl, Size(d, d),
                    style = Stroke(sw, cap = StrokeCap.Round)
                )
            }
        }
        GText("${(progress * 100).toInt()}%", (dim.value * 0.22f).sp, FontWeight.Bold, p.text, maxLines = 1)
    }
}

@Composable
fun LiquidLoader(modifier: Modifier = Modifier, dim: Dp = 64.dp) {
    val p = LocalPalette.current
    val t = rememberInfiniteTransition(label = "loader").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeat(1800), label = "t"
    )
    Canvas(modifier.size(dim)) {
        val r = size.minDimension / 2f
        val a = t.value * 2f * PI.toFloat()
        for (i in 0 until 3) {
            val ang = a + i * 2.094f
            val rr = r * (0.30f + 0.07f * sin(a * 2f + i))
            val o = Offset(center.x + cos(ang) * r * 0.45f, center.y + sin(ang) * r * 0.45f)
            val col = when (i) { 0 -> p.cyan; 1 -> p.accent; else -> p.violet }
            drawCircle(
                Brush.radialGradient(listOf(col, col.clear()), center = o, radius = rr * 1.7f),
                radius = rr * 1.7f, center = o
            )
        }
        drawCircle(p.rim, radius = r * 0.92f, style = Stroke(1.5f * density))
    }
}

private fun infiniteRepeat(ms: Int) = infiniteRepeatable<Float>(tween(ms, easing = LinearEasing))

@Composable
fun AnimatedMoney(amount: Long, size: TextUnit, weight: FontWeight, color: Color, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(0L) }
    val anim = remember { androidx.compose.animation.core.Animatable(1f) }
    androidx.compose.runtime.LaunchedEffect(amount) {
        val start = shown
        if (start != amount) {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) {
                shown = start + ((amount - start).toDouble() * value.toDouble()).toLong()
            }
            shown = amount
        }
    }
    GText(Fmt.rupiah(shown), size, weight, color, modifier = modifier, maxLines = 1)
}
