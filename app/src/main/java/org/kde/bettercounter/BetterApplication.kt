package org.kde.bettercounter

import android.app.Application
import android.util.Log
import org.kde.bettercounter.persistence.FirstHourOfDay


class BetterApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirstHourOfDay.init(this)
        Log.d("BetterApplication", "onCreate")
    }
}
