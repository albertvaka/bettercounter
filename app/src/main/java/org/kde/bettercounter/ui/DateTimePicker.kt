package org.kde.bettercounter.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Calendar
import java.util.Date

fun showDateTimePicker(activity: AppCompatActivity, initialDateTime: Calendar, callback: (Calendar) -> Unit) {
    val initialHour = initialDateTime.get(Calendar.HOUR_OF_DAY)
    val initialMinute = initialDateTime.get(Calendar.MINUTE)
    val timeFormat = if (DateFormat.is24HourFormat(activity)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
    showDatePicker(activity, initialDateTime) { cal ->
        MaterialTimePicker.Builder()
            .setTimeFormat(timeFormat)
            .setHour(initialHour)
            .setMinute(initialMinute)
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .build().apply {
                addOnPositiveButtonClickListener {
                    cal.set(Calendar.MINUTE, minute)
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    callback(cal)
                }
            }.show(activity.supportFragmentManager, "timePicker")
    }
}

fun showDatePicker(activity: AppCompatActivity, initialDateTime: Calendar, callback: (Calendar) -> Unit) {
    MaterialDatePicker.Builder.datePicker()
        .setSelection(initialDateTime.timeInMillis)
        .build().apply {
            addOnPositiveButtonClickListener {
                val cal = Calendar.getInstance()
                cal.timeInMillis = it
                callback(cal)
            }
        }
        .show(activity.supportFragmentManager, "datePicker")
}
