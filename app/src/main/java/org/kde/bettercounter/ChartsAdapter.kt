package org.kde.bettercounter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import org.kde.bettercounter.databinding.FragmentChartBinding
import org.kde.bettercounter.extensions.addInterval
import org.kde.bettercounter.extensions.copy
import org.kde.bettercounter.extensions.toZonedDateTime
import org.kde.bettercounter.extensions.truncate
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Entry
import org.kde.bettercounter.persistence.Interval
import org.kde.bettercounter.ui.ChartHolder
import java.time.temporal.ChronoUnit
import java.util.*

class ChartsAdapter(
    private val activity: AppCompatActivity,
    private val viewModel: ViewModel,
    private var counter: CounterSummary
) : RecyclerView.Adapter<ChartHolder>()
{
    private val boundViewHolders = mutableListOf<ChartHolder>()

    private val inflater: LayoutInflater = LayoutInflater.from(activity)

    private var numCharts : Int = countNumCharts(counter)
    override fun getItemCount(): Int = numCharts

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartHolder {
        val binding = FragmentChartBinding.inflate(inflater, parent, false)
        return ChartHolder(activity, binding)
    }

    override fun onBindViewHolder(holder: ChartHolder, position: Int) {
        val rangeStart = findRangeStartForPosition(position)
        val rangeEnd = rangeStart.copy().apply { addInterval(counter.intervalForChart, 1) }
        //Log.e("ChartsAdapter", "${rangeStart.toSimpleDateString()} plus ${counter.intervalForChart} equals ${rangeEnd.toSimpleDateString()}")
        boundViewHolders.add(holder)
        viewModel.getEntriesForRangeSortedByDate(counter.name, rangeStart.time, rangeEnd.time).observe(activity) {
            val periodAverage = getPeriodAverageString(it, rangeStart, rangeEnd)
            val lifetimeAverage = getLifetimeAverageString()
            val averageString = activity.getString(R.string.stats_averages, periodAverage, lifetimeAverage)
            holder.onBind(counter, it, rangeStart, averageString)
        }

    }

    private fun getLifetimeAverageString(): String {
        if (counter.totalCount == 0) {
            return activity.getString(R.string.stats_average_n_a)
        }

        val beginRange = counter.leastRecent!!
        val now = Date.from(Calendar.getInstance().toInstant())
        val lastEntryDate = counter.mostRecent!!
        val endRange = if (lastEntryDate > now) lastEntryDate else now

        return when (counter.interval) {
            Interval.DAY -> getAverageStringPerHour(counter.totalCount, beginRange, endRange)
            else -> getAverageStringPerDay(counter.totalCount, beginRange, endRange)
        }
    }

    private fun getPeriodAverageString(intervalEntries: List<Entry>, rangeStart: Calendar, rangeEnd: Calendar): String {
        if (intervalEntries.isEmpty()) {
            return activity.getString(R.string.stats_average_n_a)
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
        var days = ChronoUnit.DAYS.between(startDate.toZonedDateTime(), endDate.toZonedDateTime())
        days += 1L
        val avgPerDay = count.toFloat()/days
        return if (avgPerDay > 1) {
            activity.getString(R.string.stats_average_per_day, avgPerDay)
        } else {
            activity.getString(R.string.stats_average_every_days, 1/avgPerDay)
        }
    }

    private fun getAverageStringPerHour(count: Int, startDate: Date, endDate: Date): String {
        var hours = ChronoUnit.HOURS.between(startDate.toZonedDateTime(), endDate.toZonedDateTime())
        hours += 1L
        val avgPerHour = count.toFloat()/hours
        return if (avgPerHour > 1) {
            activity.getString(R.string.stats_average_per_hour, avgPerHour)
        } else {
            activity.getString(R.string.stats_average_every_hours, 1/avgPerHour)
        }
    }

    private fun findRangeStartForPosition(position: Int): Calendar {
        val cal = Calendar.getInstance()
        val endRange = counter.leastRecent
        if (endRange != null) {
            cal.time = endRange
        }
        cal.truncate(counter.intervalForChart)
        cal.addInterval(counter.intervalForChart, position)
        return cal
    }

    override fun onViewRecycled(holder: ChartHolder) {
        boundViewHolders.remove(holder);
    }

    private fun countNumCharts(counter: CounterSummary) : Int {
        val firstDate = counter.leastRecent
        val lastDate = counter.mostRecent
        if (lastDate == null || firstDate == null) {
            return 1
        }
        return counter.intervalForChart.count(firstDate, lastDate)
    }

/*
    fun animate() {
        for (holder in boundViewHolders) {
            holder.binding.chart.animateY(200)
        }
    }
*/
}

