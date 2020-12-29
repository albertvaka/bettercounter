package org.kde.bettercounter.boilerplate

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

fun <T> T.andLog(): T {
    return this.andLog("LOGGERINO")
}

fun <T> T.andLog(label : String): T {
    Log.e(label, this.toString())
    return this
}

fun Calendar.toSimpleDateString() : String {
    val dateFormat = SimpleDateFormat("dd-MM-yyyy HH-mm", Locale.US)
    return dateFormat.format(time)
}
