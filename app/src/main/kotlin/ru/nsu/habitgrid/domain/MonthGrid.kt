package ru.nsu.habitgrid.domain

import java.time.LocalDate
import java.time.YearMonth

fun monthWeeks(month: YearMonth): List<List<LocalDate?>> {
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
    val cells: List<LocalDate?> = List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    return cells.chunked(7) { week -> week + List(7 - week.size) { null } }
}
