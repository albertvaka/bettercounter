package org.kde.bettercounter.persistence

import androidx.room.*
import org.kde.bettercounter.boilerplate.Converters
import java.util.*

@Entity(indices = [Index("name")])
@TypeConverters(Converters::class)
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val date: Date,
    val name: String,
)

@TypeConverters(Converters::class)
data class FirstLastAndCount(
    @ColumnInfo(name = "first") val first: Date?,
    @ColumnInfo(name = "last") val last: Date?,
    @ColumnInfo(name = "count") val count: Int,
)
