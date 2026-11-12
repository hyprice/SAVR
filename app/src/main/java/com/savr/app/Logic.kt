package com.savr.app

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

enum class ThemeMode { SYSTEM, DARK, LIGHT }

object Keys {
    const val NAME = "name"
    const val THEME = "theme"
    const val MOTION = "motion"
    const val HAPTICS = "haptics"
}

@Immutable
data class AppSettings(
    val name: String = "",
    val theme: ThemeMode = ThemeMode.DARK,
    val motion: Boolean = true,
    val haptics: Boolean = true
) {
    companion object {
        fun from(list: List<Setting>): AppSettings {
            val m = list.associate { it.name to it.value }
            return AppSettings(
                name = (m[Keys.NAME] ?: "").take(Rules.MAX_NAME),
                theme = ThemeMode.entries.firstOrNull { it.name == m[Keys.THEME] } ?: ThemeMode.DARK,
                motion = m[Keys.MOTION] != "0",
                haptics = m[Keys.HAPTICS] != "0"
            )
        }
    }
}

// ---------------------------------------------------------------- Kategori

data class Category(val key: String, val label: String, val type: String?)

object Categories {
    val all = listOf(
        Category("SAVING", "Tabungan", TYPE_IN),
        Category("SALARY", "Gaji", TYPE_IN),
        Category("BONUS", "Bonus", TYPE_IN),
        Category("GIFT", "Hadiah", TYPE_IN),
        Category("WITHDRAW", "Penarikan", TYPE_OUT),
        Category("NEEDS", "Kebutuhan", TYPE_OUT),
        Category("BILLS", "Tagihan", TYPE_OUT),
        Category("FOOD", "Makanan", TYPE_OUT),
        Category("TRANSPORT", "Transportasi", TYPE_OUT),
        Category("FUN", "Hiburan", TYPE_OUT),
        Category("OTHER", "Lainnya", null)
    )
    private val byKey = all.associateBy { it.key }

    fun forType(type: String): List<Category> = all.filter { it.type == null || it.type == type }
    fun label(key: String): String = byKey[key]?.label ?: "Lainnya"
    fun isValid(key: String, type: String): Boolean = byKey[key]?.let { it.type == null || it.type == type } == true
    fun default(type: String): String = if (type == TYPE_IN) "SAVING" else "WITHDRAW"
}

// ---------------------------------------------------------------- Aturan & validasi

object Rules {
    const val MAX_AMOUNT = 999_999_999_999L
    const val MAX_NOTE = 80
    const val MAX_GOAL = 40
    const val MAX_NAME = 30
    const val MIN_TIME = 946_684_800_000L // 1 Jan 2000

    /** Hanya angka, tanpa nol di depan, maksimal 12 digit. */
    fun parseAmount(text: String): Long? {
        val d = text.filter { it in '0'..'9' }.trimStart('0')
        if (d.isEmpty() || d.length > 12) return null
        return d.toLongOrNull()
    }

    fun signed(t: SavingsTransaction): Long = if (t.type == TYPE_IN) t.amount else -t.amount

    fun bucketBalance(all: List<SavingsTransaction>, goalId: Long?, excludeId: Long? = null): Long {
        var s = 0L
        for (t in all) if (t.goalId == goalId && t.id != excludeId) s += signed(t)
        return s
    }

    /** true bila saldo salah satu bucket (goal / tanpa goal) pernah negatif jika diurut menurut waktu. */
    fun hasNegative(txs: List<SavingsTransaction>, buckets: Set<Long?>): Boolean {
        for (b in buckets) {
            var run = 0L
            val list = txs.filter { it.goalId == b }.sortedWith(
                compareBy<SavingsTransaction>({ it.createdAt }, { if (it.type == TYPE_IN) 0 else 1 }, { it.id })
            )
            for (t in list) {
                run += signed(t)
                if (run < 0) return true
            }
        }
        return false
    }

