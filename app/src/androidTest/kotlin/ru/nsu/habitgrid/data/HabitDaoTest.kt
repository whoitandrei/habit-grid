package ru.nsu.habitgrid.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HabitDaoTest {

    private lateinit var database: HabitDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var completionDao: CompletionDao

    private val date = LocalDate.of(2026, 10, 1)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HabitDatabase::class.java,
        ).build()
        habitDao = database.habitDao()
        completionDao = database.completionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeAllReturnsHabitsOrderedBySlot() = runTest {
        habitDao.insert(HabitEntity(name = "Спорт", slot = 2, createdDate = date))
        habitDao.insert(HabitEntity(name = "Чтение", slot = 0, createdDate = date))

        val habits = habitDao.observeAll().first()

        assertEquals(listOf("Чтение", "Спорт"), habits.map { it.name })
        assertEquals(listOf(0, 2), habits.map { it.slot })
    }

    @Test
    fun localDateSurvivesRoundTrip() = runTest {
        val id = habitDao.insert(HabitEntity(name = "Вода", slot = 0, createdDate = date))

        assertEquals(date, habitDao.getById(id)?.createdDate)
    }

    @Test
    fun renameChangesOnlyName() = runTest {
        val id = habitDao.insert(HabitEntity(name = "Чтение", slot = 1, createdDate = date))

        habitDao.rename(id, "Книги")

        assertEquals(HabitEntity(id, "Книги", 1, date), habitDao.getById(id))
    }

    @Test
    fun deleteRemovesHabit() = runTest {
        val id = habitDao.insert(HabitEntity(name = "Чтение", slot = 0, createdDate = date))

        habitDao.delete(id)

        assertNull(habitDao.getById(id))
    }

    @Test
    fun deleteHabitCascadesToCompletions() = runTest {
        val id = habitDao.insert(HabitEntity(name = "Чтение", slot = 0, createdDate = date))
        val otherId = habitDao.insert(HabitEntity(name = "Спорт", slot = 1, createdDate = date))
        completionDao.insert(CompletionEntity(id, date))
        completionDao.insert(CompletionEntity(id, date.plusDays(1)))
        completionDao.insert(CompletionEntity(otherId, date))

        habitDao.delete(id)

        val left = completionDao.observeForRange(date, date.plusDays(1)).first()
        assertEquals(listOf(CompletionEntity(otherId, date)), left)
    }

    @Test
    fun duplicateSlotIsRejected() = runTest {
        habitDao.insert(HabitEntity(name = "Чтение", slot = 0, createdDate = date))

        val result = runCatching { habitDao.insert(HabitEntity(name = "Спорт", slot = 0, createdDate = date)) }

        assertTrue(result.exceptionOrNull() is SQLiteConstraintException)
    }
}
