package org.kde.bettercounter.extensions

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date

// Converts an "old-style" java.util Date to a java.time Date
// This makes the assumption all dates are in the "system default" timezone, whatever that means
// FIXME: This wouldn't be needed if I used modern Dates everywhere
fun Date.toZonedDateTime() : ZonedDateTime {
    return toInstant().atZone(ZoneId.systemDefault())
}

fun Date.toCalendar(): Calendar {
    val cal = Calendar.getInstance()
    cal.time = this
    return cal
}