package com.savr.app

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val vm: SavrViewModel by viewModels { SavrVmFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val start = SystemClock.uptimeMillis()
        // Splash bertahan sampai data siap (maksimal 2,5 dtk agar tidak pernah macet).
        splash.setKeepOnScreenCondition {
            SystemClock.uptimeMillis() - start < 2500 && vm.ui.value == null && vm.dbError.value == null
        }
        enableEdgeToEdge()
        setContent { SavrRoot(vm) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Mode hemat daya sistem aktif -> animasi background dimatikan otomatis. */
@Composable
private fun rememberPowerSave(): Boolean {
    val ctx = LocalView.current.context
    val pm = remember { ctx.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var save by remember { mutableStateOf(pm.isPowerSaveMode) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { save = pm.isPowerSaveMode }
        }
        ContextCompat.registerReceiver(ctx, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { ctx.unregisterReceiver(receiver) }
    }
    return save
}

@Composable
fun SavrRoot(vm: SavrViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val dbError by vm.dbError.collectAsStateWithLifecycle()
    val overlay by vm.overlay.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val model = ui
    val err = dbError
    val settings = model?.settings ?: AppSettings()

    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val palette = if (dark) DarkPalette else LightPalette
    val powerSave = rememberPowerSave()
    val motion = settings.motion && !powerSave
    val phase: State<Float> = if (motion) {
        rememberInfiniteTransition(label = "bg").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(32000, easing = LinearEasing)),
            label = "phase"
        )
    } else {
        remember { mutableStateOf(0.12f) }
    }

    // Ikon status bar & navigation bar mengikuti tema aplikasi (kontras otomatis).
    val view = LocalView.current
    SideEffect {
        val w = view.context.findActivity()?.window
        if (w != null) {
            val c = WindowCompat.getInsetsController(w, view)
            c.isAppearanceLightStatusBars = !dark
            c.isAppearanceLightNavigationBars = !dark
        }
    }

    var bgSize by remember { mutableStateOf(Size.Zero) }
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalHaptics provides settings.haptics,
        LocalPhase provides phase,
        LocalBgSize provides bgSize
    ) {
        Box(Modifier.fillMaxSize().onSizeChanged { bgSize = Size(it.width.toFloat(), it.height.toFloat()) }) {
            LiquidBackground()
            when {
                err != null -> DbErrorPanel(err) { vm.retryDatabase() }
                model == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LiquidLoader(dim = 72.dp) }
                else -> MainShell(model, overlay != null, vm)
            }
            SheetHost(overlay, model, vm)
            ToastHost(vm)
            if (busy) BusyOverlay()
        }
    }
}

// ====================================================================== Shell: pager + nav

private enum class Tab(val label: String) { Home("Home"), Search("Search"), Analytics("Analytics"), History("History"), Profile("Profile") }

@Composable
private fun TabIcon(tab: Tab, tint: Color, dim: Dp) {
    when (tab) {
        Tab.Home -> Ico(Icons.Rounded.Home, tint, dim)
        Tab.Search -> Ico(Icons.Rounded.Search, tint, dim)
        Tab.Analytics -> Glyph(Gl.Analytics, tint, dim)
        Tab.History -> Glyph(Gl.History, tint, dim)
        Tab.Profile -> Ico(Icons.Rounded.Person, tint, dim)
    }
}

