package org.kde.bettercounter.boilerplate

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.AlarmManagerCompat.canScheduleExactAlarms
import org.kde.bettercounter.BetterApplication
import org.kde.bettercounter.extensions.millisecondsUntilNextHour

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                scheduleHourlyUpdate(context)
            }
            HOURLY_UPDATE_ACTION -> {
                val viewModel = (context.applicationContext as BetterApplication).viewModel
                viewModel.refreshAllObservers()
                scheduleHourlyUpdate(context) // Schedule next update, we don't use repeating alarms for precision
            }
        }
    }

    companion object {
        const val HOURLY_UPDATE_ACTION = "org.kde.bettercounter.HourlyUpdate"

        private const val TAG = "HourlyUpdateWorker"

        @SuppressLint("MissingPermission")
        fun scheduleHourlyUpdate(context: Context) {
            Log.d(TAG, "Scheduling next hourly update")
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            assert(canScheduleExactAlarms(alarmManager))
            val intent = Intent(context, BootReceiver::class.java)
            intent.action = HOURLY_UPDATE_ACTION
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            alarmManager.setExact(
                AlarmManager.RTC,
                System.currentTimeMillis() + millisecondsUntilNextHour(),
                pendingIntent
            )
        }
    }
}