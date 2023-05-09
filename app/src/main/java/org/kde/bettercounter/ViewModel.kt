package org.kde.bettercounter

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Message
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kde.bettercounter.boilerplate.AppDatabase
import org.kde.bettercounter.persistence.*
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class ViewModel(application: Application) : AndroidViewModel(application) {

    interface CounterObserver {
        fun onCounterAdded(counterName: String, isUserAdded : Boolean)
        fun onCounterRemoved(counterName: String)
        fun onCounterRenamed(oldName : String, newName: String)
        fun onCounterDecremented(counterName: String, oldEntryDate: Date)
    }

    private val repo : Repository
    private val counterObservers = HashSet<CounterObserver>()
    private val summaryMap = HashMap<String, MutableLiveData<CounterSummary>>()

    init {
        val db  = AppDatabase.getInstance(application)
        val prefs =  application.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        repo = Repository(application, db.entryDao(), prefs)
        val initialCounters = repo.getCounterList()
        viewModelScope.launch(Dispatchers.IO) {
            for (name in initialCounters) {
                summaryMap[name] = MutableLiveData(repo.getCounterSummary(name)) // cache it
                for (observer in counterObservers) {
                    observer.onCounterAdded(name, false)
                }
            }
        }
    }

    fun saveCounterOrder(value : List<String>) = repo.setCounterList(value)

    fun addCounter(name : String, interval : Interval, color : Int) {
        repo.setCounterList(repo.getCounterList().toMutableList() + name)
        repo.setCounterMetadata(name, color, interval)
        viewModelScope.launch(Dispatchers.IO) {
            for (observer in counterObservers) {
                summaryMap[name] = MutableLiveData(repo.getCounterSummary(name)) // cache it
                withContext(Dispatchers.Main) {
                    observer.onCounterAdded(name, true)
                }
            }
        }
    }

    fun observeCounterChange(observer: CounterObserver) {
        counterObservers.add(observer)
        for (name in summaryMap.keys) { //notify the ones we already have
            observer.onCounterAdded(name, false)
        }
    }

    fun removeCounterChangeObserver(observer: CounterObserver) {
        counterObservers.remove(observer)
    }

    fun incrementCounter(name : String, date : Date = Calendar.getInstance().time) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.addEntry(name, date)
            summaryMap[name]?.postValue(repo.getCounterSummary(name))
        }
    }

    fun decrementCounter(name : String) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldEntryDate = repo.removeEntry(name)
            summaryMap[name]?.postValue(repo.getCounterSummary(name))
            if (oldEntryDate != null) {
                for (observer in counterObservers) {
                    observer.onCounterDecremented(name, oldEntryDate)
                }
            }
        }
    }

    fun setTutorialShown(id: Tutorial) {
        repo.setTutorialShown(id)
    }

    fun isTutorialShown(id: Tutorial) : Boolean {
        return repo.isTutorialShown(id)
    }

    fun editCounterSameName(name : String, interval : Interval, color : Int) {
        repo.setCounterMetadata(name, color, interval)
        viewModelScope.launch(Dispatchers.IO) {
            summaryMap[name]?.postValue(repo.getCounterSummary(name))
        }
    }

    fun editCounter(oldName : String, newName : String, interval : Interval, color : Int) {
        repo.deleteCounterMetadata(oldName)
        repo.setCounterMetadata(newName, color, interval)
        val list = repo.getCounterList().toMutableList()
        list.forEachIndexed { index, element ->
            if (element == oldName) {
                list[index] = newName
            }
        }
        repo.setCounterList(list)

        viewModelScope.launch(Dispatchers.IO) {
            repo.renameCounter(oldName, newName)
            val counter = summaryMap.remove(oldName)
            if (counter != null) {
                summaryMap[newName] = counter
                counter.postValue(repo.getCounterSummary(newName))
            } else {
                // should not happen, in theory we always have summaryMap populated
                summaryMap[newName] = MutableLiveData(repo.getCounterSummary(newName)) // cache it
            }

            for (observer in counterObservers) {
                withContext(Dispatchers.Main) {
                    observer.onCounterRenamed(oldName, newName)
                }
            }

        }
    }

    fun getCounterSummary(name : String) : LiveData<CounterSummary> {
        return summaryMap[name]!!
    }

    fun counterExists(name: String): Boolean = repo.getCounterList().contains(name)

    fun resetCounter(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.removeAllEntries(name)
            summaryMap[name]?.postValue(repo.getCounterSummary(name))
        }
    }

    fun deleteCounter(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.removeAllEntries(name)
            for (observer in counterObservers) {
                withContext(Dispatchers.Main) {
                    observer.onCounterRemoved(name)
                }
            }
        }
        summaryMap.remove(name)
        repo.deleteCounterMetadata(name)
        val list = repo.getCounterList().toMutableList()
        list.remove(name)
        repo.setCounterList(list)
    }

    fun exportAll(stream : OutputStream, progressHandler : Handler?) {

        fun sendProgress(progress : Int) {
            val message = Message()
            message.arg1 = progress
            message.arg2 = repo.getCounterList().size
            progressHandler?.sendMessage(message)
        }

        viewModelScope.launch(Dispatchers.IO) {
            stream.use {
                it.bufferedWriter().use { writer ->
                    for ((i, name) in repo.getCounterList().withIndex()) {
                        sendProgress(i)
                        val entries = repo.getAllEntriesSortedByDate(name)
                        writer.write(name)
                        for (entry in entries) {
                            writer.write(",")
                            writer.write(entry.date.time.toString())
                        }
                        writer.write("\n")
                    }
                    sendProgress(repo.getCounterList().size)
                }
            }
        }
    }

    fun importAll(stream : InputStream, progressHandler : Handler?, defaultInterval : Interval, defaultColor : Int) {

        fun sendProgress(progress : Int, done : Int) {
            val message = Message()
            message.arg1 = progress
            message.arg2 = done // -1 -> error, 0 -> wip, 1 -> done
            progressHandler?.sendMessage(message)
        }

        viewModelScope.launch(Dispatchers.IO) {
            stream.use { stream ->
                // We read everything into memory before we update the DB so we know there are no errors
                val entriesToImport : MutableList<Entry> = mutableListOf()
                val namesToImport : MutableList<String> = mutableListOf()
                try {
                    stream.bufferedReader().use { reader ->
                        reader.forEachLine { line ->
                            val nameAndDates = line.splitToSequence(",").iterator()
                            val name = nameAndDates.next()
                            namesToImport.add(name)
                            nameAndDates.forEach { timestamp ->
                                entriesToImport.add(Entry(name = name, date = Date(timestamp.toLong())))
                            }
                            sendProgress(namesToImport.size, 0)
                        }
                    }
                    namesToImport.forEach { name ->
                        if (!counterExists(name)) {
                            addCounter(name, defaultInterval, defaultColor)
                        }

                    }
                    repo.bulkAddEntries(entriesToImport)
                    sendProgress(namesToImport.size, 1)
                } catch(e : Exception) {
                    e.printStackTrace()
                    sendProgress(namesToImport.size, -1)
                }
            }
        }
    }

    fun getEntriesForRangeSortedByDate(name : String, since: Date, until: Date): LiveData<List<Entry>> {
        val ret = MutableLiveData<List<Entry>>()
        viewModelScope.launch(Dispatchers.IO) {
            val entries = repo.getEntriesForRangeSortedByDate(name, since, until)
            //Log.e("Repository", "Queried ${entries.size} entries")
            viewModelScope.launch(Dispatchers.Main) {
                ret.value = entries
            }
        }
        return ret
    }
}
