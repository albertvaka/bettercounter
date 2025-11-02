package org.kde.bettercounter.ui.chart

import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import org.kde.bettercounter.R
import org.kde.bettercounter.databinding.FragmentChartBinding
import org.kde.bettercounter.extensions.copy
import org.kde.bettercounter.extensions.count
import org.kde.bettercounter.extensions.max
import org.kde.bettercounter.extensions.min
import org.kde.bettercounter.persistence.AverageMode
import org.kde.bettercounter.persistence.CounterColors
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Entry
import org.kde.bettercounter.persistence.Interval
import org.kde.bettercounter.persistence.Tutorial
import org.kde.bettercounter.ui.main.MainActivityViewModel
import org.kde.bettercounter.ui.main.showDatePicker
import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChartHolder(
    private val activity: AppCompatActivity,
    private val viewModel: MainActivityViewModel,
    private val binding: FragmentChartBinding,
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.chart.setup()
    }

    fun display(counter: CounterSummary, entries: List<Entry>, interval: Interval, rangeStart: Calendar, rangeEnd: Calendar, maxCount: Int, onIntervalChange: (Interval) -> Unit, onDateChange: (Calendar) -> Unit) {
        // Chart name
        val dateFormat = when (interval) {
            Interval.HOUR -> SimpleDateFormat.getDateTimeInstance()
            Interval.DAY, Interval.WEEK -> SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT)
            Interval.MONTH -> SimpleDateFormat("LLL yyyy", Locale.getDefault())
            Interval.YEAR -> SimpleDateFormat("yyyy", Locale.getDefault())
            Interval.LIFETIME -> throw IllegalStateException("Interval not valid as a chart display interval")
        }
        val dateString = dateFormat.format(rangeStart.time)
        binding.chartName.text = activity.resources.getQuantityString(R.plurals.chart_title, entries.size, dateString, entries.size)
        binding.chartName.setOnClickListener { view ->
            val popupMenu = PopupMenu(activity, view, Gravity.END)
            popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                menuItem.isChecked = true
                val newInterval = when (menuItem.itemId) {
                    R.id.hour -> Interval.HOUR
                    R.id.day -> Interval.DAY
                    R.id.week -> Interval.WEEK
                    R.id.month -> Interval.MONTH
                    else -> Interval.YEAR
                }
                onIntervalChange(newInterval)
                return@setOnMenuItemClickListener true
            }
            val selectedItem = when (interval) {
                Interval.HOUR -> R.id.hour
                Interval.DAY -> R.id.day
                Interval.WEEK -> R.id.week
                Interval.MONTH -> R.id.month
                Interval.YEAR -> R.id.year
                else -> throw IllegalStateException("Interval not valid as a chart display interval")
            }
            popupMenu.menu.findItem(selectedItem).isChecked = true
            popupMenu.show()
        }
        binding.chartName.setOnLongClickListener {
            showDatePicker(activity, rangeStart, onDateChange)
            true
        }

        val buckets = bucketizeData(entries, rangeStart, interval)

        // Only show a goal line if the displayed interval is larger than the counter's
        val goalLine = computeGoalLine(counter, interval)

        // Chart
        val colorInt = CounterColors.getInstance(activity).getColorIntForChart(counter.color)
        binding.chart.setDataBucketized(buckets, rangeStart, interval, colorInt, goalLine, maxCount)

        // Stats
        val periodAverage = getPeriodAverageString(counter, entries, rangeStart, rangeEnd)
        val lifetimeAverage = getLifetimeAverageString(counter)
        binding.chartAverage.text = activity.getString(R.string.stats_averages, periodAverage, lifetimeAverage)
        if (binding.chartAverage.lineCount > 1) {
            binding.chartAverage.text = activity.getString(R.string.stats_averages_multiline, periodAverage, lifetimeAverage)
        }
    }

    fun showChangeGraphIntervalTutorial(onDismissListener: SimpleTooltip.OnDismissListener? = null) {
        Tutorial.CHANGE_GRAPH_INTERVAL.show(activity, binding.chartName, onDismissListener)
    }

    private fun computeGoalLine(counter: CounterSummary, displayInterval: Interval): Int {
        if (counter.goal <= 0) return -1
        val baseGoal = counter.goal
        return when (counter.interval to displayInterval) {
            Interval.DAY to Interval.WEEK -> baseGoal
            Interval.DAY to Interval.MONTH -> baseGoal
            Interval.DAY to Interval.YEAR -> baseGoal * 30
            Interval.WEEK to Interval.MONTH -> baseGoal / 7
            Interval.WEEK to Interval.YEAR -> (baseGoal / 7) * 30
            Interval.MONTH to Interval.YEAR -> baseGoal
            else -> -1
        }
    }

    private fun getLifetimeAverageString(counter: CounterSummary): String {
        if (counter.totalCount <= 1) {
            return activity.getString(R.string.stats_average_n_a)
        }

        val averageMode = viewModel.getAverageCalculationMode()

        val startDate = counter.leastRecent!!
        val endDate = when (averageMode) {
            AverageMode.FIRST_TO_NOW ->  counter.latestBetweenNowAndMostRecentEntry()
            AverageMode.FIRST_TO_LAST -> counter.mostRecent ?: Date()
        }
        val numEntries = when (averageMode) {
            AverageMode.FIRST_TO_NOW -> counter.totalCount
            AverageMode.FIRST_TO_LAST -> counter.totalCount - 1
        }

        return when (counter.interval) {
            Interval.DAY -> getAverageStringPerHour(numEntries, startDate, endDate)
            else -> getAverageStringPerDay(numEntries, startDate, endDate)
        }
    }

    private fun getPeriodAverageString(counter: CounterSummary, intervalEntries: List<Entry>, rangeStart: Calendar, rangeEnd: Calendar): String {
        if (intervalEntries.isEmpty()) {
            return activity.getString(R.string.stats_average_n_a)
        }

        val averageMode = viewModel.getAverageCalculationMode()

        // Use the end of this interval and not at the beginning of the next,
        // so weeks have 7 days and not 8 because of the rounding up we do later.
        rangeEnd.add(Calendar.MINUTE, -1)

        val firstEntryDate = when (averageMode) {
            AverageMode.FIRST_TO_NOW -> min(counter.leastRecent!!, Date())
            AverageMode.FIRST_TO_LAST -> counter.leastRecent!!
        }
        val lastEntryDate = when (averageMode) {
            AverageMode.FIRST_TO_NOW -> max(counter.mostRecent!!, Date())
            AverageMode.FIRST_TO_LAST -> counter.mostRecent!!
        }

        val startDate = max(rangeStart.time, firstEntryDate)
        val endDate = min(rangeEnd.time, lastEntryDate)

        val numEntries = when (averageMode) {
            AverageMode.FIRST_TO_NOW -> intervalEntries.size
            AverageMode.FIRST_TO_LAST -> {
                val isFromRangeLimit = endDate == rangeEnd.time || startDate == rangeStart.time
                if (isFromRangeLimit) {
                    intervalEntries.size
                } else {
                    intervalEntries.size - 1
                }
            }
        }

        if (numEntries == 0) {
            return activity.getString(R.string.stats_average_n_a)
        }

        return when (counter.interval) {
            Interval.DAY, Interval.HOUR -> getAverageStringPerHour(numEntries, startDate, endDate)
            else -> getAverageStringPerDay(numEntries, startDate, endDate)
        }
    }

    private fun getAverageStringPerDay(count: Int, startDate: Date, endDate: Date): String {
        val days = ChronoUnit.DAYS.count(startDate, endDate)
        val avgPerDay = count.toFloat() / days
        return if (avgPerDay > 1) {
            activity.getString(R.string.stats_average_per_day, avgPerDay)
        } else {
            activity.getString(R.string.stats_average_every_days, 1 / avgPerDay)
        }
    }

    private fun getAverageStringPerHour(count: Int, startDate: Date, endDate: Date): String {
        val hours = ChronoUnit.HOURS.count(startDate, endDate)
        val avgPerHour = count.toFloat() / hours
        return if (avgPerHour > 1) {
            activity.getString(R.string.stats_average_per_hour, avgPerHour)
        } else {
            activity.getString(R.string.stats_average_every_hours, 1 / avgPerHour)
        }
    }

    private fun bucketizeData(
        intervalEntries: List<Entry>,
        rangeStart: Calendar,
        interval: Interval,
    ): List<Int> {
        if (intervalEntries.isEmpty()) {
            return emptyList()
        }

        val bucketIntervalAsCalendarField = interval.asCalendarField()
        val numBuckets = when (interval) {
            Interval.HOUR -> 60
            Interval.DAY -> 24
            Interval.WEEK -> 7
            Interval.MONTH -> rangeStart.getActualMaximum(Calendar.DAY_OF_MONTH)
            Interval.YEAR -> 12
            else -> throw RuntimeException("Invalid interval")
        }

        val cal = rangeStart.copy()

        // All given entries should be after rangeStart and before rangeStart+numBuckets
        assert(intervalEntries.first().date.time >= cal.timeInMillis) {
            "Entry on ${intervalEntries.first().date} is not after ${cal.time}}"
        }
        val endCal =
            ((cal.clone() as Calendar).apply { add(bucketIntervalAsCalendarField, numBuckets) })
        assert(intervalEntries.last().date.time < endCal.timeInMillis) {
            "Entry on ${intervalEntries.first().date} is not before ${endCal.time}}"
        }

        val buckets: MutableList<Int> = ArrayList(numBuckets)
        var entriesIndex = 0
        repeat(numBuckets) {
            cal.add(
                bucketIntervalAsCalendarField,
                1
            ) // Calendar is now at the end of the current bucket
            var bucketCount = 0
            while (entriesIndex < intervalEntries.size && intervalEntries[entriesIndex].date.time < cal.timeInMillis) {
                bucketCount++
                entriesIndex++
            }
            // Log.e("Bucket", "$bucket (ends ${cal.debugToSimpleDateString()}) -> $bucketCount")
            buckets.add(bucketCount)
        }
        return buckets
    }

}
