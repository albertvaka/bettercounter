package org.kde.bettercounter.ui.widget

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Exporter
import org.kde.bettercounter.persistence.Repository
import org.kde.bettercounter.ui.main.MainActivity
import java.util.Calendar
import java.util.Date

class WidgetViewModel(val application: Application) {

    private val repo: Repository = Repository.create(application)
    private val exporter: Exporter = Exporter(application, repo)

    fun incrementCounter(name: String, date: Date = Calendar.getInstance().time) {
        CoroutineScope(Dispatchers.IO).launch {
            repo.addEntry(name, date)
            WidgetProvider.refreshWidget(application, name)
            exporter.autoExportIfEnabled()
            LocalBroadcastManager.getInstance(application)
                .sendBroadcast(Intent(MainActivity.ACTION_REFRESH_COUNTER).apply {
                    putExtra(MainActivity.EXTRA_COUNTER_NAME, name)
                })
        }
    }

    fun getCounterSummary(name: String): Flow<CounterSummary> = flow {
        emit(repo.getCounterSummary(name))
    }.flowOn(Dispatchers.IO)

    fun counterExists(name: String): Boolean = repo.getCounterList().contains(name)

    fun getCounterList() = repo.getCounterList()

    companion object {
        private const val PREFS_NAME = "org.kde.bettercounter.ui.widget.WidgetProvider"
        private const val PREF_PREFIX_KEY = "appwidget_"

        fun saveWidgetCounterNamePref(
            context: Context,
            appWidgetId: Int,
            counterName: String
        ) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putString(PREF_PREFIX_KEY + appWidgetId, counterName)
            }
        }

        fun loadWidgetCounterNamePref(context: Context, appWidgetId: Int): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(PREF_PREFIX_KEY + appWidgetId, null)
                ?: throw NoSuchElementException("Counter preference not found for widget id: $appWidgetId")
        }

        fun deleteWidgetCounterNamePref(context: Context, appWidgetId: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                remove(PREF_PREFIX_KEY + appWidgetId)
            }
        }

        fun existsWidgetCounterNamePref(context: Context, appWidgetId: Int): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.contains(PREF_PREFIX_KEY + appWidgetId)
        }
    }
}