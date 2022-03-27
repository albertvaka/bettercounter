package org.kde.bettercounter.persistence

import android.content.Context
import android.util.Log
import org.kde.bettercounter.R
import org.kde.bettercounter.extensions.*
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.ceil

enum class Interval(val humanReadableResource : Int) {
    DAY(R.string.interval_day),
    WEEK(R.string.interval_week),
    MONTH(R.string.interval_month),
    YEAR(R.string.interval_year),
    LIFETIME(R.string.interval_lifetime);

    companion object {
        fun humanReadableValues(context : Context): List<String> {
            return values().map { context.getString(it.humanReadableResource) }
        }
    }

    // FIXME: Add unit test for edge cases, otherwise changing this function is super scary
    // FIXME: It's a bit weird but this returns 2 and 8 in the following cases:
    //{
    //   val a = Calendar.getInstance().apply { truncate(Interval.DAY) }
    //   val b = Calendar.getInstance().apply {  truncate(Interval.DAY); addInterval(Interval.DAY, 1) }
    //   assert(Interval.DAY.between(a.time, b.time) == 1)
    //}
    //{
    //   val a = Calendar.getInstance().apply { truncate(Interval.WEEK) }
    //   val b = Calendar.getInstance().apply {  truncate(Interval.WEEK); addInterval(Interval.WEEK, 1) }
    //   assert(Interval.WEEK.between(a.time, b.time) == 7)
    //}
    fun between(from : Date, to : Date) : Int {
        val count = when (this) {
            DAY -> {
                val dayTruncatedFrom = from.toCalendar().apply { truncate(Calendar.DAY_OF_MONTH) }.time.toZonedDateTime()
                val dayTruncatedTo = to.toCalendar().apply { truncate(Calendar.DAY_OF_MONTH) }.time.toZonedDateTime()
                // We have to do this because DAYS.between doesn't round up
                ceil(ChronoUnit.HOURS.between(dayTruncatedFrom, dayTruncatedTo)/24.0f).toInt()
            }
            WEEK -> {
                val weekTruncatedFrom = from.toCalendar().apply {truncate(Calendar.WEEK_OF_YEAR) }.time.toZonedDateTime()
                val weekTruncatedTo = to.toCalendar().apply { truncate(Calendar.WEEK_OF_YEAR) }.time.toZonedDateTime()
                ceil(ChronoUnit.DAYS.between(weekTruncatedFrom, weekTruncatedTo)/7.0f).toInt()
            }
            MONTH -> (to.month - from.month) + (to.year - from.year)*12
            YEAR -> to.year - from.year
            LIFETIME -> 0
        }
        Log.e("BETWEEN", "${count+1} $this between ${from.toCalendar().toSimpleDateString()} and ${to.toCalendar().toSimpleDateString()}")
        return count + 1
    }

}

val DEFAULT_INTERVAL = Interval.LIFETIME
