package org.kde.bettercounter.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.Utils
import org.kde.bettercounter.R
import org.kde.bettercounter.StatsCalculator
import org.kde.bettercounter.extensions.truncate
import org.kde.bettercounter.persistence.Entry
import java.text.DateFormatSymbols
import java.text.FieldPosition
import java.text.SimpleDateFormat
import java.util.*

class BetterChart : BarChart {

    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context, attrs: AttributeSet?, defStyle: Int) : super(ctx, attrs, defStyle)

    private val yAxis : YAxis get() { return axisLeft } // alias

    private lateinit var mDataSet : BarDataSet
    private lateinit var mBarData : BarData

    fun setup() {
        setScaleEnabled(false)
        setDrawBarShadow(false)
        setDrawGridBackground(false)
        setDrawValueAboveBar(true)

        val accentColor = context.getColor(R.color.colorAccent)

        // No data text
        setNoDataText(context.getString(R.string.no_data))
        mInfoPaint.textSize = Utils.convertDpToPixel(16f)
        mInfoPaint.color = accentColor

        // Axis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = accentColor
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        yAxis.textColor = accentColor
        yAxis.granularity = 1f
        yAxis.isGranularityEnabled = true

        legend.isEnabled = false
        axisRight.isEnabled = false
        description.isEnabled = false

        mDataSet = BarDataSet(listOf(), "")
        mDataSet.color = accentColor
        mDataSet.valueTextColor = accentColor
        mDataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val integer = value.toInt()
                if (integer == 0) {
                    return  ""
                }
                return integer.toString()
            }
        }

        mBarData = BarData(listOf(mDataSet))
        mBarData.isHighlightEnabled = false
        mBarData.barWidth = 0.9f
        data = mBarData
    }

    private fun setBarEntries(series: List<BarEntry>) {
        mDataSet.values = series
        mBarData.notifyDataChanged()
        data = mBarData
        invalidate()
    }

    private fun setDataBucketized(
        intervalEntries: List<Entry>,
        bucketTypeAsCalendarField: Int,
        numBuckets: Int
    ) {
        if (intervalEntries.isEmpty()) {
            clear()
            return
        }

        val sortedEntries = intervalEntries.sortedBy { it.date }

        val cal = Calendar.getInstance()
        //cal.toSimpleDateString().andLog()
        cal.add(bucketTypeAsCalendarField, -numBuckets+1)
        //cal.toSimpleDateString().andLog()
        cal.truncate(bucketTypeAsCalendarField)
        //cal.toSimpleDateString().andLog()

        var entriesIndex = 0
        while (entriesIndex < sortedEntries.size && sortedEntries[entriesIndex].date.time < cal.timeInMillis) {
            entriesIndex++
        }
        if (entriesIndex > 0) {
            Log.e(
                "setDataBucketized",
                "Skipping $entriesIndex entries that are too old. This should not happen."
            )
        }
        var maxCount = 0
        val series : MutableList<BarEntry> = mutableListOf()
        for (bucket in 1..numBuckets) {
            cal.add(bucketTypeAsCalendarField, 1) //Calendar is now at the end of the current bucket
            var bucketCount = 0
            while (entriesIndex < sortedEntries.size && sortedEntries[entriesIndex].date.time < cal.timeInMillis) {
                bucketCount++
                entriesIndex++
            }
            if (bucketCount > maxCount) {
                maxCount = bucketCount
            }
            //Log.e("Bucket", "$bucket -> ${cal.toSimpleDateString()} -> $bucketCount")
            series.add(BarEntry(bucket.toFloat(), bucketCount.toFloat()))
        }

        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = maxCount.toFloat()
        xAxis.labelCount = series.size
        xAxis.valueFormatter = when (bucketTypeAsCalendarField) {
            Calendar.DAY_OF_WEEK -> DayOfWeekFormatter(numBuckets)
            Calendar.MONTH -> MonthFormatter(numBuckets)
            else -> CalendarFormatter(numBuckets, bucketTypeAsCalendarField)
        }

        setBarEntries(series)
    }

    class DayOfWeekFormatter(private var numBuckets: Int) : ValueFormatter() {
        private val dayNames = DateFormatSymbols().shortWeekdays
        override fun getFormattedValue(value: Float): String {
            val cal = Calendar.getInstance()
            cal.truncate(Calendar.DAY_OF_WEEK)
            cal.add(Calendar.DAY_OF_WEEK, -numBuckets + value.toInt())
            return dayNames[cal.get(Calendar.DAY_OF_WEEK)]
        }
    }

    class MonthFormatter(private var numBuckets: Int) : ValueFormatter() {
        // When the language requires different month forms for formatting and stand-alone usages, then
        // DateFormatSymbols returns short month names in the formatting form, but we want stand-alone.
        private val monthNamesFormatter = SimpleDateFormat("LLL", Locale.getDefault())
        override fun getFormattedValue(value: Float): String {
            val cal = Calendar.getInstance()
            cal.truncate(Calendar.MONTH)
            cal.add(Calendar.MONTH, -numBuckets + value.toInt())
            monthNamesFormatter.timeZone = cal.timeZone
            return monthNamesFormatter.format(cal.time, StringBuffer(), FieldPosition(0)).toString()

        }
    }

    class CalendarFormatter(private var numBuckets: Int, private var field: Int) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val cal = Calendar.getInstance()
            cal.truncate(field)
            cal.add(field, -numBuckets + value.toInt())
            return cal.get(field).toString()
        }
    }

    fun setColorForNextDataSet(color : Int) {
        mDataSet.color = color
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
        val currentMonthSinceEpoch = 12 * cal.get(Calendar.YEAR) + cal.get(Calendar.MONTH)
        cal.timeInMillis = oldest
        val oldestEntryMonthSinceEpoch = 12 * cal.get(Calendar.YEAR) + cal.get(Calendar.MONTH)
        setDataBucketized(
            intervalEntries,
            Calendar.MONTH,
            currentMonthSinceEpoch - oldestEntryMonthSinceEpoch + 1
        )
    }
}
