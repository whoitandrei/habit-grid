package ru.nsu.habitgrid.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HabitNameValidatorTest {

    @Test
    fun `name is trimmed`() {
        assertEquals(HabitNameValidation.Valid("Спорт"), HabitNameValidator.validate("  Спорт \n"))
    }

    @Test
    fun `empty name is rejected`() {
        assertEquals(HabitNameValidation.Empty, HabitNameValidator.validate(""))
    }

    @Test
    fun `blank name is rejected`() {
        assertEquals(HabitNameValidation.Empty, HabitNameValidator.validate("   "))
    }

    @Test
    fun `name of max length is accepted`() {
        val name = "а".repeat(HabitNameValidator.MAX_LENGTH)
        assertEquals(HabitNameValidation.Valid(name), HabitNameValidator.validate(name))
    }

    @Test
    fun `name longer than max length is rejected`() {
        val name = "а".repeat(HabitNameValidator.MAX_LENGTH + 1)
        assertEquals(HabitNameValidation.TooLong, HabitNameValidator.validate(name))
    }

    @Test
    fun `surrounding spaces do not count toward length`() {
        val name = "а".repeat(HabitNameValidator.MAX_LENGTH)
        assertEquals(HabitNameValidation.Valid(name), HabitNameValidator.validate("  $name  "))
    }
}
