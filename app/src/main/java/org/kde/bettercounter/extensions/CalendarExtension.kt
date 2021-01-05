package org.kde.bettercounter.extensions

import java.text.SimpleDateFormat
import java.util.*

fun Calendar.truncate(field: Int) {
    set(Calendar.SECOND, 0)
    if (field == Calendar.MINUTE) return
    set(Calendar.MINUTE, 0)
    if (field == Calendar.HOUR_OF_DAY) return
    set(Calendar.HOUR_OF_DAY, 0)
    if (field in listOf(Calendar.DATE, Calendar.DAY_OF_WEEK, Calendar.DAY_OF_MONTH, Calendar.DAY_OF_YEAR)) return
    set(Calendar.DATE, 1)
    if (field == Calendar.MONTH) return
    set(Calendar.MONTH, Calendar.JANUARY)
    if (field in listOf(Calendar.HOUR, Calendar.WEEK_OF_YEAR, Calendar.WEEK_OF_MONTH)) {
        throw RuntimeException("truncate by $field not implemented")
    }
}

fun Calendar.toSimpleDateString() : String {
    val dateFormat = SimpleDateFormat("dd-MM-yyyy HH-mm", Locale.US)
    return dateFormat.format(time)
}
