package com.savr.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZonedDateTime

/** Teks branding. Ubah di sini untuk memperbarui halaman About. */
object Branding {
    const val TAGLINE = "Tabungan yang mengalir, tujuan yang jelas."
    const val HYPRICE_TITLE = "Tentang Hyprice"
    const val HYPRICE_TEXT = "SAVR dikembangkan oleh Hyprice."

    /** Tambahkan pasangan (label, isi) untuk situs, kontak, dll. Baris hanya tampil jika daftar ini terisi. */
    val HYPRICE_DETAILS: List<Pair<String, String>> = emptyList()
}

// ====================================================================== Host

@Composable
fun SheetHost(overlay: Overlay?, ui: UiModel?, vm: SavrViewModel) {
    BackHandler(enabled = overlay != null) { vm.dismiss() }
    val scrim by animateFloatAsState(if (overlay != null) 1f else 0f, tween(220), label = "scrim")
    Box(Modifier.fillMaxSize()) {
        if (scrim > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrim }
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) { detectTapGestures { vm.dismiss() } }
            )
        }
        AnimatedContent(
            targetState = overlay,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                when {
                    initialState == null ->
                        (slideInVertically(spring(dampingRatio = 0.78f, stiffness = 380f)) { it / 2 } + fadeIn(tween(160))) togetherWith fadeOut(tween(1))
                    targetState == null ->
                        fadeIn(tween(1)) togetherWith (slideOutVertically(tween(200)) { it / 2 } + fadeOut(tween(180)))
                    else -> fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                }
            },
            contentAlignment = Alignment.BottomCenter,
            contentKey = { it?.key },
            label = "sheet"
        ) { o ->
            if (o != null && ui != null) {
                SheetFrame {
                    when (o) {
                        is Overlay.TxEditor -> TxEditorSheet(o, ui, vm)
                        is Overlay.TxDetail -> TxDetailSheet(o, ui, vm)
                        is Overlay.GoalEditor -> GoalEditorSheet(o, ui, vm)
                        is Overlay.Confirm -> ConfirmSheet(o, vm)
                        is Overlay.About -> AboutSheet(vm)
                    }
                }
            } else {
                Spacer(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun SheetFrame(content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    BoxWithConstraints(
        Modifier.fillMaxSize().navigationBarsPadding().imePadding().padding(10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val maxH = maxOf(maxHeight - statusTop - 12.dp, 240.dp)
        Glass(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .heightIn(max = maxH)
                .pointerInput(Unit) { detectTapGestures { } },
            corner = 34.dp, floating = true, tint = p.accent
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), content = content)
        }
    }
}

@Composable
private fun SheetTitle(title: String, vm: SavrViewModel) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GText(title, 20.sp, FontWeight.Bold, p.text, modifier = Modifier.weight(1f), maxLines = 2)
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(p.track).bounceClick(true, 0.85f) { vm.dismiss() },
            contentAlignment = Alignment.Center
        ) { Ico(Icons.Rounded.Close, p.text, 18.dp) }
    }
    Gap(16.dp)
}

@Composable
private fun Label(text: String) {
    GText(text, 11.sp, FontWeight.Medium, LocalPalette.current.textDim, spacing = 0.5.sp)
    Gap(6.dp)
}

// ====================================================================== Editor transaksi

