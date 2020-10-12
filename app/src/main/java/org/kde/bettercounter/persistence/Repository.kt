package org.kde.bettercounter.persistence

import android.content.SharedPreferences
import org.kde.bettercounter.boilerplate.Converters
import java.util.Calendar

const val COUNTERS_PREFS_KEY = "counters"

class Repository(private val entryDao: EntryDao, private val sharedPref : SharedPreferences) {

    private var counters : List<String>

    init {
        val jsonStr = sharedPref.getString(COUNTERS_PREFS_KEY, "[]")
        counters = Converters.stringToStringList(jsonStr)
    }

    fun getCounterList() : List<String> {
        return counters
    }

    fun setCounterList(list : List<String>) {
        val jsonStr = Converters.stringListToString(list)
        sharedPref.edit().putString(COUNTERS_PREFS_KEY, jsonStr).apply()
        counters = list
    }

    suspend fun incrementCounter(name: String) {
        entryDao.insert(Entry(name=name, date= Calendar.getInstance().time))
    }

}