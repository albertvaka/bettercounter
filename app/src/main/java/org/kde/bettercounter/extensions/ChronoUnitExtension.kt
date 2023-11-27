package org.kde.bettercounter.extensions

import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date

// Rounds up to the nearest integer. Both dates included. Eg: returns 2 weeks from Monday at 00:00 to next Monday at 00:00
fun ChronoUnit.count(fromDate: Date, toDate: Date): Int {
    val calendarField = toCalendarField()
    val truncatedFrom = fromDate.toCalendar().apply { truncate(calendarField) }
    val truncatedTo = toDate.toCalendar().apply { truncate(calendarField) }
    return between(truncatedFrom, truncatedTo).toInt() + 1
}

fun ChronoUnit.between(from: Calendar, to: Calendar): Long {
    val systemTz = ZoneId.systemDefault()
    // ChronoUnit.between can use both ZonedDateTime and Instant, however,
    // ChronoUnit.WEEK.between doesn't work for Instant for some reason.
    val fromZonedDateTime = from.toInstant().atZone(systemTz)
    val toZonedDateTime = to.toInstant().atZone(systemTz)
    return between(fromZonedDateTime, toZonedDateTime)
}

fun ChronoUnit.toCalendarField(): Int {
    return when (this) {
        ChronoUnit.HOURS -> Calendar.HOUR
        ChronoUnit.DAYS -> Calendar.DAY_OF_WEEK
        ChronoUnit.WEEKS -> Calendar.WEEK_OF_YEAR
        ChronoUnit.MONTHS -> Calendar.MONTH
        ChronoUnit.YEARS -> Calendar.YEAR
        else -> throw UnsupportedOperationException("$this can't be converted to Calendar field")
    }
}
