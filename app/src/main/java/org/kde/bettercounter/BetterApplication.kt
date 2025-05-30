package org.kde.bettercounter

import android.app.Application
import android.util.Log


class BetterApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.e("BetterApplication", "onCreate")
    }
}
