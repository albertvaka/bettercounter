package org.kde.bettercounter.persistence

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit

class HourOfDay(
    val hour: Int,
    val minute: Int,
)

object FirstHourOfDay {

    lateinit var prefs: SharedPreferences

    fun init(application: Application) {
        prefs = application.getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
        prefs.edit { remove(AUTO_EXPORT_FILE_URI_KEY) }
    }

    fun set(time: HourOfDay) {
        prefs.edit {
            putInt(FIRST_HOUR_OF_DAY_KEY, time.hour)
            putInt(FIRST_MINUTE_OF_DAY_KEY, time.minute)
        }
    }

    fun get(): HourOfDay {
        val hour = prefs.getInt(FIRST_HOUR_OF_DAY_KEY, 0)
        val minute = prefs.getInt(FIRST_MINUTE_OF_DAY_KEY, 0)
        return HourOfDay(hour, minute)
    }

    private const val SHARED_PREFS_NAME = "prefs"
    private const val FIRST_HOUR_OF_DAY_KEY = "first_hour_of_day"
    private const val FIRST_MINUTE_OF_DAY_KEY = "first_minute_of_day"
}