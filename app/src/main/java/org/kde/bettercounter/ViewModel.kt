package org.kde.bettercounter

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kde.bettercounter.boilerplate.AppDatabase
import org.kde.bettercounter.extensions.toCalendar
import org.kde.bettercounter.extensions.truncated
import org.kde.bettercounter.persistence.AverageMode
import org.kde.bettercounter.persistence.CounterColor
import org.kde.bettercounter.persistence.CounterMetadata
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Entry
import org.kde.bettercounter.persistence.Interval
import org.kde.bettercounter.persistence.Repository
import org.kde.bettercounter.persistence.Tutorial
import java.io.InputStream
import java.io.OutputStream
import java.util.Calendar
import java.util.Date

private const val TAG = "ViewModel"

class ViewModel(val application: Application) {

    interface CounterObserver {
        fun onInitialCountersLoaded()
        fun onCounterAdded(counterName: String)
        fun onCounterRemoved(counterName: String)
        fun onCounterRenamed(oldName: String, newName: String)
        fun onCounterDecremented(counterName: String, oldEntryDate: Date)
    }

    private val repo: Repository
    private val counterObservers = HashSet<CounterObserver>()
    private val summaryMap = HashMap<String, MutableLiveData<CounterSummary>>()

    private var initialized = false
    private var autoExportJob : Job? = null

    init {
        val db = AppDatabase.getInstance(application)
        val prefs = application.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        repo = Repository(application, db.entryDao(), prefs)
        val initialCounters = repo.getCounterList()
        for (name in initialCounters) {
            summaryMap[name] = MutableLiveData()
        }
        CoroutineScope(Dispatchers.IO).launch {
            for (name in initialCounters) {
                summaryMap[name]?.postValue(repo.getCounterSummary(name))
            }
            withContext(Dispatchers.Main) {
                synchronized(this) {
                    for (observer in counterObservers) {
                        observer.onInitialCountersLoaded()
                    }
                    initialized = true
                }
            }
        }
    }

    @MainThread
    fun observeCounterChange(observer: CounterObserver) {
        Log.d(TAG, "observeCounterChange SIZE" + counterObservers.size)
        synchronized(this) {
            counterObservers.add(observer)
            if (initialized) {
                observer.onInitialCountersLoaded()
            }
        }
    }

    fun addCounter(counter: CounterMetadata) {
        val name = counter.name
        repo.setCounterList(repo.getCounterList().toMutableList() + name)
        repo.setCounterMetadata(counter)
        CoroutineScope(Dispatchers.IO).launch {
            summaryMap[name] = MutableLiveData(repo.getCounterSummary(name))
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
            summaryMap[name]?.postValue(repo.getCounterSummary(name))
            autoExportIfEnabled()
        }
    }

