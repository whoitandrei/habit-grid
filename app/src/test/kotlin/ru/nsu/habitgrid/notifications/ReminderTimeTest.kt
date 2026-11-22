package ru.nsu.habitgrid.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTimeTest {

    private val zone = ZoneId.of("Asia/Novosibirsk")

    private fun at(dateTime: String, zoneId: ZoneId = zone): ZonedDateTime =
        LocalDateTime.parse(dateTime).atZone(zoneId)

    @Test
    fun `time later today fires today`() {
        assertEquals(at("2026-10-30T21:00"), nextReminderTime(at("2026-10-30T12:00"), LocalTime.of(21, 0)))
    }

    @Test
    fun `time already passed fires tomorrow`() {
        assertEquals(at("2026-10-31T21:00"), nextReminderTime(at("2026-10-30T21:30"), LocalTime.of(21, 0)))
    }

    @Test
    fun `exactly at reminder time schedules next day`() {
        assertEquals(at("2026-10-31T21:00"), nextReminderTime(at("2026-10-30T21:00"), LocalTime.of(21, 0)))
    }

    @Test
    fun `midnight reminder after evening fires next midnight`() {
        assertEquals(at("2026-10-31T00:00"), nextReminderTime(at("2026-10-30T23:59"), LocalTime.MIDNIGHT))
    }

    @Test
    fun `crosses month and year`() {
        assertEquals(at("2027-01-01T08:00"), nextReminderTime(at("2026-12-31T09:00"), LocalTime.of(8, 0)))
    }

    @Test
    fun `nonexistent time on spring forward day shifts by the gap`() {
        val newYork = ZoneId.of("America/New_York")
        val result = nextReminderTime(at("2026-03-08T01:00", newYork), LocalTime.of(2, 30))

        assertEquals(at("2026-03-08T03:30", newYork), result)
    }

    @Test
    fun `fall back day keeps wall clock time`() {
        val newYork = ZoneId.of("America/New_York")
        val result = nextReminderTime(at("2026-11-01T00:30", newYork), LocalTime.of(21, 0))

        assertEquals(LocalDateTime.parse("2026-11-01T21:00"), result.toLocalDateTime())
    }
}
