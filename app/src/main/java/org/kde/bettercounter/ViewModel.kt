package org.kde.bettercounter

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Message
import androidx.lifecycle.*
import kotlinx.coroutines.*
import org.kde.bettercounter.boilerplate.AppDatabase
import org.kde.bettercounter.persistence.*
import java.io.OutputStream
import java.util.*
import kotlin.collections.HashMap


class ViewModel(application: Application) : AndroidViewModel(application) {

    fun interface CounterAddedObserver {
        fun onCounterAdded(name: String, isUserCreated : Boolean)
    }

    private val repo : Repository
    private val addCounterObservers = HashMap<LifecycleOwner, CounterAddedObserver>()
    private val summaryMap = HashMap<String, MutableLiveData<CounterSummary>>()
    private val detailsMap = HashMap<String, MutableLiveData<CounterDetails>>()

    init {
        val db  = AppDatabase.getInstance(application)
        val prefs =  application.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        repo = Repository(application, db.entryDao(), prefs)
        val initialCounters = repo.getCounterList()
        viewModelScope.launch(Dispatchers.IO) {
            for (name in initialCounters) {
                summaryMap[name] = MutableLiveData(repo.getCounterSummary(name)) // cache it
                for ((_, observer) in addCounterObservers) {
                    observer.onCounterAdded(name, false)
                }
            }
        }
    }

    fun saveCounterOrder(value : List<String>) = repo.setCounterList(value)

    fun addCounter(name : String, interval : Interval, color : Int) {
        repo.setCounterList(repo.getCounterList().toMutableList() + name)
        repo.setCounterInterval(name, interval)
        repo.setCounterColor(name, color)
        viewModelScope.launch(Dispatchers.IO) {
            for ((_, observer) in addCounterObservers) {
                summaryMap[name] = MutableLiveData(repo.getCounterSummary(name)) // cache it
                withContext(Dispatchers.Main) {
                    observer.onCounterAdded(name, true)
                }
            }
        }
    }

    fun observeNewCounter(owner : LifecycleOwner, observer: CounterAddedObserver) {
        addCounterObservers[owner] = observer
        for (name in summaryMap.keys) { //notify the ones we already have
            observer.onCounterAdded(name, false)
        }
    }

    fun incrementCounter(name : String, date : Date = Calendar.getInstance().time) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.addEntry(name, date)
            summaryMap[name]?.postValue(repo.getCounterSummary(name))
            detailsMap[name]?.postValue(repo.getCounterDetails(name))
        }
    }

    fun decrementCounter(name : String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.removeEntry(name)
            summaryMap[name]?.postValue(repo.getCounterSummary(name))
            detailsMap[name]?.postValue(repo.getCounterDetails(name))
        }
    }

    fun setTutorialShown(id: Tutorial) {
        repo.setTutorialShown(id)
    }

    fun isTutorialShown(id: Tutorial) : Boolean {
        return repo.isTutorialShown(id)
    }

    fun getCounterColor(name : String) : Int {
        return repo.getCounterColor(name)
    }

    fun getCounterInterval(name : String) : Interval {
        return repo.getCounterInterval(name)
    }

    fun editCounterSameName(name : String, interval : Interval, color : Int) {
        repo.setCounterColor(name, color)
        repo.setCounterInterval(name, interval)
        viewModelScope.launch(Dispatchers.IO) {
            summaryMap[name]?.postValue(repo.getCounterSummary(name))
            detailsMap[name]?.postValue(repo.getCounterDetails(name))
        }
    }

    fun editCounter(oldName : String, newName : String, interval : Interval, color : Int) {
        repo.setCounterColor(newName, color)
        repo.setCounterInterval(newName, interval)
        repo.removeCounterColor(oldName)
        repo.removeCounterInterval(oldName)
        viewModelScope.launch(Dispatchers.IO) {
            repo.renameCounter(oldName, newName)
            val counter = summaryMap.remove(oldName)
            if (counter != null) {
                summaryMap[newName] = counter
                counter.postValue(repo.getCounterSummary(newName))
            }

            val entries = detailsMap.remove(oldName)
            if (entries != null) {
                detailsMap[newName] = entries
                entries.postValue(repo.getCounterDetails(newName))
            }
        }
    }

    fun getCounterSummary(name : String) : LiveData<CounterSummary> {
        return summaryMap[name]!!
    }

    fun counterExists(name: String): Boolean = repo.getCounterList().contains(name)

    fun deleteCounter(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.removeAllEntries(name)
            repo.removeCounterColor(name)
            repo.removeCounterInterval(name)
            summaryMap.remove(name)
            detailsMap.remove(name)
        }
    }

    fun getCounterDetails(name : String) : LiveData<CounterDetails> {
        var liveData = detailsMap[name]
        if (liveData == null) {
            liveData = MutableLiveData()
            viewModelScope.launch(Dispatchers.IO) {
                liveData.postValue(repo.getCounterDetails(name))
            }
        }
        detailsMap[name] = liveData
        return liveData
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
                        val entries = repo.getAllEntries(name)
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

}
