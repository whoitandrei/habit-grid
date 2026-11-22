package ru.nsu.habitgrid.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class RussianPluralTest {

    @Test
    fun `from one day and from twenty one days use singular`() {
        assertEquals(listOf(1, 21, 31, 41, 51, 61, 71, 81, 91, 101, 121), (1..124).filter(::isRussianGenitiveSingular))
    }

    @Test
    fun `other counts use plural`() {
        listOf(0, 2, 4, 5, 9, 11, 12, 14, 19, 20, 25, 111, 124).forEach {
            assertEquals("count $it", false, isRussianGenitiveSingular(it))
        }
    }
}
