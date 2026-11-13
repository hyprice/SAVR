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

// ====================================================================== PROFILE

@Composable
fun ProfilePage(ui: UiModel, vm: SavrViewModel) {
    val p = LocalPalette.current
    val s = ui.settings
    var nameText by rememberSaveable(s.name) { mutableStateOf(s.name) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.requestRestore(uri)
    }
    PageList {
        item(key = "header") { PageHeader("Profile", "Pengaturan, data, dan tentang aplikasi") }
        item(key = "card") {
            Glass(Modifier.fillMaxWidth(), corner = 28.dp, floating = true, refract = true, tint = p.violet) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(64.dp).clip(CircleShape).background(Brush.linearGradient(listOf(p.accent, p.accent2))),
                            contentAlignment = Alignment.Center
                        ) { GText((s.name.trim().ifEmpty { "S" }).take(1).uppercase(), 28.sp, FontWeight.Bold, Color.White) }
                        GapW(16.dp)
                        Column(Modifier.weight(1f)) {
                            GText(s.name.ifBlank { "Penabung SAVR" }, 18.sp, FontWeight.Bold, p.text, maxLines = 1)
                            GText("${ui.tx.size} transaksi · ${ui.goals.size} goal", 12.sp, FontWeight.Normal, p.textDim, maxLines = 1)
                        }
                    }
                    Gap(16.dp)
                    GField(nameText, { nameText = it.take(Rules.MAX_NAME) }, "Nama tampilan", placeholder = "Nama kamu")
                    Gap(12.dp)
                    GlassButton("Simpan nama", { vm.saveName(nameText) }, Modifier.fillMaxWidth(), enabled = nameText.trim() != s.name,
                        leading = { c -> Ico(Icons.Rounded.Check, c, 18.dp) })
                }
            }
        }
        item(key = "appearance") {
            Glass(Modifier.fillMaxWidth(), corner = 28.dp) {
                Column(Modifier.padding(18.dp)) {
                    GText("Tampilan", 15.sp, FontWeight.Bold, p.text)
                    Gap(12.dp)
                    GText("Tema", 12.sp, FontWeight.Medium, p.textDim)
                    Gap(8.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GChip("Gelap", s.theme == ThemeMode.DARK, { vm.setSetting(Keys.THEME, ThemeMode.DARK.name) })
                        GChip("Terang", s.theme == ThemeMode.LIGHT, { vm.setSetting(Keys.THEME, ThemeMode.LIGHT.name) })
                        GChip("Ikuti sistem", s.theme == ThemeMode.SYSTEM, { vm.setSetting(Keys.THEME, ThemeMode.SYSTEM.name) })
                    }
                    Gap(16.dp)
                    SettingRow("Animasi liquid", "Background mengalir. Matikan untuk hemat baterai; otomatis nonaktif saat mode hemat daya.", s.motion) {
                        vm.setSetting(Keys.MOTION, if (it) "1" else "0")
                    }
                    Gap(14.dp)
                    SettingRow("Getaran sentuhan", "Umpan balik getar halus saat menekan tombol.", s.haptics) {
                        vm.setSetting(Keys.HAPTICS, if (it) "1" else "0")
                    }
                }
            }
        }
        item(key = "data") {
            Glass(Modifier.fillMaxWidth(), corner = 28.dp) {
                Column(Modifier.padding(18.dp)) {
                    GText("Data & backup", 15.sp, FontWeight.Bold, p.text)
                    Gap(6.dp)
                    GText(
                        "Data tersimpan permanen di perangkat. Backup berupa satu file JSON berisi seluruh transaksi, goal, dan pengaturan. " +
                            "File backup tidak dienkripsi — simpan di tempat yang aman.",
                        12.sp, FontWeight.Normal, p.textDim
                    )
                    Gap(14.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton(
                            "Backup", {
                                val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
                                exportLauncher.launch("savr-backup-$stamp.json")
                            }, Modifier.weight(1f), leading = { c -> Glyph(Gl.Export, c, 18.dp) }
                        )
                        GlassButton(
                            "Restore", { importLauncher.launch(arrayOf("*/*")) }, Modifier.weight(1f), primary = false,
                            leading = { c -> Glyph(Gl.Import, c, 18.dp) }
                        )
                    }
                }
            }
        }
        item(key = "about") {
            Glass(Modifier.fillMaxWidth().bounceClick(true, 0.97f) { vm.show(Overlay.About) }, corner = 24.dp) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    GText("Tentang SAVR & Hyprice", 14.sp, FontWeight.SemiBold, p.text, modifier = Modifier.weight(1f), maxLines = 1)
                    GText("Buka", 12.sp, FontWeight.Bold, p.cyan)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            GText(title, 14.sp, FontWeight.SemiBold, p.text)
            GText(desc, 11.sp, FontWeight.Normal, p.textDim)
        }
        GapW(12.dp)
        GSwitch(checked, onChange)
    }
}
