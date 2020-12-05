package org.kde.bettercounter.persistence

import androidx.room.*
import java.util.*

@Dao
interface EntryDao {

    @Query("SELECT * FROM entry WHERE name = (:name) ORDER BY date DESC LIMIT 1")
    fun getMostRecent(name : String) : Entry?

    @Query("SELECT COUNT(*) FROM entry WHERE name = (:name) and date >= (:since)")
    fun getCountSince(name : String, since: Date) : Int

    @Query("SELECT COUNT(*) FROM entry WHERE name = (:name)")
    fun getCount(name : String) : Int

    @Query("UPDATE entry set name = (:newName) WHERE name = (:oldName)")
    fun renameCounter(oldName : String, newName : String) : Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry : Entry)

    @Delete
    fun delete(entry : Entry)

    @Query("DELETE FROM entry WHERE name = (:name)")
    fun deleteAll(name : String)
}
