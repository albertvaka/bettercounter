package org.kde.bettercounter.db;

import androidx.room.*

@Dao
public interface EntryDao {
    @Query("SELECT * FROM entry")
    fun getAll(): List<Entry>

    @Query("SELECT * FROM entry WHERE type = (:type)")
    fun getByType(type : String) : List<Entry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry : Entry);

    @Delete
    fun delete(entry : Entry);
}