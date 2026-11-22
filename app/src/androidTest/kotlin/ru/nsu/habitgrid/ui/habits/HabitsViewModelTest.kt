package ru.nsu.habitgrid.ui.habits

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.nsu.habitgrid.data.HabitDatabase
import ru.nsu.habitgrid.data.HabitRepository
import ru.nsu.habitgrid.domain.Habit
import ru.nsu.habitgrid.domain.HabitNameValidation
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class HabitsViewModelTest {

    private val today = LocalDate.of(2026, 10, 15)
    private val clock = Clock.fixed(today.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private lateinit var database: HabitDatabase
    private lateinit var repository: HabitRepository
    private lateinit var viewModel: HabitsViewModel
    private val savedStateHandle = SavedStateHandle()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HabitDatabase::class.java,
        ).build()
        repository = HabitRepository(database, clock)
        viewModel = runBlocking(Dispatchers.Main) { HabitsViewModel(savedStateHandle, repository) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun awaitState(predicate: (HabitsUiState) -> Boolean): HabitsUiState =
        withTimeout(5_000) { viewModel.uiState.first(predicate) }

    private fun onMain(block: HabitsViewModel.() -> Unit) = runBlocking(Dispatchers.Main) { viewModel.block() }

    private suspend fun addThroughDialog(name: String) {
        withContext(Dispatchers.Main) {
            viewModel.onAddClick()
            viewModel.onNameChange(name)
            viewModel.onConfirmEdit(name)
        }
    }

    @Test
    fun loadedEmptyListAllowsAdding() = runBlocking {
        val state = awaitState { !it.isLoading }

        assertTrue(state.habits.isEmpty())
        assertTrue(state.canAdd)
        assertNull(state.dialog)
    }

    @Test
    fun addHabitSavesTrimmedNameAndClosesDialog() = runBlocking {
        awaitState { !it.isLoading }

        addThroughDialog("  Чтение ")

        val state = awaitState { it.habits.isNotEmpty() }
        assertEquals("Чтение", state.habits.single().name)
        assertNull(state.dialog)
    }

    @Test
    fun emptyNameShowsErrorAndSavesNothing() = runBlocking {
        awaitState { !it.isLoading }

        addThroughDialog("   ")

        val state = awaitState { (it.dialog as? HabitDialog.Edit)?.error != null }
        assertEquals(HabitNameValidation.Empty, (state.dialog as HabitDialog.Edit).error)
        assertTrue(repository.habits.first().isEmpty())
    }

    @Test
    fun tooLongNameShowsError() = runBlocking {
        awaitState { !it.isLoading }

        addThroughDialog("а".repeat(31))

        val state = awaitState { (it.dialog as? HabitDialog.Edit)?.error != null }
        assertEquals(HabitNameValidation.TooLong, (state.dialog as HabitDialog.Edit).error)
    }

    @Test
    fun typingClearsError() = runBlocking {
        awaitState { !it.isLoading }
        addThroughDialog("")
        awaitState { (it.dialog as? HabitDialog.Edit)?.error != null }

        onMain { onNameChange("С") }

        val state = awaitState { (it.dialog as? HabitDialog.Edit)?.name == "С" }
        assertNull((state.dialog as HabitDialog.Edit).error)
    }

    @Test
    fun addingIsBlockedAfterFourHabits() = runBlocking {
        repeat(Habit.MAX_COUNT) { repository.addHabit("Привычка $it") }

        val state = awaitState { it.habits.size == Habit.MAX_COUNT }
        assertFalse(state.canAdd)

        onMain { onAddClick() }
        assertNull(awaitState { !it.isLoading }.dialog)
    }

    @Test
    fun doubleConfirmAddsOneHabit() = runBlocking {
        awaitState { !it.isLoading }

        onMain {
            onAddClick()
            onNameChange("Чтение")
            onConfirmEdit("Чтение")
            onConfirmEdit("Чтение")
        }

        awaitState { it.habits.isNotEmpty() }
        assertEquals(1, repository.habits.first().size)
    }

    @Test
    fun renameThroughDialog() = runBlocking {
        repository.addHabit("Чтение")
        val habit = awaitState { it.habits.isNotEmpty() }.habits.single()

        onMain {
            onRenameClick(habit)
            onNameChange("Книги")
            onConfirmEdit("Книги")
        }

        assertEquals("Книги", awaitState { it.habits.single().name != "Чтение" }.habits.single().name)
    }

    @Test
    fun renameDialogStartsWithCurrentName() = runBlocking {
        repository.addHabit("Чтение")
        val habit = awaitState { it.habits.isNotEmpty() }.habits.single()

        onMain { onRenameClick(habit) }

        val dialog = awaitState { it.dialog != null }.dialog
        assertEquals(HabitDialog.Edit(habitId = habit.id, name = "Чтение"), dialog)
    }

    @Test
    fun deleteRequiresConfirmation() = runBlocking {
        repository.addHabit("Чтение")
        val habit = awaitState { it.habits.isNotEmpty() }.habits.single()

        onMain {
            onDeleteClick(habit)
            onDismissDialog()
        }
        assertEquals(1, repository.habits.first().size)

        onMain {
            onDeleteClick(habit)
            onConfirmDelete()
        }
        assertTrue(awaitState { it.habits.isEmpty() }.habits.isEmpty())
    }

    @Test
    fun openDialogSurvivesProcessDeath() = runBlocking {
        awaitState { !it.isLoading }
        onMain {
            onAddClick()
            onNameChange("Прогулка")
        }
        awaitState { (it.dialog as? HabitDialog.Edit)?.name == "Прогулка" }

        val restored = runBlocking(Dispatchers.Main) {
            HabitsViewModel(SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) }), repository)
        }

        val state = withTimeout(5_000) { restored.uiState.first { !it.isLoading } }
        assertEquals(HabitDialog.Edit(habitId = null, name = "Прогулка"), state.dialog)
    }

    @Test
    fun validationErrorSurvivesProcessDeath() = runBlocking {
        awaitState { !it.isLoading }
        onMain {
            onAddClick()
            onConfirmEdit("")
        }
        awaitState { (it.dialog as? HabitDialog.Edit)?.error != null }

        val restored = runBlocking(Dispatchers.Main) {
            HabitsViewModel(SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) }), repository)
        }

        val state = withTimeout(5_000) { restored.uiState.first { !it.isLoading } }
        assertEquals(HabitNameValidation.Empty, (state.dialog as HabitDialog.Edit).error)
    }
}
