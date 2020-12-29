package org.kde.bettercounter.boilerplate

import android.util.Log

fun <T> T.andLog(): T {
    return this.andLog("LOGGERINO")
}

fun <T> T.andLog(label : String): T {
    Log.e(label, this.toString())
    return this
}
