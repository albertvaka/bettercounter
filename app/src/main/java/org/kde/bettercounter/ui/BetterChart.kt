package org.kde.bettercounter.ui

import android.content.Context
import android.util.AttributeSet
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import org.kde.bettercounter.R
import org.kde.bettercounter.StatsCalculator
import org.kde.bettercounter.boilerplate.andLog
import org.kde.bettercounter.persistence.Entry
import java.text.DateFormatSymbols
import java.time.Month
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
        yAxis.granularity = 1f;
        yAxis.isGranularityEnabled = true

        legend.isEnabled = false
        axisRight.isEnabled = false
        description.isEnabled = false

        dataSet = BarDataSet(listOf(), "")
        dataSet.color = accentColor
        dataSet.valueTextColor = accentColor
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val integer = value.toInt()
                if (integer == 0) {
                    return  ""
                }
                return integer.toString()
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

    fun setDataBucketized(intervalEntries: List<Entry>, bucketTypeAsCalendarField : Int, minNumBuckets : Int) {
        if (intervalEntries.isEmpty()) return setBarEntries(listOf())

        val cal = Calendar.getInstance()

        val currentTimeBucket = cal.get(bucketTypeAsCalendarField)

        val counts : MutableMap<Int, Int> = mutableMapOf()
        for (bucket in (currentTimeBucket-minNumBuckets+1)..currentTimeBucket) { // Create minNumBuckets empty buckets
            counts[bucket] = 0
        }
        for (entry in intervalEntries) {
            cal.timeInMillis = entry.date.time
            val bucket = cal.get(bucketTypeAsCalendarField)
            counts[bucket] = counts.getOrDefault(bucket, 0) + 1
        }

        var maxCount = 0
        val series : MutableList<BarEntry> = mutableListOf()
        for (time in counts.keys.sorted()) {
            val count = counts.getOrDefault(time, 0)
            if (count > maxCount) {
                maxCount = count
            }
            val relTime = (time - currentTimeBucket)
            series.add(BarEntry(relTime.toFloat(), count.toFloat()))
            //Log.e("DATAPOINT", "$relTime = $count")
        }

        yAxis.axisMinimum = 0.0f
        yAxis.axisMaximum = maxCount.toFloat()
        xAxis.labelCount = counts.size
        xAxis.valueFormatter = when(bucketTypeAsCalendarField) {
            Calendar.DAY_OF_WEEK -> DayOfWeekFormatter()
            Calendar.MONTH -> MonthFormatter()
            else -> CalendarFormatter(bucketTypeAsCalendarField)
        }

        setBarEntries(series)

        Calendar.getInstance().get(Calendar.MONTH).andLog()
    }

    class DayOfWeekFormatter : ValueFormatter() {
        private val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        private val dayNames = DateFormatSymbols().shortWeekdays
        override fun getFormattedValue(value: Float): String {
            var dow = currentDayOfWeek+value.toInt()
            while (dow < 1) dow += 7
            return dayNames[dow]
        }
    }

    class MonthFormatter : ValueFormatter() {
        private val monthNames = DateFormatSymbols().shortMonths
        private val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        override fun getFormattedValue(value: Float): String {
            var month = currentMonth+value.toInt()
            while (month < 0) month += 12
            return monthNames[month]
        }
    }

    class CalendarFormatter(var field : Int) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val cal = Calendar.getInstance()
            cal.add(field, value.toInt())
            return cal.get(field).toString()
        }
    }

    fun setDailyData(intervalEntries: List<Entry>) {
        setDataBucketized(intervalEntries, Calendar.HOUR_OF_DAY, 24)
    }

    fun setWeeklyData(intervalEntries: List<Entry>) {
        setDataBucketized(intervalEntries, Calendar.DAY_OF_WEEK, 7)
    }

    fun setMonthlyData(intervalEntries: List<Entry>) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        val pastMonthDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        setDataBucketized(intervalEntries, Calendar.DAY_OF_MONTH, pastMonthDays)
    }

    fun setYearlyData(intervalEntries: List<Entry>) {
        setDataBucketized(intervalEntries, Calendar.MONTH, 12)
    }

    fun setYtdData(intervalEntries: List<Entry>) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)+1 // Calendar months are 0-indexed lol
        setDataBucketized(intervalEntries, Calendar.MONTH, currentMonth)
    }

    fun setLifetimeData(intervalEntries: List<Entry>) {
        if (intervalEntries.isEmpty()) return setBarEntries(listOf())
        val oldest = StatsCalculator.getOldestEntryTime(intervalEntries)
        val cal = Calendar.getInstance()
        cal.timeInMillis = oldest
    }
}