@Composable
private fun TxEditorSheet(o: Overlay.TxEditor, ui: UiModel, vm: SavrViewModel) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val e = o.editing
    var type by rememberSaveable { mutableStateOf(e?.type ?: o.preset) }
    var amount by rememberSaveable { mutableStateOf(e?.amount?.toString() ?: "") }
    var category by rememberSaveable { mutableStateOf(e?.category ?: Categories.default(e?.type ?: o.preset)) }
    var goalId by rememberSaveable {
        mutableStateOf<Long?>(if (e != null) e.goalId else (o.presetGoalId ?: ui.mainGoal?.goal?.id))
    }
    var note by rememberSaveable { mutableStateOf(e?.note ?: "") }
    var whenMs by rememberSaveable { mutableStateOf(e?.createdAt ?: System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }
    val available = remember(ui.tx, goalId, e?.id) { Rules.bucketBalance(ui.tx, goalId, e?.id) }

    SheetTitle(if (e == null) "Tambah transaksi" else "Edit transaksi", vm)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GChip("Pemasukan", type == TYPE_IN, {
            type = TYPE_IN
            if (!Categories.isValid(category, TYPE_IN)) category = Categories.default(TYPE_IN)
        }, Modifier.weight(1f))
        GChip("Pengeluaran", type == TYPE_OUT, {
            type = TYPE_OUT
            if (!Categories.isValid(category, TYPE_OUT)) category = Categories.default(TYPE_OUT)
        }, Modifier.weight(1f))
    }
    Gap(14.dp)

    GField(
        amount, { amount = it.filter { c -> c.isDigit() }.take(12).trimStart('0') }, "Nominal",
        keyboard = KeyboardType.Number, prefix = "Rp", placeholder = "0", visual = ThousandsTransformation
    )
    Gap(8.dp)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(10_000L to "+10rb", 50_000L to "+50rb", 100_000L to "+100rb", 500_000L to "+500rb").forEach { (inc, label) ->
            GChip(label, false, {
                amount = ((Rules.parseAmount(amount) ?: 0L) + inc).coerceAtMost(Rules.MAX_AMOUNT).toString()
            })
        }
    }
    Gap(14.dp)

    Label("Kategori")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Categories.forType(type).forEach { c -> GChip(c.label, category == c.key, { category = c.key }) }
    }
    Gap(14.dp)

    Label("Goal")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GChip("Tanpa goal", goalId == null, { goalId = null })
        ui.goals.forEach { gs -> GChip(gs.goal.name, goalId == gs.goal.id, { goalId = gs.goal.id }) }
    }
    if (type == TYPE_OUT) {
        Gap(6.dp)
        GText("Saldo tersedia: ${Fmt.rupiah(available)}", 12.sp, FontWeight.Medium, p.textDim)
    }
    Gap(14.dp)

    GField(note, { note = it.take(Rules.MAX_NOTE) }, "Catatan (opsional)", placeholder = "mis. gaji bulan ini")
    Gap(14.dp)

    Label("Tanggal & waktu")
    GlassButton(
        Fmt.dateTime(whenMs),
        {
            val cur = Instant.ofEpochMilli(whenMs).atZone(Fmt.zone)
            pickDate(ctx, cur.toLocalDate()) { d ->
                val now = ZonedDateTime.now(Fmt.zone)
                var z = d.atTime(cur.toLocalTime()).atZone(Fmt.zone)
                if (z.isAfter(now)) z = now
                whenMs = z.toInstant().toEpochMilli()
            }
        },
        Modifier.fillMaxWidth(), primary = false,
        leading = { c -> Ico(Icons.Rounded.DateRange, c, 18.dp) }
    )

    error?.let {
        Gap(12.dp)
        GText(it, 13.sp, FontWeight.Medium, p.danger)
    }
    Gap(18.dp)
    GlassButton(if (e == null) "Simpan" else "Simpan perubahan", {
        val amt = Rules.parseAmount(amount)
        error = if (amt == null) "Masukkan nominal yang valid." else vm.saveTransaction(e?.id, type, amt, category, goalId, note, whenMs)
    }, Modifier.fillMaxWidth())
    if (e != null) {
        Gap(10.dp)
        GlassButton("Hapus transaksi", { askDeleteTx(vm, e) }, Modifier.fillMaxWidth(), primary = false, danger = true,
            leading = { c -> Ico(Icons.Rounded.Delete, c, 18.dp) })
    }
}

