package org.kde.bettercounter.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.lifecycle.Observer
import org.kde.bettercounter.BetterApplication
import org.kde.bettercounter.BuildConfig
import org.kde.bettercounter.R
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.persistence.CounterSummary
import java.text.SimpleDateFormat
import java.util.Date


class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val viewModel = (context.applicationContext as BetterApplication).viewModel
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, viewModel, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            deleteWidgetCounterNamePref(context, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d("WidgetProvider", "onReceive " + intent.action)
        if (intent.action == ACTION_CLICKED) {
            val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                Log.e("BetterCounterWidget","No widget id extra set")
                return
            }
            if (!existsWidgetCounterNamePref(context, appWidgetId)) {
                Log.e("BetterCounterWidget","Counter doesn't exist")
                return
            }
            val counterName = loadWidgetCounterNamePref(context, appWidgetId)
            val viewModel = (context.applicationContext as BetterApplication).viewModel
            viewModel.incrementCounter(counterName)
        }
    }

}

fun getAllWidgetIds(context : Context) : IntArray {
    return AppWidgetManager.getInstance(context).getAppWidgetIds(
        ComponentName(context, WidgetProvider::class.java)
    )
}

fun removeWidgets(context : Context, counterName : String) {
    val ids = getAllWidgetIds(context)
    val host = AppWidgetHost(context, 0)
    for (appWidgetId in ids) {
        if (counterName == loadWidgetCounterNamePref(context, appWidgetId)) {
            Log.d("BetterCounterWidget", "Deleting widget")
            // In Android 5 deleteAppWidgetId doesn't remove the widget but in Android 13 it does.
            host.deleteAppWidgetId(appWidgetId)
            deleteWidgetCounterNamePref(context, appWidgetId)
        }
    }
}

fun forceRefreshWidgets(context : Context) {
    val intent = Intent(context, WidgetProvider::class.java)
    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, getAllWidgetIds(context))
    context.sendBroadcast(intent)
}

private const val ACTION_CLICKED = "org.kde.bettercounter.WidgetProvider.CLICKED"
private const val EXTRA_WIDGET_ID = "EXTRA_WIDGET_ID"

internal fun updateAppWidget(
    context: Context,
    viewModel: ViewModel,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    if (!existsWidgetCounterNamePref(context, appWidgetId)) {
        // This gets called right after placing the widget even if it hasn't been configured yet.
        // In that case we can't do anything. This is useful for reconfigurable widgets, which don't
        // require an initial configuration dialog. Our widget isn't reconfigurable though (because
        // I didn't find a way to stop observing the previous livedata).
        Log.e("BetterCounterWidget","Ignoring updateAppWidget for an unconfigured widget")
        return
    }

    val counterName = loadWidgetCounterNamePref(context, appWidgetId)

    val views = RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget)

    val intent = Intent(context, WidgetProvider::class.java)
    intent.action = ACTION_CLICKED
    intent.putExtra(EXTRA_WIDGET_ID, appWidgetId)
    views.setOnClickPendingIntent(R.id.widgetBackground, PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_IMMUTABLE))

    if (!viewModel.counterExists(counterName)) {
        Log.e("BetterCounterWidget","The counter for this widget doesn't exist")
        //val host = AppWidgetHost(context, 0)
        //host.deleteAppWidgetId(appWidgetId)
        views.setTextViewText(R.id.widgetCounter, "error")
        views.setTextViewText(R.id.widgetTime, "not found")
        appWidgetManager.updateAppWidget(appWidgetId, views)
        return
    }

    var prevCounterName = counterName
    // observeForever means it's not attached to any lifecycle so we need to call removeObserver manually
    viewModel.getCounterSummary(counterName).observeForever(object : Observer<CounterSummary> {
        override fun onChanged(value: CounterSummary) {
            if (!existsWidgetCounterNamePref(context, appWidgetId)) {
                // Prevent leaking the observer once the widget has been deleted by deleting it here
                viewModel.getCounterSummary(value.name).removeObserver(this)
                return
            }
            if (prevCounterName != value.name) {
                // Counter is being renamed, replace the name stored in the sharedpref
                saveWidgetCounterNamePref(context, appWidgetId, value.name)
                prevCounterName = value.name
            }

            views.setInt(R.id.widgetBackground, "setBackgroundColor", value.color)
            views.setTextViewText(R.id.widgetName, value.name)
            views.setTextViewText(R.id.widgetCounter, value.lastIntervalCount.toString())
            val date = value.mostRecent
            if (date != null) {
                val now = Date()
                val diffInMillis = now.time - date.time
                val isRecent = diffInMillis < (12 * 60 * 60 * 1000L) // 12 hours
                val dateFormat = if (isRecent) {
                    SimpleDateFormat.getTimeInstance()
                } else {
                    SimpleDateFormat.getDateInstance()
                }
                val formattedDate = dateFormat.format(date)
                views.setTextViewText(R.id.widgetTime, formattedDate)
            } else {
                views.setTextViewText(R.id.widgetTime, context.getString(R.string.never))
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    })
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

