package org.kde.bettercounter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import org.kde.bettercounter.databinding.FragmentChartBinding
import org.kde.bettercounter.extensions.addInterval
import org.kde.bettercounter.extensions.copy
import org.kde.bettercounter.extensions.count
import org.kde.bettercounter.extensions.truncate
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Interval
import org.kde.bettercounter.ui.ChartHolder
import java.util.Calendar

class ChartsAdapter(
    private val activity: AppCompatActivity,
    private val viewModel: ViewModel,
    private val counter: CounterSummary,
    private val interval: Interval,
    private val onIntervalChange: (Interval) -> Unit
) : RecyclerView.Adapter<ChartHolder>() {
    private val boundViewHolders = mutableListOf<ChartHolder>()

    private val inflater: LayoutInflater = LayoutInflater.from(activity)

    private var numCharts: Int = countNumCharts(counter)
    override fun getItemCount(): Int = numCharts

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartHolder {
        val binding = FragmentChartBinding.inflate(inflater, parent, false)
        return ChartHolder(activity, binding)
    }

    override fun onBindViewHolder(holder: ChartHolder, position: Int) {
        val rangeStart = findRangeStartForPosition(position)
        val rangeEnd = rangeStart.copy().apply { addInterval(interval, 1) }
        //Log.e("ChartsAdapter", "${rangeStart.toSimpleDateString()} plus ${counter.intervalForChart} equals ${rangeEnd.toSimpleDateString()}")
        boundViewHolders.add(holder)
        viewModel.getEntriesForRangeSortedByDate(
            counter.name,
            rangeStart.time,
            rangeEnd.time
        ).observe(activity) { entries ->
            holder.onBind(counter, entries, interval, rangeStart, rangeEnd, onIntervalChange)
        }
    }

    private fun findRangeStartForPosition(position: Int): Calendar {
        val cal = Calendar.getInstance()
        val endRange = counter.leastRecent
        if (endRange != null) {
            cal.time = endRange
        }
        cal.truncate(interval)
        cal.addInterval(interval, position)
        return cal
    }

    override fun onViewRecycled(holder: ChartHolder) {
        boundViewHolders.remove(holder)
    }

    private fun countNumCharts(counter: CounterSummary): Int {
        val firstDate = counter.leastRecent ?: return 1
        val lastDate = counter.latestBetweenNowAndMostRecentEntry()
        return interval.toChronoUnit().count(firstDate, lastDate)
    }

/*
    fun animate() {
        for (holder in boundViewHolders) {
            holder.binding.chart.animateY(200)
        }
    }
*/
}
