package org.kde.bettercounter.persistence

import androidx.room.*
import java.util.*

@Dao
interface EntryDao {

    @Query("SELECT * FROM entry WHERE name = (:name) ORDER BY id DESC LIMIT 1")
    fun getLastAdded(name: String): Entry?

    @Query("SELECT * FROM entry WHERE name = (:name) ORDER BY date DESC LIMIT 1")
    fun getMostRecent(name: String): Entry?

    @Query("SELECT * FROM entry WHERE name = (:name) ORDER BY date ASC LIMIT 1")
    fun getLeastRecent(name: String): Entry?

    @Query("SELECT COUNT(*) FROM entry WHERE name = (:name)")
    fun getCount(name: String): Int

    @Query(
        "SELECT " +
            "(SELECT date FROM entry WHERE name = (:name) ORDER BY date ASC LIMIT 1) as first," +
            "(SELECT date FROM entry WHERE name = (:name) ORDER BY date DESC LIMIT 1) as last," +
            "(SELECT COUNT(*) FROM entry WHERE name = (:name)) as count"
    )
    fun getFirstLastAndCount(name: String): FirstLastAndCount

    @Query("SELECT COUNT(*) FROM entry WHERE name = (:name) AND date >= (:since) AND date <= (:until)")
    fun getCountInRange(name: String, since: Date, until: Date): Int

    @Query("UPDATE entry set name = (:newName) WHERE name = (:oldName)")
    fun renameCounter(oldName: String, newName: String): Int

    @Query("SELECT * FROM entry WHERE name = (:name) AND date >= (:since) AND date <= (:until) ORDER BY date ASC")
    fun getAllEntriesInRangeSortedByDate(name: String, since: Date, until: Date): List<Entry>

    @Query("SELECT * FROM entry WHERE name = (:name) ORDER BY date ASC")
    fun getAllEntriesSortedByDate(name: String): List<Entry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: Entry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun bulkInsert(entry: List<Entry>)

    @Delete
    fun delete(entry: Entry)

    @Query("DELETE FROM entry WHERE name = (:name)")
    fun deleteAll(name: String)
}
