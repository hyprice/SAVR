package com.savr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val Mint = Color(0xFF9BE8C7)
private val Bg = Color(0xFF0B1119)
private val Panel = Color(0x24FFFFFF)
private val Muted = Color(0xFFB3C0CE)

class SavingsViewModel(private val dao: SavingsDao) : ViewModel() {
    val transactions = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val balance = transactions.map { list -> list.sumOf { if (it.type == "IN") it.amount else -it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    fun add(amount: Long, note: String, type: String) = viewModelScope.launch {
        dao.insert(SavingsTransaction(amount = amount, note = note.ifBlank { if (type == "IN") "Tabungan" else "Penarikan" }, type = type))
    }
}
class SavingsVmFactory(private val dao: SavingsDao) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SavingsViewModel(dao) as T
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = SavingsDatabase.get(this).savingsDao()
        setContent { SavrApp(dao) }
    }
}

@Composable
fun SavrApp(dao: SavingsDao) {
    val vm: SavingsViewModel = viewModel(factory = SavingsVmFactory(dao))
    val balance by vm.balance.collectAsState()
    val transactions by vm.transactions.collectAsState()
    var target by remember { mutableLongStateOf(7_000_000L) }
    var tab by remember { mutableStateOf("Home") }
    var dialog by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val deposits = transactions.filter { it.type == "IN" }.sumOf { it.amount }
    val withdrawals = transactions.filter { it.type == "OUT" }.sumOf { it.amount }
    val progress = if (target > 0) (balance.toFloat() / target).coerceIn(0f, 1f) else 0f

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, background = Bg, surface = Color(0xFF17212C),
        onBackground = Color.White, onSurface = Color.White
    )) {
        Box(Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF101E29), Bg, Color(0xFF20243A)))
        )) {
            Box(Modifier.align(Alignment.TopEnd).offset(65.dp, 70.dp).size(220.dp)
                .background(Color(0x3349AFA0), CircleShape).blur(65.dp))
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 18.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassCard(Modifier.size(39.dp), corner = 13.dp) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("S", color = Mint, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("SAVR.", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                            Text("YOUR MONEY SPACE", fontSize = 9.sp, color = Muted, letterSpacing = 1.5.sp)
                        }
                    }
                    GlassCard(corner = 50.dp) { Text("✦  LIQUID GLASS", Modifier.padding(horizontal = 12.dp, vertical = 9.dp), fontSize = 10.sp, color = Mint) }
                }

                GlassCard(Modifier.fillMaxWidth(), corner = 27.dp) {
                    Column(Modifier.padding(19.dp)) {
                        Text("TOTAL SAVINGS", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                        AnimatedContent(balance, transitionSpec = {
                            (slideInVertically(animationSpec = spring(dampingRatio = .72f)) { it / 3 } + fadeIn()) togetherWith
                                (slideOutVertically(animationSpec = tween(100)) { -it / 4 } + fadeOut())
                        }, label = "balance") { Text(rupiah(it), fontSize = 30.sp, fontWeight = FontWeight.SemiBold) }
                        Text("Your financial journey", color = Mint, fontSize = 11.sp)
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("TARGET", color = Muted, fontSize = 10.sp)
                                Text(rupiah(target), fontWeight = FontWeight.SemiBold)
                            }
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
                                CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(),
                                    color = Mint, trackColor = Color.White.copy(alpha = .13f), strokeWidth = 7.dp)
                                Text("${(progress * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(13.dp))
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = Mint, trackColor = Color.White.copy(alpha = .12f))
                        Spacer(Modifier.height(7.dp))
                        Text("${rupiah((target - balance).coerceAtLeast(0))} remaining", color = Muted, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("↙  DEPOSITS", rupiah(deposits), Modifier.weight(1f))
                    StatCard("↗  WITHDRAWALS", rupiah(withdrawals), Modifier.weight(1f))
                }
                Spacer(Modifier.height(19.dp))
                AnimatedContent(tab, transitionSpec = {
                    fadeIn(tween(180)) + slideInHorizontally(spring(dampingRatio = .82f)) { it / 8 } togetherWith
                        fadeOut(tween(100)) + slideOutHorizontally(tween(100)) { -it / 10 }
                }, label = "page", modifier = Modifier.weight(1f)) { selected ->
                    when (selected) {
                        "Home" -> Column {
                            Text("Quick actions", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BouncyButton("＋  Add money", true, Modifier.weight(1f)) { dialog = "IN"; amount = ""; note = ""; error = "" }
                                BouncyButton("↗  Withdraw", false, Modifier.weight(1f)) { dialog = "OUT"; amount = ""; note = ""; error = "" }
                            }
                            Spacer(Modifier.height(19.dp))
                            Text("Recent activity", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(9.dp))
                            if (transactions.isEmpty()) EmptyState()
                            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(transactions.take(5), key = { it.id }) { TransactionRow(it) }
                            }
                        }
                        "History" -> Column {
                            Text("All transactions", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(10.dp))
                            if (transactions.isEmpty()) EmptyState()
                            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(transactions, key = { it.id }) { TransactionRow(it) }
                            }
                        }
                        else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Savings goal", fontWeight = FontWeight.SemiBold)
                            GlassCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Target amount (IDR)", color = Muted, fontSize = 11.sp)
                                    var targetText by remember(target) { mutableStateOf(target.toString()) }
                                    OutlinedTextField(targetText, { targetText = it.filter(Char::isDigit) },
                                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                            focusedBorderColor = Mint, unfocusedBorderColor = Color.White.copy(.2f)))
                                    BouncyButton("Save target", true, Modifier.fillMaxWidth()) {
                                        targetText.toLongOrNull()?.takeIf { it > 0 }?.let { target = it }
                                    }
                                }
                            }
                            Text("Data tersimpan di database lokal perangkat.", color = Muted, fontSize = 11.sp)
                        }
                    }
                }
                GlassCard(Modifier.fillMaxWidth(), corner = 22.dp) {
                    Row(Modifier.fillMaxWidth().padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Home" to Icons.Rounded.Home, "History" to Icons.Rounded.History, "Goals" to Icons.Rounded.Tune).forEach { (name, icon) ->
                            val active = tab == name
                            BouncyNav(name, icon, active, Modifier.weight(1f)) { tab = name }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (dialog != null) {
            AlertDialog(
                onDismissRequest = { dialog = null },
                containerColor = Color(0xFF1B2935),
                title = { Text(if (dialog == "IN") "Add money" else "Withdraw money") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(amount, { amount = it.filter(Char::isDigit); error = "" },
                            label = { Text("Amount (IDR)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, colors = fieldColors())
                        OutlinedTextField(note, { note = it }, label = { Text("Description") }, singleLine = true, colors = fieldColors())
                        if (error.isNotBlank()) Text(error, color = Color(0xFFFFAAAA), fontSize = 12.sp)
                    }
                },
                confirmButton = { TextButton(onClick = {
                    val n = amount.toLongOrNull()
                    when {
                        n == null || n <= 0 -> error = "Masukkan nominal yang valid."
                        dialog == "OUT" && n > balance -> error = "Saldo tidak mencukupi."
                        else -> { vm.add(n, note, dialog!!); dialog = null }
                    }
                }) { Text("Confirm", color = Mint) } },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel", color = Muted) } }
            )
        }
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
    focusedBorderColor = Mint, unfocusedBorderColor = Color.White.copy(.22f),
    focusedLabelColor = Mint, unfocusedLabelColor = Muted
)

@Composable
fun GlassCard(modifier: Modifier = Modifier, corner: Dp = 20.dp, content: @Composable () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(corner))
        .background(Brush.linearGradient(listOf(Color.White.copy(.15f), Color.White.copy(.045f))))
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(.30f), Color.White.copy(.07f))), RoundedCornerShape(corner))
    ) { content() }
}

