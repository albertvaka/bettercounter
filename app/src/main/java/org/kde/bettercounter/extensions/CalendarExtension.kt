package org.kde.bettercounter.extensions

import org.kde.bettercounter.persistence.Interval
import java.text.SimpleDateFormat
import java.util.*

fun Calendar.truncate(field: Int) {
    set(Calendar.SECOND, 0)
    if (field == Calendar.MINUTE) return
    set(Calendar.MINUTE, 0)
    if (field == Calendar.HOUR_OF_DAY) return
    set(Calendar.HOUR_OF_DAY, 0)
    if (field in listOf(Calendar.DATE, Calendar.DAY_OF_WEEK, Calendar.DAY_OF_MONTH, Calendar.DAY_OF_YEAR)) return
    if (field in listOf(Calendar.WEEK_OF_YEAR, Calendar.WEEK_OF_MONTH)) {
        // TODO: Support Sunday as week start day
        add(Calendar.DAY_OF_MONTH, Calendar.MONDAY - get(Calendar.DAY_OF_WEEK));
        return
    }
    set(Calendar.DATE, 1)
    if (field == Calendar.MONTH) return
    set(Calendar.MONTH, Calendar.JANUARY)
    if (field in listOf(Calendar.HOUR)) {
        throw RuntimeException("truncate by $field not implemented")
    }
}

fun Calendar.truncate(field: Interval) {
    when(field) {
        Interval.DAY -> truncate(Calendar.DAY_OF_YEAR)
        Interval.WEEK -> truncate(Calendar.WEEK_OF_YEAR)
        Interval.MONTH -> truncate(Calendar.MONTH)
        Interval.YEAR -> truncate(Calendar.YEAR)
        Interval.LIFETIME -> throw RuntimeException("truncate by $field not implemented")
    }
}

fun Calendar.toSimpleDateString() : String {
    val dateFormat = SimpleDateFormat("dd-MM-yyyy HH-mm", Locale.US)
    return dateFormat.format(time)
}

fun Calendar.copy() : Calendar {
    return clone() as Calendar
}

fun Calendar.addInterval(interval : Interval, times : Int) {
    when (interval) {
        Interval.DAY -> add(Calendar.DAY_OF_YEAR, 1*times)
        Interval.WEEK -> add(Calendar.DAY_OF_YEAR, 7*times)
        Interval.MONTH -> add(Calendar.MONTH, 1*times)
        Interval.YEAR -> add(Calendar.YEAR, 1*times)
        Interval.LIFETIME -> add(Calendar.YEAR, 1000)
    }
}
