package org.kde.bettercounter.ui.chart

import org.kde.bettercounter.extensions.addInterval
import org.kde.bettercounter.extensions.copy
import org.kde.bettercounter.extensions.plus
import org.kde.bettercounter.extensions.toCalendar
import org.kde.bettercounter.extensions.truncated
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
        val bucketSize = interval.getBucketSubdivisions()
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

        val bucketSize = interval.getBucketSubdivisions()
        val numBuckets = when (bucketSize) {
            Calendar.MINUTE -> 60
            Calendar.HOUR_OF_DAY -> 24
            Calendar.DAY_OF_WEEK -> 7
            Calendar.DAY_OF_MONTH -> rangeStart.getActualMaximum(Calendar.DAY_OF_MONTH)
            Calendar.MONTH -> 12
            else -> error("Invalid bucket size")
        }

        // All given entries should be after rangeStart and before rangeStart+numBuckets
        val cal = rangeStart.copy()
        assert(intervalEntries.first().date.time >= cal.timeInMillis) {
            "Entry on ${intervalEntries.first().date} is not after ${cal.time}"
        }
        val endCal = cal.plus(bucketSize, numBuckets)
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

    fun computeGoalReached(goal : Int, counterInterval: Interval, displayInterval: Interval, entries: List<Entry>): Int {
        if (goal <= 0 || counterInterval == Interval.LIFETIME || displayInterval <= counterInterval) return -1
        if (entries.isEmpty()) return -1
        val counterBegin = entries.firstOrNull()?.date?.toCalendar() ?: Calendar.getInstance()
        val cal = counterBegin.truncated(counterInterval)
        var goalReached = 0
        var entriesIndex = 0
        while (entriesIndex < entries.size) {
            cal.addInterval(counterInterval, 1) // Calendar is now at the end of the current bucket
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
