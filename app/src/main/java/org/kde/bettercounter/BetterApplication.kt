package org.kde.bettercounter

import android.app.Application
import android.util.Log
import org.kde.bettercounter.ui.WidgetProvider


class BetterApplication : Application() {

    lateinit var viewModel: ViewModel

    override fun onCreate() {
        super.onCreate()
        Log.e("BetterApplication", "onCreate")
        viewModel = ViewModel(this)
        WidgetProvider.startObservingCounters(this)
    }
}
