package org.kde.bettercounter.extensions

import org.kde.bettercounter.persistence.FirstHourOfDay
import org.kde.bettercounter.persistence.Interval
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun Calendar.truncated(field: Int): Calendar {
    val cal = copy()
    cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.SECOND, 0)
    if (field == Calendar.MINUTE) return cal
    cal.set(Calendar.MINUTE, 0)
    if (field == Calendar.HOUR_OF_DAY || field == Calendar.HOUR) return cal
    cal.set(Calendar.HOUR_OF_DAY, FirstHourOfDay.get())
    if (field in listOf(Calendar.DATE, Calendar.DAY_OF_WEEK, Calendar.DAY_OF_MONTH, Calendar.DAY_OF_YEAR)) return cal
    if (field in listOf(Calendar.WEEK_OF_YEAR, Calendar.WEEK_OF_MONTH)) {
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val offset = if (dow < firstDayOfWeek) 7 - (firstDayOfWeek - dow) else dow - firstDayOfWeek
        cal.add(Calendar.DAY_OF_MONTH, -offset)
        return cal
    }
    cal.set(Calendar.DATE, 1)
    if (field == Calendar.MONTH) return cal
    cal.set(Calendar.MONTH, Calendar.JANUARY)
    if (field == Calendar.YEAR) return cal
    error("truncate by $field not implemented")
}

fun Calendar.truncated(field: Interval): Calendar = truncated(field.toChronoUnit().toCalendarField())

fun Calendar.debugToSimpleDateString(): String {
    val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US)
    return dateFormat.format(time)
}

fun Calendar.copy(): Calendar = clone() as Calendar

fun Calendar.plus(field: Int, amount: Int): Calendar {
    val cal = copy()
    cal.add(field, amount)
    return cal
}

fun Calendar.plusInterval(interval: Interval, times: Int): Calendar {
    val cal = copy()
    cal.addInterval(interval, times)
    return cal
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