private fun askDeleteTx(vm: SavrViewModel, t: SavingsTransaction) {
    vm.show(
        Overlay.Confirm(
            title = "Hapus transaksi?",
            message = "${Fmt.rupiah(t.amount)} — ${t.note}\nSaldo akan dihitung ulang. Tindakan ini tidak bisa dibatalkan.",
            confirmLabel = "Hapus",
            danger = true,
            onConfirm = { vm.deleteTransaction(t) }
        )
    )
}

// ====================================================================== Detail transaksi

@Composable
private fun DetailRow(label: String, value: String) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        GText(label, 13.sp, FontWeight.Medium, p.textDim, modifier = Modifier.widthIn(min = 92.dp, max = 92.dp))
        GText(value, 14.sp, FontWeight.SemiBold, p.text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TxDetailSheet(o: Overlay.TxDetail, ui: UiModel, vm: SavrViewModel) {
    val p = LocalPalette.current
    val t = o.tx
    val inc = t.type == TYPE_IN
    val c = if (inc) p.income else p.expense
    SheetTitle("Detail transaksi", vm)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(c.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Glyph(if (inc) Gl.In else Gl.Out, c, 26.dp)
        }
        Gap(10.dp)
        GText((if (inc) "+" else "−") + Fmt.rupiah(t.amount), 26.sp, FontWeight.ExtraBold, c, align = TextAlign.Center)
        GText(if (inc) "Pemasukan" else "Pengeluaran", 12.sp, FontWeight.Medium, p.textDim)
    }
    Gap(14.dp)
    DetailRow("Catatan", t.note)
    DetailRow("Kategori", Categories.label(t.category))
    DetailRow("Goal", t.goalId?.let { ui.goalNames[it] } ?: "Tanpa goal")
    DetailRow("Waktu", Fmt.dateTime(t.createdAt))
    Gap(16.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton("Edit", { vm.show(Overlay.TxEditor(t, t.type, t.goalId)) }, Modifier.weight(1f),
            leading = { col -> Ico(Icons.Rounded.Edit, col, 18.dp) })
        GlassButton("Hapus", { askDeleteTx(vm, t) }, Modifier.weight(1f), primary = false, danger = true,
            leading = { col -> Ico(Icons.Rounded.Delete, col, 18.dp) })
    }
}

// ====================================================================== Editor goal

@Composable
private fun GoalEditorSheet(o: Overlay.GoalEditor, ui: UiModel, vm: SavrViewModel) {
    val p = LocalPalette.current
    val g = o.goal
    val stat = g?.let { gg -> ui.goals.firstOrNull { it.goal.id == gg.id } }
    var name by rememberSaveable { mutableStateOf(g?.name ?: "") }
    var target by rememberSaveable { mutableStateOf(g?.target?.toString() ?: "") }
    var main by rememberSaveable { mutableStateOf(stat?.goal?.isMain ?: ui.goals.isEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val locked = (stat?.goal?.isMain == true) || (g == null && ui.goals.isEmpty())

    SheetTitle(if (g == null) "Goal baru" else "Edit goal", vm)

    if (stat != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                GText(Fmt.rupiahFit(stat.saved, 18), 18.sp, FontWeight.Bold, p.text, maxLines = 1)
                GText(
                    if (stat.remaining == 0L) "Target tercapai" else "Sisa ${Fmt.rupiahFit(stat.remaining, 20)}",
                    12.sp, FontWeight.Normal, p.textDim, maxLines = 1
                )
            }
            ProgressRing(stat.progress, 56.dp)
        }
        Gap(10.dp)
        LiquidBar(stat.progress)
        Gap(16.dp)
    }

    GField(name, { name = it.take(Rules.MAX_GOAL) }, "Nama goal", placeholder = "mis. Dana darurat")
    Gap(12.dp)
    GField(
        target, { target = it.filter { c -> c.isDigit() }.take(12).trimStart('0') }, "Target nominal",
        keyboard = KeyboardType.Number, prefix = "Rp", placeholder = "0", visual = ThousandsTransformation
    )
    Gap(14.dp)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            GText("Jadikan goal utama", 14.sp, FontWeight.SemiBold, p.text)
            GText(if (locked) "Goal ini sudah menjadi goal utama." else "Tampil di kartu utama Home.", 11.sp, FontWeight.Normal, p.textDim)
        }
        GapW(12.dp)
        GSwitch(main, { main = it }, enabled = !locked)
    }

    error?.let {
        Gap(12.dp)
        GText(it, 13.sp, FontWeight.Medium, p.danger)
    }
    Gap(18.dp)
    GlassButton(if (g == null) "Buat goal" else "Simpan perubahan", {
        error = vm.saveGoal(g, name, Rules.parseAmount(target), main)
    }, Modifier.fillMaxWidth())

    if (g != null) {
        Gap(10.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton("Setor", { vm.show(Overlay.TxEditor(null, TYPE_IN, g.id)) }, Modifier.weight(1f), primary = false,
                leading = { c -> Glyph(Gl.In, c, 18.dp) })
            GlassButton("Hapus goal", {
                vm.show(
                    Overlay.Confirm(
                        title = "Hapus goal \"${g.name}\"?",
                        message = "Transaksi tidak ikut dihapus. Saldo goal ini (${Fmt.rupiah(stat?.saved ?: 0L)}) dipindahkan ke \"Tanpa goal\".",
                        confirmLabel = "Hapus goal",
                        danger = true,
                        onConfirm = { vm.deleteGoal(g) }
                    )
                )
            }, Modifier.weight(1f), primary = false, danger = true, leading = { c -> Ico(Icons.Rounded.Delete, c, 18.dp) })
        }
    }
}

