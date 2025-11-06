package org.kde.bettercounter.persistence

import android.content.Context
import androidx.core.content.ContextCompat
import org.kde.bettercounter.R

@JvmInline
value class CounterColor(val colorInt: Int)

class CounterColors {
    val defaultColor: CounterColor
    val allColors: List<CounterColor>
    val rippleDrawables: Map<Int, Int>
    val defaultColorIntForChart: Int

    private constructor(context: Context) {
        defaultColor = CounterColor(ContextCompat.getColor(context, R.color.colorLightBackground))
        defaultColorIntForChart = ContextCompat.getColor(context, R.color.colorAccent)
        rippleDrawables =  mapOf(
            defaultColor.colorInt to R.drawable.ripple_color_default,
            ContextCompat.getColor(context, R.color.color1) to R.drawable.ripple_color_1,
            ContextCompat.getColor(context, R.color.color2) to R.drawable.ripple_color_2,
            ContextCompat.getColor(context, R.color.color3) to R.drawable.ripple_color_3,
            ContextCompat.getColor(context, R.color.color4) to R.drawable.ripple_color_4,
            ContextCompat.getColor(context, R.color.color5) to R.drawable.ripple_color_5,
            ContextCompat.getColor(context, R.color.color6) to R.drawable.ripple_color_6,
            ContextCompat.getColor(context, R.color.color7) to R.drawable.ripple_color_7,
            ContextCompat.getColor(context, R.color.color8) to R.drawable.ripple_color_8,
        )
        allColors = listOf(
            defaultColor,
            CounterColor(ContextCompat.getColor(context, R.color.color1)),
            CounterColor(ContextCompat.getColor(context, R.color.color2)),
            CounterColor(ContextCompat.getColor(context, R.color.color3)),
            CounterColor(ContextCompat.getColor(context, R.color.color4)),
            CounterColor(ContextCompat.getColor(context, R.color.color5)),
            CounterColor(ContextCompat.getColor(context, R.color.color6)),
            CounterColor(ContextCompat.getColor(context, R.color.color7)),
            CounterColor(ContextCompat.getColor(context, R.color.color8)),
        )
    }

    fun getRippleDrawableRes(counterColor: CounterColor): Int? {
        return rippleDrawables[counterColor.colorInt]
    }

    fun getColorIntForChart(counterColor: CounterColor): Int {
        return if (counterColor == defaultColor) {
            defaultColorIntForChart
        } else {
            counterColor.colorInt
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: CounterColors? = null

        fun getInstance(context: Context): CounterColors =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: CounterColors(context.applicationContext)
                    .also { INSTANCE = it }
            }
    }
}
