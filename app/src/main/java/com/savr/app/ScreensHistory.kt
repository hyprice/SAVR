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

// ====================================================================== HISTORY

@Composable
fun HistoryPage(ui: UiModel, vm: SavrViewModel) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    var typeF by rememberSaveable { mutableStateOf("ALL") }
    var catF by rememberSaveable { mutableStateOf("ALL") }
    var dateF by rememberSaveable { mutableStateOf("ALL") } // ALL, D7, D30, MONTH, CUSTOM
    var fromDay by rememberSaveable { mutableStateOf(LocalDate.now().minusDays(29).toEpochDay()) }
    var toDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }

    val filtered = remember(ui.tx, typeF, catF, dateF, fromDay, toDay) {
        val zone = Fmt.zone
        val today = LocalDate.now(zone)
        fun start(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()
        val (fromMs, toMs) = when (dateF) {
            "D7" -> start(today.minusDays(6)) to Long.MAX_VALUE
            "D30" -> start(today.minusDays(29)) to Long.MAX_VALUE
            "MONTH" -> start(today.withDayOfMonth(1)) to Long.MAX_VALUE
            "CUSTOM" -> start(LocalDate.ofEpochDay(minOf(fromDay, toDay))) to (start(LocalDate.ofEpochDay(maxOf(fromDay, toDay)).plusDays(1)) - 1)
            else -> Long.MIN_VALUE to Long.MAX_VALUE
        }
        ui.tx.filter { t ->
            (typeF == "ALL" || t.type == typeF) &&
                (catF == "ALL" || t.category == catF) &&
                t.createdAt in fromMs..toMs
        }
    }
    val rows = remember(filtered) {
        val zone = Fmt.zone
        val out = ArrayList<Any>(filtered.size + 16)
        var last = Long.MIN_VALUE
        for (t in filtered) {
            val d = Instant.ofEpochMilli(t.createdAt).atZone(zone).toLocalDate().toEpochDay()
            if (d != last) { out.add(d); last = d }
            out.add(t)
        }
        out
    }
    val inSum = filtered.filter { it.type == TYPE_IN }.sumOf { it.amount }
    val outSum = filtered.filter { it.type == TYPE_OUT }.sumOf { it.amount }
    val filterActive = typeF != "ALL" || catF != "ALL" || dateF != "ALL"

    PageList {
        item(key = "header") { PageHeader("History", "Seluruh riwayat transaksi") }
        item(key = "filters") {
            Glass(Modifier.fillMaxWidth(), corner = 24.dp) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Glyph(Gl.Filter, p.textDim, 16.dp)
                        GapW(8.dp)
                        GText("Filter", 12.sp, FontWeight.SemiBold, p.textDim, modifier = Modifier.weight(1f))
                        if (filterActive) GText("Reset", 12.sp, FontWeight.Bold, p.cyan, modifier = Modifier.bounceClick(true, 0.9f) {
                            typeF = "ALL"; catF = "ALL"; dateF = "ALL"
                        }.padding(6.dp))
                    }
                    Gap(8.dp)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GChip("Semua", typeF == "ALL", { typeF = "ALL" })
                        GChip("Pemasukan", typeF == TYPE_IN, { typeF = TYPE_IN; if (!Categories.isValid(catF, TYPE_IN)) catF = "ALL" })
                        GChip("Pengeluaran", typeF == TYPE_OUT, { typeF = TYPE_OUT; if (!Categories.isValid(catF, TYPE_OUT)) catF = "ALL" })
                    }
                    Gap(8.dp)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GChip("Semua kategori", catF == "ALL", { catF = "ALL" })
                        val cats = if (typeF == "ALL") Categories.all else Categories.forType(typeF)
                        cats.forEach { c -> GChip(c.label, catF == c.key, { catF = c.key }) }
                    }
                    Gap(8.dp)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GChip("Semua tanggal", dateF == "ALL", { dateF = "ALL" })
                        GChip("7 hari", dateF == "D7", { dateF = "D7" })
                        GChip("30 hari", dateF == "D30", { dateF = "D30" })
                        GChip("Bulan ini", dateF == "MONTH", { dateF = "MONTH" })
                        GChip("Rentang", dateF == "CUSTOM", { dateF = "CUSTOM" })
                    }
                    if (dateF == "CUSTOM") {
                        Gap(10.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassButton(
                                "Dari ${Fmt.date(LocalDate.ofEpochDay(fromDay))}", { pickDate(ctx, LocalDate.ofEpochDay(fromDay)) { fromDay = it.toEpochDay() } },
                                Modifier.weight(1f), primary = false,
                                leading = { c -> Ico(Icons.Rounded.DateRange, c, 16.dp) }
                            )
                            GlassButton(
                                "Sampai ${Fmt.date(LocalDate.ofEpochDay(toDay))}", { pickDate(ctx, LocalDate.ofEpochDay(toDay)) { toDay = it.toEpochDay() } },
                                Modifier.weight(1f), primary = false,
                                leading = { c -> Ico(Icons.Rounded.DateRange, c, 16.dp) }
                            )
                        }
                    }
                }
            }
        }
        item(key = "summary") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GText("${filtered.size} transaksi", 12.sp, FontWeight.SemiBold, p.textDim, modifier = Modifier.weight(1f), maxLines = 1)
                GText("+" + Fmt.compact(inSum), 12.sp, FontWeight.Bold, p.income, maxLines = 1)
                GapW(10.dp)
                GText("−" + Fmt.compact(outSum), 12.sp, FontWeight.Bold, p.expense, maxLines = 1)
            }
        }
        if (filtered.isEmpty()) {
            item(key = "empty") {
                if (ui.tx.isEmpty()) EmptyCard("Riwayat masih kosong", "Transaksi yang kamu catat akan muncul di sini.", "Tambah transaksi") {
                    vm.show(Overlay.TxEditor(null, TYPE_IN, null))
                } else EmptyCard("Tidak ada yang cocok", "Ubah atau reset filter untuk melihat transaksi lain.", "Reset filter") {
                    typeF = "ALL"; catF = "ALL"; dateF = "ALL"
                }
            }
        } else {
            items(rows, key = { if (it is SavingsTransaction) it.id else "h$it" }) { row ->
                if (row is SavingsTransaction) {
                    TxRow(row, row.goalId?.let { ui.goalNames[it] }, { vm.show(Overlay.TxDetail(row)) }, Modifier.animateItem())
                } else {
                    DateHeader(row as Long, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun DateHeader(epochDay: Long, modifier: Modifier) {
    val p = LocalPalette.current
    val d = LocalDate.ofEpochDay(epochDay)
    val today = LocalDate.now(Fmt.zone)
    val label = when (d) {
        today -> "Hari ini"
        today.minusDays(1) -> "Kemarin"
        else -> Fmt.dayHeader(d)
    }
    GText(label, 12.sp, FontWeight.SemiBold, p.textDim, modifier = modifier.padding(top = 8.dp, start = 4.dp), spacing = 0.5.sp)
}

