package org.kde.bettercounter.extensions

import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date

// Rounds up to the nearest integer. Both dates included. Eg: returns 2 weeks from Monday at 00:00 to next Monday at 00:00
fun ChronoUnit.count(from : Date, to : Date) : Int {
    val count = when (this) {
        ChronoUnit.HOURS -> {
            val hourTruncatedFrom = from.toCalendar().apply { truncate(Calendar.HOUR_OF_DAY) }.time.toZonedDateTime()
            val hourTruncatedTo = to.toCalendar().apply { truncate(Calendar.HOUR_OF_DAY) }.time.toZonedDateTime()
            ChronoUnit.HOURS.between(hourTruncatedFrom, hourTruncatedTo).toInt()
        }
        ChronoUnit.DAYS -> {
            val dayTruncatedFrom = from.toCalendar().apply { truncate(Calendar.DAY_OF_MONTH) }.time.toZonedDateTime()
            val dayTruncatedTo = to.toCalendar().apply { truncate(Calendar.DAY_OF_MONTH) }.time.toZonedDateTime()
            ChronoUnit.DAYS.between(dayTruncatedFrom, dayTruncatedTo).toInt()
        }
        ChronoUnit.WEEKS -> {
            val weekTruncatedFrom = from.toCalendar().apply {truncate(Calendar.WEEK_OF_YEAR) }.time.toZonedDateTime()
            val weekTruncatedTo = to.toCalendar().apply { truncate(Calendar.WEEK_OF_YEAR) }.time.toZonedDateTime()
            ChronoUnit.WEEKS.between(weekTruncatedFrom, weekTruncatedTo).toInt()
        }
        ChronoUnit.MONTHS -> (to.month - from.month) + (to.year - from.year)*12
        ChronoUnit.YEARS -> to.year - from.year
        else -> throw RuntimeException("Counting by $this is not supported")
    }
    //Log.e("Interval", "${count+1} $this between ${from.toCalendar().toSimpleDateString()} and ${to.toCalendar().toSimpleDateString()}")
    return count + 1
}