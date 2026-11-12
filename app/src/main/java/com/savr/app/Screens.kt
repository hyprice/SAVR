package com.savr.app

import android.app.DatePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.animateFloatAsState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ====================================================================== Pembantu bersama

@Composable
fun PageList(content: LazyListScope.() -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.fillMaxHeight().widthIn(max = 640.dp).fillMaxWidth(),
            state = rememberLazyListState(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = top + 10.dp, bottom = bottom + 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String? = null) {
    val p = LocalPalette.current
    Column(Modifier.padding(top = 6.dp)) {
        GText(title, 28.sp, FontWeight.Bold, p.text)
        if (subtitle != null) GText(subtitle, 13.sp, FontWeight.Normal, p.textDim)
    }
}

@Composable
private fun EmptyCard(title: String, subtitle: String, action: String? = null, onAction: () -> Unit = {}) {
    val p = LocalPalette.current
    Glass(Modifier.fillMaxWidth(), corner = 24.dp) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GText(title, 16.sp, FontWeight.Bold, p.text, align = TextAlign.Center)
            Gap(6.dp)
            GText(subtitle, 13.sp, FontWeight.Normal, p.textDim, align = TextAlign.Center)
            if (action != null) {
                Gap(14.dp)
                GlassButton(action, onAction)
            }
        }
    }
}

fun pickDate(ctx: Context, initial: LocalDate, onPick: (LocalDate) -> Unit) {
    val d = DatePickerDialog(ctx, { _, y, m, day -> onPick(LocalDate.of(y, m + 1, day)) }, initial.year, initial.monthValue - 1, initial.dayOfMonth)
    d.datePicker.maxDate = System.currentTimeMillis()
    d.show()
}

@Composable
fun TxRow(t: SavingsTransaction, goalName: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val inc = t.type == TYPE_IN
    val c = if (inc) p.income else p.expense
    Glass(modifier.fillMaxWidth().bounceClick(true, 0.97f, onClick), corner = 22.dp) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(c.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Glyph(if (inc) Gl.In else Gl.Out, c, 18.dp)
            }
            GapW(12.dp)
            Column(Modifier.weight(1f)) {
                GText(t.note, 14.sp, FontWeight.SemiBold, p.text, maxLines = 1)
                val meta = buildString {
                    append(Categories.label(t.category))
                    if (goalName != null) append(" · ").append(goalName)
                    append(" · ").append(Fmt.shortDate(t.createdAt))
                }
                GText(meta, 11.sp, FontWeight.Normal, p.textDim, maxLines = 1)
            }
            GapW(8.dp)
            GText((if (inc) "+" else "−") + Fmt.rupiahFit(t.amount, 16), 13.sp, FontWeight.Bold, c, maxLines = 1)
        }
    }
}

// ====================================================================== HOME

