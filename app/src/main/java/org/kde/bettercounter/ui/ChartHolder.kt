package org.kde.bettercounter.ui

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import org.kde.bettercounter.R
import org.kde.bettercounter.databinding.FragmentChartBinding
import org.kde.bettercounter.persistence.CounterDetails
import org.kde.bettercounter.persistence.Entry
import org.kde.bettercounter.persistence.Interval
import java.text.SimpleDateFormat
import java.util.*


class ChartHolder(
    private val context : Context,
    private val binding : FragmentChartBinding,
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.chart.setup()
    }

    fun onBind(counter: CounterDetails, position : Int, entries : List<Entry>, rangeStart : Calendar, averageString : String) {
        // Chart name
        val dateFormat = when (counter.intervalForChart) {
            Interval.DAY, Interval.WEEK -> "dd/MM/yyyy"
            Interval.MONTH -> "MM/yyyy"
            Interval.YEAR -> "yyyy"
            Interval.LIFETIME -> "".also { assert(false) } // Not a valid display interval
        }
        binding.chartName.text = SimpleDateFormat(dateFormat, Locale.getDefault()).format(rangeStart.time)

        // Chart
        val defaultColor = context.getColor(R.color.colorPrimary)
        val color = if (counter.color == defaultColor) context.getColor(R.color.colorAccent) else counter.color
        binding.chart.setDataBucketized(entries, rangeStart, counter.intervalForChart, color)

        // Stats
        binding.chartAverage.text = averageString
        val periodString = context.getString(counter.intervalForChart.humanReadableResource)
        binding.chartTotal.text = context.getString(R.string.stats_total_period, periodString, entries.size)
    }
}
