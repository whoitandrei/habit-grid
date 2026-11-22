package ru.nsu.habitgrid.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthStatsTest {

    private val today = LocalDate.of(2026, 10, 15)
    private val october = YearMonth.of(2026, 10)
    private val reading = Habit(id = 1, name = "Чтение", slot = 0, createdDate = LocalDate.of(2026, 9, 1))
    private val sport = Habit(id = 2, name = "Спорт", slot = 1, createdDate = LocalDate.of(2026, 10, 10))

    private fun days(month: YearMonth, range: IntRange): Set<LocalDate> = range.map(month::atDay).toSet()

    @Test
    fun `past month counts every day`() {
        val september = YearMonth.of(2026, 9)
        val stats = monthStats(september, today, listOf(reading), mapOf(reading.id to days(september, 1..15)))

        val habitStats = stats.habits.single()
        assertEquals(15, habitStats.done)
        assertEquals(30, habitStats.tracked)
        assertEquals(50, habitStats.percent)
    }

    @Test
    fun `current month without today's mark counts days until yesterday`() {
        val stats = monthStats(october, today, listOf(reading), mapOf(reading.id to days(october, 1..7)))

        val habitStats = stats.habits.single()
        assertEquals(7, habitStats.done)
        assertEquals(14, habitStats.tracked)
        assertEquals(50, habitStats.percent)
    }

    @Test
    fun `current month with today's mark counts today`() {
        val completions = days(october, 1..7) + today
        val stats = monthStats(october, today, listOf(reading), mapOf(reading.id to completions))

        val habitStats = stats.habits.single()
        assertEquals(8, habitStats.done)
        assertEquals(15, habitStats.tracked)
        assertEquals(53, habitStats.percent)
    }

    @Test
    fun `future month has no data`() {
        val stats = monthStats(YearMonth.of(2026, 11), today, listOf(reading), emptyMap())

        assertEquals(0, stats.habits.single().tracked)
        assertNull(stats.habits.single().percent)
        assertNull(stats.percent)
    }

    @Test
    fun `month before habit creation has no data`() {
        val stats = monthStats(YearMonth.of(2026, 8), today, listOf(reading), emptyMap())

        assertNull(stats.habits.single().percent)
    }

    @Test
    fun `habit created mid month counts from creation day`() {
        val completions = days(october, 5..5) + days(october, 10..11)
        val stats = monthStats(october, today, listOf(sport), mapOf(sport.id to completions))

        val habitStats = stats.habits.single()
        assertEquals(2, habitStats.done)
        assertEquals(5, habitStats.tracked)
        assertEquals(40, habitStats.percent)
    }

    @Test
    fun `completions in the future are ignored`() {
        val completions = days(october, 1..14) + days(october, 20..25)
        val stats = monthStats(october, today, listOf(reading), mapOf(reading.id to completions))

        assertEquals(14, stats.habits.single().done)
        assertEquals(100, stats.habits.single().percent)
    }

    @Test
    fun `habit without completions is zero percent`() {
        val stats = monthStats(october, today, listOf(reading), emptyMap())

        assertEquals(0, stats.habits.single().percent)
    }

    @Test
    fun `total percent sums days of all habits`() {
        val completions = mapOf(
            reading.id to days(october, 1..7),
            sport.id to days(october, 10..11),
        )
        val stats = monthStats(october, today, listOf(reading, sport), completions)

        assertEquals(9, stats.done)
        assertEquals(19, stats.tracked)
        assertEquals(47, stats.percent)
    }

    @Test
    fun `no habits has no data`() {
        val stats = monthStats(october, today, emptyList(), emptyMap())

        assertTrue(stats.habits.isEmpty())
        assertNull(stats.percent)
    }
}
