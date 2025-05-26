package org.kde.bettercounter.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import org.kde.bettercounter.BetterApplication
import org.kde.bettercounter.BuildConfig
import org.kde.bettercounter.R
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.extensions.millisecondsUntilNextHour
import java.text.SimpleDateFormat
import java.util.Date

class WidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleHourlyUpdate(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "onUpdate")
        val viewModel = (context.applicationContext as BetterApplication).viewModel
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, viewModel, AppWidgetManager.getInstance(context), appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            deleteWidgetCounterNamePref(context, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive " + intent.action)
        when (intent.action) {
        ACTION_COUNT -> {
            val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                Log.e(TAG, "No widget id extra set")
                return
            }
            if (!existsWidgetCounterNamePref(context, appWidgetId)) {
                Log.e(TAG, "Counter doesn't exist")
                return
            }
            val counterName = loadWidgetCounterNamePref(context, appWidgetId)
            val viewModel = (context.applicationContext as BetterApplication).viewModel
            viewModel.incrementCounter(counterName)
        }
        Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
            scheduleHourlyUpdate(context)
        }
        ACTION_HOURLY_UPDATE -> {
            refreshWidgets(context)
            scheduleHourlyUpdate(context) // Schedule next update, we don't use repeating alarms for precision
        }
    }

    }

    companion object {
        private const val TAG = "WidgetProvider"
        private const val ACTION_COUNT = "org.kde.bettercounter.WidgetProvider.COUNT"
        private const val ACTION_HOURLY_UPDATE = "org.kde.bettercounter.WidgetProvider.HOURLY_UPDATE"
        private const val EXTRA_WIDGET_ID = "EXTRA_WIDGET_ID"

        fun scheduleHourlyUpdate(context: Context) {
            val widgetIds = getAllWidgetIds(context)
            if (widgetIds.isEmpty()) return
            Log.d(TAG, "Scheduling next hourly update")
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WidgetProvider::class.java)
            intent.action = ACTION_HOURLY_UPDATE
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            alarmManager.set(
                AlarmManager.RTC,
                System.currentTimeMillis() + millisecondsUntilNextHour(),
                pendingIntent
            )
        }

        private fun getAllWidgetIds(context: Context): IntArray {
            return AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, WidgetProvider::class.java)
            )
        }

        fun refreshWidgets(context: Context) {
            val widgetIds = getAllWidgetIds(context)
            if (widgetIds.isEmpty()) return
            Log.d(TAG, "refreshing widgets")
            val intent = Intent(context, WidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            context.sendBroadcast(intent)
        }

        fun renameCounter(context: Context, counterName: String, prevCounterName: String) {
            val widgetIds = getAllWidgetIds(context)
            for (widgetId in widgetIds) {
                if (prevCounterName == loadWidgetCounterNamePref(context, widgetId)) {
                    saveWidgetCounterNamePref(context, widgetId, counterName)
                }
            }
        }

        fun removeWidgets(context: Context, counterName: String) {
            val widgetIds = getAllWidgetIds(context)
            val host = AppWidgetHost(context, 0)
            for (widgetId in widgetIds) {
                if (counterName == loadWidgetCounterNamePref(context, widgetId)) {
                    Log.d(TAG, "Deleting widget")
                    // In Android 5 deleteAppWidgetId doesn't remove the widget but in Android 13 it does.
                    host.deleteAppWidgetId(widgetId)
                    deleteWidgetCounterNamePref(context, widgetId)
                }
            }
        }

        internal fun updateAppWidget(
            context: Context,
            viewModel: ViewModel,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            Log.d(TAG, "updateAppWidget called for widget $appWidgetId")

            if (!existsWidgetCounterNamePref(context, appWidgetId)) {
                // This gets called right after placing the widget even if it hasn't been configured yet.
                // In that case we can't do anything. This is useful for reconfigurable widgets, which don't
                // require an initial configuration dialog. Our widget isn't reconfigurable though (because
                // I didn't find a way to stop observing the previous livedata).
                Log.e(TAG, "Ignoring updateAppWidget for an unconfigured widget")
                return
            }

            val counterName = loadWidgetCounterNamePref(context, appWidgetId)
            Log.d(TAG, "Updating widget for counter: $counterName")

            val views = RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget)

            views.setTextViewText(R.id.widgetName, counterName)

            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widgetName, openAppPendingIntent)

            if (!viewModel.counterExists(counterName)) {
                Log.e(TAG, "The counter for this widget doesn't exist")
                //val host = AppWidgetHost(context, 0)
                //host.deleteAppWidgetId(appWidgetId)
                views.setTextViewText(R.id.widgetCounter, "error")
                views.setTextViewText(R.id.widgetTime, "not found")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            val counter = viewModel.getCounterSummary(counterName).value
            if (counter == null) {
                views.setTextViewText(R.id.widgetCounter, "...")
                return
            }

            val countIntent = Intent(context, WidgetProvider::class.java)
            countIntent.action = ACTION_COUNT
            countIntent.putExtra(EXTRA_WIDGET_ID, appWidgetId)
            // We pass appWidgetId as requestCode even if it's not used to force the creation a new PendingIntent
            // instead of reusing an existing one, which is what happens if only the "extras" field differs.
            // Docs: https://developer.android.com/reference/android/app/PendingIntent.html
            val countPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, countIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widgetBackground, countPendingIntent)

            views.setInt(R.id.widgetBackground, "setBackgroundColor", counter.color.colorInt)
            views.setTextViewText(R.id.widgetCounter, counter.getFormattedCount())
            val date = counter.mostRecent
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
    }
}
