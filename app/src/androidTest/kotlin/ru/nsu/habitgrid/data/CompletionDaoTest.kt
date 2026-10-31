package ru.nsu.habitgrid.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CompletionDaoTest {

    private lateinit var database: HabitDatabase
    private lateinit var completionDao: CompletionDao
    private var habitId = 0L

    private val oct1 = LocalDate.of(2026, 10, 1)
    private val oct31 = LocalDate.of(2026, 10, 31)

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HabitDatabase::class.java,
        ).build()
        completionDao = database.completionDao()
        habitId = database.habitDao().insert(HabitEntity(name = "Чтение", slot = 0, createdDate = oct1))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeForRangeIncludesBothBounds() = runTest {
        listOf(oct1.minusDays(1), oct1, oct31, oct31.plusDays(1)).forEach {
            completionDao.insert(CompletionEntity(habitId, it))
        }

        val dates = completionDao.observeForRange(oct1, oct31).first().map { it.date }.toSet()

        assertEquals(setOf(oct1, oct31), dates)
    }

    @Test
    fun duplicateInsertIsIgnored() = runTest {
        val first = completionDao.insert(CompletionEntity(habitId, oct1))
        val second = completionDao.insert(CompletionEntity(habitId, oct1))

        assertTrue(first != -1L)
        assertEquals(-1L, second)
        assertEquals(1, completionDao.observeForRange(oct1, oct31).first().size)
    }

    @Test
    fun deleteRemovesOnlyGivenDay() = runTest {
        completionDao.insert(CompletionEntity(habitId, oct1))
        completionDao.insert(CompletionEntity(habitId, oct31))

        completionDao.delete(habitId, oct1)

        assertEquals(listOf(CompletionEntity(habitId, oct31)), completionDao.observeForRange(oct1, oct31).first())
    }

    @Test
    fun completionForMissingHabitIsRejected() = runTest {
        val result = runCatching { completionDao.insert(CompletionEntity(habitId + 100, oct1)) }

        assertTrue(result.exceptionOrNull() is SQLiteConstraintException)
    }
}
