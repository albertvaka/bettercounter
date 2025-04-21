package org.kde.bettercounter.persistence

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.kde.bettercounter.BuildConfig
import org.kde.bettercounter.boilerplate.Converters
import org.kde.bettercounter.extensions.addInterval
import org.kde.bettercounter.extensions.copy
import org.kde.bettercounter.extensions.truncate
import java.util.Calendar
import java.util.Date

const val alwaysShowTutorialsInDebugBuilds = false

const val COUNTERS_PREFS_KEY = "counters"
const val COUNTERS_INTERVAL_PREFS_KEY = "interval.%s"
const val COUNTERS_COLOR_PREFS_KEY = "color.%s"
const val COUNTERS_GOAL_PREFS_KEY = "goal.%s"
const val TUTORIALS_PREFS_KEY = "tutorials"
const val AUTO_EXPORT_ENABLED_KEY = "auto_export_enabled"
const val AVERAGE_CALCULATION_MODE_KEY = "average_calculation_mode"
const val AUTO_EXPORT_FILE_URI_KEY = "auto_export_file_uri"

class Repository(
    private val context: Context,
    private val entryDao: EntryDao,
    private val sharedPref: SharedPreferences,
) {

    private var tutorials: MutableSet<String>
    private var counters: List<String>

    init {
        val countersStr = sharedPref.getString(COUNTERS_PREFS_KEY, "[]")
        counters = Converters.stringToStringList(countersStr)
        tutorials = sharedPref.getStringSet(TUTORIALS_PREFS_KEY, setOf())!!.toMutableSet()
        if (BuildConfig.DEBUG && alwaysShowTutorialsInDebugBuilds) {
            tutorials = mutableSetOf()
        }
    }

    fun getCounterList(): List<String> = counters

    fun setCounterList(list: List<String>) {
        val jsonStr = Converters.stringListToString(list)
        sharedPref.edit { putString(COUNTERS_PREFS_KEY, jsonStr) }
        counters = list
    }

    fun setTutorialShown(id: Tutorial) {
        tutorials.add(id.name)
        sharedPref.edit { putStringSet(TUTORIALS_PREFS_KEY, tutorials) }
    }

    fun resetTutorialShown(id: Tutorial) {
        tutorials.remove(id.name)
        sharedPref.edit { putStringSet(TUTORIALS_PREFS_KEY, tutorials) }
    }

    fun isTutorialShown(id: Tutorial): Boolean {
        return tutorials.contains(id.name)
    }

    private fun getCounterColor(name: String): CounterColor {
        val key = COUNTERS_COLOR_PREFS_KEY.format(name)
        return CounterColor(sharedPref.getInt(key, CounterColor.getDefault(context).colorInt))
    }

    private fun getCounterInterval(name: String): Interval {
        val key = COUNTERS_INTERVAL_PREFS_KEY.format(name)
        val str = sharedPref.getString(key, null)
        return when (str) {
            "YTD" -> Interval.YEAR
            null -> Interval.DEFAULT
            else -> Interval.valueOf(str)
        }
    }

    private fun getCounterGoal(name: String): Int {
        val key = COUNTERS_GOAL_PREFS_KEY.format(name)
        return sharedPref.getInt(key, 0)
    }

    fun deleteCounterMetadata(name: String) {
        val colorKey = COUNTERS_COLOR_PREFS_KEY.format(name)
        val intervalKey = COUNTERS_INTERVAL_PREFS_KEY.format(name)
        val goalKey = COUNTERS_GOAL_PREFS_KEY.format(name)
        sharedPref.edit {
            remove(colorKey)
            remove(intervalKey)
            remove(goalKey)
        }
    }

    fun setCounterMetadata(counter: CounterMetadata) {
        val colorKey = COUNTERS_COLOR_PREFS_KEY.format(counter.name)
        val intervalKey = COUNTERS_INTERVAL_PREFS_KEY.format(counter.name)
        val goalKey = COUNTERS_GOAL_PREFS_KEY.format(counter.name)
        sharedPref.edit {
            putInt(colorKey, counter.color.colorInt)
            putString(intervalKey, counter.interval.toString())
            putInt(goalKey, counter.goal)
        }
    }

    suspend fun getCounterSummary(name: String): CounterSummary {
        val interval = getCounterInterval(name)
        val color = getCounterColor(name)
        val goal = getCounterGoal(name)
        val intervalStartDate = when (interval) {
            Interval.LIFETIME -> Calendar.getInstance().apply { set(Calendar.YEAR, 1990) }
            else -> Calendar.getInstance().apply { truncate(interval) }
        }
        val intervalEndDate = intervalStartDate.copy().apply { addInterval(interval, 1) }
        return CounterSummary(
            name = name,
            color = color,
            interval = interval,
            goal = goal,
            lastIntervalCount = entryDao.getCountInRange(name, intervalStartDate.time, intervalEndDate.time),
            totalCount = entryDao.getCount(name),
            leastRecent = entryDao.getFirstDate(name),
            mostRecent = entryDao.getLastDate(name),
        )
    }

    suspend fun renameCounter(oldName: String, newName: String) {
        entryDao.renameCounter(oldName, newName)
    }

    suspend fun addEntry(name: String, date: Date = Calendar.getInstance().time) {
        entryDao.insert(Entry(name = name, date = date))
    }

    suspend fun removeEntry(name: String): Date? {
        val entry = entryDao.getLastAdded(name)
        if (entry != null) {
            entryDao.delete(entry)
        }
        return entry?.date
    }

    suspend fun removeAllEntries(name: String) {
        entryDao.deleteAll(name)
    }

    suspend fun getEntriesForRangeSortedByDate(name: String, since: Date, until: Date): List<Entry> {
        return entryDao.getAllEntriesInRangeSortedByDate(name, since, until)
    }
    suspend fun getAllEntriesSortedByDate(name: String): List<Entry> {
        return entryDao.getAllEntriesSortedByDate(name)
    }

    suspend fun bulkAddEntries(entries: List<Entry>) {
        entryDao.bulkInsert(entries)
    }

    fun isAutoExportOnSaveEnabled(): Boolean {
        return sharedPref.getBoolean(AUTO_EXPORT_ENABLED_KEY, false)
    }
    
    fun setAutoExportOnSave(enabled: Boolean) {
        sharedPref.edit { putBoolean(AUTO_EXPORT_ENABLED_KEY, enabled) }
    }
    
    fun getAutoExportFileUri(): String? {
        return sharedPref.getString(AUTO_EXPORT_FILE_URI_KEY, null)
    }
    
    fun setAutoExportFileUri(uriString: String) {
        sharedPref.edit { putString(AUTO_EXPORT_FILE_URI_KEY, uriString) }
    }
    
    fun getAverageCalculationMode(): AverageMode {
        val ordinal = sharedPref.getInt(AVERAGE_CALCULATION_MODE_KEY, AverageMode.FIRST_TO_LAST.ordinal)
        return AverageMode.entries[ordinal]
    }
    
    fun setAverageCalculationMode(mode: AverageMode) {
        sharedPref.edit { putInt(AVERAGE_CALCULATION_MODE_KEY, mode.ordinal) }
    }
}