    fun incrementCounterWithCallback(name: String, date: Date = Calendar.getInstance().time, callback: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            repo.addEntry(name, date)
            summaryMap[name]?.postValue(repo.getCounterSummary(name))
            CoroutineScope(Dispatchers.Main).launch {
                callback()
            }
            autoExportIfEnabled()
        }
    }

    fun decrementCounter(name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val oldEntryDate = repo.removeEntry(name)
            summaryMap[name]?.postValue(repo.getCounterSummary(name))
            if (oldEntryDate != null) {
                for (observer in counterObservers) {
                    observer.onCounterDecremented(name, oldEntryDate)
                }
            }
            autoExportIfEnabled()
        }
    }

    fun setTutorialShown(id: Tutorial) {
        repo.setTutorialShown(id)
    }

    fun resetTutorialShown(id: Tutorial) {
        repo.resetTutorialShown(id)
    }

    fun isTutorialShown(id: Tutorial): Boolean {
        return repo.isTutorialShown(id)
    }

    fun editCounterSameName(counterMetadata: CounterMetadata) {
        val name = counterMetadata.name
        repo.setCounterMetadata(counterMetadata)

        // no need to auto-export here, since metadata doesn't get exported as of today

        CoroutineScope(Dispatchers.IO).launch {
            summaryMap[name]?.postValue(repo.getCounterSummary(name))
        }
    }

    fun editCounter(oldName: String, counterMetadata: CounterMetadata) {
        val newName = counterMetadata.name
        repo.deleteCounterMetadata(oldName)
        repo.setCounterMetadata(counterMetadata)
        val list = repo.getCounterList().toMutableList()
        list[list.indexOf(oldName)] = newName
        repo.setCounterList(list)

        CoroutineScope(Dispatchers.IO).launch {
            repo.renameCounter(oldName, newName)
            val counter: MutableLiveData<CounterSummary>? = summaryMap.remove(oldName)
            if (counter == null) {
                Log.e(TAG, "Trying to rename a counter but the old counter doesn't exist")
                return@launch
            }
            summaryMap[newName] = counter
            counter.postValue(repo.getCounterSummary(newName))
            withContext(Dispatchers.Main) {
                for (observer in counterObservers) {
                    observer.onCounterRenamed(oldName, newName)
                }
            }
            autoExportIfEnabled()
        }
    }

    fun getCounterSummary(name: String): LiveData<CounterSummary> {
        return summaryMap[name]!!
    }

    fun counterExists(name: String): Boolean = repo.getCounterList().contains(name)

    fun getCounterList() = repo.getCounterList()

    fun saveCounterOrder(value: List<String>) = repo.setCounterList(value)

    fun resetCounter(name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            repo.removeAllEntries(name)
            summaryMap[name]?.postValue(repo.getCounterSummary(name))
            autoExportIfEnabled()
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
        val list = repo.getCounterList().toMutableList()
        list.remove(name)
        repo.setCounterList(list)
        autoExportIfEnabled()
    }

    fun exportAll(stream: OutputStream, progressCallback: (progress: Int) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            stream.use {
                it.bufferedWriter().use { writer ->
                    for ((i, name) in repo.getCounterList().withIndex()) {
                        progressCallback(i)
                        val entries = repo.getAllEntriesSortedByDate(name)
                        writer.write(name)
                        for (entry in entries) {
                            writer.write(",")
                            writer.write(entry.date.time.toString())
                        }
                        writer.write("\n")
                    }
                    progressCallback(repo.getCounterList().size)
                }
            }
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
                // Disable all tutorials, they can cause problems when we update all the flows at once
                Tutorial.entries.forEach { tutorial -> setTutorialShown(tutorial) }
                try {
                    stream.bufferedReader().use { reader ->
                        reader.forEachLine { line ->
                            parseImportLine(line, namesToImport, entriesToImport)
                            progressCallback(namesToImport.size, 0)
                        }
                    }
                    val reusedCounterMetadata = CounterMetadata("", Interval.DEFAULT, 0, CounterColor.getDefault(context))
                    namesToImport.forEach { name ->
                        if (!counterExists(name)) {
                            reusedCounterMetadata.name = name
                            addCounter(reusedCounterMetadata)
                        }
                    }
                    repo.bulkAddEntries(entriesToImport)
                    summaryMap.forEach { (name, flow) ->
                        flow.postValue(repo.getCounterSummary(name))
                    }
                    progressCallback(namesToImport.size, 1)
                } catch (e: Exception) {
                    e.printStackTrace()
                    progressCallback(namesToImport.size, -1)
                }
            }
        }
    }

    fun getEntriesForRangeSortedByDate(name: String, since: Date, until: Date): LiveData<List<Entry>> {
        val ret = MutableLiveData<List<Entry>>()
        // TODO: Maybe we should cancel the existing coroutine if already running?
        CoroutineScope(Dispatchers.IO).launch {
            val entries = repo.getEntriesForRangeSortedByDate(name, since, until)
            //Log.e(TAG, "Queried ${entries.size} entries")
            CoroutineScope(Dispatchers.Main).launch {
                ret.value = entries
            }
        }
        return ret
    }

    fun getMaxCountForInterval(name: String, interval: Interval) : LiveData<Int> {
        val ret = MutableLiveData<Int>()
        // TODO: Maybe we should cancel the existing coroutine if already running?
        CoroutineScope(Dispatchers.IO).launch {
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
            CoroutineScope(Dispatchers.Main).launch {
                ret.value = maxCount
            }
        }
        return ret
    }

    suspend fun refreshAllObservers() {
        for ((name, summary) in summaryMap) {
            summary.postValue(repo.getCounterSummary(name))
        }
    }

    fun autoExportIfEnabled() {
        if (!repo.isAutoExportOnSaveEnabled()) {
            return
        }
        autoExportJob?.cancel() // Ensure we don't have two exports running in parallel
        autoExportJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val uri = getAutoExportFileUri()!!.toUri()
                application.contentResolver.openOutputStream(uri, "wt")?.let { stream ->
                    exportAll(stream) { }
                }
            } catch (_: Exception) {
                setAutoExportOnSave(false)
                Toast.makeText(application, R.string.export_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun isAutoExportOnSaveEnabled(): Boolean {
        return repo.isAutoExportOnSaveEnabled()
    }

    fun setAutoExportOnSave(enabled: Boolean) {
        repo.setAutoExportOnSave(enabled)
    }

    fun getAutoExportFileUri(): String? {
        return repo.getAutoExportFileUri()
    }

    fun setAutoExportFileUri(uriString: String) {
        repo.setAutoExportFileUri(uriString)
    }

    fun getAverageCalculationMode(): AverageMode {
        return repo.getAverageCalculationMode()
    }

    fun setAverageCalculationMode(mode: AverageMode) {
        repo.setAverageCalculationMode(mode)
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
