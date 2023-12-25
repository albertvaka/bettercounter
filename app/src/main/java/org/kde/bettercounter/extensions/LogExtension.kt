package org.kde.bettercounter.extensions

import android.util.Log

fun <T> T.andLog(): T {
    return this.andLog("LOGGERINO")
}

fun <T> T.andLog(tag: String): T {
    Log.e(tag, this.toString())
    return this
}
