package org.kde.bettercounter.persistence

data class CounterMetadata(
    var name: String,
    val interval: Interval,
    val goal: Int,
    val color: CounterColor,
)
