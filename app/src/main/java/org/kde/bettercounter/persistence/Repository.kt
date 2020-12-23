package org.kde.bettercounter.persistence

import android.content.SharedPreferences
import org.kde.bettercounter.boilerplate.Converters
import java.util.Calendar

const val COUNTERS_PREFS_KEY = "counters"
const val COUNTERS_INTERVAL_PREFS_KEY = "interval.%s"

class Repository(
    private val entryDao: EntryDao,
    private val sharedPref : SharedPreferences
) {

    private var counters : List<String>
    private var counterCache = HashMap<String, Counter>()

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

    fun getCounterInterval(name : String) : Interval {
        val key = COUNTERS_INTERVAL_PREFS_KEY.format(name)
        val str = sharedPref.getString(key, null)
        return if (str != null) {
            Interval.valueOf(str)
        } else {
            DEFAULT_INTERVAL
        }
    }

    fun setCounterInterval(name : String, interval : Interval) {
        counterCache.remove(name)
        val key = COUNTERS_INTERVAL_PREFS_KEY.format(name)
        sharedPref.edit().putString(key, interval.toString()).apply()
    }

    suspend fun getCounter(name : String): Counter {
        val interval = getCounterInterval(name)
        return counterCache.getOrPut(name, {
            Counter(
                name = name,
                count = entryDao.getCountSince(name, interval.toDate()),
                interval = interval,
                lastEdit = entryDao.getMostRecent(name)?.date
            )
        })
    }

    suspend fun renameCounter(oldName : String, newName : String) {
        counterCache.remove(oldName)
        entryDao.renameCounter(oldName, newName)
    }

    suspend fun addEntry(name: String) {
        counterCache.remove(name)
        entryDao.insert(Entry(name=name, date=Calendar.getInstance().time))
    }

    suspend fun removeEntry(name: String) {
        counterCache.remove(name)
        val entry = entryDao.getMostRecent(name)
        if (entry != null) {
            entryDao.delete(entry)
        }
    }

    suspend fun removeAllEntries(name: String) {
        counterCache.remove(name)
        entryDao.deleteAll(name)
    }

    fun getAllEntriesInCounterInterval(name : String): List<Entry> {
        val interval = getCounterInterval(name)
        return entryDao.getAllEntriesSince(name, interval.toDate())
    }
}
