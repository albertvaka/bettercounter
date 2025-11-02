package org.kde.bettercounter.ui.chart

import org.kde.bettercounter.extensions.copy
import org.kde.bettercounter.extensions.toCalendar
import org.kde.bettercounter.extensions.truncated
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Entry
import org.kde.bettercounter.persistence.Interval
import java.util.Calendar

object ChartDataAggregation {

    // Gets called once per ChartAdapter and is reused for all its pages
    fun computeMaxCountForAllEntries(
        entries: List<Entry>, // must be sorted by date from old to new
        interval: Interval,
    ): Int {
        val counterBegin = entries.firstOrNull()?.date?.toCalendar() ?: Calendar.getInstance()
        val cal = counterBegin.truncated(interval)
        val bucketSize = interval.getBucketSize()
        var maxCount = 0
        var entriesIndex = 0
        while (entriesIndex < entries.size) {
            cal.add(bucketSize, 1) // Calendar is now at the end of the current bucket
            var countInBucket = 0
            while (entriesIndex < entries.size && entries[entriesIndex].date.time < cal.timeInMillis) {
                countInBucket++
                entriesIndex++
            }
            if (countInBucket > maxCount) {
                maxCount = countInBucket
            }
        }
        return maxCount
    }

    // Gets called once for each page of the ChartAdapter
    fun computeBucketsForIntervalEntries(
        intervalEntries: List<Entry>, // must be sorted by date from old to new
        interval: Interval,
        rangeStart: Calendar,
    ): List<Int> {
        if (intervalEntries.isEmpty()) {
            return emptyList()
        }

        val bucketSize = interval.getBucketSize()
        val numBuckets = when (interval) {
            Interval.HOUR -> 60
            Interval.DAY -> 24
            Interval.WEEK -> 7
            Interval.MONTH -> rangeStart.getActualMaximum(Calendar.DAY_OF_MONTH)
            Interval.YEAR -> 12
            else -> throw RuntimeException("Invalid interval")
        }

        val cal = rangeStart.copy()

        // All given entries should be after rangeStart and before rangeStart+numBuckets
        assert(intervalEntries.first().date.time >= cal.timeInMillis) {
            "Entry on ${intervalEntries.first().date} is not after ${cal.time}"
        }
        val endCal =
            ((cal.clone() as Calendar).apply { add(bucketSize, numBuckets) })
        assert(intervalEntries.last().date.time < endCal.timeInMillis) {
            "Entry on ${intervalEntries.last().date} is not before ${endCal.time}"
        }

        val buckets: MutableList<Int> = ArrayList(numBuckets)
        var entriesIndex = 0
        repeat(numBuckets) {
            cal.add(bucketSize, 1) // Calendar is now at the end of the current bucket
            var countInBucket = 0
            while (entriesIndex < intervalEntries.size && intervalEntries[entriesIndex].date.time < cal.timeInMillis) {
                countInBucket++
                entriesIndex++
            }
            // Log.e("Bucket", "$bucket (ends ${cal.debugToSimpleDateString()}) -> $countInBucket")
            buckets.add(countInBucket)
        }
        return buckets
    }

    fun computeGoalReached(counter: CounterSummary, interval: Interval, entries: List<Entry>): Int {
        if (counter.goal <= 0 || counter.interval == Interval.LIFETIME || interval <= counter.interval) return -1
        if (entries.isEmpty()) return -1
        val counterBegin = entries.firstOrNull()?.date?.toCalendar() ?: Calendar.getInstance()
        val cal = counterBegin.truncated(counter.interval)
        val bucketSize = counter.interval.getBucketSize()
        val goal = counter.goal
        var goalReached = 0
        var entriesIndex = 0
        while (entriesIndex < entries.size) {
            cal.add(bucketSize, 1) // Calendar is now at the end of the current bucket
            var countInBucket = 0
            while (entriesIndex < entries.size && entries[entriesIndex].date.time < cal.timeInMillis) {
                countInBucket++
                entriesIndex++
            }
            if (countInBucket >= goal) {
                goalReached++
            }
        }
        return goalReached
    }
}