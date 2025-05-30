package org.kde.bettercounter.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.Date

@Dao
interface EntryDao {

    @Query("SELECT * FROM entry WHERE name = (:name) ORDER BY id DESC LIMIT 1")
    suspend fun getLastAdded(name: String): Entry?

    @Query("SELECT * FROM entry WHERE name = (:name) ORDER BY date DESC LIMIT 1")
    suspend fun getMostRecent(name: String): Entry?

    @Query("SELECT * FROM entry WHERE name = (:name) ORDER BY date ASC LIMIT 1")
    suspend fun getLeastRecent(name: String): Entry?

    @Query("SELECT COUNT(*) FROM entry WHERE name = (:name)")
    suspend fun getCount(name: String): Int

    @Query("SELECT date FROM entry WHERE name = (:name) ORDER BY date ASC LIMIT 1")
    suspend fun getFirstDate(name: String): Date?

    @Query("SELECT date FROM entry WHERE name = (:name) ORDER BY date DESC LIMIT 1")
    suspend fun getLastDate(name: String): Date?

    @Query("SELECT COUNT(*) FROM entry WHERE name = (:name) AND date >= (:since) AND date <= (:until)")
    suspend fun getCountInRange(name: String, since: Date, until: Date): Int

    @Query("UPDATE entry set name = (:newName) WHERE name = (:oldName)")
    suspend fun renameAllEntries(oldName: String, newName: String): Int

    @Query("SELECT * FROM entry WHERE name = (:name) AND date >= (:since) AND date <= (:until) ORDER BY date ASC")
    suspend fun getAllEntriesInRangeSortedByDate(name: String, since: Date, until: Date): List<Entry>

    @Query("SELECT * FROM entry WHERE name = (:name) ORDER BY date ASC")
    suspend fun getAllEntriesSortedByDate(name: String): List<Entry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: Entry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsert(entry: List<Entry>)

    @Delete
    suspend fun delete(entry: Entry)

    @Query("DELETE FROM entry WHERE name = (:name)")
    suspend fun deleteAll(name: String)
}