@Composable
fun HomePage(ui: UiModel, vm: SavrViewModel, goHistory: () -> Unit) {
    val p = LocalPalette.current
    PageList {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Glass(Modifier.size(44.dp), corner = 15.dp) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GText("S", 22.sp, FontWeight.Black, p.cyan) }
                }
                GapW(12.dp)
                Column(Modifier.weight(1f)) {
                    GText("SAVR.", 20.sp, FontWeight.ExtraBold, p.text, spacing = 2.sp)
                    GText(
                        if (ui.settings.name.isBlank()) "Tabunganmu, selalu terjaga" else "Halo, ${ui.settings.name}",
                        12.sp, FontWeight.Normal, p.textDim, maxLines = 1
                    )
                }
            }
        }
        item(key = "hero") {
            Glass(Modifier.fillMaxWidth(), corner = 32.dp, floating = true, refract = true, tint = p.accent) {
                Column(Modifier.padding(22.dp)) {
                    GText("TOTAL TABUNGAN", 11.sp, FontWeight.SemiBold, p.textDim, spacing = 2.sp)
                    Gap(8.dp)
                    AnimatedMoney(ui.balance, 34.sp, FontWeight.ExtraBold, p.text)
                    val g = ui.mainGoal
                    if (g != null) {
                        Gap(18.dp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Ico(Icons.Rounded.Star, p.cyan, 14.dp)
                                    GapW(6.dp)
                                    GText(g.goal.name, 13.sp, FontWeight.SemiBold, p.text, maxLines = 1)
                                }
                                Gap(4.dp)
                                GText(
                                    if (g.remaining == 0L) "Target tercapai 🎉" else "Sisa ${Fmt.rupiahFit(g.remaining, 18)} lagi",
                                    12.sp, FontWeight.Normal, p.textDim, maxLines = 1
                                )
                            }
                            ProgressRing(g.progress, 62.dp)
                        }
                        Gap(14.dp)
                        LiquidBar(g.progress)
                    }
                    Gap(20.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton(
                            "Tambah", { vm.show(Overlay.TxEditor(null, TYPE_IN, null)) }, Modifier.weight(1f),
                            leading = { c -> Glyph(Gl.In, c, 18.dp) }
                        )
                        GlassButton(
                            "Tarik", { vm.show(Overlay.TxEditor(null, TYPE_OUT, null)) }, Modifier.weight(1f), primary = false,
                            leading = { c -> Glyph(Gl.Out, c, 18.dp) }
                        )
                    }
                }
            }
        }
        item(key = "stats") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Masuk bulan ini", ui.monthIn, true, Modifier.weight(1f))
                StatTile("Keluar bulan ini", ui.monthOut, false, Modifier.weight(1f))
            }
        }
        item(key = "goals-title") { SectionTitle("Goals", "Tambah") { vm.show(Overlay.GoalEditor(null)) } }
        item(key = "goals") {
            if (ui.goals.isEmpty()) {
                EmptyCard("Belum ada goal", "Buat goal pertama dan tentukan target nominalnya.", "Buat goal") { vm.show(Overlay.GoalEditor(null)) }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ui.goals, key = { it.goal.id }) { gs -> GoalCard(gs) { vm.show(Overlay.GoalEditor(gs.goal)) } }
                }
            }
        }
        item(key = "recent-title") { SectionTitle("Aktivitas terbaru", if (ui.tx.isEmpty()) null else "Lihat semua", goHistory) }
        if (ui.tx.isEmpty()) {
            item(key = "recent-empty") {
                EmptyCard("Belum ada transaksi", "Catat setoran pertamamu untuk mulai menabung.", "Tambah transaksi") {
                    vm.show(Overlay.TxEditor(null, TYPE_IN, null))
                }
            }
        } else {
            items(ui.tx.take(5), key = { it.id }) { t ->
                TxRow(t, t.goalId?.let { ui.goalNames[it] }, { vm.show(Overlay.TxDetail(t)) }, Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: Long, income: Boolean, modifier: Modifier) {
    val p = LocalPalette.current
    val c = if (income) p.income else p.expense
    Glass(modifier, corner = 24.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Glyph(if (income) Gl.In else Gl.Out, c, 16.dp)
                GapW(6.dp)
                GText(label, 11.sp, FontWeight.Medium, p.textDim, maxLines = 1)
            }
            Gap(8.dp)
            GText(Fmt.rupiahFit(value, 14), 15.sp, FontWeight.SemiBold, p.text, maxLines = 1)
        }
    }
}

