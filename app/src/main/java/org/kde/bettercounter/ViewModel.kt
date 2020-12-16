package org.kde.bettercounter

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.*
import org.kde.bettercounter.boilerplate.AppDatabase
import org.kde.bettercounter.persistence.Counter
import org.kde.bettercounter.persistence.Entry
import org.kde.bettercounter.persistence.Interval
import org.kde.bettercounter.persistence.Repository
import kotlin.collections.HashMap


class ViewModel(application: Application) : AndroidViewModel(application) {

    private val repo : Repository
    private val addCounterObservers = HashMap<LifecycleOwner, Observer<String>>()
    private val counterMap = HashMap<String, MutableLiveData<Counter>>()

    init {
        val db  = AppDatabase.getInstance(application)
        val prefs =  application.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        repo = Repository(db.entryDao(), prefs)
        val initialCounters = repo.getCounterList()
        viewModelScope.launch(Dispatchers.IO) {
            for (name in initialCounters) {
                counterMap[name] = MutableLiveData(repo.getCounter(name)) // cache it
                for ((_, observer) in addCounterObservers) {
                    observer.onChanged(name)
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
                    observer.onChanged(name)
                }
            }
        }
    }

    fun observeNewCounter(owner : LifecycleOwner, observer: Observer<String>) {
        addCounterObservers[owner] = observer
        for (name in counterMap.keys) { //notify the ones we already have
            observer.onChanged(name)
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
        }
    }

    fun incrementCounter(name : String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.addEntry(name)
            counterMap[name]?.postValue(repo.getCounter(name))
        }
    }

    fun decrementCounter(name : String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.removeEntry(name)
            counterMap[name]?.postValue(repo.getCounter(name))
        }
    }

    fun getCounterInterval(name : String) : Interval {
        return repo.getCounterInterval(name)
    }

    fun setCounterInterval(name : String, interval : Interval) {
        repo.setCounterInterval(name, interval)
        viewModelScope.launch(Dispatchers.IO) {
            counterMap[name]?.postValue(repo.getCounter(name))
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
        }
    }

    fun getAllEntriesInCounterInterval(name : String) : List<Entry> {
        return repo.getAllEntriesInCounterInterval(name)
    }
}
