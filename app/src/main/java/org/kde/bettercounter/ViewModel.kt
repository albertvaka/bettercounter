package org.kde.bettercounter

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.*
import org.kde.bettercounter.boilerplate.AppDatabase
import org.kde.bettercounter.persistence.*
import kotlin.collections.HashMap


class ViewModel(application: Application) : AndroidViewModel(application) {

    fun interface CounterAddedObserver {
        fun onCounterAdded(name: String, isUserCreated : Boolean)
    }

    private val repo : Repository
    private val addCounterObservers = HashMap<LifecycleOwner, CounterAddedObserver>()
    private val counterMap = HashMap<String, MutableLiveData<Counter>>()
    private val entriesMap = HashMap<String, MutableLiveData<CounterEntries>>()

    init {
        val db  = AppDatabase.getInstance(application)
        val prefs =  application.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        repo = Repository(db.entryDao(), prefs)
        val initialCounters = repo.getCounterList()
        viewModelScope.launch(Dispatchers.IO) {
            for (name in initialCounters) {
                counterMap[name] = MutableLiveData(repo.getCounter(name)) // cache it
                for ((_, observer) in addCounterObservers) {
                    observer.onCounterAdded(name, false)
                }
            }
        }
    }

    fun saveCounterOrder(value : List<String>) = repo.setCounterList(value)

    fun addCounter(name : String, interval : Interval) {
        repo.setCounterList(repo.getCounterList().toMutableList() + name)
        repo.setCounterInterval(name, interval)
        viewModelScope.launch(Dispatchers.IO) {
            for ((_, observer) in addCounterObservers) {
                counterMap[name] = MutableLiveData(repo.getCounter(name)) // cache it
                withContext(Dispatchers.Main) {
                    observer.onCounterAdded(name, true)
                }
            }
        }
    }

    fun observeNewCounter(owner : LifecycleOwner, observer: CounterAddedObserver) {
        addCounterObservers[owner] = observer
        for (name in counterMap.keys) { //notify the ones we already have
            observer.onCounterAdded(name, false)
        }
    }

    fun renameCounter(oldName : String, newName : String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.renameCounter(oldName, newName)
            val counter = counterMap.remove(oldName)
            if (counter != null) {
                counterMap[newName] = counter
                counter.postValue(repo.getCounter(newName))
            }

            val entries = entriesMap.remove(oldName)
            if (entries != null) {
                entriesMap[newName] = entries
                entries.postValue(repo.getAllEntriesInCounterInterval(newName))
            }
        }
    }

    fun incrementCounter(name : String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.addEntry(name)
            counterMap[name]?.postValue(repo.getCounter(name))
            entriesMap[name]?.postValue(repo.getAllEntriesInCounterInterval(name))
        }
    }

    fun decrementCounter(name : String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.removeEntry(name)
            counterMap[name]?.postValue(repo.getCounter(name))
            entriesMap[name]?.postValue(repo.getAllEntriesInCounterInterval(name))
        }
    }

    fun getCounterInterval(name : String) : Interval {
        return repo.getCounterInterval(name)
    }

    fun setCounterInterval(name : String, interval : Interval) {
        repo.setCounterInterval(name, interval)
        viewModelScope.launch(Dispatchers.IO) {
            counterMap[name]?.postValue(repo.getCounter(name))
            entriesMap[name]?.postValue(repo.getAllEntriesInCounterInterval(name))
        }
    }

    fun getCounter(name : String) : LiveData<Counter>? {
        return counterMap[name]
    }

    fun counterExists(name: String): Boolean = repo.getCounterList().contains(name)

    fun deleteCounter(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.removeAllEntries(name)
            counterMap.remove(name)
            entriesMap.remove(name)
        }
    }

    fun getAllEntriesInCounterInterval(name : String) : LiveData<CounterEntries> {
        var liveData = entriesMap[name]
        if (liveData == null) {
            liveData = MutableLiveData()
            viewModelScope.launch(Dispatchers.IO) {
                liveData.postValue(repo.getAllEntriesInCounterInterval(name))
            }
        }
        entriesMap[name] = liveData
        return liveData
    }
}
