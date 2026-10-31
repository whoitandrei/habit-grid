package ru.nsu.habitgrid.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun toLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate): Long = date.toEpochDay()
}
