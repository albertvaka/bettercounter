package org.kde.bettercounter.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar

fun showDateTimePicker(context: Context, initialDateTime: Calendar, callback: (Calendar) -> Unit) {
    val initialHour = initialDateTime.get(Calendar.HOUR_OF_DAY)
    val initialMinute = initialDateTime.get(Calendar.MINUTE)
    val use24HourClock = true
    showDatePicker(context, initialDateTime) { cal ->
        TimePickerDialog(context, { _, hour, minute ->
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            callback(cal)
        }, initialHour, initialMinute, use24HourClock).show()
    }}

fun showDatePicker(context: Context, initialDateTime: Calendar, callback: (Calendar) -> Unit) {
    val initialYear = initialDateTime.get(Calendar.YEAR)
    val initialMonth = initialDateTime.get(Calendar.MONTH)
    val initialDay = initialDateTime.get(Calendar.DAY_OF_MONTH)
    DatePickerDialog(context, { _, year, month, day ->
        val cal = Calendar.getInstance()
        cal.set(year, month, day)
        callback(cal)
    }, initialYear, initialMonth, initialDay).show()
}
