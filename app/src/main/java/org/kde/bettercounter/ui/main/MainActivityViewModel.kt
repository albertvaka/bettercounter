package org.kde.bettercounter.ui.main

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.kde.bettercounter.BuildConfig
import org.kde.bettercounter.extensions.millisecondsUntilNextHour
import org.kde.bettercounter.extensions.toCalendar
import org.kde.bettercounter.extensions.truncated
import org.kde.bettercounter.persistence.AverageMode
import org.kde.bettercounter.persistence.CounterColors
import org.kde.bettercounter.persistence.CounterMetadata
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Entry
import org.kde.bettercounter.persistence.Exporter
import org.kde.bettercounter.persistence.Interval
import org.kde.bettercounter.persistence.Repository
import org.kde.bettercounter.persistence.Tutorial
import org.kde.bettercounter.ui.widget.WidgetProvider
import java.io.InputStream
import java.io.OutputStream
import java.util.Calendar
import java.util.Date

const val alwaysShowTutorialsInDebugBuilds = false

private const val TAG = "ViewModel"

class MainActivityViewModel(val application: Application) {

    interface CounterObserver {
        fun onInitialCountersLoaded()
        fun onCounterAdded(counterName: String)
        fun onCounterRemoved(counterName: String)
        fun onCounterRenamed(oldName: String, newName: String)
        fun onCounterDecremented(counterName: String, oldEntryDate: Date)
    }

    private val repo: Repository = Repository.create(application)
    private val exporter : Exporter  = Exporter(application, repo)

    private var counters: List<String>
    private val counterObservers = HashSet<CounterObserver>()
    private val summaryMap = HashMap<String, MutableStateFlow<CounterSummary>>()
    private val tutorialsShown: MutableSet<String>

    private val mutex = Mutex()
    private var initialized = false

    init {
        counters = repo.getCounterList()
        tutorialsShown = if (BuildConfig.DEBUG && alwaysShowTutorialsInDebugBuilds) {
            mutableSetOf()
        } else {
            repo.getTutorialsShown().toMutableSet()
        }
        CoroutineScope(Dispatchers.IO).launch {
            for (name in counters) {
                summaryMap[name] = MutableStateFlow(repo.getCounterSummary(name))
            }
            withContext(Dispatchers.Main) {
                synchronized(this) {
                    for (observer in counterObservers) {
                        observer.onInitialCountersLoaded()
                    }
                    initialized = true
                    WidgetProvider.refreshWidgets(application)
                }
            }
            // Start updating counters every hour
            while (isActive) {
                delay(millisecondsUntilNextHour())
                refreshAllCounters()
            }
        }
    }

    @MainThread
    fun observeCounterChange(observer: CounterObserver) {
        Log.d(TAG, "observeCounterChange size=${counterObservers.size}")
        synchronized(this) {
            counterObservers.add(observer)
            if (initialized) {
                observer.onInitialCountersLoaded()
            }
        }
    }

    fun addCounter(counter: CounterMetadata) {
        val name = counter.name
        counters = counters + name
        repo.setCounterList(counters)
        repo.setCounterMetadata(counter)
        CoroutineScope(Dispatchers.IO).launch {
            summaryMap[name] = MutableStateFlow(repo.getCounterSummary(name))
            withContext(Dispatchers.Main) {
                for (observer in counterObservers) {
                    observer.onCounterAdded(name)
                }
            }
        }
    }

    fun removeCounterChangeObserver(observer: CounterObserver) {
        counterObservers.remove(observer)
    }

    fun incrementCounter(name: String, date: Date = Calendar.getInstance().time) {
        CoroutineScope(Dispatchers.IO).launch {
            repo.addEntry(name, date)
            mutex.withLock {
                summaryMap[name]?.value = repo.getCounterSummary(name)
            }
            WidgetProvider.refreshWidget(application, name)
            exporter.autoExportIfEnabled()
        }
    }

