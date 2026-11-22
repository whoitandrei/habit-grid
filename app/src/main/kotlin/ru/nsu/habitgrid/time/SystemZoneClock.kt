package ru.nsu.habitgrid.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

// object - объявить класс и сразу создать его единственный экземпляр
object SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()
}
