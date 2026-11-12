package com.savr.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class LogicTest {
    private val base = 1_700_000_000_000L // Nov 2023
    private val now = base + 10 * 86_400_000L
    private fun day(n: Int) = base + n * 86_400_000L

    private fun tx(id: Long, type: String, amount: Long, at: Long, goal: Long? = 1L) =
        SavingsTransaction(id, amount, "t", type, Categories.default(type), goal, at)

    @Test fun rupiah_uses_dot_grouping() {
        assertEquals("Rp\u00A07.000.000", Fmt.rupiah(7_000_000))
        assertEquals("Rp\u00A00", Fmt.rupiah(0))
        assertEquals("-Rp\u00A01.500", Fmt.rupiah(-1_500))
    }

    @Test fun compact_formats() {
        assertEquals("1,5jt", Fmt.compact(1_500_000))
        assertEquals("350rb", Fmt.compact(350_000))
        assertEquals("2M", Fmt.compact(2_000_000_000))
    }

    @Test fun parseAmount_accepts_digits_only_and_limits_length() {
        assertEquals(12_500L, Rules.parseAmount("12.500"))
        assertNull(Rules.parseAmount(""))
        assertNull(Rules.parseAmount("0"))
        assertNull(Rules.parseAmount("1234567890123")) // 13 digit
    }

    @Test fun withdraw_more_than_balance_is_rejected() {
        val all = listOf(tx(1, TYPE_IN, 100_000, day(1)))
        val cand = tx(0, TYPE_OUT, 150_000, day(2))
        assertNotNull(Rules.validateTx(all, setOf(1L), cand, null, now))
    }

    @Test fun withdraw_within_balance_is_accepted() {
        val all = listOf(tx(1, TYPE_IN, 100_000, day(1)))
        val cand = tx(0, TYPE_OUT, 100_000, day(2))
        assertNull(Rules.validateTx(all, setOf(1L), cand, null, now))
    }

    @Test fun withdraw_dated_before_deposit_is_rejected() {
        val all = listOf(tx(1, TYPE_IN, 100_000, day(5)))
        val cand = tx(0, TYPE_OUT, 50_000, day(2))
        assertNotNull(Rules.validateTx(all, setOf(1L), cand, null, now))
    }

    @Test fun balances_are_tracked_per_goal() {
        val all = listOf(tx(1, TYPE_IN, 100_000, day(1), goal = 1L))
        val cand = tx(0, TYPE_OUT, 10_000, day(2), goal = 2L)
        assertNotNull(Rules.validateTx(all, setOf(1L, 2L), cand, null, now))
    }

    @Test fun deleting_a_deposit_needed_by_a_later_withdrawal_is_blocked() {
        val dep = tx(1, TYPE_IN, 100_000, day(1))
        val out = tx(2, TYPE_OUT, 60_000, day(2))
        assertTrue(Rules.breaksIfRemoved(listOf(dep, out), dep))
        assertFalse(Rules.breaksIfRemoved(listOf(dep, out), out))
    }

    @Test fun editing_amount_down_is_valid_but_up_past_balance_is_not() {
        val dep = tx(1, TYPE_IN, 100_000, day(1))
        val out = tx(2, TYPE_OUT, 60_000, day(2))
        val ok = out.copy(amount = 40_000)
        val bad = out.copy(amount = 120_000)
        assertNull(Rules.validateTx(listOf(dep, out), setOf(1L), ok, 2L, now))
        assertNotNull(Rules.validateTx(listOf(dep, out), setOf(1L), bad, 2L, now))
    }

    @Test fun future_dates_and_bad_values_are_rejected() {
        val all = emptyList<SavingsTransaction>()
        assertNotNull(Rules.validateTx(all, setOf(1L), tx(0, TYPE_IN, 1_000, now + 3 * 86_400_000L), null, now))
        assertNotNull(Rules.validateTx(all, setOf(1L), tx(0, TYPE_IN, 0, day(1)), null, now))
        assertNotNull(Rules.validateTx(all, setOf(1L), tx(0, TYPE_IN, Rules.MAX_AMOUNT + 1, day(1)), null, now))
        assertNotNull(Rules.validateTx(all, setOf(1L), tx(0, TYPE_IN, 1_000, day(1)).copy(category = "FOOD"), null, now))
        assertNotNull(Rules.validateTx(all, setOf(1L), tx(0, TYPE_IN, 1_000, day(1), goal = 99L), null, now))
    }

    @Test fun monthly_stats_sum_income_and_expense() {
        val zone = ZoneId.of("UTC")
        val list = listOf(
            tx(1, TYPE_IN, 500_000, now - 86_400_000L),
            tx(2, TYPE_OUT, 200_000, now - 86_400_000L)
        )
        val stats = Analytics.monthlyStats(list, 3, now, zone)
        assertEquals(3, stats.size)
        assertEquals(500_000L, stats.last().income)
        assertEquals(200_000L, stats.last().expense)
        assertEquals(300_000L, stats.last().net)
    }

    @Test fun balance_series_is_cumulative() {
        val zone = ZoneId.of("UTC")
        val list = listOf(tx(1, TYPE_IN, 100_000, now - 2 * 86_400_000L), tx(2, TYPE_OUT, 30_000, now - 86_400_000L))
        val s = Analytics.balanceSeries(list, true, 5, now, zone)
        assertEquals(5, s.size)
        assertEquals(70_000L, s.last().value)
        assertEquals(0L, s.first().value)
    }
}
