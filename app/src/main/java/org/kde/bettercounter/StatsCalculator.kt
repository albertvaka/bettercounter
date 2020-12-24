package org.kde.bettercounter

import android.content.Context
import org.kde.bettercounter.persistence.Entry
import java.util.*
import kotlin.math.ceil

class StatsCalculator(private val context : Context) {

    private fun getPerHour(count : Int, hours: Float) : CharSequence {
        val avg = count/hours
        return if (avg > 1) {
            context.getString(R.string.stats_average_per_hour, avg)
        } else {
            context.getString(R.string.stats_average_every_hours, 1/avg)
        }
    }

    private fun getPerDay(count : Int, days: Float) : CharSequence {
        val avg = count/days
        return if (avg > 1) {
            context.getString(R.string.stats_average_per_day, avg)
        } else {
            context.getString(R.string.stats_average_every_days, 1/avg)
        }
    }

    private fun getOldestEntryTime(intervalEntries: List<Entry>) : Long {
        var oldest = System.currentTimeMillis()
        for (entry in intervalEntries) {
            val time = entry.date.time
            if (time < oldest) {
                oldest = time
            }
        }
        return oldest
    }

    private fun daysSince(time: Long): Float {
        val deltaTimeMillis = System.currentTimeMillis()-time
        val millisPerDayFloat = (1000*60*60*24).toFloat()
        return deltaTimeMillis/millisPerDayFloat
    }

    fun getDaily(intervalEntries: List<Entry>): CharSequence {
        if (intervalEntries.isEmpty()) return context.getString(R.string.no_data)
        return getPerHour(intervalEntries.size, 24f)
    }

    fun getWeekly(intervalEntries: List<Entry>): CharSequence {
        if (intervalEntries.isEmpty()) return context.getString(R.string.no_data)
        return getPerDay(intervalEntries.size, 7f)
    }

    fun getMonthly(intervalEntries: List<Entry>): CharSequence {
        if (intervalEntries.isEmpty()) return context.getString(R.string.no_data)
        return getPerDay(intervalEntries.size, 31f)
    }

    fun getYearly(intervalEntries: List<Entry>): CharSequence {
        if (intervalEntries.isEmpty()) return context.getString(R.string.no_data)
        return getPerDay(intervalEntries.size, 365f)
    }

    fun getYtd(intervalEntries: List<Entry>): CharSequence {
        if (intervalEntries.isEmpty()) return context.getString(R.string.no_data)
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return getPerDay(intervalEntries.size, dayOfYear.toFloat())
    }

    fun getLifetime(intervalEntries: List<Entry>): CharSequence {
        if (intervalEntries.isEmpty()) return context.getString(R.string.no_data)
        val oldestEntryTime = getOldestEntryTime(intervalEntries)
        val deltaDays = daysSince(oldestEntryTime)
        return getPerDay(intervalEntries.size, ceil(deltaDays))
    }

}