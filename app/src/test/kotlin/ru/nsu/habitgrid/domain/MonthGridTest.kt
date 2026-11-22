package ru.nsu.habitgrid.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthGridTest {

    @Test
    fun `month starting on monday has no leading blanks`() {
        val weeks = monthWeeks(YearMonth.of(2026, 6))

        assertEquals(LocalDate.of(2026, 6, 1), weeks.first().first())
        assertEquals(5, weeks.size)
        assertEquals(listOf(LocalDate.of(2026, 6, 29), LocalDate.of(2026, 6, 30)), weeks.last().filterNotNull())
        assertMonthLayout(YearMonth.of(2026, 6), weeks)
    }

    @Test
    fun `month starting on sunday has six leading blanks`() {
        val weeks = monthWeeks(YearMonth.of(2026, 3))

        assertEquals(List(6) { null } + LocalDate.of(2026, 3, 1), weeks.first())
        assertEquals(6, weeks.size)
        assertMonthLayout(YearMonth.of(2026, 3), weeks)
    }

    @Test
    fun `february of a leap year has 29 days`() {
        val weeks = monthWeeks(YearMonth.of(2024, 2))

        assertEquals(29, weeks.flatten().filterNotNull().size)
        assertEquals(LocalDate.of(2024, 2, 29), weeks.flatten().filterNotNull().last())
        assertMonthLayout(YearMonth.of(2024, 2), weeks)
    }

    @Test
    fun `february of a common year has 28 days`() {
        val weeks = monthWeeks(YearMonth.of(2026, 2))

        assertEquals(28, weeks.flatten().filterNotNull().size)
        assertMonthLayout(YearMonth.of(2026, 2), weeks)
    }

    @Test
    fun `february starting on monday fits exactly four weeks`() {
        val weeks = monthWeeks(YearMonth.of(2021, 2))

        assertEquals(4, weeks.size)
        assertTrue(weeks.flatten().none { it == null })
    }

    private fun assertMonthLayout(month: YearMonth, weeks: List<List<LocalDate?>>) {
        assertTrue(weeks.all { it.size == 7 })
        weeks.forEach { week ->
            week.forEachIndexed { index, date ->
                if (date != null) assertEquals(index + 1, date.dayOfWeek.value)
            }
        }
        assertEquals((1..month.lengthOfMonth()).map(month::atDay), weeks.flatten().filterNotNull())
    }
}
