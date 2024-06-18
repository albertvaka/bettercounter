package org.kde.bettercounter.extensions

import org.kde.bettercounter.persistence.Interval
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun Calendar.truncate(field: Int) {
    set(Calendar.MILLISECOND, 0)
    set(Calendar.SECOND, 0)
    if (field == Calendar.MINUTE) return
    set(Calendar.MINUTE, 0)
    if (field == Calendar.HOUR_OF_DAY || field == Calendar.HOUR) return
    set(Calendar.HOUR_OF_DAY, 0)
    if (field in listOf(Calendar.DATE, Calendar.DAY_OF_WEEK, Calendar.DAY_OF_MONTH, Calendar.DAY_OF_YEAR)) return
    if (field in listOf(Calendar.WEEK_OF_YEAR, Calendar.WEEK_OF_MONTH)) {
        val dow = get(Calendar.DAY_OF_WEEK)
        val offset = if (dow < firstDayOfWeek) 7 - (firstDayOfWeek - dow) else dow - firstDayOfWeek
        add(Calendar.DAY_OF_MONTH, -offset)
        return
    }
    set(Calendar.DATE, 1)
    if (field == Calendar.MONTH) return
    set(Calendar.MONTH, Calendar.JANUARY)
    if (field == Calendar.YEAR) return
    throw UnsupportedOperationException("truncate by $field not implemented")
}

fun Calendar.truncate(field: Interval) {
    return truncate(field.toChronoUnit().toCalendarField())
}

fun Calendar.debugToSimpleDateString(): String {
    val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US)
    return dateFormat.format(time)
}

fun Calendar.copy(): Calendar {
    return clone() as Calendar
}

fun Calendar.addInterval(interval: Interval, times: Int) {
    when (interval) {
        Interval.HOUR -> add(Calendar.HOUR_OF_DAY, 1 * times)
        Interval.DAY -> add(Calendar.DAY_OF_YEAR, 1 * times)
        Interval.WEEK -> add(Calendar.DAY_OF_YEAR, 7 * times)
        Interval.MONTH -> add(Calendar.MONTH, 1 * times)
        Interval.YEAR -> add(Calendar.YEAR, 1 * times)
        Interval.LIFETIME -> add(Calendar.YEAR, 1000)
    }
}
