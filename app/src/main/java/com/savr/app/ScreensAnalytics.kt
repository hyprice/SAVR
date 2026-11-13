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
                        12.sp, FontWeight.Medium, if (delta >= 0) p.income else p.expense
                    )
                }
            }
        }
        item(key = "monthly") {
            Glass(Modifier.fillMaxWidth(), corner = 28.dp) {
                Column(Modifier.padding(18.dp)) {
                    GText("Perbandingan per bulan", 15.sp, FontWeight.Bold, p.text)
                    Gap(14.dp)
                    BarChart(months)
                    Gap(10.dp)
                    val cur = months.last()
                    val prev = months[months.size - 2]
                    val diff = cur.net - prev.net
                    val pct = if (prev.net != 0L) " (${diff * 100 / kotlin.math.abs(prev.net)}%)" else ""
                    GText(
                        "Bersih bulan ini vs bulan lalu: " + (if (diff >= 0) "+" else "−") + Fmt.rupiahFit(kotlin.math.abs(diff), 16) + pct,
                        12.sp, FontWeight.Medium, if (diff >= 0) p.income else p.expense
                    )
                }
            }
        }
        item(key = "in-cats") { CategoryCard("Statistik pemasukan", inCats, ui.totalIn, true) }
        item(key = "out-cats") { CategoryCard("Statistik pengeluaran", outCats, ui.totalOut, false) }
        item(key = "goals") {
            Glass(Modifier.fillMaxWidth(), corner = 28.dp) {
                Column(Modifier.padding(18.dp)) {
                    GText("Progress semua goal", 15.sp, FontWeight.Bold, p.text)
                    Gap(12.dp)
                    if (ui.goals.isEmpty()) {
                        GText("Belum ada goal.", 13.sp, FontWeight.Normal, p.textDim)
                    } else {
                        ui.goals.forEach { gs ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GText(gs.goal.name, 13.sp, FontWeight.SemiBold, p.text, modifier = Modifier.weight(1f), maxLines = 1)
                                GText("${(gs.progress * 100).toInt()}%", 12.sp, FontWeight.Bold, p.cyan)
                            }
                            Gap(6.dp)
                            LiquidBar(gs.progress, height = 8.dp)
                            Gap(4.dp)
                            GText("${Fmt.rupiahFit(gs.saved, 16)} dari ${Fmt.compact(gs.goal.target)} · sisa ${Fmt.compact(gs.remaining)}", 11.sp, FontWeight.Normal, p.textDim, maxLines = 1)
                            Gap(14.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, color: Color, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier) {
        GText(label, 11.sp, FontWeight.Medium, p.textDim, maxLines = 1)
        Gap(3.dp)
        GText(value, 15.sp, FontWeight.Bold, color, maxLines = 1)
    }
}

@Composable
private fun CategoryCard(title: String, data: List<Pair<String, Long>>, total: Long, income: Boolean) {
    val p = LocalPalette.current
    val c1 = if (income) p.income else p.expense
    Glass(Modifier.fillMaxWidth(), corner = 28.dp) {
        Column(Modifier.padding(18.dp)) {
            GText(title, 15.sp, FontWeight.Bold, p.text)
            Gap(12.dp)
            if (data.isEmpty()) {
                GText("Belum ada data.", 13.sp, FontWeight.Normal, p.textDim)
            } else {
                data.take(5).forEach { (key, v) ->
                    val frac = if (total > 0) v.toFloat() / total else 0f
                    val shown by animateFloatAsState(frac, label = "cat")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GText(Categories.label(key), 13.sp, FontWeight.Medium, p.text, modifier = Modifier.weight(1f), maxLines = 1)
                        GText(Fmt.rupiahFit(v, 16) + "  ·  ${(frac * 100).toInt()}%", 12.sp, FontWeight.SemiBold, c1, maxLines = 1)
                    }
                    Gap(6.dp)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(p.track)) {
                        Box(
                            Modifier.fillMaxWidth(shown.coerceIn(0.02f, 1f)).fillMaxHeight().clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(c1, p.accent)))
                        )
                    }
                    Gap(12.dp)
                }
            }
        }
    }
}

