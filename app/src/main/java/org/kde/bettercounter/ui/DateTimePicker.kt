package org.kde.bettercounter.ui

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
