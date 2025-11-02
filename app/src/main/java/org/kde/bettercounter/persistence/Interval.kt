package org.kde.bettercounter.persistence

import android.content.Context
import org.kde.bettercounter.R
import java.time.temporal.ChronoUnit
import java.util.Calendar

enum class Interval(val humanReadableResource: Int) {
    HOUR(R.string.interval_hour),
    DAY(R.string.interval_day),
    WEEK(R.string.interval_week),
    MONTH(R.string.interval_month),
    YEAR(R.string.interval_year),
    LIFETIME(R.string.interval_lifetime),
    ;

    companion object {
        val DEFAULT = LIFETIME

        fun humanReadableValues(context: Context): List<String> {
            return entries.map { context.getString(it.humanReadableResource) }
        }
    }

    fun getBucketSize(): Int = when (this) {
        HOUR -> Calendar.MINUTE
        DAY -> Calendar.HOUR_OF_DAY
        WEEK -> Calendar.DAY_OF_WEEK
        MONTH -> Calendar.DAY_OF_MONTH
        YEAR -> Calendar.MONTH
        LIFETIME -> Calendar.MONTH // Not really, but :shrug:
    }

    fun toChronoUnit(): ChronoUnit =
        when (this) {
            HOUR -> ChronoUnit.HOURS
            DAY -> ChronoUnit.DAYS
            WEEK -> ChronoUnit.WEEKS
            MONTH -> ChronoUnit.MONTHS
            YEAR -> ChronoUnit.YEARS
            LIFETIME -> throw UnsupportedOperationException("$this can't be converted to ChronoUnit")
        }

    fun toHumanReadableResourceId(): Int = humanReadableResource

    fun toChartDisplayableInterval(): Interval {
        // When displaying in a chart, LIFETIME counters will still display year by year
        return if (this == LIFETIME) YEAR else this
    }
}