@Composable
private fun GoalCard(gs: GoalStat, onClick: () -> Unit) {
    val p = LocalPalette.current
    Glass(Modifier.width(210.dp).bounceClick(true, 0.96f, onClick), corner = 26.dp, tint = if (gs.goal.isMain) p.cyan else null) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GText(gs.goal.name, 14.sp, FontWeight.Bold, p.text, modifier = Modifier.weight(1f), maxLines = 1)
                if (gs.goal.isMain) Ico(Icons.Rounded.Star, p.cyan, 16.dp)
            }
            Gap(10.dp)
            GText(Fmt.rupiahFit(gs.saved, 14), 15.sp, FontWeight.SemiBold, p.text, maxLines = 1)
            GText("dari ${Fmt.compact(gs.goal.target)}", 11.sp, FontWeight.Normal, p.textDim, maxLines = 1)
            Gap(10.dp)
            LiquidBar(gs.progress, height = 10.dp)
            Gap(8.dp)
            GText(
                if (gs.remaining == 0L) "Tercapai" else "Sisa ${Fmt.rupiahFit(gs.remaining, 16)}",
                11.sp, FontWeight.Medium, p.textDim, maxLines = 1
            )
        }
    }
}

// ====================================================================== SEARCH

@Composable
fun SearchPage(ui: UiModel, vm: SavrViewModel) {
    val p = LocalPalette.current
    var q by rememberSaveable { mutableStateOf("") }
    val query = q.trim()
    val result = remember(ui.tx, ui.goals, query) {
        if (query.isEmpty()) null else {
            val lower = query.lowercase()
            val numeric = query.all { it.isDigit() || it == '.' || it == ',' || it == ' ' }
            val digits = query.filter { it.isDigit() }
            val tx = ui.tx.filter { t ->
                t.note.lowercase().contains(lower) ||
                    Categories.label(t.category).lowercase().contains(lower) ||
                    (if (t.type == TYPE_IN) "pemasukan" else "pengeluaran").contains(lower) ||
                    (ui.goalNames[t.goalId]?.lowercase()?.contains(lower) == true) ||
                    (numeric && digits.isNotEmpty() && t.amount.toString().contains(digits))
            }
            val goals = ui.goals.filter { it.goal.name.lowercase().contains(lower) }
            Pair(goals, tx)
        }
    }
    PageList {
        item(key = "header") { PageHeader("Search", "Cari transaksi, kategori, nominal, atau goal") }
        item(key = "field") {
            GField(
                value = q, onValueChange = { q = it.take(40) }, label = "", placeholder = "Ketik untuk mencari…",
                leading = { Ico(Icons.Rounded.Search, p.textDim, 20.dp); GapW(10.dp) }
            )
        }
        if (result == null) {
            item(key = "hints-title") { SectionTitle("Pintasan") }
            item(key = "hints") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Pemasukan", "Pengeluaran").plus(Categories.all.map { it.label }).distinct().forEach { label ->
                        GChip(label, false, { q = label })
                    }
                }
            }
            if (ui.tx.isEmpty() && ui.goals.isEmpty()) {
                item(key = "empty") { EmptyCard("Belum ada data", "Tambahkan transaksi atau goal, lalu cari di sini.") }
            }
        } else {
            val (goals, tx) = result
            item(key = "count") {
                GText("${goals.size} goal · ${tx.size} transaksi ditemukan", 12.sp, FontWeight.Medium, p.textDim)
            }
            if (goals.isNotEmpty()) {
                item(key = "g-title") { SectionTitle("Goals") }
                items(goals, key = { "g${it.goal.id}" }) { gs ->
                    Glass(Modifier.fillMaxWidth().bounceClick(true, 0.97f) { vm.show(Overlay.GoalEditor(gs.goal)) }, corner = 22.dp) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GText(gs.goal.name, 14.sp, FontWeight.Bold, p.text, modifier = Modifier.weight(1f), maxLines = 1)
                                GText("${(gs.progress * 100).toInt()}%", 12.sp, FontWeight.SemiBold, p.cyan)
                            }
                            Gap(8.dp)
                            LiquidBar(gs.progress, height = 8.dp)
                            Gap(6.dp)
                            GText("${Fmt.rupiahFit(gs.saved, 16)} / ${Fmt.compact(gs.goal.target)}", 11.sp, FontWeight.Normal, p.textDim, maxLines = 1)
                        }
                    }
                }
            }
            if (tx.isNotEmpty()) {
                item(key = "t-title") { SectionTitle("Transaksi") }
                items(tx.take(100), key = { it.id }) { t ->
                    TxRow(t, t.goalId?.let { ui.goalNames[it] }, { vm.show(Overlay.TxDetail(t)) }, Modifier.animateItem())
                }
                if (tx.size > 100) item(key = "more") { GText("Menampilkan 100 teratas. Persempit kata kunci.", 11.sp, FontWeight.Normal, p.textDim) }
            }
            if (goals.isEmpty() && tx.isEmpty()) {
                item(key = "none") { EmptyCard("Tidak ada hasil", "Coba kata kunci lain, misalnya nama kategori atau nominal.") }
            }
        }
    }
}

