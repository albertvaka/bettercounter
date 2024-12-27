package org.kde.bettercounter.extensions

import android.content.Context

fun Int.dpToPx(context: Context): Int {
    val density = context.resources.displayMetrics.density
    return (this * density).toInt()
}
