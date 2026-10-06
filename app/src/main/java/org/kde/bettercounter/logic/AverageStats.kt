package org.kde.bettercounter.logic

import android.content.Context
import org.kde.bettercounter.R
import org.kde.bettercounter.extensions.count
import org.kde.bettercounter.extensions.lastInstant
import org.kde.bettercounter.extensions.max
import org.kde.bettercounter.extensions.min
import org.kde.bettercounter.persistence.AverageMode
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Interval
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AverageStats {

    fun getLifetimeAverageString(context: Context, counter: CounterSummary, averageMode: AverageMode): String {
        val average = getLifetimeAverage(counter, averageMode)
            ?: return context.getString(R.string.stats_average_n_a)

        return when (counter.interval) {
            Interval.DAY -> formatAveragePerHour(context, average)
            else -> formatAveragePerDay(context, average)
        }
    }

    // Returns entries per hour for DAY counters, entries per day otherwise, or null if not available
    internal fun getLifetimeAverage(counter: CounterSummary, averageMode: AverageMode, now: Date = Date()): Float? {
        if (counter.totalCount <= 1) {
            return null
        }

        val (startDate, endDate) = getLifetimeRange(counter, averageMode, now)
        val unit = when (counter.interval) {
            Interval.DAY -> ChronoUnit.HOURS
            else -> ChronoUnit.DAYS
        }

        return when (averageMode) {
            AverageMode.FIRST_TO_NOW -> counter.totalCount.toFloat() / unit.count(startDate, endDate)
            AverageMode.FIRST_TO_LAST -> getAverageBetweenFirstAndLastEntry(counter.totalCount, unit, startDate, endDate)
        }
    }

    fun getPeriodAverageString(
        context: Context,
        counter: CounterSummary,
        intervalEntries: Int,
        rangeStart: Calendar,
        rangeEnd: Calendar,
        averageMode: AverageMode
    ): String {
        val average = getPeriodAverage(counter, intervalEntries, rangeStart, rangeEnd, averageMode)
            ?: return context.getString(R.string.stats_average_n_a)

        return when (counter.interval) {
            Interval.DAY, Interval.HOUR -> formatAveragePerHour(context, average)
            else -> formatAveragePerDay(context, average)
        }
    }

    // Returns entries per hour for DAY and HOUR counters, entries per day otherwise, or null if not available
    internal fun getPeriodAverage(
        counter: CounterSummary,
        intervalEntries: Int,
        rangeStart: Calendar,
        rangeEnd: Calendar,
        averageMode: AverageMode,
        now: Date = Date(),
    ): Float? {
        if (intervalEntries == 0) {
            return null
        }

        val (startDate, endDate) = getIntervalRange(counter, rangeStart, rangeEnd, averageMode, now)
        val unit = when (counter.interval) {
            Interval.DAY, Interval.HOUR -> ChronoUnit.HOURS
            else -> ChronoUnit.DAYS
        }

        return when (averageMode) {
            AverageMode.FIRST_TO_NOW -> intervalEntries.toFloat() / unit.count(startDate, endDate)
            AverageMode.FIRST_TO_LAST -> {
                // If there are entries outside the range, we measure from/to the range limit instead of
                // from/to an entry, so all the entries in the range count.
                val isFromRangeLimit = counter.leastRecent!! < rangeStart.time || counter.mostRecent!! > rangeEnd.lastInstant()
                if (isFromRangeLimit) {
                    intervalEntries.toFloat() / unit.count(startDate, endDate)
                } else {
                    getAverageBetweenFirstAndLastEntry(intervalEntries, unit, startDate, endDate)
                }
            }
        }
    }

    // The time we measure starts at the first entry, so that entry doesn't count. That time is also
    // one unit less than what ChronoUnit.count() returns, since count() includes both ends.
    private fun getAverageBetweenFirstAndLastEntry(numEntries: Int, unit: ChronoUnit, firstEntry: Date, lastEntry: Date): Float? {
        if (numEntries <= 1) {
            return null
        }
        val elapsedUnits = maxOf(1, unit.count(firstEntry, lastEntry) - 1)
        return (numEntries - 1).toFloat() / elapsedUnits
    }

    fun getGoalStatsString(
        context: Context,
        counter: CounterSummary,
        interval: Interval,
        periodGoalReached: Int,
        lifetimeGoalReached: Int,
        rangeStart: Calendar,
        rangeEnd: Calendar,
        averageMode: AverageMode,
    ): String {
        val intervalChronoUnit = counter.interval.toChronoUnit()

        val lifetimeGoalReachedStr = if (lifetimeGoalReached < 0) {
            context.getString(R.string.stats_average_n_a)
        } else {
            val (startDate, endDate) = getLifetimeRange(counter, averageMode)
            val intervalUnits = intervalChronoUnit.count(startDate, endDate)
            String.format(Locale.getDefault(), "%.1f%%", 100*lifetimeGoalReached/intervalUnits.toFloat())
        }

        if (interval > counter.interval) {
            val goalReachedStr = if (periodGoalReached < 0) {
                context.getString(R.string.stats_average_n_a)
            } else {
                val (startDate, endDate) = getIntervalRange(counter, rangeStart, rangeEnd, averageMode)
                val intervalUnits = intervalChronoUnit.count(startDate, endDate)
                String.format(Locale.getDefault(), "%.1f%%", 100*periodGoalReached/intervalUnits.toFloat())
            }
            return context.getString(R.string.goal_stats_averages, goalReachedStr, lifetimeGoalReachedStr)
        } else {
            return context.getString(R.string.goal_stats_lifetime_averages, lifetimeGoalReachedStr)
        }
    }

    private fun getLifetimeRange(counter: CounterSummary, averageMode: AverageMode, now: Date = Date()): Pair<Date, Date> {
        val startDate = counter.leastRecent!!
        val endDate = when (averageMode) {
            AverageMode.FIRST_TO_NOW ->  counter.latestBetweenNowAndMostRecentEntry(now)
            AverageMode.FIRST_TO_LAST -> counter.mostRecent ?: now
        }
        return Pair(startDate, endDate)
    }

    private fun getIntervalRange(
        counter: CounterSummary,
        rangeStart: Calendar,
        rangeEnd: Calendar,
        averageMode: AverageMode,
        now: Date = Date(),
    ): Pair<Date, Date> {
        val firstEntryDate = when (averageMode) {
            AverageMode.FIRST_TO_NOW -> min(counter.leastRecent!!, now)
            AverageMode.FIRST_TO_LAST -> counter.leastRecent!!
        }
        val lastEntryDate = when (averageMode) {
            AverageMode.FIRST_TO_NOW -> max(counter.mostRecent!!, now)
            AverageMode.FIRST_TO_LAST -> counter.mostRecent!!
        }

        val startDate = max(rangeStart.time, firstEntryDate)
        val endDate = min(rangeEnd.lastInstant(), lastEntryDate)
        return Pair(startDate, endDate)
    }

    private fun formatAveragePerDay(context: Context, avgPerDay: Float): String {
        return if (avgPerDay > 1) {
            context.getString(R.string.stats_average_per_day, avgPerDay)
        } else {
            context.getString(R.string.stats_average_every_days, 1 / avgPerDay)
        }
    }

    private fun formatAveragePerHour(context: Context, avgPerHour: Float): String {
        return if (avgPerHour > 1) {
            context.getString(R.string.stats_average_per_hour, avgPerHour)
        } else {
            context.getString(R.string.stats_average_every_hours, 1 / avgPerHour)
        }
    }

}
