package ru.nsu.habitgrid.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.nsu.habitgrid.domain.Habit
import java.time.LocalDate

@Entity(
    tableName = "habits",
    indices = [Index(value = ["slot"], unique = true)],
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val slot: Int,
    val createdDate: LocalDate,
)

fun HabitEntity.toDomain() = Habit(id = id, name = name, slot = slot, createdDate = createdDate)
