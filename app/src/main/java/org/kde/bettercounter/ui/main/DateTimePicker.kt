package org.kde.bettercounter.ui.main

import android.text.format.DateFormat
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.kde.bettercounter.extensions.toEpochMilli
import org.kde.bettercounter.extensions.toLocalDateTime
import org.kde.bettercounter.extensions.toUTCLocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar

class HourOfDay(
    val hour: Int,
    val minute: Int,
)

fun showDateTimePicker(activity: AppCompatActivity, initialDateTime: Calendar, callback: (Calendar) -> Unit) {
    showDatePicker(activity, initialDateTime) { cal ->
        val initialHour = initialDateTime.get(Calendar.HOUR_OF_DAY)
        val initialMinute = initialDateTime.get(Calendar.MINUTE)
        showTimePicker(activity, HourOfDay(initialHour, initialMinute)) {
            cal.set(Calendar.HOUR_OF_DAY, it.hour)
            cal.set(Calendar.MINUTE, it.minute)
            callback(cal)
        }
    }
}

fun showTimePicker(activity: AppCompatActivity, initialHourOfDay: HourOfDay, callback: (HourOfDay) -> Unit) {
    val timeFormat = if (DateFormat.is24HourFormat(activity)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
    MaterialTimePicker.Builder()
        .setTimeFormat(timeFormat)
        .setHour(initialHourOfDay.hour)
        .setMinute(initialHourOfDay.minute)
        .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
        .build().apply {
            addOnPositiveButtonClickListener {
                callback(HourOfDay(hour, minute))
            }
        }.show(activity.supportFragmentManager, "timePicker")
}

fun showDatePicker(activity: AppCompatActivity, initialDateTime: Calendar, callback: (Calendar) -> Unit) {
    // MaterialDatePicker needs UTC, see https://stackoverflow.com/questions/63929730/materialdatepicker-returning-wrong-value/71541489#71541489
    val initialTime = initialDateTime.timeInMillis.toLocalDateTime().atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC)).toEpochMilli()
    MaterialDatePicker.Builder.datePicker()
        .setSelection(initialTime)
        .build().apply {
            addOnPositiveButtonClickListener {
                val cal = Calendar.getInstance()
                // MaterialDatePicker returns UTC
                cal.timeInMillis = it.toUTCLocalDateTime().atZone(ZoneId.systemDefault()).toEpochMilli()
                callback(cal)
            }
        }
        .show(activity.supportFragmentManager, "datePicker")
}
