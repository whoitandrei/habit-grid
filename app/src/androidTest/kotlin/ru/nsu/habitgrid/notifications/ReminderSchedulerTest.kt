package ru.nsu.habitgrid.notifications

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.nsu.habitgrid.data.ReminderSettings
import java.time.Clock
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class ReminderSchedulerTest {

    private val scheduler = ReminderScheduler(ApplicationProvider.getApplicationContext(), Clock.systemDefaultZone())

    @After
    fun tearDown() {
        scheduler.sync(ReminderSettings(enabled = false))
    }

    @Test
    fun enabledReminderIsScheduled() {
        scheduler.sync(ReminderSettings(enabled = true, time = LocalTime.of(21, 0)))

        assertTrue(scheduler.isScheduled())
    }

    @Test
    fun disablingCancelsReminder() {
        scheduler.sync(ReminderSettings(enabled = true, time = LocalTime.of(21, 0)))

        scheduler.sync(ReminderSettings(enabled = false, time = LocalTime.of(21, 0)))

        assertFalse(scheduler.isScheduled())
    }

    @Test
    fun changingTimeKeepsSingleReminder() {
        scheduler.sync(ReminderSettings(enabled = true, time = LocalTime.of(21, 0)))
        scheduler.sync(ReminderSettings(enabled = true, time = LocalTime.of(7, 30)))

        assertTrue(scheduler.isScheduled())
        scheduler.sync(ReminderSettings(enabled = false))
        assertFalse(scheduler.isScheduled())
    }

    @Test
    fun disablingWhenNothingScheduledIsSafe() {
        scheduler.sync(ReminderSettings(enabled = false))

        assertFalse(scheduler.isScheduled())
    }
}
