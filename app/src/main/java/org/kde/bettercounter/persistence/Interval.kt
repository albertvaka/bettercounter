package org.kde.bettercounter.persistence

import android.content.Context
import org.kde.bettercounter.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
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
        var date = LocalDateTime.now()
        date = when (this) {
            DAY -> date.minusDays(1)
            WEEK -> date.minusDays(7)
            MONTH -> date.minusMonths(1)
            YEAR -> date.minusYears(1)
            YTD -> date.withDayOfYear(1)
            LIFETIME -> date.minusYears(50)
        }
        return Date.from(date.atZone(ZoneId.systemDefault()).toInstant())
    }
}

val DEFAULT_INTERVAL = Interval.YTD
