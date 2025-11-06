package org.kde.bettercounter.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kde.bettercounter.persistence.Entry
import org.kde.bettercounter.persistence.Interval
import java.util.Calendar

class ChartDataAggregationTest {

    private fun createEntry(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Entry {
        val cal = Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Entry(date = cal.time, name = "test")
    }

    private fun createCalendar(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Calendar {
        return Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun `empty entries returns empty aggregation`() {
        val entries = emptyList<Entry>()
        val selectedRangeStart = createCalendar(2025, Calendar.JANUARY, 1)
        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.DAY, selectedRangeStart)
        assertEquals(emptyList<Int>(), buckets)

        val maxCount = ChartDataAggregation.computeMaxCountForAllEntries(entries, Interval.DAY)
        assertEquals(0, maxCount)
    }

    @Test
    fun `single entry in selected range - DAY interval`() {
        val entries = listOf(
            createEntry(2025, Calendar.JANUARY, 5, 10, 0)
        )
        val selectedRangeStart = createCalendar(2025, Calendar.JANUARY, 5)

        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.DAY, selectedRangeStart)
        assertEquals(1, buckets[10])

        val maxCount = ChartDataAggregation.computeMaxCountForAllEntries(entries, Interval.DAY)
        assertEquals(1, maxCount)
    }

    @Test
    fun `multiple entries in same bucket`() {
        val entries = listOf(
            createEntry(2025, Calendar.JANUARY, 5, 10, 15),
            createEntry(2025, Calendar.JANUARY, 5, 10, 30),
            createEntry(2025, Calendar.JANUARY, 5, 10, 30),
        )
        val selectedRangeStart = createCalendar(2025, Calendar.JANUARY, 5)

        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.DAY, selectedRangeStart)
        assertEquals(3, buckets[10]) // Hour 10 should have 3 entries

        val maxCount = ChartDataAggregation.computeMaxCountForAllEntries(entries, Interval.DAY)
        assertEquals(3, maxCount)
    }

    @Test
    fun `entries spanning multiple buckets`() {
        val entries = listOf(
            createEntry(2025, Calendar.JANUARY, 5, 10, 0),
            createEntry(2025, Calendar.JANUARY, 5, 10, 0),
            createEntry(2025, Calendar.JANUARY, 5, 10, 0),
            createEntry(2025, Calendar.JANUARY, 5, 15, 0),
            createEntry(2025, Calendar.JANUARY, 5, 15, 30),
            createEntry(2025, Calendar.JANUARY, 5, 20, 0)
        )
        val selectedRangeStart = createCalendar(2025, Calendar.JANUARY, 5)

        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.DAY, selectedRangeStart)
        assertEquals(3, buckets[10])
        assertEquals(2, buckets[15])
        assertEquals(1, buckets[20])

