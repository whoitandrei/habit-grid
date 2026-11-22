package ru.nsu.habitgrid.domain

// sealed говорит о том что все наследники/подклассы должны быть известны на этапе компиляции
sealed interface HabitNameValidation {
    data class Valid(val name: String) : HabitNameValidation
    sealed interface Error : HabitNameValidation
    data object Empty : Error
    data object TooLong : Error
}

object HabitNameValidator {
    const val MAX_LENGTH = 30

    // провалидировать строку обрезать лишнее и тд
    fun validate(raw: String): HabitNameValidation {
        val name = raw.trim()
        return when {
            name.isEmpty() -> HabitNameValidation.Empty
            name.length > MAX_LENGTH -> HabitNameValidation.TooLong
            else -> HabitNameValidation.Valid(name)
        }
    }
}
