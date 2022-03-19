package org.kde.bettercounter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import org.kde.bettercounter.databinding.FragmentChartBinding
import org.kde.bettercounter.persistence.CounterDetails
import org.kde.bettercounter.persistence.Interval
import org.kde.bettercounter.ui.ChartHolder

class ChartsAdapter(
    private var activity: AppCompatActivity,
) : RecyclerView.Adapter<ChartHolder>()
{


    private val inflater: LayoutInflater = LayoutInflater.from(activity)

    override fun getItemCount(): Int = 3

    private var boundHolder : ChartHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartHolder {
        val binding = FragmentChartBinding.inflate(inflater, parent, false)
        return ChartHolder(activity, binding)
    }
/*
    override fun onViewAttachedToWindow(holder: ChartHolder) {
        super.onViewAttachedToWindow(holder)
    }
*/
    override fun onBindViewHolder(holder: ChartHolder, position: Int) {
        holder.onBind()
        boundHolder = holder
    }

    fun setChart(counter: CounterDetails) {

        val oneHolder = boundHolder

        if (oneHolder == null) {
            Log.e("ONEHOLDER","IS NULLERINO AAAAAAAAAAAAAAAAAAAAAAAA")
            return
        }

        val defaultColor = activity.getColor(R.color.colorPrimary)

        oneHolder.binding.chart.setColorForNextDataSet(if (counter.color == defaultColor) activity.getColor(R.color.colorAccent) else counter.color)
        val firstDate = counter.intervalEntries.firstOrNull()
        when (counter.interval) {
            Interval.DAY -> {
                oneHolder.binding.chart.setDailyData(counter.intervalEntries)
                oneHolder.binding.chartName.text = firstDate?.toString() ?: "" //TODO: Truncate the date to show up to the day
            }
            Interval.WEEK -> {
                oneHolder.binding.chart.setWeeklyData(counter.intervalEntries)
                oneHolder.binding.chartName.text = firstDate?.toString() ?: ""
            }
            Interval.MONTH -> {
                oneHolder.binding.chart.setMonthlyData(counter.intervalEntries)
                oneHolder.binding.chartName.text = firstDate?.toString() ?: ""
            }
            Interval.YEAR -> {
                oneHolder.binding.chart.setYearlyData(counter.intervalEntries)
                oneHolder.binding.chartName.text = firstDate?.toString() ?: ""
            }
            Interval.YTD -> {
                oneHolder.binding.chart.setYtdData(counter.intervalEntries)
                oneHolder.binding.chartName.text = firstDate?.toString() ?: ""
            }
            Interval.LIFETIME -> {
                oneHolder.binding.chart.setLifetimeData(counter.intervalEntries)
                oneHolder.binding.chartName.text = firstDate?.toString() ?: ""
            }
        }
    }

    fun animate() {
        boundHolder?.binding?.chart?.animateY(200)
    }

}
