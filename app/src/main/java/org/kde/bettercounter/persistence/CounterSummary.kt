package org.kde.bettercounter.persistence

import java.util.Calendar
import java.util.Date

class CounterSummary(
    var name: String,
    var interval: Interval,
    var goal: Int,
    var color: CounterColor,
    val lastIntervalCount: Int,
    val totalCount: Int,
    val leastRecent: Date?,
    val mostRecent: Date?,
) {
    fun latestBetweenNowAndMostRecentEntry(): Date {
        val now = Calendar.getInstance().time
        val lastEntry = mostRecent
        return if (lastEntry != null && lastEntry > now) lastEntry else now
    }

    fun hasGoal(): Boolean {
        return goal > 0
    }

    fun isGoalMet(): Boolean {
        return goal in 1..lastIntervalCount
    }

    fun getFormattedCount(): CharSequence = buildString {
        append(lastIntervalCount)
        if (hasGoal()) {
            append('/')
            append(goal)
        }
    }
}
