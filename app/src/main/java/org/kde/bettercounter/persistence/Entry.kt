package org.kde.bettercounter.persistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import org.kde.bettercounter.boilerplate.Converters
import java.util.Date

@Entity(indices = [Index("name")])
@TypeConverters(Converters::class)
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val date: Date,
    val name: String,
)
