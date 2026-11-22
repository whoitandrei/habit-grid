package ru.nsu.habitgrid.ui.stats

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
import ru.nsu.habitgrid.data.HabitRepository
import ru.nsu.habitgrid.domain.MonthStats
import ru.nsu.habitgrid.domain.monthStats
import java.time.LocalDate
import java.time.YearMonth

data class StatsUiState(
    val month: YearMonth,
    val today: LocalDate,
    val stats: MonthStats = MonthStats(emptyList()),
    val isLoading: Boolean = true,
) {
    val isCurrentMonth: Boolean get() = month == YearMonth.from(today)
}

class StatsViewModel(
    private val savedStateHandle: SavedStateHandle,
    repository: HabitRepository,
    private val today: StateFlow<LocalDate>,
) : ViewModel() {

    private val initialMonth =
        savedStateHandle.get<String>(MONTH_KEY)?.let(YearMonth::parse) ?: YearMonth.from(today.value)

    private val month = savedStateHandle.getStateFlow(MONTH_KEY, initialMonth.toString()).map(YearMonth::parse)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = combine(month, today) { month, today -> month to today }
        .flatMapLatest { (month, today) ->
            combine(
                repository.habits,
                repository.completions(month.atDay(1), month.atEndOfMonth()),
            ) { habits, completions ->
                StatsUiState(
                    month = month,
                    today = today,
                    stats = monthStats(month, today, habits, completions),
                    isLoading = false,
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            StatsUiState(month = initialMonth, today = today.value),
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

    private fun currentMonth(): YearMonth =
        savedStateHandle.get<String>(MONTH_KEY)?.let(YearMonth::parse) ?: initialMonth

    private fun setMonth(month: YearMonth) {
        savedStateHandle[MONTH_KEY] = month.toString()
    }

    companion object {
        const val MONTH_KEY = "month"
    }
}
