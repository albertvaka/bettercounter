package org.kde.bettercounter.persistence

data class CounterMetadata(
    var name: String,
    var interval: Interval,
    var goal: Int,
    var color: CounterColor,
)
