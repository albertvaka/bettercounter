package org.kde.bettercounter.extensions

import org.junit.Assert.assertEquals
import org.junit.Test
import org.kde.bettercounter.FirstHourOfDayTestBase
import org.kde.bettercounter.persistence.Interval
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date

class ChronoUnitExtensionTest : FirstHourOfDayTestBase() {

    @Test
    fun `millis with less than one second diff`() {
        val from = Date(1697146570502) // 12 October 2023 21:36:10.502 UTC
        val to = Date(1700780933742) // 23 November 2023 23:08:53.742 UTC
        val count = ChronoUnit.DAYS.count(from, to)
        assertEquals(44, count)
    }

    @Test
    fun `millis with more than one second diff`() {
        val from = Date(1697146570502) // 12 October 2023 21:36:10.502 UTC
        val to = Date(1700780934441) // 23 November 2023 23:08:54.441 UTC
        val count = ChronoUnit.DAYS.count(from, to)
        assertEquals(44, count)
    }

    @Test
    fun `one second later is one day`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1700781096000) // 23 November 2023 23:11:36 UTC
        val count = ChronoUnit.DAYS.count(from, to)
        assertEquals(1, count)
    }

    @Test
    fun `one exact day later are two days`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1700867495000) // 24 November 2023 23:11:35 UTC
        val count = ChronoUnit.DAYS.count(from, to)
        assertEquals(2, count)
    }

    @Test
    fun `one second later is one week`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1700781096000) // 23 November 2023 23:11:36 UTC
        val count = ChronoUnit.WEEKS.count(from, to)
        assertEquals(1, count)
    }

    @Test
    fun `one exact week later are two weeks`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1701385895000) // 30 November 2023 23:11:35 UTC
        val count = ChronoUnit.WEEKS.count(from, to)
        assertEquals(2, count)
    }

    @Test
    fun `one second later is one month`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1700781096000) // 23 November 2023 23:11:36 UTC
        val count = ChronoUnit.MONTHS.count(from, to)
        assertEquals(1, count)
    }

    @Test
    fun `one exact month later are two months`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1703373095000) // 23 December 2023 23:11:35 UTC
        val count = ChronoUnit.MONTHS.count(from, to)
        assertEquals(2, count)
    }

    @Test
    fun `one second later is one year`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1700781096000) // 23 November 2023 23:11:36 UTC
        val count = ChronoUnit.YEARS.count(from, to)
        assertEquals(1, count)
    }

    @Test
    fun `one exact year later are two years`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1732403495000) // 23 November 2024 23:11:35 UTC
        val count = ChronoUnit.YEARS.count(from, to)
        assertEquals(2, count)
    }

    private fun daysInRange(year: Int, month: Int, day: Int, interval: Interval): Int {
        val rangeStart = Calendar.getInstance().apply {
            clear()
            set(year, month, day)
        }
        val rangeEnd = rangeStart.plusInterval(interval, 1)
        return ChronoUnit.DAYS.count(rangeStart.time, rangeEnd.lastInstant())
    }

    @Test
    fun `range of a 31 day month is 31 days`() {
        assertEquals(31, daysInRange(2023, Calendar.OCTOBER, 1, Interval.MONTH))
    }

    @Test
    fun `range of a 30 day month is 30 days`() {
        assertEquals(30, daysInRange(2023, Calendar.NOVEMBER, 1, Interval.MONTH))
    }

    @Test
    fun `range of february is 28 days`() {
        assertEquals(28, daysInRange(2023, Calendar.FEBRUARY, 1, Interval.MONTH))
    }

    @Test
    fun `range of february in a leap year is 29 days`() {
        assertEquals(29, daysInRange(2024, Calendar.FEBRUARY, 1, Interval.MONTH))
    }

    @Test
    fun `range of a week is 7 days`() {
        assertEquals(7, daysInRange(2023, Calendar.OCTOBER, 2, Interval.WEEK))
    }
}
