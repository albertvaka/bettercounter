package org.kde.bettercounter.ui.chart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.kde.bettercounter.databinding.FragmentChartBinding
import org.kde.bettercounter.extensions.between
import org.kde.bettercounter.extensions.count
import org.kde.bettercounter.extensions.plusInterval
import org.kde.bettercounter.extensions.toCalendar
import org.kde.bettercounter.extensions.truncated
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Interval
import org.kde.bettercounter.ui.main.MainActivityViewModel
import java.util.Calendar

class ChartsAdapter(
    private val activity: AppCompatActivity,
    private val viewModel: MainActivityViewModel,
    private val counter: CounterSummary,
    private val interval: Interval,
    private val onIntervalChange: (Interval) -> Unit,
    private val onDateChange: ChartsAdapter.(Calendar) -> Unit,
    private val onDataDisplayed: () -> Unit,
) : RecyclerView.Adapter<ChartHolder>() {

    val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val boundViewHolders = mutableListOf<ChartHolder>()

    private val inflater: LayoutInflater = LayoutInflater.from(activity)

    private var numCharts: Int = countNumCharts(counter)
    override fun getItemCount(): Int = numCharts

    val allEntriesFlow = viewModel
        .getAllEntriesSortedByDate(counter.name)
        .shareIn(coroutineScope, SharingStarted.Eagerly, 1)

    val maxCountFlow = allEntriesFlow
        .map { entries ->
            ChartDataAggregation.computeMaxCountForAllEntries(entries, interval)
        }.shareIn(coroutineScope, SharingStarted.Eagerly, 1)

    val lifetimeGoalReachedFlow = allEntriesFlow
        .map { entries ->
            ChartDataAggregation.computeGoalReached(counter.goal, counter.interval, Interval.LIFETIME, entries)
        }.shareIn(coroutineScope, SharingStarted.Eagerly, 1)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartHolder {
        val binding = FragmentChartBinding.inflate(inflater, parent, false)
        return ChartHolder(activity, viewModel, binding)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        coroutineScope.cancel()
    }

    override fun onBindViewHolder(holder: ChartHolder, position: Int) {
        val rangeStart = findRangeStartForPosition(position)
        val rangeEnd = rangeStart.plusInterval(interval, 1)
        boundViewHolders.add(holder)

        val entriesFlow = viewModel.getEntriesForRangeSortedByDate(
            counter.name,
            rangeStart.time,
            rangeEnd.time
        )

        // Use the end of this interval and not at the beginning of the next,
        // so weeks have 7 days and not 8 because of the rounding up we do later.
        // FIXME: What rounding? Is this still needed?
        rangeEnd.add(Calendar.MINUTE, -1)

        coroutineScope.launch {
            combine(maxCountFlow, lifetimeGoalReachedFlow, entriesFlow, ::Triple)
                .collect { (maxCount, lifetimeGoalReached, entries) ->
                val buckets = ChartDataAggregation.computeBucketsForIntervalEntries(
                    intervalEntries = entries,
                    interval = interval,
                    rangeStart = rangeStart,
                )
                val periodGoalReached = ChartDataAggregation.computeGoalReached(
                    goal = counter.goal,
                    counterInterval = counter.interval,
                    displayInterval = interval,
                    entries = entries,
                )
                holder.display(
                    counter,
                    buckets,
                    entries.size,
                    interval,
                    rangeStart,
                    rangeEnd,
                    maxCount,
                    periodGoalReached,
                    lifetimeGoalReached,
                    onIntervalChange,
                ) { onDateChange(it) }
                onDataDisplayed()
            }
        }
    }

    private fun findRangeStartForPosition(position: Int): Calendar {
        val counterBegin = counter.leastRecent?.toCalendar() ?: Calendar.getInstance()
        val firstInterval = counterBegin.truncated(interval)
        return firstInterval.plusInterval(interval, position)
    }

    fun findPositionForRangeStart(cal: Calendar): Int {
        val endRange = counter.leastRecent
        if (endRange != null) {
            val endCal = endRange.toCalendar().truncated(interval)
            val count = interval.toChronoUnit().between(endCal, cal).toInt()
            return count.coerceIn(0, numCharts - 1)
        }
        return 0
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
