package org.kde.bettercounter.ui.widget

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.kde.bettercounter.boilerplate.AppDatabase
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Repository
import java.util.Calendar
import java.util.Date

class WidgetViewModel(val application: Application) {

    private val repo: Repository

    init {
        val db = AppDatabase.getInstance(application)
        val prefs = application.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        repo = Repository(application, db.entryDao(), prefs)
    }

    fun incrementCounter(name: String, date: Date = Calendar.getInstance().time) {
        CoroutineScope(Dispatchers.IO).launch {
            repo.addEntry(name, date)
            WidgetProvider.refreshWidget(application, name)
        }
    }

    fun getCounterSummary(name: String): Flow<CounterSummary> = flow {
        emit(repo.getCounterSummary(name))
    }.flowOn(Dispatchers.IO)

    fun counterExists(name: String): Boolean = repo.getCounterList().contains(name)

    fun getCounterList() = repo.getCounterList()

}