@Composable
private fun MainShell(ui: UiModel, overlayOpen: Boolean, vm: SavrViewModel) {
    val pager = rememberPagerState(pageCount = { Tab.entries.size })
    val scope = rememberCoroutineScope()
    fun go(i: Int) {
        scope.launch { pager.animateScrollToPage(i, animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)) }
    }
    BackHandler(enabled = !overlayOpen && pager.currentPage != 0) { go(0) }

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { it }) { page ->
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                    alpha = 1f - off * 0.45f
                    scaleX = 1f - off * 0.06f
                    scaleY = 1f - off * 0.06f
                }
            ) {
                when (page) {
                    0 -> HomePage(ui, vm) { go(Tab.History.ordinal) }
                    1 -> SearchPage(ui, vm)
                    2 -> AnalyticsPage(ui)
                    3 -> HistoryPage(ui, vm)
                    else -> ProfilePage(ui, vm)
                }
            }
        }
        FloatingNav(
            pos = { pager.currentPage + pager.currentPageOffsetFraction },
            selected = pager.currentPage,
            onSelect = { go(it) },
            onAdd = { vm.show(Overlay.TxEditor(null, TYPE_IN, null)) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun FloatingNav(pos: () -> Float, selected: Int, onSelect: (Int) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val d = LocalDensity.current
    // Skala font dibatasi di bar navigasi agar label tidak pecah saat font sistem diperbesar.
    CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = minOf(d.fontScale, 1.15f))) {
        Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Row(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Glass(Modifier.weight(1f), corner = 32.dp, floating = true, refract = true) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                            .drawBehind {
                                val n = Tab.entries.size
                                val itemW = size.width / n
                                val pp = pos().coerceIn(0f, (n - 1).toFloat())
                                val f = pp - floor(pp)
                                val stretch = sin(f * PI.toFloat()) // pil "cair" memanjang saat berpindah
                                val w = itemW * (1f + 0.45f * stretch)
                                val left = pp * itemW - (w - itemW) / 2f
                                val r = CornerRadius(size.height / 2f)
                                drawRoundRect(
                                    Brush.linearGradient(listOf(p.accent, p.accent2), Offset(left, 0f), Offset(left + w, size.height)),
                                    Offset(left, 0f), Size(w, size.height), r
                                )
                                drawRoundRect(
                                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0f)), endY = size.height * 0.5f),
                                    Offset(left, 0f), Size(w, size.height), r
                                )
                            }
                    ) {
                        Tab.entries.forEachIndexed { i, tab -> NavCell(tab, selected == i, Modifier.weight(1f)) { onSelect(i) } }
                    }
                }
                Box(Modifier.padding(start = 12.dp)) {
                    Glass(Modifier.size(62.dp).bounceClick(true, 0.86f, onAdd), corner = 31.dp, floating = true, tint = p.cyan) {
                        Box(
                            Modifier.fillMaxSize().background(Brush.linearGradient(listOf(p.accent.copy(alpha = 0.92f), p.accent2.copy(alpha = 0.92f)))),
                            contentAlignment = Alignment.Center
                        ) { Ico(Icons.Rounded.Add, Color.White, 28.dp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavCell(tab: Tab, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val scale by animateFloatAsState(if (selected) 1.12f else 1f, spring(dampingRatio = 0.45f, stiffness = 400f), label = "navScale")
    val tint by animateColorAsState(if (selected) Color.White else p.textDim, label = "navTint")
    Column(modifier.bounceClick(true, 0.88f, onClick).padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) { TabIcon(tab, tint, 22.dp) }
        Gap(2.dp)
        GText(tab.label, 10.sp, if (selected) FontWeight.SemiBold else FontWeight.Medium, tint, maxLines = 1)
    }
}

// ====================================================================== Toast, busy, error

@Composable
private fun ToastHost(vm: SavrViewModel) {
    val p = LocalPalette.current
    var text by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(vm) {
        vm.events.collectLatest {
            text = it
            visible = true
            delay(2800)
            visible = false
        }
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            Glass(Modifier.padding(horizontal = 24.dp).widthIn(max = 520.dp), corner = 22.dp, floating = true, tint = p.accent) {
                GText(text, 13.sp, FontWeight.SemiBold, p.text, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), align = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun BusyOverlay() {
    val p = LocalPalette.current
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Glass(corner = 28.dp, floating = true) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                LiquidLoader(dim = 64.dp)
                Gap(12.dp)
                GText("Memproses…", 14.sp, FontWeight.SemiBold, p.text)
            }
        }
    }
}

@Composable
private fun DbErrorPanel(message: String, onRetry: () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Glass(Modifier.widthIn(max = 480.dp).fillMaxWidth(), corner = 30.dp, floating = true, tint = p.danger) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                GText("Database tidak bisa dibuka", 18.sp, FontWeight.Bold, p.text, align = TextAlign.Center)
                Gap(8.dp)
                GText(
                    "Data kamu tidak dihapus. Tutup aplikasi lain yang memakai banyak memori, lalu coba lagi. " +
                        "Jika tetap gagal, pasang versi SAVR terbaru di atas yang lama (tanpa uninstall).",
                    13.sp, FontWeight.Normal, p.textDim, align = TextAlign.Center
                )
                Gap(8.dp)
                GText(message, 11.sp, FontWeight.Normal, p.textDim, align = TextAlign.Center, maxLines = 3)
                Gap(18.dp)
                GlassButton("Coba lagi", onRetry, Modifier.fillMaxWidth())
            }
        }
    }
}
