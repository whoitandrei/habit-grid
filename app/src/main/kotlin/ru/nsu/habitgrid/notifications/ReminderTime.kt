package ru.nsu.habitgrid.notifications

import java.time.LocalTime
import java.time.ZonedDateTime

// вычисляет когда следующий будильник (сегодня/завтра)
fun nextReminderTime(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
    val today = now.toLocalDate().atTime(time).atZone(now.zone)
    return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
}
