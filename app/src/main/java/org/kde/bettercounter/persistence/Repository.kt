package org.kde.bettercounter.persistence

import android.content.SharedPreferences
import org.kde.bettercounter.BuildConfig
import org.kde.bettercounter.boilerplate.Converters
import java.util.*
import kotlin.collections.HashMap

const val alwaysShowTutorialsInDebugBuilds = true

const val COUNTERS_PREFS_KEY = "counters"
const val COUNTERS_INTERVAL_PREFS_KEY = "interval.%s"
const val TUTORIALS_PREFS_KEY = "tutorials"

class Repository(
    private val entryDao: EntryDao,
    private val sharedPref : SharedPreferences
) {

    private var tutorials: MutableSet<String>
    private var counters : List<String>
    private var counterCache = HashMap<String, CounterSummary>()

    init {
        val countersStr = sharedPref.getString(COUNTERS_PREFS_KEY, "[]")
        counters = Converters.stringToStringList(countersStr)
        tutorials = sharedPref.getStringSet(TUTORIALS_PREFS_KEY, setOf())!!.toMutableSet()
        if (BuildConfig.DEBUG && alwaysShowTutorialsInDebugBuilds) {
            tutorials = mutableSetOf()
        }
    }

    fun getCounterList() : List<String> {
        return counters
    }

    fun setCounterList(list : List<String>) {
        val jsonStr = Converters.stringListToString(list)
        sharedPref.edit().putString(COUNTERS_PREFS_KEY, jsonStr).apply()
        counters = list
    }

    fun setTutorialShown(id: Tutorial) {
        tutorials.add(id.name)
        sharedPref.edit().putStringSet(TUTORIALS_PREFS_KEY, tutorials).apply()
    }

    fun isTutorialShown(id: Tutorial) : Boolean {
        return tutorials.contains(id.name)
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

    suspend fun getCounterSummary(name : String): CounterSummary {
        val interval = getCounterInterval(name)
        return counterCache.getOrPut(name, {
            CounterSummary(
                name = name,
                count = entryDao.getCountSince(name, interval.toDate()),
                interval = interval,
                mostRecent = entryDao.getMostRecent(name)?.date
            )
        })
    }

    suspend fun renameCounter(oldName : String, newName : String) {
        counterCache.remove(oldName)
        entryDao.renameCounter(oldName, newName)
    }

    suspend fun addEntry(name: String, date: Date = Calendar.getInstance().time) {
        counterCache.remove(name)
        entryDao.insert(Entry(name=name, date=date))
    }

    suspend fun removeEntry(name: String) {
        counterCache.remove(name)
        val entry = entryDao.getLastAdded(name)
        if (entry != null) {
            entryDao.delete(entry)
        }
    }

    suspend fun removeAllEntries(name: String) {
        counterCache.remove(name)
        entryDao.deleteAll(name)
    }

    suspend fun getCounterDetails(name : String): CounterDetails {
        val interval = getCounterInterval(name)
        val entries = entryDao.getAllEntriesInRange(name, interval.toDate(), Calendar.getInstance().time)
        return CounterDetails(name, interval, entries)
    }

    suspend fun getAllEntries(name : String): List<Entry> {
        return entryDao.getAllEntries(name)
    }
}
