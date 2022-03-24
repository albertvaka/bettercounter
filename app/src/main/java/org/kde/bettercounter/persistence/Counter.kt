package org.kde.bettercounter.persistence

import java.util.*

class CounterSummary(var name : String,
                     var count : Int,
                     var color : Int,
                     var interval : Interval,
                     var mostRecent : Date?)

class CounterDetails(var name : String,
                     var color : Int,
                     var interval : Interval,
                     var sortedEntries : List<Entry>)
{
    // When displaying in a chart, LIFETIME counters will still display year by year
    val intervalForChart : Interval = if (interval == Interval.LIFETIME) Interval.YEAR else interval;
}
