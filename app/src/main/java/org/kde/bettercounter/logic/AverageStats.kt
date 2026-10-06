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
        if (counter.totalCount <= 1) {
            return context.getString(R.string.stats_average_n_a)
        }

        val (startDate, endDate) = getLifetimeRange(counter, averageMode)
        val numEntries = when (averageMode) {
            AverageMode.FIRST_TO_NOW -> counter.totalCount
            AverageMode.FIRST_TO_LAST -> counter.totalCount - 1
        }

        return when (counter.interval) {
            Interval.DAY -> getAverageStringPerHour(context, numEntries, startDate, endDate)
            else -> getAverageStringPerDay(context,numEntries, startDate, endDate)
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
        if (intervalEntries == 0) {
            return context.getString(R.string.stats_average_n_a)
        }

        val (startDate, endDate) = getIntervalRange(counter, rangeStart, rangeEnd, averageMode)
        val numEntries = when (averageMode) {
            AverageMode.FIRST_TO_NOW -> intervalEntries
            AverageMode.FIRST_TO_LAST -> {
                val isFromRangeLimit = endDate == rangeEnd.lastInstant() || startDate == rangeStart.time
                if (isFromRangeLimit) {
                    intervalEntries
                } else {
                    intervalEntries - 1
                }
            }
        }

        if (numEntries == 0) {
            return context.getString(R.string.stats_average_n_a)
        }

        return when (counter.interval) {
            Interval.DAY, Interval.HOUR -> getAverageStringPerHour(context, numEntries, startDate, endDate)
            else -> getAverageStringPerDay(context, numEntries, startDate, endDate)
        }
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

    private fun getLifetimeRange(counter: CounterSummary, averageMode: AverageMode): Pair<Date, Date> {
        val startDate = counter.leastRecent!!
        val endDate = when (averageMode) {
            AverageMode.FIRST_TO_NOW ->  counter.latestBetweenNowAndMostRecentEntry()
            AverageMode.FIRST_TO_LAST -> counter.mostRecent ?: Date()
        }
        return Pair(startDate, endDate)
    }

    private fun getIntervalRange(
        counter: CounterSummary,
        rangeStart: Calendar,
        rangeEnd: Calendar,
        averageMode: AverageMode
    ): Pair<Date, Date> {
        val firstEntryDate = when (averageMode) {
            AverageMode.FIRST_TO_NOW -> min(counter.leastRecent!!, Date())
            AverageMode.FIRST_TO_LAST -> counter.leastRecent!!
        }
        val lastEntryDate = when (averageMode) {
            AverageMode.FIRST_TO_NOW -> max(counter.mostRecent!!, Date())
            AverageMode.FIRST_TO_LAST -> counter.mostRecent!!
        }

        val startDate = max(rangeStart.time, firstEntryDate)
        val endDate = min(rangeEnd.lastInstant(), lastEntryDate)
        return Pair(startDate, endDate)
    }

    private fun getAverageStringPerDay(context: Context, count: Int, startDate: Date, endDate: Date): String {
        val days = ChronoUnit.DAYS.count(startDate, endDate)
        val avgPerDay = count.toFloat() / days
        return if (avgPerDay > 1) {
            context.getString(R.string.stats_average_per_day, avgPerDay)
        } else {
            context.getString(R.string.stats_average_every_days, 1 / avgPerDay)
        }
    }

    private fun getAverageStringPerHour(context: Context, count: Int, startDate: Date, endDate: Date): String {
        val hours = ChronoUnit.HOURS.count(startDate, endDate)
        val avgPerHour = count.toFloat() / hours
        return if (avgPerHour > 1) {
            context.getString(R.string.stats_average_per_hour, avgPerHour)
        } else {
            context.getString(R.string.stats_average_every_hours, 1 / avgPerHour)
        }
    }

}
