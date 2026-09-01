package org.kde.bettercounter.extensions

import android.view.Window
import android.view.WindowManager

fun Window.setKeepScreenOn(enabled: Boolean) {
    if (enabled) {
        addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
