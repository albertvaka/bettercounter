package org.kde.bettercounter.ui

import android.content.Context
import android.util.AttributeSet
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import org.kde.bettercounter.R
import org.kde.bettercounter.persistence.Entry
import java.util.*

class BetterChart : BarChart {

    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context, attrs: AttributeSet?, defStyle: Int) : super(ctx, attrs, defStyle)

    private val yAxis : YAxis get() { return axisLeft } // alias

    lateinit var dataSet : BarDataSet

    fun setup() {
        setScaleEnabled(false)
        setDrawBarShadow(false)
        setDrawGridBackground(false)
        setDrawValueAboveBar(true)

        val accentColor = context.getColor(R.color.colorAccent)

        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = accentColor
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        yAxis.textColor = accentColor
        yAxis.granularity = 1.0f;
        yAxis.isGranularityEnabled = true

        legend.isEnabled = false
        axisRight.isEnabled = false
        description.isEnabled = false

        dataSet = BarDataSet(listOf(), "")
        dataSet.color = accentColor
        dataSet.valueTextColor = accentColor
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        val barData = BarData(listOf(dataSet))
        barData.isHighlightEnabled = false
        barData.barWidth = 0.9f
        data = barData
    }

    private fun setBarEntries(series : List<BarEntry>) {
        dataSet.values = series
        dataSet.notifyDataSetChanged()
        data.notifyDataChanged()
        notifyDataSetChanged()
        invalidate()
    }


    fun setDailyData(intervalEntries: List<Entry>) {

        val millisInHour = 60*60*1000L
        val nowHour = System.currentTimeMillis()/millisInHour

        val counts : MutableMap<Long, Int> = mutableMapOf()
        for (h in (nowHour-23)..nowHour) { // Create buckets for the last 24 hours
            counts[h] = 0
        }
        for (entry in intervalEntries) {
            val hour = entry.date.time/millisInHour
            counts[hour] = counts.getOrDefault(hour, 0) + 1
        }

        var maxCount = 0
        val series : MutableList<BarEntry> = mutableListOf()
        for (time in counts.keys.sorted()) {
            val count = counts.getOrDefault(time, 0)
            if (count > maxCount) {
                maxCount = count
            }
            val relTime: Long = (time - nowHour)
            series.add(BarEntry(relTime.toFloat(), count.toFloat()))
            //Log.e("DATAPOINT", "$relTime = $count")
        }

        yAxis.axisMinimum = 0.0f
        yAxis.axisMaximum = maxCount.toFloat()
        xAxis.labelCount = 24
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                var hour = Calendar.getInstance().get(Calendar.HOUR)+value.toInt()
                if (hour < 0) hour += 24
                return hour.toString()
            }
        }

        setBarEntries(series)
    }

}