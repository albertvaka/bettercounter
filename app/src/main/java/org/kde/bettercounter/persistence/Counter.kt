package org.kde.bettercounter.persistence

import java.util.*

class CounterSummary(var name : String,
                     var count : Int,
                     var color : Int,
                     var mostRecent : Date?)

class CounterDetails(var name : String,
                     var color : Int,
                     var interval : Interval,
                     var intervalEntries : List<Entry>)
