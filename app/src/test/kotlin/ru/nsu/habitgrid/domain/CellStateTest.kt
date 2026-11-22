package ru.nsu.habitgrid.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CellStateTest {

    private val today = LocalDate.of(2026, 10, 15)
    private val habit = Habit(id = 1, name = "Чтение", slot = 0, createdDate = LocalDate.of(2026, 10, 10))

    @Test
    fun `no habit in slot is empty slot`() {
        assertEquals(CellState.EMPTY_SLOT, cellState(null, today, today, isDone = false))
    }

    @Test
    fun `day before habit creation is not tracked`() {
        assertEquals(CellState.NOT_TRACKED, cellState(habit, LocalDate.of(2026, 10, 9), today, isDone = false))
    }

    @Test
    fun `day after today is future`() {
        assertEquals(CellState.FUTURE, cellState(habit, LocalDate.of(2026, 10, 16), today, isDone = false))
    }

    @Test
    fun `completed past day is done`() {
        assertEquals(CellState.DONE, cellState(habit, LocalDate.of(2026, 10, 12), today, isDone = true))
    }

    @Test
    fun `completed today is done`() {
        assertEquals(CellState.DONE, cellState(habit, today, today, isDone = true))
    }

    @Test
    fun `not completed today is pending`() {
        assertEquals(CellState.PENDING, cellState(habit, today, today, isDone = false))
    }

    @Test
    fun `not completed past day is missed`() {
        assertEquals(CellState.MISSED, cellState(habit, LocalDate.of(2026, 10, 14), today, isDone = false))
    }

    @Test
    fun `creation day in the past without completion is missed`() {
        assertEquals(CellState.MISSED, cellState(habit, habit.createdDate, today, isDone = false))
    }

    @Test
    fun `habit created today is pending today`() {
        val newHabit = habit.copy(createdDate = today)
        assertEquals(CellState.PENDING, cellState(newHabit, today, today, isDone = false))
    }

    @Test
    fun `completion in the future stays future`() {
        assertEquals(CellState.FUTURE, cellState(habit, LocalDate.of(2026, 10, 20), today, isDone = true))
    }

    @Test
    fun `completion before creation stays not tracked`() {
        assertEquals(CellState.NOT_TRACKED, cellState(habit, LocalDate.of(2026, 10, 1), today, isDone = true))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `slot outside 0 to 3 is rejected`() {
        habit.copy(slot = Habit.MAX_COUNT)
    }
}
