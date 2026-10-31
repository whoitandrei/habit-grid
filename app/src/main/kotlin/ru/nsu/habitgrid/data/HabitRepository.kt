package ru.nsu.habitgrid.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.nsu.habitgrid.domain.Habit
import ru.nsu.habitgrid.domain.HabitNameValidation
import ru.nsu.habitgrid.domain.HabitNameValidator
import java.time.Clock
import java.time.LocalDate

sealed interface AddHabitResult {
    data class Added(val habit: Habit) : AddHabitResult
    data class InvalidName(val error: HabitNameValidation.Error) : AddHabitResult
    data object LimitReached : AddHabitResult
}

class HabitRepository(
    private val database: HabitDatabase,
    private val clock: Clock,
) {
    private val habitDao = database.habitDao()
    private val completionDao = database.completionDao()

    val habits: Flow<List<Habit>> = habitDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun completions(from: LocalDate, to: LocalDate): Flow<Map<Long, Set<LocalDate>>> =
        completionDao.observeForRange(from, to).map { list ->
            list.groupBy(keySelector = { it.habitId }, valueTransform = { it.date })
                .mapValues { (_, dates) -> dates.toSet() }
        }

    suspend fun addHabit(name: String): AddHabitResult {
        val validName = when (val validation = HabitNameValidator.validate(name)) {
            is HabitNameValidation.Valid -> validation.name
            is HabitNameValidation.Error -> return AddHabitResult.InvalidName(validation)
        }
        return database.withTransaction {
            val usedSlots = habitDao.getAll().map { it.slot }.toSet()
            val slot = (0 until Habit.MAX_COUNT).firstOrNull { it !in usedSlots }
                ?: return@withTransaction AddHabitResult.LimitReached
            val habit = HabitEntity(name = validName, slot = slot, createdDate = today())
            AddHabitResult.Added(habit.copy(id = habitDao.insert(habit)).toDomain())
        }
    }

    suspend fun renameHabit(id: Long, name: String): HabitNameValidation {
        val validation = HabitNameValidator.validate(name)
        if (validation is HabitNameValidation.Valid) habitDao.rename(id, validation.name)
        return validation
    }

    suspend fun deleteHabit(id: Long) {
        habitDao.delete(id)
    }

    suspend fun toggleCompletion(habitId: Long, date: LocalDate) {
        require(date <= today()) { "Cannot mark future date $date" }
        database.withTransaction {
            val habit = habitDao.getById(habitId) ?: return@withTransaction
            require(date >= habit.createdDate) { "Cannot mark $date before habit creation ${habit.createdDate}" }
            val inserted = completionDao.insert(CompletionEntity(habitId, date)) != -1L
            if (!inserted) completionDao.delete(habitId, date)
        }
    }

    private fun today(): LocalDate = LocalDate.now(clock)
}