    fun validateTx(
        all: List<SavingsTransaction>,
        goalIds: Set<Long>,
        cand: SavingsTransaction,
        editingId: Long?,
        nowMs: Long = System.currentTimeMillis()
    ): String? {
        if (cand.type != TYPE_IN && cand.type != TYPE_OUT) return "Jenis transaksi tidak valid."
        if (cand.amount <= 0 || cand.amount > MAX_AMOUNT) return "Masukkan nominal yang valid (maksimal Rp 999 miliar)."
        if (cand.note.length > MAX_NOTE) return "Catatan maksimal $MAX_NOTE karakter."
        if (!Categories.isValid(cand.category, cand.type)) return "Kategori tidak sesuai jenis transaksi."
        if (cand.goalId != null && cand.goalId !in goalIds) return "Goal tidak ditemukan."
        if (cand.createdAt > nowMs + 60_000L) return "Tanggal tidak boleh di masa depan."
        if (cand.createdAt < MIN_TIME) return "Tanggal tidak valid."
        val old = editingId?.let { id -> all.firstOrNull { it.id == id } }
        val next = all.filter { it.id != editingId } + cand.copy(id = editingId ?: -1L)
        if (hasNegative(next, setOf(cand.goalId, old?.goalId))) {
            return "Saldo tidak mencukupi. Periksa nominal, tanggal, atau goal yang dipilih."
        }
        return null
    }

    fun breaksIfRemoved(all: List<SavingsTransaction>, t: SavingsTransaction): Boolean =
        hasNegative(all.filter { it.id != t.id }, setOf(t.goalId))
}

// ---------------------------------------------------------------- Format

object Fmt {
    private val id = Locale.forLanguageTag("id-ID")
    private val fDateTime = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", id)
    private val fDate = DateTimeFormatter.ofPattern("dd MMM yyyy", id)
    private val fDay = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", id)
    private val fMonth = DateTimeFormatter.ofPattern("MMMM yyyy", id)
    private val fMonthShort = DateTimeFormatter.ofPattern("MMM", id)
    private val fDayShort = DateTimeFormatter.ofPattern("dd MMM", id)

    val zone: ZoneId get() = ZoneId.systemDefault()

    fun rupiah(v: Long): String {
        val digits = if (v < 0) (-v).toString() else v.toString()
        val sb = StringBuilder()
        if (v < 0) sb.append('-')
        sb.append("Rp\u00A0")
        for (i in digits.indices) {
            if (i > 0 && (digits.length - i) % 3 == 0) sb.append('.')
            sb.append(digits[i])
        }
        return sb.toString()
    }

    /** Rupiah penuh bila muat, selain itu ringkas (mis. Rp 1,2jt) agar tidak terpotong di layar sempit. */
    fun rupiahFit(v: Long, maxChars: Int): String {
        val full = rupiah(v)
        return if (full.length <= maxChars) full else "Rp\u00A0" + compact(v)
    }

    fun compact(v: Long): String {
        val a = abs(v)
        val s = if (v < 0) "-" else ""
        return when {
            a >= 1_000_000_000L -> s + trim(a / 1e9) + "M"
            a >= 1_000_000L -> s + trim(a / 1e6) + "jt"
            a >= 1_000L -> s + trim(a / 1e3) + "rb"
            else -> s + a
        }
    }

    private fun trim(d: Double): String {
        val r = Math.round(d * 10) / 10.0
        return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString().replace('.', ',')
    }

    fun dateTime(ms: Long): String = fDateTime.format(Instant.ofEpochMilli(ms).atZone(zone))
    fun shortDate(ms: Long): String = fDate.format(Instant.ofEpochMilli(ms).atZone(zone))
    fun date(d: LocalDate): String = fDate.format(d)
    fun dayHeader(d: LocalDate): String = fDay.format(d)
    fun dayShort(d: LocalDate): String = fDayShort.format(d)
    fun month(m: YearMonth): String = fMonth.format(m)
    fun monthShort(m: YearMonth): String = fMonthShort.format(m)
}

// ---------------------------------------------------------------- Analitik

data class MonthStat(val month: YearMonth, val income: Long, val expense: Long) {
    val net: Long get() = income - expense
}

data class SeriesPoint(val label: String, val value: Long)