    fun decrementCounter(name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val oldEntryDate = repo.removeEntry(name)
            mutex.withLock {
                summaryMap[name]?.value = repo.getCounterSummary(name)
            }
            if (oldEntryDate != null) {
                for (observer in counterObservers) {
                    observer.onCounterDecremented(name, oldEntryDate)
                }
            }
            WidgetProvider.refreshWidget(application, name)
            exporter.autoExportIfEnabled()
        }
    }

    fun setTutorialShown(id: Tutorial) {
        tutorialsShown.add(id.name)
        repo.setTutorialsShown(tutorialsShown)
    }

    fun unsetTutorialShown(id: Tutorial) {
        tutorialsShown.remove(id.name)
        repo.setTutorialsShown(tutorialsShown)
    }

    fun isTutorialShown(id: Tutorial): Boolean {
        return tutorialsShown.contains(id.name)
    }

    fun editCounterSameName(counterMetadata: CounterMetadata) {
        val name = counterMetadata.name
        repo.setCounterMetadata(counterMetadata)

        // no need to auto-export here, since metadata doesn't get exported as of today

        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                summaryMap[name]?.value = repo.getCounterSummary(name)
            }
            WidgetProvider.refreshWidgets(application)
        }
    }

    fun editCounter(oldName: String, counterMetadata: CounterMetadata) {
        val newName = counterMetadata.name
        repo.deleteCounterMetadata(oldName)
        repo.setCounterMetadata(counterMetadata)
        val list = counters.toMutableList()
        list[list.indexOf(oldName)] = newName
        counters = list
        repo.setCounterList(list)

        CoroutineScope(Dispatchers.IO).launch {
            repo.renameCounter(oldName, newName)
            val counterFlow = summaryMap.remove(oldName)
            if (counterFlow == null) {
                Log.e(TAG, "Trying to rename a counter but the old counter doesn't exist")
                return@launch
            }
            summaryMap[newName] = counterFlow
            mutex.withLock {
                counterFlow.value = repo.getCounterSummary(newName)
            }
            WidgetProvider.renameCounter(application, oldName, newName)
            withContext(Dispatchers.Main) {
                for (observer in counterObservers) {
                    observer.onCounterRenamed(oldName, newName)
                }
            }
            exporter.autoExportIfEnabled()
        }
    }

    fun getCounterSummary(name: String): StateFlow<CounterSummary> {
        return summaryMap[name]!!
    }

    fun counterExists(name: String): Boolean = counters.contains(name)

    fun getCounterList() = counters

    fun saveCounterOrder(value: List<String>) {
        counters = value
        repo.setCounterList(value)
    }

    fun resetCounter(name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            repo.removeAllEntries(name)
            mutex.withLock {
                summaryMap[name]?.value = repo.getCounterSummary(name)
            }
            WidgetProvider.refreshWidget(application, name)
            exporter.autoExportIfEnabled()
        }
    }

    fun deleteCounter(name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            repo.removeAllEntries(name)
            withContext(Dispatchers.Main) {
                for (observer in counterObservers) {
                    observer.onCounterRemoved(name)
                }
            }
        }
        summaryMap.remove(name)
        repo.deleteCounterMetadata(name)
        counters = counters - name
        repo.setCounterList(counters)
        exporter.autoExportIfEnabled()
        WidgetProvider.removeWidgets(application, name)
    }

    fun getEntriesForRangeSortedByDate(name: String, since: Date, until: Date) = flow {
        emit(repo.getEntriesForRangeSortedByDate(name, since, until))
    }.flowOn(Dispatchers.IO)

    fun getMaxCountForInterval(name: String, interval: Interval) = flow {
        val counterBegin = repo.getLeastRecentEntry(name)?.toCalendar() ?: Calendar.getInstance()
        val cal = counterBegin.truncated(interval)
        val entries = repo.getAllEntriesSortedByDate(name)
        val bucketIntervalAsCalendarField = interval.asCalendarField()
        var maxCount = 0
        var entriesIndex = 0
        while (entriesIndex < entries.size) {
            cal.add(bucketIntervalAsCalendarField, 1) // Calendar is now at the end of the current bucket
            var bucketCount = 0
            while (entriesIndex < entries.size && entries[entriesIndex].date.time < cal.timeInMillis) {
                bucketCount++
                entriesIndex++
            }
            if (bucketCount > maxCount) {
                maxCount = bucketCount
            }
        }
        emit(maxCount)
    }

    private suspend fun refreshAllCounters() {
        Log.d(TAG, "refreshAllCounters called")
        mutex.withLock {
            for ((name, summary) in summaryMap) {
                summary.value = repo.getCounterSummary(name)
            }
        }
    }

    fun refreshCounter(counterName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                summaryMap[counterName]?.value = repo.getCounterSummary(counterName)
            }
        }
    }

    fun getAverageCalculationMode(): AverageMode {
        return repo.getAverageCalculationMode()
    }

    fun exportAll(stream: OutputStream, progressCallback: (progress: Int) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            exporter.exportAll(stream, progressCallback)
        }
    }

    fun importAll(
        context: Context,
        stream: InputStream,
        progressCallback: (progress: Int, status: Int) -> Unit, // status: -1 -> error, 0 -> wip, 1 -> done
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            stream.use { stream ->
                // We read everything into memory before we update the DB so we know there are no errors
                val namesToImport: MutableList<String> = mutableListOf()
                val entriesToImport: MutableList<Entry> = mutableListOf()
                try {
                    stream.bufferedReader().use { reader ->
                        reader.forEachLine { line ->
                            parseImportLine(
                                line,
                                namesToImport,
                                entriesToImport
                            )
                            progressCallback(namesToImport.size, 0)
                        }
                    }
                    val reusedCounterMetadata = CounterMetadata("", Interval.DEFAULT, 0, CounterColors.getInstance(context).defaultColor)
                    namesToImport.forEach { name ->
                        if (!counterExists(name)) {
                            reusedCounterMetadata.name = name
                            addCounter(reusedCounterMetadata)
                        }
                    }
                    repo.bulkAddEntries(entriesToImport)
                    mutex.withLock {
                        summaryMap.forEach { (name, counterFlow) ->
                            counterFlow.value = repo.getCounterSummary(name)
                        }
                    }
                    progressCallback(namesToImport.size, 1)
                    WidgetProvider.refreshWidgets(application)
                } catch (e: Exception) {
                    e.printStackTrace()
                    progressCallback(namesToImport.size, -1)
                }
            }
        }
    }


    companion object {
        fun parseImportLine(line: String, namesToImport: MutableList<String>, entriesToImport: MutableList<Entry>) {
            val nameAndDates = line.splitToSequence(",").iterator()
            var name = nameAndDates.next()
            var nameEnded = false
            nameAndDates.forEach { timestamp ->
                // Hack to support counters with commas in their names
                val timestampLong = if (nameEnded) {
                    timestamp.toLong()
                } else {
                    val maybeTimestamp = timestamp.toLongOrNull()
                    if (maybeTimestamp == null || maybeTimestamp < 100000000000L) {
                        name += ",$timestamp"
                        return@forEach
                    }
                    nameEnded = true
                    maybeTimestamp
                }
                entriesToImport.add(Entry(name = name, date = Date(timestampLong)))
            }
            namesToImport.add(name)
        }
    }

}
