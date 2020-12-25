package org.kde.bettercounter.persistence

import java.util.*

class CounterSummary(var name : String,
                     var count : Int,
                     var interval : Interval,
                     var mostRecent : Date?)

class CounterDetails(var name : String,
                     var interval : Interval,
                     var intervalEntries : List<Entry>)
