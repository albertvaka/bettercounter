package org.kde.bettercounter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import org.kde.bettercounter.databinding.FragmentChartBinding
import org.kde.bettercounter.extensions.*
import org.kde.bettercounter.persistence.CounterDetails
import org.kde.bettercounter.persistence.Entry
import org.kde.bettercounter.persistence.Interval
import org.kde.bettercounter.ui.ChartHolder
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.*

class ChartsAdapter(
    private val activity: AppCompatActivity,
    private val viewModel: ViewModel,
    private var counter: CounterDetails
) : RecyclerView.Adapter<ChartHolder>()
{
    private val boundViewHolders = mutableListOf<ChartHolder>()

    private val inflater: LayoutInflater = LayoutInflater.from(activity)

    private var numCharts : Int = countNumCharts(counter).andLog("AAAA NumCharts")

    override fun getItemCount(): Int = numCharts

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartHolder {
        val binding = FragmentChartBinding.inflate(inflater, parent, false)
        return ChartHolder(activity, binding)
    }

    override fun onBindViewHolder(holder: ChartHolder, position: Int) {
        val rangeStart = findRangeStartForPosition(position)
        val rangeEnd = rangeStart.copy().also { it.addInterval(counter.intervalForChart, 1) }
        val entries = entriesForRange(rangeStart, rangeEnd).andLog("ENTRIES POS $position")
        val periodAverage = getPeriodAverageString(entries, rangeStart, rangeEnd)
        val lifetimeAverage = getLifetimeAverageString()
        val averageString = activity.getString(R.string.stats_averages, periodAverage, lifetimeAverage)
        holder.onBind(counter, entries, rangeStart, averageString)
        boundViewHolders.add(holder)
    }

    private fun getLifetimeAverageString(): String {
        if (counter.sortedEntries.isEmpty()) {
            return activity.getString(R.string.stats_average_n_a)
        }

        val firstEntryDate = counter.sortedEntries.first().date
        val lastEntryDate = counter.sortedEntries.last().date

        return when (counter.interval) {
            Interval.DAY -> getAverageStringPerHour(counter.sortedEntries.size,  firstEntryDate, lastEntryDate)
            else -> getAverageStringPerDay(counter.sortedEntries.size,  firstEntryDate, lastEntryDate)
        }
    }

    private fun getPeriodAverageString(intervalEntries: List<Entry>, rangeStart: Calendar?, rangeEnd: Calendar): String {
        if (intervalEntries.isEmpty()) {
            return activity.getString(R.string.stats_average_n_a)
        }

        var startDate = rangeStart!!.time
        val firstEntryDate = counter.sortedEntries.first().date
        val hasEntriesBefore = (firstEntryDate < rangeStart.time)
        if (hasEntriesBefore) {
            // Use the beginning of the current interval as the begin date
            startDate = rangeStart.copy().time
        }

        var endDate = rangeEnd.time
        val lastEntryDate = counter.sortedEntries.last().date
        val hasEntriesAfter = (lastEntryDate > rangeEnd.time)
        if (hasEntriesAfter) {
            // Use the end of the current interval as the end date
            endDate = rangeEnd.copy().also { it.addInterval(counter.intervalForChart, 1) }.time
        }

        return when (counter.interval) {
            Interval.DAY -> getAverageStringPerHour(intervalEntries.size, startDate, endDate)
            else -> getAverageStringPerDay(intervalEntries.size, startDate, endDate)
        }
    }

    private fun getAverageStringPerDay(count: Int, startDate: Date, endDate: Date): String {
        val days = ChronoUnit.DAYS.between(startDate.toZonedDateTime(), endDate.toZonedDateTime())
        if (days == 0L) {
            return activity.getString(R.string.stats_average_n_a)
        }
        val avgPerDay = count.toFloat()/days
        return if (avgPerDay > 1) {
            activity.getString(R.string.stats_average_per_day, avgPerDay)
        } else {
            activity.getString(R.string.stats_average_every_days, 1/avgPerDay)
        }
    }

    private fun getAverageStringPerHour(count: Int, startDate: Date, endDate: Date): String {
        val hours = ChronoUnit.HOURS.between(startDate.toZonedDateTime(), endDate.toZonedDateTime())
        if (hours == 0L) {
            return activity.getString(R.string.stats_average_n_a)
        }
        val avgPerHour = count.toFloat()/hours
        return if (avgPerHour > 1) {
            activity.getString(R.string.stats_average_per_hour, avgPerHour)
        } else {
            activity.getString(R.string.stats_average_every_hours, 1/avgPerHour)
        }
    }

    private fun entriesForRange(rangeStart: Calendar, rangeEnd : Calendar): List<Entry> {
        var from = counter.sortedEntries.indexOfFirst { it.date.after(rangeStart.time) }
        if (from == -1) {
            Log.e("DOES_THIS_HAPPEN APART FROM DUMMY CHART?", "from == -1")
            from = 0
        }
        var to = counter.sortedEntries.indexOfLast { it.date.before(rangeEnd.time) }
        to = if (to == -1) {
            Log.e("DOES_THIS_HAPPEN APART FROM DUMMY CHART?", "to == -1")
            counter.sortedEntries.size
        } else {
            to + 1
        }

        return counter.sortedEntries.subList(from, to)
    }

    private fun findRangeStartForPosition(position: Int): Calendar {
        val cal = Calendar.getInstance()
        val endRange = counter.sortedEntries.lastOrNull()?.date
        if (endRange != null) {
            cal.time = endRange
        }
        when (counter.interval) {
            Interval.DAY -> cal.truncate(Calendar.HOUR)
            else -> cal.truncate(Calendar.DATE)
        }
        cal.addInterval(counter.intervalForChart, -position)
        return cal
    }

    override fun onViewRecycled(holder: ChartHolder) {
        boundViewHolders.remove(holder);
    }

    private fun countNumCharts(counter: CounterDetails) : Int {
        val firstDate = counter.sortedEntries.firstOrNull()?.date?.toZonedDateTime()
        val lastDate = counter.sortedEntries.lastOrNull()?.date?.toZonedDateTime()
        if (lastDate == null || firstDate == null) {
            return 1
        }
        val count = when (counter.intervalForChart) {
            Interval.DAY -> ChronoUnit.DAYS.between(firstDate, lastDate)
            Interval.WEEK -> ChronoUnit.WEEKS.between(firstDate, lastDate)
            Interval.MONTH -> ChronoUnit.MONTHS.between(firstDate, lastDate)
            Interval.YEAR -> ChronoUnit.YEARS.between(firstDate, lastDate)
            Interval.LIFETIME -> 0.also { assert(false) } // Not a valid display interval
        }
        return count.toInt() + 1 // between end date is non-inclusive
    }

/*
    fun animate() {
        for (holder in boundViewHolders) {
            holder.binding.chart.animateY(200)
        }
    }
*/
}
