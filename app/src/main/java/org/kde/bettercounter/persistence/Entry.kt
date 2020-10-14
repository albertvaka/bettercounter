package org.kde.bettercounter.persistence

import androidx.room.*
import org.kde.bettercounter.boilerplate.Converters
import java.util.*

@Entity(indices = [Index("name")])
@TypeConverters(Converters::class)
data class Entry(
        @PrimaryKey(autoGenerate = true) val id: Int? =  null,
        val date: Date,
        val name: String,
)
