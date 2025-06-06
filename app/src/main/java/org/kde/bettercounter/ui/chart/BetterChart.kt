package org.kde.bettercounter.ui.chart

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import org.kde.bettercounter.R
import org.kde.bettercounter.extensions.copy
import org.kde.bettercounter.persistence.Entry
import org.kde.bettercounter.persistence.Interval
import java.text.DateFormatSymbols
import java.text.FieldPosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BetterChart : BarChart {

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context, attrs: AttributeSet?, defStyle: Int) : super(ctx, attrs, defStyle)

    private val yAxis: YAxis get() = axisLeft // alias

    private lateinit var mDataSet: BarDataSet
    private lateinit var mBarData: BarData

    fun setup() {
        setScaleEnabled(false)
        setDrawBarShadow(false)
        setDrawGridBackground(false)
        setDrawValueAboveBar(true)
        setXAxisRenderer(object : XAxisRenderer(mViewPortHandler, mXAxis, mLeftAxisTransformer) {
            override fun computeSize() {
                super.computeSize()
                // Hack so that labels are not cut on the bottom
                mXAxis.mLabelHeight += Utils.convertDpToPixel(5.0f).toInt()
            }
        })

        val accentColor = ContextCompat.getColor(context, R.color.colorAccent)

        // No data text
        setNoDataText(context.getString(R.string.no_data))
        mInfoPaint.textSize = Utils.convertDpToPixel(16f)
        mInfoPaint.color = accentColor

        // Axis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textSize = 13.0f
        xAxis.textColor = accentColor
        xAxis.isGranularityEnabled = true // granularity defined in setDataBucketized
        yAxis.textSize = 13.0f
        yAxis.textColor = accentColor
        yAxis.granularity = 1f
        yAxis.isGranularityEnabled = true

        legend.isEnabled = false
        axisRight.isEnabled = false
        description.isEnabled = false

        mDataSet = BarDataSet(listOf(), "")
        mDataSet.color = accentColor
        mDataSet.valueTextSize = 12.0f
        mDataSet.valueTextColor = accentColor
        mDataSet.valueFormatter = object : IValueFormatter {
            override fun getFormattedValue(
                value: Float,
                entry: com.github.mikephil.charting.data.Entry?,
                dataSetIndex: Int,
                viewPortHandler: ViewPortHandler?
            ): String? {
                val integer = value.toInt()
                if (integer == 0) {
                    return ""
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
        mDataSet.entries = series
        mBarData.notifyDataChanged()
        data = mBarData
        invalidate()
    }

    fun setDataBucketized(
        intervalEntries: List<Entry>,
        rangeStart: Calendar,
        totalInterval: Interval,
        color: Int,
        goalLine: Float,
        maxCount: Int,
    ) {
        mDataSet.color = color

        if (intervalEntries.isEmpty()) {
            clear()
            return
        }

        val bucketIntervalAsCalendarField = totalInterval.asCalendarField()
        val numBuckets = when (totalInterval) {
            Interval.HOUR -> 60
            Interval.DAY -> 24
            Interval.WEEK -> 7
            Interval.MONTH -> rangeStart.getActualMaximum(Calendar.DAY_OF_MONTH)
            Interval.YEAR -> 12
            else -> 0.also { assert(false) }
        }

        xAxis.granularity = when (totalInterval) {
            Interval.HOUR -> 3.0f
            Interval.MONTH -> 2.0f
            else -> 1f
        }

        val cal = rangeStart.copy()

        // All given entries should be after rangeStart and before rangeStart+numBuckets
        assert(intervalEntries.first().date.time >= cal.timeInMillis) {
            "Entry on ${intervalEntries.first().date} is not after ${cal.time}}"
        }
        val endCal = ((cal.clone() as Calendar).apply { add(bucketIntervalAsCalendarField, numBuckets) })
        assert(intervalEntries.last().date.time < endCal.timeInMillis) {
            "Entry on ${intervalEntries.first().date} is not before ${endCal.time}}"
        }

        var entriesIndex = 0
        val series: MutableList<BarEntry> = mutableListOf()
        for (bucket in 0 until numBuckets) {
            cal.add(bucketIntervalAsCalendarField, 1) // Calendar is now at the end of the current bucket
            var bucketCount = 0
            while (entriesIndex < intervalEntries.size && intervalEntries[entriesIndex].date.time < cal.timeInMillis) {
                bucketCount++
                entriesIndex++
            }
            // Log.e("Bucket", "$bucket (ends ${cal.debugToSimpleDateString()}) -> $bucketCount")
            series.add(BarEntry(bucket.toFloat(), bucketCount.toFloat()))
        }

        yAxis.limitLines.clear()
        if (goalLine > 0) {
            val limitLine = LimitLine(goalLine).apply {
                lineColor = color
            }
            yAxis.limitLines.add(limitLine)
        }

        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = maxCount.toFloat()
        xAxis.labelCount = series.size
        xAxis.valueFormatter = when (bucketIntervalAsCalendarField) {
            Calendar.MINUTE -> RawFormatter()
            Calendar.HOUR_OF_DAY -> RawFormatter()
            Calendar.DAY_OF_WEEK -> DayOfWeekFormatter()
            Calendar.DAY_OF_MONTH -> MonthDayFormatter()
            Calendar.MONTH -> MonthFormatter(rangeStart)
            else -> null.also { assert(false) }
        }

        setBarEntries(series)
    }

    class DayOfWeekFormatter : IAxisValueFormatter {
        private val dayNames = DateFormatSymbols().shortWeekdays
        private val firstDayOfWeek = Calendar.getInstance().firstDayOfWeek
        override fun getFormattedValue(
            value: Float,
            axis: AxisBase?
        ): String? {
            // dayNames are meant to be indexed with Calendar.SATURDAY,
            // Calendar.MONDAY, etc. so the range is [1,7] with 1 being Sunday.
            // The range of bucket indices is [0,6] with 0 being Monday.
            return dayNames[((firstDayOfWeek + value.toInt() - 1) % 7) + 1]
        }
    }

    class MonthFormatter(private var startDate: Calendar) : IAxisValueFormatter {
        // We can't use DateFormatSymbols like for week days, because when
        // the user language uses different month name forms for formatting
        // and stand-alone usages, then DateFormatSymbols returns names in
        // the formatting form, but we want the stand-alone form.
        private val monthNamesFormatter = SimpleDateFormat("LLL", Locale.getDefault())
        override fun getFormattedValue(
            value: Float,
            axis: AxisBase?
        ): String? {
            val cal = startDate.copy()
            cal.add(Calendar.MONTH, value.toInt())
            monthNamesFormatter.timeZone = cal.timeZone
            return monthNamesFormatter.format(cal.time, StringBuffer(), FieldPosition(0)).toString()
        }
    }

    class MonthDayFormatter : IAxisValueFormatter {
        override fun getFormattedValue(
            value: Float,
            axis: AxisBase?
        ): String? {
            return (value.toInt() + 1).toString()
        }
    }

    class RawFormatter : IAxisValueFormatter {
        override fun getFormattedValue(
            value: Float,
            axis: AxisBase?
        ): String? {
            return (value.toInt()).toString()
        }
    }
}
