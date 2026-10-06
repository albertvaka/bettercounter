package org.kde.bettercounter.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import org.kde.bettercounter.FirstHourOfDayTestBase
import org.kde.bettercounter.persistence.AverageMode
import org.kde.bettercounter.persistence.CounterColor
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Interval
import java.util.Calendar
import java.util.Date

// Tests for "Average between first entry and last entry" (FIRST_TO_LAST). The time we measure starts
// at the first entry, so the first entry doesn't count: N entries give an average of N-1 entries over
// the time between the first and last entry. But if the counter has entries outside the range, we
// measure from/to the range limit instead, so all the entries in the range count (otherwise we would
// under-count).
//
// Tests use a weekly counter, so averages are in entries per day. Unless stated otherwise, the counter
// has one entry every day at noon, viewed in a week range.
class AverageStatsTest : FirstHourOfDayTestBase() {

    private fun date(day: Int, hour: Int = 0): Date = calendar(day, hour).time

    private fun calendar(day: Int, hour: Int = 0): Calendar = Calendar.getInstance().apply {
        clear()
        set(2025, Calendar.JANUARY, day, hour, 0, 0)
    }

    // Monday 6th to Monday 13th (exclusive)
    private val rangeStart = calendar(6)
    private val rangeEnd = calendar(13)

    private fun weeklyCounter(leastRecent: Date, mostRecent: Date, totalCount: Int = 0) = CounterSummary(
        name = "test",
        interval = Interval.WEEK,
        goal = 0,
        color = CounterColor(0),
        lastIntervalCount = 0,
        totalCount = totalCount,
        leastRecent = leastRecent,
        mostRecent = mostRecent,
    )

    private fun average(counter: CounterSummary, intervalEntries: Int): Float? =
        AverageStats.getPeriodAverage(counter, intervalEntries, rangeStart, rangeEnd, AverageMode.FIRST_TO_LAST)

    @Test
    fun `entries before and after the range count all entries in the range`() {
        // Entries every two days, from the 1st to the 19th. 3 entries inside the range, on the 7th, 9th
        // and 11th. The data spans from the 6th at 00:00 to the 12th at 23:59, which is 7 days. Without
        // counting the first entry in the range we would get 2 / 6 entries per day.
        val counter = weeklyCounter(leastRecent = date(1, 12), mostRecent = date(19, 12))
        assertEquals(3f / 7f, average(counter, intervalEntries = 3)!!, 0.0001f)
    }

    @Test
    fun `entries before the range count all entries in the range`() {
        // Entries every two days, from the 1st to the 9th. 2 entries inside the range, on the 7th and
        // 9th. The data spans from the 6th at 00:00 to the 9th at 12:00, which is 4 days. Without
        // counting the first entry in the range we would get 1 / 3 entries per day.
        val counter = weeklyCounter(leastRecent = date(1, 12), mostRecent = date(9, 12))
        assertEquals(0.5f, average(counter, intervalEntries = 2)!!, 0.0001f)
    }

    @Test
    fun `entries after the range count all entries in the range`() {
        // Entries every two days, from the 7th to the 19th. 3 entries inside the range, on the 7th, 9th
        // and 11th. The data spans from the 7th at 12:00 to the 12th at 23:59, which is 6 days. Without
        // counting the first entry in the range we would get 2 / 5 entries per day.
        val counter = weeklyCounter(leastRecent = date(7, 12), mostRecent = date(19, 12))
        assertEquals(0.5f, average(counter, intervalEntries = 3)!!, 0.0001f)
    }

    @Test
    fun `entries only inside the range do not count the first entry`() {
        // 4 entries from the 7th to the 10th: 3 entries after the first one, in 3 days.
        val counter = weeklyCounter(leastRecent = date(7, 12), mostRecent = date(10, 12))
        assertEquals(1.0f, average(counter, intervalEntries = 4)!!, 0.0001f)
    }

    @Test
    fun `entries every two days only inside the range`() {
        // 3 entries on the 7th, 9th and 11th: 2 entries after the first one, in 4 days.
        val counter = weeklyCounter(leastRecent = date(7, 12), mostRecent = date(11, 12))
        assertEquals(0.5f, average(counter, intervalEntries = 3)!!, 0.0001f)
    }

    @Test
    fun `entries on the same day only inside the range`() {
        // 2 entries on the 8th: 1 entry after the first one, in less than a day, which we count as 1 day.
        val counter = weeklyCounter(leastRecent = date(8, 10), mostRecent = date(8, 12))
        assertEquals(1.0f, average(counter, intervalEntries = 2)!!, 0.0001f)
    }

    @Test
    fun `an entry exactly at the start of the range does not mean there are entries before the range`() {
        // 3 entries on the 6th, 8th and 10th at 00:00, with the first one exactly at the range start.
        // There are no entries before the range, so it's like any other counter with entries only
        // inside the range: 2 entries after the first one, in 4 days.
        val counter = weeklyCounter(leastRecent = date(6), mostRecent = date(10))
        assertEquals(0.5f, average(counter, intervalEntries = 3)!!, 0.0001f)
    }

    @Test
    fun `a single entry only inside the range has no average`() {
        val counter = weeklyCounter(leastRecent = date(8, 12), mostRecent = date(8, 12))
        assertEquals(null, average(counter, intervalEntries = 1))
    }

    @Test
    fun `a single entry inside the range with entries outside has an average`() {
        // The counter starts on the 12th, the last day of the range. The data spans from the 12th
        // at 12:00 to 23:59, which is 1 day. Without counting the first entry in the range we
        // would get no average at all.
        val counter = weeklyCounter(leastRecent = date(12, 12), mostRecent = date(20, 12))
        assertEquals(1.0f, average(counter, intervalEntries = 1)!!, 0.0001f)
    }

    @Test
    fun `lifetime average does not count the first entry`() {
        // 20 entries from the 1st to the 20th: 19 entries after the first one, in 19 days.
        val counter = weeklyCounter(leastRecent = date(1, 12), mostRecent = date(20, 12), totalCount = 20)
        assertEquals(1.0f, AverageStats.getLifetimeAverage(counter, AverageMode.FIRST_TO_LAST)!!, 0.0001f)
    }

    @Test
    fun `lifetime average of a single entry is not available`() {
        val counter = weeklyCounter(leastRecent = date(8, 12), mostRecent = date(8, 12), totalCount = 1)
        assertEquals(null, AverageStats.getLifetimeAverage(counter, AverageMode.FIRST_TO_LAST))
    }
}
