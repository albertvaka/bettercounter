package org.kde.bettercounter

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kde.bettercounter.extensions.millisecondsUntilNextHour
import java.util.concurrent.TimeUnit

class HourlyUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("WidgetUpdateWorker", "Worker started")
        val viewModel = (context.applicationContext as BetterApplication).viewModel
        withContext(Dispatchers.IO) {
            viewModel.refreshAllObservers()
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "HourlyUpdateWorker"
        private const val WORK_NAME = "widget_update_work"

        fun scheduleHourlyUpdate(context: Context) {
            Log.d(TAG, "Scheduling hourly update")
            val workRequest = PeriodicWorkRequestBuilder<HourlyUpdateWorker>(1, TimeUnit.HOURS)
                .setInitialDelay(millisecondsUntilNextHour(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.Companion.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        private fun cancelHourlyUpdate(context: Context) {
            Log.d(TAG, "Cancelling hourly update")
            WorkManager.Companion.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}