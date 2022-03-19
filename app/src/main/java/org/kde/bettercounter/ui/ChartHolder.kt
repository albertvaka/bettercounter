package org.kde.bettercounter.ui

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import org.kde.bettercounter.databinding.FragmentChartBinding


class ChartHolder(
    private val context : Context,
    val binding : FragmentChartBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun onBind() {
        binding.chart.setup()
    }
}
