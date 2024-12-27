package org.kde.bettercounter.extensions

import android.util.Log

fun <T> T.andLog(tag: String = "LOGGERINO"): T {
    Log.e(tag, this.toString())
    return this
}