object Analytics {
    /** Pemasukan/pengeluaran per bulan, urut dari paling lama ke bulan ini. */
    fun monthlyStats(
        tx: List<SavingsTransaction>,
        months: Int,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = Fmt.zone
    ): List<MonthStat> {
        val current = YearMonth.from(Instant.ofEpochMilli(nowMs).atZone(zone))
        val list = (months - 1 downTo 0).map { current.minusMonths(it.toLong()) }
        val first = list.first()
        val inMap = HashMap<YearMonth, Long>()
        val outMap = HashMap<YearMonth, Long>()
        for (t in tx) {
            val ym = YearMonth.from(Instant.ofEpochMilli(t.createdAt).atZone(zone))
            if (ym.isBefore(first) || ym.isAfter(current)) continue
            val map = if (t.type == TYPE_IN) inMap else outMap
            map.merge(ym, t.amount) { a, b -> a + b }
        }
        return list.map { MonthStat(it, inMap[it] ?: 0L, outMap[it] ?: 0L) }
    }

    /** Saldo kumulatif di akhir tiap hari (days=true) atau tiap bulan (days=false). */
    fun balanceSeries(
        tx: List<SavingsTransaction>,
        days: Boolean,
        count: Int,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = Fmt.zone
    ): List<SeriesPoint> {
        val asc = tx.sortedWith(compareBy<SavingsTransaction>({ it.createdAt }, { it.id }))
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val ends: List<Pair<String, Long>> = if (days) {
            (count - 1 downTo 0).map { d ->
                val day = today.minusDays(d.toLong())
                Fmt.dayShort(day) to (day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1)
            }
        } else {
            val cur = YearMonth.from(today)
            (count - 1 downTo 0).map { m ->
                val ym = cur.minusMonths(m.toLong())
                Fmt.monthShort(ym) to (ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1)
            }
        }
        var idx = 0
        var run = 0L
        return ends.map { (label, end) ->
            while (idx < asc.size && asc[idx].createdAt <= end) {
                run += Rules.signed(asc[idx])
                idx++
            }
            SeriesPoint(label, run)
        }
    }

    fun categoryTotals(tx: List<SavingsTransaction>, type: String, sinceMs: Long = Long.MIN_VALUE): List<Pair<String, Long>> {
        val m = HashMap<String, Long>()
        for (t in tx) if (t.type == type && t.createdAt >= sinceMs) m.merge(t.category, t.amount) { a, b -> a + b }
        return m.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }
}

// ---------------------------------------------------------------- Model UI

@Immutable
data class GoalStat(val goal: Goal, val saved: Long) {
    val remaining: Long get() = (goal.target - saved).coerceAtLeast(0L)
    val progress: Float
        get() = if (goal.target > 0) (saved.toDouble() / goal.target).toFloat().coerceIn(0f, 1f) else 0f
}

@Immutable
data class UiModel(
    val tx: List<SavingsTransaction>,
    val goals: List<GoalStat>,
    val goalNames: Map<Long, String>,
    val balance: Long,
    val unassigned: Long,
    val totalIn: Long,
    val totalOut: Long,
    val monthIn: Long,
    val monthOut: Long,
    val settings: AppSettings
) {
    val mainGoal: GoalStat? get() = goals.firstOrNull { it.goal.isMain } ?: goals.firstOrNull()

    companion object {
        fun build(tx: List<SavingsTransaction>, goals: List<Goal>, st: List<Setting>): UiModel {
            val zone = Fmt.zone
            val monthStart = YearMonth.now(zone).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val per = HashMap<Long, Long>()
            var unassigned = 0L
            var tin = 0L
            var tout = 0L
            var min = 0L
            var mout = 0L
            for (t in tx) {
                val s = Rules.signed(t)
                val g = t.goalId
                if (g == null) unassigned += s else per.merge(g, s) { a, b -> a + b }
                if (t.type == TYPE_IN) tin += t.amount else tout += t.amount
                if (t.createdAt >= monthStart) {
                    if (t.type == TYPE_IN) min += t.amount else mout += t.amount
                }
            }
            return UiModel(
                tx = tx,
                goals = goals.map { GoalStat(it, per[it.id] ?: 0L) },
                goalNames = goals.associate { it.id to it.name },
                balance = tin - tout,
                unassigned = unassigned,
                totalIn = tin,
                totalOut = tout,
                monthIn = min,
                monthOut = mout,
                settings = AppSettings.from(st)
            )
        }
    }
}
