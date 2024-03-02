package org.kde.bettercounter.extensions

import java.util.Calendar
import java.util.Date

fun Date.toCalendar(): Calendar {
    val cal = Calendar.getInstance()
    cal.time = this
    return cal
}

fun max(a: Date, b: Date): Date {
    return if (a.time > b.time) a else b
}

fun min(a: Date, b: Date): Date {
    return if (a.time < b.time) a else b
}
