package org.kde.bettercounter.ui

import android.content.Context
import android.view.Gravity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import org.kde.bettercounter.R
import org.kde.bettercounter.databinding.FragmentChartBinding
import org.kde.bettercounter.extensions.count
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Entry
import org.kde.bettercounter.persistence.Interval
import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChartHolder(
    private val context: Context,
    private val binding: FragmentChartBinding,
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.chart.setup()
    }

    fun onBind(counter: CounterSummary, entries: List<Entry>, interval: Interval, rangeStart: Calendar, rangeEnd: Calendar, onIntervalChange: (Interval) -> Unit) {
        // Chart name
        val dateFormat = when (interval) {
            Interval.DAY, Interval.WEEK -> SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT)
            Interval.MONTH -> SimpleDateFormat("LLL yyyy", Locale.getDefault())
            Interval.YEAR -> SimpleDateFormat("yyyy", Locale.getDefault())
            Interval.LIFETIME -> throw IllegalStateException("Interval not valid as a chart display interval")
        }
        val dateString = dateFormat.format(rangeStart.time)
        binding.chartName.text = context.resources.getQuantityString(R.plurals.chart_title, entries.size, dateString, entries.size)
        binding.chartName.setOnClickListener { view ->
            val popupMenu = PopupMenu(context, view, Gravity.END)
            popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                menuItem.isChecked = true
                val newInterval = when (menuItem.itemId) {
                    R.id.day -> Interval.DAY
                    R.id.week -> Interval.WEEK
                    R.id.month -> Interval.MONTH
                    else -> Interval.YEAR
                }
                onIntervalChange(newInterval)
                return@setOnMenuItemClickListener true
            }
            val selectedItem = when (interval) {
                Interval.DAY -> R.id.day
                Interval.WEEK -> R.id.week
                Interval.MONTH -> R.id.month
                Interval.YEAR -> R.id.year
                else -> throw IllegalStateException("Interval not valid as a chart display interval")
            }
            popupMenu.menu.findItem(selectedItem).isChecked = true
            popupMenu.show()
        }

        // Only show a goal line if the displayed interval is larger than the counter's
        val goalLine = computeGoalLine(counter, interval)

        // Chart
        binding.chart.setDataBucketized(entries, rangeStart, interval, counter.color.toColorForChart(context), goalLine)

        // Stats
        val periodAverage = getPeriodAverageString(counter, entries, rangeStart, rangeEnd)
        val lifetimeAverage = getLifetimeAverageString(counter)
        binding.chartAverage.text = context.getString(R.string.stats_averages, periodAverage, lifetimeAverage)
        if (binding.chartAverage.lineCount > 1) {
            binding.chartAverage.text = context.getString(R.string.stats_averages_multiline, periodAverage, lifetimeAverage)
        }
    }

    private fun computeGoalLine(counter: CounterSummary, displayInterval: Interval): Float {
        if (counter.goal <= 0) return -1.0f
        val baseGoal = counter.goal.toFloat()
        return when (counter.interval to displayInterval) {
            Interval.DAY to Interval.WEEK -> baseGoal
            Interval.DAY to Interval.MONTH -> baseGoal
            Interval.DAY to Interval.YEAR -> baseGoal * 30
            Interval.WEEK to Interval.MONTH -> baseGoal / 7
            Interval.WEEK to Interval.YEAR -> (baseGoal / 7) * 30
            Interval.MONTH to Interval.YEAR -> baseGoal
            else -> -1.0f
        }
    }

    private fun getLifetimeAverageString(counter: CounterSummary): String {
        if (counter.totalCount == 0) {
            return context.getString(R.string.stats_average_n_a)
        }

        val beginRange = counter.leastRecent!!
        val endRange = counter.latestBetweenNowAndMostRecentEntry()

        return when (counter.interval) {
            Interval.DAY -> getAverageStringPerHour(counter.totalCount, beginRange, endRange)
            else -> getAverageStringPerDay(counter.totalCount, beginRange, endRange)
        }
    }

    private fun getPeriodAverageString(counter: CounterSummary, intervalEntries: List<Entry>, rangeStart: Calendar, rangeEnd: Calendar): String {
        if (intervalEntries.isEmpty()) {
            return context.getString(R.string.stats_average_n_a)
        }

        // Hack so to use the end of this interval and not at the beginning of the next,
        // so weeks have 7 days and not 8 because of the rounding up we do later.
        rangeEnd.add(Calendar.MINUTE, -1)

        val firstEntryDate = counter.leastRecent!!
        val hasEntriesInPreviousPeriods = (firstEntryDate < rangeStart.time)
        val startDate = if (hasEntriesInPreviousPeriods) {
            // Use the period start as the start date
            rangeStart.time
        } else {
            // Use the firstEntry as the start date
            firstEntryDate
        }

        val lastEntryDate = counter.mostRecent!!
        val now = Calendar.getInstance().time
        val hasEntriesInFuturePeriods = (lastEntryDate > rangeEnd.time)
        val hasEntriesInTheFuture = (lastEntryDate > now)
        val endDate = when {
            hasEntriesInFuturePeriods -> rangeEnd.time // Use the period end as the end date
            hasEntriesInTheFuture -> lastEntryDate // Use lastEntry as the end date
            else -> now // Use now as the end date
        }

        return when (counter.interval) {
            Interval.DAY -> getAverageStringPerHour(intervalEntries.size, startDate, endDate)
            else -> getAverageStringPerDay(intervalEntries.size, startDate, endDate)
        }
    }

    private fun getAverageStringPerDay(count: Int, startDate: Date, endDate: Date): String {
        var days = ChronoUnit.DAYS.count(startDate, endDate)
        val avgPerDay = count.toFloat() / days
        return if (avgPerDay > 1) {
            context.getString(R.string.stats_average_per_day, avgPerDay)
        } else {
            context.getString(R.string.stats_average_every_days, 1 / avgPerDay)
        }
    }

    private fun getAverageStringPerHour(count: Int, startDate: Date, endDate: Date): String {
        var hours = ChronoUnit.HOURS.count(startDate, endDate)
        val avgPerHour = count.toFloat() / hours
        return if (avgPerHour > 1) {
            context.getString(R.string.stats_average_per_hour, avgPerHour)
        } else {
            context.getString(R.string.stats_average_every_hours, 1 / avgPerHour)
        }
    }
}