// ====================================================================== Konfirmasi & About

@Composable
private fun ConfirmSheet(o: Overlay.Confirm, vm: SavrViewModel) {
    val p = LocalPalette.current
    SheetTitle(o.title, vm)
    GText(o.message, 14.sp, FontWeight.Normal, p.textDim)
    Gap(20.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton("Batal", { vm.dismiss() }, Modifier.weight(1f), primary = false)
        GlassButton(o.confirmLabel, { o.onConfirm() }, Modifier.weight(1f), primary = !o.danger, danger = o.danger)
    }
}

@Composable
private fun AboutSheet(vm: SavrViewModel) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: ""
    }
    SheetTitle("Tentang", vm)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Glass(Modifier.size(84.dp), corner = 28.dp, tint = p.cyan) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GText("S", 44.sp, FontWeight.Black, p.cyan) }
        }
        Gap(12.dp)
        GText("SAVR", 24.sp, FontWeight.ExtraBold, p.text, spacing = 3.sp)
        GText("Versi $version", 12.sp, FontWeight.Medium, p.textDim)
        Gap(6.dp)
        GText(Branding.TAGLINE, 13.sp, FontWeight.Normal, p.textDim, align = TextAlign.Center)
    }
    Gap(18.dp)
    Glass(Modifier.fillMaxWidth(), corner = 22.dp) {
        Column(Modifier.padding(16.dp)) {
            GText(Branding.HYPRICE_TITLE, 14.sp, FontWeight.Bold, p.text)
            Gap(6.dp)
            GText(Branding.HYPRICE_TEXT, 13.sp, FontWeight.Normal, p.textDim)
            Branding.HYPRICE_DETAILS.forEach { (label, value) ->
                Gap(8.dp)
                GText(label, 11.sp, FontWeight.Medium, p.textDim)
                GText(value, 13.sp, FontWeight.SemiBold, p.text)
            }
        }
    }
    Gap(12.dp)
    Glass(Modifier.fillMaxWidth(), corner = 22.dp) {
        Column(Modifier.padding(16.dp)) {
            GText("Privasi", 14.sp, FontWeight.Bold, p.text)
            Gap(6.dp)
            GText(
                "Seluruh data tersimpan di perangkat ini. SAVR tidak meminta izin apa pun dan tidak terhubung ke internet. " +
                    "Gunakan Profile > Backup untuk menyimpan salinan data.",
                13.sp, FontWeight.Normal, p.textDim
            )
        }
    }
}
