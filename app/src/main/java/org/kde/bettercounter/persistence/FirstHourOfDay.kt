package org.kde.bettercounter.persistence

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit

object FirstHourOfDay {

    lateinit var prefs: SharedPreferences

    fun init(application: Application) {
        prefs = application.getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
    }

    fun set(hour: Int) {
        prefs.edit {
            putInt(FIRST_HOUR_OF_DAY_KEY, hour)
        }
    }

    fun get(): Int = prefs.getInt(FIRST_HOUR_OF_DAY_KEY, 0)

    private const val SHARED_PREFS_NAME = "prefs"
    private const val FIRST_HOUR_OF_DAY_KEY = "first_hour_of_day"
}
