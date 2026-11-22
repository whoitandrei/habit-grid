package ru.nsu.habitgrid.time

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TodayProviderTest {

    private class SchedulerClock(
        private val scheduler: TestCoroutineScheduler,
        private val start: Instant,
        private var zone: ZoneId,
    ) : Clock() {
        var jump: Duration = Duration.ZERO

        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = SchedulerClock(scheduler, start, zone)

        override fun instant(): Instant = start.plusMillis(scheduler.currentTime).plus(jump)

        fun moveTo(zone: ZoneId) {
            this.zone = zone
        }
    }

    private val novosibirsk = ZoneId.of("Asia/Novosibirsk")

    private fun TestCoroutineScheduler.clockAt(dateTime: LocalDateTime, zone: ZoneId = novosibirsk) =
        SchedulerClock(this, dateTime.atZone(zone).toInstant(), zone)

    @Test
    fun `today changes exactly at midnight`() = runTest {
        val clock = testScheduler.clockAt(LocalDateTime.of(2026, 10, 30, 23, 59))
        val provider = TodayProvider(clock, backgroundScope)
        assertEquals(LocalDate.of(2026, 10, 30), provider.today.value)

        advanceTimeBy(59_999)
        runCurrent()
        assertEquals(LocalDate.of(2026, 10, 30), provider.today.value)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(LocalDate.of(2026, 10, 31), provider.today.value)
    }

    @Test
    fun `today keeps updating every night`() = runTest {
        val clock = testScheduler.clockAt(LocalDateTime.of(2026, 10, 30, 12, 0))
        val provider = TodayProvider(clock, backgroundScope)

        advanceTimeBy(Duration.ofDays(3).toMillis())
        runCurrent()

        assertEquals(LocalDate.of(2026, 11, 2), provider.today.value)
    }

    @Test
    fun `refresh picks up clock change and rearms midnight timer`() = runTest {
        val clock = testScheduler.clockAt(LocalDateTime.of(2026, 10, 30, 12, 0))
        val provider = TodayProvider(clock, backgroundScope)

        clock.jump = Duration.ofDays(5).plusHours(11).plusMinutes(59)
        provider.refresh()
        assertEquals(LocalDate.of(2026, 11, 4), provider.today.value)

        advanceTimeBy(Duration.ofMinutes(1).toMillis())
        runCurrent()
        assertEquals(LocalDate.of(2026, 11, 5), provider.today.value)
    }

    @Test
    fun `refresh picks up time zone change`() = runTest {
        val clock = testScheduler.clockAt(LocalDateTime.of(2026, 10, 30, 23, 0))
        val provider = TodayProvider(clock, backgroundScope)

        clock.moveTo(ZoneId.of("Asia/Tokyo"))
        provider.refresh()

        assertEquals(LocalDate.of(2026, 10, 31), provider.today.value)
    }

    @Test
    fun `without refresh nothing changes before midnight`() = runTest {
        val clock = testScheduler.clockAt(LocalDateTime.of(2026, 10, 30, 0, 0))
        val provider = TodayProvider(clock, backgroundScope)

        advanceTimeBy(Duration.ofHours(23).plusMinutes(59).toMillis())
        runCurrent()

        assertEquals(LocalDate.of(2026, 10, 30), provider.today.value)
    }
}
