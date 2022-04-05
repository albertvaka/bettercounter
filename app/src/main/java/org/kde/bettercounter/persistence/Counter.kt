package org.kde.bettercounter.persistence

import java.util.*

class CounterSummary(var name : String,
                     var color : Int,
                     var interval : Interval,
                     var lastIntervalCount : Int,
                     var totalCount : Int,
                     var leastRecent : Date?,
                     var mostRecent : Date?,
) {
    // When displaying in a chart, LIFETIME counters will still display year by year
    val intervalForChart : Interval = if (interval == Interval.LIFETIME) Interval.YEAR else interval;

    fun latestBetweenNowAndMostRecentEntry() : Date {
        val now = Calendar.getInstance().time
        val lastEntry = mostRecent
        return if (lastEntry != null && lastEntry > now) lastEntry else now
    }
}
