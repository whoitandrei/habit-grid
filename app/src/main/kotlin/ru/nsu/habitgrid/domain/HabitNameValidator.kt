package ru.nsu.habitgrid.domain

sealed interface HabitNameValidation {
    data class Valid(val name: String) : HabitNameValidation
    data object Empty : HabitNameValidation
    data object TooLong : HabitNameValidation
}

object HabitNameValidator {
    const val MAX_LENGTH = 30

    fun validate(raw: String): HabitNameValidation {
        val name = raw.trim()
        return when {
            name.isEmpty() -> HabitNameValidation.Empty
            name.length > MAX_LENGTH -> HabitNameValidation.TooLong
            else -> HabitNameValidation.Valid(name)
        }
    }
}
