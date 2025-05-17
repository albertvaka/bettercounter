package org.kde.bettercounter.ui

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun onStopped() {
        Log.d("WidgetUpdateWorker", "Stopped")
    }

    override fun doWork(): Result {
        Log.d("WidgetUpdateWorker", "Worker started")
        WidgetProvider.forceRefreshWidgets(context)
        return Result.success()
    }
} 