        val maxCount = ChartDataAggregation.computeMaxCountForAllEntries(entries, Interval.DAY)
        assertEquals(3, maxCount)
    }

    @Test
    fun `HOUR interval - 60 buckets`() {
        val entries = listOf(
            createEntry(2025, Calendar.JANUARY, 5, 10, 15),
            createEntry(2025, Calendar.JANUARY, 5, 10, 45)
        )
        val selectedRangeStart = createCalendar(2025, Calendar.JANUARY, 5, 10, 0)

        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.HOUR, selectedRangeStart)
        assertEquals(60, buckets.size)

        val maxCount = ChartDataAggregation.computeMaxCountForAllEntries(entries, Interval.HOUR)
        assertEquals(1, maxCount) // Each minute has max 1 entry
    }

    @Test
    fun `DAY interval - 24 buckets`() {
        val entries = listOf(
            createEntry(2025, Calendar.JANUARY, 5, 0, 0),
            createEntry(2025, Calendar.JANUARY, 5, 12, 0),
            createEntry(2025, Calendar.JANUARY, 5, 23, 59)
        )
        val selectedRangeStart = createCalendar(2025, Calendar.JANUARY, 5)

        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.DAY, selectedRangeStart)
        assertEquals(24, buckets.size)

        val maxCount = ChartDataAggregation.computeMaxCountForAllEntries(entries, Interval.DAY)
        assertEquals(1, maxCount)
    }

    @Test
    fun `WEEK interval - 7 buckets`() {
        val entries = listOf(
            createEntry(2025, Calendar.JANUARY, 6, 10, 0),  // Monday
            createEntry(2025, Calendar.JANUARY, 7, 10, 0),  // Tuesday
            createEntry(2025, Calendar.JANUARY, 10, 10, 0)  // Friday
        )
        val selectedRangeStart = createCalendar(2025, Calendar.JANUARY, 6)

        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.WEEK, selectedRangeStart)
        assertEquals(7, buckets.size)

        val maxCount = ChartDataAggregation.computeMaxCountForAllEntries(entries, Interval.WEEK)
        assertEquals(1, maxCount)
    }

    @Test
    fun `MONTH interval - days in month buckets`() {
        val entries = listOf(
            createEntry(2025, Calendar.JANUARY, 1, 10, 0),
            createEntry(2025, Calendar.JANUARY, 15, 10, 0),
            createEntry(2025, Calendar.JANUARY, 31, 10, 0)
        )
        val selectedRangeStart = createCalendar(2025, Calendar.JANUARY, 1)

        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.MONTH, selectedRangeStart)
        assertEquals(31, buckets.size)

        val maxCount = ChartDataAggregation.computeMaxCountForAllEntries(entries, Interval.MONTH)
        assertEquals(1, maxCount)
    }

    @Test
    fun `MONTH interval - February leap year`() {
        val entries = listOf(
            createEntry(2024, Calendar.FEBRUARY, 15, 10, 0)
        )
        val selectedRangeStart = createCalendar(2024, Calendar.FEBRUARY, 1)

        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.MONTH, selectedRangeStart)
        assertEquals(29, buckets.size)
    }

    @Test
    fun `MONTH interval - February non-leap year`() {
        val entries = listOf(
            createEntry(2025, Calendar.FEBRUARY, 15, 10, 0)
        )
        val selectedRangeStart = createCalendar(2025, Calendar.FEBRUARY, 1)

        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.MONTH, selectedRangeStart)
        assertEquals(28, buckets.size)
    }

    @Test
    fun `YEAR interval - 12 buckets`() {
        val entries = listOf(
            createEntry(2025, Calendar.JANUARY, 15, 10, 0),
            createEntry(2025, Calendar.JUNE, 15, 10, 0),
            createEntry(2025, Calendar.DECEMBER, 15, 10, 0)
        )
        val selectedRangeStart = createCalendar(2025, Calendar.JANUARY, 1)

        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.YEAR, selectedRangeStart)
        assertEquals(12, buckets.size)

        val maxCount = ChartDataAggregation.computeMaxCountForAllEntries(entries, Interval.YEAR)
        assertEquals(1, maxCount)
    }


    @Test
    fun `all entries in single bucket - HOUR interval`() {
        val entries = listOf(
            createEntry(2025, Calendar.JANUARY, 5, 10, 0),
            createEntry(2025, Calendar.JANUARY, 5, 10, 0),
            createEntry(2025, Calendar.JANUARY, 5, 10, 0),
            createEntry(2025, Calendar.JANUARY, 5, 10, 0),
            createEntry(2025, Calendar.JANUARY, 5, 10, 0)
        )
        val selectedRangeStart = createCalendar(2025, Calendar.JANUARY, 5, 10, 0)

        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.HOUR, selectedRangeStart)
        assertEquals(5, buckets[0]) // All in minute 0

        val maxCount = ChartDataAggregation.computeMaxCountForAllEntries(entries, Interval.DAY)
        assertEquals(5, maxCount)
    }

    @Test
    fun `MONTH interval - entries at start middle and end of month`() {
        val entries = listOf(
            createEntry(2025, Calendar.MARCH, 1, 10, 0),
            createEntry(2025, Calendar.MARCH, 15, 10, 0),
            createEntry(2025, Calendar.MARCH, 31, 23, 59)
        )
        val selectedRangeStart = createCalendar(2025, Calendar.MARCH, 1)

        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.MONTH, selectedRangeStart)
        assertEquals(31, buckets.size) // March has 31 days

        val maxCount = ChartDataAggregation.computeMaxCountForAllEntries(entries, Interval.DAY)
        assertEquals(1, maxCount)
    }

    @Test
    fun `single entry at exact selected range start - HOUR`() {
        val entries = listOf(
            createEntry(2025, Calendar.JANUARY, 5, 10, 0)
        )
        val selectedRangeStart = createCalendar(2025, Calendar.JANUARY, 5, 10, 0)

        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.HOUR, selectedRangeStart)
        assertEquals(60, buckets.size)
        assertEquals(1, buckets[0]) // Entry in minute 0
    }

    @Test(expected = AssertionError::class)
    fun `single entry at exact selected range end - HOUR`() {
        val entries = listOf(
            createEntry(2025, Calendar.JANUARY, 6, 0, 0)
        )
        val selectedRangeStart = createCalendar(2025, Calendar.JANUARY, 5, 10, 0)

        ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.HOUR, selectedRangeStart)
        assertTrue(false) // Should not reach here
    }

    @Test
    fun `boundary test - entry at 59th minute should be in that hour bucket`() {
        val entries = listOf(
            createEntry(2025, Calendar.JANUARY, 5, 10, 0), // 10:00
            createEntry(2025, Calendar.JANUARY, 5, 10, 59), // 10:59
            createEntry(2025, Calendar.JANUARY, 5, 11, 0)  // 11:00
        )
        val selectedRangeStart = createCalendar(2025, Calendar.JANUARY, 5)

        val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(entries, Interval.DAY, selectedRangeStart)
        // Entries at 10:00 and 10:59 should both be in hour 10 bucket
        assertEquals(2, buckets[10])
        assertEquals(1, buckets[11])
    }


}
