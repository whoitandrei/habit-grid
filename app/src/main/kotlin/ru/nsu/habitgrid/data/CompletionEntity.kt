package ru.nsu.habitgrid.data

import androidx.room.Entity
import androidx.room.ForeignKey
import java.time.LocalDate

// таблица выполнений привычек
@Entity(
    tableName = "completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CompletionEntity(
    val habitId: Long,
    val date: LocalDate,
)
