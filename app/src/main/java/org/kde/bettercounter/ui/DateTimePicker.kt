package org.kde.bettercounter.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar

fun showDateTimePicker(context: Context, initialDateTime: Calendar, callback: (Calendar) -> Unit) {
    val initialYear = initialDateTime.get(Calendar.YEAR)
    val initialMonth = initialDateTime.get(Calendar.MONTH)
    val initialDay = initialDateTime.get(Calendar.DAY_OF_MONTH)
    val initialHour = initialDateTime.get(Calendar.HOUR_OF_DAY)
    val initialMinute = initialDateTime.get(Calendar.MINUTE)
    val use24HourClock = true
    DatePickerDialog(context, { _, year, month, day ->
        TimePickerDialog(context, { _, hour, minute ->
            val cal = Calendar.getInstance()
            cal.set(year, month, day, hour, minute)
            callback(cal)
        }, initialHour, initialMinute, use24HourClock).show()
    }, initialYear, initialMonth, initialDay).show()
}
