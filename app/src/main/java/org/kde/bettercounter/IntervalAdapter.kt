package org.kde.bettercounter

import android.content.Context
import android.widget.ArrayAdapter
import org.kde.bettercounter.persistence.Interval

class IntervalAdapter(
    context: Context
) : ArrayAdapter<String>(
    context,
    android.R.layout.simple_spinner_dropdown_item,
    Interval.humanReadableValues(context)
) {
    fun positionOf(interval: Interval): Int = Interval.entries.indexOf(interval)

    fun itemAt(position: Int): Interval = Interval.entries[position]
}