@Composable
fun BouncyButton(label: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .93f else 1f,
        spring(dampingRatio = .48f, stiffness = Spring.StiffnessMedium), label = "buttonSpring")
    Surface(onClick = onClick, interactionSource = interaction, modifier = modifier.scale(scale),
        shape = RoundedCornerShape(17.dp),
        color = if (primary) Mint else Color.White.copy(.10f),
        contentColor = if (primary) Color(0xFF132C25) else Color.White,
        border = if (primary) null else BorderStroke(1.dp, Color.White.copy(.17f))
    ) {
        Box(Modifier.padding(vertical = 15.dp, horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun BouncyNav(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .90f else if (active) 1f else .97f, spring(dampingRatio = .52f), label = "nav")
    Surface(onClick = onClick, interactionSource = interaction, modifier = modifier.scale(scale), color = if (active) Color.White.copy(.17f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (active) Mint else Muted, modifier = Modifier.size(19.dp))
            Text(name, color = if (active) Color.White else Muted, fontSize = 10.sp)
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier) {
    GlassCard(modifier) { Column(Modifier.padding(13.dp)) {
        Text(label, color = Muted, fontSize = 9.sp)
        Spacer(Modifier.height(7.dp))
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    } }
}

@Composable
fun TransactionRow(item: SavingsTransaction) {
    GlassCard(Modifier.fillMaxWidth(), 16.dp) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(.10f)), contentAlignment = Alignment.Center) {
                Icon(if (item.type == "IN") Icons.Rounded.SouthWest else Icons.Rounded.NorthEast, null,
                    tint = if (item.type == "IN") Mint else Color(0xFFFFB4B4))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.note, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("id", "ID")).format(Date(item.createdAt)), color = Muted, fontSize = 9.sp)
            }
            Text((if (item.type == "IN") "+" else "−") + rupiah(item.amount),
                color = if (item.type == "IN") Mint else Color(0xFFFFB4B4), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun EmptyState() {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(25.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Savings, null, tint = Muted, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(8.dp))
            Text("Belum ada transaksi", color = Muted, fontSize = 12.sp)
            Text("Mulai isi tabunganmu.", color = Muted.copy(.7f), fontSize = 10.sp)
        }
    }
}

fun rupiah(value: Long): String = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
    maximumFractionDigits = 0
}.format(value).replace("Rp", "Rp")
