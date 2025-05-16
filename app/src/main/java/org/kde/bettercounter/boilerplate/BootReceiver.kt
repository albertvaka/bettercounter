package org.kde.bettercounter.boilerplate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.kde.bettercounter.ui.WidgetProvider

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                WidgetProvider.scheduleHourlyUpdate(context)
                WidgetProvider.forceRefreshWidgets(context)
            }
        }
    }
}