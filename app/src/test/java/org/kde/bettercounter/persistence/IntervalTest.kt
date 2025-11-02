package org.kde.bettercounter.persistence

import org.junit.Assert.assertTrue
import org.junit.Test

class IntervalTest {

    @Test
    fun `compare intervals`() {
        assertTrue(Interval.LIFETIME > Interval.YEAR)
        assertTrue(Interval.YEAR > Interval.MONTH)
        assertTrue(Interval.MONTH > Interval.WEEK)
        assertTrue(Interval.WEEK > Interval.DAY)
        assertTrue(Interval.DAY > Interval.HOUR)
    }

}