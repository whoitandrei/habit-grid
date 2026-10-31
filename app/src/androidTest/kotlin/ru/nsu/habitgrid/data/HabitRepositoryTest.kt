package ru.nsu.habitgrid.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.nsu.habitgrid.domain.Habit
import ru.nsu.habitgrid.domain.HabitNameValidation
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class HabitRepositoryTest {

    private val today = LocalDate.of(2026, 10, 15)
    private val clock = Clock.fixed(today.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private lateinit var database: HabitDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HabitDatabase::class.java,
        ).build()
        repository = HabitRepository(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun add(name: String): Habit =
        (repository.addHabit(name) as AddHabitResult.Added).habit

    private suspend fun completionsOf(habit: Habit): Set<LocalDate> =
        repository.completions(today.withDayOfMonth(1), today).first()[habit.id].orEmpty()

    @Test
    fun addHabitTrimsNameAndSetsCreationDateToToday() = runTest {
        val habit = add("  Чтение ")

        assertEquals("Чтение", habit.name)
        assertEquals(today, habit.createdDate)
        assertEquals(listOf(habit), repository.habits.first())
    }

    @Test
    fun habitsTakeSlotsInOrder() = runTest {
        val habits = listOf("Чтение", "Спорт", "Вода", "Сон").map { add(it) }

        assertEquals(listOf(0, 1, 2, 3), habits.map { it.slot })
    }

    @Test
    fun fifthHabitIsRejected() = runTest {
        repeat(Habit.MAX_COUNT) { add("Привычка $it") }

        assertEquals(AddHabitResult.LimitReached, repository.addHabit("Пятая"))
        assertEquals(Habit.MAX_COUNT, repository.habits.first().size)
    }

    @Test
    fun deletedSlotIsReusedFirst() = runTest {
        val habits = listOf("Чтение", "Спорт", "Вода").map { add(it) }

        repository.deleteHabit(habits[1].id)

        assertEquals(1, add("Сон").slot)
    }

    @Test
    fun invalidNameIsNotSaved() = runTest {
        assertEquals(AddHabitResult.InvalidName(HabitNameValidation.Empty), repository.addHabit("   "))
        assertEquals(AddHabitResult.InvalidName(HabitNameValidation.TooLong), repository.addHabit("а".repeat(31)))
        assertTrue(repository.habits.first().isEmpty())
    }

    @Test
    fun parallelAddsNeverExceedLimit() = runTest {
        val results = withContext(Dispatchers.Default) {
            (1..10).map { async { repository.addHabit("Привычка $it") } }.awaitAll()
        }

        assertEquals(Habit.MAX_COUNT, results.count { it is AddHabitResult.Added })
        assertEquals(listOf(0, 1, 2, 3), repository.habits.first().map { it.slot })
    }

    @Test
    fun renameSavesValidName() = runTest {
        val habit = add("Чтение")

        assertEquals(HabitNameValidation.Valid("Книги"), repository.renameHabit(habit.id, " Книги "))
        assertEquals("Книги", repository.habits.first().single().name)
    }

    @Test
    fun renameRejectsInvalidName() = runTest {
        val habit = add("Чтение")

        assertEquals(HabitNameValidation.Empty, repository.renameHabit(habit.id, ""))
        assertEquals("Чтение", repository.habits.first().single().name)
    }

    @Test
    fun deleteHabitRemovesItsCompletions() = runTest {
        val habit = add("Чтение")
        repository.toggleCompletion(habit.id, today)

        repository.deleteHabit(habit.id)

        assertTrue(repository.habits.first().isEmpty())
        assertTrue(repository.completions(today, today).first().isEmpty())
    }

    @Test
    fun toggleTwiceMarksAndUnmarks() = runTest {
        val habit = add("Чтение")

        repository.toggleCompletion(habit.id, today)
        assertEquals(setOf(today), completionsOf(habit))

        repository.toggleCompletion(habit.id, today)
        assertTrue(completionsOf(habit).isEmpty())
    }

    @Test
    fun pastDayCanBeMarked() = runTest {
        val habit = HabitEntity(name = "Чтение", slot = 0, createdDate = today.minusDays(10))
        val id = database.habitDao().insert(habit)

        repository.toggleCompletion(id, today.minusDays(3))

        assertEquals(setOf(today.minusDays(3)), repository.completions(today.minusDays(10), today).first()[id])
    }

    @Test(expected = IllegalArgumentException::class)
    fun futureDayCannotBeMarked() = runTest {
        val habit = add("Чтение")

        repository.toggleCompletion(habit.id, today.plusDays(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun dayBeforeCreationCannotBeMarked() = runTest {
        val habit = add("Чтение")

        repository.toggleCompletion(habit.id, today.minusDays(1))
    }

    @Test
    fun toggleForDeletedHabitDoesNothing() = runTest {
        val habit = add("Чтение")
        repository.deleteHabit(habit.id)

        repository.toggleCompletion(habit.id, today)

        assertTrue(repository.completions(today, today).first().isEmpty())
    }
}
