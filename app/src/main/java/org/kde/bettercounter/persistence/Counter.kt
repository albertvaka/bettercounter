package org.kde.bettercounter.persistence

import java.util.*

class Counter(var name : String,
              var count : Int,
              var interval : Interval,
              var lastEdit : Date?)

class CounterEntries(var name : String,
                     var interval : Interval,
                     var entries : List<Entry>)
