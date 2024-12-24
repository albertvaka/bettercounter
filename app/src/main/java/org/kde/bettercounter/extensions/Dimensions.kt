package org.kde.bettercounter.extensions;

import android.content.Context;

public fun Int.dptoPx(context: Context): Int {
    val density = context.resources.displayMetrics.density
    return (this * density).toInt()
}

