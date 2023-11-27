package org.kde.bettercounter.ui

import android.content.Context
import android.view.Gravity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
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
            Interval.DAY, Interval.WEEK -> "dd/MM/yyyy"
            Interval.MONTH -> "MM/yyyy"
            Interval.YEAR -> "yyyy"
            Interval.LIFETIME -> "".also { assert(false) } // Not a valid display interval
        }
        val dateString = SimpleDateFormat(dateFormat, Locale.getDefault()).format(rangeStart.time)
        binding.chartName.text = context.resources.getQuantityString(R.plurals.chart_title, entries.size, dateString, entries.size)
        binding.chartName.setOnClickListener { view ->
            val popupMenu = PopupMenu(context, view, Gravity.END)
            popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val newInterval = when (menuItem.itemId) {
                    R.id.day -> Interval.DAY
                    R.id.week -> Interval.WEEK
                    R.id.month -> Interval.MONTH
                    else -> Interval.YEAR
                }
                onIntervalChange(newInterval)
                return@setOnMenuItemClickListener true
            }
            popupMenu.show()
        }

        // Chart
        val defaultColor = ContextCompat.getColor(context, R.color.colorPrimary)
        val color = if (counter.color == defaultColor) ContextCompat.getColor(context, R.color.colorAccent) else counter.color
        binding.chart.setDataBucketized(entries, rangeStart, interval, color)

        // Stats
        val periodAverage = getPeriodAverageString(counter, entries, rangeStart, rangeEnd)
        val lifetimeAverage = getLifetimeAverageString(counter)
        val averageString = context.getString(R.string.stats_averages, periodAverage, lifetimeAverage)
        binding.chartAverage.text = averageString
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