// ====================================================================== ANALYTICS

@Composable
fun AnalyticsPage(ui: UiModel) {
    val p = LocalPalette.current
    var range by rememberSaveable { mutableStateOf(1) } // 0=30 hari, 1=6 bulan, 2=12 bulan
    val series = remember(ui.tx, range) {
        when (range) {
            0 -> Analytics.balanceSeries(ui.tx, true, 30)
            1 -> Analytics.balanceSeries(ui.tx, false, 6)
            else -> Analytics.balanceSeries(ui.tx, false, 12)
        }
    }
    val months = remember(ui.tx) { Analytics.monthlyStats(ui.tx, 6) }
    val inCats = remember(ui.tx) { Analytics.categoryTotals(ui.tx, TYPE_IN) }
    val outCats = remember(ui.tx) { Analytics.categoryTotals(ui.tx, TYPE_OUT) }
    PageList {
        item(key = "header") { PageHeader("Analytics", "Perkembangan tabungan dan arus uangmu") }
        item(key = "summary") {
            Glass(Modifier.fillMaxWidth(), corner = 28.dp, tint = p.violet) {
                Column(Modifier.padding(18.dp)) {
                    GText("Ringkasan keuangan", 15.sp, FontWeight.Bold, p.text)
                    Gap(12.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric("Saldo", Fmt.rupiahFit(ui.balance, 14), p.text, Modifier.weight(1f))
                        Metric("Transaksi", ui.tx.size.toString(), p.text, Modifier.weight(1f))
                    }
                    Gap(12.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric("Total masuk", Fmt.rupiahFit(ui.totalIn, 14), p.income, Modifier.weight(1f))
                        Metric("Total keluar", Fmt.rupiahFit(ui.totalOut, 14), p.expense, Modifier.weight(1f))
                    }
                    Gap(12.dp)
                    val ratio = if (ui.totalIn > 0) ((ui.totalIn - ui.totalOut) * 100 / ui.totalIn).coerceAtLeast(0L) else 0L
                    val avg = months.sumOf { it.net } / months.size
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric("Rasio tabungan", "$ratio%", p.text, Modifier.weight(1f))
                        Metric("Rata-rata bersih/bln", Fmt.rupiahFit(avg, 14), p.text, Modifier.weight(1f))
                    }
                }
            }
        }
        item(key = "growth") {
            Glass(Modifier.fillMaxWidth(), corner = 28.dp) {
                Column(Modifier.padding(18.dp)) {
                    GText("Perkembangan tabungan", 15.sp, FontWeight.Bold, p.text)
                    Gap(10.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("30 Hari", "6 Bulan", "12 Bulan").forEachIndexed { i, l -> GChip(l, range == i, { range = i }) }
                    }
                    Gap(14.dp)
                    LineChart(series.map { it.value }, series.map { it.label })
                    Gap(8.dp)
                    val delta = series.last().value - series.first().value
                    GText(
                        "Perubahan pada periode ini: " + (if (delta >= 0) "+" else "−") + Fmt.rupiahFit(kotlin.math.abs(delta), 18),
                        12.sp, FontWeight.Medium, if (delta >= 0) p.income else p.expens
