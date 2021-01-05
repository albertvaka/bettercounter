package org.kde.bettercounter.persistence

import android.content.Context
import org.kde.bettercounter.R
import org.kde.bettercounter.extensions.truncate
import java.util.*

enum class Interval(val humanReadableResource : Int) {
    DAY(R.string.interval_day),
    WEEK(R.string.interval_week),
    MONTH(R.string.interval_month),
    YEAR(R.string.interval_year),
    YTD(R.string.interval_ytd),
    LIFETIME(R.string.interval_lifetime);

    companion object {
        fun humanReadableValues(context : Context): List<String> {
            return values().map { context.getString(it.humanReadableResource) }
        }
    }

    fun toDate() : Date {
        val cal = Calendar.getInstance()
        when (this) {
            DAY -> cal.add(Calendar.DAY_OF_YEAR, -1)
            WEEK -> cal.add(Calendar.DAY_OF_YEAR, -7)
            MONTH -> cal.add(Calendar.MONTH, -1)
            YEAR -> cal.add(Calendar.YEAR, -1)
            YTD -> cal.truncate(Calendar.YEAR)
            LIFETIME -> cal.add(Calendar.YEAR, -50)
        }
        return cal.time
    }
}

val DEFAULT_INTERVAL = Interval.LIFETIME
