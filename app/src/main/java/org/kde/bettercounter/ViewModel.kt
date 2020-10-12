package org.kde.bettercounter

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import org.kde.bettercounter.boilerplate.AppDatabase
import org.kde.bettercounter.persistence.Repository

class ViewModel(application: Application) : AndroidViewModel(application) {

    private val repo : Repository

    init {
        val db  = AppDatabase.getInstance(application)
        val prefs =  application.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        repo = Repository(db.entryDao(), prefs)
    }

    var counters : List<String>
        get() = repo.getCounterList()
        set(value) = repo.setCounterList(value)

    private val addCounterObservers = HashMap<LifecycleOwner, Observer<List<String>>>()
    fun addCounter(value:String) {
        repo.setCounterList(counters.toMutableList() + value)
        for ((_, observer) in addCounterObservers) {
            observer.onChanged(counters)
        }
    }
    fun observeAddCounter(owner : LifecycleOwner, observer: Observer<List<String>>) {
        addCounterObservers[owner] = observer
    }

    fun renameCounter(oldName : String, newName : String) {

    }

    fun increaseCounter(oldName : String, newName : String) {

    }

}
