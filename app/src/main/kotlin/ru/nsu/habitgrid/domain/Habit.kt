package ru.nsu.habitgrid.domain

import java.time.LocalDate

// датакласс автоматически создает методы equals(), hashCode, toString, copy и componentN
data class Habit(
    val id: Long,
    val name: String,
    val slot: Int,
    val createdDate: LocalDate,
) {
    init {
        require(slot in 0 until MAX_COUNT) { "slot must be in 0 until $MAX_COUNT, was $slot" }
    }

    // статический синглтон всего класса (реал как статик переменная константная в плюсах)
    companion object {
        const val MAX_COUNT = 4
    }
}
