package org.kde.bettercounter.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters
import java.util.*

@Entity
@TypeConverters(Converters::class)
data class Entry(
        @PrimaryKey val uid: Int,
        @ColumnInfo(name = "date") val date: Date?,
        @ColumnInfo(name = "type") val type: String?
)
