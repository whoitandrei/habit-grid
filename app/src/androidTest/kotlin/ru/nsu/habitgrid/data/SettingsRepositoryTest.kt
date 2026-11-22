package ru.nsu.habitgrid.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    private lateinit var file: File
    private val scopes = mutableListOf<CoroutineScope>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        file = File(context.cacheDir, "settings-test-${System.nanoTime()}.preferences_pb")
    }

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        file.delete()
    }

    private fun newRepository(): SettingsRepository {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }
        return SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
    }

    @Test
    fun defaultsAreDisabledAt21() = runBlocking {
        assertEquals(ReminderSettings(enabled = false, time = LocalTime.of(21, 0)), newRepository().reminder.first())
    }

    @Test
    fun settersReturnAndStoreNewValues() = runBlocking {
        val repository = newRepository()

        assertEquals(ReminderSettings(true, LocalTime.of(21, 0)), repository.setReminderEnabled(true))
        assertEquals(ReminderSettings(true, LocalTime.of(7, 30)), repository.setReminderTime(LocalTime.of(7, 30)))
        assertEquals(ReminderSettings(true, LocalTime.of(7, 30)), repository.reminder.first())
    }

    @Test
    fun valuesSurviveNewDataStoreInstance() = runBlocking {
        val first = newRepository()
        first.setReminderEnabled(true)
        first.setReminderTime(LocalTime.of(23, 45))
        scopes.single().cancel()

        assertEquals(ReminderSettings(true, LocalTime.of(23, 45)), newRepository().reminder.first())
    }

    @Test
    fun disablingKeepsChosenTime() = runBlocking {
        val repository = newRepository()
        repository.setReminderTime(LocalTime.of(6, 5))
        repository.setReminderEnabled(true)

        assertEquals(ReminderSettings(false, LocalTime.of(6, 5)), repository.setReminderEnabled(false))
    }

    @Test
    fun midnightIsStoredCorrectly() = runBlocking {
        val repository = newRepository()

        assertEquals(LocalTime.MIDNIGHT, repository.setReminderTime(LocalTime.MIDNIGHT).time)
        assertEquals(LocalTime.of(23, 59), repository.setReminderTime(LocalTime.of(23, 59)).time)
    }
}
