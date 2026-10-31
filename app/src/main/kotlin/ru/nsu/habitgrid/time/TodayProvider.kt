package ru.nsu.habitgrid.time

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

class TodayProvider(private val clock: Clock, private val scope: CoroutineScope) {

    private val mutableToday = MutableStateFlow(LocalDate.now(clock))
    val today: StateFlow<LocalDate> = mutableToday.asStateFlow()

    private var midnightJob: Job? = null

    init {
        refresh()
    }

    @Synchronized
    fun refresh() {
        mutableToday.value = LocalDate.now(clock)
        midnightJob?.cancel()
        midnightJob = scope.launch {
            delay(millisUntilNextMidnight())
            refresh()
        }
    }

    private fun millisUntilNextMidnight(): Long {
        val now = ZonedDateTime.now(clock)
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1)
    }
}
