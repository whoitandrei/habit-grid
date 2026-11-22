package ru.nsu.habitgrid.ui.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.nsu.habitgrid.data.HabitRepository
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: HabitRepository,
    private val today: StateFlow<LocalDate>,
) : ViewModel() {

    private val initialMonth =
        savedStateHandle.get<String>(MONTH_KEY)?.let(YearMonth::parse) ?: YearMonth.from(today.value)

    private val month = savedStateHandle.getStateFlow(MONTH_KEY, initialMonth.toString()).map(YearMonth::parse)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthState = combine(month, today) { month, today -> month to today }
        .flatMapLatest { (month, today) ->
            combine(
                repository.habits,
                repository.completions(month.atDay(1), month.atEndOfMonth()),
            ) { habits, completions ->
                CalendarUiState(
                    month = month,
                    today = today,
                    habits = habits,
                    weeks = calendarWeeks(month, today, habits, completions),
                    isLoading = false,
                )
            }
        }

    private val selectedDate = savedStateHandle.getStateFlow<String?>(SELECTED_DATE_KEY, null)
        .map { it?.let(LocalDate::parse) }

    val uiState: StateFlow<CalendarUiState> =
        combine(monthState, selectedDate) { state, date -> state.withSelectedDate(date) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                CalendarUiState(month = initialMonth, today = today.value),
            )

    fun onPreviousMonth() {
        setMonth(currentMonth().minusMonths(1))
    }

    fun onNextMonth() {
        setMonth(currentMonth().plusMonths(1))
    }

    fun onTodayClick() {
        setMonth(YearMonth.from(today.value))
    }

    fun onDayClick(date: LocalDate) {
        if (date > today.value) return
        savedStateHandle[SELECTED_DATE_KEY] = date.toString()
    }

    fun onOpenTodayRequest(requestId: Long) {
        if (requestId == 0L || savedStateHandle.get<Long>(HANDLED_REQUEST_KEY) == requestId) return
        savedStateHandle[HANDLED_REQUEST_KEY] = requestId
        val date = today.value
        setMonth(YearMonth.from(date))
        savedStateHandle[SELECTED_DATE_KEY] = date.toString()
    }

    fun onDismissDay() {
        savedStateHandle[SELECTED_DATE_KEY] = null
    }

    fun onToggleHabit(habitId: Long) {
        val day = uiState.value.selectedDay ?: return
        val item = day.items.firstOrNull { it.habit.id == habitId } ?: return
        if (!item.isEnabled) return
        viewModelScope.launch { repository.toggleCompletion(habitId, day.date) }
    }

    private fun currentMonth(): YearMonth =
        savedStateHandle.get<String>(MONTH_KEY)?.let(YearMonth::parse) ?: initialMonth

    private fun setMonth(month: YearMonth) {
        savedStateHandle[MONTH_KEY] = month.toString()
    }

    companion object {
        const val MONTH_KEY = "month"
        const val SELECTED_DATE_KEY = "selectedDate"
        const val HANDLED_REQUEST_KEY = "handledOpenTodayRequest"
    }
}
