package org.kde.bettercounter.extensions

import java.time.ZonedDateTime

fun ZonedDateTime.toEpochMilli(): Long = toInstant().toEpochMilli()
