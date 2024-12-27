package org.kde.bettercounter.persistence

import android.content.Context
import androidx.core.content.ContextCompat
import org.kde.bettercounter.R

@JvmInline
value class CounterColor(val colorInt: Int) {

    fun toColorForChart(context: Context): Int {
        val defaultCounterColor = getDefault(context)
        if (this == defaultCounterColor) {
            return ContextCompat.getColor(context, R.color.colorAccent)
        }
        return colorInt
    }

    companion object {
        fun getDefault(context: Context): CounterColor {
            val colorArray = context.resources.obtainTypedArray(R.array.picker_colors)
            val color = colorArray.getColor(0, 0)
            colorArray.recycle()
            return CounterColor(color)
        }
    }
}
