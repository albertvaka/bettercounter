package org.kde.bettercounter.boilerplate

import androidx.room.TypeConverter
import org.json.JSONArray
import java.util.*


class Converters {
    companion object {
        @TypeConverter
        @JvmStatic
        fun dateFromTimestamp(value: Long?): Date? {
            return value?.let { Date(it) }
        }

        @TypeConverter
        @JvmStatic
        fun dateToTimestamp(date: Date?): Long? {
            return date?.time
        }

        @JvmStatic
        fun stringListToString(list: List<String>?): String? {
            return JSONArray(list).toString()
        }

        @JvmStatic
        fun stringToStringList(jsonStr: String?): List<String> {
            return try {
                val json = JSONArray(jsonStr)
                val ret: MutableList<String> = ArrayList()
                for (i in 0 until json.length()) {
                    ret.add(json.getString(i))
                }
                ret
            } catch(e : Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
