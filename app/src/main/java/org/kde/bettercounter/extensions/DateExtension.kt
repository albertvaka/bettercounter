package org.kde.bettercounter.extensions

import java.util.Calendar
import java.util.Date

fun Date.toCalendar(): Calendar {
    val cal = Calendar.getInstance()
    cal.time = this
    return cal
}
