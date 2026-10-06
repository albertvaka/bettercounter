package org.kde.bettercounter.ui.chart

import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import org.kde.bettercounter.R
import org.kde.bettercounter.databinding.FragmentChartBinding
import org.kde.bettercounter.extensions.count
import org.kde.bettercounter.extensions.lastInstant
import org.kde.bettercounter.extensions.max
import org.kde.bettercounter.extensions.min
import org.kde.bettercounter.logic.AverageStats
import org.kde.bettercounter.persistence.AverageMode
import org.kde.bettercounter.persistence.CounterColors
import org.kde.bettercounter.persistence.CounterSummary
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

    fun display(counter: CounterSummary, buckets: List<Int>, intervalEntries: Int, displayInterval: Interval, rangeStart: Calendar, rangeEnd: Calendar, maxCount: Int, periodGoalReached: Int, lifetimeGoalReached: Int, onIntervalChange: (Interval) -> Unit, onDateChange: (Calendar) -> Unit) {
        // Chart name
        val dateFormat = when (displayInterval) {
            Interval.HOUR -> SimpleDateFormat.getDateTimeInstance()
            Interval.DAY, Interval.WEEK -> SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT)
            Interval.MONTH -> SimpleDateFormat("LLL yyyy", Locale.getDefault())
            Interval.YEAR -> SimpleDateFormat("yyyy", Locale.getDefault())
            Interval.LIFETIME -> error("Interval not valid as a chart display interval")
        }
        val dateString = dateFormat.format(rangeStart.time)
        binding.chartName.text = activity.resources.getQuantityString(R.plurals.chart_title, intervalEntries, dateString, intervalEntries)
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
            val selectedItem = when (displayInterval) {
                Interval.HOUR -> R.id.hour
                Interval.DAY -> R.id.day
                Interval.WEEK -> R.id.week
                Interval.MONTH -> R.id.month
                Interval.YEAR -> R.id.year
            }
            popupMenu.menu.findItem(selectedItem).isChecked = true
            popupMenu.show()
        }
        binding.chartName.setOnLongClickListener {
            showDatePicker(activity, rangeStart, null, onDateChange)
            true
        }

        // Chart
        binding.chart.setDataBucketized(
            buckets = buckets,
            bucketSizeCalendarUnit = displayInterval.getBucketSubdivisions(),
            rangeStart = rangeStart,
            color = CounterColors.getInstance(activity).getColorIntForChart(counter.color),
            goalLine = computeGoalLine(counter, displayInterval),
            maxCount = maxCount,
        )

        // Stats
        val averageMode = viewModel.getAverageCalculationMode()
        val periodAverage = AverageStats.getPeriodAverageString(activity, counter, intervalEntries, rangeStart, rangeEnd, averageMode)
        val lifetimeAverage = AverageStats.getLifetimeAverageString(activity, counter, averageMode)
        binding.chartAverage.text = activity.getString(R.string.stats_averages, periodAverage, lifetimeAverage)
        if (binding.chartAverage.lineCount > 1) {
            binding.chartAverage.text = activity.getString(R.string.stats_averages_multiline, periodAverage, lifetimeAverage)
        }

        // Goal stats
        if (counter.goal > 0 && counter.interval != Interval.LIFETIME) {
            binding.chartGoalAverage.text = AverageStats.getGoalStatsString(activity, counter, displayInterval, periodGoalReached, lifetimeGoalReached, rangeStart, rangeEnd, averageMode)
            binding.chartGoalAverage.visibility = View.VISIBLE
        } else {
            binding.chartGoalAverage.visibility = View.GONE
        }
    }

    fun showChangeGraphIntervalTutorial(onDismissListener: SimpleTooltip.OnDismissListener? = null) {
        Tutorial.CHANGE_GRAPH_INTERVAL.show(activity, binding.chartName, onDismissListener)
    }

    private fun computeGoalLine(counter: CounterSummary, displayInterval: Interval): Int {
        if (counter.goal <= 0) return -1
        val baseGoal = counter.goal
        // Only show a goal line if the displayed interval is larger than the counter's
        return when (counter.interval to displayInterval) {
            Interval.HOUR to Interval.DAY -> baseGoal
            Interval.HOUR to Interval.WEEK -> baseGoal * 24
            Interval.HOUR to Interval.MONTH -> baseGoal * 24
            Interval.HOUR to Interval.YEAR -> baseGoal * 24 * 30
            Interval.DAY to Interval.WEEK -> baseGoal
            Interval.DAY to Interval.MONTH -> baseGoal
            Interval.DAY to Interval.YEAR -> baseGoal * 30
            Interval.WEEK to Interval.MONTH -> baseGoal / 7
            Interval.WEEK to Interval.YEAR -> (baseGoal / 7) * 30
            Interval.MONTH to Interval.YEAR -> baseGoal
            else -> -1
        }
    }

}
